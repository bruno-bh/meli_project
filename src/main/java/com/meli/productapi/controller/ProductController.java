package com.meli.productapi.controller;

import com.meli.productapi.model.Product;
import com.meli.productapi.model.PageResponse;
import com.meli.productapi.model.ProductComparisonResponse;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Products", description = "Product CRUD, search with filters/pagination, and comparison endpoints")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    private static final Set<String> KNOWN_PARAMS = Set.of(
            "name", "type", "priceMin", "priceMax", "page", "pageSize");

    @Operation(summary = "List / search products", description = "Returns products matching the given filters with optional pagination. Supports filtering by name, type, price range, and specification keys.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    @GetMapping
    public ResponseEntity<PageResponse<Product>> getProducts(
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
        PageResponse<Product> pageResponse = productService.searchProducts(filter);
        log.debug("Search returned {} products", pageResponse.getContent().size());
        return ResponseEntity.ok(pageResponse);
    }

    @Operation(summary = "Get product by ID", description = "Retrieves a single product by its unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @Parameter(description = "Product ID", required = true) @PathVariable String id) {
        log.debug("Fetching product by ID: {}", id);
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "Create a new product", description = "Creates a product after validating required fields and type-specific constraints defined in the YAML template.",
            requestBody = @RequestBody(description = "Product to create", required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Cellphone example",
                            summary = "Samsung Galaxy S24 Ultra",
                            value = "{\n" +
                                    "  \"name\": \"Samsung Galaxy S24 Ultra\",\n" +
                                    "  \"description\": \"Smartphone Samsung Galaxy S24 Ultra 256GB 12GB RAM\",\n" +
                                    "  \"type\": \"CELLPHONES\",\n" +
                                    "  \"color\": \"Titanium Gray\",\n" +
                                    "  \"rating\": 4.8,\n" +
                                    "  \"price\": { \"value\": 7499.99, \"unit\": \"BRL\" },\n" +
                                    "  \"size\": { \"value\": 6.8, \"unit\": \"inches\" },\n" +
                                    "  \"weight\": { \"value\": 0.233, \"unit\": \"kg\" },\n" +
                                    "  \"specifications\": {\n" +
                                    "    \"brand\": \"Samsung\",\n" +
                                    "    \"storage_gb\": \"256\",\n" +
                                    "    \"memory_gb\": \"12\",\n" +
                                    "    \"screen_size\": \"6.8\",\n" +
                                    "    \"camera_mp\": \"200\",\n" +
                                    "    \"battery_capacity\": \"5000\",\n" +
                                    "    \"operating_system\": \"Android 14\",\n" +
                                    "    \"model_version\": \"S24 Ultra\"\n" +
                                    "  }\n" +
                                    "}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @org.springframework.web.bind.annotation.RequestBody Product product) {
        log.info("Creating product: {}", product.getName());
        Product createdProduct = productService.createProduct(product);
        log.info("Product created successfully with ID: {}", createdProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @Operation(summary = "Update product", description = "Updates an existing product by ID. All fields from the request body will overwrite the existing product.",
            requestBody = @RequestBody(description = "Updated product data", required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Cellphone update example",
                            summary = "Update Samsung Galaxy S24 Ultra",
                            value = "{\n" +
                                    "  \"name\": \"Samsung Galaxy S24 Ultra\",\n" +
                                    "  \"description\": \"Smartphone Samsung Galaxy S24 Ultra 512GB 12GB RAM\",\n" +
                                    "  \"type\": \"CELLPHONES\",\n" +
                                    "  \"color\": \"Titanium Black\",\n" +
                                    "  \"rating\": 4.9,\n" +
                                    "  \"price\": { \"value\": 8999.99, \"unit\": \"BRL\" },\n" +
                                    "  \"size\": { \"value\": 6.8, \"unit\": \"inches\" },\n" +
                                    "  \"weight\": { \"value\": 0.233, \"unit\": \"kg\" },\n" +
                                    "  \"specifications\": {\n" +
                                    "    \"brand\": \"Samsung\",\n" +
                                    "    \"storage_gb\": \"512\",\n" +
                                    "    \"memory_gb\": \"12\",\n" +
                                    "    \"screen_size\": \"6.8\",\n" +
                                    "    \"camera_mp\": \"200\",\n" +
                                    "    \"battery_capacity\": \"5000\",\n" +
                                    "    \"operating_system\": \"Android 14\",\n" +
                                    "    \"model_version\": \"S24 Ultra\"\n" +
                                    "  }\n" +
                                    "}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable String id,
            @Valid @org.springframework.web.bind.annotation.RequestBody Product product) {
        log.info("Updating product ID: {}", id);
        Product updatedProduct = productService.updateProduct(id, product);
        log.info("Product ID: {} updated successfully", id);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Delete product", description = "Deletes a product by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable String id) {
        log.info("Deleting product ID: {}", id);
        productService.deleteProduct(id);
        log.info("Product ID: {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get total product count", description = "Returns the total number of products in the system.")
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    @GetMapping("/stats/count")
    public ResponseEntity<Long> getTotalProducts() {
        long count = productService.getTotalProducts();
        log.debug("Total products count: {}", count);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Compare products", description = "Compares multiple products of the same type side by side. Returns comparable fields defined in the YAML template.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparison completed"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Incompatible product types")
    })
    @GetMapping("/compare")
    public ResponseEntity<ProductComparisonResponse> compareProducts(
            @Parameter(description = "Comma-separated product IDs to compare", required = true) @RequestParam String ids,
            @Parameter(description = "Comma-separated field names to include in comparison") @RequestParam(required = false) String filters) {
        
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

