package com.novabank.operacion.repository;

import com.novabank.operacion.model.EstadoOperacion;
import com.novabank.operacion.model.OperacionIdempotente;
import com.novabank.operacion.model.TipoOperacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OperacionIdempotenteRepositoryTest {

    @Autowired
    private OperacionIdempotenteRepository repository;

    @Test
    void guardaYBuscaPorIdempotencyKey() {
        repository.save(operacion("deposito-key", "hash-1"));

        assertThat(repository.findByIdempotencyKey("deposito-key"))
                .isPresent()
                .get()
                .extracting(OperacionIdempotente::getTipoOperacion)
                .isEqualTo(TipoOperacion.DEPOSITO);
    }

    @Test
    void idempotencyKeyDebeSerUnica() {
        repository.saveAndFlush(operacion("same-key", "hash-1"));

        assertThatThrownBy(() -> repository.saveAndFlush(operacion("same-key", "hash-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private OperacionIdempotente operacion(String key, String hash) {
        OperacionIdempotente operacion = new OperacionIdempotente();
        operacion.setIdempotencyKey(key);
        operacion.setRequestHash(hash);
        operacion.setTipoOperacion(TipoOperacion.DEPOSITO);
        operacion.setEstado(EstadoOperacion.EN_PROCESO);
        operacion.setCuentaOrigen(1L);
        operacion.setImporte(new BigDecimal("50.00"));
        return operacion;
    }
}
