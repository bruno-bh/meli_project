package com.meli.productapi.service;

import com.meli.productapi.exception.ProductNotFoundException;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("123")
                .name("Teste Produto")
                .description("Descrição do teste")
                .price(99.99)
                .size("M")
                .weight(1.5)
                .color("Vermelho")
                .type("ELETRÔNICOS")
                .rating(4.5)
                .specifications(new HashMap<>(Map.of("marca", "Samsung", "voltagem", "110V")))
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os produtos sem filtros")
    void testGetAllProducts() {
        List<Product> products = List.of(testProduct);
        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder().build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(1, result.size());
        assertEquals(testProduct.getName(), result.get(0).getName());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar produto por ID")
    void testGetProductById() {
        when(repository.findById("123")).thenReturn(Optional.of(testProduct));

        Product result = productService.getProductById("123");

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("Teste Produto", result.getName());
        verify(repository, times(1)).findById("123");
    }

    @Test
    @DisplayName("Deve lançar exceção quando produto não encontrado")
    void testGetProductByIdNotFound() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById("999");
        });

        verify(repository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Deve criar um novo produto")
    void testCreateProduct() {
        Product newProduct = Product.builder()
                .name("Novo Produto")
                .description("Descrição nova")
                .price(50.0)
                .size("G")
                .weight(2.0)
                .color("Azul")
                .type("ROUPAS")
                .rating(3.8)
                .specifications(new HashMap<>(Map.of("tamanho_usa", "XL")))
                .build();

        when(repository.save(any(Product.class))).thenReturn(newProduct);

        Product result = productService.createProduct(newProduct);

        assertNotNull(result);
        assertEquals("Novo Produto", result.getName());
        verify(repository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios ao criar produto")
    void testCreateProductWithInvalidName() {
        Product invalidProduct = Product.builder()
                .name("")
                .description("Descrição")
                .price(50.0)
                .type("ROUPAS")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });
    }

    @Test
    @DisplayName("Deve validar preço ao criar produto")
    void testCreateProductWithInvalidPrice() {
        Product invalidProduct = Product.builder()
                .name("Produto")
                .description("Descrição")
                .price(-10.0)
                .type("ROUPAS")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });
    }

    @Test
    @DisplayName("Deve validar tipo ao criar produto")
    void testCreateProductWithInvalidType() {
        Product invalidProduct = Product.builder()
                .name("Produto")
                .description("Descrição")
                .price(50.0)
                .type("")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });
    }

    @Test
    @DisplayName("Deve validar name null ao criar produto")
    void testCreateProductWithNullName() {
        Product invalidProduct = Product.builder()
                .name(null)
                .price(50.0)
                .type("CELLPHONES")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });
        
        assertTrue(exception.getMessage().contains("Nome do produto é obrigatório"));
    }

    @Test
    @DisplayName("Deve validar price null ao criar produto")
    void testCreateProductWithNullPrice() {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(null)
                .type("CELLPHONES")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });
        
        assertTrue(exception.getMessage().contains("Preço do produto"));
    }

    @Test
    @DisplayName("Deve validar type null ao criar produto")
    void testCreateProductWithNullType() {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(50.0)
                .type(null)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });
        
        assertTrue(exception.getMessage().contains("Tipo do produto é obrigatório"));
    }

    @Test
    @DisplayName("Deve atualizar um produto existente")
    void testUpdateProduct() {
        Product updateData = Product.builder()
                .name("Produto Atualizado")
                .description("Descrição atualizada")
                .price(120.0)
                .size("G")
                .weight(2.0)
                .color("Verde")
                .type("ELETRÔNICOS")
                .rating(4.2)
                .specifications(new HashMap<>(Map.of("modelo", "2024")))
                .build();

        Product updatedProduct = Product.builder()
                .id("123")
                .name("Produto Atualizado")
                .description("Descrição atualizada")
                .price(120.0)
                .size("G")
                .weight(2.0)
                .color("Verde")
                .type("ELETRÔNICOS")
                .rating(4.2)
                .specifications(new HashMap<>(Map.of("modelo", "2024")))
                .build();

        when(repository.findById("123")).thenReturn(Optional.of(testProduct));
        when(repository.save(any(Product.class))).thenReturn(updatedProduct);

        Product result = productService.updateProduct("123", updateData);

        assertNotNull(result);
        assertEquals("Produto Atualizado", result.getName());
        assertEquals(120.0, result.getPrice());
        verify(repository, times(1)).findById("123");
        verify(repository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve deletar um produto")
    void testDeleteProduct() {
        Product product = Product.builder()
                .id("123")
                .name("Produto Teste")
                .description("Descrição")
                .price(99.99)
                .build();
        
        when(repository.findById("123")).thenReturn(Optional.of(product));

        productService.deleteProduct("123");

        verify(repository, times(1)).findById("123");
        verify(repository, times(1)).deleteById("123");
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar produto inexistente")
    void testDeleteProductNotFound() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.deleteProduct("999");
        });

        verify(repository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Deve retornar total de produtos")
    void testGetTotalProducts() {
        when(repository.count()).thenReturn(5L);

        long result = productService.getTotalProducts();

        assertEquals(5L, result);
        verify(repository, times(1)).count();
    }

    @Test
    @DisplayName("Deve buscar produtos por nome e tipo")
    void testSearchProductsByNameAndType() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(999.99)
                .description("Smartphone Apple")
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("iPhone 15")
                .type("CELLPHONES")
                .price(1099.99)
                .description("Smartphone Apple")
                .build();

        Product product3 = Product.builder()
                .id("3")
                .name("Samsung TV")
                .type("TV")
                .price(599.99)
                .description("Smart TV")
                .build();

        List<Product> allProducts = List.of(product1, product2, product3);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .name("iPhone")
                .type("CELLPHONES")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getType().equals("CELLPHONES")));
        assertTrue(result.stream().allMatch(p -> p.getName().contains("iPhone")));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar produtos apenas por nome (type null)")
    void testSearchProductsByNameOnly() {
        Product product1 = Product.builder()
                .id("1")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(799.99)
                .description("Smartphone Samsung")
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung TV")
                .type("TV")
                .price(599.99)
                .description("Smart TV")
                .build();

        List<Product> allProducts = List.of(product1, product2);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .name("Samsung")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getName().contains("Samsung")));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar produtos apenas por tipo (name null)")
    void testSearchProductsByTypeOnly() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(999.99)
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(799.99)
                .build();

        List<Product> allProducts = List.of(product1, product2);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .type("CELLPHONES")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getType().equals("CELLPHONES")));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nenhum produto encontrado")
    void testSearchProductsNoResults() {
        Product product = Product.builder()
                .id("1")
                .name("iPad")
                .type("TABLETS")
                .price(599.99)
                .build();

        List<Product> allProducts = List.of(product);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .name("iPhone")
                .type("CELLPHONES")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(0, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar produtos por descrição")
    void testSearchProductsByDescription() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(999.99)
                .description("Smartphone Apple com chip A15")
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("MacBook Pro")
                .type("COMPUTERS")
                .price(2999.99)
                .description("Laptop Apple com chip M2")
                .build();

        List<Product> allProducts = List.of(product1, product2);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .description("Apple")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getDescription().contains("Apple")));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar produtos por faixa de preço")
    void testSearchProductsByPriceRange() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(999.99)
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(799.99)
                .build();

        Product product3 = Product.builder()
                .id("3")
                .name("Nokia")
                .type("CELLPHONES")
                .price(299.99)
                .build();

        List<Product> allProducts = List.of(product1, product2, product3);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .priceMin(700.0)
                .priceMax(1000.0)
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getPrice() >= 700.0 && p.getPrice() <= 1000.0));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve aplicar paginação corretamente")
    void testSearchProductsWithPagination() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(200.0).build(),
                Product.builder().id("3").name("P3").type("CELLPHONES").price(300.0).build(),
                Product.builder().id("4").name("P4").type("CELLPHONES").price(400.0).build(),
                Product.builder().id("5").name("P5").type("CELLPHONES").price(500.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        // Página 1, tamanho 2
        ProductFilter filter1 = ProductFilter.builder()
                .page(1)
                .pageSize(2)
                .build();
        List<Product> page1 = productService.searchProducts(filter1);
        assertEquals(2, page1.size());
        assertEquals("P1", page1.get(0).getName());
        assertEquals("P2", page1.get(1).getName());

        // Página 2, tamanho 2
        ProductFilter filter2 = ProductFilter.builder()
                .page(2)
                .pageSize(2)
                .build();
        List<Product> page2 = productService.searchProducts(filter2);
        assertEquals(2, page2.size());
        assertEquals("P3", page2.get(0).getName());
        assertEquals("P4", page2.get(1).getName());

        // Página 3, tamanho 2
        ProductFilter filter3 = ProductFilter.builder()
                .page(3)
                .pageSize(2)
                .build();
        List<Product> page3 = productService.searchProducts(filter3);
        assertEquals(1, page3.size());
        assertEquals("P5", page3.get(0).getName());
    }

    @Test
    @DisplayName("Deve retornar todos produtos quando pageSize é null")
    void testSearchProductsWithoutPagination() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(200.0).build(),
                Product.builder().id("3").name("P3").type("CELLPHONES").price(300.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder().build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(3, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando página está fora dos limites")
    void testSearchProductsPageOutOfBounds() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(200.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder()
                .page(10)
                .pageSize(2)
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(0, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve lançar exceção quando priceMin é zero ou negativo")
    void testSearchProductsWithInvalidPriceMin() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .priceMin(0.0)
                    .build();
            productService.searchProducts(filter);
        });
        
        assertTrue(exception.getMessage().contains("Preço mínimo deve ser maior que zero"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .priceMin(-10.0)
                    .build();
            productService.searchProducts(filter);
        });
        
        assertTrue(exception.getMessage().contains("Preço mínimo deve ser maior que zero"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando priceMax é zero ou negativo")
    void testSearchProductsWithInvalidPriceMax() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .priceMax(0.0)
                    .build();
            productService.searchProducts(filter);
        });
        
        assertTrue(exception.getMessage().contains("Preço máximo deve ser maior que zero"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .priceMax(-5.0)
                    .build();
            productService.searchProducts(filter);
        });
        
        assertTrue(exception.getMessage().contains("Preço máximo deve ser maior que zero"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando priceMax é menor que priceMin")
    void testSearchProductsWithMaxLessThanMin() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .priceMin(1000.0)
                    .priceMax(500.0)
                    .build();
            productService.searchProducts(filter);
        });
        
        assertTrue(exception.getMessage().contains("Preço máximo não pode ser menor que o preço mínimo"));
    }

    @Test
    @DisplayName("Deve aceitar priceMin e priceMax válidos")
    void testSearchProductsWithValidPriceRange() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(500.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        // Deve funcionar sem lançar exceção
        ProductFilter filter = ProductFilter.builder()
                .priceMin(50.0)
                .priceMax(1000.0)
                .build();
        List<Product> result = productService.searchProducts(filter);
        
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve lançar exceção quando pageSize é zero")
    void testSearchProductsWithZeroPageSize() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(200.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .page(1)
                    .pageSize(0)
                    .build();
            productService.searchProducts(filter);
        });
        
        assertTrue(exception.getMessage().contains("Tamanho da página deve ser maior que zero"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando pageSize é negativo")
    void testSearchProductsWithNegativePageSize() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(200.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .page(1)
                    .pageSize(-5)
                    .build();
            productService.searchProducts(filter);
        });
        
        assertTrue(exception.getMessage().contains("Tamanho da página deve ser maior que zero"));
    }

    @Test
    @DisplayName("Deve tratar page null como página 1")
    void testSearchProductsWithNullPage() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(200.0).build(),
                Product.builder().id("3").name("P3").type("CELLPHONES").price(300.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        // page=null deve ser tratado como page=1
        ProductFilter filter = ProductFilter.builder()
                .pageSize(2)
                .build();
        List<Product> result = productService.searchProducts(filter);
        
        assertEquals(2, result.size());
        assertEquals("P1", result.get(0).getName());
        assertEquals("P2", result.get(1).getName());
    }

    @Test
    @DisplayName("Deve tratar page zero ou negativo como página 1")
    void testSearchProductsWithInvalidPage() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(100.0).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(200.0).build()
        );

        when(repository.findAll()).thenReturn(products);

        // page=0 deve ser tratado como page=1
        ProductFilter filterZero = ProductFilter.builder()
                .page(0)
                .pageSize(2)
                .build();
        List<Product> resultZero = productService.searchProducts(filterZero);
        assertEquals(2, resultZero.size());
        assertEquals("P1", resultZero.get(0).getName());

        // page=-1 deve ser tratado como page=1
        ProductFilter filterNegative = ProductFilter.builder()
                .page(-1)
                .pageSize(2)
                .build();
        List<Product> resultNegative = productService.searchProducts(filterNegative);
        assertEquals(2, resultNegative.size());
        assertEquals("P1", resultNegative.get(0).getName());
    }
}
