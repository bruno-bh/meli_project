package com.meli.productapi.controller;

import com.meli.productapi.model.template.ProductTemplate;
import com.meli.productapi.service.ProductTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for product template operations.
 * Provides endpoints to query and reload product type templates.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Templates", description = "Product type template management (YAML-driven)")
public class TemplateController {

    private final ProductTemplateService templateService;

    public TemplateController(ProductTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Returns all loaded product templates.
     */
    @Operation(summary = "List all product type templates", description = "Returns every product type template loaded from YAML.")
    @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    @GetMapping
    public ResponseEntity<Map<String, ProductTemplate>> getAllTemplates() {
        Map<String, ProductTemplate> templates = templateService.getAllTemplates();
        log.debug("Returning {} product templates", templates.size());
        return ResponseEntity.ok(templates);
    }

    /**
     * Returns the details of a specific product template by type name.
     * Returns 404 if the type is not found.
     */
    @Operation(summary = "Get template by type", description = "Returns the template definition for a specific product type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template found"),
            @ApiResponse(responseCode = "404", description = "Template type not found")
    })
    @GetMapping("/{type}")
    public ResponseEntity<ProductTemplate> getTemplateByType(
            @Parameter(description = "Product type name (e.g. CELLPHONES)", required = true) @PathVariable String type) {
        log.debug("Fetching template for type: {}", type);
        return templateService.getTemplate(type)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Template not found for type: {}", type);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Reloads templates from disk without restarting the application.
     */
    @Operation(summary = "Reload templates", description = "Reloads all product type templates from the YAML file at runtime.")
    @ApiResponse(responseCode = "200", description = "Templates reloaded successfully")
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadTemplates() {
        log.info("Reloading product templates");
        templateService.reloadTemplates();
        int count = templateService.getAllTemplates().size();
        log.info("Templates reloaded successfully, {} templates available", count);
        Map<String, Object> response = Map.of(
                "message", "Templates reloaded successfully",
                "count", count
        );
        return ResponseEntity.ok(response);
    }
}
