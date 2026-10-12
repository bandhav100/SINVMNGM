# Smart Inventory Management System (SINVMNGM)

An enterprise-grade, full-stack Smart Inventory Management System built with **React 18 + Vite**, **Java 21 + Spring Boot 3**, and **PostgreSQL**. Designed for robust inventory tracking, purchase workflows, sales orders, automated stock alerts, business analytics, role-based access control (RBAC), and containerized deployments.

---

## 1. Architecture & Technology Stack

### System Architecture
The application follows a clean 3-tier enterprise architecture:
- **Presentation Layer**: Responsive React 18 single-page application built with Vite, Tailwind CSS, Lucide Icons, and Axios client with HttpOnly cookie + Bearer JWT support.
- **Application Layer**: Spring Boot 3 REST API in Java 21, featuring stateless JWT authentication with token revocation, database-level locking for concurrency safety, global exception handling, and Flyway database migrations.
- **Data Persistence Layer**: PostgreSQL relational database with schema-level integrity constraints, indexes, and an immutable inventory transaction ledger.

### Technology Stack
| Layer | Technologies |
| :--- | :--- |
| **Frontend** | React 18, Vite 5, Tailwind CSS, Lucide Icons, Axios |
| **Backend** | Java 21, Spring Boot 3.3.x, Spring Security, Spring Data JPA / Hibernate |
| **Database** | PostgreSQL 15+, Flyway Migration Engine |
| **Testing** | JUnit 5, AssertJ, MockMvc, Karate API Test Framework |
| **DevOps & Containers** | Docker, Docker Compose, Kubernetes manifests, Jenkins Pipeline |

---

## 2. Prerequisites & Supported Versions

Ensure the following tools are installed in your development environment:
- **Java Development Kit (JDK)**: OpenJDK 21 or Oracle JDK 21
- **Node.js**: Node.js 18.x or 20.x LTS with `npm` 9.x+
- **PostgreSQL**: PostgreSQL 15, 16, or compatible Cloud SQL instance
- **Docker & Docker Compose**: Docker Engine 24.x+ and Docker Compose v2 (optional for local containerization)
- **Kubernetes CLI (`kubectl`)**: v1.27+ (optional for cluster deployments)

---

## 3. PostgreSQL Database Setup

1. Connect to PostgreSQL via `psql` or pgAdmin:
   ```bash
   psql -U postgres
   ```
2. Create the application database:
   ```sql
   CREATE DATABASE smart_inventory_db;
   ```
3. Verify connection credentials (default development user `postgres`).

Flyway automatically applies database migrations (`V1` through `V7`) on application startup.

---

## 4. Environment Configuration

Copy `.env.example` to create your local `.env` file:
```bash
cp .env.example .env
```

### Key Environment Variables
| Variable | Description | Default Development Value |
| :--- | :--- | :--- |
| `POSTGRES_DB` | Database name | `smart_inventory_db` |
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | *(your password)* |
| `SPRING_DATASOURCE_URL` | JDBC Connection URL | `jdbc:postgresql://localhost:5432/smart_inventory_db` |
| `SERVER_PORT` | Spring Boot server port | `8081` |
| `JWT_SECRET` | Secret key for JWT signing | *(base64 string minimum 256 bits)* |
| `JWT_EXPIRATION_MS` | Token lifespan | `86400000` (24 hours) |
| `VITE_API_BASE_URL` | Frontend API URL | `http://localhost:8081/api/v1` |

---

## 5. Local Startup

### 5.1 Backend Startup (Spring Boot)
From the project root:
```bash
cd backend
./mvnw clean spring-boot:run
```
*(On Windows PowerShell, use `.\mvnw.cmd clean spring-boot:run`)*

The backend server starts on `http://localhost:8081`.

### 5.2 Frontend Startup (React + Vite)
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```

The frontend development server starts on `http://localhost:5173`.

---

## 6. Initial Administrator Setup & Roles

The system employs a strict bootstrap workflow for security:
1. When launched on a fresh database, `GET /api/v1/auth/setup-status` returns `{ "setupRequired": true }`.
2. The UI automatically presents the **Initial Administrator Setup** form.
3. Submitting `POST /api/v1/auth/setup-admin` creates the primary `ADMIN` account using PostgreSQL transaction advisory locks to guarantee race safety.
4. Subsequent setup calls are permanently rejected with HTTP `409 Conflict`. Public registration is disabled.

### User Roles & Permissions
- **ADMIN**: Full system access including User Management and System Settings.
- **INVENTORY_MANAGER**: Stock adjustments, products, categories, stock audits, alerts, and reports.
- **PURCHASE_MANAGER**: Supplier management, purchase orders creation, receiving, and purchase reports.
- **SALES_STAFF**: Sales orders creation, customer records, and product availability lookups.

---

## 7. Database Migrations (Flyway)

Migrations are located in `backend/src/main/resources/db/migration/`:
- `V1__create_categories_table.sql`: Categories schema and indexes.
- `V2__create_products_and_suppliers_tables.sql`: Products, suppliers, and relationship constraints.
- `V3__create_users_and_roles_tables.sql`: Users and role constraint checks.
- `V4__create_purchase_orders_and_ledger_tables.sql`: Purchase orders, line items, and transaction ledger.
- `V5__create_sales_orders_table.sql`: Sales orders, line items, and customer references.
- `V6__create_system_settings_table.sql`: Business and application configuration table.
- `V7__create_stock_adjustments_table.sql`: Stock adjustment audit log with mandatory reasons.

To run or verify migrations via Maven:
```bash
cd backend
./mvnw flyway:info
```

---

## 8. Testing

### 8.1 Backend Unit & Integration Tests (JUnit 5)
```bash
cd backend
./mvnw clean test
```

### 8.2 API Integration Tests (Karate Framework)
Karate tests validate end-to-end REST API flows across authentication, suppliers, products, purchases, sales, inventory adjustments, and settings:
```bash
cd tests/karate
mvn test
```

### 8.3 Frontend Tests & Production Build
```bash
cd frontend
npm run build
```

---

## 9. API Documentation

Interactive OpenAPI/Swagger documentation is available when the backend is running:
- **Swagger UI**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

---

## 10. Containerization with Docker & Docker Compose

### 10.1 Running the Full Stack with Docker Compose
Run the entire platform (PostgreSQL, Backend, and Frontend) with one command:
```bash
docker compose up -d --build
```

### 10.2 Verifying Services
```bash
docker compose ps
docker compose logs -f backend
```

- **Frontend**: [http://localhost:80](http://localhost:80)
- **Backend API**: [http://localhost:8081](http://localhost:8081)
- **PostgreSQL**: Internal container network on port 5432 (data persisted in Docker volume `postgres_data`).

To tear down containers:
```bash
docker compose down
```

---

## 11. Kubernetes Deployment

Production-ready Kubernetes manifests are provided in the `k8s/` directory:
- `k8s/namespace.yaml`: Dedicated `smart-inventory` namespace.
- `k8s/configmap.yaml`: Non-sensitive application configuration.
- `k8s/secret.yaml`: Database credentials and JWT signing secret placeholders.
- `k8s/postgres-pvc.yaml` & `k8s/postgres-deployment.yaml`: Persistent volume claim and PostgreSQL service.
- `k8s/backend-deployment.yaml`: Spring Boot deployment with health/readiness probes and horizontal scaling support.
- `k8s/frontend-deployment.yaml`: Nginx-served React frontend deployment and ClusterIP service.
- `k8s/ingress.yaml`: Ingress controller routing for external domain traffic.

### Deploying to Kubernetes:
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres-pvc.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/ingress.yaml
```

Check status:
```bash
kubectl get pods -n smart-inventory
```

---

## 12. Jenkins CI/CD Pipeline

The declarative `Jenkinsfile` automates the complete CI/CD lifecycle:
1. **Checkout**: Pulls the target branch.
2. **Backend Build & JUnit**: Compiles Java 21 code and executes 180+ unit and integration tests.
3. **API Integration (Karate)**: Spawns the test environment and runs Karate API tests.
4. **Frontend Build**: Installs dependencies and runs Vite production build.
5. **Docker Build**: Builds multi-stage container images for frontend and backend.
6. **Publish & Deploy**: Conditionally pushes container images to the container registry and applies Kubernetes manifests.

### Required Jenkins Credentials:
- `DOCKER_REGISTRY_CREDENTIALS`: Username/password or token for container registry.
- `KUBECONFIG_CREDENTIALS`: Kubeconfig secret file for cluster deployment.

---

## 13. Troubleshooting & Limitations

- **Port Conflict (8081 or 5432 in use)**: Adjust `SERVER_PORT` or `POSTGRES_PORT` in your `.env` file.
- **CORS Errors**: The backend CORS configuration allows `http://localhost:*` and `http://127.0.0.1:*`. For custom domain deployments, configure `CORS_ALLOWED_ORIGINS`.
- **Sole Administrator Protection**: The system prevents demoting or deactivating the last active administrator to avoid administrative lockouts.
- **Negative Inventory Prevention**: Direct stock tampering is blocked; all stock mutations must pass through audited transactions (`/api/v1/purchases/*/receive`, `/api/v1/sales`, or `/api/v1/inventory/adjustments`).