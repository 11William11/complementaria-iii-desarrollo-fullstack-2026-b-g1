package co.edu.corhuila.barflow.inventory.service;

import co.edu.corhuila.barflow.inventory.dto.ProductRequest;
import co.edu.corhuila.barflow.inventory.dto.ProductResponse;
import co.edu.corhuila.barflow.inventory.entity.Product;
import co.edu.corhuila.barflow.inventory.exception.DuplicateProductException;
import co.edu.corhuila.barflow.inventory.exception.ProductNotFoundException;
import co.edu.corhuila.barflow.inventory.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Reglas de negocio del catálogo de productos. No conoce HTTP: recibe DTOs,
 * decide y delega la persistencia al repository.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductResponse> list(String category) {
        List<Product> products = (category == null || category.isBlank())
                ? repository.findAll(Sort.by("name"))
                : repository.findByCategoryIgnoreCaseAndActiveTrueOrderByNameAsc(category);
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse get(UUID id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateProductException(name);
        }
        Product product = new Product(name, request.category().trim(),
                request.price(), request.stockQuantity());
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = find(id);
        String name = request.name().trim();
        if (!product.getName().equalsIgnoreCase(name) && repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateProductException(name);
        }
        product.rename(name);
        product.changeCategory(request.category().trim());
        product.changePrice(request.price());
        product.changeStock(request.stockQuantity());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private Product find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
