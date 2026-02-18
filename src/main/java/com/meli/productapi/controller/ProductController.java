package com.meli.productapi.controller;

import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductComparisonResponse;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    private static final Set<String> KNOWN_PARAMS = Set.of(
            "name", "type", "priceMin", "priceMax", "page", "pageSize");

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @Valid ProductFilter filter,
            @RequestParam Map<String, String> allParams) {
        
        // Extract specification filters from extra query params
        Map<String, String> specFilters = new HashMap<>();
        allParams.forEach((key, value) -> {
            if (!KNOWN_PARAMS.contains(key) && value != null && !value.isEmpty()) {
                specFilters.put(key, value);
            }
        });
        if (!specFilters.isEmpty()) {
            filter.setSpecifications(specFilters);
        }

        log.debug("Searching products with filters: name={}, type={}, priceMin={}, priceMax={}, page={}, pageSize={}, specs={}",
                filter.getName(), filter.getType(), filter.getPriceMin(), filter.getPriceMax(),
                filter.getPage(), filter.getPageSize(), filter.getSpecifications());
        filter.validate();
        List<Product> products = productService.searchProducts(filter);
        log.debug("Search returned {} products", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        log.debug("Fetching product by ID: {}", id);
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        log.info("Creating product: {}", product.getName());
        Product createdProduct = productService.createProduct(product);
        log.info("Product created successfully with ID: {}", createdProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody Product product) {
        log.info("Updating product ID: {}", id);
        Product updatedProduct = productService.updateProduct(id, product);
        log.info("Product ID: {} updated successfully", id);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        log.info("Deleting product ID: {}", id);
        productService.deleteProduct(id);
        log.info("Product ID: {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/count")
    public ResponseEntity<Long> getTotalProducts() {
        long count = productService.getTotalProducts();
        log.debug("Total products count: {}", count);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/compare")
    public ResponseEntity<ProductComparisonResponse> compareProducts(
            @RequestParam String ids,
            @RequestParam(required = false) String filters) {
        
        log.info("Comparing products with IDs: {}", ids);

        // Parse IDs — keep all entries including empty/null strings for validation
        List<String> productIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        // Parse filters (optional)
        log.debug("Comparison filters: {}", filters);
        List<String> appliedFilters = null;
        if (filters != null && !filters.isEmpty()) {
            appliedFilters = Arrays.stream(filters.split(","))
                    .map(String::trim)
                    .filter(filter -> !filter.isEmpty())
                    .collect(Collectors.toList());
        }

        ProductComparisonResponse comparison = productService.compareProducts(productIds, appliedFilters);
        return ResponseEntity.ok(comparison);
    }
}

