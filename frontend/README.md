# Customer management – frontend

React + TypeScript + Vite single-page app for the customer REST API (`/api/customers`).
The user interface is in Turkish.

Features: customer list with search, sorting and paging, add/edit form with the same
validation rules as the backend, active/passive switch, and delete with confirmation.

## Requirements

- Node.js 24 or newer (LTS)
- The backend running on `http://localhost:8080` (see the project root)

## Run in development

```bash
npm install
npm run dev
```

Open http://localhost:5173. Vite forwards every `/api` request to `http://localhost:8080`,
so no extra setup is needed while developing.

## Other commands

| Command           | What it does                                  |
| ----------------- | --------------------------------------------- |
| `npm test`        | Runs the unit tests (Vitest)                  |
| `npm run lint`    | Checks the code with Oxlint                   |
| `npm run build`   | Type-checks and builds the production files into `dist/` |
| `npm run preview` | Serves the built `dist/` folder locally       |

## Backend address in production

When the frontend and the backend are on different addresses, set `VITE_API_URL`
before building (see `.env.example`), for example:

```bash
VITE_API_URL=https://api.example.com npm run build
```

The backend must also allow the frontend's address with `CORS_ALLOWED_ORIGINS`.

## Folder layout

```
src/
  api/          HTTP client and customer API calls
  components/   Table, form, dialogs, pagination, toast
  hooks/        Small reusable React hooks
  lib/          Form validation and formatting helpers (with tests)
```
