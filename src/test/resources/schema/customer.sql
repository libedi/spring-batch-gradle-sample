CREATE TABLE IF NOT EXISTS customer (
    billing_id BIGINT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL
);

DELETE FROM customer;

INSERT INTO customer (billing_id, customer_name, email) VALUES
(1, 'Alice Kim', 'alice@example.com'),
(2, 'Bob Lee', 'bob@example.com');
