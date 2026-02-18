package com.meli.productapi.service;

import com.meli.productapi.exception.IncompatibleProductTypesException;
import com.meli.productapi.exception.ProductNotFoundException;
import com.meli.productapi.model.MeasurableValue;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductComparisonResponse;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.model.template.FieldDefinition;
import com.meli.productapi.model.template.ProductTemplate;
import com.meli.productapi.repository.ProductRepositoryInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductService {

    private final ProductRepositoryInterface repository;
    private final ProductTemplateService templateService;

    public ProductService(ProductRepositoryInterface repository, ProductTemplateService templateService) {
        this.repository = repository;
        this.templateService = templateService;
    }

    public Product getProductById(String id) {
        log.debug("Fetching product by ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product with ID {} not found", id);
                    return new ProductNotFoundException("Product with ID " + id + " not found");
                });
    }

    public Product createProduct(Product product) {
        validateProduct(product);
        // #9: Normalize type to UPPERCASE before saving
        product.setType(product.getType().toUpperCase().trim());
        applyDefaultUnits(product);
        log.info("Creating product: {}", product.getName());
        Product saved = repository.save(product);
        log.info("Product created with ID: {}", saved.getId());
        return saved;
    }

    public Product updateProduct(String id, Product productDetails) {
        Product product = getProductById(id);
        validateProduct(productDetails);
        // #9: Normalize type to UPPERCASE before saving
        productDetails.setType(productDetails.getType().toUpperCase().trim());
        applyDefaultUnits(productDetails);
        log.info("Updating product ID: {}", id);

        // #5: Only update field if value received is not null
        if (productDetails.getName() != null) {
            product.setName(productDetails.getName());
        }
        if (productDetails.getDescription() != null) {
            product.setDescription(productDetails.getDescription());
        }
        if (productDetails.getPrice() != null) {
            product.setPrice(productDetails.getPrice());
        }
        if (productDetails.getSize() != null) {
            product.setSize(productDetails.getSize());
        }
        if (productDetails.getWeight() != null) {
            product.setWeight(productDetails.getWeight());
        }
        if (productDetails.getColor() != null) {
            product.setColor(productDetails.getColor());
        }
        if (productDetails.getType() != null) {
            product.setType(productDetails.getType());
        }
        if (productDetails.getImageUrl() != null) {
            product.setImageUrl(productDetails.getImageUrl());
        }
        if (productDetails.getRating() != null) {
            product.setRating(productDetails.getRating());
        }
        if (productDetails.getSpecifications() != null) {
            product.setSpecifications(productDetails.getSpecifications());
        }

        Product saved = repository.save(product);
        log.info("Product ID: {} updated successfully", id);
        return saved;
    }

    public void deleteProduct(String id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Attempt to delete non-existent product ID: {}", id);
                    return new ProductNotFoundException("Product with ID " + id + " not found");
                });

        log.info("Deleting product ID: {} ({})", id, product.getName());
        repository.deleteById(id);
    }

    public long getTotalProducts() {
        return repository.count();
    }

    /**
     * Searches products with multiple filters and pagination
     * 
     * @param filter Object with all search filters
     * @return Filtered and paginated list of products
     */
    public List<Product> searchProducts(ProductFilter filter) {
        // Validate price range relationship
        filter.validate();

        // Validate type against templates if provided
        if (filter.getType() != null && !filter.getType().isEmpty()) {
            if (!templateService.isValidType(filter.getType())) {
                throw new IllegalArgumentException(
                        "Invalid product type: '" + filter.getType() + "'. Valid types: " +
                        templateService.getValidTypeNames());
            }
        }

        // Validate specification keys in filter
        if (filter.getSpecifications() != null && !filter.getSpecifications().isEmpty()) {
            if (filter.getType() != null && !filter.getType().isEmpty()) {
                // Type provided: validate spec keys against that type's template
                List<String> validSpecKeys = templateService.getMetadataFields(filter.getType());
                for (String specKey : filter.getSpecifications().keySet()) {
                    if (!validSpecKeys.contains(specKey)) {
                        throw new IllegalArgumentException(
                                "Invalid specification key '" + specKey + "' for product type '" +
                                filter.getType().toUpperCase() + "'. Valid specification keys: " + validSpecKeys);
                    }
                }
            } else {
                // No type provided: validate against all known spec keys
                Set<String> allSpecKeys = templateService.getAllSpecificationKeys();
                for (String specKey : filter.getSpecifications().keySet()) {
                    if (!allSpecKeys.contains(specKey)) {
                        throw new IllegalArgumentException(
                                "Invalid specification key '" + specKey + "'. " +
                                "This key does not exist in any product type template.");
                    }
                }
            }
        }
        
        log.debug("Searching products with filters: name={}, type={}, priceMin={}, priceMax={}, specifications={}",
                filter.getName(), filter.getType(), filter.getPriceMin(), filter.getPriceMax(),
                filter.getSpecifications());
        
        List<Product> allProducts = repository.findAll();
        
        // Apply filters
        List<Product> filteredProducts = allProducts.stream()
                .filter(product -> {
                    // Filter by name
                    if (filter.getName() != null && !filter.getName().isEmpty()) {
                        if (product.getName() == null ||
                            !product.getName().toLowerCase().contains(filter.getName().toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filter by type
                    if (filter.getType() != null && !filter.getType().isEmpty()) {
                        if (product.getType() == null ||
                            !product.getType().equalsIgnoreCase(filter.getType())) {
                            return false;
                        }
                    }
                    
                    // Filter by minimum price
                    if (filter.getPriceMin() != null) {
                        if (product.getPrice() == null || product.getPrice().getValue() == null
                                || product.getPrice().getValue() < filter.getPriceMin()) {
                            return false;
                        }
                    }
                    
                    // Filter by maximum price
                    if (filter.getPriceMax() != null) {
                        if (product.getPrice() == null || product.getPrice().getValue() == null
                                || product.getPrice().getValue() > filter.getPriceMax()) {
                            return false;
                        }
                    }

                    // Filter by specifications (partial, case-insensitive for text values)
                    if (filter.getSpecifications() != null && !filter.getSpecifications().isEmpty()) {
                        if (product.getSpecifications() == null || product.getSpecifications().isEmpty()) {
                            return false;
                        }
                        for (Map.Entry<String, String> specFilter : filter.getSpecifications().entrySet()) {
                            Object productSpecValue = product.getSpecifications().get(specFilter.getKey());
                            if (productSpecValue == null) {
                                return false;
                            }
                            String productValStr = productSpecValue.toString().toLowerCase();
                            String filterValStr = specFilter.getValue().toLowerCase();
                            if (!productValStr.contains(filterValStr)) {
                                return false;
                            }
                        }
                    }
                    
                    return true;
                })
                .toList();
        
        log.debug("Filtered {} products from {} total", filteredProducts.size(), allProducts.size());
        
        // Apply pagination
        List<Product> result = applyPagination(filteredProducts, filter.getPage(), filter.getPageSize());
        log.debug("Returning {} products after pagination (page={}, pageSize={})",
                result.size(), filter.getPage(), filter.getPageSize());
        return result;
    }

    /**
     * Applies pagination to the product list
     * 
     * @param products List of products
     * @param page     Page number (default: 1)
     * @param pageSize Page size (null = all)
     * @return Paginated list
     */
    private List<Product> applyPagination(List<Product> products, Integer page, Integer pageSize) {
        // If pageSize is null, return all
        if (pageSize == null) {
            return products;
        }
        
        // Validate pageSize
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        
        // Default page to 1 if null, zero or negative
        int currentPage = (page != null && page > 0) ? page : 1;
        
        // Calculate indices
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, products.size());
        
        // Check if page is within bounds
        if (startIndex >= products.size()) {
            return new ArrayList<>();
        }
        
        return products.subList(startIndex, endIndex);
    }

    /**
     * Compares multiple products
     * 
     * @param productIds List of product IDs
     * @param filters    List of fields to compare (empty = all comparable fields from template)
     * @return ProductComparisonResponse with comparison data
     * @throws ProductNotFoundException          If any ID does not exist
     * @throws IncompatibleProductTypesException If products have different types
     */
    public ProductComparisonResponse compareProducts(List<String> productIds, List<String> filters) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("At least two product IDs must be provided for comparison");
        }

        // Validate individual IDs: no null, blank, or "null" string values
        for (String id : productIds) {
            if (id == null || id.isBlank() || id.equalsIgnoreCase("null")) {
                throw new IllegalArgumentException(
                        "Invalid product ID detected: IDs cannot be null, empty, or blank");
            }
        }

        // Validate no duplicate IDs
        Set<String> uniqueIds = new LinkedHashSet<>(productIds);
        if (uniqueIds.size() != productIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate product IDs are not allowed in comparison");
        }

        // Validate at least 2 IDs after cleaning
        if (productIds.size() < 2) {
            throw new IllegalArgumentException("At least two product IDs must be provided for comparison");
        }

        log.info("Comparing {} products: {}", productIds.size(), productIds);

        // Get products
        List<Product> products = productIds.stream()
                .map(this::getProductById)
                .collect(Collectors.toList());

        // #10: Validate all products have the same type (null-safe)
        String firstType = products.get(0).getType();
        if (!products.stream().allMatch(p -> Objects.equals(p.getType(), firstType))) {
            throw new IncompatibleProductTypesException(
                    "Cannot compare products of different types. " +
                            "All products must be of the same type.");
        }

        // Validate filters against comparable fields from the template
        if (filters != null && !filters.isEmpty()) {
            List<String> comparableFields = templateService.getComparableFields(firstType);
            // Fixed fields that are always valid (always included in response)
            Set<String> alwaysAllowed = Set.of("id", "name", "description", "imageUrl", "imageurl", "specifications");

            for (String filter : filters) {
                String filterLower = filter.toLowerCase();
                if (!alwaysAllowed.contains(filterLower) && !comparableFields.contains(filter)) {
                    throw new IllegalArgumentException(
                            "Filter '" + filter + "' is not a comparable field for product type '" +
                            firstType + "'. Comparable fields: " + comparableFields);
                }
            }
        }

        // Prepare filters: use comparable fields from template as default
        List<String> appliedFilters;
        if (filters == null || filters.isEmpty()) {
            appliedFilters = templateService.getComparableFields(firstType);
        } else {
            appliedFilters = filters;
        }

        // Build comparison data
        List<Map<String, Object>> comparisonData = products.stream()
                .map(product -> extractProductFields(product, appliedFilters))
                .collect(Collectors.toList());

        return ProductComparisonResponse.builder()
                .productType(firstType)
                .appliedFilters(appliedFilters)
                .products(comparisonData)
                .build();
    }

    /**
     * Extracts specified fields from a product.
     * Always includes mandatory fields: id, name.
     * Other fields are included only when explicitly requested via filters.
     */
    private Map<String, Object> extractProductFields(Product product, List<String> filters) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Mandatory fields - always included
        result.put("id", product.getId());
        result.put("name", product.getName());

        // Add fields based on filters
        for (String filter : filters) {
            String filterLower = filter.toLowerCase();

            // Skip mandatory fields already added
            if (filterLower.equals("id") || filterLower.equals("name")) {
                continue;
            }

            switch (filterLower) {
                case "description":
                    if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                        result.put("description", product.getDescription());
                    }
                    break;
                case "imageurl":
                    if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
                        result.put("imageUrl", product.getImageUrl());
                    }
                    break;
                case "price":
                    result.put("price", product.getPrice());
                    break;
                case "size":
                    result.put("size", product.getSize());
                    break;
                case "weight":
                    result.put("weight", product.getWeight());
                    break;
                case "color":
                    result.put("color", product.getColor());
                    break;
                case "rating":
                    result.put("rating", product.getRating());
                    break;
                case "specifications":
                    result.put("specifications", product.getSpecifications());
                    break;
                default:
                    // Try to get from specifications if not a standard field
                    if (product.getSpecifications() != null &&
                            product.getSpecifications().containsKey(filter)) {
                        result.put(filter, product.getSpecifications().get(filter));
                    }
            }
        }

        return result;
    }

    /**
     * Validates a product against the template rules.
     * #3: Validates required fields and specs from YAML template.
     * #4: Always fetches template via templateService.getTemplate().
     * #6: Price value must be > 0 (not just >= 0).
     * #7: Specification keys must be a subset of template keys.
     * #15: Validates spec value types (number, date).
     */
    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            log.warn("Product validation failed: name is required");
            throw new IllegalArgumentException("Product name is required");
        }
        // #6: Price must be positive (> 0, not just >= 0)
        if (product.getPrice() == null || product.getPrice().getValue() == null || product.getPrice().getValue() <= 0) {
            log.warn("Product validation failed: price must be positive");
            throw new IllegalArgumentException("Product price must be a positive value greater than zero");
        }
        if (product.getType() == null || product.getType().trim().isEmpty()) {
            log.warn("Product validation failed: type is required");
            throw new IllegalArgumentException("Product type is required");
        }
        // Validate type against loaded templates
        if (!templateService.isValidType(product.getType())) {
            throw new IllegalArgumentException(
                    "Invalid product type: '" + product.getType() + "'. Valid types: " +
                    templateService.getValidTypeNames());
        }
        // Validate rating bounds
        if (product.getRating() != null && (product.getRating() < 0.0 || product.getRating() > 5.0)) {
            throw new IllegalArgumentException("Rating must be between 0.0 and 5.0");
        }

        // #3, #4: Always fetch template for required field/spec validation
        Optional<ProductTemplate> templateOpt = templateService.getTemplate(product.getType());
        if (templateOpt.isPresent()) {
            ProductTemplate template = templateOpt.get();

            // Validate required fields (price, size, weight)
            if (template.getFields() != null) {
                for (Map.Entry<String, FieldDefinition> entry : template.getFields().entrySet()) {
                    if (entry.getValue().isRequired()) {
                        validateRequiredField(product, entry.getKey());
                    }
                }
            }

            // Validate required specifications
            if (template.getSpecifications() != null) {
                for (Map.Entry<String, FieldDefinition> entry : template.getSpecifications().entrySet()) {
                    if (entry.getValue().isRequired()) {
                        validateRequiredSpec(product, entry.getKey());
                    }
                }
            }

            // #7: Validate specification keys are a subset of template keys
            if (product.getSpecifications() != null && !product.getSpecifications().isEmpty()
                    && template.getSpecifications() != null) {
                Set<String> validKeys = template.getSpecifications().keySet();
                for (String key : product.getSpecifications().keySet()) {
                    if (!validKeys.contains(key)) {
                        throw new IllegalArgumentException(
                                "Unknown specification key '" + key + "' for product type '" +
                                product.getType().toUpperCase() + "'. Valid keys: " + validKeys);
                    }
                }
            }

            // #15: Validate specification value types
            if (product.getSpecifications() != null && template.getSpecifications() != null) {
                for (Map.Entry<String, Object> specEntry : product.getSpecifications().entrySet()) {
                    FieldDefinition fieldDef = template.getSpecifications().get(specEntry.getKey());
                    if (fieldDef != null && specEntry.getValue() != null) {
                        validateSpecValueType(specEntry.getKey(), specEntry.getValue(), fieldDef);
                    }
                }
            }
        }
    }

    /**
     * Validates that a required field (price, size, weight) is present on the product.
     */
    private void validateRequiredField(Product product, String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "price":
                // Already validated above
                break;
            case "size":
                if (product.getSize() == null) {
                    throw new IllegalArgumentException(
                            "Field 'size' is required for product type '" + product.getType().toUpperCase() + "'");
                }
                break;
            case "weight":
                if (product.getWeight() == null) {
                    throw new IllegalArgumentException(
                            "Field 'weight' is required for product type '" + product.getType().toUpperCase() + "'");
                }
                break;
            default:
                log.debug("Unknown required field: {}", fieldName);
        }
    }

    /**
     * Validates that a required specification is present on the product.
     */
    private void validateRequiredSpec(Product product, String specKey) {
        if (product.getSpecifications() == null || !product.getSpecifications().containsKey(specKey)
                || product.getSpecifications().get(specKey) == null
                || product.getSpecifications().get(specKey).toString().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Specification '" + specKey + "' is required for product type '" +
                    product.getType().toUpperCase() + "'");
        }
    }

    /**
     * #15: Validates that a specification value matches its expected type from the template.
     */
    private void validateSpecValueType(String key, Object value, FieldDefinition fieldDef) {
        String type = fieldDef.getType();
        if (type == null) return;

        String strValue = value.toString().trim();

        switch (type.toLowerCase()) {
            case "number":
                try {
                    Double.parseDouble(strValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Specification '" + key + "' must be a valid number, but got: '" + strValue + "'");
                }
                break;
            case "date":
                if (!strValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    throw new IllegalArgumentException(
                            "Specification '" + key + "' must be a valid date in format 'yyyy-MM-dd', but got: '" + strValue + "'");
                }
                break;
            default:
                // text or unknown types: no validation needed
                break;
        }
    }

    /**
     * Applies default units from the template to MeasurableValue fields that are missing a unit.
     */
    private void applyDefaultUnits(Product product) {
        Optional<ProductTemplate> templateOpt = templateService.getTemplate(product.getType());
        if (templateOpt.isEmpty()) {
            return;
        }
        ProductTemplate template = templateOpt.get();
        Map<String, FieldDefinition> fields = template.getFields();

        // Apply default unit for price
        if (product.getPrice() != null && product.getPrice().getUnit() == null && fields.containsKey("price")) {
            product.getPrice().setUnit(fields.get("price").getDefaultUnit());
        }

        // Apply default unit for size
        if (product.getSize() != null && product.getSize().getUnit() == null && fields.containsKey("size")) {
            product.getSize().setUnit(fields.get("size").getDefaultUnit());
        }

        // Apply default unit for weight
        if (product.getWeight() != null && product.getWeight().getUnit() == null && fields.containsKey("weight")) {
            product.getWeight().setUnit(fields.get("weight").getDefaultUnit());
        }
    }
}
