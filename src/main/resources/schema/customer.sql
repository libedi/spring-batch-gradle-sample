CREATE TABLE IF NOT EXISTS customer (
    billing_id BIGINT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL
);
