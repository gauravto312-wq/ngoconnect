# NGOConnect

NGOConnect is a community donation, food distribution, and volunteer management platform connecting citizens, NGOs, restaurants, and volunteers to reduce food waste and coordinate relief efforts efficiently.

---

## Key Features

- **Multi-Role Role-Based Access Control**:
  - **Citizen (Donor)**: Create food donations, browse marketplace, register for food drives, and track donation claims.
  - **NGO**: Browse surplus food donations, claim items, dispatch volunteers, submit verifications, and organize community food drives.
  - **Restaurant**: Publish surplus meals, schedule donations, manage branches, and coordinate volunteer pickups.
  - **Volunteer**: View and accept assigned delivery tasks, submit pickup/handover codes, track earnings history, and participate in food drives.
  - **Admin**: Review NGO and restaurant verifications, resolve payment disputes, inspect contact inquiries, manage users, and view platform metrics.
- **Two-Stage Handover & Verification**: Secure donation pickup and handover codes between donors, volunteers, and NGOs.
- **Community Food Drives**: Scheduled meal distribution drives with evidence photo uploads and completion tracking.
- **Notifications & Unread Badge Sync**: System notifications with real-time unread badge counts and user-isolated read operations.
- **Paid & Unpaid Volunteer Tasks**: Task dispatch with optional stipends, status tracking, and earnings summaries.

---

## Technology Stack

### Backend
- **Language & Runtime**: Java 17+ (compatible up to Java 25)
- **Framework**: Spring Boot 3 (Spring Security, Spring Data JPA, Spring Web)
- **Authentication**: Stateless JWT (`jjwt` 0.12.5) with BCrypt password hashing
- **Build Tool**: Apache Maven
- **Database**: MySQL 8.0+ (Production / Local), H2 In-Memory (Automated test profile)

### Frontend
- **Framework**: React 18
- **Build Tool & Dev Server**: Vite 5
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **Routing & Networking**: React Router 6, Axios

---

## Prerequisites

Before running the application locally, ensure you have installed:
- **Java Development Kit (JDK)**: Version 17 or higher
- **Node.js**: Version 18.x or higher (with npm)
- **Apache Maven**: Version 3.8.x or higher
- **MySQL Server**: Version 8.0 or higher

---

## Environment Configuration

1. Copy the root environment template:
   ```bash
   cp .env.example .env
   ```
   Or configure environment files individually in `backend/` and `frontend/`:
   ```bash
   cp backend/.env.example backend/.env
   cp frontend/.env.example frontend/.env
   ```

2. Configure environment variables in `.env`:

   | Variable | Description | Example / Default |
   | :--- | :--- | :--- |
   | `SPRING_DATASOURCE_URL` | MySQL JDBC Connection URL | `jdbc:mysql://localhost:3306/ngoconnect?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
   | `SPRING_DATASOURCE_USERNAME` | MySQL database username | `root` |
   | `SPRING_DATASOURCE_PASSWORD` | MySQL database password | `your_db_password` |
   | `JWT_SECRET` | 256-bit secret key for JWT signing | `your_jwt_secret_key_minimum_256_bits_long` |
   | `PORT` | Backend server port | `8080` |
   | `FRONTEND_URL` | Allowed CORS origin for frontend | `http://localhost:5173` |
   | `INITIAL_ADMIN_EMAIL` | Initial admin email (fresh setup only) | `admin@ngoconnect.org` |
   | `INITIAL_ADMIN_PASSWORD` | Initial admin password (min 8 chars) | `your_secure_admin_password` |
   | `INITIAL_ADMIN_NAME` | Initial admin name (optional) | `System Administrator` |
   | `INITIAL_ADMIN_PHONE` | Initial admin phone (optional) | `9999999999` |
   | `VITE_API_BASE_URL` | Frontend REST API base URL | `/api/v1` |
   | `VITE_BACKEND_URL` | Backend server URL for uploads | `http://localhost:8080` |

   > **Note on Administrator Setup**: The application automatically checks if an administrator account exists. If an admin is already present in the database, its password, role, and profile are **never** modified or reset upon startup. For fresh database initializations where no admin exists, configure `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD` in your local environment.

---

## Database Setup

1. Start your local MySQL service.
2. Create the database:
   ```sql
   CREATE DATABASE ngoconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Initialize the schema:
   - Apply base schema from `database/schema.sql`
   - Apply incremental migrations from `database/migrations/` (`V1__initial_schema.sql`, `V2__reconcile_schema.sql`)
   *(Spring Boot Hibernate DDL is configured with `update` mode for seamless table creation upon initial run).*

---

## Running the Application

### 1. Start the Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
The REST API server will start on `http://localhost:8080`.

### 2. Start the Frontend (React + Vite)
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
The frontend development server will start on `http://localhost:5173`.

---

## Testing & Build

### Running Backend Tests
Execute the automated test suite (runs against an isolated in-memory H2 database):
```bash
cd backend
mvn test
```

### Building the Frontend for Production
```bash
cd frontend
npm run build
```
Production assets are generated in `frontend/dist/`.

---

## Security & Repository Guidelines

- **Never Commit Secrets**: Ensure `.env`, local database dumps, private credentials, or personal configuration files are never committed.
- **Git Ignore**: The repository includes comprehensive `.gitignore` rules for build artifacts (`target/`, `dist/`), upload files (`uploads/`), database dumps, and editor settings (`.vscode/`, `.idea/`).
- **File Uploads**: User-uploaded media in `backend/uploads/` are excluded from version control while preserving the directory via `.gitkeep`.
