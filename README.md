# MiniCommerce API (Spring Boot)

![CI](https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/actions/workflows/ci.yml/badge.svg)
[![codecov](https://codecov.io/gh/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/branch/main/graph/badge.svg)](https://codecov.io/gh/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>)

A small REST API used for Software Quality Assurance & Testing assignments.

## Tech stack
- Java 17
- Spring Boot 3.5.9
- Spring Web + Validation + Spring Data JPA
- H2 database
- Swagger UI via springdoc-openapi

## Run
```bash
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console: http://localhost:8080/h2-console  
  JDBC URL (dev): `jdbc:h2:file:./data/minicommerce`

## Test
```bash
mvn test
```

### Coverage
JaCoCo report:
- `target/site/jacoco/index.html`

## API
Base path: `/api`

Resources:
- `/users`
- `/categories`
- `/products`
- `/orders` (PATCH: status)
- `/reviews` (PATCH: rating/comment)

Category deletion rule:
- `DELETE /api/categories/{id}` returns **409 Conflict** if the category still has products.

## Docker + PostgreSQL (optional)
If you want to run the API against PostgreSQL:

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

- DB: `postgres://minicommerce:minicommerce@localhost:5432/minicommerce`

## Usage examples (curl)

### Create a user
```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'
```

### Create a category
```bash
curl -s -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Clothing"}'
```

### Create a product
```bash
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Jacket","sku":"SKU-1","price":99.90,"stock":10,"categoryId":1}'
```

### Patch product
```bash
curl -s -X PATCH http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"price":89.90,"stock":8}'
```

### Create an order (decreases stock)
```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"items":[{"productId":1,"quantity":2}]}'
```

### Update order status
```bash
curl -s -X PATCH http://localhost:8080/api/orders/1 \
  -H "Content-Type: application/json" \
  -d '{"status":"PAID"}'
```

### Create a review
```bash
curl -s -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"rating":5,"comment":"Nice!"}'
```

### Patch a review
```bash
curl -s -X PATCH http://localhost:8080/api/reviews/1 \
  -H "Content-Type: application/json" \
  -d '{"rating":4,"comment":"Still good"}'
```
