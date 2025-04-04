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

### Account Operations
```http
GET /api/v1/accounts/{accountNo}/balance    # Get account balance
```

#### Example Responses:
```json
// GET /api/v1/accounts/ACC001/balance
200 OK
{
    "balance": 1000.00
}
```

### Transaction Operations
```http
# Create Transaction
POST /api/transactions
Content-Type: application/json

{
    "accountNo": "ACC001",
    "amount": 100.00,
    "direction": "DEBIT"
}

# Update Transaction Status
PUT /api/transactions/{id}/status?status=SUCCESS

# Query Transactions with Filters
GET /api/transactions?accountNo=ACC001&direction=DEBIT&status=SUCCESS&minAmount=100&maxAmount=1000&fromDate=2024-01-01T00:00:00&toDate=2024-12-31T23:59:59&page=0&size=10

# Delete Transaction
DELETE /api/transactions/{id}
```

#### Query Parameters for Transactions:
| Parameter  | Type          | Required | Default | Description |
|-----------|---------------|----------|---------|-------------|
| accountNo | String        | No       | -       | Account number to filter by |
| direction | Enum          | No       | -       | DEBIT or CREDIT |
| status    | Enum          | No       | -       | Transaction status |
| minAmount | BigDecimal    | No       | -       | Minimum transaction amount |
| maxAmount | BigDecimal    | No       | -       | Maximum transaction amount |
| fromDate  | LocalDateTime | No       | -       | Start date for transaction search |
| toDate    | LocalDateTime | No       | -       | End date for transaction search |
| page      | Integer       | No       | 0       | Page number (0-based) |
| size      | Integer       | No       | 10      | Number of items per page |

#### Example Responses:
```json
// POST /api/transactions
201 Created
{
    "transactionId": "TX123",
    "accountNo": "ACC001",
    "amount": 100.00,
    "direction": "DEBIT",
    "status": "PENDING",
    "timestamp": "2024-01-20T10:30:00"
}

// GET /api/transactions
200 OK
{
    "content": [
        {
            "transactionId": "TX123",
            "accountNo": "ACC001",
            "amount": 100.00,
            "direction": "DEBIT",
            "status": "SUCCESS",
            "timestamp": "2024-01-20T10:30:00"
        }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 10
}
```

### API Documentation
The API is documented using OpenAPI (Swagger) and can be accessed at:
```http
GET /swagger-ui.html    # Interactive API documentation
GET /v3/api-docs       # OpenAPI specification
```

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