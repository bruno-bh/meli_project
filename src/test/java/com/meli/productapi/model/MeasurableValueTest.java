package com.meli.productapi.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MeasurableValue model
 */
@DisplayName("MeasurableValue — value+unit model tests")
class MeasurableValueTest {

    @Test
    @DisplayName("Should create MeasurableValue with builder")
    void testMeasurableValueBuilder() {
        MeasurableValue mv = MeasurableValue.builder()
                .value(5999.99)
                .unit("BRL")
                .build();

        assertNotNull(mv);
        assertEquals(5999.99, mv.getValue());
        assertEquals("BRL", mv.getUnit());
    }

    @Test
    @DisplayName("Should allow null value (optional field)")
    void testMeasurableValueNullValue() {
        MeasurableValue mv = MeasurableValue.builder()
                .value(null)
                .unit("M")
                .build();

        assertNotNull(mv);
        assertNull(mv.getValue());
        assertEquals("M", mv.getUnit());
    }

    @Test
    @DisplayName("Should serialize correctly with Jackson")
    void testMeasurableValueSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        MeasurableValue mv = MeasurableValue.builder()
                .value(99.99)
                .unit("BRL")
                .build();

        String json = mapper.writeValueAsString(mv);
        assertTrue(json.contains("\"value\""));
        assertTrue(json.contains("99.99"));
        assertTrue(json.contains("\"unit\""));
        assertTrue(json.contains("\"BRL\""));

        // Deserialize back
        MeasurableValue deserialized = mapper.readValue(json, MeasurableValue.class);
        assertEquals(mv.getValue(), deserialized.getValue());
        assertEquals(mv.getUnit(), deserialized.getUnit());
    }
}
