CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) UNIQUE NOT NULL,
    balance DECIMAL(18, 2) NOT NULL CHECK (balance >= 0),
    status VARCHART(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    source_account_id BIGINT NOT NULL REFERENCES accounts(id),
    destination_account_id BIGINT NOT NULL REFERENCES accounts(id),
    amount DECIMAL(18, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
);

CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(18, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
);

INSERT INTO accounts (id, account_number, balance, status) VALUES
(1001, 'ACC1234567890', 1000.00, 'ACTIVE'),
(1002, 'ACC0987654321', 500.00, 'FROZEN');