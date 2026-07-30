package com.novabank.operacion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaAsyncClientBuilder;

import java.net.URI;

@Configuration
public class LambdaConfig {

    @Bean
    public LambdaAsyncClient lambdaAsyncClient(
            @Value("${novabank.aws.region:eu-west-1}") String region,
            @Value("${novabank.aws.endpoint-override:}") String endpointOverride
    ) {
        LambdaAsyncClientBuilder builder = LambdaAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build());

        if (StringUtils.hasText(endpointOverride)) {
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }
}
