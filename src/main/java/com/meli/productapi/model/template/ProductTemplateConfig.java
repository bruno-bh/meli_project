package com.meli.productapi.model.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * Wrapper class for the YAML root structure.
 * Maps the top-level "templates" key to a map of ProductTemplate objects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTemplateConfig {

    /**
     * Map of template name (e.g., "CELLPHONES") to its ProductTemplate definition.
     */
    @JsonProperty("templates")
    private Map<String, ProductTemplate> templates;
}
