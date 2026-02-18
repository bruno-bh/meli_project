# Plan — Melhorias nos Endpoints GET /products e GET /compare

> Documento gerado após análise do código atual dos endpoints `GET /api/v1/products` e `GET /api/v1/products/compare`.  
> Cada item descreve o **problema/lacuna**, o **comportamento esperado** e os **arquivos impactados**.

---

## 1. GET /api/v1/products

### 1.1 Validar o `type` informado no filtro

| Item | Detalhe |
|------|---------|
| **Problema** | Quando o usuário passa `?type=XPTO`, o filtro simplesmente não encontra produtos e retorna uma lista vazia `[]`. Não há nenhuma validação contra os templates YAML. O usuário não recebe feedback de que o type informado é inválido e nem sabe quais types existem. |
| **Comportamento esperado** | Se `type` não for `null`/vazio e **não existir** em `ProductTemplateService.isValidType()`, lançar `IllegalArgumentException` com mensagem clara: `"Invalid product type: 'XPTO'. Available types: [BEVERAGES, BOOKS, CELLPHONES, CLOTHING, COMPUTERS, FOOD, FURNITURE, SPORTS]"`. |
| **Camada** | `ProductService.searchProducts()` — adicionar validação **antes** do filtro ser aplicado. |
| **Arquivos** | `ProductService.java`, `ProductServiceTest.java`, `ProductControllerTest.java` |
| **Testes** | Criar teste unitário que envia type inválido e espera 400 com a mensagem de sugestão. Criar teste de controller que valida a resposta HTTP. |

---

### 1.2 Remover filtro por `description` e adicionar filtro por `specifications`

| Item | Detalhe |
|------|---------|
| **Problema** | O filtro atual aceita `description` (busca textual na descrição), mas de acordo com os requisitos a busca por `description` não é necessária. Em vez disso, o usuário deveria poder filtrar por **specifications** conhecidas (ex: `?brand=Samsung`, `?storage_gb=128`). |
| **Comportamento esperado** | 1) Remover o campo `description` do `ProductFilter`.<br>2) Adicionar suporte a parâmetros dinâmicos de specification no filtro (via `Map<String, String> specifications` ou `@RequestParam Map<String, String>` capturando params extras).<br>3) Ao receber um parâmetro de specification, verificar se o campo é uma specification **conhecida** consultando o template YAML do `type` informado (se `type` foi passado) ou procurar em **todos os templates** (se `type` não foi passado).<br>4) Filtrar produtos cujas `specifications` contenham a chave e o valor corresponda (parcial/case-insensitive para texto, exato para números). |
| **Camada** | `ProductFilter.java` (remover `description`, adicionar `Map<String, String> specifications`), `ProductController.java` (capturar params extras), `ProductService.searchProducts()` (aplicar filtros de spec). |
| **Arquivos** | `ProductFilter.java`, `ProductController.java`, `ProductService.java`, testes relacionados |
| **Testes** | Testar filtro por spec válida com e sem `type`. Testar spec inválida (retornar 400 ou ignorar). Testar combinação de specs com outros filtros. Remover testes de `description`. |

---

### 1.3 Validar specification keys informadas no filtro

| Item | Detalhe |
|------|---------|
| **Problema** | Se o usuário enviar um parâmetro de specification que não existe em nenhum template (ex: `?foo=bar`), não há validação. |
| **Comportamento esperado** | Se `type` foi informado: validar as spec keys contra o template daquele type. Se a spec não pertence ao type, retornar 400 com mensagem informando as specs válidas para aquele type.<br>Se `type` não foi informado: validar contra a união de todas as specs de todos os templates. Se não existir em nenhum template, retornar 400. |
| **Camada** | `ProductService.searchProducts()` |
| **Arquivos** | `ProductService.java`, `ProductTemplateService.java` (possivelmente novo método `getAllSpecificationKeys()`), testes |
| **Testes** | Testar spec inválida com type → 400 + lista de specs válidas. Testar spec inválida sem type → 400. |

---

## 2. GET /api/v1/products/compare

### 2.1 Validar IDs nulos ou vazios dentro do array

| Item | Detalhe |
|------|---------|
| **Problema** | Se o usuário passa `?ids=1,,3` ou `?ids=1,null,3`, o código atual filtra strings vazias mas **não valida valores que não correspondem a produtos antes de prosseguir**. Mais importante: se `ids` contém strings como `"null"`, o `getProductById("null")` lança `ProductNotFoundException` (404), não um Bad Request (400). Não há validação clara de que o array contém entradas inválidas. |
| **Comportamento esperado** | 1) Após o parsing de IDs, verificar se algum ID é `null`, `"null"`, vazio ou blank → lançar `IllegalArgumentException` (400) com mensagem: `"Invalid product ID found in the list. IDs must be non-null, non-empty values."`<br>2) Validar que não há IDs duplicados.<br>3) Validar que, após a limpeza, restam ao menos 2 IDs. |
| **Camada** | `ProductController.compareProducts()` (validação de input) e/ou `ProductService.compareProducts()` |
| **Arquivos** | `ProductController.java`, `ProductService.java`, `ProductComparisonControllerTest.java`, `ProductComparisonServiceTest.java` |
| **Testes** | Testar `ids=1,,3` → 400. Testar `ids=1,null,3` → 400. Testar `ids=1` (menos de 2) → 400. Testar `ids=1,1` (duplicados) → 400. |

---

### 2.2 Validar se os filtros passados são campos comparáveis (conforme YAML)

| Item | Detalhe |
|------|---------|
| **Problema** | Atualmente, qualquer string passada em `filters` é aceita. O endpoint não verifica se o campo é `comparable: true` no template YAML. Exemplo: para CELLPHONES, `brand` tem `comparable: false`, mas o usuário pode passar `?filters=brand` e o campo é incluído normalmente na resposta. Não faz sentido "comparar" um campo não comparável. |
| **Comportamento esperado** | 1) Após determinar o `productType` (após buscar os produtos e verificar que são do mesmo type), consultar o template YAML.<br>2) Para cada filtro informado, verificar se o campo existe E se `comparable: true`.<br>3) Se algum filtro não for comparável, retornar 400 com mensagem: `"Field 'brand' is not comparable for type 'CELLPHONES'. Comparable fields: [price, size, weight, storage_gb, memory_gb, screen_size, camera_mp, battery_capacity]"`.<br>4) Campos fixos (`id`, `name`) continuam sempre presentes na resposta (não precisam ser `comparable`). |
| **Camada** | `ProductService.compareProducts()`, `ProductTemplateService.java` (novo método: `getComparableFields(String type)`) |
| **Arquivos** | `ProductService.java`, `ProductTemplateService.java`, `ProductComparisonServiceTest.java`, `ProductComparisonControllerTest.java` |
| **Testes** | Testar filtro comparável → sucesso. Testar filtro não comparável → 400 com lista de campos comparáveis. Testar mix de comparáveis e não comparáveis → 400. |

---

### 2.3 Retornar campos comparáveis como default (quando `filters` não é informado)

| Item | Detalhe |
|------|---------|
| **Problema** | Quando `filters` não é passado, o método `getDefaultFilters()` retorna uma lista hardcoded (`description, imageUrl, price, size, weight, color, rating, specifications`). Isso inclui campos não comparáveis e ignora as specs comparáveis do type. |
| **Comportamento esperado** | Quando `filters` é `null`, os filtros default devem ser os **campos comparáveis** do template YAML para aquele product type, em vez de uma lista hardcoded. Exemplo para CELLPHONES: `[price, size, weight, storage_gb, memory_gb, screen_size, camera_mp, battery_capacity]`. |
| **Camada** | `ProductService.compareProducts()` — substituir `getDefaultFilters()` por uma consulta ao template |
| **Arquivos** | `ProductService.java`, `ProductTemplateService.java` |
| **Testes** | Testar compare sem filters → resposta contém apenas campos comparáveis do type. |

---

## 3. Itens de Suporte (necessários para implementar o plano)

### 3.1 Novo método em `ProductTemplateService`: `getComparableFields(String type)`

| Item | Detalhe |
|------|---------|
| **Descrição** | Retorna a lista de campos (fields + specifications) que possuem `comparable: true` para um dado type. |
| **Retorno** | `List<String>` com os nomes dos campos comparáveis. |
| **Arquivos** | `ProductTemplateService.java`, `ProductTemplateServiceTest.java` |

### 3.2 Novo método em `ProductTemplateService`: `getAllSpecificationKeys()`

| Item | Detalhe |
|------|---------|
| **Descrição** | Retorna a união de todas as specification keys de todos os templates. Usado para validar filtros de spec quando `type` não é informado no GET /products. |
| **Retorno** | `Set<String>` com todas as specification keys. |
| **Arquivos** | `ProductTemplateService.java`, `ProductTemplateServiceTest.java` |

### 3.3 Nova exception (opcional): `NonComparableFieldException`

| Item | Detalhe |
|------|---------|
| **Descrição** | Exception dedicada para quando o usuário tenta comparar por um campo não comparável. Pode ser tratada pelo `GlobalExceptionHandler` retornando 400. Alternativamente pode-se reutilizar `IllegalArgumentException`. |
| **Decisão** | Avaliar se vale criar uma exception dedicada ou se `IllegalArgumentException` com mensagem descritiva é suficiente. |

---

## 4. Resumo de Impacto por Arquivo

| Arquivo | Mudanças |
|---------|----------|
| `ProductFilter.java` | Remover `description`; adicionar `Map<String, String> specifications` |
| `ProductController.java` | Capturar params extras como specs; validar IDs no compare |
| `ProductService.java` | Validar type no search; filtro por specs; validar IDs no compare; validar comparable fields; substituir default filters |
| `ProductTemplateService.java` | Novos métodos: `getComparableFields()`, `getAllSpecificationKeys()` |
| `GlobalExceptionHandler.java` | Possivelmente nova exception handler (se criar `NonComparableFieldException`) |
| `ProductServiceTest.java` | Novos testes para type inválido, filtro por spec, spec inválida |
| `ProductComparisonServiceTest.java` | Novos testes para IDs nulos, fields não comparáveis, default filters dinâmicos |
| `ProductControllerTest.java` | Testes de integração para os novos cenários de erro |
| `ProductComparisonControllerTest.java` | Testes de integração para validação de IDs e filters comparáveis |
| `ProductTemplateServiceTest.java` | Testes para novos métodos |

---

## 5. Ordem de Implementação Sugerida

```
Fase 1 — Fundação
  ├── 3.1 Criar ProductTemplateService.getComparableFields()
  ├── 3.2 Criar ProductTemplateService.getAllSpecificationKeys()
  └── Testes unitários dos novos métodos

Fase 2 — GET /products
  ├── 1.1 Validar type no filtro (ProductService.searchProducts)
  ├── 1.2 Remover description e adicionar filtro por specifications
  ├── 1.3 Validar specification keys no filtro
  └── Testes unitários + controller

Fase 3 — GET /compare
  ├── 2.1 Validar IDs nulos/vazios/duplicados
  ├── 2.2 Validar campos comparáveis nos filters
  ├── 2.3 Default filters baseados no template YAML
  └── Testes unitários + controller

Fase 4 — Validação final
  └── mvn clean test (todos os testes devem passar)
```

---

## 6. Checklist do ProjectDescription.md

Itens do requisito original vs. estado atual:

| Requisito | Status | Observação |
|-----------|--------|------------|
| RESTful API that returns details for multiple items to be compared | ✅ Implementado | Endpoint `/compare` existe |
| Fields: name, image URL, description, price, rating, specifications | ✅ Implementado | Modelo `Product` possui todos |
| Basic error handling | ✅ Implementado | `GlobalExceptionHandler` com respostas estruturadas |
| Inline comments to explain logic | ⚠️ Parcial | Alguns métodos têm comments, mas muitos não têm Javadoc |
| Simulate data persistence (JSON/CSV/in-memory) | ✅ Implementado | JSON file-based via `ProductRepository` |
| User should be able to query specific comparisons and ignore other fields | ✅ Implementado | Param `filters` no compare |
| Specialized information per product type (battery, camera, etc.) | ✅ Implementado | YAML templates + specifications |
| Error handling best practices | ⚠️ Parcial | Falta validação de type no GET /products, IDs nulos no compare, campos comparáveis |
| Documentation | ✅ Implementado | README.md existe |
| Testing | ✅ Implementado | 92+ testes passando, mas faltam testes para os cenários novos |

---

> **Próximo passo:** Confirmar este plano e começar pela **Fase 1**.
