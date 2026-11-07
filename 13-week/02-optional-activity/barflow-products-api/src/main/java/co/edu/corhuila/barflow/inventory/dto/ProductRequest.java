package co.edu.corhuila.barflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo que recibe POST y PUT. El cliente nunca envía id ni fechas. */
public record ProductRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 40) String category,
        @NotNull @Min(0) Integer price,
        @NotNull @Min(0) Integer stockQuantity) {
}
