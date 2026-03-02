# Payment Gateway - Design Documentation

## Overview

This document states the key design considerations, assumptions made in implementing this payment gateway solution for the coding challenge. 

**Core Functionality:**
- `POST /payment` - Process a new card payment
- `GET /payment/{id}` - Retrieve a previously processed payment by UUID
---

## Architecture

My changes build on classic Spring Boot MVC architecture with clear separation of concerns:

```
┌─────────────────────────────┐
│   PaymentGatewayController  │  ← REST endpoints
└─────────────┬───────────────┘
              │
┌─────────────▼───────────────┐
│   PaymentGatewayService     │  ← Business logic orchestration
└─────┬───────────────────┬───┘
      │                   │
      ▼                   ▼
┌─────────────┐   ┌──────────────────┐
│  Payments   │   │      Bank        │  ← External integrations
│  Repository │   │     Client       │
└─────────────┘   └──────────────────┘
      │
      ▼
┌────────────────────────────┐
│  PaymentRequestValidator   │  ← Business validation
└────────────────────────────┘
```

**Rationale:** This layered approach ensures:
- Clear separation of concerns/responsibilities
- Easy testability (can mock layers independently)
- Maintainability (changes in one layer don't ripple through others)
---

## Two-Tier Validation Strategy

### Tier 1: Field-Level Validation (Bean Validation)

Applied directly to `PostPaymentRequest` using annotations:

- **`card_number`**: 14-19 digits, numeric only (`@Pattern`, `@Size`)
- **`expiry_month`**: 1-12 (`@Min`, `@Max`)
- **`expiry_year`**: >= 1900 (`@Min`)
- **`currency`**: Exactly 3 characters (`@Size`)
- **`amount`**: >= 1 (`@Min`)
- **`cvv`**: 3-4 digits, numeric only (`@Pattern`, `@Size`)

Triggered by Spring's `@Valid` annotation on controller method parameters.

### Tier 2: Business Validation (Custom Logic)

Implemented in `PaymentRequestValidator`:

1. **Currency Supported**
   - Only USD, EUR, GBP supported
   - Throws `PaymentValidationException` for unsupported currencies

2. **Expiry Date Validation**
   - Validates date is actually valid
   - Checks card hasn't expired (month/year comparison with current date)
   - Uses Java Time API (`YearMonth`) for accurate date arithmetic

**Rationale for Two-Tier Approach:**
- **Separation of concerns**: Input Format validation vs. business rules
- **Business logic clarity**: Custom validator explicitly testing neccessary business rules
- **Error granularity**: Different validation failures produce specific error messages
---

### Error Handling Strategy

Implemented in `CommonExceptionHandler` :

| Exception Type | HTTP Status | Scenario |
|---------------|-------------|----------|
| `PaymentValidationException` | 400 BAD_REQUEST | Business validation failed (unsupported currency, expired card) |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | Field validation failed (invalid format, missing required field) |
| `BankCommunicationException` | 502 BAD_GATEWAY | Acquiring bank is unavailable (5xx error from bank) |

All exceptions are logged with SLF4J for debugging and monitoring.

### Bank Error Handling Strategy

Implemented in `BankClient`:

- **5xx errors from bank / Network failures/timeouts** → Throw `BankCommunicationException` → Gateway returns 502

- **4xx errors from bank** → Return declined payment response → Gateway returns 200 with DECLINED status
  - Bank processed the request but declined it; this is a valid business outcome, not a system error

---

## Testing Strategy

### Integration Tests (`PaymentGatewayControllerTest`)

**Coverage:**
- Successful payment authorization (card ending in 1)
- Payment declined by bank (card ending in 2)
- Bank communication failure (card ending in 0) → 502
- Payment retrieval by ID
- Missing required fields → 400
- Invalid card number format → 400
- Invalid CVV format → 400
- Negative amount → 400
- Unsupported currency → 400
- Expired card → 400

### Unit Tests (`PaymentRequestValidatorTest`)

Tests custom validation logic in isolation.

**Coverage:**
- Valid request passes validation
- Unsupported currency throws `PaymentValidationException`
- Expired card throws `PaymentValidationException`

---

### Assumptions Made
 - no thread safety/concurrent handling of requests needed
 - no rate limiting on incoming requsts
 - no authorization/authentication needed when connecting to acquiring bank

