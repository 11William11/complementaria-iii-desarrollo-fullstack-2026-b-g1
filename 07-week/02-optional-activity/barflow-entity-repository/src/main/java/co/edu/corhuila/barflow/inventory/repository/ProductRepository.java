package co.edu.corhuila.barflow.inventory.repository;

import co.edu.corhuila.barflow.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Acceso a la tabla products. Al extender JpaRepository hereda el CRUD
 * (save, findById, findAll, deleteById, count...) sin escribir SQL.
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /** Carta del bar: productos activos de una categoría, ordenados por nombre. */
    List<Product> findByCategoryIgnoreCaseAndActiveTrueOrderByNameAsc(String category);

    /** Alerta de inventario: productos activos con stock igual o menor al umbral. */
    List<Product> findByStockQuantityLessThanEqualAndActiveTrue(int threshold);

    /** Evita registrar dos veces el mismo producto. */
    boolean existsByNameIgnoreCase(String name);
}
