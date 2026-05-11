package com.novabank.cliente.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("clientes")
public class Cliente {

    @Id
    private Long id;

    @Column("nombre")
    private String nombre;

    @Column("apellidos")
    private String apellidos;

    @Column("dni")
    private String dni;

    @Column("email")
    private String email;

    @Column("telefono")
    private String telefono;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    public void prepararParaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
