package co.edu.corhuila.barflow.inventory.repository;

import co.edu.corhuila.barflow.inventory.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private TestEntityManager em;

    private Product aguila;

    @BeforeEach
    void seed() {
        aguila = repository.save(new Product("Cerveza Aguila", "Cervezas", 5000, 48));
        repository.save(new Product("Club Colombia", "Cervezas", 6000, 3));
        repository.save(new Product("Aguardiente Doble Anis", "Licores", 65000, 2));
        Product retired = new Product("Cerveza Pilsen", "Cervezas", 4500, 0);
        retired.retire();
        repository.save(retired);
        em.flush();
        em.clear();
    }

    @Test
    void create_assignsIdAndCreationDate() {
        assertThat(aguila.getId()).isNotNull();
        assertThat(aguila.getCreatedAt()).isNotNull();
        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    void read_findsByIdAndReturnsEmptyForUnknownId() {
        assertThat(repository.findById(aguila.getId()))
                .get().extracting(Product::getName).isEqualTo("Cerveza Aguila");
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void update_savingAChangedEntityUpdatesTheRow() {
        Product product = repository.findById(aguila.getId()).orElseThrow();
        product.changePrice(5500);
        repository.saveAndFlush(product);
        em.clear();

        assertThat(repository.findById(aguila.getId()).orElseThrow().getPrice()).isEqualTo(5500);
    }

    @Test
    void delete_removesTheRow() {
        repository.deleteById(aguila.getId());
        em.flush();

        assertThat(repository.existsById(aguila.getId())).isFalse();
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void findByCategory_returnsOnlyActiveProductsSortedByName() {
        List<Product> beers = repository.findByCategoryIgnoreCaseAndActiveTrueOrderByNameAsc("cervezas");

        assertThat(beers).extracting(Product::getName)
                .containsExactly("Cerveza Aguila", "Club Colombia");
    }

    @Test
    void findByLowStock_returnsActiveProductsAtOrBelowTheThreshold() {
        List<Product> lowStock = repository.findByStockQuantityLessThanEqualAndActiveTrue(5);

        assertThat(lowStock).extracting(Product::getName)
                .containsExactlyInAnyOrder("Club Colombia", "Aguardiente Doble Anis");
    }

    @Test
    void existsByName_ignoresCase() {
        assertThat(repository.existsByNameIgnoreCase("cerveza aguila")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("Poker")).isFalse();
    }

    @Test
    void entity_rejectsNegativePriceAndStock() {
        assertThatThrownBy(() -> new Product("Gaseosa", "Bebidas", -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Product("Gaseosa", "Bebidas", 3000, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
