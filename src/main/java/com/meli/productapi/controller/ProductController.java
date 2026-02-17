package com.meli.productapi.controller;

import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductComparisonResponse;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(@Valid ProductFilter filter) {
        filter.validate(); // Valida regra customizada de priceMax >= priceMin
        List<Product> products = productService.searchProducts(filter);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @RequestBody Product product) {
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/count")
    public ResponseEntity<Long> getTotalProducts() {
        long count = productService.getTotalProducts();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/compare")
    public ResponseEntity<ProductComparisonResponse> compareProducts(
            @RequestParam String ids,
            @RequestParam(required = false) String filters) {
        
        // Parsear IDs
        List<String> productIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        // Parsear filtros (opcional)
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

