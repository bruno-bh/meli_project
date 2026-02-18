package com.meli.productapi.controller;

import com.meli.productapi.model.template.ProductTemplate;
import com.meli.productapi.service.ProductTemplateService;
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
public class TemplateController {

    private final ProductTemplateService templateService;

    public TemplateController(ProductTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Returns all loaded product templates.
     */
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
    @GetMapping("/{type}")
    public ResponseEntity<ProductTemplate> getTemplateByType(@PathVariable String type) {
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
