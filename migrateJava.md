# Análise de Migração: Java 17 → Java 21

> **Projeto:** Product API (Meli Project)  
> **Data da análise:** 18/02/2026  
> **Versão atual:** Java 17.0.18 + Spring Boot 3.2.0 + Maven 3.6.3  
> **Versão alvo:** Java 21 (LTS)

---

## Sumário Executivo

A migração de Java 17 para Java 21 é **de baixo risco** para este projeto. O código não utiliza APIs internas do JDK, não possui `SecurityManager`, não usa `finalize()`, e toda a concorrência é baseada em `ReentrantLock` (compatível com virtual threads). A maior parte do esforço consiste em alterar a versão no `pom.xml`, instalar o JDK 21 e, opcionalmente, atualizar o Spring Boot para melhor suporte.

| Aspecto | Risco | Esforço |
|---------|-------|---------|
| Compilação e runtime | 🟢 Baixo | Mínimo — alterar `pom.xml` |
| Dependências (Lombok, Jackson, SpringDoc) | 🟢 Baixo | Verificar versões compatíveis |
| Comportamento em runtime (encoding, threads) | 🟢 Baixo | Nenhuma mudança de comportamento esperada |
| Testes (92 testes existentes) | 🟡 Médio | `@MockBean` pode gerar warnings se Spring Boot for atualizado para 3.4+ |
| Oportunidades de modernização | 🟢 Opcional | Virtual threads, switch expressions, records |

---

## 1. Mudanças Obrigatórias (Breaking)

### 1.1 `pom.xml` — Versão do Java

**Arquivo:** `pom.xml` (linha 24)

```xml
<!-- ANTES -->
<java.version>17</java.version>

<!-- DEPOIS -->
<java.version>21</java.version>
```

Isso configura o `maven-compiler-plugin` (herdado do `spring-boot-starter-parent`) para compilar com target 21.

### 1.2 Instalação do JDK 21

O ambiente atual usa OpenJDK 17.0.18. É necessário instalar o JDK 21:

```bash
# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# Ou via SDKMAN (recomendado)
sdk install java 21.0.2-tem
sdk use java 21.0.2-tem
```

Verificar com `java -version` e `mvn --version` que ambos apontam para o JDK 21.

### 1.3 Atualização do Spring Boot (Recomendado)

O Spring Boot 3.2.0 **já suporta** Java 21, mas versões mais recentes trazem melhor suporte e correções:

| Versão Spring Boot | Suporte Java 21 | Notas |
|---------------------|-----------------|-------|
| 3.2.0 (atual) | ✅ Funcional | Suporte inicial |
| **3.2.5+** | ✅ Recomendado | Correções de bugs para Java 21 |
| **3.3.x** | ✅ Ideal | Virtual threads estáveis, melhor integração |
| 3.4.x | ✅ Completo | `@MockBean` depreciado → `@MockitoBean` |

**Recomendação:** Atualizar para **3.2.5** ou **3.3.x** (menor risco). Evitar 3.4+ sem antes migrar os testes.

```xml
<!-- Recomendado -->
<version>3.3.5</version>
```

---

## 2. Análise de Compatibilidade das Dependências

### 2.1 Lombok

| Item | Detalhe |
|------|---------|
| **Versão atual** | ~1.18.30 (herdada do Spring Boot 3.2.0 parent) |
| **Versão mínima para Java 21** | 1.18.30 |
| **Versão recomendada** | ≥ 1.18.32 |
| **Risco** | 🟢 Baixo |

Anotações usadas no projeto: `@Builder`, `@Data`, `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Slf4j`.

**Recomendação:** Fixar explicitamente a versão no `pom.xml`:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.34</version>
    <optional>true</optional>
</dependency>
```

### 2.2 Jackson (JSON + YAML)

| Item | Detalhe |
|------|---------|
| **Versão atual** | ~2.15.x (herdada do Spring Boot 3.2.0) |
| **Compatibilidade Java 21** | ✅ Totalmente compatível |
| **Risco** | 🟢 Nenhum |

O projeto usa: `ObjectMapper`, `TypeReference`, `@JsonInclude`, `@JsonProperty`, `YAMLFactory`. Todas são APIs estáveis sem mudanças de comportamento no Java 21.

### 2.3 SpringDoc OpenAPI (Swagger)

| Item | Detalhe |
|------|---------|
| **Versão atual** | 2.3.0 |
| **Compatibilidade Java 21** | ✅ Compatível |
| **Recomendação** | Atualizar para 2.6+ para melhor suporte a Spring Boot 3.3 |

```xml
<version>2.6.0</version>
```

### 2.4 Mockito / JUnit 5

| Item | Detalhe |
|------|---------|
| **Versão atual** | Herdada do Spring Boot 3.2.0 |
| **Compatibilidade Java 21** | ✅ Compatível |
| **Risco** | 🟢 Nenhum com Spring Boot 3.2/3.3 |

> ⚠️ **Se atualizar para Spring Boot 3.4+:** `@MockBean` (de `org.springframework.boot.test.mock.mockito`) é depreciado. O substituto é `@MockitoBean` (de `org.springframework.test.context.bean.override.mockito`). Isso afetaria **4 classes de teste**:
> - `ProductControllerTest`
> - `ProductComparisonControllerTest`  
> - `TemplateControllerTest`
> - `ApiKeyInterceptorTest`

---

## 3. Mudanças de Comportamento do Java 17 → 21

### 3.1 UTF-8 como Encoding Padrão (Java 18+, JEP 400)

**Impacto:** 🟢 **Nenhum** — O projeto já define `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` no `pom.xml`. O `ProductRepository` usa Jackson `ObjectMapper` que opera em UTF-8 por padrão.

### 3.2 Remoção do SecurityManager (Java 17: depreciado → Java 21: permanentemente desativado)

**Impacto:** 🟢 **Nenhum** — Não há uso de `SecurityManager` no projeto.

### 3.3 Depreciação de Finalization (Java 18+, JEP 421)

**Impacto:** 🟢 **Nenhum** — Nenhum método `finalize()` no código-fonte.

### 3.4 Stronger Encapsulation of JDK Internals (Java 17+)

**Impacto:** 🟢 **Nenhum** — Nenhum uso de `sun.*`, `com.sun.*`, ou APIs internas do JDK.

### 3.5 Mudanças em `java.lang.Thread` e Thread Groups (Java 21)

**Impacto:** 🟢 **Nenhum** — O projeto não cria threads diretamente. A concorrência é gerenciada via `ReentrantLock` no `ProductRepository` e `ConcurrentHashMap`/`volatile` no `ProductTemplateService`.

### 3.6 Deprecação de `Thread.stop()`, `Thread.suspend()`, `Thread.resume()`

**Impacto:** 🟢 **Nenhum** — Não utilizados.

---

## 4. Análise do Código-Fonte Atual

### 4.1 Funcionalidades Java 16+ já em uso

| Feature | Arquivo | Linha | Status |
|---------|---------|-------|--------|
| Pattern matching `instanceof` | `GlobalExceptionHandler.java` | `if (error instanceof FieldError fieldError)` | ✅ Funciona no Java 21 |

### 4.2 Switch Statements (Candidatos a Modernização)

O projeto possui **3 blocos `switch`** tradicionais que funcionam perfeitamente no Java 21, mas podem ser modernizados:

| Arquivo | Método | Tipo |
|---------|--------|------|
| `ProductService.java` | `extractProductFields()` | `switch` com 9 cases + `default` |
| `ProductService.java` | `validateRequiredField()` | `switch` com 3 cases + `default` |
| `ProductService.java` | `validateSpecValueType()` | `switch` com 3 cases + `default` |

### 4.3 Concorrência (Análise de Compatibilidade com Virtual Threads)

| Mecanismo | Arquivo | Compatível com VT? |
|-----------|---------|---------------------|
| `ReentrantLock` | `ProductRepository.java` | ✅ Sim (não faz pin de virtual thread) |
| `volatile` | `ProductTemplateService.java` | ✅ Sim |
| `ConcurrentHashMap` | `ProductTemplateService.java` | ✅ Sim |
| `ThreadLocalRandom` | `ProductRepository.java` | ✅ Sim |

> **Nenhum `synchronized` encontrado** no projeto — excelente para virtual threads.

### 4.4 APIs Usadas — Verificação Completa

| API | Arquivos | Java 21 |
|-----|----------|---------|
| `java.nio.file.Files` / `Paths` | `ProductRepository` | ✅ Sem mudanças |
| `java.util.stream.*` (filter, map, collect, toList) | Múltiplos | ✅ Sem mudanças |
| `java.util.Optional` | Múltiplos | ✅ Sem mudanças |
| `java.util.concurrent.locks.ReentrantLock` | `ProductRepository` | ✅ Sem mudanças |
| `java.util.concurrent.ConcurrentHashMap` | `ProductTemplateService` | ✅ Sem mudanças |
| `java.util.UUID` | `ProductRepository` | ✅ Sem mudanças |
| `jakarta.validation.*` | Controllers, Models | ✅ Sem mudanças |
| `jakarta.annotation.PostConstruct` | `ProductTemplateService` | ✅ Sem mudanças |
| `jakarta.servlet.http.*` | `ApiKeyInterceptor` | ✅ Sem mudanças |

> ✅ O projeto já usa namespace `jakarta.*` (não `javax.*`), portanto **não há migração Jakarta necessária**.

---

## 5. Oportunidades de Modernização (Opcionais)

### 5.1 Virtual Threads (JEP 444) — ⭐ Recomendado

O projeto é um **excelente candidato** para virtual threads:

- ✅ Faz I/O de arquivo (leitura/escrita de `products.json`)
- ✅ Usa `ReentrantLock` (não `synchronized` — sem risco de thread pinning)
- ✅ Sem `ThreadLocal` problemático
- ✅ Spring Boot 3.2+ suporta nativamente

**Habilitação simples em `application.properties`:**

```properties
spring.threads.virtual.enabled=true
```

Isso faz o Tomcat usar virtual threads para processar requisições HTTP, melhorando throughput em cenários de alta concorrência com I/O.

### 5.2 Switch Expressions (JEP 361) + Pattern Matching for Switch (JEP 441)

**Exemplo — `extractProductFields()` no `ProductService.java`:**

```java
// ANTES (Java 17)
switch (filterLower) {
    case "description":
        if (product.getDescription() != null && !product.getDescription().isEmpty())
            result.put("description", product.getDescription());
        break;
    case "imageurl":
        if (product.getImageUrl() != null && !product.getImageUrl().isBlank())
            result.put("imageUrl", product.getImageUrl());
        break;
    // ... mais cases com break
}

// DEPOIS (Java 21) — arrow syntax
switch (filterLower) {
    case "description" -> {
        if (product.getDescription() != null && !product.getDescription().isEmpty())
            result.put("description", product.getDescription());
    }
    case "imageurl" -> {
        if (product.getImageUrl() != null && !product.getImageUrl().isBlank())
            result.put("imageUrl", product.getImageUrl());
    }
    // ... sem break necessário
}
```

### 5.3 Record Classes (JEP 395)

Candidatos a se tornarem `record`:

| Classe Atual | Viabilidade | Notas |
|--------------|------------|-------|
| `MeasurableValue` | ⚠️ Parcial | Usa `@Builder` + setters mutáveis — records são imutáveis |
| `ErrorResponse` | ✅ Bom candidato | Simples DTO de resposta |
| `ProductComparisonResponse` | ✅ Bom candidato | DTO de saída |
| `ProductFilter` | ❌ Não recomendado | Tem `validate()` com lógica de negócio e setters |
| `PageResponse<T>` | ⚠️ Parcial | Genérico com factory methods estáticos |
| `FieldDefinition` | ✅ Bom candidato | Simples DTO de configuração |

> ⚠️ **Atenção:** Converter para records remove `@Builder` do Lombok. Avaliar se o trade-off vale para cada caso.

### 5.4 Sequenced Collections (JEP 431)

Java 21 adiciona `getFirst()`, `getLast()`, `reversed()` a `List`, `LinkedHashMap`, `LinkedHashSet`. No projeto, o uso de `LinkedHashMap` e `LinkedHashSet` em `ProductService.java` poderia se beneficiar.

### 5.5 `Collectors.toList()` → `.toList()`

O projeto já usa `.toList()` (Java 16+) em alguns lugares (`searchProducts`), mas ainda usa `Collectors.toList()` em outros (`ProductTemplateService.getValidTypeNames()`, `ProductService.compareProducts()`). Pode-se padronizar.

> ⚠️ **Nota:** `.toList()` retorna lista **imutável**, enquanto `Collectors.toList()` retorna lista **mutável**. Verificar se algum código downstream modifica a lista.

### 5.6 `var` (Local Variable Type Inference)

Não utilizado no projeto. Pode ser adotado para simplificar declarações de variáveis locais onde o tipo é óbvio:

```java
// Antes
List<Product> products = repository.findAll();

// Depois (opcional)
var products = repository.findAll();
```

---

## 6. Checklist de Migração

### Fase 1 — Preparação
- [ ] Instalar JDK 21 no ambiente de desenvolvimento
- [ ] Verificar compatibilidade do IDE (VS Code + Extension Pack for Java)
- [ ] Backup do projeto / criar branch de migração

### Fase 2 — Mudanças Mínimas (Compilação)
- [ ] Alterar `<java.version>21</java.version>` no `pom.xml`
- [ ] (Recomendado) Atualizar Spring Boot para `3.2.5` ou `3.3.x`
- [ ] (Recomendado) Fixar versão do Lombok ≥ `1.18.32`
- [ ] (Recomendado) Atualizar SpringDoc para `2.6.0`
- [ ] Executar `mvn clean compile` — verificar sucesso

### Fase 3 — Validação
- [ ] Executar `mvn clean test` — todos os 92 testes devem passar
- [ ] Executar `mvn spring-boot:run` — verificar inicialização
- [ ] Testar endpoints manualmente (Swagger UI em `/docs`)
- [ ] Verificar logs de warning por APIs depreciadas

### Fase 4 — Modernização (Opcional)
- [ ] Habilitar virtual threads (`spring.threads.virtual.enabled=true`)
- [ ] Converter `switch` statements para arrow syntax
- [ ] Padronizar uso de `.toList()` vs `Collectors.toList()`
- [ ] Avaliar conversão de DTOs simples para records
- [ ] Adotar `var` onde apropriado

### Fase 5 — Se Atualizar para Spring Boot 3.4+
- [ ] Migrar `@MockBean` → `@MockitoBean` em 4 classes de teste
- [ ] Atualizar imports de `org.springframework.boot.test.mock.mockito` → `org.springframework.test.context.bean.override.mockito`

---

## 7. Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Lombok incompatível com Java 21 | 🟢 Baixa | 🔴 Alto | Fixar versão ≥ 1.18.32 |
| Testes falhando após migração | 🟢 Baixa | 🟡 Médio | Executar suite completa antes de mergear |
| Virtual threads causam problemas | 🟢 Baixa | 🟡 Médio | Feature flag — desligar se necessário |
| SpringDoc incompatível | 🟢 Baixa | 🟡 Médio | Atualizar para 2.6+ |
| Mudança de encoding default | 🟢 Muito baixa | 🟢 Baixo | Projeto já usa UTF-8 explicitamente |

---

## 8. Conclusão

A migração para Java 21 é **segura e recomendada** para este projeto. Os principais benefícios são:

1. **Suporte LTS estendido** — Java 21 é LTS (suporte até 2031), enquanto Java 17 tem suporte até 2029
2. **Virtual threads** — Potencial melhoria significativa de throughput para operações de I/O (leitura/escrita de JSON)
3. **Linguagem moderna** — Switch expressions, pattern matching, sequenced collections
4. **Ecossistema atualizado** — Melhor suporte de bibliotecas e ferramentas

O esforço estimado para a migração mínima (Fases 1-3) é de **~30 minutos**. A modernização completa (Fase 4) pode levar **~2-4 horas** dependendo do escopo escolhido.
