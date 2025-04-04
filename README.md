# Transaction Service

A Spring Boot microservice for managing financial transactions with support for concurrent processing, caching, and comprehensive error handling.

## Features

- Transaction Management (CRUD operations)
- Account Balance Management
- Pagination and Filtering Support
- Caching Implementation
- Comprehensive Error Handling
- Thread-safe Operations
- RESTful API with Swagger Documentation

## Technical Stack

- Java 21
- Spring Boot
- Spring Cache
- JUnit 5
- Swagger/OpenAPI
- Docker
- Kubernetes

## Project Structure

## API Documentation

The API documentation is available through Swagger UI when the application is running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Specification: http://localhost:8080/api-docs

You can use the Swagger UI to:
- Explore available endpoints
- View request/response schemas
- Test API endpoints directly from the browser
- Download the OpenAPI specification

## API Endpoints

### Transaction Endpoints
- POST `/api/transactions` - Create a new transaction
- GET `/api/transactions` - Get all transactions
- GET `/api/transactions/{id}` - Get a specific transaction
- PUT `/api/transactions/{id}` - Update a transaction
- DELETE `/api/transactions/{id}` - Delete a transaction

### Account Endpoints
- GET `/api/accounts/{accountNo}/balance` - Get account balance
- POST `/api/accounts/transfer` - Transfer money between accounts

## Building and Running

1. Build the project:
   ```bash
   mvn clean install
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on port 8080.

## API Usage Examples

### Create a Transaction
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.50,
    "accountNo": "12345678901",
    "description": "Payment for services",
    "direction": "OUT",
    "status": "COMPLETED"
  }'
```

### Get All Transactions
```bash
curl http://localhost:8080/api/transactions
```

### Get Transactions by Direction
```bash
curl http://localhost:8080/api/transactions/direction/OUT
```

### Get Account Balance
```bash
curl http://localhost:8080/api/transactions/account/12345678901/balance
```

### Transfer Money Between Accounts
```bash
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "12345678901",
    "toAccount": "98765432100",
    "amount": 50.00,
    "description": "Payment for rent"
  }'
```

## Account Balance Management

The service automatically manages account balances based on transaction directions:
- IN: Amount is added to the account balance
- OUT: Amount is subtracted from the account balance

Account balances are initialized to 0 when the first transaction is made for a new account.
All balance updates are atomic and thread-safe using ConcurrentHashMap.

### Account Transfers
- Transfers between accounts are handled as atomic operations
- Both debit and credit transactions are created in a single transaction
- Insufficient balance check is performed before the transfer
- Transaction consistency is maintained using Spring's @Transactional

## Error Handling

The API uses standard HTTP status codes:
- 200: Success
- 201: Created
- 400: Bad Request (including insufficient balance)
- 404: Not Found
- 500: Internal Server Error

Validation errors and exceptions are returned with descriptive messages. 