package com.novabank.auth.controller;

import com.novabank.auth.dto.LoginRequestDTO;
import com.novabank.auth.dto.LoginResponseDTO;
import com.novabank.auth.dto.RegisterRequestDTO;
import com.novabank.auth.dto.RegisterResponseDTO;
import com.novabank.auth.dto.ValidateTokenResponseDTO;
import com.novabank.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateTokenResponseDTO> validate(
            @RequestParam @NotBlank(message = "El token es obligatorio") String token
    ) {
        return ResponseEntity.ok(authService.validarToken(token));
    }
}
