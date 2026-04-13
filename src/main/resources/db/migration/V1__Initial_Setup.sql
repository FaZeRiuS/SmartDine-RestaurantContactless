-- Flyway Initial Setup: Full Database Schema (Hardened with Constraints)
-- This script creates all tables for the SmartDine application with strict data integrity

-- 1. Menus
CREATE TABLE IF NOT EXISTS menu (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_time TIME WITHOUT TIME ZONE,
    end_time TIME WITHOUT TIME ZONE
);

-- 2. Dishes
CREATE TABLE IF NOT EXISTS dish (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price REAL NOT NULL CHECK (price > 0),
    image_url VARCHAR(512),
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. Dish to Menus (ManyToMany)
CREATE TABLE IF NOT EXISTS dish_menus (
    dish_id INT NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    menu_id INT NOT NULL REFERENCES menu(id) ON DELETE CASCADE,
    PRIMARY KEY (dish_id, menu_id)
);

-- 4. Dish Tags (ElementCollection)
CREATE TABLE IF NOT EXISTS dish_tags (
    dish_id INT NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL
);

-- 5. Orders
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    total_price REAL NOT NULL DEFAULT 0 CHECK (total_price >= 0),
    table_number INT CHECK (table_number > 0),
    loyalty_discount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (loyalty_discount >= 0),
    loyalty_points_spent NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (loyalty_points_spent >= 0),
    tip_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (tip_amount >= 0),
    needs_waiter BOOLEAN NOT NULL DEFAULT FALSE
);

-- 6. Order Items
CREATE TABLE IF NOT EXISTS order_item (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    dish_id INT REFERENCES dish(id) ON DELETE SET NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity >= 1),
    special_request VARCHAR(255)
);

-- 7. Carts
CREATE TABLE IF NOT EXISTS carts (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL
);

-- 8. Cart Items
CREATE TABLE IF NOT EXISTS cart_item (
    id SERIAL PRIMARY KEY,
    cart_id INT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    dish_id INT REFERENCES dish(id) ON DELETE SET NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity >= 1),
    special_request VARCHAR(255)
);

-- 9. Push Subscriptions
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    endpoint VARCHAR(2048) NOT NULL UNIQUE,
    p256dh VARCHAR(255),
    auth VARCHAR(255),
    user_id VARCHAR(255),
    roles VARCHAR(255)
);

-- 10. Loyalty System
CREATE TABLE IF NOT EXISTS loyalty_account (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS loyalty_transaction (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES loyalty_account(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount >= 0),
    balance_after NUMERIC(19, 2) NOT NULL CHECK (balance_after >= 0),
    reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_loyalty_transaction_reference
    ON loyalty_transaction(reference)
    WHERE reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_loyalty_transaction_account_id
    ON loyalty_transaction(account_id);

-- 11. Customer Reviews
CREATE TABLE IF NOT EXISTS order_service_review (
    id BIGSERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_order_service_review_order_id UNIQUE (order_id)
);

CREATE INDEX IF NOT EXISTS ix_order_service_review_user_id
    ON order_service_review(user_id);

CREATE TABLE IF NOT EXISTS order_dish_review (
    id BIGSERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    dish_id INT NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_order_dish_review_order_dish UNIQUE (order_id, dish_id)
);

CREATE INDEX IF NOT EXISTS ix_order_dish_review_dish_id
    ON order_dish_review(dish_id);

CREATE INDEX IF NOT EXISTS ix_order_dish_review_user_id
    ON order_dish_review(user_id);
