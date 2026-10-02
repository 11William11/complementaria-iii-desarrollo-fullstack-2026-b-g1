package co.edu.corhuila.barflow.inventory.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) {
        super("product " + id + " does not exist");
    }
}
