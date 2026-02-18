package com.meli.productapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MeasurableValue {

    /**
     * The numeric value (e.g., 5999.99, 0.187, 6.1)
     */
    @JsonProperty("value")
    private Double value;

    /**
     * The unit/metric (e.g., "BRL", "kg", "inches")
     */
    @JsonProperty("unit")
    private String unit;
}
