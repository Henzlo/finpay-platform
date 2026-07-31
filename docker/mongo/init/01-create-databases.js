// Runs only on first Mongo boot (empty data volume).
// Creates the databases referenced by the Spring Boot Mongo URIs.
db.getSiblingDB('finpay_notifications').createCollection('_init');
db.getSiblingDB('finpay_analytics').createCollection('_init');
db.getSiblingDB('finpay_documents').createCollection('_init');
db.getSiblingDB('finpay_fraud').createCollection('_init');
db.getSiblingDB('finpay_reporting').createCollection('_init');
