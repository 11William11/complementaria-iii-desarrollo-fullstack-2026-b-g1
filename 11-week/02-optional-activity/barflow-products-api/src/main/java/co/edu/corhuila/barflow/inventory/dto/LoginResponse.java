package co.edu.corhuila.barflow.inventory.dto;

/** Respuesta del login: el token se envía después como {@code Authorization: Bearer <token>}. */
public record LoginResponse(String token, String tokenType, long expiresIn) {
}
