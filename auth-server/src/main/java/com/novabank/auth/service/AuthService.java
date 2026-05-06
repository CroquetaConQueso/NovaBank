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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public RegisterResponseDTO registrar(RegisterRequestDTO request) {
        String username = normalizeUsername(request.username());
        if (usuarioRepository.existsByUsername(username)) {
            throw new DuplicateUserException("Ya existe un usuario con ese username");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRole(DEFAULT_ROLE);
        usuario.setEnabled(true);

        Usuario saved = usuarioRepository.save(usuario);
        return toRegisterResponse(saved);
    }

    /**
     * Emite un JWT formativo despues de validar la password almacenada como
     * hash, sin implementar un servidor OAuth completo.
     */
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        String username = normalizeUsername(request.username());
        Usuario usuario = usuarioRepository.findByUsername(username)
                .filter(Usuario::getEnabled)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales invalidas");
        }

        return new LoginResponseDTO(jwtService.generarToken(username), "Bearer", jwtService.getExpiration());
    }

    /**
     * Permite que el Gateway consulte a auth-server como autoridad central de
     * autenticacion antes de enrutar a los servicios de negocio.
     */
    public ValidateTokenResponseDTO validarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("El token es obligatorio");
        }

        String normalizedToken = normalizeBearerToken(token);
        if (!jwtService.esTokenValido(normalizedToken)) {
            return new ValidateTokenResponseDTO(false, null);
        }

        return new ValidateTokenResponseDTO(true, jwtService.extraerUsername(normalizedToken));
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
