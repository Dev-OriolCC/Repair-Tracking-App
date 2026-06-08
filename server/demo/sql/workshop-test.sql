-- =============================================================
--  Workshop Database Schema
--  Engine  : MySQL 9.3.0
--  Charset : utf8mb4 / utf8mb4_unicode_ci
--  Created : 2026-04-14
-- =============================================================

CREATE DATABASE IF NOT EXISTS workshop_db_test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE workshop_db_test;

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
               'REEL_REPAIR',
               'ROD_REPAIR',
               'MAINTENANCE',
               'OTHER'
             )                NOT NULL DEFAULT 'OTHER',
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
                   'PENDING',
                   'IN_PROGRESS',
                   'READY',
                   'COMPLETED'
                 )                NOT NULL DEFAULT 'PENDING',
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
                    'CASH',
                    'TRANSFER',
                    'CARD'
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
                   'CASH',
                   'TRANSFER',
                   'CARD'
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
    status         = IF((total - (amount_paid + NEW.amount)) <= 0, 'COMPLETED', status)
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

-- Data for Testing
DELIMITER //
create procedure set_known_good_state()
begin

    -- =========================================================
    --  1. CLEAN — delete in reverse FK-dependency order
    -- =========================================================
    DELETE FROM installment_payments;
    DELETE FROM installments;
    DELETE FROM repair_order_payments;
    DELETE FROM repair_order_items;
    DELETE FROM repair_orders;
    DELETE FROM services;
    DELETE FROM clients;
    DELETE FROM users;
    DELETE FROM roles;

    -- =========================================================
    --  2. RESET auto-increment counters
    -- =========================================================
    ALTER TABLE roles                  AUTO_INCREMENT = 1;
    ALTER TABLE users                  AUTO_INCREMENT = 1;
    ALTER TABLE clients                AUTO_INCREMENT = 1;
    ALTER TABLE services               AUTO_INCREMENT = 1;
    ALTER TABLE repair_orders          AUTO_INCREMENT = 1;
    ALTER TABLE repair_order_items     AUTO_INCREMENT = 1;
    ALTER TABLE repair_order_payments  AUTO_INCREMENT = 1;
    ALTER TABLE installments           AUTO_INCREMENT = 1;
    ALTER TABLE installment_payments   AUTO_INCREMENT = 1;

    -- =========================================================
    --  3. SEED — deterministic test data
    -- =========================================================

    -- ---------------------------------------------------------
    --  roles  (2 rows)
    -- ---------------------------------------------------------
    INSERT INTO roles (id, role_name) VALUES
        (1, 'ADMIN'),
        (2, 'SUPERADMIN');

    -- ---------------------------------------------------------
    --  users  (3 rows)
    --  Passwords are bcrypt hashes of: Password1!, Password2!, Password3!
    -- ---------------------------------------------------------
    INSERT INTO users (id, role_id, name, email, password, phone, created_at) VALUES
        (1, 2, 'Carlos Mendoza',  'carlos@workshop.test',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+525551000001', '2026-01-10 08:00:00'),
        (2, 1, 'Ana Torres',      'ana@workshop.test',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+525551000002', '2026-01-15 09:30:00'),
        (3, 1, 'Luis Ramirez',    'luis@workshop.test',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '+525551000003', '2026-02-01 10:00:00');

    -- ---------------------------------------------------------
    --  clients  (5 rows)
    -- ---------------------------------------------------------
    INSERT INTO clients (id, name, alias, phone, email, comment, created_at) VALUES
        (1, 'Roberto Garcia',    'Beto',    '+525559001001', 'beto@email.test',     'Frequent customer, prefers cash',         '2026-01-20 11:00:00'),
        (2, 'Maria Lopez',       'Mari',    '+525559001002', 'mari@email.test',     'Owns multiple reels',                     '2026-02-05 14:00:00'),
        (3, 'Pedro Sanchez',      NULL,     '+525559001003', 'pedro@email.test',    NULL,                                      '2026-02-18 09:00:00'),
        (4, 'Sofia Hernandez',   'Sofi',    '+525559001004',  NULL,                 'Referred by Roberto Garcia',              '2026-03-01 16:30:00'),
        (5, 'Juan Castillo',      NULL,     '+525559001005', 'juan@email.test',     'Commercial fishing client, bulk orders',  '2026-03-10 10:00:00');

    -- ---------------------------------------------------------
    --  services  (6 rows — covers every category)
    -- ---------------------------------------------------------
    INSERT INTO services (id, name, category, price, active, created_at) VALUES
        (1, 'Full Reel Overhaul',        'REEL_REPAIR',   350.00, 1, '2026-01-01 00:00:00'),
        (2, 'Reel Bearing Replacement',  'REEL_REPAIR',   180.00, 1, '2026-01-01 00:00:00'),
        (3, 'Rod Tip Repair',            'ROD_REPAIR',    250.00, 1, '2026-01-01 00:00:00'),
        (4, 'Rod Guide Wrap',            'ROD_REPAIR',    420.00, 1, '2026-01-01 00:00:00'),
        (5, 'General Cleaning & Lube',   'MAINTENANCE',   120.00, 1, '2026-01-01 00:00:00'),
        (6, 'Custom Handle Grip',        'OTHER',         300.00, 0, '2026-01-01 00:00:00');

    -- ---------------------------------------------------------
    --  repair_orders  (4 rows — one per status)
    --  Financial summaries are pre-calculated to match payments
    --  inserted below so triggers aren't needed during seeding.
    -- ---------------------------------------------------------
    INSERT INTO repair_orders (id, client_id, created_by, status, comment, total, amount_paid, pending_amount, created_at, updated_at) VALUES
        -- Order 1: pending — no payments yet
        (1, 1, 1, 'PENDING',     'Customer will drop off reel tomorrow',     530.00,   0.00, 530.00, '2026-04-01 09:00:00', '2026-04-01 09:00:00'),
        -- Order 2: in_progress — partial payment
        (2, 2, 2, 'IN_PROGRESS', 'Rod shipped from Cancun, awaiting parts',  670.00, 250.00, 420.00, '2026-04-05 11:30:00', '2026-04-06 14:00:00'),
        -- Order 3: ready — fully paid, awaiting pickup
        (3, 3, 2, 'READY',       'Client notified via WhatsApp',             120.00, 120.00,   0.00, '2026-04-10 08:00:00', '2026-04-12 17:00:00'),
        -- Order 4: completed — fully paid and delivered
        (4, 5, 1, 'COMPLETED',   'Bulk job, 2 reels + cleaning',             700.00, 700.00,   0.00, '2026-03-20 10:00:00', '2026-03-28 16:00:00');

    -- ---------------------------------------------------------
    --  repair_order_items  (7 rows)
    -- ---------------------------------------------------------
    INSERT INTO repair_order_items (id, repair_order_id, service_id, quantity, unit_price, subtotal) VALUES
        -- Order 1 items → total = 350 + 180 = 530
        (1, 1, 1, 1, 350.00, 350.00),
        (2, 1, 2, 1, 180.00, 180.00),
        -- Order 2 items → total = 250 + 420 = 670
        (3, 2, 3, 1, 250.00, 250.00),
        (4, 2, 4, 1, 420.00, 420.00),
        -- Order 3 items → total = 120
        (5, 3, 5, 1, 120.00, 120.00),
        -- Order 4 items → total = 350 + 350 = 700  (two reel overhauls)
        (6, 4, 1, 2, 350.00, 700.00);

    -- ---------------------------------------------------------
    --  repair_order_payments  (5 rows)
    -- ---------------------------------------------------------
    INSERT INTO repair_order_payments (id, repair_order_id, amount, payment_method, note, created_at) VALUES
        -- Order 2: partial payment of 250
        (1, 2, 250.00, 'TRANSFER', 'Bank transfer — down payment',          '2026-04-06 14:00:00'),
        -- Order 3: single full payment
        (2, 3, 120.00, 'CASH',     'Paid in full at pickup',                '2026-04-12 17:00:00'),
        -- Order 4: two payments totalling 700
        (3, 4, 400.00, 'CARD',     'First payment via terminal',            '2026-03-22 10:00:00'),
        (4, 4, 300.00, 'CASH',     'Balance paid at delivery',              '2026-03-28 16:00:00');

    -- ---------------------------------------------------------
    --  installments  (3 rows)
    -- ---------------------------------------------------------
    INSERT INTO installments (id, client_id, created_by, article, comment, interest_rate, total_amount, amount_paid, pending_amount, status, created_at, updated_at) VALUES
        -- Active plan, partially paid
        (1, 4, 1, 'Shimano Stella SW 8000',     'Layaway — 4 monthly payments',    5.00, 12600.00, 3200.00, 9400.00, 'ACTIVE',    '2026-03-15 11:00:00', '2026-04-15 11:00:00'),
        -- Completed plan, fully paid
        (2, 1, 2, 'Daiwa Saltiga 5000',          NULL,                              0.00,  8500.00, 8500.00,    0.00, 'COMPLETED', '2026-01-20 14:00:00', '2026-03-20 14:00:00'),
        -- Active plan, first payment only
        (3, 5, 1, 'Penn International 50VISW',   'Commercial client — net 60 terms', 3.50, 15450.00, 5000.00, 10450.00, 'ACTIVE', '2026-04-01 09:00:00', '2026-04-01 09:00:00');

    -- ---------------------------------------------------------
    --  installment_payments  (5 rows)
    -- ---------------------------------------------------------
    INSERT INTO installment_payments (id, installment_id, amount, payment_method, note, created_at) VALUES
        -- Installment 1: two payments = 3200
        (1, 1, 1600.00, 'TRANSFER', 'March payment',                        '2026-03-15 11:00:00'),
        (2, 1, 1600.00, 'TRANSFER', 'April payment',                        '2026-04-15 11:00:00'),
        -- Installment 2: two payments = 8500  (completed)
        (3, 2, 4250.00, 'CASH',     'First half',                           '2026-02-20 14:00:00'),
        (4, 2, 4250.00, 'CARD',     'Final payment — plan completed',       '2026-03-20 14:00:00'),
        -- Installment 3: one payment = 5000
        (5, 3, 5000.00, 'TRANSFER', 'Initial deposit',                      '2026-04-01 09:00:00');

end //
DELIMITER ;
