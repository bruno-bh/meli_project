package com.meli.productapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Represents a value with an associated unit of measurement.
 * Used for price, size, and weight fields in Product to avoid ambiguity.
 *
 * Example JSON serialization:
 * { "value": 5999.99, "unit": "BRL" }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A numeric value paired with its measurement unit")
public class MeasurableValue {

    /**
     * The numeric value (e.g., 5999.99, 0.187, 6.1)
     */
    @Schema(description = "Numeric value (e.g. 7499.99 for price, 6.8 for screen size, 0.233 for weight)", example = "7499.99")
    @JsonProperty("value")
    private Double value;

    /**
     * The unit/metric (e.g., "BRL", "kg", "inches")
     */
    @Schema(description = "Unit of measurement (e.g. BRL, inches, kg — defaults are set per product type in the YAML template)", example = "BRL")
    @JsonProperty("unit")
    private String unit;
}
