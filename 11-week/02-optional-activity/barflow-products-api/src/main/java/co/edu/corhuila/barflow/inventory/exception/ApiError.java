package co.edu.corhuila.barflow.inventory.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Formato de error de BarFlow (07-api/rest-conventions.md):
 * {@code error} es un código estable, {@code message} es para humanos y
 * {@code details} solo aparece en errores de validación.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String error, String message, List<FieldDetail> details) {

    public record FieldDetail(String field, String message) {
    }

    public static ApiError of(String error, String message) {
        return new ApiError(error, message, null);
    }
}
