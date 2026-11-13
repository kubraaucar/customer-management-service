CREATE TABLE customers (
    id             UUID         PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    surname        VARCHAR(100) NOT NULL,
    citizen_number BIGINT       NOT NULL,
    birth_date     DATE,
    is_active      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_customers_citizen_number UNIQUE (citizen_number)
);
