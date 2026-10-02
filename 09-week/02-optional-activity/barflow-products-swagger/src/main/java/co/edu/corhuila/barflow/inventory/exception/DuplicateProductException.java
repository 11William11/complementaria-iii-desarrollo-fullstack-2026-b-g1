package co.edu.corhuila.barflow.inventory.exception;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(String name) {
        super("a product named '" + name + "' already exists");
    }
}
