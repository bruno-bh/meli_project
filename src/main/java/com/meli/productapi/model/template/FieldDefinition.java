package com.meli.productapi.model.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Represents the definition of a field in a product template.
 * Defines the type, default unit, whether it is required, and whether it can be compared.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {

    /**
     * The data type of the field: "number", "text", or "date"
     */
    @JsonProperty("type")
    private String type;

    /**
     * The default unit for this field (e.g., "BRL", "kg", "GB", "inches").
     * Mapped from "default_unit" in YAML.
     */
    @JsonProperty("default_unit")
    private String defaultUnit;

    /**
     * Whether this field is required when creating a product of this type.
     */
    @JsonProperty("required")
    private boolean required;

    /**
     * Whether this field can be used in product comparison.
     */
    @JsonProperty("comparable")
    private boolean comparable;
}
