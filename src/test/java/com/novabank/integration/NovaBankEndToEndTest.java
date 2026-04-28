package com.novabank.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.dto.ClienteRequestDTO;
import com.novabank.dto.CuentaCreateRequestDTO;
import com.novabank.dto.LoginRequestDTO;
import com.novabank.dto.OperacionRequestDTO;
import com.novabank.dto.TransferenciaRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NovaBankEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void flujoBancarioAutenticadoCompleto() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());

        JsonNode clienteOrigen = crearCliente(
                token,
                new ClienteRequestDTO(
                        "Ana",
                        "Garcia Lopez",
                        "12345678Z",
                        "ana.e2e@example.com",
                        "600111222"
                )
        );
        JsonNode clienteDestino = crearCliente(
                token,
                new ClienteRequestDTO(
                        "Luis",
                        "Perez Martin",
                        "87654321X",
                        "luis.e2e@example.com",
                        "600333444"
                )
        );

        JsonNode cuentaOrigen = crearCuenta(token, clienteOrigen.get("id").asLong());
        JsonNode cuentaDestino = crearCuenta(token, clienteDestino.get("id").asLong());

        long cuentaOrigenId = cuentaOrigen.get("id").asLong();
        String numeroOrigen = cuentaOrigen.get("numeroCuenta").asText();
        String numeroDestino = cuentaDestino.get("numeroCuenta").asText();
        assertThat(numeroOrigen).startsWith("ES91210000");
        assertThat(numeroDestino).startsWith("ES91210000");

        mockMvc.perform(post("/api/operaciones/deposito")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(numeroOrigen, new BigDecimal("250.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("DEPOSITO"));

        mockMvc.perform(post("/api/operaciones/retiro")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(numeroOrigen, new BigDecimal("40.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("RETIRO"));

        mockMvc.perform(post("/api/operaciones/transferencia")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferenciaRequestDTO(numeroOrigen, numeroDestino, new BigDecimal("60.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("TRANSFERENCIA_SALIENTE"))
                .andExpect(jsonPath("$[1].tipo").value("TRANSFERENCIA_ENTRANTE"));

        mockMvc.perform(post("/api/operaciones/retiro")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperacionRequestDTO(numeroOrigen, new BigDecimal("1000.00"))
                        )))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SALDO_INSUFICIENTE"))
                .andExpect(jsonPath("$.message").value(
                        "Saldo insuficiente. Saldo disponible: 150.00 EUR. Importe solicitado: 1000.00 EUR."
                ));

        JsonNode cuentaActualizada = getJson("/api/cuentas/" + cuentaOrigenId, token);
        assertThat(cuentaActualizada.get("saldo").decimalValue()).isEqualByComparingTo("150.00");

        mockMvc.perform(get("/api/cuentas/{id}/movimientos", cuentaOrigenId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.tipo == 'DEPOSITO')]").exists())
                .andExpect(jsonPath("$[?(@.tipo == 'RETIRO')]").exists())
                .andExpect(jsonPath("$[?(@.tipo == 'TRANSFERENCIA_SALIENTE')]").exists());

        LocalDate inicio = LocalDate.now().minusDays(7);
        LocalDate fin = LocalDate.now().plusDays(7);

        mockMvc.perform(get("/api/cuentas/{id}/movimientos", cuentaOrigenId)
                        .param("fechaInicio", inicio.toString())
                        .param("fechaFin", fin.toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("admin", "password"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private JsonNode crearCliente(String token, ClienteRequestDTO request) throws Exception {
        String response = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode crearCuenta(String token, Long clienteId) throws Exception {
        String response = mockMvc.perform(post("/api/cuentas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CuentaCreateRequestDTO(clienteId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String response = mockMvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
