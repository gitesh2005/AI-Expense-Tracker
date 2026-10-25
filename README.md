# AI Expense Tracker — Backend

Spring Boot REST API for the AI Expense Tracker. Authenticated users can add
expenses manually or from a supported payment screenshot, review category and
monthly summaries, request AI savings suggestions, and export their data to
Excel.

## Live services

- **Frontend:** [ai-expense-tracker-frontend.netlify.app](https://ai-expense-tracker-frontend.netlify.app)
- **Backend API:** [ai-expense-tracker-ow9p.onrender.com](https://ai-expense-tracker-ow9p.onrender.com)

The hosted backend runs on Render's free tier, so the first request after
inactivity can take 30–60 seconds while it wakes up.

## Local setup

### Backend

1. Start MongoDB locally or create a MongoDB Atlas cluster.
2. Copy `src/main/resources/application.properties.example` to
   `src/main/resources/application.properties`.
3. Set `SPRING_DATA_MONGODB_URI`, `GROQ_API_KEY`, `GROQ_API_URL`, `GROQ_MODEL`,
   and a random `JWT_SECRET` (at least 32 UTF-8 bytes).
4. Keep `CORS_ALLOWED_ORIGINS` at its default for the Vite frontend, or set it
   to a comma-separated list of trusted frontend origins.
5. Run the API:

   ```bash
   ./mvnw spring-boot:run
   ```

The API listens on `http://localhost:8080` by default. `GET /api/health` is a
public health check; all expense endpoints require a JWT returned by
`POST /api/auth/register` or `POST /api/auth/login`.

### Frontend

The frontend lives in the sibling `expense-tracker-frontend` directory. From
that directory:

```bash
npm ci
copy .env.example .env.local       # PowerShell: Copy-Item .env.example .env.local
npm run dev
```

The default `VITE_API_BASE_URL=/api` uses the Vite development proxy to forward
API calls to `http://localhost:8080`. Set `VITE_DEV_API_PROXY_TARGET` when the
backend runs on another local port. For a separately hosted frontend, set
`VITE_API_BASE_URL` to the deployed API's `/api` URL at build time.

## Features

- JWT authentication with account-scoped expenses and monthly archives
- AI auto-categorization and savings suggestions through Groq
- Voice input through the browser Speech Recognition API
- PNG, JPEG, or WebP payment screenshot upload (5 MB maximum)
- Manual category override, charts, monthly history, and Excel export

## Checks

```bash
./mvnw test
```

The repository CI also runs the frontend lint and production build.
