package com.meli.productapi.service;

import com.meli.productapi.exception.IncompatibleProductTypesException;
import com.meli.productapi.model.MeasurableValue;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductComparisonResponse;
import com.meli.productapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductComparisonService — comparison logic tests")
class ProductComparisonServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ProductTemplateService templateService;

    private ProductService productService;

    private Product iphone;
    private Product samsung;
    private Product tshirt;

    private static MeasurableValue price(Double value) {
        return MeasurableValue.builder().value(value).unit("BRL").build();
    }

    private static MeasurableValue size(Double value, String unit) {
        return MeasurableValue.builder().value(value).unit(unit).build();
    }

    private static MeasurableValue weight(Double value) {
        return MeasurableValue.builder().value(value).unit("kg").build();
    }

    @BeforeEach
    void setUp() {
        productService = new ProductService(repository, templateService);

        // Product 1: iPhone (CELLPHONES)
        iphone = Product.builder()
                .id("iphone-1")
                .name("iPhone 15 Pro")
                .description("Apple smartphone")
                .imageUrl("https://picsum.photos/400/600?random=1")
                .price(price(999.99))
                .size(size(6.1, "inches"))
                .weight(weight(0.187))
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

        // Product 2: Samsung (CELLPHONES)
        samsung = Product.builder()
                .id("samsung-1")
                .name("Samsung Galaxy S24")
                .description("Samsung smartphone")
                .imageUrl("https://picsum.photos/400/600?random=2")
                .price(price(899.99))
                .size(size(6.2, "inches"))
                .weight(weight(0.167))
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

        // Product 3: T-Shirt (CLOTHING)
        tshirt = Product.builder()
                .id("tshirt-1")
                .name("Premium T-Shirt")
                .description("Cotton t-shirt")
                .price(price(29.99))
                .size(size(null, "M"))
                .weight(weight(0.25))
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
    @DisplayName("Should compare 2 cellphones with all fields")
    void testCompareProductsAllFields() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(
                List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight"));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        ProductComparisonResponse response = productService.compareProducts(ids, null);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(2, response.getProductCount());
        assertTrue(response.getFieldCount() > 0);
        
        // Verify mandatory fields (id and name) are always present
        Map<String, Object> firstProduct = response.getProducts().get(0);
        assertTrue(firstProduct.containsKey("id"));
        assertTrue(firstProduct.containsKey("name"));
        // Comparable fields from template are present
        assertTrue(firstProduct.containsKey("price"));
        // description and imageUrl are NOT included by default (not comparable)
        assertFalse(firstProduct.containsKey("description"));
        assertFalse(firstProduct.containsKey("imageUrl"));
    }

    @Test
    @DisplayName("Should compare cellphones with specific filters")
    void testCompareProductsWithSpecificFilters() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(
                List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight"));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        List<String> filters = Arrays.asList("price", "memory_gb", "camera_mp");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(3, response.getFieldCount());
        assertEquals(filters, response.getAppliedFilters());
        
        // Validate comparison data contains mandatory + filter fields
        Map<String, Object> firstProduct = response.getProducts().get(0);
        // Mandatory fields always present
        assertTrue(firstProduct.containsKey("id"));
        assertTrue(firstProduct.containsKey("name"));
        // Description and imageUrl NOT auto-included when not in filters
        assertFalse(firstProduct.containsKey("description"));
        assertFalse(firstProduct.containsKey("imageUrl"));
        // Filter fields
        assertTrue(firstProduct.containsKey("price"));
        assertTrue(firstProduct.containsKey("memory_gb"));
        assertTrue(firstProduct.containsKey("camera_mp"));
    }

    @Test
    @DisplayName("Should throw exception when comparing products of different types")
    void testCompareIncompatibleTypes() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("tshirt-1")).thenReturn(Optional.of(tshirt));

        List<String> ids = Arrays.asList("iphone-1", "tshirt-1");

        IncompatibleProductTypesException exception = assertThrows(
                IncompatibleProductTypesException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("different types"));
    }

    @Test
    @DisplayName("Should throw exception with empty ID list")
    void testCompareEmptyIds() {
        List<String> ids = new ArrayList<>();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("At least two product IDs"));
    }

    @Test
    @DisplayName("Should throw exception when ID does not exist")
    void testCompareProductNotFound() {
        when(repository.findById("invalid-id")).thenReturn(Optional.empty());

        List<String> ids = Arrays.asList("invalid-id", "another-invalid");

        assertThrows(
                com.meli.productapi.exception.ProductNotFoundException.class,
                () -> productService.compareProducts(ids, null)
        );
    }

    @Test
    @DisplayName("Should compare 3 products of the same type")
    void testCompareThreeProducts() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        
        Product samsung2 = Product.builder()
                .id("samsung-2")
                .name("Samsung Galaxy A50")
                .type("CELLPHONES")
                .price(price(599.99))
                .rating(4.6)
                .build();
        
        when(repository.findById("samsung-2")).thenReturn(Optional.of(samsung2));
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(
                List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight"));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1", "samsung-2");
        List<String> filters = Arrays.asList("id", "name", "price");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(3, response.getProductCount());
        assertEquals(3, response.getFieldCount());
    }

    @Test
    @DisplayName("Should include specification fields when requested by filter")
    void testCompareWithSpecificationsFields() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(
                List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight"));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        List<String> filters = Arrays.asList("name", "camera_mp", "memory_gb");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);

        assertNotNull(response);
        Map<String, Object> firstProduct = response.getProducts().get(0);
        assertTrue(firstProduct.containsKey("camera_mp"));
        assertTrue(firstProduct.containsKey("memory_gb"));
    }

    @Test
    @DisplayName("Should throw exception when comparing a single product")
    void testCompareSingleProduct() {
        List<String> ids = Arrays.asList("iphone-1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("At least two product IDs"));
    }

    @Test
    @DisplayName("Should throw exception when filter is not a comparable field")
    void testCompareWithNonComparableFilter() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(
                List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight"));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        List<String> filters = Arrays.asList("price", "brand");  // brand is not comparable

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.compareProducts(ids, filters)
        );

        assertTrue(exception.getMessage().contains("brand"));
        assertTrue(exception.getMessage().contains("not a comparable field"));
    }

    @Test
    @DisplayName("Should compare products with null specifications without NPE")
    void testCompareProductsWithNullSpecifications() {
        Product p1 = Product.builder()
                .id("1")
                .name("Product A")
                .type("CELLPHONES")
                .price(price(100.0))
                .specifications(null)
                .build();

        Product p2 = Product.builder()
                .id("2")
                .name("Product B")
                .type("CELLPHONES")
                .price(price(200.0))
                .specifications(null)
                .build();

        when(repository.findById("1")).thenReturn(Optional.of(p1));
        when(repository.findById("2")).thenReturn(Optional.of(p2));
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(
                List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight"));

        List<String> ids = Arrays.asList("1", "2");
        ProductComparisonResponse response = productService.compareProducts(ids, null);

        assertNotNull(response);
        assertEquals("CELLPHONES", response.getProductType());
        assertEquals(2, response.getProductCount());
    }

    @Test
    @DisplayName("Should throw exception when IDs contain 'null' string value")
    void testCompareProductsWithNullStringId() {
        List<String> ids = Arrays.asList("1", "null", "3");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("Invalid product ID"));
    }

    @Test
    @DisplayName("Should throw exception when IDs contain blank values")
    void testCompareProductsWithBlankId() {
        List<String> ids = Arrays.asList("1", "", "3");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("Invalid product ID"));
    }

    @Test
    @DisplayName("Should throw exception when IDs contain duplicates")
    void testCompareProductsWithDuplicateIds() {
        List<String> ids = Arrays.asList("1", "2", "1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.compareProducts(ids, null)
        );

        assertTrue(exception.getMessage().contains("Duplicate product IDs"));
    }

    @Test
    @DisplayName("Should throw exception for comparable filter — valid filters succeed")
    void testCompareProductsComparableFilterSucceeds() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(
                List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight"));

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        List<String> filters = Arrays.asList("price", "camera_mp");

        ProductComparisonResponse response = productService.compareProducts(ids, filters);
        assertNotNull(response);
        assertEquals(2, response.getProductCount());
    }

    @Test
    @DisplayName("Should use comparable fields from template as default filters")
    void testCompareProductsDefaultFiltersFromTemplate() {
        when(repository.findById("iphone-1")).thenReturn(Optional.of(iphone));
        when(repository.findById("samsung-1")).thenReturn(Optional.of(samsung));
        List<String> comparableFields = List.of("battery_capacity", "camera_mp", "memory_gb", "price", "screen_size", "size", "storage_gb", "weight");
        when(templateService.getComparableFields("CELLPHONES")).thenReturn(comparableFields);

        List<String> ids = Arrays.asList("iphone-1", "samsung-1");
        ProductComparisonResponse response = productService.compareProducts(ids, null);

        assertNotNull(response);
        assertEquals(comparableFields, response.getAppliedFilters());
        
        Map<String, Object> firstProduct = response.getProducts().get(0);
        assertTrue(firstProduct.containsKey("id"));
        assertTrue(firstProduct.containsKey("name"));
        assertTrue(firstProduct.containsKey("price"));
        // description is not a comparable field, should NOT be included by default
        assertFalse(firstProduct.containsKey("description"));
    }
}
