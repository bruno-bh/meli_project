# ProductType Enum - Type Management System

## Overview

The `ProductType` enum defines standardized product categories with associated metadata requirements. This ensures consistency across the API and helps developers understand what metadata fields should be included for each product type.

## Supported Types

### 1. CELLPHONES (Smartphones)
- **Display Name**: Smartphones
- **Required Metadata**:
  - `brand`: Manufacturer name (e.g., "Apple", "Samsung")
  - `storage_gb`: Storage capacity in GB
  - `memory_gb`: RAM memory in GB
  - `screen_size`: Display size in inches
  - `camera_mp`: Camera resolution in megapixels

**Example**:
```json
{
  "type": "CELLPHONES",
  "metadata": {
    "brand": "Apple",
    "storage_gb": "256",
    "memory_gb": "8",
    "screen_size": "6.1",
    "camera_mp": "48"
  }
}
```

### 2. COMPUTERS (Computadores)
- **Display Name**: Computadores
- **Required Metadata**:
  - `brand`: Manufacturer name
  - `processor`: CPU model
  - `ram_gb`: RAM amount in GB
  - `storage_gb`: Storage capacity in GB
  - `screen_size`: Monitor/display size in inches

### 3. CLOTHING (Roupas)
- **Display Name**: Roupas
- **Required Metadata**:
  - `size_us`: US size standard
  - `size_eu`: European size standard
  - `material`: Main material composition
  - `composition`: Detailed material composition percentage
  - `color_variations`: Number of available colors

**Example**:
```json
{
  "type": "CLOTHING",
  "metadata": {
    "size_us": "M",
    "size_eu": "40",
    "material": "100% cotton",
    "composition": "Pure cotton",
    "color_variations": "5"
  }
}
```

### 4. FOOD (Alimentos)
- **Display Name**: Alimentos
- **Required Metadata**:
  - `manufacturing_date`: Production date (YYYY-MM-DD)
  - `expiration_date`: Best before date (YYYY-MM-DD)
  - `nutriscore`: Nutrition score (A-E)
  - `origin`: Country/region of origin
  - `weight`: Product weight

### 5. BEVERAGES (Bebidas)
- **Display Name**: Bebidas
- **Required Metadata**:
  - `volume_ml`: Volume in milliliters
  - `origin`: Country of origin
  - `expiration_date`: Expiration date
  - `ingredients`: Main ingredients
  - `alcohol_content`: Alcohol percentage if applicable

### 6. FURNITURE (Móveis)
- **Display Name**: Móveis
- **Required Metadata**:
  - `material`: Material type
  - `dimensions`: Length x Width x Height
  - `weight_kg`: Weight in kilograms
  - `color`: Available color
  - `warranty_months`: Warranty period in months

### 7. BOOKS (Livros)
- **Display Name**: Livros
- **Required Metadata**:
  - `author`: Author name
  - `publisher`: Publishing company
  - `pages`: Number of pages
  - `language`: Language of publication
  - `isbn`: ISBN identifier

### 8. SPORTS (Esportes)
- **Display Name**: Esportes
- **Required Metadata**:
  - `size`: Product size
  - `material`: Material composition
  - `color`: Color designation
  - `technology`: Special technology/features
  - `warranty_months`: Warranty period

## Usage Examples

### In Java Code

```java
// Get type and its metadata requirements
ProductType type = ProductType.CELLPHONES;
List<String> requiredMetadata = type.getMetadata();
String displayName = type.getDisplayName();

System.out.println(type.name()); // "CELLPHONES"
System.out.println(displayName); // "Smartphones"
System.out.println(requiredMetadata); // [brand, storage_gb, memory_gb, screen_size, camera_mp]

// Parse string to enum
Optional<ProductType> parsed = ProductType.fromString("FOOD");
if (parsed.isPresent()) {
    ProductType foodType = parsed.get();
    // Use food type
}

// Get all types info
String allInfo = ProductType.getAllTypesInfo();
System.out.println(allInfo);
```

### In API Requests

**Search by type**:
```bash
GET /api/v1/products/search?type=CELLPHONES
GET /api/v1/products/search?type=CLOTHING
```

**Create product with type**:
```json
POST /api/v1/products
{
  "name": "iPhone 15",
  "type": "CELLPHONES",
  "price": 999.99,
  "metadata": {
    "brand": "Apple",
    "storage_gb": "256",
    "memory_gb": "8",
    "screen_size": "6.1",
    "camera_mp": "48"
  }
}
```

### In Data Files

See `data/products-example.json` for complete examples of each product type with proper metadata structure.

## Enum API Reference

### Methods

#### `String getDisplayName()`
Returns the display name of the product type.
```java
ProductType.CELLPHONES.getDisplayName(); // "Smartphones"
```

#### `List<String> getMetadata()`
Returns an unmodifiable list of required metadata field names for this type.
```java
ProductType.FOOD.getMetadata(); // [manufacturing_date, expiration_date, nutriscore, origin, weight]
```

#### `Optional<ProductType> fromString(String type)`
Parses a string to its corresponding ProductType. Case-insensitive.
```java
ProductType.fromString("cellphones"); // Optional[CELLPHONES]
ProductType.fromString("INVALID"); // Optional.empty()
```

#### `String getAllTypesInfo()`
Returns a formatted string with all types and their metadata.
```java
ProductType.getAllTypesInfo();
// Output:
// CELLPHONES: Smartphones - Metadata: [brand, storage_gb, memory_gb, screen_size, camera_mp]
// COMPUTERS: Computadores - Metadata: [brand, processor, ram_gb, storage_gb, screen_size]
// ...
```

## Best Practices

1. **Always use ProductType enum** instead of string literals for type validation
2. **Validate metadata** against the required fields for each type
3. **Use the metadata list** to guide API users on required fields
4. **Keep enum updated** when adding new product types
5. **Use case-insensitive** string parsing with `fromString()` for user input

## Adding New Types

To add a new product type:

1. Add new enum constant:
   ```java
   NEW_TYPE("Display Name", List.of("field1", "field2", "field3"))
   ```

2. Add tests in `ProductTypeTest.java`

3. Update this documentation

4. Run `mvn test` to ensure everything passes

## Testing

Comprehensive tests for ProductType are available in `src/test/java/com/meli/productapi/model/ProductTypeTest.java`:
- Type value validation
- Display name correctness
- Metadata field verification
- String parsing (valid and invalid)
- Enum metadata immutability

Run tests: `mvn test`
