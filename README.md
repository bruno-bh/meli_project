# 🛒 Product API

A REST API for product management and comparison, built with **Java 17** and **Spring Boot 3.2.0**. Features full CRUD operations, advanced filtering with pagination, multi-product comparison by type, and YAML-driven product templates.

---

## 📋 Table of Contents

- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [API Security](#-api-security)
- [API Documentation (Swagger)](#-api-documentation-swagger)
- [Endpoints](#-endpoints)
- [Product Model](#-product-model)
- [Product Types (YAML Templates)](#-product-types-yaml-templates)
- [Pagination](#-pagination)
- [Error Handling](#-error-handling)
- [Testing](#-testing)
- [Project Structure](#-project-structure)
- [Configuration](#%EF%B8%8F-configuration)

---

## 🚀 Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Language |
| Spring Boot | 3.2.0 | Web framework |
| Maven | 3.9+ | Build tool |
| Lombok | — | Boilerplate reduction |
| Jackson | — | JSON/YAML serialization |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI & API docs |
| JUnit 5 + Mockito | — | Testing |

**Data Persistence:** JSON file (`data/products.json`) — no database required.

---

## ✅ Prerequisites

- **Java 21** or higher
- **Maven 3.9+**

Verify your setup:

```bash
java -version    # Should output java 21.x.x or higher
mvn -version     # Should output Apache Maven 3.9.x or higher
```

---

## 🏁 Getting Started

### Build

```bash
mvn clean package
```

### Run

```bash
mvn spring-boot:run
```

The API will start on **http://localhost:8080**.

### Quick Test

```bash
curl -H "X-API-KEY: meli-product-api-key-2025" http://localhost:8080/api/v1/products
```

---

## 🔐 API Security

The API is protected by **API Key authentication**. All requests to `/api/**` endpoints must include the `X-API-KEY` header.

| Header | Value |
|--------|-------|
| `X-API-KEY` | `meli-product-api-key-2025` |

**Example:**

```bash
curl -H "X-API-KEY: meli-product-api-key-2025" http://localhost:8080/api/v1/products
```

**Missing or invalid key returns `401 Unauthorized`:**

```json
{
  "timestamp": "2026-02-18T12:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or missing API Key",
  "path": "/api/v1/products"
}
```

**Excluded paths** (no API key required):

- `/docs` — Swagger UI
- `/api-docs/**` — OpenAPI specification
- `OPTIONS` requests — CORS preflight

> The API key and security toggle are configured in `application.properties` via `api.security.key` and `api.security.enabled`.

---

## 📖 API Documentation (Swagger)

The API includes full **OpenAPI 3.0** documentation with an interactive Swagger UI.

| Resource | URL |
|----------|-----|
| **Swagger UI** | [http://localhost:8080/docs](http://localhost:8080/docs) |
| **OpenAPI JSON spec** | [http://localhost:8080/api-docs](http://localhost:8080/api-docs) |

Swagger UI allows you to:

- Browse all endpoints with descriptions and examples
- Test requests directly in the browser
- Authenticate with the API Key via the **Authorize** button (🔒)

---

## 📡 Endpoints

> All examples include the required `X-API-KEY` header.

### Products

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/products` | List/search products with filters & pagination |
| `GET` | `/api/v1/products/{id}` | Get product by ID |
| `POST` | `/api/v1/products` | Create a new product |
| `PUT` | `/api/v1/products/{id}` | Update a product |
| `DELETE` | `/api/v1/products/{id}` | Delete a product |
| `GET` | `/api/v1/products/stats/count` | Get total product count |
| `GET` | `/api/v1/products/compare` | Compare products by IDs |

### Templates

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/templates` | List all product type templates |
| `GET` | `/api/v1/templates/{type}` | Get template for a specific type |
| `POST` | `/api/v1/templates/reload` | Reload templates from YAML (no restart needed) |

---

### `GET /api/v1/products` — List & Search

Supports filtering, specification-based search, and pagination.

**Query parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `name` | String | Partial, case-insensitive name search |
| `type` | String | Exact product type match (e.g., `CELLPHONES`) |
| `priceMin` | Double | Minimum price (inclusive, must be > 0) |
| `priceMax` | Double | Maximum price (inclusive, must be > 0 and ≥ priceMin) |
| `page` | Integer | Page number (default: 1, must be ≥ 1) |
| `pageSize` | Integer | Items per page (default: all results) |
| _any other_ | String | Matched against product `specifications` keys |

**Examples:**

```bash
# List all products
curl -H "X-API-KEY: meli-product-api-key-2025" \
  http://localhost:8080/api/v1/products

# Search by name
curl -H "X-API-KEY: meli-product-api-key-2025" \
  "http://localhost:8080/api/v1/products?name=iPhone"

# Filter by type and price range
curl -H "X-API-KEY: meli-product-api-key-2025" \
  "http://localhost:8080/api/v1/products?type=CELLPHONES&priceMin=500&priceMax=3000"

# Paginate results
curl -H "X-API-KEY: meli-product-api-key-2025" \
  "http://localhost:8080/api/v1/products?page=1&pageSize=10"

# Filter by specification (e.g., brand)
curl -H "X-API-KEY: meli-product-api-key-2025" \
  "http://localhost:8080/api/v1/products?type=CELLPHONES&brand=Apple"
```

---

### `GET /api/v1/products/{id}` — Get by ID

```bash
curl -H "X-API-KEY: meli-product-api-key-2025" \
  http://localhost:8080/api/v1/products/1
```

Returns `404 Not Found` if the product does not exist.

---

### `POST /api/v1/products` — Create

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "X-API-KEY: meli-product-api-key-2025" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Samsung Galaxy S24 Ultra",
    "description": "Samsung flagship smartphone with AI features",
    "price": { "value": 7499.99, "unit": "BRL" },
    "type": "CELLPHONES",
    "color": "Titanium Black",
    "size": { "value": 6.8, "unit": "inches" },
    "weight": { "value": 0.232, "unit": "kg" },
    "specifications": {
      "brand": "Samsung",
      "storage_gb": "512",
      "memory_gb": "12",
      "camera_mp": "200",
      "battery_capacity": "5000"
    }
  }'
```

Returns `201 Created` with the created product (including the auto-generated `id`).

---

### `PUT /api/v1/products/{id}` — Update

```bash
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "X-API-KEY: meli-product-api-key-2025" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Samsung Galaxy S24 Ultra (Updated)",
    "price": { "value": 6999.99, "unit": "BRL" }
  }'
```

---

### `DELETE /api/v1/products/{id}` — Delete

```bash
curl -X DELETE \
  -H "X-API-KEY: meli-product-api-key-2025" \
  http://localhost:8080/api/v1/products/1
```

Returns `204 No Content` on success. Returns `404 Not Found` if the product does not exist.

---

### `GET /api/v1/products/stats/count` — Product Count

```bash
curl -H "X-API-KEY: meli-product-api-key-2025" \
  http://localhost:8080/api/v1/products/stats/count
```

Returns the total number of products as a plain number.

---

### `GET /api/v1/products/compare` — Compare Products

Compares multiple products of the **same type** side-by-side.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `ids` | String | Yes | Comma-separated product IDs (e.g., `1,2,3`) |
| `filters` | String | No | Comma-separated fields to compare (e.g., `price,rating,storage_gb`) |

```bash
# Compare all fields
curl -H "X-API-KEY: meli-product-api-key-2025" \
  "http://localhost:8080/api/v1/products/compare?ids=1,2"

# Compare specific fields only
curl -H "X-API-KEY: meli-product-api-key-2025" \
  "http://localhost:8080/api/v1/products/compare?ids=1,2&filters=price,rating,storage_gb"
```

**Response example:**

```json
{
  "productType": "CELLPHONES",
  "appliedFilters": ["price", "rating", "storage_gb"],
  "products": [
    {
      "id": "1",
      "name": "iPhone 15 Pro",
      "description": "Latest generation Apple smartphone",
      "imageUrl": "https://picsum.photos/400/600?random=1",
      "price": 5999.99,
      "rating": 4.8,
      "storage_gb": "256"
    },
    {
      "id": "2",
      "name": "Samsung Galaxy S24 Plus",
      "description": "Latest generation Samsung smartphone",
      "imageUrl": "https://picsum.photos/400/600?random=2",
      "price": 4999.99,
      "rating": 4.4,
      "storage_gb": "512"
    }
  ]
}
```

**Rules:**

- All products must be of the **same type** — returns `409 Conflict` if types differ
- `id` and `name` are **always included** in the response
- `description` and `imageUrl` are included when available
- Without `filters`, all comparable fields are returned

---

### Templates

```bash
# List all templates
curl -H "X-API-KEY: meli-product-api-key-2025" \
  http://localhost:8080/api/v1/templates

# Get template for a specific type
curl -H "X-API-KEY: meli-product-api-key-2025" \
  http://localhost:8080/api/v1/templates/CELLPHONES

# Reload templates from disk (no restart needed)
curl -X POST \
  -H "X-API-KEY: meli-product-api-key-2025" \
  http://localhost:8080/api/v1/templates/reload
```

---

## 📦 Product Model

```json
{
  "id": "1",
  "name": "Samsung Galaxy S24 Ultra",
  "description": "Samsung flagship smartphone with AI features",
  "price": { "value": 7499.99, "unit": "BRL" },
  "type": "CELLPHONES",
  "color": "Titanium Black",
  "imageUrl": "https://picsum.photos/400/600?random=1",
  "size": { "value": 6.8, "unit": "inches" },
  "weight": { "value": 0.232, "unit": "kg" },
  "rating": 4.7,
  "specifications": {
    "brand": "Samsung",
    "storage_gb": "512",
    "memory_gb": "12",
    "camera_mp": "200",
    "battery_capacity": "5000"
  }
}
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | — | Auto-generated (incremental) |
| `name` | String | **Yes** | Product name (non-empty) |
| `description` | String | No | Product description |
| `price` | MeasurableValue | **Yes** | `{ "value": 99.99, "unit": "BRL" }` — value must be > 0 |
| `type` | String | **Yes** | Must match a type in `product-templates.yaml` (case-insensitive) |
| `color` | String | No | Product color |
| `imageUrl` | String | No | Auto-generated if empty |
| `size` | MeasurableValue | No | `{ "value": 6.1, "unit": "inches" }` |
| `weight` | MeasurableValue | No | `{ "value": 0.187, "unit": "kg" }` |
| `rating` | Double | No | 0.0 to 5.0 (default: 0.0) |
| `specifications` | Map\<String, Object\> | No | Type-specific metadata (keys validated against templates) |

> **MeasurableValue** is a `{ "value": Double, "unit": String }` pair. Default units per product type are defined in the YAML templates and applied automatically when not provided.

---

## 🏷️ Product Types (YAML Templates)

Product types are defined in `data/product-templates.yaml`. New types can be added or modified **without recompilation** — just edit the file and call `POST /api/v1/templates/reload`.

| Type | Display Name | Key Specifications |
|------|-------------|-------------------|
| `CELLPHONES` | Smartphones | brand, storage_gb, memory_gb, screen_size, camera_mp, battery_capacity |
| `COMPUTERS` | Computers | brand, processor, ram_gb, storage_gb, screen_size |
| `CLOTHING` | Clothing | size_us, size_eu, material, composition |
| `FOOD` | Food | manufacturing_date, expiration_date, nutriscore, origin |
| `BEVERAGES` | Beverages | volume_ml, origin, expiration_date, alcohol_content |
| `FURNITURE` | Furniture | material, dimensions, weight_kg, warranty_months |
| `BOOKS` | Books | author, publisher, pages, language, isbn |
| `SPORTS` | Sports | size, material, technology, warranty_months |

Each template defines:

- **Fields** with `type`, `default_unit`, `required`, and `comparable` attributes
- **Specifications** with type-specific metadata keys and validation rules

---

## 📄 Pagination

When `pageSize` is provided, responses are wrapped in a `PageResponse` object:

```json
{
  "content": [ ... ],
  "page": 1,
  "pageSize": 10,
  "totalElements": 47,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `content` | Array | Items in the current page |
| `page` | int | Current page number (1-based) |
| `pageSize` | int | Items per page |
| `totalElements` | long | Total items across all pages |
| `totalPages` | int | Total number of pages |
| `hasNext` | boolean | Whether a next page exists |
| `hasPrevious` | boolean | Whether a previous page exists |

> If `pageSize` is not provided, all results are returned in a single-page response.

---

## ⚠️ Error Handling

All errors return a structured JSON response:

```json
{
  "timestamp": "2026-02-18T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message",
  "path": "/api/v1/products"
}
```

### HTTP Status Codes

| Status | Meaning | Example |
|--------|---------|---------|
| `200` | OK | Successful GET/PUT |
| `201` | Created | Successful POST |
| `204` | No Content | Successful DELETE |
| `400` | Bad Request | Validation error, invalid parameters |
| `401` | Unauthorized | Missing or invalid API key |
| `404` | Not Found | Product ID not found |
| `409` | Conflict | Comparing products of different types |
| `500` | Internal Server Error | Unexpected server error |

---

## 🧪 Testing

Run all tests:

```bash
mvn clean test
```

The project includes **186 tests** across 14 test classes, all passing.

| Layer | Test Class | Tests |
|-------|-----------|-------|
| Service | `ProductServiceTest` | 48 |
| Service | `ProductComparisonServiceTest` | 19 |
| Service | `ProductTemplateServiceTest` | 13 |
| Controller | `ProductControllerTest` | 26 |
| Controller | `ProductComparisonControllerTest` | 8 |
| Controller | `TemplateControllerTest` | 4 |
| Model | `MeasurableValueTest` | 3 |
| Model | `PageResponseTest` | 7 |
| Model | `ProductFilterTest` | 8 |
| Repository | `ProductRepositoryTest` | 18 |
| Exception | `ErrorResponseTest` | 5 |
| Exception | `GlobalExceptionHandlerTest` | 11 |
| Config | `ApiKeyInterceptorTest` | 5 |
| Integration | `ProductApiIntegrationTest` | 11 |

---

## 📁 Project Structure

```
src/main/java/com/meli/productapi/
├── ProductApiApplication.java              # Spring Boot entry point
├── config/
│   ├── ApiKeyInterceptor.java              # API Key authentication
│   ├── CorsConfig.java                     # Global CORS filter
│   ├── OpenApiConfig.java                  # Swagger/OpenAPI configuration
│   └── WebMvcConfig.java                   # Interceptor registration
├── controller/
│   ├── ProductController.java              # Product REST endpoints (7)
│   └── TemplateController.java             # Template endpoints (3)
├── model/
│   ├── Product.java                        # Main entity
│   ├── MeasurableValue.java                # Value + Unit DTO
│   ├── PageResponse.java                   # Paginated response wrapper
│   ├── ProductFilter.java                  # Query parameter DTO
│   ├── ProductComparisonResponse.java      # Comparison result DTO
│   └── template/
│       ├── FieldDefinition.java            # YAML field configuration
│       ├── ProductTemplate.java            # Product type template
│       └── ProductTemplateConfig.java      # YAML root wrapper
├── service/
│   ├── ProductService.java                 # Business logic
│   └── ProductTemplateService.java         # YAML template management
├── repository/
│   ├── ProductRepositoryInterface.java     # Repository abstraction
│   └── ProductRepository.java             # JSON file implementation
└── exception/
    ├── GlobalExceptionHandler.java         # Centralized error handling
    ├── ErrorResponse.java                  # Error response DTO
    ├── ProductNotFoundException.java       # 404 exception
    └── IncompatibleProductTypesException.java  # 409 exception

data/
├── products.json                           # Product data storage
├── product-templates.yaml                  # Product type definitions
└── test/
    ├── products-test.json                  # Test data
    └── product-templates-test.yaml         # Test templates
```

---

## ⚙️ Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Server port |
| `api.security.enabled` | `true` | Enable/disable API Key authentication |
| `api.security.key` | `meli-product-api-key-2025` | API Key value |
| `product.data.file` | `data/products.json` | Product data file path |
| `product.template.file` | `data/product-templates.yaml` | Template file path |
| `springdoc.swagger-ui.path` | `/docs` | Swagger UI path |
| `springdoc.api-docs.path` | `/api-docs` | OpenAPI spec path |

---

## 📝 License

This project was built as part of a Mercado Libre product comparison challenge.
