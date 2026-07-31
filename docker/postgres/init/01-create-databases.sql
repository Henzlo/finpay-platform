-- Runs only on first Postgres boot (empty data volume).
-- Matches the JDBC database names used by the Spring Boot modules.

CREATE DATABASE finpay_auth;
CREATE DATABASE finpay_loans;
CREATE DATABASE finpay_payments;
CREATE DATABASE finpay_credit;
CREATE DATABASE finpay_collections;
CREATE DATABASE finpay_reporting;

GRANT ALL PRIVILEGES ON DATABASE finpay_auth TO finpay_user;
GRANT ALL PRIVILEGES ON DATABASE finpay_loans TO finpay_user;
GRANT ALL PRIVILEGES ON DATABASE finpay_payments TO finpay_user;
GRANT ALL PRIVILEGES ON DATABASE finpay_credit TO finpay_user;
GRANT ALL PRIVILEGES ON DATABASE finpay_collections TO finpay_user;
GRANT ALL PRIVILEGES ON DATABASE finpay_reporting TO finpay_user;
