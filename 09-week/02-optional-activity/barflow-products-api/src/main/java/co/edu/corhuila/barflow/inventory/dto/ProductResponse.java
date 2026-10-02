package co.edu.corhuila.barflow.inventory.dto;

import co.edu.corhuila.barflow.inventory.entity.Product;

import java.time.Instant;
import java.util.UUID;

/** Lo que la API devuelve: nunca se expone la entity directamente. */
public record ProductResponse(
        UUID id,
        String name,
        String category,
        Integer price,
        Integer stockQuantity,
        boolean isActive,
        Instant createdAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive(),
                product.getCreatedAt());
    }
}
