# TODO — Phase 1 Improvements

> Checklist based on [plan.md](plan.md). Mark `[x]` as each item is completed.

---

## 1. 📖 Swagger / OpenAPI Documentation _(1st — zero risk)_

### 1.1 Setup
- [x] Add `springdoc-openapi-starter-webmvc-ui` (v2.3.0) dependency to `pom.xml`
- [x] Add Swagger/OpenAPI properties to `src/main/resources/application.properties`
- [x] Create `src/main/java/com/meli/productapi/config/OpenApiConfig.java` (info, security scheme, contact)

### 1.2 Controller Annotations
- [x] Annotate `ProductController.java` — `@Operation`, `@ApiResponse`, `@Parameter` on all endpoints
- [x] Annotate `TemplateController.java` — `@Operation`, `@ApiResponse` on all endpoints

### 1.3 Model Annotations (optional, improves docs)
- [x] Add `@Schema` to `Product.java` fields
- [x] Add `@Schema` / `@Parameter` to `ProductFilter.java`
- [x] Add `@Schema` to `ErrorResponse.java`

### 1.4 Validation
- [x] Swagger UI accessible at `http://localhost:8080/swagger-ui.html`
- [x] OpenAPI spec accessible at `http://localhost:8080/api-docs`
- [x] All endpoints listed with descriptions
- [x] `mvn clean test` — all existing tests still passing

---

## 2. 🔐 API Key Security via Header _(2nd — low risk)_

### 2.1 Configuration
- [x] Add `api.security.enabled=true` and `api.security.key=meli-product-api-key-2025` to `src/main/resources/application.properties`
- [x] Add `api.security.enabled=false` to `src/test/resources/application.properties`

### 2.2 Implementation
- [x] Create `src/main/java/com/meli/productapi/config/ApiKeyInterceptor.java`
  - Validates `X-API-KEY` header
  - Bypasses `OPTIONS` requests (CORS preflight)
  - Returns 401 `ErrorResponse` JSON if missing/invalid
  - Respects `api.security.enabled` flag
- [x] Create `src/main/java/com/meli/productapi/config/WebMvcConfig.java`
  - Registers interceptor on `/api/**`
  - Excludes `/swagger-ui/**`, `/api-docs/**`, `/swagger-ui.html`
- [x] Add 401 handler to `GlobalExceptionHandler.java` (for `AccessDeniedException` or custom exception)

### 2.3 Tests
- [x] Create `src/test/java/com/meli/productapi/config/ApiKeyInterceptorTest.java`:
  - [x] `testValidApiKey_ShouldAllowRequest`
  - [x] `testMissingApiKey_ShouldReturn401`
  - [x] `testInvalidApiKey_ShouldReturn401`
  - [x] `testOptionsRequest_ShouldBypass`
  - [x] `testSecurityDisabled_ShouldAllowWithoutKey`

### 2.4 Validation
- [x] `curl -H "X-API-KEY: meli-product-api-key-2025" http://localhost:8080/api/v1/products` → 200
- [x] `curl http://localhost:8080/api/v1/products` → 401
- [x] `curl -H "X-API-KEY: wrong-key" http://localhost:8080/api/v1/products` → 401
- [x] `mvn clean test` — all tests passing (existing unaffected + 5 new)

---

## 3. 📄 Pagination Metadata on `GET /api/v1/products` _(3rd — medium risk)_

### 3.1 New DTO
- [x] Create `src/main/java/com/meli/productapi/model/PageResponse.java`
  - Generic `PageResponse<T>` with: `content`, `page`, `pageSize`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`
  - `of(List<T>, int page, int pageSize)` factory method
  - `ofAll(List<T>)` factory method (non-paginated)
  - Add `@Schema` annotations for Swagger

### 3.2 Service & Controller Changes
- [x] Update `ProductService.searchProducts()` to return `PageResponse<Product>`
- [x] Remove or simplify private `applyPagination()` method
- [x] Update `ProductController.getProducts()` to return `ResponseEntity<PageResponse<Product>>`

### 3.3 Tests — New
- [x] Create `src/test/java/com/meli/productapi/model/PageResponseTest.java`:
  - [x] `testOfWithPagination_ShouldReturnCorrectMetadata`
  - [x] `testOfWithPageBeyondTotal_ShouldReturnEmptyContent`
  - [x] `testOfAll_ShouldReturnAllItemsWithSinglePage`
  - [x] `testHasNext_ShouldBeTrueWhenMorePagesExist`
  - [x] `testHasPrevious_ShouldBeTrueWhenNotFirstPage`
  - [x] `testOfWithEmptyList_ShouldReturnEmptyPageResponse`

### 3.4 Tests — Update Existing (~15-20 assertions)
- [x] Update `ProductServiceTest.java` — `searchProducts()` now returns `PageResponse<Product>`
- [x] Update `ProductControllerTest.java` — `jsonPath("$.content", ...)`, `jsonPath("$.totalElements", ...)`
- [x] Update `ProductApiIntegrationTest.java` — same adjustments for new response format

### 3.5 Validation
- [x] `GET /api/v1/products` returns `PageResponse` wrapper (even without pagination params)
- [x] `GET /api/v1/products?page=1&pageSize=2` returns correct metadata
- [x] `hasNext` / `hasPrevious` are accurate
- [x] `totalElements` and `totalPages` are accurate
- [x] `mvn clean test` — all tests passing (modified + ~6 new)

---

## 4. 🏁 Final Validation

- [x] `mvn clean test` — **all tests green** (existing + ~16 new) → **182 tests, 0 failures**
- [x] Swagger UI shows all endpoints with descriptions + "Authorize" button for API Key
- [x] API Key correctly blocks/allows requests
- [x] Pagination metadata present and correct in all `GET /products` responses
- [x] No regressions in any existing functionality
