# Customer Management

[![CI](https://github.com/kubraaucar/customer-management-service/actions/workflows/ci.yml/badge.svg)](https://github.com/kubraaucar/customer-management-service/actions/workflows/ci.yml)

A full-stack customer management application: a Spring Boot REST API with a React frontend,
PostgreSQL storage and token-based login. The user interface is in Turkish.

## Features

- Customer list with search (by full name), sorting and paging
- Add, edit and delete customers (with confirmation); active/passive switch
- The same validation rules and Turkish messages in the form and in the API
- Login as administrator (full access), or with one click as a read-only demo user
- Errors returned as RFC 9457 problem details (`detail`, plus per-field `errors`)
- Database schema versioned with Flyway
- Interactive API documentation (Swagger UI)
- Automated tests and a CI pipeline that also builds and starts the Docker images

## Tech stack

| Part     | Technologies |
| -------- | ------------ |
| Backend  | Java 17, Spring Boot 4.1, Spring Security (JWT), Spring Data JPA, Flyway, springdoc-openapi |
| Database | PostgreSQL 17 |
| Frontend | React 19, TypeScript, Vite 8 |
| Tests    | JUnit 5, Mockito, MockMvc, Testcontainers, Vitest |
| Tooling  | Docker, Docker Compose, nginx, GitHub Actions, Oxlint |

## Run the whole app with Docker

Requires Docker Desktop.

```bash
docker compose up -d --build
```

Then open http://localhost:3000 and log in with `admin` / `admin12345`, or press **Demo olarak dene**.
Swagger UI: http://localhost:3000/swagger-ui.html

Stop with `docker compose down` (the data is kept; add `-v` to delete it too).

## Local development

1. Start only the database: `docker compose up -d postgresql`
2. Backend (http://localhost:8080, Swagger UI at `/swagger-ui.html`):
   `./mvnw spring-boot:run` (on Windows: `.\mvnw.cmd spring-boot:run`)
3. Frontend (http://localhost:5173), in another terminal:
   `cd frontend`, `npm install`, `npm run dev`. See [frontend/README.md](frontend/README.md).

## Configuration

Settings are read from environment variables (see [.env.example](.env.example)).
The defaults are meant for local development only.

| Variable | Default | Meaning |
| -------- | ------- | ------- |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5432`, `customer_db` | Database address |
| `DB_USERNAME`, `DB_PASSWORD` | `postgres`, `12345` | Database login |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | – | Full JDBC address and login; replace the `DB_*` settings (used for hosted databases that need `?sslmode=require`) |
| `PORT` | `8080` | HTTP port (set by hosting services) |
| `JWT_SECRET` | development value | Key that signs login tokens (32+ characters) |
| `JWT_EXPIRATION` | `8h` | How long a login stays valid |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | `admin`, `admin12345` | Administrator account, created or updated at startup |
| `DEMO_ENABLED` | `true` | Shows the read-only demo login |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Frontend addresses allowed to call the API |
| `SPRING_PROFILES_ACTIVE` | – | Set to `prod` in production: then `JWT_SECRET` and `ADMIN_PASSWORD` are required |

## Deployment

The live version runs on free plans:

| Part | Service | Setup |
| ---- | ------- | ----- |
| Database | [Neon](https://neon.tech) (PostgreSQL) | Direct (non-pooled) connection, used by the backend through `SPRING_DATASOURCE_*` |
| Backend | [Render](https://render.com) (Docker) | [render.yaml](render.yaml) Blueprint; `SPRING_PROFILES_ACTIVE=prod` |
| Frontend | [Vercel](https://vercel.com) | Root directory `frontend`, `VITE_API_URL` = backend address |

The backend's `CORS_ALLOWED_ORIGINS` must contain the frontend address. Render's free plan stops the
backend after a period without traffic, so the first request afterwards can take up to a minute;
the login page shows a notice while the server wakes up.

## API

All endpoints except login need the header `Authorization: Bearer <token>`.

| Method | Path | Who | Description |
| ------ | ---- | --- | ----------- |
| POST | `/api/auth/login` | everyone | Log in with `{"username", "password"}`, returns a token |
| POST | `/api/auth/demo` | everyone | Read-only demo login |
| GET | `/api/customers?search=&page=&size=&sort=` | admin, demo | Search and page through customers |
| GET | `/api/customers/{id}` | admin, demo | One customer |
| POST | `/api/customers` | admin | Add a customer |
| PUT | `/api/customers/{id}` | admin | Update a customer |
| PATCH | `/api/customers/{id}/activation` | admin | Set active/passive: `{"active": true}` |
| DELETE | `/api/customers/{id}` | admin | Delete a customer |
| GET | `/actuator/health` | everyone | Health check (`{"status":"UP"}`) |

## Tests

```bash
./mvnw verify          # backend; integration tests need Docker and are skipped without it
cd frontend && npm test
```

## Project structure

```
src/main/java/.../customermanagementservice
  controller/   REST endpoints
  service/      Business rules
  repository/   Database access (Spring Data JPA)
  model/        JPA entities
  dto/          Request and response objects
  security/     Login, tokens, permissions, CORS
  exceptions/   Errors and the error response handler
src/main/resources
  db/migration/ Flyway SQL migrations
frontend/       React app (see frontend/README.md)
```
