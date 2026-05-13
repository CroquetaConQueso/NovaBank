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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
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
        when(usuarioRepository.existsByUsername("ana")).thenReturn(Mono.just(false));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(authService.registrar(new RegisterRequestDTO(" Ana ", "password123")))
                .assertNext(response -> assertThat(response.username()).isEqualTo("ana"))
                .verifyComplete();

        var captor = forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", guardado.getPasswordHash())).isTrue();
    }

    @Test
    void registrarUsuarioDuplicadoDevuelveConflicto() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.registrar(new RegisterRequestDTO("ana", "password123")))
                .expectError(DuplicateUserException.class)
                .verify();
    }

    @Test
    void registrarConvierteViolacionUniqueConcurrenteEnDuplicadoControlado() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(Mono.just(false));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("unique constraint")));

        StepVerifier.create(authService.registrar(new RegisterRequestDTO("ana", "password123")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DuplicateUserException.class);
                    assertThat(error).hasMessage("Ya existe un usuario con alguno de los datos unicos indicados");
                })
                .verify();
    }

    @Test
    void loginCorrectoDevuelveBearerToken() {
        Usuario usuario = usuario("ana", passwordEncoder.encode("password123"), true);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Mono.just(usuario));

        StepVerifier.create(authService.login(new LoginRequestDTO("ana", "password123")))
                .assertNext(response -> {
                    assertThat(response.tipo()).isEqualTo("Bearer");
                    assertThat(response.token()).isNotBlank();
                    assertThat(jwtService.esTokenValido(response.token())).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() {
        Usuario usuario = usuario("ana", passwordEncoder.encode("password123"), true);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Mono.just(usuario));

        StepVerifier.create(authService.login(new LoginRequestDTO("ana", "bad-password")))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void loginConUsuarioInexistenteDevuelve401() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Mono.empty());

        StepVerifier.create(authService.login(new LoginRequestDTO("nadie", "password123")))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void loginConUsuarioDeshabilitadoDevuelve401() {
        Usuario usuario = usuario("ana", passwordEncoder.encode("password123"), false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Mono.just(usuario));

        StepVerifier.create(authService.login(new LoginRequestDTO("ana", "password123")))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void tokenValidoDevuelveUsername() {
        String token = jwtService.generarToken("ana");

        StepVerifier.create(authService.validarToken(token))
                .assertNext(response -> {
                    assertThat(response.valido()).isTrue();
                    assertThat(response.username()).isEqualTo("ana");
                })
                .verifyComplete();
    }

    @Test
    void tokenInvalidoDevuelveValidoFalse() {
        StepVerifier.create(authService.validarToken("token-invalido"))
                .assertNext(response -> {
                    assertThat(response.valido()).isFalse();
                    assertThat(response.username()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void tokenConPrefijoBearerDevuelveUsername() {
        String token = jwtService.generarToken("ana");

        StepVerifier.create(authService.validarToken("Bearer " + token))
                .assertNext(response -> {
                    assertThat(response.valido()).isTrue();
                    assertThat(response.username()).isEqualTo("ana");
                })
                .verifyComplete();
    }

    @Test
    void tokenFirmadoConOtroSecretoDevuelveValidoFalse() {
        JwtService otroJwtService = new JwtService("other-auth-server-secret-key-with-at-least-32-chars", 86400000L);
        String token = otroJwtService.generarToken("ana");

        StepVerifier.create(authService.validarToken(token))
                .assertNext(response -> {
                    assertThat(response.valido()).isFalse();
                    assertThat(response.username()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void tokenExpiradoDevuelveValidoFalse() {
        JwtService jwtExpirado = new JwtService("test-auth-server-secret-key-with-at-least-32-chars", -1000L);
        String token = jwtExpirado.generarToken("ana");

        StepVerifier.create(authService.validarToken(token))
                .assertNext(response -> {
                    assertThat(response.valido()).isFalse();
                    assertThat(response.username()).isNull();
                })
                .verifyComplete();
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
