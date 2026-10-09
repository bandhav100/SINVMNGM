# Smart Inventory Management System (SIMS)

A full-stack, role-based inventory management application designed to help businesses manage products, monitor stock levels, track purchases and sales, and make smarter inventory decisions.

This project is being developed to learn and apply **full-stack development, automated testing, and DevOps practices** through a practical software engineering project.

**Project Status:** Planning and initial setup

## Project Objectives

- Manage products, categories, suppliers, and inventory.
- Track stock movements caused by purchases and sales.
- Detect low-stock and out-of-stock products.
- Generate inventory, purchase, and sales reports.
- Provide analytics and demand forecasting.
- Implement role-based access control.
- Automate builds, tests, and deployment using Jenkins.
- Containerize the application using Docker.

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | React |
| Backend | Java, Spring Boot |
| API | REST API |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Backend Unit Testing | JUnit |
| API Testing | Karate |
| Build Tool | Maven |
| API Documentation | Swagger / OpenAPI |
| Version Control | Git and GitHub |
| CI/CD | Jenkins |
| Containerization | Docker and Docker Compose |
| Database Migrations | Flyway |
| Application Monitoring | Spring Boot Actuator |

## Application Modules

1. **Dashboard** — Display inventory statistics and important alerts.
2. **Product Management** — Create, view, update, and manage products.
3. **Inventory Management** — Monitor stock quantities and stock movements.
4. **Purchase Management** — Create purchase orders and record received stock.
5. **Sales Management** — Record sales and update inventory.
6. **Supplier Management** — Maintain supplier information and purchase history.
7. **Smart Insights** — Identify stock risks, analyze trends, and recommend reorder quantities.
8. **Alerts** — Notify users about low stock and other inventory issues.
9. **Reports** — Generate inventory, sales, and purchase reports.
10. **User and Role Management** — Manage users and permissions.
11. **Settings** — Configure application preferences.

The application will include login and authentication, with dashboards and access permissions based on the user's role.

## Role-Based Access Control

The initial design includes four roles:

| Role | Primary Responsibility |
|---|---|
| Admin | Manage users, permissions, settings, and overall operations |
| Inventory Manager | Manage products, monitor stock, and handle inventory adjustments |
| Purchase Manager | Manage suppliers, purchase orders, and stock replenishment |
| Sales Staff | Record sales and view relevant product and stock information |

Permissions will be enforced in the backend using Spring Security, rather than relying only on frontend navigation restrictions.

## System Architecture

```text
                 User
                  |
                  v
             React Frontend
                  |
                  v
            REST API Requests
                  |
                  v
          Spring Boot Backend
                  |
          +-------+--------+
          |                |
          v                v
     PostgreSQL       Business Logic
                           |
                   +-------+--------+
                   |                |
                   v                v
              Stock Alerts     Smart Analytics
```

## DevOps and CI/CD Workflow

The project will follow a version-controlled development and delivery process.

```text
Developer
    |
    v
Git Feature Branch
    |
    v
GitHub Pull Request
    |
    v
Jenkins Pipeline
    |
    v
Build and Unit Tests
    |
    v
Karate API Tests
    |
    v
Frontend Build
    |
    v
Docker Image Build
    |
    v
Deployment and Health Checks
```

The exact pipeline stages will be implemented and refined as the application develops.

## What We Aim to Learn

### Full-Stack Development
- React components, routing, forms, and API integration.
- Java and Spring Boot application development.
- REST API design and HTTP methods.
- PostgreSQL, SQL, database relationships, and transactions.
- Spring Data JPA and Hibernate.
- Authentication and role-based authorization.

### Software Testing
- Unit testing with JUnit.
- REST API automation with Karate.
- Positive and negative test cases.
- Integration testing and database validation.
- Automated test execution through Jenkins.

### DevOps
- Git branching, commits, merges, and pull requests.
- GitHub repository management and collaboration.
- Jenkins pipelines and Pipeline-as-Code.
- Maven build automation.
- Docker images, containers, and Docker Compose.
- CI/CD, environment configuration, and deployment.
- Database migrations and version control.
- Application health checks, logging, and monitoring.
- Secure management of environment variables and credentials.

### Smart Inventory Features
- Stock threshold calculations and alerts.
- Inventory and sales analytics.
- Historical sales data processing.
- Demand forecasting and reorder recommendations.

## Development Roadmap

- [ ] Initialize the GitHub repository and documentation.
- [ ] Establish the project directory structure.
- [ ] Create the Spring Boot backend.
- [ ] Set up PostgreSQL and database migrations.
- [ ] Create the React frontend.
- [ ] Implement product and category management.
- [ ] Implement inventory and stock movements.
- [ ] Implement purchases, suppliers, and sales.
- [ ] Implement authentication and role-based access.
- [ ] Add inventory alerts, analytics, and reports.
- [ ] Add demand forecasting and reorder recommendations.
- [ ] Write JUnit and Karate tests.
- [ ] Containerize the application using Docker.
- [ ] Configure the Jenkins CI/CD pipeline.
- [ ] Add deployment health checks and monitoring.
- [ ] Document the final architecture and testing results.

## Getting Started

Development setup instructions will be added as the project environment is established.

The intended local development environment includes Java, Maven, Node.js, npm, Git, and Docker Desktop.

## Project Principles

- Build features incrementally.
- Keep application code and infrastructure configuration version-controlled.
- Test business logic and APIs continuously.
- Never commit passwords, API keys, or other secrets.
- Use reproducible builds and database migrations.
- Deploy only after the required checks pass.
- Document architectural decisions and implementation progress.

## Future Enhancements

- Advanced demand forecasting using historical sales datasets.
- Automated reorder recommendations.
- Multi-warehouse inventory management.
- Barcode or QR-code-based stock operations.
- Enhanced monitoring and deployment automation.

---

**Project Focus:** Full-Stack Development + Software Testing + DevOps + Smart Inventory Analytics.
