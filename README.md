# AI Expense Tracker

A full-stack expense tracker that eliminates manual data entry — type, speak, or upload a payment screenshot, and AI handles the rest.

## Features

- **AI auto-categorization** — type "swiggy order 350" and it's tagged "Food" automatically
- **Voice input** — mic button uses browser speech recognition to fill the description
- **Screenshot/invoice upload** — upload a UPI payment confirmation or invoice; a vision AI model extracts the merchant and amount directly from the image
- **Manual category override** — click any category badge to correct it when AI gets it wrong
- **Spending breakdown** — donut chart by category
- **AI savings suggestions** — analyzes your spending pattern and tells you where to cut back
- **Excel export** — download expenses as a formatted .xlsx file
- **Monthly reset with history** — "Clear All" archives the month's totals before wiping, and a Monthly History view shows month-over-month % change

## Tech Stack

- Backend: Spring Boot, Spring Data JPA, MySQL, Apache POI (Excel export)
- Frontend: React (Vite), Recharts, Web Speech API
- AI: Groq API — openai/gpt-oss-120b (text categorization/insights), qwen/qwen3.6-27b (vision/OCR for screenshots)

## Setup

1. Create MySQL database `expense_tracker_db` and run the schema below
2. Copy `application.properties.example` to `application.properties`, fill in your DB password and Groq API key
3. Run backend: `./mvnw spring-boot:run`
4. Run frontend: `cd expense-tracker-frontend && npm install && npm run dev`

## Schema

```sql
CREATE TABLE expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    expense_date DATE NOT NULL
);
```

```sql
CREATE TABLE monthly_archive (
    id INT AUTO_INCREMENT PRIMARY KEY,
    month_label VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    category_breakdown TEXT,
    archived_date DATE NOT NULL
);
```

## Future Scope

- Native Android companion app for SMS-based automatic transaction detection
- Multi-user support with authentication
- Budget limits per category with alerts
