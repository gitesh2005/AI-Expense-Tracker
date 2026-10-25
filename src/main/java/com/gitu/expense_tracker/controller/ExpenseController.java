package com.gitu.expense_tracker.controller;

import com.gitu.expense_tracker.entity.Expense;
import com.gitu.expense_tracker.entity.MonthlyArchive;
import com.gitu.expense_tracker.dto.CategoryUpdateRequest;
import com.gitu.expense_tracker.dto.ExpenseRequest;
import com.gitu.expense_tracker.dto.ExpenseResponse;
import com.gitu.expense_tracker.exception.ResourceNotFoundException;
import com.gitu.expense_tracker.repository.ExpenseRepository;
import com.gitu.expense_tracker.repository.MonthlyArchiveRepository;
import com.gitu.expense_tracker.security.UserPrincipal;
import com.gitu.expense_tracker.service.CategoryService;
import com.gitu.expense_tracker.service.SavingsService;
import com.gitu.expense_tracker.service.ScreenshotService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import java.util.Set;
import java.util.TreeMap;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of("Food", "Travel", "Shopping", "Bills", "Entertainment", "Health", "Other");
    // The response remains a list for compatibility with the dashboard. Clients can request
    // subsequent pages with ?page=N&size=N, while an omitted size is still bounded.
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int BATCH_SIZE = 250;
    private static final Sort EXPENSE_SORT = Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id"));
    private static final Sort ARCHIVE_SORT = Sort.by(Sort.Order.desc("archivedDate"), Sort.Order.desc("id"));

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
    public List<ExpenseResponse> getAllExpenses(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "1000") int size,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return expenseRepository.findAllByOwnerId(principal.id(), pageable(page, size, EXPENSE_SORT))
                .getContent().stream().map(ExpenseResponse::from).toList();
    }

    @PostMapping
    public ExpenseResponse addExpense(@Valid @RequestBody ExpenseRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Expense expense = new Expense();
        expense.setDescription(request.description().trim());
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setId(null);
        expense.setOwnerId(principal.id());
        String category = categoryService.categorize(expense.getDescription());
        expense.setCategory(normalizeCategory(category));

        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        expenseRepository.deleteByIdAndOwnerId(id, principal.id());
    }

    @DeleteMapping
    public void deleteAllExpenses(@AuthenticationPrincipal UserPrincipal principal) throws Exception {

        Map<String, BigDecimal> categoryTotals = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean hasExpenses = false;
        int page = 0;
        Page<Expense> batch;
        do {
            batch = expenseRepository.findAllByOwnerId(principal.id(),
                    PageRequest.of(page++, BATCH_SIZE, EXPENSE_SORT));
            for (Expense e : batch.getContent()) {
                hasExpenses = true;
                String cat = e.getCategory() != null ? e.getCategory() : "Other";
                BigDecimal amount = requireAmount(e.getAmount());
                categoryTotals.merge(normalizeCategory(cat), amount, BigDecimal::add);
                total = total.add(amount);
            }
        } while (batch.hasNext());

        if (hasExpenses) {

            ObjectMapper mapper = new ObjectMapper();
            String breakdownJson = mapper.writeValueAsString(categoryTotals);
            LocalDate archivedDate = LocalDate.now();

            MonthlyArchive archive = new MonthlyArchive();
            archive.setOwnerId(principal.id());
            archive.setMonthLabel(archivedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            archive.setTotalAmount(total);
            archive.setCategoryBreakdown(breakdownJson);
            archive.setArchivedDate(archivedDate);

            // Archive first. If saving the archive fails, expenses are left intact and can be retried.
            monthlyArchiveRepository.save(archive);
        }

        expenseRepository.deleteAllByOwnerId(principal.id());
    }

    @GetMapping("/archives")
    public List<MonthlyArchive> getArchives(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "1000") int size,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return monthlyArchiveRepository.findAllByOwnerId(principal.id(), pageable(page, size, ARCHIVE_SORT))
                .getContent();
    }

    @GetMapping("/insights")
    public Map<String, String> getInsights(@AuthenticationPrincipal UserPrincipal principal) {
        Map<String, BigDecimal> categoryTotals = new TreeMap<>();
        int page = 0;
        Page<Expense> batch;
        do {
            batch = expenseRepository.findAllByOwnerId(principal.id(),
                    PageRequest.of(page++, BATCH_SIZE, EXPENSE_SORT));
            for (Expense e : batch.getContent()) {
                String cat = e.getCategory() != null ? e.getCategory() : "Other";
                categoryTotals.merge(normalizeCategory(cat), requireAmount(e.getAmount()), BigDecimal::add);
            }
        } while (batch.hasNext());

        if (categoryTotals.isEmpty()) {
            Map<String, String> result = new HashMap<>();
            result.put("suggestion", "Add some expenses first to get savings suggestions.");
            return result;
        }

        Map<String, Double> suggestionTotals = new TreeMap<>();
        categoryTotals.forEach((key, value) -> suggestionTotals.put(key, value.doubleValue()));
        String suggestion = savingsService.getSavingsSuggestion(suggestionTotals);

        Map<String, String> result = new HashMap<>();
        result.put("suggestion", suggestion);
        return result;
    }

    @PostMapping("/from-screenshot")
    public ExpenseResponse addFromScreenshot(@RequestPart("image") MultipartFile image,
                                             @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        Map<String, String> extracted = screenshotService.extractExpenseFromImage(image);

        String description = extracted.get("description");
        BigDecimal amount;
        try {
            amount = new BigDecimal(extracted.get("amount"));
        } catch (Exception ex) {
            throw new IllegalArgumentException("The screenshot did not contain a valid amount");
        }
        validateDescriptionAndAmount(description, amount);
        description = description.trim();

        String category = categoryService.categorize(description);

        Expense expense = new Expense();
        expense.setOwnerId(principal.id());
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setCategory(normalizeCategory(category));
        expense.setExpenseDate(LocalDate.now());

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @PutMapping("/{id}/category")
    public ExpenseResponse updateCategory(@PathVariable String id, @Valid @RequestBody CategoryUpdateRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Expense expense = expenseRepository.findByIdAndOwnerId(id, principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        String category = request.category();
        if (category == null || !ALLOWED_CATEGORIES.contains(category)) throw new IllegalArgumentException("Invalid category");
        expense.setCategory(category);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @DeleteMapping("/archives/{id}")
    public void deleteArchive(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        monthlyArchiveRepository.deleteByIdAndOwnerId(id, principal.id());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Expenses");

        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Description", "Category", "Amount", "Date"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;
        BigDecimal total = BigDecimal.ZERO;
        int page = 0;
        Page<Expense> batch;
        do {
            batch = expenseRepository.findAllByOwnerId(principal.id(),
                    PageRequest.of(page++, BATCH_SIZE, EXPENSE_SORT));
            for (Expense e : batch.getContent()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(safeSpreadsheetText(e.getDescription()));
                row.createCell(2).setCellValue(safeSpreadsheetText(e.getCategory()));
                row.createCell(3).setCellValue(requireAmount(e.getAmount()).doubleValue());
                row.createCell(4).setCellValue(e.getExpenseDate() == null ? "" : e.getExpenseDate().toString());
                total = total.add(requireAmount(e.getAmount()));
            }
        } while (batch.hasNext());

        Row totalRow = sheet.createRow(rowIdx + 1);
        totalRow.createCell(2).setCellValue("Total:");
        totalRow.createCell(3).setCellValue(total.doubleValue());

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

    private void validateDescriptionAndAmount(String description, BigDecimal amount) {
        if (description == null || description.isBlank() || description.length() > 200) {
            throw new IllegalArgumentException("Description must be 1-200 characters");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() > 12) {
            throw new IllegalArgumentException("Amount must be positive and contain at most two decimals");
        }
    }

    private BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Expense amount is required");
        return amount;
    }

    private Pageable pageable(int page, int size, Sort sort) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }

    private String normalizeCategory(String category) {
        if (category == null) return "Other";
        return ALLOWED_CATEGORIES.stream().filter(valid -> valid.equalsIgnoreCase(category.trim())).findFirst().orElse("Other");
    }

    private String safeSpreadsheetText(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.matches("^[=+\\-@].*") ? "'" + trimmed : value;
    }
}
