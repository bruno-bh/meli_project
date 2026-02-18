package com.meli.productapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.meli.productapi.model.template.ProductTemplate;
import com.meli.productapi.model.template.ProductTemplateConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service responsible for loading, caching, and providing access to product templates.
 * Templates are loaded from a YAML file and define the expected fields and specifications
 * for each product type.
 */
@Slf4j
@Service
public class ProductTemplateService {

    private final String templateFile;
    private final ObjectMapper yamlMapper;
    private volatile Map<String, ProductTemplate> templates = new ConcurrentHashMap<>();

    public ProductTemplateService(@Value("${product.template.file}") String templateFile) {
        this.templateFile = templateFile;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Loads templates from the YAML file on application startup.
     */
    @PostConstruct
    public void init() {
        loadTemplates();
    }

    /**
     * Loads templates from the configured YAML file.
     */
    private void loadTemplates() {
        try {
            File file = new File(templateFile);
            if (!file.exists()) {
                log.error("Template file not found: {}", templateFile);
                throw new RuntimeException("Template file not found: " + templateFile);
            }

            ProductTemplateConfig config = yamlMapper.readValue(file, ProductTemplateConfig.class);

            if (config == null || config.getTemplates() == null || config.getTemplates().isEmpty()) {
                log.error("No templates found in file: {}", templateFile);
                throw new RuntimeException("No templates found in file: " + templateFile);
            }

            Map<String, ProductTemplate> newTemplates = new ConcurrentHashMap<>();
            config.getTemplates().forEach((key, template) -> {
                // Set the name from the YAML key (uppercased for consistency)
                template.setName(key.toUpperCase());
                newTemplates.put(key.toUpperCase(), template);
            });

            this.templates = newTemplates;
            log.info("Loaded {} product templates from {}", templates.size(), templateFile);

        } catch (IOException e) {
            log.error("Error loading templates from {}: {}", templateFile, e.getMessage());
            throw new RuntimeException("Error loading product templates", e);
        }
    }

    /**
     * Returns the template for a given type name (case-insensitive).
     *
     * @param typeName the product type name (e.g., "CELLPHONES", "cellphones")
     * @return Optional containing the template if found
     */
    public Optional<ProductTemplate> getTemplate(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return Optional.empty();
        }
        log.debug("Looking up template for type: {}", typeName);
        return Optional.ofNullable(templates.get(typeName.toUpperCase()));
    }

    /**
     * Returns all loaded templates.
     *
     * @return unmodifiable map of all templates
     */
    public Map<String, ProductTemplate> getAllTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    /**
     * Returns a sorted list of all valid type names.
     *
     * @return list of valid type names (e.g., ["BEVERAGES", "BOOKS", "CELLPHONES", ...])
     */
    public List<String> getValidTypeNames() {
        return templates.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns the metadata/specification field names for a given type.
     *
     * @param typeName the product type name
     * @return list of specification field names, or empty list if type not found
     */
    public List<String> getMetadataFields(String typeName) {
        return getTemplate(typeName)
                .map(template -> {
                    if (template.getSpecifications() == null) {
                        return Collections.<String>emptyList();
                    }
                    return new ArrayList<>(template.getSpecifications().keySet());
                })
                .orElse(Collections.emptyList());
    }

    /**
     * Checks if a given type name is a valid product type (case-insensitive).
     *
     * @param typeName the product type name to check
     * @return true if the type is valid
     */
    public boolean isValidType(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return false;
        }
        boolean valid = templates.containsKey(typeName.toUpperCase());
        log.debug("Type '{}' is valid: {}", typeName, valid);
        return valid;
    }

    /**
     * Reloads templates from the YAML file.
     * Useful for updating templates without restarting the application.
     */
    public void reloadTemplates() {
        log.info("Reloading product templates from {}", templateFile);
        loadTemplates();
        log.info("Templates reloaded successfully, {} templates available", templates.size());
    }

    /**
     * Returns a list of comparable field names (from both fields and specifications)
     * for a given product type. Only fields where comparable=true are included.
     *
     * @param typeName the product type name (case-insensitive)
     * @return list of comparable field names, or empty list if type not found
     */
    public List<String> getComparableFields(String typeName) {
        return getTemplate(typeName)
                .map(template -> {
                    List<String> comparableFields = new ArrayList<>();

                    // Add comparable fields (price, size, weight)
                    if (template.getFields() != null) {
                        template.getFields().forEach((key, fieldDef) -> {
                            if (fieldDef.isComparable()) {
                                comparableFields.add(key);
                            }
                        });
                    }

                    // Add comparable specifications
                    if (template.getSpecifications() != null) {
                        template.getSpecifications().forEach((key, fieldDef) -> {
                            if (fieldDef.isComparable()) {
                                comparableFields.add(key);
                            }
                        });
                    }

                    Collections.sort(comparableFields);
                    return comparableFields;
                })
                .orElse(Collections.emptyList());
    }

    /**
     * Returns the union of all specification keys across all loaded templates.
     *
     * @return set of all known specification keys
     */
    public Set<String> getAllSpecificationKeys() {
        Set<String> allKeys = new TreeSet<>();
        templates.values().forEach(template -> {
            if (template.getSpecifications() != null) {
                allKeys.addAll(template.getSpecifications().keySet());
            }
        });
        return Collections.unmodifiableSet(allKeys);
    }

    /**
     * Returns a human-readable string with all types and their info.
     *
     * @return formatted string with all types info
     */
    public String getAllTypesInfo() {
        StringBuilder sb = new StringBuilder();
        templates.forEach((key, template) -> {
            List<String> specFields = template.getSpecifications() != null
                    ? new ArrayList<>(template.getSpecifications().keySet())
                    : Collections.emptyList();
            sb.append(String.format("%s: %s - Metadata: %s\n",
                    key, template.getDisplayName(), specFields));
        });
        return sb.toString();
    }
}
