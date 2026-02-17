# Changelog - Unificação de Endpoints e Filtros Ampliados

## 🎯 Objetivo
Eliminar duplicidade entre endpoints de listagem e busca, ampliando os filtros de pesquisa e adicionando paginação.

## 📝 Mudanças Implementadas

### 1. **Endpoints Removidos** ❌
- `GET /api/v1/products/search` - Removido (funcionalidade incorporada ao endpoint principal)

### 2. **Endpoints Modificados** ✏️

#### `GET /api/v1/products`
**Antes:** Retornava apenas todos os produtos sem filtros
```bash
curl http://localhost:8080/api/v1/products
```

**Agora:** Endpoint unificado com múltiplos filtros e paginação
```bash
# Sem parâmetros - retorna todos
curl http://localhost:8080/api/v1/products

# Com filtros
curl "http://localhost:8080/api/v1/products?name=iPhone&type=CELLPHONES"
curl "http://localhost:8080/api/v1/products?description=Apple&priceMin=500&priceMax=1000"

# Com paginação
curl "http://localhost:8080/api/v1/products?page=1&pageSize=10"

# Filtros + paginação
curl "http://localhost:8080/api/v1/products?type=CELLPHONES&page=2&pageSize=5"
```

### 3. **Novos Parâmetros de Busca** 🆕

| Parâmetro | Tipo | Descrição | Exemplo |
|-----------|------|-----------|---------|
| `name` | String | Busca parcial no nome (case-insensitive) | `?name=iPhone` |
| `description` | String | Busca parcial na descrição (case-insensitive) | `?description=Apple` |
| `type` | String | Busca exata por tipo | `?type=CELLPHONES` |
| `priceMin` | Double | Preço mínimo (inclusive) | `?priceMin=500` |
| `priceMax` | Double | Preço máximo (inclusive) | `?priceMax=1000` |
| `page` | Integer | Número da página (default: 1) | `?page=2` |
| `pageSize` | Integer | Itens por página (default: null = todos) | `?pageSize=10` |

### 4. **Código Alterado** 💻

#### ProductService.java
- **Método antigo removido:** `getAllProducts()`
- **Método antigo modificado:** `searchProducts(String name, String type)`
- **Novo método:** `searchProducts(String name, String description, String type, Double priceMin, Double priceMax, Integer page, Integer pageSize)`
- **Método privado adicionado:** `applyPagination(List<Product> products, Integer page, Integer pageSize)`

#### ProductController.java
- **Endpoint removido:** `getAllProducts()` 
- **Endpoint removido:** `searchProducts()` (GET /search)
- **Endpoint modificado:** `getProducts()` com 7 parâmetros opcionais

### 5. **Testes Atualizados** ✅

#### ProductServiceTest (18 → 23 testes)
**Novos testes adicionados:**
- `testSearchProductsByDescription()` - Busca por descrição
- `testSearchProductsByPriceRange()` - Filtro de preço
- `testSearchProductsWithPagination()` - Paginação múltiplas páginas
- `testSearchProductsWithoutPagination()` - Sem paginação (todos resultados)
- `testSearchProductsPageOutOfBounds()` - Página fora dos limites

**Testes modificados:**
- `testGetAllProducts()` - Atualizado para usar searchProducts com parâmetros null
- `testSearchProductsByNameAndType()` - Novos parâmetros
- `testSearchProductsByNameOnly()` - Novos parâmetros
- `testSearchProductsByTypeOnly()` - Novos parâmetros
- `testSearchProductsNoResults()` - Novos parâmetros

#### ProductControllerTest (11 → 14 testes)
**Novos testes adicionados:**
- `testSearchProductsByDescription()` - GET /products?description=Apple
- `testSearchProductsByPriceRange()` - GET /products?priceMin=700&priceMax=1000
- `testSearchProductsWithPagination()` - GET /products?page=1&pageSize=10

**Testes modificados:**
- `testGetAllProducts()` - Atualizado para endpoint unificado
- `testSearchProducts()` - Mudou de /search para /products
- `testSearchProductsByNameOnly()` - Mudou de /search para /products

### 6. **Documentação** 📚
- README.md atualizado com novos exemplos de uso
- Contagem de endpoints: 8 → 7
- Contagem de testes: 54 → 62

## 🔍 Comportamento de Paginação

### Sem pageSize (default)
Retorna **todos** os resultados filtrados
```bash
curl "http://localhost:8080/api/v1/products?type=CELLPHONES"
# Retorna todos os CELLPHONES
```

### Com pageSize
Aplica paginação nos resultados
```bash
curl "http://localhost:8080/api/v1/products?type=CELLPHONES&page=1&pageSize=5"
# Retorna primeiros 5 CELLPHONES

curl "http://localhost:8080/api/v1/products?type=CELLPHONES&page=2&pageSize=5"
# Retorna próximos 5 CELLPHONES
```

### Página fora dos limites
Retorna lista vazia
```bash
curl "http://localhost:8080/api/v1/products?page=999&pageSize=10"
# Retorna: []
```

## ✅ Validação

### Testes executados
```bash
mvn test
```

**Resultado:**
- ✅ ProductServiceTest: 23 testes (100% passando)
- ✅ ProductControllerTest: 14 testes (100% passando)
- ✅ ProductComparisonServiceTest: 9 testes (100% passando)
- ✅ ProductComparisonControllerTest: 5 testes (100% passando)
- ✅ ProductTypeTest: 7 testes (100% passando)
- ✅ ProductApiIntegrationTest: 4 testes (100% passando)

**Total: 62 testes (0 falhas)**

## 📊 Comparação Antes/Depois

| Aspecto | Antes | Depois |
|---------|-------|--------|
| Endpoints | 8 | 7 |
| Endpoint de listagem | GET /products | GET /products (com filtros) |
| Endpoint de busca | GET /products/search | ❌ Removido (unificado) |
| Filtros disponíveis | name, type | name, description, type, priceMin, priceMax |
| Paginação | ❌ Não | ✅ Sim (opcional) |
| Testes | 54 | 62 |
| Duplicidade | ⚠️ Sim | ✅ Não |

## 🎉 Benefícios

1. **Menos duplicidade** - Um único endpoint para todas as operações de busca
2. **Mais filtros** - Busca por descrição e faixa de preço
3. **Paginação** - Suporte para grandes volumes de dados
4. **Flexibilidade** - Paginação opcional (mantém compatibilidade)
5. **Melhor cobertura de testes** - +8 testes (54 → 62)
6. **API mais intuitiva** - Menos endpoints para aprender

## 🔄 Breaking Changes

⚠️ **Endpoint removido:** `GET /api/v1/products/search`

**Migração:**
```bash
# Antes
GET /api/v1/products/search?name=iPhone&type=CELLPHONES

# Depois
GET /api/v1/products?name=iPhone&type=CELLPHONES
```

