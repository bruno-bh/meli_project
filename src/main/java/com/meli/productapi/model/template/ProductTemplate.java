package com.meli.productapi.model.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * Represents a complete product template (e.g., CELLPHONES, COMPUTERS).
 * Each template defines the expected fields and specifications for a product type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTemplate {

    /**
     * The template name/key (e.g., "CELLPHONES"). Set programmatically from the YAML key.
     */
    private String name;

    /**
     * The human-readable display name (e.g., "Smartphones").
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * Core product fields (price, size, weight) with their definitions.
     */
    @JsonProperty("fields")
    private Map<String, FieldDefinition> fields;

    /**
     * Type-specific specification fields with their definitions.
     */
    @JsonProperty("specifications")
    private Map<String, FieldDefinition> specifications;
}
