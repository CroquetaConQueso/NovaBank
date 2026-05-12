package com.novabank.auth.repository;

import com.novabank.auth.model.Usuario;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioRepository extends ReactiveCrudRepository<Usuario, Long> {

    Mono<Usuario> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);
}
