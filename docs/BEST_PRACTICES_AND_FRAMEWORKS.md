# 🏗️ Best Practices, Frameworks & Testing — Product API

> Documento técnico detalhando as boas práticas adotadas, os frameworks/bibliotecas utilizados, o motivo de cada escolha e a estratégia de testes do projeto.

---

## 📑 Sumário

1. [Stack Tecnológica](#-stack-tecnológica)
2. [Frameworks & Bibliotecas](#-frameworks--bibliotecas)
   - [Spring Boot 3.2.0](#1-spring-boot-320)
   - [Spring Web (spring-boot-starter-web)](#2-spring-web-spring-boot-starter-web)
   - [Spring Validation (spring-boot-starter-validation)](#3-spring-validation-spring-boot-starter-validation)
   - [Lombok](#4-lombok)
   - [Jackson Databind](#5-jackson-databind)
   - [Jackson YAML (jackson-dataformat-yaml)](#6-jackson-yaml-jackson-dataformat-yaml)
   - [SpringDoc OpenAPI (Swagger UI)](#7-springdoc-openapi-swagger-ui)
   - [Logback (via Spring Boot)](#8-logback-via-spring-boot)
   - [Maven](#9-maven)
3. [Boas Práticas Adotadas](#-boas-práticas-adotadas)
   - [Arquitetura em Camadas](#1-arquitetura-em-camadas-layered-architecture)
   - [Injeção de Dependência por Construtor](#2-injeção-de-dependência-por-construtor)
   - [Repository Interface Pattern](#3-repository-interface-pattern)
   - [Global Exception Handler](#4-global-exception-handler-restcontrolleradvice)
   - [DTO Pattern](#5-dto-pattern)
   - [Builder Pattern (via Lombok)](#6-builder-pattern-via-lombok)
   - [Configuração Externalizada](#7-configuração-externalizada)
   - [YAML-Driven Product Templates](#8-yaml-driven-product-templates)
   - [Segurança via API Key Interceptor](#9-segurança-via-api-key-interceptor)
   - [CORS Configuration](#10-cors-configuration)
   - [Thread Safety no Repository](#11-thread-safety-no-repository)
   - [Validação em Múltiplas Camadas](#12-validação-em-múltiplas-camadas)
   - [Logging Estruturado com SLF4J](#13-logging-estruturado-com-slf4j)
   - [Paginação Genérica](#14-paginação-genérica)
   - [Convenções REST](#15-convenções-rest)
4. [Estratégia de Testes](#-estratégia-de-testes)
   - [Frameworks de Teste](#frameworks-de-teste)
   - [Tipos de Testes Utilizados](#tipos-de-testes-utilizados)
   - [Mapa de Cobertura dos Testes](#mapa-de-cobertura-dos-testes)
   - [Boas Práticas nos Testes](#boas-práticas-nos-testes)
5. [Resumo Visual](#-resumo-visual)

---

## 🖥️ Stack Tecnológica

| Componente          | Tecnologia                   | Versão   |
|---------------------|------------------------------|----------|
| **Linguagem**       | Java                         | 17       |
| **Framework**       | Spring Boot                  | 3.2.0    |
| **Build Tool**      | Apache Maven                 | 3.6+     |
| **Serialização**    | Jackson (JSON + YAML)        | 2.15.x   |
| **Documentação API**| SpringDoc OpenAPI / Swagger  | 2.3.0    |
| **Code Generation** | Lombok                       | 1.18.x   |
| **Testes**          | JUnit 5 + Mockito + MockMvc  | —        |
| **Logging**         | SLF4J + Logback              | —        |
| **Persistência**    | Arquivo JSON (sem banco)     | —        |

---

## 📦 Frameworks & Bibliotecas

### 1. Spring Boot 3.2.0

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
```

**O que é:** Framework que simplifica a criação de aplicações Spring prontas para produção, com configuração automática (auto-configuration), servidor embarcado e convenções sensíveis.

**Por que foi utilizado:**
- **Auto-configuration:** Configura automaticamente beans como `ObjectMapper`, `DispatcherServlet`, validação, etc. — eliminando centenas de linhas de XML.
- **Embedded Tomcat:** Não precisa de servidor externo — a aplicação roda como um JAR executável.
- **Starter dependencies:** Agrupar dependências transitivas evita conflitos de versão.
- **Profiles:** Suporte nativo a perfis (prod/dev) para separar configurações (ex: `logback-spring.xml` com `<springProfile>`).

**O que melhora no código:**
- Reduz boilerplate de configuração em ~90%.
- Permite focar na lógica de negócio ao invés de infraestrutura.
- Oferece um ciclo de vida bem definido (startup, shutdown, health checks).

---

### 2. Spring Web (spring-boot-starter-web)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**O que é:** Módulo que fornece o Spring MVC completo com suporte a REST, serialização JSON via Jackson, e servidor Tomcat embarcado.

**Por que foi utilizado:**
- Fornece as anotações `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.
- Serialização/deserialização automática de JSON via Jackson.
- `ResponseEntity<T>` permite controle total sobre status HTTP, headers e body.
- Suporte nativo a `@Valid` para validação de request body/params.

**O que melhora no código:**
- Controllers ficam extremamente enxutos — apenas mapeamento HTTP.
- Conversão automática Java ↔ JSON sem código manual.
- Padronização de endpoints REST com anotações declarativas.

**Exemplo no projeto:**
```java
@GetMapping("/{id}")
public ResponseEntity<Product> getProductById(@PathVariable String id) {
    Product product = productService.getProductById(id);
    return ResponseEntity.ok(product);
}
```

---

### 3. Spring Validation (spring-boot-starter-validation)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**O que é:** Integração do Bean Validation (JSR-380/Jakarta Validation) com Spring, usando Hibernate Validator como implementação.

**Por que foi utilizado:**
- Permite validar DTOs com anotações como `@Min`, `@NotNull`, `@Size` diretamente nos campos.
- O `@Valid` no controller dispara a validação automaticamente antes de entrar no método.
- Erros de validação são capturados pelo `GlobalExceptionHandler` e retornados de forma estruturada.

**O que melhora no código:**
- Elimina `if/else` manual de validação nos controllers.
- Centraliza regras de validação nos DTOs (Single Source of Truth).
- Gera mensagens de erro padronizadas e amigáveis.

**Exemplo no projeto (`ProductFilter`):**
```java
@Min(value = 1, message = "Minimum price must be greater than zero")
private Double priceMin;

@Min(value = 1, message = "Page size must be greater than zero")
private Integer pageSize;
```

---

### 4. Lombok

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**O que é:** Biblioteca de geração de código em tempo de compilação que elimina boilerplate Java (getters, setters, constructors, builders, toString, equals/hashCode, logging).

**Por que foi utilizado:**
- **`@Builder`** — Cria objetos complexos de forma fluente e legível.
- **`@Data` / `@Getter` / `@Setter`** — Gera acessors automaticamente.
- **`@AllArgsConstructor` / `@NoArgsConstructor`** — Necessários para Jackson e Spring.
- **`@Slf4j`** — Injeta automaticamente um logger SLF4J (`log.info()`, `log.debug()`).

**O que melhora no código:**
- Reduz cada classe de modelo em 50–70% de linhas.
- Código mais legível — foco nos campos e na lógica, não em boilerplate.
- `@Builder` padroniza a criação de objetos em todo o projeto (inclusive testes).

**Exemplo — `Product` com Lombok:**
```java
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class Product {
    private String id;
    private String name;
    private MeasurableValue price;
    // ... sem getters/setters manuais
}
```

> **Nota:** O Lombok é excluído do JAR final via `spring-boot-maven-plugin` pois atua apenas em compile-time.

---

### 5. Jackson Databind

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

**O que é:** Biblioteca de serialização/deserialização JSON para Java — o padrão de facto no ecossistema Spring.

**Por que foi utilizado:**
- Serializa objetos Java em JSON automaticamente nas respostas HTTP.
- Deserializa JSON do request body em objetos Java.
- Suporte a anotações como `@JsonProperty`, `@JsonInclude` para controle fino da serialização.
- Usado também diretamente no `ProductRepository` para leitura/escrita do arquivo `products.json`.

**O que melhora no código:**
- **`@JsonInclude(Include.NON_NULL)`** — Omite campos nulos do JSON (respostas mais limpas).
- **`@JsonProperty`** — Mapeia nomes de propriedades quando o JSON difere do Java (ex: `display_name` → `displayName`).
- Configuração via `application.properties`:
  ```properties
  spring.jackson.serialization.indent-output=true
  spring.jackson.default-property-inclusion=non_null
  ```

---

### 6. Jackson YAML (jackson-dataformat-yaml)

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

**O que é:** Extensão do Jackson que adiciona suporte a leitura/escrita de YAML.

**Por que foi utilizado:**
- Permite carregar os **Product Templates** diretamente de um arquivo `product-templates.yaml`.
- O `ProductTemplateService` usa `new ObjectMapper(new YAMLFactory())` para deserializar o YAML em objetos Java.
- YAML é mais legível que JSON para configurações estruturadas com aninhamento.

**O que melhora no código:**
- Novos tipos de produto podem ser adicionados editando apenas o YAML — **sem recompilação**.
- Templates podem ser recarregados em runtime via `POST /api/v1/templates/reload`.
- Separação clara entre **dados de configuração** (YAML) e **código de negócio** (Java).

---

### 7. SpringDoc OpenAPI (Swagger UI)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**O que é:** Biblioteca que gera automaticamente a especificação OpenAPI 3.0 a partir dos controllers Spring e serve uma interface Swagger UI interativa.

**Por que foi utilizado:**
- Documentação da API gerada automaticamente a partir das anotações do código.
- Interface interativa para testar endpoints diretamente pelo navegador (`/docs`).
- Suporte a segurança (`X-API-KEY`) via `OpenApiConfig`.
- Anotações `@Operation`, `@ApiResponse`, `@Schema`, `@Parameter` enriquecem a documentação.

**O que melhora no código:**
- Documentação sempre sincronizada com o código — elimina docs desatualizados.
- Desenvolvedores frontend podem consumir a API sem documentação manual.
- Facilita testes manuais durante o desenvolvimento.

**Configuração customizada (`OpenApiConfig`):**
```java
@Bean
public OpenAPI productApiOpenAPI() {
    return new OpenAPI()
        .info(new Info().title("Product API — Mercado Libre Challenge"))
        .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
        .components(new Components()
            .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-KEY")));
}
```

---

### 8. Logback (via Spring Boot)

**O que é:** Framework de logging padrão do Spring Boot, configurado via `logback-spring.xml`.

**Por que foi utilizado:**
- Logging estruturado com níveis (DEBUG, INFO, WARN, ERROR).
- Suporte a **Spring Profiles** — console em dev, console + arquivo rotativo em prod.
- Rolling file appender com retenção de 30 dias e limite de 1GB.

**O que melhora no código:**
- Logs organizados por camada facilitam debugging.
- Rotação automática de logs evita consumo excessivo de disco.
- Uso de `@Slf4j` (Lombok) injeta o logger sem boilerplate.

**Configuração no projeto:**
```xml
<springProfile name="prod">
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/product-api.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>
</springProfile>
```

---

### 9. Maven

**O que é:** Ferramenta de build e gerenciamento de dependências para projetos Java.

**Por que foi utilizado:**
- Gerenciamento automático de dependências e versionamento via `pom.xml`.
- Ciclo de vida padronizado: `compile` → `test` → `package`.
- O `spring-boot-maven-plugin` gera um JAR executável com todas as dependências (fat JAR).
- Integração direta com ferramentas CI/CD.

---

## ✅ Boas Práticas Adotadas

### 1. Arquitetura em Camadas (Layered Architecture)

```
Controller → Service → Repository
```

| Camada       | Responsabilidade                                           |
|--------------|------------------------------------------------------------|
| **Controller** | Mapeamento HTTP, validação de entrada, delegação ao Service |
| **Service**    | Lógica de negócio, validação de domínio, orquestração       |
| **Repository** | Acesso a dados (abstração sobre JSON file)                  |
| **Exception**  | Tratamento centralizado de erros                            |
| **Model**      | Entidades, DTOs, templates                                  |
| **Config**     | Configurações transversais (CORS, Security, OpenAPI)        |

**Benefício:** Cada camada tem uma única responsabilidade. Mudanças no repositório não afetam controllers. Regras de negócio ficam isoladas no Service.

---

### 2. Injeção de Dependência por Construtor

```java
// ✅ Correto — usado em todo o projeto
public ProductService(ProductRepositoryInterface repository, ProductTemplateService templateService) {
    this.repository = repository;
    this.templateService = templateService;
}

// ❌ Evitado — field injection
@Autowired
private ProductRepositoryInterface repository;
```

**Benefícios:**
- Dependências são **explícitas** e **imutáveis** (podem ser `final`).
- Facilita testes — basta passar mocks no construtor.
- Detecta dependências circulares em tempo de compilação.
- Recomendado oficialmente pelo Spring Team.

---

### 3. Repository Interface Pattern

```java
public interface ProductRepositoryInterface {
    List<Product> findAll();
    Optional<Product> findById(String id);
    Product save(Product product);
    void deleteById(String id);
    boolean existsById(String id);
    long count();
}
```

**Benefício:** O `ProductService` depende da **interface**, não da implementação concreta. Isso permite:
- Trocar JSON file por banco de dados sem alterar uma linha do Service.
- Testar com mocks facilmente.
- Respeitar o **Princípio da Inversão de Dependência (DIP)** do SOLID.

---

### 4. Global Exception Handler (`@RestControllerAdvice`)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(...) { ... }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(...) { ... }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobal(...) { ... }
}
```

**Benefício:**
- **Todas** as exceções são tratadas em um único lugar.
- Respostas de erro sempre seguem o mesmo formato (`ErrorResponse`).
- Controllers não têm blocos try/catch — ficam limpos.
- Mapeamento claro: `ProductNotFoundException` → 404, `IllegalArgumentException` → 400, `IncompatibleProductTypesException` → 409.

---

### 5. DTO Pattern

O projeto utiliza DTOs especializados para diferentes propósitos:

| DTO                          | Propósito                                              |
|------------------------------|--------------------------------------------------------|
| `ProductFilter`              | Encapsula query params com validações                  |
| `ProductComparisonResponse`  | Resposta estruturada para comparação de produtos       |
| `PageResponse<T>`            | Wrapper genérico de paginação com metadados            |
| `ErrorResponse`              | Formato padronizado de erros da API                    |
| `MeasurableValue`            | Value Object para valores com unidade (price, size, weight) |

**Benefício:** Separa representação externa (API) da representação interna (domínio). Cada DTO tem exatamente os campos necessários para seu contexto.

---

### 6. Builder Pattern (via Lombok)

```java
Product product = Product.builder()
    .name("iPhone 15 Pro")
    .price(MeasurableValue.builder().value(5999.99).unit("BRL").build())
    .type("CELLPHONES")
    .specifications(Map.of("brand", "Apple"))
    .build();
```

**Benefício:**
- Criação de objetos complexos com muitos campos opcionais.
- Código auto-documentado — cada campo é nomeado explicitamente.
- Imutabilidade parcial — após o `build()`, os valores são definidos.
- Amplamente usado nos testes para criar objetos de teste legíveis.

---

### 7. Configuração Externalizada

Todas as configurações estão em `application.properties` — **nenhum valor hardcoded**:

```properties
product.data.file=data/products.json        # Caminho do arquivo de dados
product.template.file=data/product-templates.yaml  # Caminho dos templates
api.security.enabled=true                     # Habilita/desabilita segurança
api.security.key=meli-product-api-key-2025    # API Key
api.server.url=http://localhost:8080          # URL do servidor (OpenAPI)
```

**Benefício:** Trocar dados de produção para teste requer apenas outro `application.properties` — o código não muda.

---

### 8. YAML-Driven Product Templates

Os tipos de produto (CELLPHONES, COMPUTERS, CLOTHING, etc.) são definidos em `product-templates.yaml`:

```yaml
templates:
  CELLPHONES:
    display_name: "Smartphones"
    fields:
      price:
        type: number
        default_unit: "BRL"
        required: true
        comparable: true
    specifications:
      brand:
        type: text
        required: true
        comparable: false
```

**Benefícios:**
- **Zero recompilação** para adicionar novos tipos de produto.
- Template define quais campos são obrigatórios, comparáveis e suas unidades padrão.
- `POST /api/v1/templates/reload` permite recarregar em runtime.
- Cache via `ConcurrentHashMap` com leitura `volatile` para thread safety.

---

### 9. Segurança via API Key Interceptor

```java
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // Verifica header X-API-KEY
    }
}
```

**Benefícios:**
- Interceptor roda antes dos controllers — proteção transversal.
- Bypass automático para `OPTIONS` (CORS preflight) e endpoints de documentação.
- Configurável via properties — pode ser desabilitado para testes.
- Retorna `ErrorResponse` estruturado em caso de falha de autenticação.

---

### 10. CORS Configuration

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        // Filtro a nível de servlet — roda ANTES dos interceptors Spring MVC
    }
}
```

**Benefício:** Usando `CorsFilter` (servlet-level) ao invés de `@CrossOrigin` (controller-level), os headers CORS são adicionados mesmo que um interceptor bloqueie a request — essencial para reverse proxies e HTTPS termination.

---

### 11. Thread Safety no Repository

```java
private final ReentrantLock lock = new ReentrantLock();

public Product save(Product product) {
    lock.lock();
    try {
        // leitura, modificação e escrita atômica
    } finally {
        lock.unlock();
    }
}
```

**Benefício:** Operações de escrita no arquivo JSON são protegidas por lock, evitando race conditions em cenários de requests concorrentes.

---

### 12. Validação em Múltiplas Camadas

| Camada      | Tipo de Validação                                          |
|-------------|-------------------------------------------------------------|
| Controller  | `@Valid`, `@Min`, Bean Validation automática                |
| Service     | Validação de regras de negócio (nome não vazio, price > 0)  |
| Service     | Validação contra template (required fields, spec types)     |
| DTO         | `ProductFilter.validate()` — cross-field (priceMax ≥ priceMin) |

**Benefício:** Defense in depth — mesmo que uma camada falhe, a próxima valida.

---

### 13. Logging Estruturado com SLF4J

```java
log.info("Creating product: {}", product.getName());
log.warn("Product with ID {} not found", id);
log.debug("Filtered {} products from {} total", filtered, total);
```

**Benefícios:**
- Uso de placeholders `{}` em vez de concatenação (performance).
- Níveis adequados: `DEBUG` para busca/filtros, `INFO` para CRUD, `WARN` para não encontrados, `ERROR` para falhas.
- Nível configurável por package via properties.

---

### 14. Paginação Genérica

```java
public class PageResponse<T> {
    private List<T> content;
    private int page, pageSize, totalPages;
    private long totalElements;
    private boolean hasNext, hasPrevious;

    public static <T> PageResponse<T> of(List<T> items, int page, int pageSize) { ... }
    public static <T> PageResponse<T> ofAll(List<T> items) { ... }
}
```

**Benefícios:**
- Reutilizável para qualquer tipo de entidade (generics).
- Dois factory methods: `of()` para paginado e `ofAll()` para listagem completa.
- Metadados de navegação (`hasNext`, `hasPrevious`) facilitam o frontend.

---

### 15. Convenções REST

| Operação | Método | Endpoint                    | Status Code |
|----------|--------|-----------------------------|-------------|
| Listar   | GET    | `/api/v1/products`          | 200         |
| Buscar   | GET    | `/api/v1/products/{id}`     | 200 / 404   |
| Criar    | POST   | `/api/v1/products`          | 201         |
| Atualizar| PUT    | `/api/v1/products/{id}`     | 200 / 404   |
| Deletar  | DELETE | `/api/v1/products/{id}`     | 204 / 404   |
| Comparar | GET    | `/api/v1/products/compare`  | 200 / 409   |

**Benefícios:**
- Versionamento via URL (`/api/v1/`).
- Status codes HTTP semânticos (201 Created, 204 No Content, 409 Conflict).
- Verbos HTTP mapeados corretamente às operações CRUD.

---

## 🧪 Estratégia de Testes

### Frameworks de Teste

#### JUnit 5 (Jupiter)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**O que é:** Framework de testes unitários para Java — o padrão da indústria.

**Por que foi utilizado:**
- `@Test`, `@BeforeEach`, `@DisplayName` para organizar testes.
- `@ParameterizedTest` com `@CsvSource`, `@ValueSource`, `@NullAndEmptySource`, `@MethodSource` para testar múltiplos cenários com um único método.
- `@ExtendWith(MockitoExtension.class)` para integração com Mockito.
- Assertions fluentes: `assertEquals`, `assertThrows`, `assertDoesNotThrow`, `assertTrue`.

---

#### Mockito

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

**O que é:** Framework de mocking para simular dependências em testes unitários.

**Por que foi utilizado:**
- `@Mock` cria mocks automáticos do `ProductRepository` e `ProductTemplateService`.
- `when(...).thenReturn(...)` define comportamentos simulados.
- `verify(...)` confirma que métodos foram chamados corretamente.
- `lenient()` permite stubs que podem não ser utilizados em todos os cenários.
- `any()`, `anyList()`, `eq()` são matchers flexíveis para argumentos.

**O que melhora nos testes:**
- Testes do Service não dependem de arquivo JSON real.
- Testes do Controller não dependem do Service real.
- Isolamento total entre camadas — falhas são localizadas precisamente.

---

#### MockMvc (Spring Test)

**O que é:** Ferramenta do Spring Test que simula requests HTTP sem subir um servidor real.

**Por que foi utilizado:**
- Testa a camada HTTP (serialização, routing, status codes) sem rede.
- `@WebMvcTest` carrega apenas o controller sendo testado — inicialização rápida.
- Suporte a `.andExpect(jsonPath("$.field"))` para validar JSON de resposta.
- Integra com `@MockBean` para mockar serviços injetados nos controllers.

---

### Tipos de Testes Utilizados

#### 1. Testes Unitários de Service

**Arquivos:** `ProductServiceTest.java`, `ProductComparisonServiceTest.java`, `ProductTemplateServiceTest.java`

**Anotação:** `@ExtendWith(MockitoExtension.class)`

**O que testam:**
- Lógica de negócio isolada (CRUD, validação, filtro, paginação, comparação).
- Regras de validação (nome obrigatório, preço positivo, tipo válido, rating 0-5).
- Comportamento com dados inválidos (null, vazio, fora de range).
- Aplicação de unidades padrão dos templates.
- Validação de specifications (tipo numérico, formato de data, chaves desconhecidas).
- Comparação de produtos (mesmos tipos, tipos diferentes, filtros comparáveis).

**Por que testes unitários no Service:**
- É onde reside **toda** a lógica de negócio.
- Execução extremamente rápida (sem contexto Spring).
- Feedback imediato sobre regressões em regras de negócio.

**Técnicas utilizadas:**
```java
// Testes parametrizados para múltiplos cenários
@ParameterizedTest
@NullAndEmptySource
void testCreateProductWithInvalidName(String invalidName) { ... }

// Method source para objetos complexos
@MethodSource("invalidPriceProvider")
void testCreateProductWithInvalidPrice(MeasurableValue invalidPrice) { ... }

// Value source para valores escalares
@ValueSource(ints = {0, -5})
void testSearchProductsWithInvalidPageSize(int invalidPageSize) { ... }
```

---

#### 2. Testes de Controller (`@WebMvcTest`)

**Arquivos:** `ProductControllerTest.java`, `ProductComparisonControllerTest.java`, `TemplateControllerTest.java`

**Anotação:** `@WebMvcTest(Controller.class)`

**O que testam:**
- Mapeamento correto de endpoints (URL, método HTTP).
- Serialização/deserialização de request/response JSON.
- Status codes HTTP (200, 201, 204, 400, 404, 409).
- Validação de query params (Bean Validation via `@Min`).
- Tratamento de erros pela camada HTTP.
- Parsing de parâmetros (IDs separados por vírgula, filtros).

**Por que `@WebMvcTest`:**
- Carrega **apenas** o controller e componentes web (sem Service/Repository).
- Inicialização ~10x mais rápida que `@SpringBootTest`.
- Testa a camada HTTP de forma isolada.
- `@MockBean` substitui o Service por um mock.

**Exemplo:**
```java
@Test
void testCreateProduct() throws Exception {
    when(productService.createProduct(any())).thenReturn(testProduct);

    mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newProduct)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Novo Produto"));
}
```

---

#### 3. Testes de Integração (`@SpringBootTest`)

**Arquivo:** `ProductApiIntegrationTest.java`

**Anotação:** `@SpringBootTest` + `@AutoConfigureMockMvc`

**O que testam:**
- Fluxo completo end-to-end: Controller → Service → Repository → JSON file.
- Ciclo de vida completo do produto (Create → Read → Update → Delete).
- Comparação de produtos via HTTP com persistência real.
- Templates carregados do YAML real.
- Validação de campos obrigatórios via HTTP.

**Por que testes de integração:**
- Garantem que todas as camadas funcionam **juntas** corretamente.
- Detectam problemas de configuração que testes unitários não pegam.
- Validam o comportamento da API exatamente como um client externo veria.

**Isolamento de dados de teste:**
```properties
# src/test/resources/application.properties
product.data.file=data/test/products-test.json
product.template.file=data/test/product-templates-test.yaml
api.security.enabled=false
```

---

#### 4. Testes de Modelo/DTO

**Arquivos:** `MeasurableValueTest.java`, `PageResponseTest.java`, `ProductFilterTest.java`

**O que testam:**
- Construção correta via Builder.
- Serialização/deserialização Jackson (JSON ↔ Java).
- Lógica de paginação (`PageResponse.of()`, `PageResponse.ofAll()`).
- Validação cross-field (`priceMax >= priceMin`).
- Defaults corretos (null quando não informado).

**Por que testar modelos:**
- `PageResponse` tem lógica de paginação (slicing, metadados) — precisa de cobertura.
- `ProductFilter.validate()` tem regra de negócio (cross-field).
- Serialização incorreta pode quebrar toda a API silenciosamente.

---

#### 5. Testes de Repository

**Arquivo:** `ProductRepositoryTest.java`

**O que testam:**
- CRUD completo no arquivo JSON (findAll, findById, save, deleteById).
- Auto-geração de IDs incrementais.
- Geração automática de imageUrl e rating padrão.
- Persistência real em arquivo (verifica conteúdo do JSON).
- Comportamento com arquivo corrompido (RuntimeException).
- Update de produto existente (replace por ID).
- Operações idempotentes (delete de ID inexistente).

**Técnica:** Usa `@TempDir` do JUnit 5 para criar diretório temporário — cada teste roda com um arquivo JSON limpo.

```java
@TempDir
Path tempDir;

@BeforeEach
void setUp() throws IOException {
    productsFilePath = tempDir.resolve("products.json").toString();
    objectMapper.writeValue(new File(productsFilePath), List.of());
    repository = new ProductRepository(tempDir.toString(), productsFilePath, objectMapper);
}
```

---

#### 6. Testes de Exception Handler

**Arquivos:** `GlobalExceptionHandlerTest.java`, `ErrorResponseTest.java`

**O que testam:**
- Mapeamento correto de cada exceção para o status HTTP correspondente.
- Formato do JSON de erro (`status`, `error`, `message`, `fieldErrors`).
- Tratamento de JSON malformado (400).
- Tratamento de content type inválido (415).
- Mensagens amigáveis para type mismatch (ex: "priceMin" recebendo texto).
- Factory methods do `ErrorResponse` (`of()`, `ofValidation()`).
- Construtores (no-args, all-args, builder).

---

#### 7. Testes de Configuração/Segurança

**Arquivo:** `ApiKeyInterceptorTest.java`

**O que testam:**
- API Key válida permite acesso (200).
- API Key ausente retorna 401 com mensagem clara.
- API Key inválida retorna 401.
- Requests OPTIONS (CORS preflight) são bypassados.
- Segurança desabilitada permite acesso sem chave.

**Técnica:** Usa `MockHttpServletRequest` e `MockHttpServletResponse` do Spring Test para simular requests sem HTTP real.

---

### Mapa de Cobertura dos Testes

| Classe Testada                | Arquivo de Teste                       | Nº Testes | Tipo           |
|-------------------------------|----------------------------------------|-----------|----------------|
| `ProductService`              | `ProductServiceTest`                   | ~38       | Unitário       |
| `ProductService` (comparação) | `ProductComparisonServiceTest`         | 15        | Unitário       |
| `ProductTemplateService`      | `ProductTemplateServiceTest`           | 12        | Unitário       |
| `ProductController`           | `ProductControllerTest`                | 22        | Controller     |
| `ProductController` (compare) | `ProductComparisonControllerTest`      | 8         | Controller     |
| `TemplateController`          | `TemplateControllerTest`               | 4         | Controller     |
| `ProductRepository`           | `ProductRepositoryTest`                | 16        | Repository     |
| `GlobalExceptionHandler`      | `GlobalExceptionHandlerTest`           | 11        | Exception      |
| `ErrorResponse`               | `ErrorResponseTest`                    | 5         | Model/DTO      |
| `MeasurableValue`             | `MeasurableValueTest`                  | 3         | Model/DTO      |
| `PageResponse`                | `PageResponseTest`                     | 7         | Model/DTO      |
| `ProductFilter`               | `ProductFilterTest`                    | 8         | Model/DTO      |
| `ApiKeyInterceptor`           | `ApiKeyInterceptorTest`                | 5         | Config         |
| Fluxo E2E                     | `ProductApiIntegrationTest`            | 11        | Integração     |
| **Total**                     |                                        | **~165**  |                |

---

### Boas Práticas nos Testes

| Prática                             | Descrição                                                                                     |
|--------------------------------------|-----------------------------------------------------------------------------------------------|
| **`@DisplayName` descritivo**       | Todo método de teste possui `@DisplayName` em inglês descrevendo o comportamento esperado.     |
| **Arrange-Act-Assert**              | Todos os testes seguem o padrão AAA (setup, execução, verificação).                           |
| **Dados de teste isolados**         | `@TempDir` para repository, `@BeforeEach` para reset, arquivo de teste separado.              |
| **Testes parametrizados**           | `@ParameterizedTest` com `@CsvSource`, `@ValueSource`, `@NullAndEmptySource`, `@MethodSource`. |
| **Verificação de interações**       | `verify()` do Mockito confirma que métodos corretos foram chamados com os argumentos certos.   |
| **Happy path + Edge cases + Errors**| Cada funcionalidade testa o cenário feliz, casos limite (empty, null) e erros esperados.       |
| **Segurança desabilitada em testes**| `api.security.enabled=false` no `application.properties` de teste evita falsos negativos.      |
| **Assertions expressivas**          | Uso de `assertThrows` + verificação da mensagem da exceção para garantir a causa correta.     |

---

## 📊 Resumo Visual

```
┌─────────────────────────────────────────────────────┐
│                   Product API                        │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌─────────┐   ┌──────────┐   ┌──────────────────┐  │
│  │ Spring  │   │ Jackson  │   │ SpringDoc OpenAPI │  │
│  │  Boot   │   │ JSON/YAML│   │  (Swagger UI)    │  │
│  │ 3.2.0   │   │          │   │                  │  │
│  └────┬────┘   └────┬─────┘   └────────┬─────────┘  │
│       │              │                  │            │
│  ┌────▼────┐   ┌────▼─────┐   ┌────────▼─────────┐  │
│  │ Spring  │   │ Lombok   │   │ Bean Validation  │  │
│  │   Web   │   │ @Builder │   │ @Min, @Valid     │  │
│  │   MVC   │   │ @Slf4j   │   │                  │  │
│  └────┬────┘   └──────────┘   └──────────────────┘  │
│       │                                              │
│  ┌────▼──────────────────────────────────────────┐   │
│  │         Arquitetura em Camadas                 │   │
│  │                                                │   │
│  │  Controller ──▶ Service ──▶ Repository         │   │
│  │       │             │            │             │   │
│  │   @Valid       Validação     JSON File         │   │
│  │   HTTP map     Negócio      ReentrantLock      │   │
│  │   Swagger      Templates    Auto-ID            │   │
│  └────────────────────────────────────────────────┘   │
│                                                      │
│  ┌────────────────────────────────────────────────┐   │
│  │              Testes (JUnit 5 + Mockito)         │   │
│  │                                                │   │
│  │  Unit ──▶ Controller ──▶ Integration           │   │
│  │  @Mock     @WebMvcTest    @SpringBootTest      │   │
│  │  38+ tests  34 tests      11 tests             │   │
│  └────────────────────────────────────────────────┘   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

> **Autor:** Gerado automaticamente com base na análise completa do código-fonte do projeto Product API.
> **Data:** Fevereiro 2026
