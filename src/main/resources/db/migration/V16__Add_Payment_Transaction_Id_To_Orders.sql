-- V16: Add payment_transaction_id to orders table to prevent replay attacks
ALTER TABLE orders ADD COLUMN payment_transaction_id VARCHAR(100) UNIQUE;
