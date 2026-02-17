# Validação de Filtros de Preço

## 🎯 Objetivo
Garantir que os parâmetros de preço na busca de produtos sejam válidos, evitando queries inconsistentes.

## ✅ Regras Implementadas

### 1. Preço Mínimo (priceMin)
- **Regra:** Deve ser maior que zero
- **Validação:** `priceMin > 0`

```bash
# ❌ INVÁLIDO - priceMin = 0
curl "http://localhost:8080/api/v1/products?priceMin=0"
# Response: 400 Bad Request
# "Preço mínimo deve ser maior que zero"

# ❌ INVÁLIDO - priceMin negativo
curl "http://localhost:8080/api/v1/products?priceMin=-10"
# Response: 400 Bad Request
# "Preço mínimo deve ser maior que zero"

# ✅ VÁLIDO
curl "http://localhost:8080/api/v1/products?priceMin=50"
# Response: 200 OK
```

### 2. Preço Máximo (priceMax)
- **Regra:** Deve ser maior que zero
- **Validação:** `priceMax > 0`

```bash
# ❌ INVÁLIDO - priceMax = 0
curl "http://localhost:8080/api/v1/products?priceMax=0"
# Response: 400 Bad Request
# "Preço máximo deve ser maior que zero"

# ❌ INVÁLIDO - priceMax negativo
curl "http://localhost:8080/api/v1/products?priceMax=-5"
# Response: 400 Bad Request
# "Preço máximo deve ser maior que zero"

# ✅ VÁLIDO
curl "http://localhost:8080/api/v1/products?priceMax=1000"
# Response: 200 OK
```

### 3. Faixa de Preço (priceMin + priceMax)
- **Regra:** priceMax não pode ser menor que priceMin
- **Validação:** `priceMax >= priceMin`

```bash
# ❌ INVÁLIDO - Max < Min
curl "http://localhost:8080/api/v1/products?priceMin=1000&priceMax=500"
# Response: 400 Bad Request
# "Preço máximo não pode ser menor que o preço mínimo"

# ✅ VÁLIDO - Max > Min
curl "http://localhost:8080/api/v1/products?priceMin=500&priceMax=1000"
# Response: 200 OK

# ✅ VÁLIDO - Max = Min
curl "http://localhost:8080/api/v1/products?priceMin=500&priceMax=500"
# Response: 200 OK
```

## 📊 Exemplos de Uso

### Busca com faixa de preço válida
```bash
# Buscar celulares entre R$ 1000 e R$ 3000
curl "http://localhost:8080/api/v1/products?type=CELLPHONES&priceMin=1000&priceMax=3000"

# Buscar produtos com preço mínimo
curl "http://localhost:8080/api/v1/products?priceMin=500"

# Buscar produtos com preço máximo
curl "http://localhost:8080/api/v1/products?priceMax=2000"

# Combinar com outros filtros
curl "http://localhost:8080/api/v1/products?name=Samsung&priceMin=800&priceMax=1500&page=1&pageSize=10"
```

## 🔍 Códigos HTTP

| Cenário | HTTP Status | Mensagem |
|---------|-------------|----------|
| priceMin = 0 | 400 | "Preço mínimo deve ser maior que zero" |
| priceMin < 0 | 400 | "Preço mínimo deve ser maior que zero" |
| priceMax = 0 | 400 | "Preço máximo deve ser maior que zero" |
| priceMax < 0 | 400 | "Preço máximo deve ser maior que zero" |
| priceMax < priceMin | 400 | "Preço máximo não pode ser menor que o preço mínimo" |
| Valores válidos | 200 | Lista de produtos filtrados |

## 🧪 Testes

### ProductServiceTest
```java
@Test
void testSearchProductsWithInvalidPriceMin()
@Test
void testSearchProductsWithInvalidPriceMax()
@Test
void testSearchProductsWithMaxLessThanMin()
@Test
void testSearchProductsWithValidPriceRange()
```

### ProductControllerTest
```java
@Test
void testSearchProductsWithZeroPriceMin()
@Test
void testSearchProductsWithNegativePriceMax()
@Test
void testSearchProductsWithMaxLessThanMin()
```

**Total:** 7 novos testes (62 → 69)

## 💻 Implementação

### ProductService.java
```java
private void validatePriceRange(Double priceMin, Double priceMax) {
    if (priceMin != null && priceMin <= 0) {
        throw new IllegalArgumentException("Preço mínimo deve ser maior que zero");
    }
    if (priceMax != null && priceMax <= 0) {
        throw new IllegalArgumentException("Preço máximo deve ser maior que zero");
    }
    if (priceMin != null && priceMax != null && priceMax < priceMin) {
        throw new IllegalArgumentException("Preço máximo não pode ser menor que o preço mínimo");
    }
}
```

Chamado no início de `searchProducts()`:
```java
public List<Product> searchProducts(...) {
    validatePriceRange(priceMin, priceMax);
    // ... resto da lógica
}
```

## ✅ Checklist de Validação

- [x] priceMin não pode ser zero
- [x] priceMin não pode ser negativo
- [x] priceMax não pode ser zero
- [x] priceMax não pode ser negativo
- [x] priceMax não pode ser menor que priceMin
- [x] Valores null são permitidos (filtro opcional)
- [x] Testes unitários no Service
- [x] Testes de integração no Controller
- [x] Mensagens de erro descritivas

## 🎉 Resultado

Antes: 62 testes | Depois: **69 testes** ✅

Validação robusta que previne queries inválidas e melhora a experiência do usuário com mensagens claras.
