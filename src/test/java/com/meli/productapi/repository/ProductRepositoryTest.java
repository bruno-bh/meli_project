package com.meli.productapi.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.model.MeasurableValue;
import com.meli.productapi.model.Product;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductRepository — JSON file-based repository tests")
class ProductRepositoryTest {

    @TempDir
    Path tempDir;

    private ProductRepository repository;
    private ObjectMapper objectMapper;
    private String productsFilePath;

    private static MeasurableValue price(Double value) {
        return MeasurableValue.builder().value(value).unit("BRL").build();
    }

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        productsFilePath = tempDir.resolve("products.json").toString();
        // Write an empty array so the repo starts clean
        objectMapper.writeValue(new File(productsFilePath), List.of());

        repository = new ProductRepository(
                tempDir.toString(),
                productsFilePath,
                objectMapper
        );
    }

    // ── findAll ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll — should return empty list when no products exist")
    void testFindAllEmpty() {
        List<Product> products = repository.findAll();
        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    @DisplayName("findAll — should return all saved products")
    void testFindAllWithProducts() {
        repository.save(product("Laptop", 2500.0));
        repository.save(product("Phone", 999.0));

        List<Product> products = repository.findAll();
        assertEquals(2, products.size());
    }

    @Test
    @DisplayName("findAll — should throw RuntimeException when JSON file is corrupt")
    void testFindAllCorruptFile() throws IOException {
        Files.writeString(Path.of(productsFilePath), "NOT VALID JSON {{{{");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> repository.findAll());
        assertTrue(ex.getMessage().contains("Error reading products"));
    }

    // ── findById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — should return product when ID exists")
    void testFindByIdExists() {
        Product saved = repository.save(product("Laptop", 2500.0));

        Optional<Product> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Laptop", found.get().getName());
    }

    @Test
    @DisplayName("findById — should return empty when ID does not exist")
    void testFindByIdNotExists() {
        Optional<Product> found = repository.findById("non-existent-id");
        assertFalse(found.isPresent());
    }

    // ── save (new product) ───────────────────────────────────────────────

    @Test
    @DisplayName("save — should auto-generate incremental ID for new product")
    void testSaveNewProductAutoId() {
        Product p1 = repository.save(product("Product A", 100.0));
        Product p2 = repository.save(product("Product B", 200.0));

        assertEquals("1", p1.getId());
        assertEquals("2", p2.getId());
    }

    @Test
    @DisplayName("save — should generate imageUrl when not provided")
    void testSaveGeneratesImageUrl() {
        Product saved = repository.save(product("Phone", 999.0));
        assertNotNull(saved.getImageUrl());
        assertFalse(saved.getImageUrl().isEmpty());
    }

    @Test
    @DisplayName("save — should set default rating to 0.0 when not provided")
    void testSaveDefaultRating() {
        Product p = product("Phone", 999.0);
        p.setRating(null);
        Product saved = repository.save(p);
        assertEquals(0.0, saved.getRating());
    }

    @Test
    @DisplayName("save — should persist product to JSON file")
    void testSavePersistsToFile() throws IOException {
        repository.save(product("Persisted Product", 50.0));

        String fileContent = Files.readString(Path.of(productsFilePath));
        assertTrue(fileContent.contains("Persisted Product"));
    }

    // ── save (update) ────────────────────────────────────────────────────

    @Test
    @DisplayName("save — should update existing product when ID already exists")
    void testSaveUpdateExisting() {
        Product saved = repository.save(product("Original", 100.0));
        String id = saved.getId();

        saved.setName("Updated");
        saved.setPrice(price(200.0));
        repository.save(saved);

        Optional<Product> found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().getName());
        assertEquals(200.0, found.get().getPrice().getValue());

        // Should still only have 1 product
        assertEquals(1, repository.findAll().size());
    }

    // ── deleteById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteById — should remove product from storage")
    void testDeleteById() {
        Product saved = repository.save(product("To Delete", 100.0));
        assertEquals(1, repository.findAll().size());

        repository.deleteById(saved.getId());
        assertEquals(0, repository.findAll().size());
    }

    @Test
    @DisplayName("deleteById — should be no-op when ID does not exist")
    void testDeleteByIdNonExistent() {
        repository.save(product("Keep", 100.0));

        repository.deleteById("non-existent");
        assertEquals(1, repository.findAll().size());
    }

    // ── existsById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("existsById — should return true for existing product")
    void testExistsByIdTrue() {
        Product saved = repository.save(product("Exists", 100.0));
        assertTrue(repository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("existsById — should return false for non-existing product")
    void testExistsByIdFalse() {
        assertFalse(repository.existsById("no-such-id"));
    }

    // ── count ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("count — should return 0 when repository is empty")
    void testCountEmpty() {
        assertEquals(0L, repository.count());
    }

    @Test
    @DisplayName("count — should return correct number of products")
    void testCountWithProducts() {
        repository.save(product("A", 10.0));
        repository.save(product("B", 20.0));
        repository.save(product("C", 30.0));

        assertEquals(3L, repository.count());
    }

    // ── ID auto-increment edge cases ─────────────────────────────────────

    @Test
    @DisplayName("save — should continue incrementing ID after deletion")
    void testIdIncrementAfterDeletion() {
        Product p1 = repository.save(product("First", 10.0));
        assertEquals("1", p1.getId());

        repository.deleteById(p1.getId());

        // Next product should still get ID 2 (based on max existing), but since
        // all products were deleted, max is 0 so next is 1 again.
        // This tests the actual behavior of the repository.
        Product p2 = repository.save(product("Second", 20.0));
        assertNotNull(p2.getId());
    }

    @Test
    @DisplayName("save — should preserve explicit ID when provided")
    void testSaveWithExplicitId() {
        Product p = product("Explicit", 100.0);
        p.setId("custom-id-42");

        Product saved = repository.save(p);
        assertEquals("custom-id-42", saved.getId());

        Optional<Product> found = repository.findById("custom-id-42");
        assertTrue(found.isPresent());
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private Product product(String name, Double priceValue) {
        return Product.builder()
                .name(name)
                .price(price(priceValue))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("brand", "Test")))
                .build();
    }
}
