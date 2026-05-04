package com.novabank.cliente.repository;

import com.novabank.cliente.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDni(String dni);

    Optional<Cliente> findByEmail(String email);

    Optional<Cliente> findByTelefono(String telefono);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByTelefono(String telefono);

    @Query("""
           SELECT c
           FROM Cliente c
           WHERE c.dni = :dni
              OR c.email = :email
              OR c.telefono = :telefono
           """)
    List<Cliente> buscarDuplicados(
            @Param("dni") String dni,
            @Param("email") String email,
            @Param("telefono") String telefono
    );

    @Query("""
           SELECT c
           FROM Cliente c
           WHERE c.id <> :id
             AND (c.dni = :dni
              OR c.email = :email
              OR c.telefono = :telefono)
           """)
    List<Cliente> buscarDuplicadosExcluyendoId(
            @Param("id") Long id,
            @Param("dni") String dni,
            @Param("email") String email,
            @Param("telefono") String telefono
    );
}
