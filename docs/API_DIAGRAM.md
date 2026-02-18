# 📊 Product API — Architecture & Flow Diagrams

> Spring Boot 3.2.0 REST API for product management and comparison. Java 17, Maven, JSON file persistence, YAML-driven product templates.

---

## 🔄 Request Flow & Architecture

```mermaid
flowchart TB
    Client["🌐 Client"]

    subgraph HTTP["🔐 HTTP Pipeline"]
        CORS["CorsFilter"] --> APIKey["ApiKeyInterceptor\n(X-API-KEY)"]
    end

    subgraph Controllers["🎮 Controllers"]
        PC["ProductController\n/api/v1/products"]
        TC["TemplateController\n/api/v1/templates"]
    end

    subgraph Services["⚙️ Services"]
        PS["ProductService\nCRUD · Filters · Comparison"]
        PTS["ProductTemplateService\nYAML loading · Validation · Hot-reload"]
    end

    subgraph Data["💾 Persistence"]
        PR["ProductRepository\nReentrantLock · Auto-ID"]
        JSON[("products.json")]
        YAML[("product-templates.yaml")]
    end

    GEH["⚠️ GlobalExceptionHandler"]

    Client -->|"+ X-API-KEY"| HTTP
    APIKey -->|"✅"| Controllers
    APIKey -->|"❌ 401"| Client
    PC --> PS
    TC --> PTS
    PS --> PTS
    PS --> PR
    PR --> JSON
    PTS --> YAML
    Controllers & Services -.->|"exceptions"| GEH
    GEH -->|"ErrorResponse JSON"| Client

    style HTTP fill:#fff3e0,stroke:#e65100
    style Controllers fill:#e3f2fd,stroke:#1565c0
    style Services fill:#e8f5e9,stroke:#2e7d32
    style Data fill:#f3e5f5,stroke:#6a1b9a
```

---

## 📡 Endpoints

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| `GET` | `/api/v1/products` | List/search + filters + pagination | `200` |
| `GET` | `/api/v1/products/{id}` | Get by ID | `200` / `404` |
| `POST` | `/api/v1/products` | Create product | `201` |
| `PUT` | `/api/v1/products/{id}` | Update product | `200` / `404` |
| `DELETE` | `/api/v1/products/{id}` | Delete product | `204` / `404` |
| `GET` | `/api/v1/products/stats/count` | Total count | `200` |
| `GET` | `/api/v1/products/compare` | Compare by IDs (same type) | `200` / `409` |
| `GET` | `/api/v1/templates` | List all templates | `200` |
| `GET` | `/api/v1/templates/{type}` | Get template by type | `200` / `404` |
| `POST` | `/api/v1/templates/reload` | Hot-reload YAML | `200` |

**Filters** (`GET /products`): `name`, `description` (partial), `type` (exact), `priceMin`/`priceMax`, `page`/`pageSize`, `specFilters`.

---

## 📦 Domain Model

```mermaid
classDiagram
    class Product {
        +String id ← auto
        +String name *
        +String description
        +MeasurableValue price *
        +String type *
        +String color
        +String imageUrl ← auto
        +MeasurableValue size
        +MeasurableValue weight
        +Double rating 0‑5
        +Map~String,String~ specifications
    }

    class MeasurableValue {
        +Double value
        +String unit
    }

    class ProductTemplate {
        +String name
        +String displayName
        +Map~String,FieldDefinition~ fields
        +Map~String,FieldDefinition~ specifications
    }

    class FieldDefinition {
        +String type
        +String defaultUnit
        +boolean required
        +boolean comparable
    }

    Product *-- "1" MeasurableValue : price
    Product *-- "0..1" MeasurableValue : size
    Product *-- "0..1" MeasurableValue : weight
    ProductTemplate *-- "*" FieldDefinition : fields/specs
```

**Supporting DTOs:** `ProductFilter` (query params + validation), `PageResponse<T>` (paginated results), `ProductComparisonResponse` (comparison output), `ErrorResponse` (structured errors).

---

## 🔄 Create Product Flow

```mermaid
sequenceDiagram
    actor C as Client
    participant Auth as ApiKeyInterceptor
    participant Ctrl as ProductController
    participant Svc as ProductService
    participant Tmpl as TemplateService
    participant Repo as Repository

    C->>+Auth: POST /products + X-API-KEY
    Auth->>+Ctrl: ✅ Authorized

    Ctrl->>+Svc: createProduct(product)
    Note over Svc: Validate name, price, type
    Svc->>+Tmpl: getTemplate(type)
    Tmpl-->>-Svc: Template (fields + specs)
    Svc->>Svc: Validate fields · Apply default units

    Svc->>+Repo: save(product)
    Repo->>Repo: Generate ID + imageUrl · Lock · Write JSON
    Repo-->>-Svc: Product (persisted)

    Svc-->>-Ctrl: Product
    Ctrl-->>-C: 201 Created
```

---

## 🔍 Comparison Flow

```mermaid
sequenceDiagram
    actor C as Client
    participant Ctrl as ProductController
    participant Svc as ProductService
    participant Repo as Repository

    C->>+Ctrl: GET /compare?ids=1,2&filters=price,rating
    Ctrl->>+Svc: compareProducts(ids, filters)
    Svc->>Svc: Validate ≥2 IDs, no duplicates

    loop Each ID
        Svc->>+Repo: findById(id)
        Repo-->>-Svc: Product
    end

    alt Types differ
        Svc-->>Ctrl: ❌ 409 Conflict
    end

    Svc->>Svc: Get comparable fields · Validate filters · Build result
    Svc-->>-Ctrl: ComparisonResponse
    Ctrl-->>-C: 200 OK
```

---

## 🗂️ Product Types

```mermaid
mindmap
  root((YAML Templates))
    📱 CELLPHONES
      brand · storage · memory · camera · battery
    💻 COMPUTERS
      brand · processor · ram · storage · screen
    👕 CLOTHING
      size_us · size_eu · material · composition
    🍔 FOOD
      manufacturing · expiration · nutriscore · origin
    🥤 BEVERAGES
      volume · origin · alcohol · expiration
    🪑 FURNITURE
      material · dimensions · weight · warranty
    📚 BOOKS
      author · publisher · pages · language · isbn
    ⚽ SPORTS
      size · material · technology · warranty
```

---

## ⚠️ Error Handling

All errors routed through `GlobalExceptionHandler` → structured `ErrorResponse`:

| Exception | HTTP Status |
|-----------|-------------|
| `IllegalArgumentException` / `MethodArgumentNotValidException` / `ConstraintViolationException` / `TypeMismatchException` | **400** Bad Request |
| `ProductNotFoundException` | **404** Not Found |
| `IncompatibleProductTypesException` | **409** Conflict |
| `Exception` (catch-all) | **500** Internal Server Error |

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/v1/products" }
```

---

> 📌 Diagrams use [Mermaid](https://mermaid.js.org/) — renders on GitHub, GitLab, and VS Code.
