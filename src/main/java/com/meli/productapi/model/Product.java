package com.meli.productapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("price")
    private MeasurableValue price;

    @JsonProperty("size")
    private MeasurableValue size;

    @JsonProperty("weight")
    private MeasurableValue weight;

    @JsonProperty("color")
    private String color;

    @JsonProperty("type")
    private String type;

    @JsonProperty("imageUrl")
    private String imageUrl;

    @JsonProperty("rating")
    private Double rating;

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
