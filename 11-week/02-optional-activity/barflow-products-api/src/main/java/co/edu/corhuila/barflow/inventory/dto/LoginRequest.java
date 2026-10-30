package co.edu.corhuila.barflow.inventory.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/v1/auth/login. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
