# Transaction Management Service

A simple Spring Boot application for managing financial transactions within a banking system.

## Features

- Create, read, update, and delete transactions
- List all transactions
- Filter transactions by type
- In-memory data storage with caching
- Input validation
- Exception handling
- RESTful API design

## Technologies

- Java 21
- Spring Boot 3.2.3
- Maven
- Spring Cache
- Lombok

## API Endpoints

- POST `/api/transactions` - Create a new transaction
- GET `/api/transactions` - Get all transactions
- GET `/api/transactions/{id}` - Get a specific transaction
- PUT `/api/transactions/{id}` - Update a transaction
- DELETE `/api/transactions/{id}` - Delete a transaction
- GET `/api/transactions/type/{type}` - Get transactions by type

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
    "description": "Payment for services",
    "type": "PAYMENT",
    "status": "COMPLETED"
  }'
```

### Get All Transactions
```bash
curl http://localhost:8080/api/transactions
```

## Error Handling

The API uses standard HTTP status codes:
- 200: Success
- 201: Created
- 400: Bad Request
- 404: Not Found
- 500: Internal Server Error

Validation errors and exceptions are returned with descriptive messages. 