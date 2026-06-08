# Fishing Store Workshop App

## Overview

Fishing Store Workshop App is a web application for a fishing store owner to manage workshop repair orders and installment payment plans from one workspace. The app focuses on the backend domain for repair tracking, payment history, customer records, and reminder workflows, supported by a clean React UI with boards, tables, charts, and history views.

The target system uses a Java/Spring Boot backend with MySQL persistence. Repair Orders and Installments are modeled as first-class business records with related clients, users, payments, statuses, timestamps, and history. Records should be preserved for reporting and audit purposes, so delete actions for Repair Orders and Installments must be implemented as soft delete instead of physical deletion.

## Goals

1. Allow the store owner or admin user to manage Repair Orders and Installments from a single web app.
2. Track client information, repair progress, installment balances, payments, and historical activity.
3. Provide WhatsApp message/reminder tools for contacting clients about order updates or pending installment payments.
4. Provide a clean dashboard with charts and summaries for repair volume, installment status, collected payments, pending balances, and recent activity.
5. Build a reliable Spring Boot backend with clear DTO, service, repository, entity, and database boundaries.

## Core User Flow

1. The admin user signs in with email and password.
2. The user lands on the Home dashboard and reviews repair order metrics, installment balances, charts, and recent activity.
3. The user navigates between Home, Repair Orders, Installments, History views, and Profile.
4. The user opens the Repair Orders board to view orders by status: Pending, In Progress, Ready, and Completed.
5. The user creates, views, edits, or soft deletes a Repair Order, including client details, selected services, line items, totals, payment records, and notes.
6. The user opens the Installments board or list to view active, completed, and defaulted installment plans.
7. The user creates, views, edits, or soft deletes an Installment, including client details, article description, interest rate, total amount, payment history, pending balance, status, and notes.
8. The user drafts or sends a WhatsApp reminder/message from a Repair Order or Installment context and can review message history.
9. The user searches history to review past orders, installment records, payments, and client activity.

## Features

### Authentication and Role-Based Access

- Admin users sign in with email and password.
- Users are assigned roles through the backend role model.
- Backend access should protect Repair Order, Installment, Client, Service, and User operations.
- User records track the operator who created Repair Orders and Installments.

### Client Records

- Store customer master data in a shared Client model.
- Track client name, alias, phone, email, comments, and creation date.
- Reuse client records across Repair Orders and Installments.
- Support lookup by name, phone, or email for faster workflow and duplicate prevention.

### Repair Order Management

- Create and manage Repair Orders linked to a client and creating user.
- Track order status using the workflow: Pending, In Progress, Ready, Completed.
- Display Repair Orders in a Kanban board for operational visibility.
- Store comments, totals, amount paid, pending amount, created date, and updated date.
- Support view, edit, history, and soft delete actions.

### Repair Order Services, Line Items, and Payments

- Maintain a service catalog for reel repair, rod repair, maintenance, and other services.
- Attach one or more service line items to a Repair Order.
- Store quantity, unit price, and subtotal per Repair Order item.
- Preserve historical pricing by storing the unit price on the order item.
- Record one or more Repair Order payments with amount, payment method, note, and created date.
- Support cash, transfer, and card payment methods.

### Installment Plan Management

- Create and manage Installments linked to a client and creating user.
- Track the article or item being financed.
- Store interest rate, total amount, amount paid, pending amount, comments, created date, and updated date.
- Track installment status using Active, Completed, Defaulted, and Deleted for soft-deleted history.
- Support view, edit, history, and soft delete actions.

### Installment Payment Tracking

- Record one or more payments against an Installment.
- Store payment amount, payment method, note, and created date.
- Update amount paid and pending amount as payments are recorded.
- Mark an Installment as completed when the pending amount reaches zero.
- Preserve payment history for financial reporting.

### WhatsApp Reminder Workspace

- Provide a message panel for Repair Orders and Installments.
- Let the user draft client messages for repair status updates or installment payment reminders.
- Store or display message history in the relevant order or installment context.
- Keep automated WhatsApp delivery outside the first scope unless OpenWA or another WhatsApp integration is added later.

### Dashboard, Charts, and History Views

- Show total Repair Orders, total Installments, repair revenue, installment value, paid amounts, and open balances.
- Display charts for trends, status distribution, and payment progress.
- Show recent activity across Repair Orders and Installments.
- Provide Repair Order and Installment history views for search and review.

### Backend Persistence and Database Integrity

- Use Spring Boot with Java 17 for the backend application.
- Use Spring Data JPA repositories for persistence access.
- Use MySQL as the primary relational database.
- Keep business rules in service classes and expose clean DTOs between service/API layers.
- Maintain database relationships with foreign keys between users, roles, clients, services, repair orders, repair order items, repair order payments, installments, and installment payments.
- Prefer soft delete for business records that must remain visible in history, reporting, and audit workflows.

## Backend Architecture Summary

The backend is organized around a standard Spring Boot layered architecture:

- **Entities**: JPA entities represent database tables and relationships.
- **Repositories**: Spring Data JPA repositories provide persistence operations and query methods.
- **Services**: Service interfaces and implementations contain validation, normalization, lookup, create, update, delete, and business logic.
- **DTOs**: Data Transfer Objects define the shape of data moving through the service and API boundary.
- **Exceptions**: Domain-specific exceptions describe invalid requests, duplicates, resources not found, and resources in use.
- **SQL Schema**: MySQL schema files define production/test tables, relationships, indexes, seed data, and payment summary behavior.
- **REST API Intent**: The backend should expose OpenAPI-documented REST endpoints for authentication, clients, users, services, repair orders, repair order payments, installments, and installment payments.

## Main Data Model

### Users and Roles

- `roles` stores system roles such as Admin and Superadmin.
- `users` stores operator accounts with role, name, email, password, phone, and creation date.
- Repair Orders and Installments reference the user who created them.

### Clients

- `clients` stores customer master data.
- Clients can be referenced by multiple Repair Orders and multiple Installments.
- Client deletion should be restricted when records depend on the client.

### Services

- `services` stores the repair service catalog.
- Service categories include reel repair, rod repair, maintenance, and other.
- Services include active/inactive state and a default price.
- Repair Order items reference services while preserving historical item pricing.

### Repair Orders

- `repair_orders` stores the parent repair job record.
- Each Repair Order belongs to a client and a creating user.
- Each Repair Order can have many `repair_order_items`.
- Each Repair Order can have many `repair_order_payments`.
- Financial summary fields include total, amount paid, and pending amount.
- Status drives the Repair Orders board.

### Installments

- `installments` stores layaway or credit payment plans.
- Each Installment belongs to a client and a creating user.
- Each Installment can have many `installment_payments`.
- Financial summary fields include total amount, amount paid, and pending amount.
- Status tracks whether the plan is active, completed, defaulted, or soft-deleted.

## Scope

### In Scope

- A web app for managing Repair Orders and Installments.
- Admin login with role-aware backend access.
- Client records shared by Repair Orders and Installments.
- Repair Order CRUD with soft delete, service line items, status tracking, comments, payments, and history.
- Installment CRUD with soft delete, article details, interest, status tracking, comments, payments, and history.
- Service catalog for repair order line items.
- Payment tracking for Repair Orders and Installments.
- Dashboard metrics, charts, recent activity, and history/search views.
- WhatsApp message/reminder workspace and message history.
- Spring Boot backend with JPA entities, repositories, services, DTOs, validation, exceptions, MySQL schema, and OpenAPI-ready REST endpoints.

### Out of Scope

- Admin Profile UI for creating and managing additional users.
- Full CRM for inventory products or customer marketing.
- Full point-of-sale checkout software.
- Email integration for reminders.
- Automated WhatsApp delivery beyond the message/reminder workspace unless a future WhatsApp integration is added.
- Inventory purchasing, stock management, barcode scanning, or supplier management.
- Accounting system integration.

## Success Criteria

1. A signed-in admin user can create, view, update, and soft delete a Repair Order.
2. A signed-in admin user can create, view, update, and soft delete an Installment.
3. Repair Orders preserve client, creator, status, service line items, totals, payments, pending balance, comments, and history.
4. Installments preserve client, creator, article, interest rate, total amount, payments, pending balance, status, comments, and history.
5. Payment records can be added to Repair Orders and Installments without losing historical payment details.
6. Dashboard views summarize order counts, installment counts, revenue, payments collected, pending balances, and status distribution.
7. History views allow the user to review previous Repair Orders and Installments, including soft-deleted records when appropriate.
8. The user can draft or send a WhatsApp message/reminder to a client from a Repair Order or Installment context.
9. Backend validation prevents invalid clients, services, users, payment amounts, statuses, and required fields from being saved.
10. Database relationships protect referential integrity across users, roles, clients, services, repair records, installment records, and payment records.
