# 📋 Implementation Plan — Phase 1 Improvements

> **Objetivo:** Implementar 3 melhorias identificadas na análise do projeto:
> 1. Camada de segurança leve via API Key Header
> 2. Documentação Swagger / OpenAPI
> 3. Metadata de paginação no `GET /api/v1/products`

---

## 1. 🔐 Camada de Segurança — API Key via Header

### Contexto
Atualmente a API não possui nenhuma camada de autenticação. Qualquer cliente pode acessar todos os endpoints sem restrição. A proposta é adicionar uma validação simples via header `X-API-KEY` — leve o suficiente para proteger a API sem a complexidade de JWT/OAuth2.

### Estratégia
Usar um **Spring `HandlerInterceptor`** (não Spring Security) para manter a dependência mínima. O interceptor valida a presença e valor do header `X-API-KEY` em todas as requisições, exceto endpoints públicos configuráveis.

### Arquivos a Criar

| Arquivo | Descrição |
|---------|-----------|
| `src/main/java/com/meli/productapi/config/ApiKeyInterceptor.java` | Interceptor que valida o header `X-API-KEY` |
| `src/main/java/com/meli/productapi/config/WebMvcConfig.java` | Registra o interceptor no Spring MVC |
| `src/test/java/com/meli/productapi/config/ApiKeyInterceptorTest.java` | Testes unitários do interceptor |

### Arquivos a Modificar

| Arquivo | Mudança |
|---------|---------|
| `src/main/resources/application.properties` | Adicionar `api.security.key` e `api.security.enabled` |
| `src/test/resources/application.properties` | Configurar key de teste ou desabilitar segurança nos testes |
| `src/main/java/com/meli/productapi/exception/GlobalExceptionHandler.java` | Adicionar handler para `AccessDeniedException` (401) |

### Detalhamento Técnico

#### 1.1 — `ApiKeyInterceptor.java`
```java
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("${api.security.key}")
    private String apiKey;

    @Value("${api.security.enabled:true}")
    private boolean securityEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Se segurança desabilitada, permite tudo
        // Se método OPTIONS (CORS preflight), permite
        // Valida header "X-API-KEY" contra api.security.key
        // Retorna 401 com ErrorResponse JSON se inválido
    }
}
```

**Regras:**
- Header esperado: `X-API-KEY`
- Se ausente ou inválido → HTTP 401 (`Unauthorized`) com body `ErrorResponse`
- Requisições `OPTIONS` (CORS preflight) são liberadas
- Flag `api.security.enabled=false` desabilita a verificação (útil para dev/testes)
- A key é configurada via `application.properties` (não hardcoded)

#### 1.2 — `WebMvcConfig.java`
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;

    // Constructor injection

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**");       // Protege todos endpoints /api/**
    }
}
```

#### 1.3 — `application.properties`
```properties
# Security
api.security.enabled=true
api.security.key=meli-product-api-key-2025
```

#### 1.4 — `application.properties` (test)
```properties
# Desabilitar segurança nos testes existentes para não quebrá-los
api.security.enabled=false
```

> **Alternativa:** Em vez de desabilitar, configurar uma key de teste e adicioná-la nos testes via `MockMvc.header("X-API-KEY", "test-key")`. Porém isso exigiria modificar TODOS os testes existentes. A abordagem `enabled=false` é mais segura para não quebrar os 92+ testes.

#### 1.5 — `GlobalExceptionHandler` — Novo handler
```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    ErrorResponse response = ErrorResponse.of(
        HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
        ex.getMessage(), extractPath(request));
    return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
}
```
> Nota: Usar uma custom exception `ApiKeyUnauthorizedException` ou a exceção padrão — a decisão será feita na implementação.

#### 1.6 — Testes do Interceptor
```
ApiKeyInterceptorTest:
├── testValidApiKey_ShouldAllowRequest
├── testMissingApiKey_ShouldReturn401
├── testInvalidApiKey_ShouldReturn401
├── testOptionsRequest_ShouldBypass
├── testSecurityDisabled_ShouldAllowWithoutKey
```

### Exemplo de Uso (curl)
```bash
# ✅ Com API Key válida
curl -H "X-API-KEY: meli-product-api-key-2025" http://localhost:8080/api/v1/products

# ❌ Sem API Key
curl http://localhost:8080/api/v1/products
# → 401 {"status":401,"error":"Unauthorized","message":"Missing or invalid API Key"}

# ❌ Com API Key inválida
curl -H "X-API-KEY: wrong-key" http://localhost:8080/api/v1/products
# → 401 {"status":401,"error":"Unauthorized","message":"Missing or invalid API Key"}
```

### Impacto nos Testes Existentes
- **Nenhum** — `api.security.enabled=false` no `application.properties` de teste garante que todos os 92+ testes continuam passando sem modificação.
- Os testes **novos** (`ApiKeyInterceptorTest`) testarão o interceptor isoladamente com `securityEnabled=true`.

---

## 2. 📖 Swagger / OpenAPI Documentation

### Contexto
A API não possui documentação interativa. O README documenta os endpoints manualmente, o que fica desatualizado facilmente. A adição do Swagger UI permite documentação auto-gerada, testável pelo navegador, e exportável como spec OpenAPI 3.0.

### Estratégia
Usar a biblioteca **`springdoc-openapi`** (padrão para Spring Boot 3.x). Apenas adicionar a dependência e anotar os controllers — a maior parte da documentação é gerada automaticamente.

### Arquivos a Criar

| Arquivo | Descrição |
|---------|-----------|
| `src/main/java/com/meli/productapi/config/OpenApiConfig.java` | Configuração global do OpenAPI (info, security scheme) |

### Arquivos a Modificar

| Arquivo | Mudança |
|---------|---------|
| `pom.xml` | Adicionar dependência `springdoc-openapi-starter-webmvc-ui` |
| `src/main/resources/application.properties` | Configurar path do Swagger UI |
| `src/main/java/com/meli/productapi/controller/ProductController.java` | Adicionar `@Operation`, `@ApiResponse`, `@Parameter` |
| `src/main/java/com/meli/productapi/controller/TemplateController.java` | Adicionar `@Operation`, `@ApiResponse` |
| `src/main/java/com/meli/productapi/model/Product.java` | Adicionar `@Schema` nos campos (opcional, melhora docs) |
| `src/main/java/com/meli/productapi/model/ProductFilter.java` | Adicionar `@Schema` e `@Parameter` |
| `src/main/java/com/meli/productapi/exception/ErrorResponse.java` | Adicionar `@Schema` para aparecer nos exemplos de erro |

### Detalhamento Técnico

#### 2.1 — `pom.xml` — Nova dependência
```xml
<!-- SpringDoc OpenAPI (Swagger UI) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

#### 2.2 — `application.properties`
```properties
# Swagger / OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
```

#### 2.3 — `OpenApiConfig.java`
```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Product API — Meli Project")
                .description("REST API for product management and comparison")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Bruno Melo")
                    .url("https://github.com/brunomelo")))
            .addSecurityItem(new SecurityRequirement().addList("API Key"))
            .components(new Components()
                .addSecuritySchemes("API Key",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-KEY")
                        .description("API Key for authentication")));
    }
}
```

> Isso faz o Swagger UI mostrar o botão "Authorize" onde o usuário pode inserir a API Key para testar os endpoints protegidos.

#### 2.4 — Anotações no `ProductController.java`

Cada endpoint recebe anotações descritivas:

```java
@Operation(
    summary = "List products with filters",
    description = "Search and filter products by name, type, price range, and specifications. Supports pagination."
)
@ApiResponse(responseCode = "200", description = "Products retrieved successfully")
@ApiResponse(responseCode = "400", description = "Invalid filter parameters")
@GetMapping
public ResponseEntity<PageResponse<Product>> getProducts(...) { ... }
```

```java
@Operation(summary = "Get product by ID")
@ApiResponse(responseCode = "200", description = "Product found")
@ApiResponse(responseCode = "404", description = "Product not found")
@GetMapping("/{id}")
public ResponseEntity<Product> getProductById(@PathVariable String id) { ... }
```

```java
@Operation(summary = "Create a new product")
@ApiResponse(responseCode = "201", description = "Product created successfully")
@ApiResponse(responseCode = "400", description = "Invalid product data")
@PostMapping
public ResponseEntity<Product> createProduct(...) { ... }
```

```java
@Operation(summary = "Update an existing product")
@ApiResponse(responseCode = "200", description = "Product updated successfully")
@ApiResponse(responseCode = "404", description = "Product not found")
@PutMapping("/{id}")
public ResponseEntity<Product> updateProduct(...) { ... }
```

```java
@Operation(summary = "Delete a product")
@ApiResponse(responseCode = "204", description = "Product deleted successfully")
@ApiResponse(responseCode = "404", description = "Product not found")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteProduct(...) { ... }
```

```java
@Operation(summary = "Compare products by IDs")
@ApiResponse(responseCode = "200", description = "Comparison result")
@ApiResponse(responseCode = "400", description = "Invalid IDs or filters")
@ApiResponse(responseCode = "409", description = "Products are of different types")
@GetMapping("/compare")
public ResponseEntity<ProductComparisonResponse> compareProducts(...) { ... }
```

#### 2.5 — Anotações no `TemplateController.java`
Mesma abordagem — `@Operation` + `@ApiResponse` em cada endpoint.

#### 2.6 — Liberar Swagger da API Key
O interceptor (`WebMvcConfig`) deve **excluir** os paths do Swagger:
```java
registry.addInterceptor(apiKeyInterceptor)
    .addPathPatterns("/api/**")
    .excludePathPatterns("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html");
```

### URLs Disponíveis Após Implementação
| URL | Descrição |
|-----|-----------|
| `http://localhost:8080/swagger-ui.html` | Interface interativa do Swagger |
| `http://localhost:8080/api-docs` | JSON spec do OpenAPI 3.0 |

### Impacto nos Testes Existentes
- **Nenhum** — springdoc não interfere nos testes `@WebMvcTest` ou `@SpringBootTest` existentes. As anotações `@Operation`/`@ApiResponse` são puramente declarativas.

---

## 3. 📄 Pagination Metadata no `GET /api/v1/products`

### Contexto
Atualmente o endpoint retorna uma lista raw de produtos (`List<Product>`). O cliente não recebe informações de paginação (total de itens, total de páginas, se há próxima página, etc.), impossibilitando a construção de controles de paginação no frontend.

### Estratégia
Criar um DTO genérico `PageResponse<T>` que encapsula os resultados com metadata de paginação. O `ProductService.searchProducts()` passa a retornar `PageResponse<Product>` em vez de `List<Product>`.

### Arquivos a Criar

| Arquivo | Descrição |
|---------|-----------|
| `src/main/java/com/meli/productapi/model/PageResponse.java` | DTO genérico de resposta paginada |
| `src/test/java/com/meli/productapi/model/PageResponseTest.java` | Testes unitários do DTO |

### Arquivos a Modificar

| Arquivo | Mudança |
|---------|---------|
| `src/main/java/com/meli/productapi/service/ProductService.java` | `searchProducts()` retorna `PageResponse<Product>` |
| `src/main/java/com/meli/productapi/controller/ProductController.java` | `getProducts()` retorna `ResponseEntity<PageResponse<Product>>` |
| `src/test/java/com/meli/productapi/controller/ProductControllerTest.java` | Ajustar assertions para novo formato |
| `src/test/java/com/meli/productapi/service/ProductServiceTest.java` | Ajustar assertions para `PageResponse` |
| `src/test/java/com/meli/productapi/ProductApiIntegrationTest.java` | Ajustar assertions para novo formato |

### Detalhamento Técnico

#### 3.1 — `PageResponse.java`
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response wrapper with metadata")
public class PageResponse<T> {

    @Schema(description = "List of items in the current page")
    private List<T> content;

    @Schema(description = "Current page number (1-based)", example = "1")
    private int page;

    @Schema(description = "Number of items per page", example = "10")
    private int pageSize;

    @Schema(description = "Total number of items matching the filter", example = "47")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Whether there is a next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there is a previous page", example = "false")
    private boolean hasPrevious;

    /**
     * Factory method to create a PageResponse from a full list + pagination params.
     */
    public static <T> PageResponse<T> of(List<T> allItems, int page, int pageSize) {
        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);

        List<T> content = (startIndex >= totalElements)
                ? List.of()
                : allItems.subList(startIndex, endIndex);

        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrevious(page > 1)
                .build();
    }

    /**
     * Factory method for non-paginated responses (all results).
     */
    public static <T> PageResponse<T> ofAll(List<T> items) {
        return PageResponse.<T>builder()
                .content(items)
                .page(1)
                .pageSize(items.size())
                .totalElements(items.size())
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
```

#### 3.2 — Mudança no `ProductService.searchProducts()`

**Antes:**
```java
public List<Product> searchProducts(ProductFilter filter) {
    // ... filtragem ...
    List<Product> result = applyPagination(filteredProducts, filter.getPage(), filter.getPageSize());
    return result;
}
```

**Depois:**
```java
public PageResponse<Product> searchProducts(ProductFilter filter) {
    // ... filtragem (mesma lógica) ...

    // Paginação com metadata
    if (filter.getPageSize() != null) {
        int page = (filter.getPage() != null && filter.getPage() > 0) ? filter.getPage() : 1;
        return PageResponse.of(filteredProducts, page, filter.getPageSize());
    }

    // Sem paginação — retorna tudo
    return PageResponse.ofAll(filteredProducts);
}
```

> O método privado `applyPagination()` pode ser removido ou simplificado, já que a lógica de fatiamento agora vive no `PageResponse.of()`.

#### 3.3 — Mudança no `ProductController.getProducts()`

**Antes:**
```java
@GetMapping
public ResponseEntity<List<Product>> getProducts(...) {
    List<Product> products = productService.searchProducts(filter);
    return ResponseEntity.ok(products);
}
```

**Depois:**
```java
@GetMapping
public ResponseEntity<PageResponse<Product>> getProducts(...) {
    PageResponse<Product> response = productService.searchProducts(filter);
    return ResponseEntity.ok(response);
}
```

#### 3.4 — Formato da Resposta

**Antes:**
```json
[
  { "id": "1", "name": "iPhone 15", "price": { "value": 4999.99, "unit": "BRL" } },
  { "id": "2", "name": "Galaxy S24", "price": { "value": 3999.99, "unit": "BRL" } }
]
```

**Depois (com `?page=1&pageSize=2`):**
```json
{
  "content": [
    { "id": "1", "name": "iPhone 15", "price": { "value": 4999.99, "unit": "BRL" } },
    { "id": "2", "name": "Galaxy S24", "price": { "value": 3999.99, "unit": "BRL" } }
  ],
  "page": 1,
  "pageSize": 2,
  "totalElements": 15,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

**Depois (sem paginação — `GET /api/v1/products`):**
```json
{
  "content": [
    { "id": "1", "name": "iPhone 15" },
    { "id": "2", "name": "Galaxy S24" }
  ],
  "page": 1,
  "pageSize": 15,
  "totalElements": 15,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

> A resposta **sempre** vem no formato `PageResponse`, mesmo sem paginação, garantindo consistência para o cliente.

#### 3.5 — Ajustes nos Testes

**`ProductServiceTest`** — Os testes que chamam `searchProducts()` agora recebem `PageResponse<Product>`:
```java
// Antes
List<Product> result = productService.searchProducts(filter);
assertEquals(2, result.size());

// Depois
PageResponse<Product> result = productService.searchProducts(filter);
assertEquals(2, result.getContent().size());
assertEquals(2, result.getTotalElements());
```

**`ProductControllerTest`** — Assertions no MockMvc mudam para navegar pelo JSON:
```java
// Antes
mockMvc.perform(get("/api/v1/products"))
    .andExpect(jsonPath("$", hasSize(3)));

// Depois
mockMvc.perform(get("/api/v1/products"))
    .andExpect(jsonPath("$.content", hasSize(3)))
    .andExpect(jsonPath("$.totalElements").value(3))
    .andExpect(jsonPath("$.page").value(1));
```

**`ProductApiIntegrationTest`** — Mesma lógica de ajuste.

#### 3.6 — Testes do `PageResponse`
```
PageResponseTest:
├── testOfWithPagination_ShouldReturnCorrectMetadata
├── testOfWithPageBeyondTotal_ShouldReturnEmptyContent
├── testOfAll_ShouldReturnAllItemsWithSinglePage
├── testHasNext_ShouldBeTrueWhenMorePagesExist
├── testHasPrevious_ShouldBeTrueWhenNotFirstPage
├── testOfWithEmptyList_ShouldReturnEmptyPageResponse
```

### Impacto nos Testes Existentes
- **Médio** — Todos os testes que validam a resposta de `GET /api/v1/products` e chamadas a `searchProducts()` precisam ser ajustados para o novo formato. Estimativa: ~15-20 assertions a modificar.

---

## 📅 Ordem de Implementação Recomendada

| Ordem | Feature | Justificativa |
|-------|---------|---------------|
| **1º** | Swagger / OpenAPI | Não modifica código existente — apenas adiciona anotações. Zero risco de quebrar testes. |
| **2º** | API Key Security | Impacto baixo — novo interceptor + config. Testes existentes protegidos por `enabled=false`. |
| **3º** | Pagination Metadata | Maior impacto — muda o contrato da API e exige ajuste em ~15-20 testes. Implementar por último. |

---

## ✅ Checklist de Validação

Após cada implementação, validar:

- [ ] `mvn clean test` — Todos os testes passando (92+ existentes + novos)
- [ ] `curl` manual nos endpoints com cenários de sucesso e erro
- [ ] Swagger UI acessível em `http://localhost:8080/swagger-ui.html`
- [ ] Todos endpoints aparecem no Swagger com descrições
- [ ] API Key bloqueando requests sem header
- [ ] API Key permitindo requests com header válido
- [ ] `GET /api/v1/products` retornando `PageResponse` com metadata
- [ ] Paginação correta: `hasNext`, `hasPrevious`, `totalPages`, `totalElements`
- [ ] Sem paginação: response no formato `PageResponse` com `totalPages: 1`

---

## 📊 Resumo de Impacto

| Métrica | Antes | Depois |
|---------|-------|--------|
| **Arquivos novos** | — | 4 (`PageResponse`, `OpenApiConfig`, `ApiKeyInterceptor`, `WebMvcConfig`) |
| **Arquivos modificados** | — | ~10 (controllers, service, properties, pom, testes) |
| **Dependências novas** | 0 | 1 (`springdoc-openapi-starter-webmvc-ui`) |
| **Testes novos** | 0 | ~16 (interceptor: 5, PageResponse: 6, integration: 5) |
| **Testes modificados** | 0 | ~15-20 assertions (para PageResponse) |
| **Segurança** | ⭐☆☆☆☆ | ⭐⭐⭐☆☆ (API Key header) |
| **Documentação** | Manual (README) | Auto-gerada (Swagger UI + OpenAPI spec) |
| **UX do cliente** | Lista raw sem metadata | `PageResponse` com metadata completa |
