package com.novabank.auth.repository;

import com.novabank.auth.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Test
    void buscaUsuarioPorUsername() {
        repository.save(usuario("ana"));

        assertThat(repository.findByUsername("ana"))
                .isPresent()
                .get()
                .extracting(Usuario::getRole)
                .isEqualTo("USER");
    }

    @Test
    void usernameDebeSerUnico() {
        repository.saveAndFlush(usuario("ana"));

        assertThatThrownBy(() -> repository.saveAndFlush(usuario("ana")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Usuario usuario(String username) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash("{bcrypt}hash");
        usuario.setRole("USER");
        usuario.setEnabled(true);
        return usuario;
    }
}
