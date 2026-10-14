CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY, 
    fulll_name VARCHAR(255) NOT NULL,
    nik VARCHAR(16) UNIQUE NOT NULL,
    phone_number VARCHAR(15) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
);


CREATE TABLE customer_accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    account_id BIGINT NOT NULL,
    relationship_type VARCHAR(30) NOT NULL DEFAULT 'PRIMARY'
    CONSTRAINT fk_cust_acc_unique UNIQUE (customer_id, account_id)
);

CREATE TABLE account_security_settings (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT UNIQUE NOT NULL,
    is_two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_frozen BOOLEAN NOT NULL DEFAULT FALSE,
);

INSERT INTO customers (id, fulll_name, nik, phone_number, email) VALUES
(1, 'John Doe', '1234567890123456', '+1234567890', 'john.doe@example.com')

INSERT INTO customer_accounts (customer_id, account_id, relationship_type) VALUES
(1, 1001, 'PRIMARY'), (1, 1002, 'SECONDARY');

INSERT INTO account_security_settings (account_id, is_two_factor_enabled, is_frozen) VALUES
(1001, false, false), (1002, FALSE, TRUE);

