CREATE TABLE IF NOT EXISTS order_service_review (
    id BIGSERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_order_service_review_order_id
    ON order_service_review(order_id);

CREATE INDEX IF NOT EXISTS ix_order_service_review_user_id
    ON order_service_review(user_id);

CREATE TABLE IF NOT EXISTS order_dish_review (
    id BIGSERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    dish_id INT NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_order_dish_review_order_dish
    ON order_dish_review(order_id, dish_id);

CREATE INDEX IF NOT EXISTS ix_order_dish_review_dish_id
    ON order_dish_review(dish_id);

CREATE INDEX IF NOT EXISTS ix_order_dish_review_user_id
    ON order_dish_review(user_id);

