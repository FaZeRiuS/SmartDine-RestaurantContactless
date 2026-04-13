-- Additional indexes for common joins and dashboards

-- Order items
CREATE INDEX IF NOT EXISTS ix_order_item_order_id ON order_item(order_id);
CREATE INDEX IF NOT EXISTS ix_order_item_dish_id ON order_item(dish_id);

-- Cart items
CREATE INDEX IF NOT EXISTS ix_cart_item_cart_id ON cart_item(cart_id);
CREATE INDEX IF NOT EXISTS ix_cart_item_dish_id ON cart_item(dish_id);

-- Orders: payment dashboards / time-based filters
CREATE INDEX IF NOT EXISTS ix_orders_payment_status_created_at ON orders(payment_status, created_at DESC);

