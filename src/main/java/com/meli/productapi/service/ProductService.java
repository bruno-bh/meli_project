package com.meli.productapi.service;

import com.meli.productapi.exception.IncompatibleProductTypesException;
import com.meli.productapi.exception.ProductNotFoundException;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductComparisonResponse;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product getProductById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto com ID " + id + " não encontrado"));
    }

    public Product createProduct(Product product) {
        validateProduct(product);
        // ID incremental será gerado automaticamente pelo repository
        return repository.save(product);
    }

    public Product updateProduct(String id, Product productDetails) {
        Product product = getProductById(id);
        validateProduct(productDetails);

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setSize(productDetails.getSize());
        product.setWeight(productDetails.getWeight());
        product.setColor(productDetails.getColor());
        product.setType(productDetails.getType());
        product.setImageUrl(productDetails.getImageUrl());
        product.setRating(productDetails.getRating());
        product.setSpecifications(productDetails.getSpecifications());

        return repository.save(product);
    }

    public void deleteProduct(String id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto com ID " + id + " não encontrado"));

        repository.deleteById(id);
    }

    public long getTotalProducts() {
        return repository.count();
    }

    /**
     * Busca produtos com filtros múltiplos e paginação
     * 
     * @param filter Objeto com todos os filtros de busca
     * @return Lista de produtos filtrados e paginados
     */
    public List<Product> searchProducts(ProductFilter filter) {
        // Validar parâmetros de preço
        if (filter.getPriceMin() != null && filter.getPriceMin() <= 0) {
            throw new IllegalArgumentException("Preço mínimo deve ser maior que zero");
        }
        
        if (filter.getPriceMax() != null && filter.getPriceMax() <= 0) {
            throw new IllegalArgumentException("Preço máximo deve ser maior que zero");
        }
        
        // Validar relação entre preços
        filter.validate();
        
        List<Product> allProducts = repository.findAll();
        
        // Aplicar filtros
        List<Product> filteredProducts = allProducts.stream()
                .filter(product -> {
                    // Filtro por nome
                    if (filter.getName() != null && !filter.getName().isEmpty()) {
                        if (!product.getName().toLowerCase().contains(filter.getName().toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filtro por descrição
                    if (filter.getDescription() != null && !filter.getDescription().isEmpty()) {
                        if (product.getDescription() == null || 
                            !product.getDescription().toLowerCase().contains(filter.getDescription().toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filtro por tipo
                    if (filter.getType() != null && !filter.getType().isEmpty()) {
                        if (!product.getType().equalsIgnoreCase(filter.getType())) {
                            return false;
                        }
                    }
                    
                    // Filtro por preço mínimo
                    if (filter.getPriceMin() != null && product.getPrice() < filter.getPriceMin()) {
                        return false;
                    }
                    
                    // Filtro por preço máximo
                    if (filter.getPriceMax() != null && product.getPrice() > filter.getPriceMax()) {
                        return false;
                    }
                    
                    return true;
                })
                .toList();
        
        // Aplicar paginação
        return applyPagination(filteredProducts, filter.getPage(), filter.getPageSize());
    }

    /**
     * Aplica paginação na lista de produtos
     * 
     * @param products Lista de produtos
     * @param page     Número da página (default: 1)
     * @param pageSize Tamanho da página (null = todos)
     * @return Lista paginada
     */
    private List<Product> applyPagination(List<Product> products, Integer page, Integer pageSize) {
        // Se pageSize for null, retorna todos
        if (pageSize == null) {
            return products;
        }
        
        // Validar pageSize
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Tamanho da página deve ser maior que zero");
        }
        
        // Define página padrão como 1 se null, zero ou negativo
        int currentPage = (page != null && page > 0) ? page : 1;
        
        // Calcula índices
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, products.size());
        
        // Verifica se a página está dentro dos limites
        if (startIndex >= products.size()) {
            return new ArrayList<>();
        }
        
        return products.subList(startIndex, endIndex);
    }

    /**
     * Compara múltiplos produtos
     * 
     * @param productIds Lista de IDs dos produtos
     * @param filters    Lista de campos para comparar (vazio = todos os campos)
     * @return ProductComparisonResponse com dados da comparação
     * @throws ProductNotFoundException          Se algum ID não existir
     * @throws IncompatibleProductTypesException Se os produtos tiverem tipos
     *                                           diferentes
     */
    public ProductComparisonResponse compareProducts(List<String> productIds, List<String> filters) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Deve ser fornecido pelo menos um ID de produto");
        }

        // Obter produtos
        List<Product> products = productIds.stream()
                .map(this::getProductById)
                .collect(Collectors.toList());

        // Validar se todos os produtos têm o mesmo tipo
        String firstType = products.get(0).getType();
        if (!products.stream().allMatch(p -> p.getType().equals(firstType))) {
            throw new IncompatibleProductTypesException(
                    "Não é possível comparar produtos de tipos diferentes. " +
                            "Todos os produtos devem ser do mesmo tipo.");
        }

        // Preparar filtros
        List<String> appliedFilters = (filters == null || filters.isEmpty())
                ? getDefaultFilters()
                : filters;

        // Montar dados de comparação
        List<Map<String, Object>> comparisonData = products.stream()
                .map(product -> extractProductFields(product, appliedFilters))
                .collect(Collectors.toList());

        return ProductComparisonResponse.builder()
                .productType(firstType)
                .appliedFilters(appliedFilters)
                .products(comparisonData)
                .build();
    }

    /**
     * Extrai campos especificados do produto
     * Sempre inclui campos obrigatórios: id, name
     */
    private Map<String, Object> extractProductFields(Product product, List<String> filters) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Campos obrigatórios - sempre incluídos
        result.put("id", product.getId());
        result.put("name", product.getName());

        // Campos opcionais - incluídos se não forem nulos/vazios
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            result.put("description", product.getDescription());
        }

        if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            result.put("imageUrl", product.getImageUrl());
        }

        // Adiciona campos adicionais baseados nos filtros
        for (String filter : filters) {
            String filterLower = filter.toLowerCase();

            // Pula campos obrigatórios que já foram adicionados
            if (filterLower.equals("id") || filterLower.equals("name")) {
                continue;
            }

            switch (filterLower) {
                case "description":
                case "imageurl":
                    // Já incluídos automaticamente se não forem vazios
                    break;
                case "price":
                    result.put("price", product.getPrice());
                    break;
                case "size":
                    result.put("size", product.getSize());
                    break;
                case "weight":
                    result.put("weight", product.getWeight());
                    break;
                case "color":
                    result.put("color", product.getColor());
                    break;
                case "rating":
                    result.put("rating", product.getRating());
                    break;
                case "specifications":
                    result.put("specifications", product.getSpecifications());
                    break;
                default:
                    // Tenta obter do specifications se não for um campo padrão
                    if (product.getSpecifications() != null &&
                            product.getSpecifications().containsKey(filter)) {
                        result.put(filter, product.getSpecifications().get(filter));
                    }
            }
        }

        return result;
    }

    /**
     * Retorna lista padrão de campos para comparação
     * Nota: id e name são sempre incluídos automaticamente
     */
    private List<String> getDefaultFilters() {
        return Arrays.asList(
                "description",
                "imageUrl",
                "price",
                "size",
                "weight",
                "color",
                "rating",
                "specifications");
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new IllegalArgumentException("Preço do produto deve ser um valor positivo");
        }
        if (product.getType() == null || product.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo do produto é obrigatório");
        }
    }
}
