CREATE TABLE IF NOT EXISTS loyalty_account (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS loyalty_transaction (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES loyalty_account(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    reference VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_loyalty_transaction_reference
    ON loyalty_transaction(reference)
    WHERE reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_loyalty_transaction_account_id
    ON loyalty_transaction(account_id);

