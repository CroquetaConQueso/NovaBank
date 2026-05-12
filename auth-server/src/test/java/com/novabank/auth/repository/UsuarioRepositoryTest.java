package com.novabank.auth.repository;

import com.novabank.auth.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
    }

    @Test
    void buscaUsuarioPorUsername() {
        StepVerifier.create(repository.save(usuario("ana"))
                        .then(repository.findByUsername("ana")))
                .assertNext(usuario -> assertThat(usuario.getRole()).isEqualTo("USER"))
                .verifyComplete();
    }

    @Test
    void usernameDebeSerUnico() {
        StepVerifier.create(repository.save(usuario("ana"))
                        .then(repository.save(usuario("ana"))))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    private Usuario usuario(String username) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash("{bcrypt}hash");
        usuario.setRole("USER");
        usuario.setEnabled(true);
        usuario.prepararParaCreacion();
        return usuario;
    }
}
