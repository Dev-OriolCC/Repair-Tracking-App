# Progress Tracker

Update this file after every meaningful implementation
change.

## Current Phase

- Complete

## Current Goal

- RepairOrder, RepairOrderItem, and RepairOrderPayment controller
  endpoints implemented.

## Completed

- Completed Data/Service layer for Client, Installment,
  InstallmentPayment, RepairOrderItem, RepairOrder, RepairOrderPayment,
  Role, Service, User

## In Progress

- None yet.

## Next Up

- Add the next feature unit from `context/feature-specs`.

## Open Questions

- [Any unresolved product or technical decisions]

## Architecture Decisions

- [Decisions made that affect the system design or
  data model — include why the decision was made]

## Session Notes

- Implemented RepairOrderController, RepairOrderItemController, and
  RepairOrderPaymentController endpoints using their existing service
  methods.
- Added Swagger/OpenAPI annotations for all three repair controller
  groups.
- Verified with `.\mvnw.cmd -DskipTests compile`.
- Full `.\mvnw.cmd test` still fails before repository tests complete
  because MySQL credentials are unavailable (`Access denied for user
  '${DB_USERNAME}'...`).
- Started repair controller implementation from feature spec 05.
- Implemented UserController, ClientController, and ServiceController
  endpoints using their existing service methods.
- Added ErrorResponseDTO and GlobalExceptionHandler with
  generalExceptionHandler, forbiddenException, and ioException methods.
- Removed RoleController-scoped exception handlers so RoleController uses
  the global handler too.
- Added Swagger/OpenAPI annotations for User, Client, and Service
  controllers.
- Verified with `.\mvnw.cmd -DskipTests compile`.
- Full `.\mvnw.cmd test` still fails before repository tests complete
  because MySQL credentials are unavailable (`Access denied for user
  '${DB_USERNAME}'...`).
- Started User, Client, and Service controller implementation from
  feature spec 04.
- Implemented RoleController endpoints for list, id lookup, name lookup,
  name existence, create, update, and delete using RoleService methods.
- Added Swagger/OpenAPI annotations and RoleController-scoped domain
  exception mapping for invalid requests, not found, duplicates, and
  resource-in-use conflicts.
- Verified with `.\mvnw.cmd -DskipTests compile`.
- Full `.\mvnw.cmd test` still fails before repository tests complete
  because MySQL credentials are unavailable (`Access denied for user
  '${DB_USERNAME}'...`).
- Started RoleController implementation from feature spec 03.
- Implemented InstallmentDTO, InstallmentService,
  InstallmentServiceImpl, InstallmentPaymentDTO,
  InstallmentPaymentService, InstallmentPaymentServiceImpl,
  repository lookup helpers, `DELETED` installment status soft delete,
  SQL enum updates, context documentation updates, and focused service
  tests.
- Verified with `.\mvnw.cmd -Dtest=InstallmentServiceTest test`,
  `.\mvnw.cmd -Dtest=InstallmentPaymentServiceTest test`, and
  `.\mvnw.cmd "-Dtest=InstallmentServiceTest,InstallmentPaymentServiceTest,ClientServiceTest,UserServiceTest" test`.
- Full `.\mvnw.cmd test` still fails before repository tests complete
  because the MySQL connection is unavailable (`Communications link
  failure`).
- Started Installment and InstallmentPayment service-layer implementation
  from feature spec 02.
- Implemented RepairOrderPaymentDTO, RepairOrderPaymentService,
  RepairOrderPaymentServiceImpl, repository lookup helpers, and focused
  service tests.
- Verified with `.\mvnw.cmd -Dtest=RepairOrderPaymentServiceTest test`
  and `.\mvnw.cmd "-Dtest=RepairOrderPaymentServiceTest,RepairOrderItemServiceTest,RepairOrderServiceTest" test`.
- Full `.\mvnw.cmd test` currently fails before repository tests complete
  because the MySQL connection is unavailable (`Communications link
  failure`).
