# Validação de Tipos com ProductFilter

## Data: 2024

## Alterações Implementadas

### 1. ProductFilter - DTO com Validações

Foi criado um novo modelo `ProductFilter` para encapsular todos os parâmetros de busca e aplicar validações de tipo e valor automaticamente usando Bean Validation (JSR-303).

**Localização**: `src/main/java/com/meli/productapi/model/ProductFilter.java`

**Validações aplicadas**:
- `priceMin`: Deve ser maior ou igual a 1 (se fornecido)
- `priceMax`: Deve ser maior ou igual a 1 (se fornecido)
- `page`: Deve ser maior ou igual a 1 (se fornecido)
- `pageSize`: Deve ser maior ou igual a 1 (se fornecido)
- `priceMax >= priceMin`: Validação customizada no método `validate()`

**Campos**:
```java
public class ProductFilter {
    private String name;
    private String description;
    private String type;
    
    @Min(value = 1, message = "Preço mínimo deve ser maior que zero")
    private Double priceMin;
    
    @Min(value = 1, message = "Preço máximo deve ser maior que zero")
    private Double priceMax;
    
    @Min(value = 1, message = "Número da página deve ser maior ou igual a 1")
    private Integer page;
    
    @Min(value = 1, message = "Tamanho da página deve ser maior que zero")
    private Integer pageSize;
}
```

### 2. ProductController - Uso do @Valid

**Antes**:
```java
@GetMapping
public ResponseEntity<List<Product>> getProducts(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String description,
    @RequestParam(required = false) String type,
    @RequestParam(required = false) Double priceMin,
    @RequestParam(required = false) Double priceMax,
    @RequestParam(required = false) Integer page,
    @RequestParam(required = false) Integer pageSize
)
```

**Depois**:
```java
@Validated
@RestController
public class ProductController {
    
    @GetMapping
    public ResponseEntity<List<Product>> getProducts(@Valid ProductFilter filter) {
        filter.validate(); // Validação customizada
        List<Product> products = productService.searchProducts(filter);
        return ResponseEntity.ok(products);
    }
}
```

### 3. ProductService - Assinatura Simplificada

**Antes**:
```java
public List<Product> searchProducts(
    String name, 
    String description, 
    String type, 
    Double priceMin, 
    Double priceMax, 
    Integer page, 
    Integer pageSize
)
```

**Depois**:
```java
public List<Product> searchProducts(ProductFilter filter)
```

### 4. GlobalExceptionHandler - Tratamento de Erros de Validação

Foram adicionados handlers para capturar erros de validação e retornar respostas HTTP 400 adequadas:

- `MethodArgumentNotValidException`: Erros de validação do Bean Validation (@Min, etc)
- `ConstraintViolationException`: Violações de constraints
- `MethodArgumentTypeMismatchException`: Erros de conversão de tipo (ex: string em campo numérico)

## Exemplos de Uso

### Busca simples (funcionam normalmente):
```bash
GET /api/v1/products
GET /api/v1/products?name=iPhone
GET /api/v1/products?type=CELLPHONES&page=1&pageSize=10
GET /api/v1/products?priceMin=500&priceMax=1000
```

### Validações que retornam HTTP 400:

#### 1. Tipo inválido em campo numérico
```bash
GET /api/v1/products?page=abc
Resposta: 400 Bad Request
{
  "timestamp": "2024-XX-XX...",
  "status": 400,
  "error": "Bad Request",
  "message": "Parâmetro 'page' deve ser do tipo Integer"
}
```

#### 2. Valor menor que o mínimo permitido
```bash
GET /api/v1/products?page=0
Resposta: 400 Bad Request
{
  "timestamp": "2024-XX-XX...",
  "status": 400,
  "error": "Bad Request",
  "errors": {
    "page": "Número da página deve ser maior ou igual a 1"
  }
}
```

#### 3. Preço máximo menor que preço mínimo
```bash
GET /api/v1/products?priceMin=1000&priceMax=500
Resposta: 400 Bad Request
{
  "timestamp": "2024-XX-XX...",
  "status": 400,
  "error": "Bad Request",
  "message": "Preço máximo não pode ser menor que o preço mínimo"
}
```

## Benefícios

1. **Type Safety**: Tipos são validados automaticamente pelo Spring antes de chegar ao controller
2. **Menos Código**: Assinatura de método simplificada (1 parâmetro ao invés de 7)
3. **Validação Centralizada**: Todas as regras de validação estão no ProductFilter
4. **Mensagens Claras**: Erros de validação retornam mensagens descritivas ao usuário
5. **Manutenibilidade**: Mais fácil adicionar novos filtros (apenas modificar ProductFilter)

## Testes

Total de testes: **87** (todos passando)

### Novos testes adicionados:
- `testSearchProductsWithInvalidPageType()`: Valida string em campo page
- `testSearchProductsWithInvalidPageSizeType()`: Valida string em campo pageSize
- `testSearchProductsWithInvalidPriceMinType()`: Valida string em priceMin
- `testSearchProductsWithInvalidPriceMaxType()`: Valida string em priceMax
- `testSearchProductsWithPageLessThanOne()`: Valida page < 1
- `testSearchProductsWithPageSizeZero()`: Valida pageSize = 0

### Testes atualizados:
Todos os testes que chamavam `searchProducts()` com 7 parâmetros individuais foram refatorados para usar `ProductFilter.builder()`.

## Arquivos Alterados

1. **Criados**:
   - `ProductFilter.java` - DTO com validações

2. **Modificados**:
   - `ProductController.java` - Usa @Valid ProductFilter
   - `ProductService.java` - Aceita ProductFilter, removido validatePriceRange()
   - `GlobalExceptionHandler.java` - Adicionados handlers para erros de validação
   - `ProductServiceTest.java` - Todos os testes atualizados
   - `ProductControllerTest.java` - Todos os testes atualizados + 6 novos testes
