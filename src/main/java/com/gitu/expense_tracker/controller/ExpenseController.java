package com.gitu.expense_tracker.controller;

import com.gitu.expense_tracker.entity.Expense;
import com.gitu.expense_tracker.entity.MonthlyArchive;
import com.gitu.expense_tracker.repository.ExpenseRepository;
import com.gitu.expense_tracker.repository.MonthlyArchiveRepository;
import com.gitu.expense_tracker.service.CategoryService;
import com.gitu.expense_tracker.service.SavingsService;
import com.gitu.expense_tracker.service.ScreenshotService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MonthlyArchiveRepository monthlyArchiveRepository;

    @Autowired
    private ScreenshotService screenshotService;

    @Autowired
    private SavingsService savingsService;

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    @PostMapping
    public Expense addExpense(@RequestBody Expense expense) {
        String category = categoryService.categorize(expense.getDescription());
        expense.setCategory(category);

        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }

        return expenseRepository.save(expense);
    }

    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable String id) {
        expenseRepository.deleteById(id);
    }

    @DeleteMapping
    public void deleteAllExpenses() throws Exception {

        List<Expense> expenses = expenseRepository.findAll();

        if (!expenses.isEmpty()) {
            Map<String, Double> categoryTotals = new HashMap<>();
            double total = 0;

            for (Expense e : expenses) {
                String cat = e.getCategory() != null ? e.getCategory() : "Other";
                categoryTotals.merge(cat, e.getAmount().doubleValue(), Double::sum);
                total += e.getAmount().doubleValue();
            }

            ObjectMapper mapper = new ObjectMapper();
            String breakdownJson = mapper.writeValueAsString(categoryTotals);

            MonthlyArchive archive = new MonthlyArchive();
            archive.setMonthLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            archive.setTotalAmount(BigDecimal.valueOf(total));
            archive.setCategoryBreakdown(breakdownJson);
            archive.setArchivedDate(LocalDate.now());

            monthlyArchiveRepository.save(archive);
        }

        expenseRepository.deleteAll();
    }

    @GetMapping("/archives")
    public List<MonthlyArchive> getArchives() {
        return monthlyArchiveRepository.findAll();
    }

    @GetMapping("/insights")
    public Map<String, String> getInsights() {
        List<Expense> expenses = expenseRepository.findAll();

        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense e : expenses) {
            String cat = e.getCategory() != null ? e.getCategory() : "Other";
            categoryTotals.merge(cat, e.getAmount().doubleValue(), Double::sum);
        }

        if (categoryTotals.isEmpty()) {
            Map<String, String> result = new HashMap<>();
            result.put("suggestion", "Add some expenses first to get savings suggestions.");
            return result;
        }

        String suggestion = savingsService.getSavingsSuggestion(categoryTotals);

        Map<String, String> result = new HashMap<>();
        result.put("suggestion", suggestion);
        return result;
    }

    @PostMapping("/from-screenshot")
    public Expense addFromScreenshot(@RequestParam("image") MultipartFile image) throws IOException {

        Map<String, String> extracted = screenshotService.extractExpenseFromImage(image);

        String description = extracted.get("description");
        BigDecimal amount = new BigDecimal(extracted.get("amount"));

        String category = categoryService.categorize(description);

        Expense expense = new Expense();
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setExpenseDate(LocalDate.now());

        return expenseRepository.save(expense);
    }

    @PutMapping("/{id}/category")
    public Expense updateCategory(@PathVariable String id, @RequestBody Map<String, String> body) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expense.setCategory(body.get("category"));
        return expenseRepository.save(expense);
    }

    @DeleteMapping("/archives/{id}")
    public void deleteArchive(@PathVariable String id) {
        monthlyArchiveRepository.deleteById(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        List<Expense> expenses = expenseRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Expenses");

        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Description", "Category", "Amount", "Date"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;
        double total = 0;
        for (Expense e : expenses) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getId());
            row.createCell(1).setCellValue(e.getDescription());
            row.createCell(2).setCellValue(e.getCategory());
            row.createCell(3).setCellValue(e.getAmount().doubleValue());
            row.createCell(4).setCellValue(e.getExpenseDate().toString());
            total += e.getAmount().doubleValue();
        }

        Row totalRow = sheet.createRow(rowIdx + 1);
        totalRow.createCell(2).setCellValue("Total:");
        totalRow.createCell(3).setCellValue(total);

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
    }
}