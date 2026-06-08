# Code Standards

## Purpose

These standards define how code should be written for the Fishing Store Workshop App. The focus is the backend: Java 17, Spring Boot 4, MySQL 8, Spring Data JPA, and a Three-Layer Architecture. The frontend standards are intentionally brief and exist mainly to keep React/TypeScript integration consistent with the backend API.

## General Principles

- Keep modules small, focused, and named after the business concept they own.
- Prefer clear, boring code over clever code.
- Keep Repair Orders, Installments, Clients, Services, Users, Roles, and Payments as explicit domain concepts.
- Do not mix unrelated concerns in one class, service, repository, controller, or component.
- Keep business rules close to the service layer, not scattered across UI code, controllers, repositories, or SQL scripts.
- Validate data at system boundaries and again where business rules require it.
- Preserve financial and historical data. Workshop records are business records, not temporary UI state.
- Use explicit exceptions and predictable error handling instead of returning `null` for failure cases.
- Keep documentation, tests, and code aligned when domain rules change.

## Three-Layer Architecture

The backend follows a Three-Layer Architecture:

1. **API/Controller Layer**: Handles HTTP, authentication context, request DTOs, response DTOs, validation entrypoints, and status codes.
2. **Service Layer**: Owns business rules, validation, normalization, transactions, mapping, and domain exceptions.
3. **Repository/Data Layer**: Owns persistence access through Spring Data JPA repositories and JPA entities.

Rules:

- Controllers must be thin. They should call services and return responses, not implement business workflows.
- Services must not depend on controllers or frontend-specific types.
- Repositories must not contain business rules. They should expose query methods and persistence operations only.
- Entities represent persistence state and must not be exposed directly to frontend/API callers.
- DTOs define the boundary between services, controllers, and API clients.
- Domain decisions should be testable through service tests without starting a full web stack.

## Java 17 Standards

- Use Java 17 language features where they improve clarity, but keep code familiar and maintainable.
- Use `PascalCase` for classes and enums.
- Use `camelCase` for methods, fields, and local variables.
- Use `UPPER_SNAKE_CASE` for enum constants.
- Prefer constructor injection for dependencies.
- Mark dependencies as `final` where practical.
- Use `BigDecimal` for all money fields. Never use `double` or `float` for currency.
- Use `LocalDateTime` for database timestamps unless a timezone-specific requirement is introduced.
- Avoid returning `null` collections. Return empty lists instead.
- Keep methods short enough that validation, lookup, mapping, and persistence steps remain easy to follow.
- Do not swallow exceptions. Convert expected domain failures into explicit domain exceptions.

## Spring Boot 4 Standards

- Keep Spring annotations close to the layer they describe:
  - `@Service` on service implementations.
  - `@Repository` on repositories when needed.
  - `@Entity` and `@Table` on JPA entities.
  - `@RestController` on future API controllers.
- Use constructor injection. Do not use field injection.
- Use `@Transactional` on service classes or service methods that perform database work.
- Use `@Transactional(readOnly = true)` for read-only service methods.
- Keep configuration in `application.properties` or environment variables. Do not hardcode credentials.
- Use `DB_USERNAME` and `DB_PASSWORD` for local database credentials.
- Use Spring Validation for request DTO validation once controllers are added.
- Use springdoc OpenAPI annotations or configuration for public REST API documentation when endpoints are implemented.

## API and Controller Standards

REST controllers are target architecture responsibilities. When added, they must follow these rules:

- Controllers accept request DTOs and return response DTOs.
- Controllers must not return JPA entities.
- Controllers must not access repositories directly.
- Controllers must enforce authentication before mutations.
- Controllers should delegate authorization decisions to Spring Security or a service method.
- Controllers should map common domain exceptions to consistent HTTP responses.
- API responses should be predictable and documented through OpenAPI.
- Use standard HTTP status codes:
  - `200 OK` for successful reads and updates.
  - `201 Created` for successful creates.
  - `204 No Content` for successful deletes or soft deletes when no body is needed.
  - `400 Bad Request` for invalid input.
  - `401 Unauthorized` for missing or invalid authentication.
  - `403 Forbidden` for authenticated users without access.
  - `404 Not Found` for missing resources.
  - `409 Conflict` for duplicate resources or resources in use.

## Service Layer Standards

- Service interfaces live in `server/demo/src/main/java/com/workshop_app/demo/service`.
- Service implementations live in `server/demo/src/main/java/com/workshop_app/demo/service/impl`.
- Name service interfaces as `ThingService`.
- Name service implementations as `ThingServiceImpl`.
- Services own:
  - validation beyond simple DTO field checks,
  - normalization of email, phone, names, statuses, and categories,
  - duplicate checks,
  - resource lookup and not-found behavior,
  - create/update/delete rules,
  - soft delete workflows,
  - transaction boundaries,
  - mapping between entities and DTOs.
- Services should throw domain exceptions rather than returning ambiguous values.
- Services should keep money calculations explicit and based on `BigDecimal`.
- Services should check referenced IDs before saving records that depend on them.
- Services should preserve business history unless a documented correction workflow exists.

## Repository and JPA Standards

- Repositories live in `server/demo/src/main/java/com/workshop_app/demo/data/repository`.
- Name repositories as `ThingRepository`.
- Repositories extend `JpaRepository<EntityType, Long>`.
- Use Spring Data method names for simple lookups, existence checks, and filters.
- Use custom queries only when method names become unclear or inefficient.
- Repositories must not perform business validation.
- Repositories must not map entities to DTOs.
- Repositories should expose existence checks used by services to protect referential integrity.
- Avoid loading large object graphs accidentally. Use lazy relationships unless eager loading is required.

## Entity Standards

- Entities live in `server/demo/src/main/java/com/workshop_app/demo/data/entity`.
- Name entities as `ThingEntity`.
- Every entity should map explicitly to a table with `@Table(name = "...")`.
- Use `Long` IDs with `GenerationType.IDENTITY` for MySQL auto-increment keys.
- Use `@Column` to document required fields, lengths, precision, scale, and database column names when they differ from Java names.
- Use `@Enumerated(EnumType.STRING)` for enums so database values remain readable.
- Use `@PrePersist` for creation timestamps.
- Use `@PreUpdate` for update timestamps where the entity supports updates.
- Keep entity relationships aligned with database foreign keys.
- Use cascade and orphan removal only when the child record truly belongs to the parent lifecycle.
- Keep business calculations out of entities unless they are simple, local, and side-effect free.

Current entity groups:

- Users and Roles: `UserEntity`, `RoleEntity`.
- Clients: `ClientEntity`.
- Service catalog: `ServiceEntity`.
- Repair Orders: `RepairOrderEntity`, `RepairOrderItemEntity`, `RepairOrderPaymentEntity`.
- Installments: `InstallmentEntity`, `InstallmentPaymentEntity`.

## DTO Standards

- DTOs live in `server/demo/src/main/java/com/workshop_app/demo/service/dto`.
- Name DTOs as `ThingDTO`.
- DTOs should expose only the fields needed by the service/API boundary.
- DTOs should not contain persistence annotations.
- DTOs should not contain business workflows.
- Future request/response DTOs may split read and write shapes when a single DTO becomes ambiguous.
- Do not expose passwords, password hashes, internal security details, or unnecessary relational internals in DTO responses.

## Database Standards

- MySQL 8 is the target database.
- The primary database is `workshop_db`.
- Production schema lives in `server/demo/sql/workshop-prod.sql`.
- Test schema lives in `server/demo/sql/workshop-test.sql`.
- Table names use `snake_case` and plural nouns.
- Column names use `snake_case`.
- Foreign keys must be explicit and named clearly.
- Add indexes for common lookup fields such as client, creator, status, category, active state, and payment method.
- Money columns must use `DECIMAL`, not floating-point types.
- Timestamp columns should have clear creation/update semantics.
- Database constraints should protect integrity, but service validation should provide user-friendly errors first.
- Schema changes must keep entities, repositories, tests, and docs synchronized.
- Trigger behavior that updates financial summaries must be documented and tested against service expectations.

## Domain Business Rules

- Repair Orders use statuses: `PENDING`, `IN_PROGRESS`, `READY`, `COMPLETED`.
- Installments use statuses: `ACTIVE`, `COMPLETED`, `DEFAULTED`, `DELETED`.
- Payment methods are `CASH`, `TRANSFER`, and `CARD`.
- Service categories are `REEL_REPAIR`, `ROD_REPAIR`, `MAINTENANCE`, and `OTHER`.
- Repair Orders and Installments must use soft delete for user-facing delete actions.
- Installment soft delete is represented by status `DELETED`; default list and lookup workflows should exclude deleted installments unless deletion history is explicitly requested.
- Payment records are append-only business history unless a deliberate correction workflow is designed.
- Repair Order item unit prices must preserve the price at order time and must not change when the service catalog price changes later.
- Parent financial fields such as total, amount paid, total amount, and pending amount must stay synchronized with item and payment records.
- Pending amount must never be negative unless a formal overpayment policy is introduced.
- Amount paid must not exceed total amount unless a formal overpayment policy is introduced.
- Clients, services, users, and roles must not be hard-deleted when dependent business records exist.

## Auth and Security Standards

- Authentication is target architecture built on Spring Security and JWT.
- User login is based on email and password.
- Passwords must be hashed with a strong password encoder.
- Password hashes must never be returned through DTOs or logs.
- API mutations must require authentication.
- Role-aware access should use the `roles` and `users` model.
- Admin users can manage workshop records.
- Superadmin users are reserved for broader administration if user-management features are added.
- Secrets, database credentials, JWT signing keys, and tokens must come from environment variables or secure configuration.
- Never commit real credentials or production tokens.

## Validation and Exception Standards

- Validate required fields before persistence.
- Normalize emails, phone numbers, names, role names, service names, and statuses consistently.
- Validate phone numbers as international numbers when the workflow requires WhatsApp contact.
- Validate money fields as non-null and non-negative where required.
- Validate referenced IDs before saving records that depend on them.
- Use domain exceptions for expected failures:
  - `InvalidRequestException` for malformed or invalid input.
  - `ResourceNotFoundException` for missing records.
  - `DuplicateResourceException` for uniqueness conflicts.
  - `ResourceInUseException` for blocked deletes caused by dependent records.
- Exception messages should be clear enough for debugging and safe enough for API responses.
- Future controllers should translate domain exceptions into consistent error response bodies.

## Transaction Standards

- Put transaction boundaries in the service layer.
- Use read-only transactions for queries.
- Use write transactions for create, update, delete, soft delete, and payment workflows.
- Keep each transaction focused on one business operation.
- Avoid opening transactions in controllers.
- Avoid performing external network calls inside database transactions.
- Payment creation and parent summary updates must be atomic from the business perspective.
- If SQL triggers update financial summaries, service behavior and tests must account for that behavior.

## Testing Standards

- Repository tests should validate entity mappings, query methods, relationship constraints, and persistence behavior.
- Service tests should validate business rules, normalization, duplicate checks, validation failures, not-found behavior, resource-in-use behavior, and DTO mapping.
- Future controller tests should validate auth, request validation, response shape, status codes, and exception mapping.
- Test names should describe the behavior being verified.
- Tests should cover both success paths and failure paths.
- Soft delete behavior for Repair Orders and Installments must be tested when implemented.
- Payment workflows must test financial summary behavior.
- Avoid tests that depend on execution order.
- Keep test fixtures clear and close to the scenario they support.

## TypeScript and Frontend Integration Standards

- Keep frontend types aligned with backend DTOs once API integration begins.
- Avoid `any`; use explicit TypeScript types for Repair Orders, Installments, Clients, Services, Users, and Payments.
- Treat static frontend data as temporary until replaced by API-backed data.
- Validate user input in the UI for a better experience, but do not rely on frontend validation as the only protection.
- Keep API client code isolated from page components when backend integration is added.

## File Organization

- `frontend/src` — React UI routes, pages, forms, dashboard, components, static data, and shared frontend types.
- `server/demo/src/main/java/com/workshop_app/demo` — Spring Boot backend root package and application entrypoint.
- `server/demo/src/main/java/com/workshop_app/demo/data/entity` — JPA entities and persistence enums.
- `server/demo/src/main/java/com/workshop_app/demo/data/repository` — Spring Data JPA repositories.
- `server/demo/src/main/java/com/workshop_app/demo/service` — Service interfaces.
- `server/demo/src/main/java/com/workshop_app/demo/service/impl` — Service implementations and business logic.
- `server/demo/src/main/java/com/workshop_app/demo/service/dto` — DTOs used across service and future API boundaries.
- `server/demo/src/main/java/com/workshop_app/demo/service/exception` — Domain exception classes.
- `server/demo/sql` — MySQL schema, seed data, indexes, foreign keys, and trigger definitions.
- `server/demo/src/test/java/com/workshop_app/demo` — Repository and service tests.

## Review Checklist

- Does the change preserve the Three-Layer Architecture?
- Is business logic in a service rather than a controller or repository?
- Are entities hidden behind DTOs at API boundaries?
- Are money values represented with `BigDecimal` and `DECIMAL`?
- Are required fields and referenced IDs validated before persistence?
- Are domain exceptions used for expected failures?
- Are Repair Orders and Installments preserved through soft delete?
- Are payment records treated as business history?
- Are database schema, JPA entities, DTOs, services, and tests still aligned?
- Are tests added or updated for changed business rules?
