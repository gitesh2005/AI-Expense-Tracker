# AI Expense Tracker

A full-stack expense tracker that eliminates manual data entry — type, speak, or upload a payment screenshot, and AI handles the rest.

🔗 **Live Demo:** [ai-expense-tracker-frontend.netlify.app](https://ai-expense-tracker-frontend.netlify.app)
🔗 **Backend API:** [ai-expense-tracker-ow9p.onrender.com](https://ai-expense-tracker-ow9p.onrender.com)

> Note: Backend is on Render's free tier, so the first request after inactivity may take 30-60 seconds to wake up.

## Features

- **AI auto-categorization** — type "swiggy order 350" and it's tagged "Food" automatically
- **Voice input** — mic button uses browser speech recognition to fill the description
- **Screenshot/invoice upload** — upload a UPI payment confirmation or invoice; a vision AI model extracts the merchant and amount directly from the image
- **Manual category override** — click any category badge to correct it when AI gets it wrong
- **Spending breakdown** — donut chart by category
- **AI savings suggestions** — analyzes your spending pattern and tells you where to cut back
- **Excel export** — download expenses as a formatted .xlsx file
- **Monthly reset with history** — "Clear All" archives the month's totals before wiping, and a Monthly History view shows month-over-month % change (with the ability to delete a mistaken archive entry)

## Tech Stack

- **Backend:** Spring Boot, Spring Data MongoDB, Apache POI (Excel export)
- **Database:** MongoDB Atlas (cloud, free tier)
- **Frontend:** React (Vite), Recharts, Web Speech API
- **AI:** Groq API — `openai/gpt-oss-120b` (text categorization/insights), `qwen/qwen3.6-27b` (vision/OCR for screenshots)
- **Deployment:** Render (backend, Docker), Netlify (frontend)

## Architecture

This is a two-repo project:
- **Backend** (this repo): Spring Boot REST API
- **Frontend:** [AI-Expense-Tracker-Frontend](https://github.com/gitesh2005/AI-Expense-Tracker-Frontend)

## Local Setup

### Backend
1. Create a free [MongoDB Atlas](https://www.mongodb.com/cloud/atlas/register) cluster and get your connection string
2. Copy `application.properties.example` to `application.properties`
3. Fill in your MongoDB URI and Groq API key
4. Run: `./mvnw spring-boot:run`

### Frontend
1. Clone the [frontend repo](https://github.com/gitesh2005/AI-Expense-Tracker-Frontend)
2. Update `src/api/axiosConfig.js` with your backend URL (or `http://localhost:8080/api` for local)
3. Run: `npm install && npm run dev`

## Data Model (MongoDB Collections)

**expenses**
```json
{
  "description": "string",
  "amount": "decimal",
  "category": "string",
  "expenseDate": "date"
}
```

**monthly_archive**
```json
{
  "monthLabel": "string",
  "totalAmount": "decimal",
  "categoryBreakdown": "JSON string",
  "archivedDate": "date"
}
```

## Future Scope

- Native Android companion app for SMS-based automatic transaction detection
- Multi-user support with authentication
- Budget limits per category with alerts