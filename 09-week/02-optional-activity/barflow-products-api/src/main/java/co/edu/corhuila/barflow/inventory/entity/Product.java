package co.edu.corhuila.barflow.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Producto que vende el bar (cerveza, aguardiente, gaseosa...).
 * Se mapea a la tabla {@code products} del módulo inventory de BarFlow.
 */
@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_name", columnNames = "name"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    /** Precio en pesos colombianos enteros (sin centavos). */
    @Column(nullable = false)
    private Integer price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío que exige JPA para crear la instancia al leer una fila. */
    protected Product() {
    }

    public Product(String name, String category, int price, int stockQuantity) {
        this.name = name;
        this.category = category;
        changePrice(price);
        changeStock(stockQuantity);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /** Invariante de BarFlow: el precio es un entero de pesos, cero o más. */
    public void changePrice(int newPrice) {
        if (newPrice < 0) {
            throw new IllegalArgumentException("price must be zero or more");
        }
        this.price = newPrice;
    }

    /** Invariante BR-02: el stock nunca queda por debajo de cero. */
    public void changeStock(int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("stock quantity must be zero or more");
        }
        this.stockQuantity = newStock;
    }

    /** Un producto retirado ya no se puede agregar a pedidos (BR-01). */
    public void retire() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void changeCategory(String category) {
        this.category = category;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
