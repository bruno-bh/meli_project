## Comprehensive Project Analysis

### 1. 🐛 Bugs & Potential Errors

#### 1.1 — ~~`ProductType` enum is defined but never used~~ ✅ RESOLVED
> **Resolution:** The `ProductType` enum has been deleted and replaced by a YAML-driven template system (`ProductTemplateService`). Product types are now validated against templates loaded from `data/product-templates.yaml`. `ProductService.validateProduct()` calls `templateService.isValidType(type)` to reject unknown types. Default units from templates are applied to `MeasurableValue` fields (price, size, weight) via `applyDefaultUnits()`.

#### 1.2 — `extractProductFields` always includes `description` and `imageUrl` regardless of filters
In ProductService.java, description and imageUrl are **always injected** before processing filters. If the user asks to compare only `price`, they still get `id`, `name`, `description`, and `imageUrl`. The `switch` cases for `"description"` and `"imageurl"` even explicitly say `// Já incluídos automaticamente` and do nothing. This **violates the spec requirement**: *"A user should be able to query specific comparisons between items and ignore other fields."*

#### 1.3 — Comparison allows a single product
The compare endpoint accepts 1 product ID. A comparison with a single item is semantically meaningless. There should be a minimum of 2 products for comparison.

#### 1.4 — `ProductRepository` reads/writes the entire JSON file on every operation
Every call to `findById`, `save`, `deleteById`, or `count` calls `findAll()` which deserializes the full JSON file from disk. `save()` calls `findAll()`, mutates the list, then writes it back. This is a concurrency hazard — two concurrent saves can silently lose data. There's no synchronization.

#### 1.5 — `@Valid` on `ProductFilter` doesn't trigger validation
In the controller, `ProductFilter` is bound as query parameters via `@Valid ProductFilter filter`. However, `ProductFilter` uses `@Min` from Jakarta Validation. For query-parameter binding (not `@RequestBody`), `@Valid` alone doesn't trigger validation — it requires `@Validated` on the controller **and** method-level validation. The controller *does* have `@Validated`, which is good, but the `filter.validate()` call inside the controller also duplicates the price range check that's already done again inside `searchProducts()`. The same validation runs **three times** (annotation → `filter.validate()` → `searchProducts()` method body).

#### 1.6 — `NullPointerException` risk in `searchProducts` filters
At ProductService.java, the filter checks `product.getType().equalsIgnoreCase(filter.getType())`. If `product.getType()` is `null` (which is possible since the Product model doesn't enforce non-null), this throws a `NullPointerException`. Same risk with `product.getName()` and `product.getPrice()`.

#### 1.7 — Data file item inconsistency: Running Shoes categorized as `CLOTHING`
In products-example.json, "Sports Running Shoes" has type `"CLOTHING"` but should logically be `"SPORTS"`, which is a type defined in `ProductType`. This means you can't properly compare shoes with other clothing items since their specifications differ.

---

### 2. 📐 Missing Fields & Spec Gaps

#### 2.1 — ~~Missing specification fields from the spec~~ ✅ RESOLVED
> **Resolution:** Specification fields are now defined in `data/product-templates.yaml` per product type. New fields can be added directly to the YAML file without code changes. The template system is extensible — simply edit the YAML and call `POST /api/v1/templates/reload`.

#### 2.2 — No `rating` validation
Rating is a `Double` with no bounds. A product can be created with a rating of `-5` or `999`. There's no validation that rating is between 0 and 5 (or whatever scale is used).

#### 2.3 — No `@RequestBody` validation on create/update
The `createProduct` and `updateProduct` endpoints accept `@RequestBody Product product` without `@Valid`, so Jakarta Bean Validation annotations on `Product` (if they existed) wouldn't fire. Currently `Product` has no validation annotations at all — all validation is manual in `ProductService.validateProduct()`.

---

### 3. 🏗️ SOLID Principle Violations & Design Issues

#### 3.1 — **Single Responsibility Principle (SRP) Violations**

- **`ProductService`** handles: CRUD operations, search/filtering logic, pagination, comparison logic, field extraction, and product validation. This is at least 4 different responsibilities that should be separated into:
  - `ProductCrudService` (create, read, update, delete)
  - `ProductSearchService` (filtering, pagination)
  - `ProductComparisonService` (comparison logic, field extraction)
  - `ProductValidator` (validation rules)

- **`ProductRepository`** handles: file I/O, data serialization, ID generation, random image URL generation, and default rating assignment. The image URL generation and default value assignment are **repository concerns leaking into business logic**. ID generation policy should be separate.

- **`ProductController`** handles CSV parsing of `ids` and `filters` query parameters directly. This parsing logic should be delegated.

#### 3.2 — **Open/Closed Principle (OCP) Violations** — partially ✅ RESOLVED

- **`extractProductFields`** uses a hardcoded `switch` statement. Adding a new product field requires modifying the switch. This should use reflection or a field-mapping registry.

- ~~**`ProductType`** defines valid types but isn't used. If it were used, adding new types would be simply adding to the enum. Currently, types are free-form strings, making the system fragile.~~ ✅ **RESOLVED:** Product types are now defined in YAML templates. Adding a new type requires only adding a new entry to `product-templates.yaml` — no recompilation needed. The system is now open for extension and closed for modification.

#### 3.3 — **Interface Segregation Principle (ISP) Violations**

- `ProductRepository` has no interface. It's a concrete class injected directly. This violates ISP and DIP (see below) and makes testing harder (tests must mock a concrete class).

#### 3.4 — **Dependency Inversion Principle (DIP) Violations**

- `ProductService` depends directly on the concrete `ProductRepository` class, not an abstraction (interface). If you wanted to swap to an H2 database or any other storage, you'd need to modify `ProductService`.

- `ProductRepository` uses hardcoded file paths (`"data/products.json"`). This should be configurable via application.properties.

#### 3.5 — **Liskov Substitution Principle (LSP)**

- The `Product` model uses a generic `Map<String, Object>` for specifications rather than polymorphism. The spec asks for specialized product types (e.g., a `Smartphone` with specific fields). A better design would use inheritance or composition (e.g., `SmartphoneProduct extends Product`) so the type system enforces required fields.

---

### 4. 🔧 Additional Design Concerns

#### 4.1 — No API documentation (Swagger/OpenAPI)
The spec asks for documentation. While a README exists, there's no Swagger/OpenAPI integration. Adding `springdoc-openapi` would auto-generate interactive API docs.

#### 4.2 — Inconsistent error response structure
The `GlobalExceptionHandler` returns `ResponseEntity<?>` with a manually built `Map<String, Object>`. This should use a standardized `ErrorResponse` DTO to ensure type safety and consistency. Some responses have `"errors"` (map), some have `"message"` (string) — consumers can't predict the shape.

#### 4.3 — `@CrossOrigin(origins = "*")` is too permissive
Allowing all origins is a security risk. This should be configurable.

#### 4.4 — ~~No logging~~ ✅ RESOLVED
> **Resolution:** Comprehensive logging has been implemented across all layers. Added `logback-spring.xml` with console appender (default) and file appender (prod profile only, 30-day retention). Replaced 3 manual `LoggerFactory` instances with Lombok `@Slf4j`. Translated 15 Portuguese log messages to English. Added ~38 new log statements across controllers, services, repository, and exception handler. Total log statements: ~57. All 92 tests pass.

#### 4.5 — Integration tests mutate the real data file
`ProductApiIntegrationTest` uses `@SpringBootTest` and operates on the real products.json. This can corrupt production data. Tests should use a test-specific profile with a different data path, or use an in-memory store.

#### 4.6 — `Serializable` with `serialVersionUID` is unnecessary
`Product` implements `Serializable` but the application never does Java serialization (it uses JSON via Jackson). This is dead code.

#### 4.7 — Unused import/field in `Product`
The `Product` model imports `java.util.Random` and `java.util.UUID` — these are unused.

#### 4.8 — `commons-io` dependency is unused
The pom.xml includes Apache Commons IO but it's never referenced anywhere in the code.

#### 4.9 — Deprecated `@MockBean`
The tests use `@MockBean` from `org.springframework.boot.test.mock.mockito`, which is deprecated in Spring Boot 3.4+. Should migrate to `@MockitoBean`.

#### 4.10 — Missing pagination metadata in response
The search endpoint returns a raw `List<Product>` with no pagination info (total count, total pages, current page). Consumers can't tell if there are more results.

---

### 5. 📋 Summary of Recommended Changes (Priority-Ordered)

| Priority | Issue | Category |
|----------|-------|----------|
| 🔴 Critical | `extractProductFields` ignores user-requested filters (always includes description/imageUrl) | Bug |
| ~~🔴 Critical~~ | ~~`ProductType` enum is unused — no type validation~~ ✅ RESOLVED | Missing Feature |
| 🔴 Critical | `ProductRepository` has no interface (DIP violation) | SOLID |
| 🟠 High | `ProductService` has too many responsibilities (SRP violation) | SOLID |
| ~~🟠 High~~ | ~~Missing smartphone spec fields (battery, OS, model version)~~ ✅ RESOLVED (YAML templates) | Spec Gap |
| 🟠 High | No rating validation (bounds checking) | Bug |
| 🟠 High | `NullPointerException` risk in search filters | Bug |
| 🟠 High | Integration tests write to real data file | Testing |
| 🟡 Medium | No Swagger/OpenAPI documentation | Non-functional |
| 🟡 Medium | No pagination metadata in list response | Design |
| 🟡 Medium | Inconsistent error response (no DTO) | Design |
| 🟡 Medium | Triplicated validation logic for price range | Code Smell |
| ~~🟡 Medium~~ | ~~No logging anywhere~~ ✅ RESOLVED | Non-functional |
| 🟢 Low | Hardcoded file path in repository | Configuration |
| 🟢 Low | Unused `Serializable`, `commons-io` dependency | Cleanup |
| 🟢 Low | `@CrossOrigin(origins = "*")` security concern | Security |
| 🟢 Low | Comparison allows single product | UX |