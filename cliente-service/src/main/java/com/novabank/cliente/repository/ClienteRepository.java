package com.novabank.cliente.repository;

import com.novabank.cliente.model.Cliente;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClienteRepository extends ReactiveCrudRepository<Cliente, Long> {

    Mono<Cliente> findByDni(String dni);

    Mono<Cliente> findByEmail(String email);

    Mono<Cliente> findByTelefono(String telefono);

    Mono<Boolean> existsByDni(String dni);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByTelefono(String telefono);

    @Query("""
           SELECT id, nombre, apellidos, dni, email, telefono, fecha_creacion
           FROM clientes
           WHERE dni = :dni
              OR email = :email
              OR telefono = :telefono
           """)
    Flux<Cliente> buscarDuplicados(
            @Param("dni") String dni,
            @Param("email") String email,
            @Param("telefono") String telefono
    );

    @Query("""
           SELECT id, nombre, apellidos, dni, email, telefono, fecha_creacion
           FROM clientes
           WHERE id <> :id
             AND (dni = :dni
              OR email = :email
              OR telefono = :telefono)
           """)
    Flux<Cliente> buscarDuplicadosExcluyendoId(
            @Param("id") Long id,
            @Param("dni") String dni,
            @Param("email") String email,
            @Param("telefono") String telefono
    );
}
