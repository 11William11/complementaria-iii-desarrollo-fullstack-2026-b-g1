package co.edu.corhuila.barflow.inventory.service;

import co.edu.corhuila.barflow.inventory.dto.ProductRequest;
import co.edu.corhuila.barflow.inventory.dto.ProductResponse;
import co.edu.corhuila.barflow.inventory.entity.Product;
import co.edu.corhuila.barflow.inventory.exception.DuplicateProductException;
import co.edu.corhuila.barflow.inventory.exception.ProductNotFoundException;
import co.edu.corhuila.barflow.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del service (semana 13): sin Spring, sin base de datos y sin HTTP.
 * El repository es un mock de Mockito, así que solo se prueban las reglas de negocio.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void createTrimsTheFieldsAndSavesTheProduct() {
        when(repository.existsByNameIgnoreCase("Cerveza Aguila")).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse created = service.create(
                new ProductRequest("  Cerveza Aguila ", " Cervezas ", 5000, 48));

        assertThat(created.name()).isEqualTo("Cerveza Aguila");
        assertThat(created.category()).isEqualTo("Cervezas");
        assertThat(created.price()).isEqualTo(5000);
        assertThat(created.stockQuantity()).isEqualTo(48);
        assertThat(created.isActive()).isTrue();
    }

    // ---------- Casos de error ----------

    @Test
    void createWithADuplicateNameThrowsAndDoesNotSave() {
        when(repository.existsByNameIgnoreCase("Cerveza Aguila")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ProductRequest("Cerveza Aguila", "Cervezas", 5000, 48)))
                .isInstanceOf(DuplicateProductException.class)
                .hasMessageContaining("Cerveza Aguila");

        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void getAnUnknownIdThrowsProductNotFound() {
        UUID unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(unknown))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    @Test
    void updateWithANegativeStockIsRejected() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new Product("Cerveza Aguila", "Cervezas", 5000, 48)));

        // Límite BR-02: el stock nunca queda por debajo de cero
        assertThatThrownBy(() -> service.update(id, new ProductRequest("Cerveza Aguila", "Cervezas", 5000, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stock quantity must be zero or more");
    }
}
