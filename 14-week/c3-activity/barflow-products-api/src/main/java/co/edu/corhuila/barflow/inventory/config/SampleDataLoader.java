package co.edu.corhuila.barflow.inventory.config;

import co.edu.corhuila.barflow.inventory.entity.Product;
import co.edu.corhuila.barflow.inventory.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** Carga unos productos de ejemplo al arrancar, para probar la API sin preparar datos. */
@Component
@ConditionalOnProperty(name = "barflow.sample-data", havingValue = "true")
public class SampleDataLoader implements CommandLineRunner {

    private final ProductRepository repository;

    public SampleDataLoader(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new Product("Cerveza Aguila", "Cervezas", 5000, 48),
                new Product("Club Colombia", "Cervezas", 6000, 36),
                new Product("Aguardiente Doble Anis", "Licores", 65000, 12),
                new Product("Gaseosa Postobon", "Bebidas", 3000, 24)));
    }
}
