package com.novabank.auth.service;

import com.novabank.auth.dto.LoginRequestDTO;
import com.novabank.auth.dto.LoginResponseDTO;
import com.novabank.auth.dto.RegisterRequestDTO;
import com.novabank.auth.dto.RegisterResponseDTO;
import com.novabank.auth.dto.ValidateTokenResponseDTO;
import com.novabank.auth.exception.DuplicateUserException;
import com.novabank.auth.exception.InvalidCredentialsException;
import com.novabank.auth.exception.InvalidTokenException;
import com.novabank.auth.model.Usuario;
import com.novabank.auth.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Guarda credenciales con BCrypt y normaliza el username para evitar altas
     * duplicadas por diferencias de mayusculas o espacios.
     */
    @Transactional
    public Mono<RegisterResponseDTO> registrar(RegisterRequestDTO request) {
        return Mono.defer(() -> {
            String username = normalizeUsername(request.username());

            return usuarioRepository.existsByUsername(username)
                    .flatMap(exists -> {
                        if (Boolean.TRUE.equals(exists)) {
                            return Mono.error(new DuplicateUserException("Ya existe un usuario con ese username"));
                        }

                        Usuario usuario = new Usuario();
                        usuario.setUsername(username);
                        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
                        usuario.setRole(DEFAULT_ROLE);
                        usuario.setEnabled(true);
                        usuario.prepararParaCreacion();

                        return usuarioRepository.save(usuario)
                                .map(this::toRegisterResponse)
                                .onErrorMap(
                                        DataIntegrityViolationException.class,
                                        ex -> new DuplicateUserException(
                                                "Ya existe un usuario con alguno de los datos unicos indicados"
                                        )
                                );
                    });
        });
    }

    /**
     * Emite un JWT formativo despues de validar la password almacenada como
     * hash, sin implementar un servidor OAuth completo.
     */
    @Transactional(readOnly = true)
    public Mono<LoginResponseDTO> login(LoginRequestDTO request) {
        return Mono.defer(() -> {
            String username = normalizeUsername(request.username());

            return usuarioRepository.findByUsername(username)
                    .filter(usuario -> Boolean.TRUE.equals(usuario.getEnabled()))
                    .switchIfEmpty(Mono.error(new InvalidCredentialsException("Credenciales invalidas")))
                    .flatMap(usuario -> {
                        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
                            return Mono.error(new InvalidCredentialsException("Credenciales invalidas"));
                        }

                        return Mono.just(new LoginResponseDTO(
                                jwtService.generarToken(username),
                                "Bearer",
                                jwtService.getExpiration()
                        ));
                    });
        });
    }

    /**
     * Permite que el Gateway consulte a auth-server como autoridad central de
     * autenticacion antes de enrutar a los servicios de negocio.
     */
    public Mono<ValidateTokenResponseDTO> validarToken(String token) {
        return Mono.defer(() -> {
            if (token == null || token.isBlank()) {
                throw new InvalidTokenException("El token es obligatorio");
            }

            String normalizedToken = normalizeBearerToken(token);
            if (!jwtService.esTokenValido(normalizedToken)) {
                return Mono.just(new ValidateTokenResponseDTO(false, null));
            }

            return Mono.just(new ValidateTokenResponseDTO(true, jwtService.extraerUsername(normalizedToken)));
        });
    }

    private RegisterResponseDTO toRegisterResponse(Usuario usuario) {
        return new RegisterResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRole(),
                usuario.getEnabled(),
                usuario.getFechaCreacion()
        );
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String normalizeBearerToken(String token) {
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }
}
