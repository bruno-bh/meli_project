package com.meli.productapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product entity representing an item in the catalog (e.g. a cellphone, computer, clothing)")
public class Product {

    @Schema(description = "Unique product identifier (auto-generated)", example = "1")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Product name", example = "Samsung Galaxy S24 Ultra", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    private String name;

    @Schema(description = "Product description", example = "Smartphone Samsung Galaxy S24 Ultra 256GB 12GB RAM Tela 6.8 pol")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Product price with currency unit", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("price")
    private MeasurableValue price;

    @Schema(description = "Product size with unit")
    @JsonProperty("size")
    private MeasurableValue size;

    @Schema(description = "Product weight with unit")
    @JsonProperty("weight")
    private MeasurableValue weight;

    @Schema(description = "Product color", example = "Titanium Gray")
    @JsonProperty("color")
    private String color;

    @Schema(description = "Product type (must match a YAML template)", example = "CELLPHONES", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("type")
    private String type;

    @Schema(description = "Product image URL (auto-generated if empty)")
    @JsonProperty("imageUrl")
    private String imageUrl;

    @Schema(description = "Product rating (0.0 – 5.0)", example = "4.5")
    @JsonProperty("rating")
    private Double rating;

    @Schema(description = "Type-specific specifications defined by the product template (e.g. brand, storage_gb, memory_gb, camera_mp, battery_capacity, operating_system for CELLPHONES)",
           example = "{\"brand\":\"Samsung\",\"storage_gb\":\"256\",\"memory_gb\":\"12\",\"screen_size\":\"6.8\",\"camera_mp\":\"200\",\"battery_capacity\":\"5000\",\"operating_system\":\"Android 14\",\"model_version\":\"S24 Ultra\"}")
    @JsonProperty("specifications")
    private Map<String, Object> specifications;

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", size=" + size +
                ", weight=" + weight +
                ", color='" + color + '\'' +
                ", type='" + type + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", rating=" + rating +
                ", specifications=" + specifications +
                '}';    }
}
