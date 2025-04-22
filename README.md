# Changes Consumer Application

This is a Spring Boot application that demonstrates Change Data Capture (CDC) using Debezium.

## Prerequisites

- Java 17 or later
- Docker and Docker Compose
- Gradle

## Components

### 1. Spring Boot Application
- Swagger UI for API documentation
- RabbitMQ consumer for external consumers

### 2. PostgreSQL Database
- Stores CRM changes
- Configured with rabbitmq with CRM

### 3. RabbitMQ
- Message broker for consumers
- Management interface available

## Access Points

- Spring Boot Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- RabbitMQ Management: http://localhost:15672
