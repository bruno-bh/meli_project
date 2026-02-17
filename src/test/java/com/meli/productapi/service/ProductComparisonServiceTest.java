package com.meli.productapi.service;

import com.meli.productapi.exception.IncompatibleProductTypesException;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductComparisonResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do ProductService - Comparação")
class ProductComparisonServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService productService;

    private Product iphone;
    private Product samsung;
    private Product tshirt;

    @BeforeEach
    void setUp() {
        // Produto 1: iPhone (CELLPHONES)
        iphone = Product.builder()
                .id("iphone-1")
                .name("iPhone 15 Pro")
                .description("Apple smartphone")
                .imageUrl("https://picsum.photos/400/600?random=1")
                .price(999.99)
                .size("6.1 inches")
                .weight(0.187)
                .color("Space Black")
                .type("CELLPHONES")
                .rating(4.8)
                .specifications(new HashMap<>(Map.of(
                        "brand", "Apple",
                        "storage_gb", "256",
                        "memory_gb", "8",
                        "camera_mp", "48"
                )))
                .build();

        // Produto 2: Samsung (CELLPHONES)
        samsung = Product.builder()
                .id("samsung-1")
                .name("Samsung Galaxy S24")
                .description("Samsung smartphone")
                .imageUrl("https://picsum.photos/400/600?random=2")
                .price(899.99)
                .size("6.2 inches")
                .weight(0.167)
                .color("Phantom Black")
                .type("CELLPHONES")
                .rating(4.7)
                .specifications(new HashMap<>(Map.of(
                        "brand", "Samsung",
                        "storage_gb", "256",
                        "memory_gb", "12",
                        "camera_mp", "50"
                )))
                .build();

        // Produto 3: T-Shirt (CLOTHING)
        tshirt = Product.builder()
                .id("tshirt-1")
                .name("Premium T-Shirt")
                .description("Cotton t-shirt")
                .price(29.99)
                .size("M")
                .weight(0.25)
                .color("White")
                .type("CLOTHING")
                .rating(4.5)
                .specifications(new HashMap<>(Map.of(
                        "size_us", "M",
                        "material", "100% cotton"
                )))
                .build();
    }

    @Test
    @DisplayName("Deve comparar 2 celulares com todos os campos")
    void testCompareProductsAllFields() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        ProductComparisonResponse response = productService.compareProducts(ids, null);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(2, response.getProductCount());
        assertTrue(response.getFieldCount() > 0);
        
        // Verificar que os produtos sempre têm os campos obrigatórios (id e name)
        Map<String, Object> firstProduct = response.getProducts().get(0);
        assertTrue(firstProduct.containsKey("id"));
        assertTrue(firstProduct.containsKey("name"));
        // Outros campos estão presentes quando sem filtros
        assertTrue(firstProduct.containsKey("description"));
        assertTrue(firstProduct.containsKey("imageUrl"));
        assertTrue(firstProduct.containsKey("price"));
    }

    @Test
    @DisplayName("Deve comparar celulares com filtros específicos")
    void testCompareProductsWithSpecificFilters() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        List<String> filters = Arrays.asList("price", "memory_gb", "camera_mp");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(3, response.getFieldCount());
        assertEquals(filters, response.getAppliedFilters());
        
        // Validar que os dados de comparação contêm os campos obrigatórios + filtros
        Map<String, Object> firstProduct = response.getProducts().get(0);
        // Campos obrigatórios sempre presentes
        assertTrue(firstProduct.containsKey("id"));
        assertTrue(firstProduct.containsKey("name"));
        // Description e imageUrl sempre incluídos quando não vazios
        assertTrue(firstProduct.containsKey("description"));
        assertTrue(firstProduct.containsKey("imageUrl"));
        // Campos dos filtros
        assertTrue(firstProduct.containsKey("price"));
        assertTrue(firstProduct.containsKey("memory_gb"));
        assertTrue(firstProduct.containsKey("camera_mp"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao comparar produtos de tipos diferentes")
    void testCompareIncompatibleTypes() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("tshirt-1")).thenReturn(Optional.of(tshirt));

        List<String> ids = Arrays.asList("iphone-1", "tshirt-1");

        IncompatibleProductTypesException exception = assertThrows(
                IncompatibleProductTypesException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("tipos diferentes"));
    }

    @Test
    @DisplayName("Deve lançar exceção com lista vazia de IDs")
    void testCompareEmptyIds() {
        List<String> ids = new ArrayList<>();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("pelo menos um ID"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando ID não existe")
    void testCompareProductNotFound() {
        when(repository.findById("invalid-id")).thenReturn(Optional.empty());

        List<String> ids = Arrays.asList("invalid-id");

        assertThrows(
                com.meli.productapi.exception.ProductNotFoundException.class,
                () -> productService.compareProducts(ids, null)
        );
    }

    @Test
    @DisplayName("Deve comparar 3 produtos do mesmo tipo")
    void testCompareThreeProducts() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        
        Product samsung2 = Product.builder()
                .id("samsung-2")
                .name("Samsung Galaxy A50")
                .type("CELLPHONES")
                .price(599.99)
                .rating(4.6)
                .build();
        
        when(repository.findById("samsung-2")).thenReturn(Optional.of(samsung2));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1", "samsung-2");
        List<String> filters = Arrays.asList("id", "name", "price");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(3, response.getProductCount());
        assertEquals(3, response.getFieldCount());
    }

    @Test
    @DisplayName("Deve incluir campos do specifications quando filtro pedir")
    void testCompareWithSpecificationsFields() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        List<String> filters = Arrays.asList("name", "camera_mp", "memory_gb");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);

        assertNotNull(response);
        Map<String, Object> firstProduct = response.getProducts().get(0);
        assertTrue(firstProduct.containsKey("camera_mp"));
        assertTrue(firstProduct.containsKey("memory_gb"));
    }

    @Test
    @DisplayName("Deve comparar com um único produto")
    void testCompareSingleProduct() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));

        List<String> ids = Arrays.asList("iphone-1");
        ProductComparisonResponse response = productService.compareProducts(ids, null);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(1, response.getProductCount());
    }

    @Test
    @DisplayName("Deve ignorar campos de specifications inexistentes e incluir apenas os existentes")
    void testCompareWithNonExistentSpecificationFields() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        // Solicitando campos que não existem em CELLPHONES (composition, material)
        // e campos que existem (memory_gb, camera_mp)
        List<String> filters = Arrays.asList("price", "memory_gb", "camera_mp", "composition", "material");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(2, response.getProductCount());
        
        // Verificar que os campos existentes estão presentes
        Map<String, Object> firstProduct = response.getProducts().get(0);
        assertTrue(firstProduct.containsKey("id"));
        assertTrue(firstProduct.containsKey("name"));
        assertTrue(firstProduct.containsKey("price"));
        assertTrue(firstProduct.containsKey("memory_gb"));
        assertTrue(firstProduct.containsKey("camera_mp"));
        
        // Verificar que os campos inexistentes NÃO estão presentes (composition, material não existem em CELLPHONES)
        assertFalse(firstProduct.containsKey("composition"));
        assertFalse(firstProduct.containsKey("material"));
    }
}
