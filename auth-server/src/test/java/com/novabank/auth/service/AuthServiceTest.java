package com.novabank.auth.service;

import com.novabank.auth.dto.LoginRequestDTO;
import com.novabank.auth.dto.LoginResponseDTO;
import com.novabank.auth.dto.RegisterRequestDTO;
import com.novabank.auth.dto.RegisterResponseDTO;
import com.novabank.auth.dto.ValidateTokenResponseDTO;
import com.novabank.auth.exception.DuplicateUserException;
import com.novabank.auth.exception.InvalidCredentialsException;
import com.novabank.auth.model.Usuario;
import com.novabank.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService("test-auth-server-secret-key-with-at-least-32-chars", 86400000L);
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void registrarGuardaPasswordConBCryptYNormalizaUsername() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponseDTO response = authService.registrar(new RegisterRequestDTO(" Ana ", "password123"));

        assertThat(response.username()).isEqualTo("ana");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrarUsuarioDuplicadoDevuelveConflicto() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(new RegisterRequestDTO("ana", "password123")))
                .isInstanceOf(DuplicateUserException.class);
    }

    @Test
    void loginCorrectoDevuelveBearerToken() {
        Usuario usuario = usuario("ana", passwordEncoder.encode("password123"), true);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        LoginResponseDTO response = authService.login(new LoginRequestDTO("ana", "password123"));

        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.token()).isNotBlank();
        assertThat(jwtService.esTokenValido(response.token())).isTrue();
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() {
        Usuario usuario = usuario("ana", passwordEncoder.encode("password123"), true);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("ana", "bad-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void tokenValidoDevuelveUsername() {
        String token = jwtService.generarToken("ana");

        ValidateTokenResponseDTO response = authService.validarToken(token);

        assertThat(response.valido()).isTrue();
        assertThat(response.username()).isEqualTo("ana");
    }

    @Test
    void tokenInvalidoDevuelveValidoFalse() {
        ValidateTokenResponseDTO response = authService.validarToken("token-invalido");

        assertThat(response.valido()).isFalse();
        assertThat(response.username()).isNull();
    }

    private Usuario usuario(String username, String passwordHash, boolean enabled) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordHash);
        usuario.setRole("USER");
        usuario.setEnabled(enabled);
        return usuario;
    }
}
