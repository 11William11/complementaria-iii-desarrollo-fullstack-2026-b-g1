package co.edu.corhuila.barflow.inventory.controller;

import co.edu.corhuila.barflow.inventory.dto.LoginRequest;
import co.edu.corhuila.barflow.inventory.dto.LoginResponse;
import co.edu.corhuila.barflow.inventory.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login: cambia usuario y contraseña por un JWT. Es el único endpoint de la API que no pide token. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // Si usuario o contraseña no coinciden lanza BadCredentialsException → 401 (GlobalExceptionHandler)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return new LoginResponse(jwtService.generate(authentication.getName()), "Bearer",
                jwtService.expiresInSeconds());
    }
}
