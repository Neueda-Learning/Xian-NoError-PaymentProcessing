# Demo Script

## Demo 1: Successful payment

1. Open http://localhost:8080
2. Click "Fill Demo Data"
3. Click "Create Payment"
4. Select the created payment row
5. Click "Validate"
6. Click "Send"
7. Click "Complete"
8. Show the status history timeline
9. Highlight that the demo now uses a CNY source account and a USD destination account
10. Explain that the destination balance is credited after exchange conversion

## Demo 2: Failed payment

1. Create a new payment
2. Click "Validate"
3. Click "Fail"
4. Use error code: NETWORK_ERROR
5. Use message: Simulated network error
6. Show FAILED status and error details

## Demo 3: Duplicate idempotency key

1. Create a payment with idempotency key PAY-DEMO-DUPLICATE
2. Try creating another payment with the same idempotency key
3. Show DUPLICATE_PAYMENT error

## Demo 4: Invalid status transition

1. Pick a CREATED payment
2. Use Swagger or sample-api-requests.http to call POST /api/payments/{id}/send
3. Show INVALID_STATUS_TRANSITION error
