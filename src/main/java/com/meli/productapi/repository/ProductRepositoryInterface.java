package com.meli.productapi.repository;

import com.meli.productapi.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Interface for the product repository.
 * Allows different implementations (JSON file, database, etc.).
 */
public interface ProductRepositoryInterface {

    List<Product> findAll();

    Optional<Product> findById(String id);

    Product save(Product product);

    void deleteById(String id);

    boolean existsById(String id);

    long count();
}
