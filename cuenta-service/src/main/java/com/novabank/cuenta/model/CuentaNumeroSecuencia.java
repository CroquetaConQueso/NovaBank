package com.novabank.cuenta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("account_number_sequence")
public class CuentaNumeroSecuencia {

    @Id
    private Long id;

    @Column("next_value")
    private Long nextValue;
}
