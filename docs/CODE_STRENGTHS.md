# 💪 Code Strengths — Patterns, Practices & Rationale

> **Perspective:** Senior Java/Spring Boot Engineer  
> **Scope:** Analysis of all design patterns, best practices, and architectural decisions — **why** they were made and **what value** they deliver.

---

## 📋 Table of Contents

1. [Layered Architecture](#1-layered-architecture)
2. [Repository Interface Abstraction](#2-repository-interface-abstraction)
3. [Constructor Injection (DI)](#3-constructor-injection-di)
4. [Lombok — Strategic Boilerplate Elimination](#4-lombok--strategic-boilerplate-elimination)
5. [Builder Pattern](#5-builder-pattern)
6. [Optional for Nullable Returns](#6-optional-for-nullable-returns)
7. [Java Streams for Data Transformation](#7-java-streams-for-data-transformation)
8. [YAML-Driven Configuration (Template Engine)](#8-yaml-driven-configuration-template-engine)
9. [MeasurableValue — Value Object Pattern](#9-measurablevalue--value-object-pattern)
10. [Global Exception Handler](#10-global-exception-handler)
11. [PageResponse — Generic Pagination Wrapper](#11-pageresponse--generic-pagination-wrapper)
12. [API Key Interceptor — Chain of Responsibility](#12-api-key-interceptor--chain-of-responsibility)
13. [Servlet-Level CORS Filter](#13-servlet-level-cors-filter)
14. [OpenAPI / Swagger Integration](#14-openapi--swagger-integration)
15. [Thread Safety with ReentrantLock](#15-thread-safety-with-reentrantlock)
16. [ConcurrentHashMap for Template Cache](#16-concurrenthashmap-for-template-cache)
17. [Test Architecture](#17-test-architecture)
18. [Defensive Programming](#18-defensive-programming)
19. [Logging Strategy](#19-logging-strategy)
20. [Patterns Summary Matrix](#20-patterns-summary-matrix)

---

## 1. Layered Architecture

### 📍 Where: Entire project structure

```
Controller → Service → Repository → Data Store
```

### 🔑 What It Is

The classic **3-tier architecture** pattern, where each layer has a single responsibility and communicates only with its adjacent layer.

### 🤔 Why This Pattern?

| Benefit | How It Applies |
|---------|---------------|
| **Separation of Concerns** | Controllers handle HTTP, services handle business logic, repository handles data access |
| **Testability** | Each layer can be tested independently (MockMvc for controllers, mocked repo for services) |
| **Maintainability** | Changing the data store doesn't affect controllers; changing HTTP contracts doesn't affect business logic |
| **Team Scalability** | Multiple developers can work on different layers without conflicts |

### 💡 Implementation Quality

The controllers are genuinely **thin** — they only:
1. Extract/parse request parameters
2. Delegate to the service
3. Wrap the result in `ResponseEntity`

There is **zero business logic** in any controller. This is exactly how Spring controllers should be written. Many projects fail here by putting validation, transformation, or persistence logic in controllers.

---

## 2. Repository Interface Abstraction

### 📍 Where: `ProductRepositoryInterface` + `ProductRepository`

```java
public interface ProductRepositoryInterface {
    List<Product> findAll();
    Optional<Product> findById(String id);
    Product save(Product product);
    void deleteById(String id);
    boolean existsById(String id);
    long count();
}
```

### 🤔 Why This Pattern?

This is the **Repository Pattern** combined with the **Dependency Inversion Principle (DIP)** — the "D" in SOLID:

> "High-level modules should not depend on low-level modules. Both should depend on abstractions."

The `ProductService` depends on the **interface**, not the JSON file implementation. This means:

- **Today:** JSON file storage → `ProductRepository`
- **Tomorrow:** PostgreSQL → `JpaProductRepository implements ProductRepositoryInterface`
- **Day after:** MongoDB → `MongoProductRepository implements ProductRepositoryInterface`

**Zero changes needed** in `ProductService`, `ProductController`, or any test that mocks the interface.

### 💡 Why It Matters

In a real production migration (e.g., moving from file-based to database), this abstraction would save days of refactoring. The interface also makes testing trivial — Mockito can mock `ProductRepositoryInterface` directly:

```java
@Mock
private ProductRepositoryInterface repository;
```

This is a **textbook implementation** of DIP that many production codebases lack.

---

## 3. Constructor Injection (DI)

### 📍 Where: Every Spring-managed class

```java
// ✅ Used in this project
public ProductService(ProductRepositoryInterface repository, ProductTemplateService templateService) {
    this.repository = repository;
    this.templateService = templateService;
}

// ❌ NOT used (field injection — anti-pattern)
@Autowired
private ProductRepositoryInterface repository;
```

### 🤔 Why Constructor Injection?

| Aspect | Constructor Injection | Field Injection |
|--------|----------------------|-----------------|
| **Immutability** | Fields can be `final` | Fields must be mutable |
| **Required deps** | Enforced at compile time | Fails at runtime (NPE) |
| **Testing** | Just pass mocks via constructor | Requires reflection or Spring context |
| **Thread safety** | Final fields are safely published | Mutable fields need synchronization |
| **Circular deps** | Detected immediately at startup | Silently hidden until runtime |

Constructor injection is **Spring's officially recommended approach** since Spring 4.3. The project follows this consistently across all classes:
- `ProductController(ProductService)`
- `ProductService(ProductRepositoryInterface, ProductTemplateService)`
- `WebMvcConfig(ApiKeyInterceptor)`
- `ApiKeyInterceptor(@Value, @Value, ObjectMapper)`
- `ProductRepository(@Value, @Value, ObjectMapper)`

This consistency demonstrates engineering discipline.

---

## 4. Lombok — Strategic Boilerplate Elimination

### 📍 Where: All model classes, services, controllers

### Annotations Used and Why

| Annotation | Where | What It Eliminates |
|------------|-------|--------------------|
| `@Getter` / `@Setter` | `Product`, `ProductFilter` | ~40 lines of getters/setters per class |
| `@Builder` | `Product`, `MeasurableValue`, `ErrorResponse`, `PageResponse` | Complex object construction with immutable-style API |
| `@Data` | `MeasurableValue`, `FieldDefinition`, `ProductTemplate` | `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` in one |
| `@NoArgsConstructor` | All model classes | Required by Jackson for deserialization |
| `@AllArgsConstructor` | All model classes | Required by `@Builder` |
| `@Slf4j` | All services, controllers, repository | `private static final Logger log = LoggerFactory.getLogger(...)` |

### 🤔 Why Not Just Use Java Records?

Java Records (Java 16+) are great for immutable DTOs but **don't support**:
- Mutable objects (needed for `Product` where fields are updated)
- `@Builder` pattern
- `@NoArgsConstructor` (required by Jackson)
- Inheritance

Lombok is the right choice for this codebase because `Product` is mutable (partial updates), requires Jackson deserialization, and benefits from the Builder pattern.

### 💡 Impact

Without Lombok, the `Product` class alone would be ~250 lines instead of ~85. Across 10+ model classes, Lombok saves an estimated **500+ lines** of pure boilerplate.

---

## 5. Builder Pattern

### 📍 Where: `Product`, `MeasurableValue`, `ErrorResponse`, `PageResponse`, `ProductComparisonResponse`, `ProductFilter`

```java
Product product = Product.builder()
    .name("Galaxy S24")
    .type("CELLPHONES")
    .price(MeasurableValue.builder().value(5999.99).unit("BRL").build())
    .rating(4.5)
    .build();
```

### 🤔 Why Builder Over Constructor?

`Product` has **11 fields**. A constructor with 11 parameters is:
- Unreadable: `new Product(null, "Galaxy", "desc", price, size, weight, "Black", "CELLPHONES", null, 4.5, specs)`
- Error-prone: Easy to swap two `String` parameters
- Inflexible: Adding a new field requires changing every call site

The Builder pattern solves all three:
- **Readable:** Named parameters make code self-documenting
- **Safe:** Wrong argument order is impossible
- **Extensible:** New fields don't break existing builders

### 💡 Especially Valuable in Tests

The 153+ test methods use builders extensively. Without builders, test setup code would be significantly more verbose and harder to read.

---

## 6. Optional for Nullable Returns

### 📍 Where: `ProductRepositoryInterface.findById()`, `ProductTemplateService.getTemplate()`

```java
Optional<Product> findById(String id);

// Usage:
return repository.findById(id)
    .orElseThrow(() -> new ProductNotFoundException("Product with ID " + id + " not found"));
```

### 🤔 Why Optional?

**Before Java 8 (anti-pattern):**
```java
Product product = repository.findById(id);
if (product == null) {  // Easy to forget this check!
    throw new ProductNotFoundException(...);
}
```

**With Optional:**
- The return type **declares the intent**: "this value might not exist"
- The compiler forces you to handle the absence case
- No `NullPointerException` risk — the API makes absence explicit
- Methods like `orElseThrow`, `map`, `flatMap` enable fluent handling

### 💡 Usage Quality

The project uses `Optional` correctly:
- `orElseThrow()` for required lookups (product by ID)
- `.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build())` for template lookup — elegant!
- Never calls `.get()` without checking — no `NoSuchElementException` risk

This follows **Josh Bloch's Effective Java Item 55**: "Return optionals judiciously."

---

## 7. Java Streams for Data Transformation

### 📍 Where: `ProductService.searchProducts()`, `ProductService.compareProducts()`, `ProductTemplateService`

```java
List<Product> filteredProducts = allProducts.stream()
    .filter(product -> {
        if (filter.getName() != null && !filter.getName().isEmpty()) {
            if (!product.getName().toLowerCase().contains(filter.getName().toLowerCase())) {
                return false;
            }
        }
        // ... more filters
        return true;
    })
    .toList();
```

### 🤔 Why Streams?

| Aspect | Imperative (for-loop) | Declarative (Stream) |
|--------|----------------------|---------------------|
| **Readability** | "How to do it" | "What to do" |
| **Parallelism** | Manual threading | `.parallelStream()` one-word change |
| **Composability** | Nested loops + temp vars | Chained operations |
| **Immutability** | Mutates a result list | Creates new collections |

### 💡 Consistent Usage

Streams are used consistently across the codebase:
- **Filtering:** `filter()` with multi-criteria predicates
- **Mapping:** `map(this::getProductById)` to resolve IDs to products
- **Collecting:** `Collectors.toList()`, `.toList()`
- **Aggregation:** `mapToLong(...).max()` for ID generation
- **Sorting:** `sorted()` for template key listing

---

## 8. YAML-Driven Configuration (Template Engine)

### 📍 Where: `ProductTemplateService`, `data/product-templates.yaml`

### 🔑 What It Is

Product types (CELLPHONES, COMPUTERS, etc.) are **not hardcoded in Java enums or classes**. They are defined in a YAML file and loaded at runtime.

### 🤔 Why This Pattern?

This is the **Data-Driven Design** pattern (also called **Configuration-Driven Architecture**):

| Benefit | How It Works Here |
|---------|------------------|
| **No recompilation** | Add a new product type by editing YAML — no Java changes needed |
| **Hot-reload** | `POST /templates/reload` reloads types at runtime without restart |
| **Non-developer friendly** | Product managers can define new types by editing YAML |
| **Separation of data from code** | Business rules (required fields, default units) live outside Java |

### 💡 Why It's Superior to a Java Enum

```java
// ❌ Enum approach — requires code change + rebuild + redeploy
public enum ProductType {
    CELLPHONES, COMPUTERS, CLOTHING, FOOD, BEVERAGES, FURNITURE, BOOKS, SPORTS
}

// ✅ YAML approach — change a file, call /reload
templates:
  CELLPHONES:
    display_name: "Smartphones"
    fields:
      price: { type: number, default_unit: BRL, required: true, comparable: true }
```

This is a **production-grade pattern** used by companies like Netflix (runtime configuration), feature flag systems, and content management platforms.

### 💡 Template Validation Integration

The templates don't just define types — they drive **validation logic**:
- Required fields are enforced from YAML `required: true`
- Specification keys are validated against the template
- Default units are auto-applied from `default_unit`
- Comparable fields for comparison are defined via `comparable: true`

This means the YAML file is the **single source of truth** for product type definitions.

---

## 9. MeasurableValue — Value Object Pattern

### 📍 Where: `MeasurableValue` class, used for `price`, `size`, `weight`

```java
public class MeasurableValue {
    private Double value;
    private String unit;
}
```

### 🤔 Why Not Just `Double price`?

This is the **Value Object** pattern from Domain-Driven Design (DDD):

| Problem with `Double price` | Solution with `MeasurableValue` |
|-----------------------------|--------------------------------|
| "Is 5999.99 in BRL, USD, or EUR?" | `{ value: 5999.99, unit: "BRL" }` — unambiguous |
| "Is 6.1 in inches or centimeters?" | `{ value: 6.1, unit: "inches" }` — self-describing |
| "How do I compare prices in different currencies?" | Unit-aware comparison is possible |
| "What's the default unit for CELLPHONES weight?" | Template defines `default_unit: "kg"` |

### 💡 Real-World Relevance

This pattern is used by:
- **Financial systems** — `Money(BigDecimal amount, Currency currency)`
- **Scientific computing** — `Measurement(double value, Unit unit)`
- **E-commerce platforms** — `Price(decimal, currency)`, `Weight(decimal, unit)`

The Mars Climate Orbiter was lost in 1999 because of a unit mismatch (pounds vs. newtons). The `MeasurableValue` pattern **prevents this class of bugs by design**.

### 💡 Auto-Applied Defaults

```java
// In ProductService.applyDefaultUnits():
if (product.getPrice().getUnit() == null && fields.containsKey("price")) {
    product.getPrice().setUnit(fields.get("price").getDefaultUnit());
}
```

If a client sends `{ "value": 5999.99 }` without a unit, the template's `default_unit` is automatically applied. This is a great UX/DX decision — clients don't need to know every type's default unit.

---

## 10. Global Exception Handler

### 📍 Where: `GlobalExceptionHandler` (187 lines)

### 🔑 What It Is

A `@RestControllerAdvice` that intercepts all exceptions thrown by controllers and services, converting them to structured `ErrorResponse` DTOs.

### 🤔 Why This Pattern?

**Without it:**
```json
{
    "timestamp": "2026-02-18T10:30:00",
    "status": 500,
    "error": "Internal Server Error",
    "trace": "java.lang.IllegalArgumentException: ...\n\tat com.meli.productapi.service...\n\tat..."
}
```

**With it:**
```json
{
    "timestamp": "2026-02-18T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Product name is required",
    "path": "/api/v1/products"
}
```

### 💡 Coverage of 8+ Exception Types

| Exception | HTTP Status | Category |
|-----------|-------------|----------|
| `HttpMessageNotReadableException` | 400 | Malformed JSON |
| `HttpMediaTypeNotSupportedException` | 400 | Wrong Content-Type |
| `MissingServletRequestParameterException` | 400 | Missing query param |
| `BindException` | 400 | Bean Validation + type conversion |
| `ConstraintViolationException` | 400 | Jakarta Validation |
| `MethodArgumentTypeMismatchException` | 400 | Wrong param type |
| `ProductNotFoundException` | 404 | Business logic |
| `IncompatibleProductTypesException` | 409 | Business logic |
| `IllegalArgumentException` | 400 | Business logic |
| `Exception` (catch-all) | 500 | Unexpected errors |

This is **comprehensive** — most Spring Boot projects handle 3-4 exception types at most. Having 10 specific handlers means clients always get meaningful, actionable error messages.

### 💡 `ErrorResponse.of()` and `ErrorResponse.ofValidation()` — Factory Methods

Using static factory methods instead of constructors is from **Effective Java Item 1**: "Consider static factory methods instead of constructors." The names describe intent:
- `of()` — simple error with message
- `ofValidation()` — error with field-level details

---

## 11. PageResponse — Generic Pagination Wrapper

### 📍 Where: `PageResponse<T>` (114 lines)

```java
public class PageResponse<T> {
    private List<T> content;
    private int page, pageSize, totalPages;
    private long totalElements;
    private boolean hasNext, hasPrevious;

    public static <T> PageResponse<T> of(List<T> allItems, int page, int pageSize) { ... }
    public static <T> PageResponse<T> ofAll(List<T> allItems) { ... }
}
```

### 🤔 Why This Pattern?

| Without Pagination | With PageResponse |
|-------------------|------------------|
| Returns 10,000 products in one response | Returns 20 per page |
| Frontend can't show "Page 1 of 50" | `totalPages`, `hasNext`, `hasPrevious` |
| Unpredictable response sizes | Consistent, bounded responses |
| No navigation metadata | Frontend can build paginator UI |

### 💡 Generic `<T>` Makes It Reusable

`PageResponse<T>` can paginate **any entity type** — products, orders, users, etc. It's not coupled to `Product`. This is a reusable infrastructure component.

### 💡 Two Factory Methods for Different Use Cases

- `of(list, page, pageSize)` — Standard paginated response
- `ofAll(list)` — When no pagination is requested, wraps all items as page 1

This dual API is thoughtful — it handles both `GET /products?page=1&pageSize=10` and `GET /products` (no pagination) elegantly.

---

## 12. API Key Interceptor — Chain of Responsibility

### 📍 Where: `ApiKeyInterceptor` implements `HandlerInterceptor`

### 🤔 Why an Interceptor (Not a Filter or Spring Security)?

| Approach | Complexity | Granularity | Best For |
|----------|-----------|-------------|----------|
| Servlet Filter | Low | All requests | CORS, logging |
| **HandlerInterceptor** | **Low** | **MVC endpoints** | **API key validation** |
| Spring Security | High | Full auth/authz | OAuth2, JWT, RBAC |

The interceptor is the **right level of abstraction** for simple API key auth:
- Lightweight — no Spring Security dependency overhead
- Configurable — path patterns defined in `WebMvcConfig`
- Excludable — Swagger endpoints are excluded
- Toggleable — `api.security.enabled` flag

### 💡 Smart Design Decisions

1. **CORS preflight bypass:** `OPTIONS` requests skip auth — prevents CORS failures
2. **Configurable security:** `@Value("${api.security.enabled:true}")` — disabled for tests
3. **Structured error response:** Returns `ErrorResponse` JSON (not plain text) on 401
4. **Swagger exclusion:** Documentation is accessible without API key

---

## 13. Servlet-Level CORS Filter

### 📍 Where: `CorsConfig` — `CorsFilter` bean

### 🤔 Why a Servlet Filter (Not `@CrossOrigin`)?

```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    // ...
    return new CorsFilter(source);
}
```

| Approach | Scope | Execution Order |
|----------|-------|-----------------|
| `@CrossOrigin` on controller | Per-controller | After interceptors |
| `WebMvcConfigurer.addCorsMappings()` | Global MVC | After interceptors |
| **`CorsFilter` (Servlet)** | **Global** | **Before everything** |

The servlet-level filter runs **before** the `ApiKeyInterceptor`. This is critical because:
1. Browser sends `OPTIONS` preflight request
2. `CorsFilter` adds CORS headers
3. `ApiKeyInterceptor` sees `OPTIONS` and bypasses auth
4. Browser receives CORS headers and proceeds with actual request

If CORS were configured at the MVC level, preflight requests might be rejected by the interceptor before CORS headers are set.

---

## 14. OpenAPI / Swagger Integration

### 📍 Where: `OpenApiConfig`, `@Operation`, `@Schema`, `@ApiResponse` annotations

### 🤔 Why SpringDoc OpenAPI?

The project uses `springdoc-openapi-starter-webmvc-ui` — the standard for Spring Boot 3.x OpenAPI support:
- **Auto-generated API docs** from code annotations
- **Interactive Swagger UI** at `/docs`
- **API key security scheme** configured — Swagger UI shows the "Authorize" button
- **Rich examples** — `@ExampleObject` provides realistic JSON payloads
- **Schema descriptions** — Every field in every model is documented

### 💡 Documentation-as-Code

Instead of maintaining a separate API documentation, the documentation **lives in the code**:

```java
@Schema(description = "Product price with currency unit", requiredMode = Schema.RequiredMode.REQUIRED)
private MeasurableValue price;
```

This ensures documentation and code are **always in sync** — a fundamental principle of good API design.

---

## 15. Thread Safety with ReentrantLock

### 📍 Where: `ProductRepository.save()` and `deleteById()`

```java
private final ReentrantLock lock = new ReentrantLock();

public Product save(Product product) {
    lock.lock();
    try {
        // read → modify → write
    } finally {
        lock.unlock();
    }
}
```

### 🤔 Why Not `synchronized`?

| Feature | `synchronized` | `ReentrantLock` |
|---------|---------------|-----------------|
| Try-lock | ❌ | ✅ `tryLock(timeout)` |
| Fairness | ❌ | ✅ `new ReentrantLock(true)` |
| Multiple conditions | ❌ | ✅ `lock.newCondition()` |
| Interruptible | ❌ | ✅ `lockInterruptibly()` |
| Lock scope | Method/block | Any scope |

`ReentrantLock` was chosen because:
1. It's more **explicit** — `lock()`/`unlock()` in try-finally makes the critical section visible
2. It's **upgradeable** — can switch to `ReadWriteLock` later without API change
3. It's the **recommended approach** for concurrent file access in Java

---

## 16. ConcurrentHashMap for Template Cache

### 📍 Where: `ProductTemplateService`

```java
private volatile Map<String, ProductTemplate> templates = new ConcurrentHashMap<>();
```

### 🤔 Why `ConcurrentHashMap` + `volatile`?

- **`ConcurrentHashMap`:** Thread-safe reads without locking — multiple threads can look up templates simultaneously
- **`volatile`:** Ensures that when `reloadTemplates()` assigns a new map, all threads see the update immediately (visibility guarantee)

This is a **publish-subscribe cache pattern** — the map reference itself is volatile (for atomic swap during reload), while the map contents are thread-safe for concurrent reads.

---

## 17. Test Architecture

### 📍 Where: 14 test files, 153+ test methods

### 🔑 Multi-Layer Testing Strategy

```
├── Integration Tests     → @SpringBootTest + MockMvc (full stack)
├── Controller Tests      → @WebMvcTest + MockMvc (HTTP layer only)
├── Service Tests         → @ExtendWith(Mockito) (business logic only)
├── Repository Tests      → @TempDir (file system isolation)
├── Model Tests           → Plain JUnit 5 (DTOs, value objects)
└── Exception Tests       → @WebMvcTest (error handling)
```

### 🤔 Why This Pyramid?

This follows the **Test Pyramid** pattern:

```
         /  Integration  \      ← Few, slow, high confidence
        /   Controller    \     ← Medium, verify HTTP contracts
       /     Service       \    ← Many, fast, core business logic
      /   Model + Repo      \   ← Many, fast, foundational
```

Each layer tests a **specific concern**:
- **Service tests** verify business rules without HTTP overhead
- **Controller tests** verify HTTP mapping without service logic
- **Integration tests** verify the full stack works together

### 💡 Notable Testing Practices

| Practice | Where | Why |
|----------|-------|-----|
| `@DisplayName` | All test methods | Human-readable test reports |
| `@ParameterizedTest` + `@MethodSource` | `ProductServiceTest`, `ProductControllerTest` | Tests multiple inputs without duplication |
| `@TempDir` | `ProductRepositoryTest` | Isolated file system per test — no test pollution |
| `@MockBean` | Controller tests | Replaces Spring beans with mocks |
| `@BeforeEach` cleanup | Integration tests | Ensures test independence |
| `verify()` | Service tests | Confirms interactions happened |

---

## 18. Defensive Programming

### 📍 Where: Throughout the codebase

### Key Defensive Patterns

**1. Null-safe comparisons:**
```java
if (!products.stream().allMatch(p -> Objects.equals(p.getType(), firstType)))
```
Uses `Objects.equals()` instead of `.equals()` — safe when either value is null.

**2. Input normalization:**
```java
product.setType(product.getType().toUpperCase().trim());
```
Normalizes user input before storage — `"cellphones"`, `"Cellphones"`, `"  CELLPHONES  "` all become `"CELLPHONES"`.

**3. Case-insensitive matching:**
```java
templates.get(typeName.toUpperCase());
```
Template lookups are case-insensitive — flexible for API consumers.

**4. Default values:**
```java
if (product.getRating() == null) product.setRating(0.0);
if (product.getImageUrl() == null) product.setImageUrl(generateRandomImageUrl());
```
Missing optional fields get sensible defaults — the API never returns incomplete data.

**5. Unmodifiable collections:**
```java
return Collections.unmodifiableMap(templates);
return Collections.unmodifiableSet(allKeys);
```
Prevents callers from modifying internal state — a key principle of encapsulation.

**6. ObjectMapper copy:**
```java
this.objectMapper = objectMapper.copy();
```
The repository copies the injected `ObjectMapper` to prevent its serialization settings from affecting the global Spring MVC `ObjectMapper`.

---

## 19. Logging Strategy

### 📍 Where: All services, controllers, repository, interceptor

### Logging Level Usage

| Level | When Used | Example |
|-------|-----------|---------|
| `DEBUG` | Read operations, lookups, filter details | `log.debug("Loaded {} products from file", products.size())` |
| `INFO` | Write operations, lifecycle events | `log.info("Product created with ID: {}", saved.getId())` |
| `WARN` | Validation failures, expected errors | `log.warn("Product with ID {} not found", id)` |
| `ERROR` | Unexpected errors, infrastructure failures | `log.error("Error reading products: {}", e.getMessage())` |

### 🤔 Why These Levels?

- **DEBUG** for reads: High-volume, useful for debugging, disabled in production
- **INFO** for writes: Important state changes, always visible
- **WARN** for business errors: Expected but noteworthy (404s, validation failures)
- **ERROR** for infrastructure: Needs immediate attention (file I/O failures)

### 💡 Parameterized Logging

```java
log.debug("Searching products with filters: name={}, type={}, priceMin={}, priceMax={}",
    filter.getName(), filter.getType(), filter.getPriceMin(), filter.getPriceMax());
```

Uses SLF4J's `{}` placeholders instead of string concatenation — no performance penalty when DEBUG is disabled (the string is never built).

---

## 20. Patterns Summary Matrix

| Pattern | Java Concept | Where Applied | SOLID Principle |
|---------|-------------|---------------|-----------------|
| Layered Architecture | Package structure | Entire project | SRP, DIP |
| Repository Pattern | Interface + Implementation | `ProductRepositoryInterface` | DIP, OCP |
| Constructor Injection | Spring DI | All Spring beans | DIP |
| Builder Pattern | Lombok `@Builder` | All model classes | — |
| Value Object | `MeasurableValue` | price, size, weight | SRP |
| Factory Method | `ErrorResponse.of()`, `PageResponse.of()` | Error/Pagination DTOs | — |
| Template Method | YAML-driven validation | `ProductTemplateService` | OCP |
| Chain of Responsibility | `HandlerInterceptor` | `ApiKeyInterceptor` | SRP |
| Strategy Pattern | Filter predicates in streams | `searchProducts()` | OCP |
| Singleton (Spring) | `@Service`, `@Repository` | All Spring beans | — |
| Data-Driven Design | YAML templates | Product type definitions | OCP |
| Defensive Copying | `objectMapper.copy()`, `Collections.unmodifiable*` | Repository, TemplateService | Encapsulation |
| Generic Types | `PageResponse<T>` | Pagination wrapper | Reusability |

### SOLID Principles Scorecard

| Principle | Score | Evidence |
|-----------|-------|----------|
| **S** — Single Responsibility | ⭐⭐⭐⭐ | Clean layer separation; `ProductService` could be further split |
| **O** — Open/Closed | ⭐⭐⭐⭐⭐ | New product types via YAML without code changes |
| **L** — Liskov Substitution | ⭐⭐⭐⭐⭐ | `ProductRepositoryInterface` implementations are interchangeable |
| **I** — Interface Segregation | ⭐⭐⭐⭐ | Repository interface is focused; no unnecessary methods |
| **D** — Dependency Inversion | ⭐⭐⭐⭐⭐ | Service depends on interface, not implementation |

---

> **Bottom line:** This codebase demonstrates a strong command of Java ecosystem patterns and Spring Boot best practices. The combination of YAML-driven configuration, Repository abstraction, comprehensive error handling, and a 153+ test suite makes this project stand out as a well-engineered, maintainable solution. The patterns chosen are not arbitrary — each one solves a specific problem and follows established Java/Spring conventions.
