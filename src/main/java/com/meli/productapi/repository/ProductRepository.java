package com.meli.productapi.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Repository
public class ProductRepository implements ProductRepositoryInterface {

    private final String dataDir;
    private final String productsFile;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock = new ReentrantLock();
    private static final String[] IMAGE_DOMAINS = {
            "https://picsum.photos",
            "https://via.placeholder.com",
            "https://loremflickr.com"
    };
    private static final String[] CATEGORIES = {
            "technology", "fashion", "food", "furniture", "sports", "random"
    };

    public ProductRepository(
            @Value("${product.data.dir:data}") String dataDir,
            @Value("${product.data.file:data/products.json}") String productsFile,
            ObjectMapper objectMapper) {
        this.dataDir = dataDir;
        this.productsFile = productsFile;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        log.info("Initializing ProductRepository with data file: {}", productsFile);
        initializeDataDir();
    }

    private void initializeDataDir() {
        try {
            Path path = Paths.get(dataDir);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
                log.info("Data directory created: {}", dataDir);
            }

            File file = new File(productsFile);
            if (!file.exists()) {
                objectMapper.writeValue(file, new ArrayList<Product>());
                log.info("Products file created: {}", productsFile);
            }
        } catch (IOException e) {
            log.error("Error initializing data directory: {}", e.getMessage());
            throw new RuntimeException("Error initializing data directory", e);
        }
    }

    public List<Product> findAll() {
        try {
            File file = new File(productsFile);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            List<Product> products = objectMapper.readValue(file, new TypeReference<List<Product>>() {});
            
            log.debug("Loaded {} products from file", products.size());
            return products;
        } catch (IOException e) {
            log.error("Error reading products: {}", e.getMessage());
            throw new RuntimeException("Error reading products from data file", e);
        }
    }

    public Optional<Product> findById(String id) {
        return findAll().stream()
                .filter(product -> product.getId().equals(id))
                .findFirst();
    }

    public Product save(Product product) {
        lock.lock();
        try {
            List<Product> products = findAll();

            if (product.getId() == null || product.getId().isEmpty()) {
                // Generate incremental ID
                long nextId = products.stream().mapToLong(p -> {
                    try {
                        return Long.parseLong(p.getId());
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                }).max().orElse(0L) + 1;

                product.setId(String.valueOf(nextId));
                log.debug("Auto-generated ID: {}", product.getId());
            }

            // Generate random imageUrl if not set
            if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
                product.setImageUrl(generateRandomImageUrl());
            }

            // Set default rating if not set
            if (product.getRating() == null) {
                product.setRating(0.0);
            }

            products.removeIf(p -> p.getId().equals(product.getId()));
            products.add(product);

            objectMapper.writeValue(new File(productsFile), products);
            log.debug("Product saved with ID: {}", product.getId());
            return product;
        } catch (IOException e) {
            log.error("Error saving product: {}", e.getMessage());
            throw new RuntimeException("Error saving product", e);
        } finally {
            lock.unlock();
        }
    }

    public void deleteById(String id) {
        lock.lock();
        try {
            List<Product> products = findAll();
            products.removeIf(product -> product.getId().equals(id));
            objectMapper.writeValue(new File(productsFile), products);
            log.debug("Product deleted with ID: {}", id);
        } catch (IOException e) {
            log.error("Error deleting product ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Error deleting product", e);
        } finally {
            lock.unlock();
        }
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public long count() {
        long total = findAll().size();
        log.debug("Total product count: {}", total);
        return total;
    }

    /**
     * Generates a random image URL.
     * Created to facilitate testing and simulate image submission.
     * An alternative approach would be to create an endpoint for uploading images
     * to a blob store such as Amazon S3
     * and return a URL. Consider setting a TTL on the image if the object is not
     * persisted.
     */
    private String generateRandomImageUrl() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String domain = IMAGE_DOMAINS[random.nextInt(IMAGE_DOMAINS.length)];
        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        int width = 300 + random.nextInt(200);
        int height = 300 + random.nextInt(200);

        if (domain.contains("picsum")) {
            return domain + "/" + width + "/" + height + "?random=" + UUID.randomUUID();
        } else if (domain.contains("placeholder")) {
            return domain + "/" + width + "x" + height + "?text=" + category;
        } else {
            return domain + "/" + width + "/" + height + "/" + category;
        }
    }
}
