DROP DATABASE IF EXISTS payment_processing_db;
CREATE DATABASE payment_processing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE payment_processing_db;

CREATE TABLE payments
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key     VARCHAR(100)   NOT NULL UNIQUE,
    source_account      VARCHAR(50)    NOT NULL,
    destination_account VARCHAR(50)    NOT NULL,
    amount              DECIMAL(15, 2) NOT NULL,
    currency            VARCHAR(10)    NOT NULL,
    reference           VARCHAR(255),
    status              VARCHAR(20)    NOT NULL,
    error_code          VARCHAR(50),
    error_message       VARCHAR(500),
    created_at          DATETIME       NOT NULL,
    updated_at          DATETIME       NOT NULL,
    version             BIGINT DEFAULT 0,
    INDEX idx_payments_status (status),
    INDEX idx_payments_created_at (created_at)
);

CREATE TABLE payment_status_history
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id      BIGINT      NOT NULL,
    previous_status VARCHAR(20),
    new_status      VARCHAR(20) NOT NULL,
    reason          VARCHAR(500),
    triggered_by    VARCHAR(100),
    changed_at      DATETIME    NOT NULL,
    INDEX idx_history_payment_id (payment_id),
    CONSTRAINT fk_history_payment
        FOREIGN KEY (payment_id)
            REFERENCES payments (id)
            ON DELETE CASCADE
);

CREATE TABLE accounts
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,

    account_number VARCHAR(50)    NOT NULL,
    account_name   VARCHAR(100)   NOT NULL,

    currency       VARCHAR(10)    NOT NULL,
    balance        DECIMAL(15, 2) NOT NULL,

    status         VARCHAR(20)    NOT NULL,

    created_at     DATETIME       NOT NULL,
    updated_at     DATETIME       NOT NULL,

    version        BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT chk_accounts_currency CHECK (currency IN ('CNY', 'GBP', 'EUR', 'USD'))
);

INSERT INTO payments
(id, idempotency_key, source_account, destination_account, amount, currency, reference, status, error_code,
 error_message, created_at, updated_at, version)
VALUES (1, 'PAY-SEED-001', 'ACC-001', 'ACC-002', 250.00, 'GBP', 'Seed payment - created only', 'CREATED', NULL, NULL,
        '2026-07-28 09:00:00', '2026-07-28 09:00:00', 0),
       (2, 'PAY-SEED-002', 'ACC-003', 'ACC-004', 500.50, 'EUR', 'Seed payment - validated', 'VALIDATED', NULL, NULL,
        '2026-07-28 09:10:00', '2026-07-28 09:12:00', 0),
       (3, 'PAY-SEED-003', 'ACC-005', 'ACC-006', 999.99, 'USD', 'Seed payment - completed', 'COMPLETED', NULL, NULL,
        '2026-07-28 09:20:00', '2026-07-28 09:25:00', 0),
       (4, 'PAY-SEED-004', 'ACC-999', 'ACC-007', 100.00, 'GBP', 'Seed payment - failed insufficient funds', 'FAILED',
        'INSUFFICIENT_FUNDS', 'Source account ACC-999 has insufficient funds', '2026-07-28 09:30:00',
        '2026-07-28 09:31:00', 0);

INSERT INTO payment_status_history
(payment_id, previous_status, new_status, reason, triggered_by, changed_at)
VALUES (1, NULL, 'CREATED', 'Payment created', 'SYSTEM', '2026-07-28 09:00:00'),

       (2, NULL, 'CREATED', 'Payment created', 'SYSTEM', '2026-07-28 09:10:00'),
       (2, 'CREATED', 'VALIDATED', 'Payment passed validation', 'SYSTEM', '2026-07-28 09:12:00'),

       (3, NULL, 'CREATED', 'Payment created', 'SYSTEM', '2026-07-28 09:20:00'),
       (3, 'CREATED', 'VALIDATED', 'Payment passed validation', 'SYSTEM', '2026-07-28 09:21:00'),
       (3, 'VALIDATED', 'SENT', 'Payment sent to simulated payment network', 'SYSTEM', '2026-07-28 09:23:00'),
       (3, 'SENT', 'COMPLETED', 'Payment successfully completed', 'SYSTEM', '2026-07-28 09:25:00'),

       (4, NULL, 'CREATED', 'Payment created', 'SYSTEM', '2026-07-28 09:30:00'),
       (4, 'CREATED', 'FAILED', 'Validation failed: Source account ACC-999 has insufficient funds', 'SYSTEM',
        '2026-07-28 09:31:00');

INSERT INTO accounts (account_number,
                      account_name,
                      currency,
                      balance,
                      status,
                      created_at,
                      updated_at,
                      version)
VALUES ('ACC-001', 'Alice Current Account', 'CNY', 10000.00, 'ACTIVE', NOW(), NOW(), 0),
       ('ACC-002', 'Bob Current Account', 'CNY', 5000.00, 'ACTIVE', NOW(), NOW(), 0),
       ('ACC-003', 'Company Main Account', 'CNY', 100000.00, 'ACTIVE', NOW(), NOW(), 0),
       ('ACC-004', 'Low Balance Account', 'CNY', 50.00, 'ACTIVE', NOW(), NOW(), 0),
       ('ACC-005', 'Closed Account', 'CNY', 3000.00, 'CLOSED', NOW(), NOW(), 0),

       ('ACC-USD-001', 'USD Test Account A', 'USD', 2000.00, 'ACTIVE', NOW(), NOW(), 0),
       ('ACC-USD-002', 'USD Test Account B', 'USD', 800.00, 'ACTIVE', NOW(), NOW(), 0),

       ('ACC-EUR-001', 'EUR Test Account A', 'EUR', 1500.00, 'ACTIVE', NOW(), NOW(), 0),
       ('ACC-GBP-001', 'GBP Test Account A', 'GBP', 1200.00, 'ACTIVE', NOW(), NOW(), 0);
