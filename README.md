# Product API - REST API Simple CRUD

REST API simples para gerenciamento de produtos com Java 17, Spring Boot e Maven.

## 🚀 Stack

- **Java:** 17.0.18
- **Framework:** Spring Boot 3.2.0
- **Build:** Maven 3.6+
- **Testing:** JUnit 5, Mockito
- **Storage:** JSON file (`data/products.json`)

## 📋 Endpoints

### GET /api/v1/products
Lista todos os produtos (com filtros opcionais e paginação)
```bash
# Listar todos os produtos
curl http://localhost:8080/api/v1/products

# Buscar por nome
curl "http://localhost:8080/api/v1/products?name=iPhone"

# Buscar por descrição
curl "http://localhost:8080/api/v1/products?description=Apple"

# Buscar por tipo
curl "http://localhost:8080/api/v1/products?type=CELLPHONES"

# Buscar por faixa de preço
curl "http://localhost:8080/api/v1/products?priceMin=500&priceMax=1000"

# Combinar múltiplos filtros
curl "http://localhost:8080/api/v1/products?name=Samsung&type=CELLPHONES&priceMin=700"

# Com paginação (página 1, 10 itens por página)
curl "http://localhost:8080/api/v1/products?page=1&pageSize=10"

# Filtros + paginação
curl "http://localhost:8080/api/v1/products?type=CELLPHONES&page=2&pageSize=5"
```

**Parâmetros de busca:**
- `name`: Filtra por nome (case-insensitive, busca parcial)
- `description`: Filtra por descrição (case-insensitive, busca parcial)
- `type`: Filtra por tipo exato do produto
- `priceMin`: Preço mínimo (inclusive, deve ser >= 1)
- `priceMax`: Preço máximo (inclusive, deve ser >= 1 e >= priceMin)
- `page`: Número da página (deve ser >= 1, default: 1)
- `pageSize`: Itens por página (deve ser >= 1, default: null = todos os resultados)

**Validações automáticas:**
- ✅ Tipos validados automaticamente (ex: `page=abc` retorna HTTP 400)
- ✅ Valores negativos ou zero em campos numéricos retornam HTTP 400
- ✅ `priceMax < priceMin` retorna HTTP 400 com mensagem clara
- ✅ Parâmetros inválidos retornam mensagem descritiva do erro

**Exemplos de erros de validação:**
```bash
# Tipo inválido
curl "http://localhost:8080/api/v1/products?page=abc"
# Retorna: 400 Bad Request - "Parâmetro 'page' deve ser do tipo Integer"

# Valor inválido
curl "http://localhost:8080/api/v1/products?page=0"
# Retorna: 400 Bad Request - "Número da página deve ser maior ou igual a 1"

# Lógica inválida
curl "http://localhost:8080/api/v1/products?priceMin=1000&priceMax=500"
# Retorna: 400 Bad Request - "Preço máximo não pode ser menor que o preço mínimo"
```

**Observações sobre paginação:**
- Se `pageSize` não for informado, retorna todos os produtos (sem paginação)
- Se `page` não for informado, considera página 1
- Páginas fora do limite retornam lista vazia

### GET /api/v1/products/{id}
Obtém um produto por ID
```bash
curl http://localhost:8080/api/v1/products/{id}
```

### POST /api/v1/products
Cria um novo produto
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15",
    "description": "Smartphone Apple",
    "price": 999.99,
    "type": "CELLPHONES",
    "color": "Black",
    "imageUrl": "https://example.com/iphone15.jpg"
  }'
```

### PUT /api/v1/products/{id}
Atualiza um produto
```bash
curl -X PUT http://localhost:8080/api/v1/products/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Smartphone Apple Pro",
    "price": 1099.99,
    "type": "CELLPHONES",
    "color": "Titanium",
    "imageUrl": "https://example.com/iphone15pro.jpg"
  }'
```

### DELETE /api/v1/products/{id}
Deleta um produto
```bash
curl -X DELETE http://localhost:8080/api/v1/products/{id}
```

### GET /api/v1/products/stats/count
Conta total de produtos
```bash
curl http://localhost:8080/api/v1/products/stats/count
```

### GET /api/v1/products/compare
Compara múltiplos produtos do mesmo tipo
```bash
# Comparar todos os campos (sem filtros)
curl "http://localhost:8080/api/v1/products/compare?ids=1,2"

# Comparar apenas campos específicos
curl "http://localhost:8080/api/v1/products/compare?ids=1,2&filters=price,rating,memory_gb"

# Comparar com campos de specifications
curl "http://localhost:8080/api/v1/products/compare?ids=1,2&filters=price,camera_mp,storage_gb"
```

**Resposta:**
```json
{
  "productType": "CELLPHONES",
  "appliedFilters": ["price", "rating", "memory_gb"],
  "products": [
    {
      "id": "1",
      "name": "iPhone 15 Pro",
      "description": "Latest generation Apple smartphone",
      "imageUrl": "https://picsum.photos/400/600?random=1",
      "price": 5999.99,
      "rating": 4.8,
      "memory_gb": "8"
    },
    {
      "id": "2",
      "name": "Samsung Galaxy S24 Plus",
      "description": "Latest generation samsung smartphone",
      "imageUrl": "https://picsum.photos/400/600?random=2",
      "price": 4999.99,
      "rating": 4.4,
      "memory_gb": "12"
    }
  ]
}
```

**Regras de Comparação:**
- ✅ Campos **sempre retornados**: `id`, `name`
- ✅ Campos **opcionais** (se não vazios): `description`, `imageUrl`
- ✅ Todos os produtos devem ser do **mesmo tipo** (retorna HTTP 409 se diferentes)
- ✅ Campos de `specifications` podem ser usados como filtros
- ✅ Campos inexistentes em specifications são **ignorados** (não geram erro)
- ✅ Sem filtros = retorna todos os campos disponíveis

## 🏃 Como Executar

### Build
```bash
mvn clean package
```

### Executar
```bash
mvn spring-boot:run
```

API estará disponível em: `http://localhost:8080`

## 🏷️ Product Types

Todos os produtos devem usar um dos tipos definidos no enum `ProductType`. Cada tipo possui specifications específicas:

- **CELLPHONES** (Smartphones): brand, storage_gb, memory_gb, screen_size, camera_mp
- **COMPUTERS** (Computadores): brand, processor, ram_gb, storage_gb, screen_size
- **CLOTHING** (Roupas): size_us, size_eu, material, composition, color_variations
- **FOOD** (Alimentos): manufacturing_date, expiration_date, nutriscore, origin
- **BEVERAGES** (Bebidas): volume_ml, origin, expiration_date, ingredients, alcohol_content
- **FURNITURE** (Móveis): material, dimensions, weight_kg, color, warranty_months
- **BOOKS** (Livros): author, publisher, pages, language, isbn
- **SPORTS** (Esportes): size, material, color, technology, warranty_months

Veja [PRODUCT_TYPE_GUIDE.md](PRODUCT_TYPE_GUIDE.md) para detalhes completos e exemplos.

## ✅ Testes

Executar todos os testes:
```bash
mvn clean test
```

Testes disponíveis:
- ProductServiceTest (23 testes - ✨ busca com filtros + paginação)
- ProductControllerTest (14 testes - ✨ endpoint unificado com filtros)
- ProductComparisonServiceTest (9 testes - comparação)
- ProductComparisonControllerTest (5 testes - comparação)
- ProductTypeTest (7 testes - enum validation)
- ProductApiIntegrationTest (4 testes)

**Total:** 62 testes (100% passando)

## 📁 Estrutura

```
src/
├── main/java/com/meli/productapi/
│   ├── ProductApiApplication.java
│   ├── model/
│   │   ├── Product.java
│   │   ├── ProductType.java                (Enum de tipos)
│   │   ├── ProductFilter.java              (✨ DTO de filtros com validação)
│   │   └── ProductComparisonResponse.java  (✨ DTO de comparação)
│   ├── controller/ProductController.java   (7 endpoints)
│   ├── service/ProductService.java
│   ├── repository/ProductRepository.java
│   └── exception/
│       ├── GlobalExceptionHandler.java     (✨ Validação de tipos e erros)
│       ├── ProductNotFoundException.java
│       └── IncompatibleProductTypesException.java (✨ Validação de tipos)
└── test/java/com/meli/productapi/
    ├── ProductServiceTest.java
    ├── ProductControllerTest.java
    ├── ProductComparisonServiceTest.java       (✨ Testes de comparação)
    ├── ProductComparisonControllerTest.java    (✨ Testes de comparação)
    ├── ProductTypeTest.java
    └── ProductApiIntegrationTest.java
```

## 📝 Model

```json
{
  "id": "1",
  "name": "iPhone 15 Pro",
  "description": "Latest generation Apple smartphone",
  "price": 5999.99,
  "type": "CELLPHONES",
  "color": "Space Black",
  "imageUrl": "https://picsum.photos/400/600?random=1",
  "size": "6.1 inches",
  "weight": 0.187,
  "rating": 4.8,
  "specifications": {
    "brand": "Apple",
    "storage_gb": "256",
    "memory_gb": "8",
    "screen_size": "6.1",
    "camera_mp": "48"
  }
}
```

**Campos Obrigatórios:**
- `name` - Nome do produto (não pode ser vazio ou null)
- `price` - Preço (deve ser valor positivo, não pode ser null)
- `type` - Tipo do produto (usar ProductType enum, não pode ser vazio ou null)

**Campos Opcionais:**
- `description` - Descrição
- `imageUrl` - URL da imagem (gerada automaticamente se vazia)
- `color`, `size`, `weight` - Características físicas
- `rating` - Avaliação 0-5 (padrão: 0.0)
- `specifications` - Metadados específicos do tipo

## 📊 Resumo

- ✅ **7 endpoints** (CRUD completo + comparação + count)
- ✅ **62 testes** (100% passando)
- ✅ **Endpoint unificado de busca**: GET /api/v1/products com múltiplos filtros
- ✅ **Filtros disponíveis**: name, description, type, priceMin, priceMax
- ✅ **Paginação**: page e pageSize (opcional)
- ✅ **ProductType enum** com 8 tipos padronizados
- ✅ **Validação rigorosa**: name, type e price obrigatórios (HTTP 400)
- ✅ **Comparação de produtos** com filtros flexíveis
- ✅ **Specifications** específicas por tipo
- ✅ **Validação de tipos** na comparação (HTTP 409)
- ✅ **Auto-geração**: ID incremental e imageUrl aleatória
- ✅ **Rating system**: 0-5 estrelas
- ✅ **Persistência** em JSON
- ✅ **Exception handling** global

Para mais detalhes sobre tipos e specifications, consulte [PRODUCT_TYPE_GUIDE.md](PRODUCT_TYPE_GUIDE.md)

## ✅ Validações e Segurança

### Validação de Tipos (ProductFilter)
A API implementa validação automática de tipos e valores usando Bean Validation (JSR-303):

- **Type Safety**: Conversão automática de tipos (Integer, Double) com tratamento de erros
- **Range Validation**: Valores numéricos validados (page >= 1, priceMin >= 1, etc)
- **Business Rules**: Validações de regras de negócio (priceMax >= priceMin)
- **Error Messages**: Mensagens de erro descritivas e user-friendly

Para mais detalhes, consulte [VALIDACAO_TIPOS.md](VALIDACAO_TIPOS.md)

## 🚧 Próximos Passos

Melhorias planejadas para versões futuras (não incluídas na versão atual):

### 🐳 **Dockerização**
- Containerizar a aplicação com Docker
- Criar `docker-compose.yml` para facilitar deploy
- Melhorar escalabilidade e portabilidade
- Facilitar deploy em ambientes cloud (AWS, Azure, GCP)

### ⚡ **Sistema de Cache**
- Implementar Redis para cache de comparações frequentes
- Cache de produtos mais acessados
- Reduzir latência em comparações complexas
- Invalidação inteligente de cache em atualizações

### 🔐 **Camada de Segurança**
- Implementar autenticação via JWT tokens
- Suporte a usuários temporários/anônimos
- Rate limiting por IP/token
- API keys para integrações externas
- CORS configurável

### 🗄️ **Migração para NoSQL**
- Migrar de JSON para MongoDB ou DynamoDB
- Melhor suporte para specifications dinâmicas
- Schema flexível para diferentes tipos de produtos
- Melhor performance em buscas complexas
- Facilitar adição de novos campos sem migrações

### 📊 **Observabilidade e Monitoramento**
- Integrar Spring Boot Actuator
- Métricas com Prometheus + Grafana
- Logging estruturado (ELK Stack ou CloudWatch)
- Health checks detalhados
- Distributed tracing (Zipkin/Jaeger)
- Alertas automatizados

### 📚 **Documentação Interativa**
- Adicionar Swagger/OpenAPI 3.0
- Documentação interativa da API
- Geração automática de cliente SDKs
- Exemplos de requisições e respostas
- Playground para testar endpoints

### 🎯 **Outras Melhorias**
- ~~Paginação em listagens (`/products?page=1&size=20`)~~ ✅ Implementado
- ~~Filtros avançados (faixa de preço, múltiplos tipos)~~ ✅ Implementado
- ~~Validação de tipos e valores nos parâmetros~~ ✅ Implementado
- Ordenação customizável (`/products?sort=price,desc`)
- Versionamento de API (`/api/v2/products`)
- Suporte a bulk operations (criar/atualizar múltiplos produtos)
- Webhooks para notificações de mudanças

---

**Contribuições são bem-vindas!** 🚀
