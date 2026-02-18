package com.meli.productapi.controller;

import com.meli.productapi.model.template.FieldDefinition;
import com.meli.productapi.model.template.ProductTemplate;
import com.meli.productapi.service.ProductTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TemplateController.class)
@DisplayName("TemplateController — template endpoint tests")
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductTemplateService templateService;

    @Test
    @DisplayName("GET /api/v1/templates — should return all templates")
    void testGetAllTemplates() throws Exception {
        ProductTemplate cellphones = ProductTemplate.builder()
                .name("CELLPHONES")
                .displayName("Smartphones")
                .fields(Map.of("price", FieldDefinition.builder()
                        .type("number").defaultUnit("BRL").required(true).comparable(true).build()))
                .specifications(Map.of("brand", FieldDefinition.builder()
                        .type("text").required(true).comparable(false).build()))
                .build();

        when(templateService.getAllTemplates()).thenReturn(Map.of("CELLPHONES", cellphones));

        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CELLPHONES").exists())
                .andExpect(jsonPath("$.CELLPHONES.name").value("CELLPHONES"))
                .andExpect(jsonPath("$.CELLPHONES.display_name").value("Smartphones"));

        verify(templateService, times(1)).getAllTemplates();
    }

    @Test
    @DisplayName("GET /api/v1/templates/{type} — should return template by type")
    void testGetTemplateByType() throws Exception {
        ProductTemplate cellphones = ProductTemplate.builder()
                .name("CELLPHONES")
                .displayName("Smartphones")
                .fields(Map.of("price", FieldDefinition.builder()
                        .type("number").defaultUnit("BRL").required(true).comparable(true).build()))
                .specifications(Map.of("brand", FieldDefinition.builder()
                        .type("text").required(true).comparable(false).build()))
                .build();

        when(templateService.getTemplate("CELLPHONES")).thenReturn(Optional.of(cellphones));

        mockMvc.perform(get("/api/v1/templates/CELLPHONES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CELLPHONES"))
                .andExpect(jsonPath("$.display_name").value("Smartphones"))
                .andExpect(jsonPath("$.fields.price.type").value("number"));

        verify(templateService, times(1)).getTemplate("CELLPHONES");
    }

    @Test
    @DisplayName("GET /api/v1/templates/{type} — should return 404 for invalid type")
    void testGetTemplateNotFound() throws Exception {
        when(templateService.getTemplate("INVALID")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/templates/INVALID"))
                .andExpect(status().isNotFound());

        verify(templateService, times(1)).getTemplate("INVALID");
    }

    @Test
    @DisplayName("POST /api/v1/templates/reload — should reload templates successfully")
    void testReloadTemplates() throws Exception {
        doNothing().when(templateService).reloadTemplates();
        when(templateService.getAllTemplates()).thenReturn(Map.of(
                "CELLPHONES", ProductTemplate.builder().name("CELLPHONES").displayName("Smartphones").build(),
                "COMPUTERS", ProductTemplate.builder().name("COMPUTERS").displayName("Computadores").build()
        ));

        mockMvc.perform(post("/api/v1/templates/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Templates reloaded successfully"))
                .andExpect(jsonPath("$.count").value(2));

        verify(templateService, times(1)).reloadTemplates();
    }
}
