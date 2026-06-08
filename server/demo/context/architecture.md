# Architecture Context

## Purpose

This document describes the target architecture for the Fishing Store Workshop App, with a heavy focus on the Java/Spring Boot backend, MySQL database model, and service-layer boundaries. The current codebase already contains the backend entity, repository, service, DTO, exception, SQL, and test foundations. REST controllers, JWT security wiring, and full frontend API integration are target architecture responsibilities to complete as the application moves from static UI data to backend-backed workflows.

## Stack

| Layer | Technology | Role |
| --- | --- | --- |
| Frontend Framework | React 19 + TypeScript + Vite | Browser UI for dashboard, boards, forms, history views, and navigation |
| Frontend Styling | Tailwind CSS + shadcn/ui-style components | Shared UI primitives, layouts, forms, tables, panels, and responsive styling |
| Frontend Routing | React Router | Client-side routes for Home, Login, Repair Orders, Installments, and history pages |
| Backend Framework | Java 17 + Spring Boot 4 | Main backend application runtime |
| Web/API | Spring Web MVC + springdoc OpenAPI | Target REST API layer and generated API documentation |
| Persistence | Spring Data JPA + Spring JDBC | Repository access and relational database integration |
| Validation | Spring Validation | Request and service-layer validation rules |
| Security | Spring Security + JWT | Target email/password login, authenticated API access, and role-aware authorization |
| Database | MySQL 8 / `workshop_db` | Primary relational data store for users, clients, repair orders, installments, services, and payments |
| Boilerplate Reduction | Lombok | Entity and DTO accessor/constructor generation |
| Testing | JUnit, Mockito, Spring Boot test dependencies | Repository and service tests |

## System Boundaries

- `frontend/src` owns the React user interface, routes, pages, forms, dashboard widgets, navigation, shared components, and temporary static data used before API integration.
- `server/demo/src/main/java/com/workshop_app/demo` owns the Spring Boot backend application entrypoint and backend package root.
- `server/demo/src/main/java/com/workshop_app/demo/data/entity` owns JPA entity classes that map Java domain objects to MySQL tables.
- `server/demo/src/main/java/com/workshop_app/demo/data/repository` owns Spring Data JPA repositories and database query methods.
- `server/demo/src/main/java/com/workshop_app/demo/service` owns service interfaces that define backend business operations.
- `server/demo/src/main/java/com/workshop_app/demo/service/impl` owns service implementations, validation, normalization, lookup, create, update, delete, and business rules.
- `server/demo/src/main/java/com/workshop_app/demo/service/dto` owns Data Transfer Objects used across service and future REST API boundaries.
- `server/demo/src/main/java/com/workshop_app/demo/service/exception` owns domain-specific exceptions such as invalid request, duplicate resource, resource not found, and resource in use.
- `server/demo/sql` owns MySQL production/test schema files, seed data, indexes, constraints, and trigger definitions.
- `server/demo/src/test/java/com/workshop_app/demo` owns repository and service tests using Spring Boot test support, JUnit, and Mockito.

## Storage Model

- **Primary database**: MySQL database named `workshop_db`.
- **Production schema**: `server/demo/sql/workshop-prod.sql`.
- **Testing schema**: `server/demo/sql/workshop-test.sql`.
- **Spring connection config**: `server/demo/src/main/resources/application.properties`, using `DB_USERNAME` and `DB_PASSWORD` environment variables.

The database stores the main business records:

- `roles`: system roles such as Admin and Superadmin.
- `users`: operator accounts with role, name, email, password, phone, and creation timestamp.
- `clients`: customer master data shared across Repair Orders and Installments.
- `services`: repair service catalog with category, price, active state, and creation timestamp.
- `repair_orders`: parent repair job records with client, creator, status, comments, totals, payment summary fields, and timestamps.
- `repair_order_items`: line items that connect services to Repair Orders while preserving quantity, unit price, and subtotal.
- `repair_order_payments`: payment history for Repair Orders.
- `installments`: layaway or credit payment plans with client, creator, article, interest rate, totals, payment summary fields, status, and timestamps.
- `installment_payments`: payment history for Installments.

The schema uses foreign keys and indexes to protect relationships and support lookup by common workflow fields such as client, creator, status, category, active state, and payment method. Payment summary columns, such as `amount_paid` and `pending_amount`, exist on parent Repair Order and Installment records for fast dashboard and board reads. SQL triggers currently describe automatic summary updates after payment inserts.

## Auth and Access Model

- Users authenticate with email and password backed by the `users` table.
- Passwords must be stored as hashes, never plaintext.
- Spring Security is the target security layer for authentication and authorization.
- JWT is the target session model for stateless frontend-to-backend API calls.
- Roles are stored in the `roles` table and linked to users through `role_id`.
- Admin users can manage Repair Orders, Installments, Clients, Services, and payment records.
- Superadmin users are reserved for broader backend administration if future user-management functionality is added.
- Repair Orders and Installments store `created_by` so the backend preserves who opened each business record.
- The Admin Profile UI for creating or managing additional users remains out of scope unless added later.

## Backend Domain Model

### Users and Roles

- `RoleEntity` maps system roles.
- `UserEntity` maps operator accounts.
- `UserRepository`, `RoleRepository`, `UserService`, and `RoleService` own persistence and business logic for these records.
- Users are referenced by Repair Orders and Installments through the creator relationship.

### Clients

- `ClientEntity` maps customer master data.
- `ClientRepository` and `ClientService` own client lookup, validation, creation, update, and deletion rules.
- Clients can be linked to both Repair Orders and Installments.
- Client deletion must be restricted when active or historical business records depend on the client.

### Service Catalog

- `ServiceEntity` maps repair services.
- Service categories are `REEL_REPAIR`, `ROD_REPAIR`, `MAINTENANCE`, and `OTHER`.
- Services have a default price and active flag.
- Repair Order items reference services while copying the unit price into the item for historical pricing.

### Repair Orders

- `RepairOrderEntity` is the parent repair job record.
- Status values are `PENDING`, `IN_PROGRESS`, `READY`, and `COMPLETED`.
- Each Repair Order belongs to one client and one creator user.
- Each Repair Order can have many `RepairOrderItemEntity` records and many `RepairOrderPaymentEntity` records.
- Payment methods are `CASH`, `TRANSFER`, and `CARD`.
- Repair Orders must use soft delete in the target architecture so history, payments, and reporting remain intact.

### Installments

- `InstallmentEntity` is the parent layaway or credit plan record.
- Status values are `ACTIVE`, `COMPLETED`, `DEFAULTED`, and `DELETED`.
- Each Installment belongs to one client and one creator user.
- Each Installment can have many `InstallmentPaymentEntity` records.
- Payment methods are `CASH`, `TRANSFER`, and `CARD`.
- Installments use status `DELETED` for soft delete so financial history and reporting remain intact.

## API Boundary

The target backend API should expose thin Spring Web MVC controllers documented through springdoc OpenAPI. Controllers should translate HTTP requests into DTOs, call service methods, and return DTO responses. Controllers should not contain business logic.

Planned API areas:

- Authentication: login, token refresh if needed, current user/profile.
- Users and roles: backend-managed user/role lookup and administration endpoints as required.
- Clients: create, find, update, and delete with referential integrity checks.
- Services: service catalog lookup, create, update, activate/deactivate, and delete when safe.
- Repair Orders: board/list/history lookup, create, update, soft delete, status changes, line item handling, and payment handling.
- Installments: list/history lookup, create, update, soft delete, status changes, and payment handling.
- WhatsApp messages: message draft/history endpoints if persistence or external delivery is added.

## Invariants

1. Business rules belong in service implementations, not in controllers or repositories.
2. DTOs define the service/API boundary; frontend requests should not bind directly to JPA entities.
3. Repair Orders and Installments must be soft deleted, not physically deleted, when user-facing delete actions are performed.
4. Payment records are append-only business history unless a deliberate correction workflow is designed.
5. Parent financial summary fields must stay synchronized with payment records.
6. Client, service, and user deletion must respect referential integrity and fail safely when dependent records exist.
7. Repair Order item unit prices must preserve historical pricing and must not change just because the service catalog price changes later.
8. Required fields, enum values, payment amounts, totals, phone/email formats, and referenced IDs must be validated before persistence.
9. Controllers, when added, must remain thin REST/OpenAPI entrypoints over the service layer.
10. Frontend static data should be treated as temporary and replaced with API-backed data as endpoints become available.

## Testing Strategy

- Repository tests validate JPA mappings, query methods, persistence behavior, and database relationship expectations.
- Service tests validate normalization, validation, duplicate checks, create/update/delete behavior, and domain exceptions.
- Future controller tests should validate authentication, authorization, request validation, response shape, and HTTP status mapping.
- Soft delete behavior for Repair Orders and Installments must be covered once the target deletion model is implemented.
- Payment workflows must be tested to confirm amount paid, pending amount, and completed status behavior stay consistent.
