package com.meli.productapi.service;

import com.meli.productapi.model.template.FieldDefinition;
import com.meli.productapi.model.template.ProductTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProductTemplateService
 */
@DisplayName("ProductTemplateService — YAML template tests")
class ProductTemplateServiceTest {

    private ProductTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new ProductTemplateService("data/test/product-templates-test.yaml");
        templateService.init();
    }

    @Test
    @DisplayName("Should load all 8 templates from YAML")
    void testLoadTemplatesFromYaml() {
        Map<String, ProductTemplate> templates = templateService.getAllTemplates();
        assertNotNull(templates);
        assertEquals(8, templates.size(), "Should have 8 product templates");
    }

    @Test
    @DisplayName("Should return valid template by name")
    void testGetTemplateValid() {
        Optional<ProductTemplate> cellphones = templateService.getTemplate("CELLPHONES");
        assertTrue(cellphones.isPresent());
        assertEquals("CELLPHONES", cellphones.get().getName());
        assertEquals("Smartphones", cellphones.get().getDisplayName());
    }

    @Test
    @DisplayName("Should return empty for invalid template")
    void testGetTemplateInvalid() {
        Optional<ProductTemplate> invalid = templateService.getTemplate("INVALID_TYPE");
        assertFalse(invalid.isPresent());

        Optional<ProductTemplate> empty = templateService.getTemplate("");
        assertFalse(empty.isPresent());

        Optional<ProductTemplate> nullType = templateService.getTemplate(null);
        assertFalse(nullType.isPresent());
    }

    @Test
    @DisplayName("Should return all valid type names")
    void testGetValidTypeNames() {
        List<String> names = templateService.getValidTypeNames();
        assertNotNull(names);
        assertEquals(8, names.size());
        assertTrue(names.contains("CELLPHONES"));
        assertTrue(names.contains("COMPUTERS"));
        assertTrue(names.contains("CLOTHING"));
        assertTrue(names.contains("FOOD"));
        assertTrue(names.contains("BEVERAGES"));
        assertTrue(names.contains("FURNITURE"));
        assertTrue(names.contains("BOOKS"));
        assertTrue(names.contains("SPORTS"));
    }

    @Test
    @DisplayName("Should return metadata fields for a type")
    void testGetMetadataFields() {
        List<String> cellphoneFields = templateService.getMetadataFields("CELLPHONES");
        assertNotNull(cellphoneFields);
        assertTrue(cellphoneFields.contains("brand"));
        assertTrue(cellphoneFields.contains("storage_gb"));
        assertTrue(cellphoneFields.contains("memory_gb"));
        assertTrue(cellphoneFields.contains("camera_mp"));
        assertTrue(cellphoneFields.contains("battery_capacity"));
        assertTrue(cellphoneFields.contains("operating_system"));
        assertTrue(cellphoneFields.contains("model_version"));

        List<String> invalidFields = templateService.getMetadataFields("INVALID");
        assertTrue(invalidFields.isEmpty());
    }

    @Test
    @DisplayName("Should reload templates from disk")
    void testReloadTemplates() {
        assertDoesNotThrow(() -> templateService.reloadTemplates());
        assertEquals(8, templateService.getAllTemplates().size());
    }

    @Test
    @DisplayName("Should validate valid and invalid types")
    void testIsValidType() {
        assertTrue(templateService.isValidType("CELLPHONES"));
        assertTrue(templateService.isValidType("COMPUTERS"));
        assertTrue(templateService.isValidType("CLOTHING"));
        assertFalse(templateService.isValidType("INVALID"));
        assertFalse(templateService.isValidType(""));
        assertFalse(templateService.isValidType(null));
    }

    @Test
    @DisplayName("Should be case-insensitive when looking up types")
    void testCaseInsensitiveLookup() {
        assertTrue(templateService.isValidType("cellphones"));
        assertTrue(templateService.isValidType("CELLPHONES"));
        assertTrue(templateService.isValidType("Cellphones"));
        assertTrue(templateService.isValidType("CeLlPhOnEs"));

        Optional<ProductTemplate> lower = templateService.getTemplate("cellphones");
        Optional<ProductTemplate> upper = templateService.getTemplate("CELLPHONES");
        Optional<ProductTemplate> mixed = templateService.getTemplate("Cellphones");

        assertTrue(lower.isPresent());
        assertTrue(upper.isPresent());
        assertTrue(mixed.isPresent());
        assertEquals(lower.get().getName(), upper.get().getName());
        assertEquals(upper.get().getName(), mixed.get().getName());
    }

    @Test
    @DisplayName("Should validate template FieldDefinitions")
    void testTemplateFieldDefinitions() {
        Optional<ProductTemplate> cellphones = templateService.getTemplate("CELLPHONES");
        assertTrue(cellphones.isPresent());

        ProductTemplate template = cellphones.get();

        // Validate fields
        Map<String, FieldDefinition> fields = template.getFields();
        assertNotNull(fields);
        assertTrue(fields.containsKey("price"));

        FieldDefinition priceDef = fields.get("price");
        assertEquals("number", priceDef.getType());
        assertEquals("BRL", priceDef.getDefaultUnit());
        assertTrue(priceDef.isRequired());
        assertTrue(priceDef.isComparable());

        // Validate specifications
        Map<String, FieldDefinition> specs = template.getSpecifications();
        assertNotNull(specs);
        assertTrue(specs.containsKey("brand"));

        FieldDefinition brandDef = specs.get("brand");
        assertEquals("text", brandDef.getType());
        assertTrue(brandDef.isRequired());
        assertFalse(brandDef.isComparable());
    }

    @Test
    @DisplayName("Should return info for all types")
    void testGetAllTypesInfo() {
        String info = templateService.getAllTypesInfo();
        assertNotNull(info);
        assertTrue(info.contains("CELLPHONES"));
        assertTrue(info.contains("COMPUTERS"));
        assertTrue(info.contains("Smartphones"));
        assertTrue(info.contains("Metadata:"));
    }

    @Test
    @DisplayName("Should return only comparable fields for a valid type")
    void testGetComparableFieldsValidType() {
        List<String> comparableFields = templateService.getComparableFields("CELLPHONES");
        assertNotNull(comparableFields);
        assertFalse(comparableFields.isEmpty());

        // Fields: price, size, weight are comparable for CELLPHONES
        assertTrue(comparableFields.contains("price"));
        assertTrue(comparableFields.contains("size"));
        assertTrue(comparableFields.contains("weight"));

        // Comparable specifications for CELLPHONES
        assertTrue(comparableFields.contains("storage_gb"));
        assertTrue(comparableFields.contains("memory_gb"));
        assertTrue(comparableFields.contains("screen_size"));
        assertTrue(comparableFields.contains("camera_mp"));
        assertTrue(comparableFields.contains("battery_capacity"));

        // Non-comparable specifications should NOT be included
        assertFalse(comparableFields.contains("brand"));
        assertFalse(comparableFields.contains("operating_system"));
        assertFalse(comparableFields.contains("model_version"));
    }

    @Test
    @DisplayName("Should return empty list for invalid type in getComparableFields")
    void testGetComparableFieldsInvalidType() {
        List<String> comparableFields = templateService.getComparableFields("INVALID_TYPE");
        assertNotNull(comparableFields);
        assertTrue(comparableFields.isEmpty());

        List<String> nullResult = templateService.getComparableFields(null);
        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());

        List<String> emptyResult = templateService.getComparableFields("");
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    @DisplayName("Should return all specification keys from all templates")
    void testGetAllSpecificationKeys() {
        Set<String> allKeys = templateService.getAllSpecificationKeys();
        assertNotNull(allKeys);
        assertFalse(allKeys.isEmpty());

        // CELLPHONES specs
        assertTrue(allKeys.contains("brand"));
        assertTrue(allKeys.contains("storage_gb"));
        assertTrue(allKeys.contains("memory_gb"));
        assertTrue(allKeys.contains("camera_mp"));

        // COMPUTERS specs
        assertTrue(allKeys.contains("processor"));
        assertTrue(allKeys.contains("ram_gb"));

        // CLOTHING specs
        assertTrue(allKeys.contains("material"));
        assertTrue(allKeys.contains("size_us"));

        // FOOD specs
        assertTrue(allKeys.contains("manufacturing_date"));
        assertTrue(allKeys.contains("expiration_date"));
        assertTrue(allKeys.contains("nutriscore"));

        // BEVERAGES specs
        assertTrue(allKeys.contains("volume_ml"));
        assertTrue(allKeys.contains("alcohol_content"));

        // BOOKS specs
        assertTrue(allKeys.contains("author"));
        assertTrue(allKeys.contains("pages"));
        assertTrue(allKeys.contains("isbn"));
    }
}
