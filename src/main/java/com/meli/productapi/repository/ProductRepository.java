package com.meli.productapi.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.model.Product;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Repository
public class ProductRepository {

    private static final String DATA_DIR = "data";
    private static final String PRODUCTS_FILE = "data/products.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] IMAGE_DOMAINS = {
        "https://picsum.photos",
        "https://via.placeholder.com",
        "https://loremflickr.com"
    };
    private static final String[] CATEGORIES = {
        "technology", "fashion", "food", "furniture", "sports", "random"
    };

    public ProductRepository() {
        initializeDataDir();
    }

    private void initializeDataDir() {
        try {
            Path path = Paths.get(DATA_DIR);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
            
            File productsFile = new File(PRODUCTS_FILE);
            if (!productsFile.exists()) {
                objectMapper.writeValue(productsFile, new ArrayList<Product>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar diretório de dados", e);
        }
    }

    public List<Product> findAll() {
        try {
            File file = new File(PRODUCTS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<Product>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler produtos do banco de dados", e);
        }
    }

    public Optional<Product> findById(String id) {
        return findAll().stream()
                .filter(product -> product.getId().equals(id))
                .findFirst();
    }

    public Product save(Product product) {
        try {
            List<Product> products = findAll();
            
            if (product.getId() == null || product.getId().isEmpty()) {
                // Gerar ID incremental
                long nextId = products.stream()
                    .mapToLong(p -> {
                        try {
                            return Long.parseLong(p.getId());
                        } catch (NumberFormatException e) {
                            return 0L;
                        }
                    })
                    .max()
                    .orElse(0L) + 1;
                product.setId(String.valueOf(nextId));
            }
            
            // Gerar imageUrl aleatória se não tiver
            if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
                product.setImageUrl(generateRandomImageUrl());
            }
            
            // Definir rating padrão se não tiver
            if (product.getRating() == null) {
                product.setRating(0.0);
            }
            
            products.removeIf(p -> p.getId().equals(product.getId()));
            products.add(product);
            
            objectMapper.writeValue(new File(PRODUCTS_FILE), products);
            return product;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar produto", e);
        }
    }

    public void deleteById(String id) {
        try {
            List<Product> products = findAll();
            products.removeIf(product -> product.getId().equals(id));
            objectMapper.writeValue(new File(PRODUCTS_FILE), products);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao deletar produto", e);
        }
    }

    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    public long count() {
        return findAll().size();
    }

    /**
     * Gera uma URL de imagem aleatória
     * Criado para facilitar testes e simular o envio da imagem
     * Porém uma das opções é criar um endpoint para upload da imagem em blob store como amazon s3
     * E retornar uma url, deve se considerar criar um ttl na imagem caso o objeto não seja persistido 
     */
    private String generateRandomImageUrl() {
        Random random = new Random();
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
