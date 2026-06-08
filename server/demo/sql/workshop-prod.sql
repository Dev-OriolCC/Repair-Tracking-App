-- =============================================================
--  Workshop Database Schema
--  Engine  : MySQL 9.3.0
--  Charset : utf8mb4 / utf8mb4_unicode_ci
--  Created : 2026-04-14
-- =============================================================

CREATE DATABASE IF NOT EXISTS workshop_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE workshop_db;

-- -------------------------------------------------------------
--  1. roles
--     Stores the two system roles: ADMIN and SUPERADMIN.
-- -------------------------------------------------------------
CREATE TABLE roles (
  id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_name VARCHAR(50)     NOT NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uq_roles_role_name (role_name)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='System roles: ADMIN, SUPERADMIN';

-- Seed default roles
INSERT INTO roles (role_name) VALUES ('ADMIN'), ('SUPERADMIN');


-- -------------------------------------------------------------
--  2. users
--     Operator accounts. Each user is assigned one role.
-- -------------------------------------------------------------
CREATE TABLE users (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_id    BIGINT UNSIGNED NOT NULL,
  name       VARCHAR(120)    NOT NULL,
  email      VARCHAR(180)    NOT NULL,
  password   VARCHAR(255)    NOT NULL          COMMENT 'Hashed password (bcrypt / argon2)',
  phone      VARCHAR(30)                       COMMENT 'E.164 or local format',
  created_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  CONSTRAINT fk_users_role
    FOREIGN KEY (role_id) REFERENCES roles (id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Operator accounts with role assignment';


-- -------------------------------------------------------------
--  3. clients
--     Customer master data shared across repair orders
--     and installment plans.
-- -------------------------------------------------------------
CREATE TABLE clients (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(120)    NOT NULL,
  alias      VARCHAR(60),
  phone      VARCHAR(30),
  email      VARCHAR(180),
  comment    TEXT,
  created_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_clients_name  (name),
  KEY idx_clients_phone (phone)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Customer master data';


-- -------------------------------------------------------------
--  4. services
--     Service catalog: reel repair, rod repair, maintenance,
--     and other. Prices are stored here as defaults; each
--     repair_order_item may override with a custom unit_price.
-- -------------------------------------------------------------
CREATE TABLE services (
  id         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  name       VARCHAR(120)     NOT NULL,
  category   ENUM(
               'reel_repair',
               'rod_repair',
               'maintenance',
               'other'
             )                NOT NULL DEFAULT 'other',
  price      DECIMAL(10,2)    NOT NULL DEFAULT 0.00,
  active     TINYINT(1)       NOT NULL DEFAULT 1  COMMENT '1 = available, 0 = retired',
  created_at TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_services_category (category),
  KEY idx_services_active   (active)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Service catalog with category and base price';


-- -------------------------------------------------------------
--  5. repair_orders
--     One order per repair job. Status drives the Kanban board.
--     Financial summary columns are kept denormalised for fast
--     reads; they must be kept in sync with repair_order_payments.
-- -------------------------------------------------------------
CREATE TABLE repair_orders (
  id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  client_id      BIGINT UNSIGNED  NOT NULL,
  created_by     BIGINT UNSIGNED  NOT NULL           COMMENT 'User who created the order',
  status         ENUM(
                   'pending',
                   'in_progress',
                   'ready',
                   'completed'
                 )                NOT NULL DEFAULT 'pending',
  comment        TEXT,
  total          DECIMAL(10,2)    NOT NULL DEFAULT 0.00,
  amount_paid    DECIMAL(10,2)    NOT NULL DEFAULT 0.00,
  pending_amount DECIMAL(10,2)    NOT NULL DEFAULT 0.00
                                  COMMENT 'total − amount_paid; updated on each payment',
  created_at     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_ro_client_id  (client_id),
  KEY idx_ro_created_by (created_by),
  KEY idx_ro_status     (status),
  CONSTRAINT fk_ro_client
    FOREIGN KEY (client_id)  REFERENCES clients (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_ro_created_by
    FOREIGN KEY (created_by) REFERENCES users (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Repair job orders — drives the Kanban board';


-- -------------------------------------------------------------
--  6. repair_order_items
--     Line items that link services to a repair order.
--     unit_price is copied from services.price at insert time
--     so historical records are unaffected by future price changes.
-- -------------------------------------------------------------
CREATE TABLE repair_order_items (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  repair_order_id BIGINT UNSIGNED NOT NULL,
  service_id      BIGINT UNSIGNED NOT NULL,
  quantity        INT UNSIGNED    NOT NULL DEFAULT 1,
  unit_price      DECIMAL(10,2)   NOT NULL           COMMENT 'Snapshot of price at order time',
  subtotal        DECIMAL(10,2)   NOT NULL           COMMENT 'quantity × unit_price',

  PRIMARY KEY (id),
  KEY idx_roi_repair_order_id (repair_order_id),
  KEY idx_roi_service_id      (service_id),
  CONSTRAINT fk_roi_repair_order
    FOREIGN KEY (repair_order_id) REFERENCES repair_orders (id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_roi_service
    FOREIGN KEY (service_id) REFERENCES services (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Line items: services attached to a repair order';


-- -------------------------------------------------------------
--  7. repair_order_payments
--     Payment records for repair orders.
--     Multiple partial payments are supported.
-- -------------------------------------------------------------
CREATE TABLE repair_order_payments (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  repair_order_id BIGINT UNSIGNED NOT NULL,
  amount          DECIMAL(10,2)   NOT NULL,
  payment_method  ENUM(
                    'cash',
                    'transfer',
                    'card'
                  )               NOT NULL,
  note            TEXT,
  created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_rop_repair_order_id (repair_order_id),
  KEY idx_rop_payment_method  (payment_method),
  CONSTRAINT fk_rop_repair_order
    FOREIGN KEY (repair_order_id) REFERENCES repair_orders (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Payments recorded against repair orders';


-- -------------------------------------------------------------
--  8. installments
--     Credit/layaway plans. A client reserves an article and
--     pays it off over time with optional interest.
--     Financial summary columns mirror the repair_orders pattern.
-- -------------------------------------------------------------
CREATE TABLE installments (
  id             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  client_id      BIGINT UNSIGNED  NOT NULL,
  created_by     BIGINT UNSIGNED  NOT NULL           COMMENT 'User who opened the plan',
  article        VARCHAR(255)     NOT NULL           COMMENT 'Name / description of the reserved item',
  comment        TEXT,
  interest_rate  DECIMAL(5,2)     NOT NULL DEFAULT 0.00
                                  COMMENT 'Annual interest rate (%)',
  total_amount   DECIMAL(10,2)    NOT NULL DEFAULT 0.00
                                  COMMENT 'Principal + interest',
  amount_paid    DECIMAL(10,2)    NOT NULL DEFAULT 0.00,
  pending_amount DECIMAL(10,2)    NOT NULL DEFAULT 0.00
                                  COMMENT 'total_amount − amount_paid',
  status         ENUM(
                 'ACTIVE',
                 'COMPLETED',
                 'DEFAULTED',
                 'DELETED'
                 )                NOT NULL DEFAULT 'ACTIVE',
  created_at     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_inst_client_id  (client_id),
  KEY idx_inst_created_by (created_by),
  KEY idx_inst_status     (status),
  CONSTRAINT fk_inst_client
    FOREIGN KEY (client_id)  REFERENCES clients (id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_inst_created_by
    FOREIGN KEY (created_by) REFERENCES users (id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Installment / layaway credit plans per client';


-- -------------------------------------------------------------
--  9. installment_payments
--     Individual payment records against an installment plan.
-- -------------------------------------------------------------
CREATE TABLE installment_payments (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  installment_id BIGINT UNSIGNED NOT NULL,
  amount         DECIMAL(10,2)   NOT NULL,
  payment_method ENUM(
                   'cash',
                   'transfer',
                   'card'
                 )               NOT NULL,
  note           TEXT,
  created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_ip_installment_id  (installment_id),
  KEY idx_ip_payment_method  (payment_method),
  CONSTRAINT fk_ip_installment
    FOREIGN KEY (installment_id) REFERENCES installments (id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Individual payments recorded against an installment plan';


-- =============================================================
--  TRIGGERS
--  Keep the denormalised financial summary columns in sync
--  automatically after every payment insert.
-- =============================================================

DELIMITER $$

-- After a repair order payment is inserted, update the parent order.
CREATE TRIGGER trg_after_rop_insert
AFTER INSERT ON repair_order_payments
FOR EACH ROW
BEGIN
  UPDATE repair_orders
  SET
    amount_paid    = amount_paid + NEW.amount,
    pending_amount = total - (amount_paid + NEW.amount),
    status         = IF((total - (amount_paid + NEW.amount)) <= 0, 'done', status)
  WHERE id = NEW.repair_order_id;
END$$

-- After an installment payment is inserted, update the parent plan.
CREATE TRIGGER trg_after_ip_insert
AFTER INSERT ON installment_payments
FOR EACH ROW
BEGIN
  UPDATE installments
  SET
    amount_paid    = amount_paid + NEW.amount,
    pending_amount = total_amount - (amount_paid + NEW.amount),
    status         = IF((total_amount - (amount_paid + NEW.amount)) <= 0, 'COMPLETED', status)
  WHERE id = NEW.installment_id;
END$$

DELIMITER ;
