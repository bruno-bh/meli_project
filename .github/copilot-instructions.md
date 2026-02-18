# Copilot Agent Rules — Product API (Meli Project)

## Project Overview

This is a **Spring Boot 3.2.0 REST API** for product management and comparison, built as part of a Mercado Libre item comparison challenge. It provides full CRUD operations, advanced filtering/pagination, and multi-product comparison by type.

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.0 (Spring Web, Validation)
- **Build Tool:** Maven 3.6+
- **Testing:** JUnit 5 + Mockito
- **Data Persistence:** JSON file (`data/products.json`) — no database
- **Templates:** YAML-driven product types (`data/product-templates.yaml`)
- **Annotations:** Lombok (builders, getters/setters, etc.)

---

## Architecture & Package Structure

```
src/main/java/com/meli/productapi/
├── ProductApiApplication.java          # Spring Boot entry point
├── controller/
│   ├── ProductController.java          # REST endpoints (@RestController)
│   └── TemplateController.java         # Template endpoints (list, get, reload)
├── model/
│   ├── Product.java                    # Main entity (Lombok @Builder)
│   ├── MeasurableValue.java            # Value+Unit DTO for price/size/weight
│   ├── ProductFilter.java              # DTO for query params with validation
│   ├── ProductComparisonResponse.java  # DTO for comparison results
│   └── template/
│       ├── FieldDefinition.java        # YAML field config (type, defaultUnit, required, comparable)
│       ├── ProductTemplate.java        # Product type template (name, displayName, fields, specs)
│       └── ProductTemplateConfig.java  # YAML root wrapper
├── service/
│   ├── ProductService.java            # Business logic layer
│   └── ProductTemplateService.java    # YAML template loading + validation
├── repository/
│   ├── ProductRepositoryInterface.java # Repository abstraction
│   └── ProductRepository.java          # JSON file-based implementation
└── exception/
    ├── GlobalExceptionHandler.java     # @RestControllerAdvice for all errors
    ├── ErrorResponse.java              # Standard error response DTO
    ├── ProductNotFoundException.java   # 404 errors
    └── IncompatibleProductTypesException.java # 409 errors (comparison)
```

### Layer Responsibilities

- **Controller:** Thin layer — delegates to service, handles HTTP mapping only. Uses `@Valid` for request validation.
- **Service:** All business logic, product validation, filtering, pagination, and comparison logic. `ProductTemplateService` manages YAML template loading, caching, and type validation.
- **Repository:** Abstracts data access via `ProductRepositoryInterface`. Current implementation reads/writes `data/products.json`.
- **Exception:** Global handler translates exceptions to structured `ErrorResponse` JSON with HTTP status codes.

---

## Coding Conventions

### General
- **Language:** All code (variables, methods, classes) in **English**
- **User-facing messages & docs:** Written in **English**
- **Logging:** Use SLF4J (`log.debug`, `log.info`, `log.warn`). Log at appropriate levels.
- **Comments:** Inline comments in English to explain non-obvious logic. Javadoc for public classes/methods.
- **No magic strings:** Use constants or enums.

### Java Style
- Use **Lombok** annotations (`@Builder`, `@Data`, `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`) to reduce boilerplate.
- Use **constructor injection** (not `@Autowired` on fields).
- Use `Optional` for potentially absent values (e.g., `findById` returns `Optional<Product>`).
- Use **Java Streams** for filtering, mapping, and collecting.
- Use **`Collections.unmodifiableList`** for immutable collections in enums.
- Prefer `List.of()`, `Map.of()` for immutable literals.

### Spring Boot
- Controllers annotated with `@RestController`, `@RequestMapping`, `@Validated`.
- Return `ResponseEntity<T>` from all controller methods with explicit HTTP status codes.
- Use `@Valid` on `@RequestBody` and DTO parameters for Bean Validation (JSR-303).
- CORS is configured at the controller level with `@CrossOrigin(origins = "*", maxAge = 3600)`.

### Naming
- **Endpoints:** Follow REST conventions under `/api/v1/products`
- **Classes:** PascalCase (e.g., `ProductService`, `ProductFilter`)
- **Methods:** camelCase, verb-first (e.g., `getProductById`, `createProduct`, `validateProduct`)
- **Test classes:** `<ClassName>Test` (e.g., `ProductServiceTest`)
- **Test methods:** `test<Behavior>` (e.g., `testCreateProductWithNullPrice`)

---

## API Endpoints

| Method | Path                         | Description                        |
|--------|------------------------------|------------------------------------|
| GET    | `/api/v1/products`           | List/search with filters + pagination |
| GET    | `/api/v1/products/{id}`      | Get product by ID                  |
| POST   | `/api/v1/products`           | Create product                     |
| PUT    | `/api/v1/products/{id}`      | Update product                     |
| DELETE | `/api/v1/products/{id}`      | Delete product                     |
| GET    | `/api/v1/products/stats/count` | Get total product count          |
| GET    | `/api/v1/products/compare`   | Compare products by IDs + filters  |
| GET    | `/api/v1/templates`          | List all product type templates    |
| GET    | `/api/v1/templates/{type}`   | Get template for a specific type   |
| POST   | `/api/v1/templates/reload`   | Reload templates from YAML         |

### Query Parameters (GET /products)
- `name`, `description` — partial, case-insensitive string search
- `type` — exact match against `ProductType` enum
- `priceMin`, `priceMax` — numeric range (inclusive, must be >= 1)
- `page` (default: 1), `pageSize` (default: all) — pagination

### Comparison Rules (GET /products/compare)
- Requires `ids` parameter (comma-separated product IDs)
- All products must be of the **same ProductType** (returns 409 if not)
- Optional `filters` parameter to select specific fields
- Always returns `id` and `name`; optionally `description`, `imageUrl`

---

## Product Model

### Required Fields
- `name` — non-empty string
- `price` — `MeasurableValue` (`{ "value": 99.99, "unit": "BRL" }`) — value must be positive
- `type` — valid product type (must exist in `product-templates.yaml`, case-insensitive)

### Optional Fields
- `id` — auto-generated (incremental string)
- `description`, `imageUrl` (auto-generated if empty), `color`
- `size` — `MeasurableValue` (e.g., `{ "value": 6.1, "unit": "inches" }`)
- `weight` — `MeasurableValue` (e.g., `{ "value": 0.187, "unit": "kg" }`)
- `rating` — Double, range 0.0–5.0 (default: 0.0)
- `specifications` — `Map<String, String>` with type-specific metadata

### MeasurableValue
Used for `price`, `size`, and `weight`. Contains `value` (Double) and `unit` (String). Default units per type are defined in YAML templates and applied automatically.

### Product Types (YAML Templates)
`CELLPHONES`, `COMPUTERS`, `CLOTHING`, `FOOD`, `BEVERAGES`, `FURNITURE`, `BOOKS`, `SPORTS`

Defined in `data/product-templates.yaml`. Each type specifies fields (with default units), specifications, and display name. New types can be added by editing the YAML — no recompilation needed.

---

## Error Handling

All errors return a structured `ErrorResponse`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive message here",
  "path": "/api/v1/products"
}
```

### HTTP Status Mapping
| Exception                          | HTTP Status |
|------------------------------------|-------------|
| `IllegalArgumentException`         | 400         |
| `MethodArgumentNotValidException`  | 400         |
| `ConstraintViolationException`     | 400         |
| `MethodArgumentTypeMismatchException` | 400      |
| `ProductNotFoundException`         | 404         |
| `IncompatibleProductTypesException` | 409        |
| `Exception` (catch-all)           | 500         |

---

## Testing Guidelines

### Test Structure
```
src/test/java/com/meli/productapi/
├── ProductApiIntegrationTest.java            # Full integration tests
├── controller/
│   ├── ProductControllerTest.java            # @WebMvcTest with MockMvc
│   ├── ProductComparisonControllerTest.java  # Comparison endpoint tests
│   └── TemplateControllerTest.java           # Template endpoint tests
├── model/
│   └── MeasurableValueTest.java              # MeasurableValue builder + serialization
└── service/
    ├── ProductServiceTest.java               # Unit tests with mocked repo + templateService
    ├── ProductComparisonServiceTest.java     # Comparison logic tests
    └── ProductTemplateServiceTest.java       # YAML loading, validation, reload
```

### Testing Rules
- **Unit tests:** Use `@ExtendWith(MockitoExtension.class)` for service tests. Mock `ProductRepositoryInterface`.
- **Controller tests:** Use `@WebMvcTest` with `MockMvc`. Mock `ProductService` with `@MockBean`.
- **Integration tests:** Use `@SpringBootTest` with `@AutoConfigureMockMvc`. Uses `data/test/products-test.json`.
- **Test display names:** Use `@DisplayName` with descriptive Portuguese text.
- **Assertions:** Use JUnit 5 assertions (`assertEquals`, `assertThrows`, etc.).
- **Coverage goal:** All public methods should have tests. Validate happy path, edge cases, and error scenarios.
- **Run all tests:** `mvn clean test` — currently **92 tests, 100% passing**.

### Test Data
- Production data: `data/products.json`
- Production templates: `data/product-templates.yaml`
- Test data: `data/test/products-test.json`
- Test templates: `data/test/product-templates-test.yaml`
- Test `application.properties` points to test data/template files.

---

## Data Storage

- Products are stored in `data/products.json` (configured via `product.data.file` in `application.properties`).
- The repository reads/writes this file using Jackson `ObjectMapper`.
- IDs are auto-incremented strings.
- No database — this is intentional per the project requirements (simulated persistence).

---

## Build & Run

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Test
mvn clean test
```

Server runs on `http://localhost:8080`.

---

## Key Design Decisions

1. **Repository Interface Pattern:** `ProductRepositoryInterface` allows swapping JSON-file storage for a database without changing service/controller layers.
2. **Unified Search Endpoint:** Single `GET /products` with optional filters instead of multiple specialized endpoints.
3. **YAML-Driven Product Templates:** Product types are defined in `data/product-templates.yaml` with fields (including default units) and specifications. `ProductTemplateService` loads and caches them. Types can be added/modified without recompilation. Templates can be reloaded at runtime via `POST /api/v1/templates/reload`.
4. **MeasurableValue Pattern:** `price`, `size`, and `weight` use `MeasurableValue` (value + unit) instead of primitives, enabling unit-aware comparisons and automatic default unit assignment from templates.
4. **Global Exception Handler:** Centralized error handling via `@RestControllerAdvice` ensures consistent error responses.
5. **ProductFilter DTO:** Encapsulates all query parameters with Bean Validation annotations and custom `validate()` method for cross-field rules.
6. **Comparison by Type:** Products can only be compared within the same `ProductType`, returning 409 Conflict otherwise.

---

## Do's and Don'ts

### ✅ Do
- Follow the existing layered architecture (Controller → Service → Repository)
- Add `@DisplayName` to all new test methods
- Validate all inputs in the service layer
- Return appropriate HTTP status codes
- Use the `ErrorResponse` DTO for error responses
- Keep controllers thin — put logic in services
- Use Lombok annotations to reduce boilerplate
- Write tests for new features (unit + controller)
- Use SLF4J for logging with appropriate levels
- Add new product types via YAML templates, not code changes
- Use `MeasurableValue` for any new numeric fields that have units

### ❌ Don't
- Don't add database dependencies (H2, JPA, etc.) without explicit instruction
- Don't bypass the `ProductRepositoryInterface` abstraction
- Don't put business logic in controllers
- Don't return raw exceptions to the client — use `GlobalExceptionHandler`
- Don't use `@Autowired` field injection — use constructor injection
- Don't hardcode file paths — use `application.properties` configuration
- Don't break existing tests — run `mvn clean test` before finalizing changes
- Don't mix languages: code in English, user-facing messages in English, documentation in English
- Don't add a `ProductType` enum — types are managed via YAML templates
- Don't use primitive `Double` for price/size/weight — use `MeasurableValue`
