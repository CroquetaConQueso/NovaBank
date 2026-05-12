package com.novabank.auth.controller;

import com.novabank.auth.dto.LoginRequestDTO;
import com.novabank.auth.dto.LoginResponseDTO;
import com.novabank.auth.dto.RegisterRequestDTO;
import com.novabank.auth.dto.RegisterResponseDTO;
import com.novabank.auth.dto.ValidateTokenResponseDTO;
import com.novabank.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Registro, login y validacion de tokens JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un usuario para autenticacion JWT formativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "409", description = "Usuario ya existente")
    })
    public Mono<ResponseEntity<RegisterResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.registrar(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Valida credenciales y devuelve un token JWT Bearer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login correcto"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o peticion mal formada"),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
    })
    public Mono<ResponseEntity<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/validate")
    @Operation(summary = "Validar token", description = "Comprueba si un token JWT es valido para el Gateway.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validacion realizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Token ausente o mal informado")
    })
    public Mono<ResponseEntity<ValidateTokenResponseDTO>> validate(
            @RequestParam @NotBlank(message = "El token es obligatorio") String token
    ) {
        return authService.validarToken(token)
                .map(ResponseEntity::ok);
    }
}
