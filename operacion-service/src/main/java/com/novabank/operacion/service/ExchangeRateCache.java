package com.novabank.operacion.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.novabank.operacion.dto.ExchangeRateResultDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Component
public class ExchangeRateCache {

    static final Duration TTL = Duration.ofMinutes(5);

    private final Cache<String, CachedExchangeRate> cache;

    public ExchangeRateCache() {
        this(Ticker.systemTicker());
    }

    ExchangeRateCache(Ticker ticker) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .ticker(ticker)
                .build();
    }

    public void guardar(String from, String to, BigDecimal tasa) {
        cache.put(key(from, to), new CachedExchangeRate(tasa, Instant.now()));
    }

    public Optional<ExchangeRateResultDTO> obtener(String from, String to) {
        return Optional.ofNullable(cache.getIfPresent(key(from, to)))
                .map(cached -> new ExchangeRateResultDTO(cached.tasa(), true, cached.timestamp()));
    }

    String key(String from, String to) {
        return normalizar(from) + "->" + normalizar(to);
    }

    private String normalizar(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record CachedExchangeRate(BigDecimal tasa, Instant timestamp) {
    }
}
