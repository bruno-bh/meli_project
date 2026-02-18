# TODO — Implementação do Plan

> Checklist baseado no [plan.md](plan.md). Marcar `[x]` conforme cada item for concluído.

---

## Fase 1 — Fundação (`ProductTemplateService`)

### 3.1 `getComparableFields(String type)`
- [x] Criar método que retorna `List<String>` com campos (`fields` + `specifications`) onde `comparable: true`
- [x] Teste: type válido retorna apenas campos comparáveis
- [x] Teste: type inválido retorna lista vazia

### 3.2 `getAllSpecificationKeys()`
- [x] Criar método que retorna `Set<String>` com a união de todas as spec keys de todos os templates
- [x] Teste: retorna todas as specs conhecidas de todos os types

### Validação Fase 1
- [x] `mvn clean test` — todos os testes passando

---

## Fase 2 — GET /products

### 1.1 Validar `type` no filtro
- [x] Em `ProductService.searchProducts()`, validar `filter.getType()` contra `templateService.isValidType()` antes de aplicar filtros
- [x] Lançar `IllegalArgumentException` com mensagem incluindo os types disponíveis
- [x] Teste unitário (`ProductServiceTest`): type inválido → `IllegalArgumentException`
- [x] Teste controller (`ProductControllerTest`): `?type=XPTO` → 400 com mensagem de sugestão

### 1.2 Remover `description` e adicionar filtro por `specifications`
- [x] Remover campo `description` do `ProductFilter.java`
- [x] Adicionar campo `Map<String, String> specifications` no `ProductFilter.java`
- [x] Alterar `ProductController.getProducts()` para capturar params extras de spec e popular o `ProductFilter`
- [x] Alterar `ProductService.searchProducts()` para remover filtro por description
- [x] Alterar `ProductService.searchProducts()` para aplicar filtro por specifications (parcial/case-insensitive para texto)
- [x] Remover testes antigos de filtro por `description`
- [x] Teste unitário: filtro por spec válida com `type` informado
- [x] Teste unitário: filtro por spec válida sem `type` informado
- [x] Teste unitário: combinação de spec com outros filtros (name, priceMin, etc.)
- [x] Teste controller: `?type=CELLPHONES&brand=Samsung` → produtos filtrados

### 1.3 Validar specification keys no filtro
- [x] Se `type` informado: validar spec keys contra template daquele type → 400 com specs válidas do type
- [x] Se `type` não informado: validar contra `getAllSpecificationKeys()` → 400 se não existir em nenhum template
- [x] Teste unitário: spec inválida com type → 400 + lista de specs válidas
- [x] Teste unitário: spec inválida sem type → 400
- [x] Teste controller: `?type=CELLPHONES&foo=bar` → 400

### Validação Fase 2
- [x] `mvn clean test` — todos os testes passando

---

## Fase 3 — GET /compare

### 2.1 Validar IDs nulos/vazios/duplicados
- [x] Validar que nenhum ID é `"null"`, vazio ou blank após parsing → `IllegalArgumentException` (400)
- [x] Validar que não há IDs duplicados → `IllegalArgumentException` (400)
- [x] Validar que restam ao menos 2 IDs após limpeza → `IllegalArgumentException` (400)
- [x] Teste unitário (`ProductComparisonServiceTest`): `ids` com valor `"null"` → 400
- [x] Teste unitário: IDs duplicados → 400
- [x] Teste controller (`ProductComparisonControllerTest`): `?ids=1,,3` → 400
- [x] Teste controller: `?ids=1,null,3` → 400

### 2.2 Validar campos comparáveis nos `filters`
- [x] Criar validação em `ProductService.compareProducts()`: após obter o type, checar cada filter contra `getComparableFields(type)`
- [x] Se filter não é comparável → `IllegalArgumentException` com mensagem e lista de campos comparáveis
- [x] Campos fixos `id` e `name` são ignorados na validação (sempre presentes)
- [x] Teste unitário: filter comparável → sucesso
- [x] Teste unitário: filter não comparável → 400 com campos comparáveis listados
- [x] Teste controller: `?ids=1,2&filters=brand` (CELLPHONES) → 400

### 2.3 Default filters baseados no template YAML
- [x] Substituir `getDefaultFilters()` hardcoded por consulta a `getComparableFields(type)`
- [x] Remover método `getDefaultFilters()` (ou torná-lo fallback)
- [x] Teste unitário: compare sem filters → resposta contém apenas campos comparáveis do type
- [x] Teste controller: `?ids=1,2` (sem filters) → resposta com campos comparáveis

### Validação Fase 3
- [x] `mvn clean test` — todos os testes passando

---

## Fase 4 — Validação Final

- [x] `mvn clean test` — **168 testes passando** (sem regressões, +8 novos testes)
- [ ] Testar manualmente via curl/Postman os cenários principais
- [ ] Atualizar `copilot-instructions.md` se necessário (novos métodos, mudanças na API)
