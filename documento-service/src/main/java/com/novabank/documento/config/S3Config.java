package com.novabank.documento.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3AsyncClient s3AsyncClient(S3Properties properties) {
        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (hasText(properties.getAws().getEndpointOverride())) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()))
                    .forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties properties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (hasText(properties.getAws().getEndpointOverride())) {
            builder.endpointOverride(URI.create(properties.getAws().getEndpointOverride()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        return builder.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
