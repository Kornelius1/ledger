CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    nik VARCHAR(16) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customer_accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    account_id BIGINT NOT NULL, -- Logical Reference ke db_ledger.accounts
    relationship_type VARCHAR(30) NOT NULL DEFAULT 'PRIMARY',
    CONSTRAINT fk_cust_acc_unique UNIQUE (customer_id, account_id)
);

CREATE TABLE account_security_settings (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT UNIQUE NOT NULL, -- Logical Reference ke db_ledger.accounts
    is_two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_frozen BOOLEAN NOT NULL DEFAULT FALSE
);

-- Seed Data Awal untuk Testing
INSERT INTO customers (id, full_name, nik, phone_number, email) 
VALUES (1001, 'Kornelius Manik', '1234567890123456', '081234567890', 'kornelius@bank.com');

INSERT INTO customer_accounts (customer_id, account_id, relationship_type) 
VALUES (1001, 5001, 'PRIMARY'), (1001, 5002, 'PRIMARY');

INSERT INTO account_security_settings (account_id, is_two_factor_enabled, is_frozen) 
VALUES (5001, false, false), (5002, false, true); -- Account 5002 dalam kondisi FROZEN