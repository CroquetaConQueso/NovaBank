package com.novabank.documento;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureBoundaryTest {

    @Test
    void domainYApplicationNoDependenDeAdaptadoresNiInfraestructuraSpring() throws IOException {
        Path base = Path.of("src/main/java/com/novabank/documento");
        List<Path> files = Files.walk(base)
                .filter(path -> path.toString().contains("\\domain\\")
                        || path.toString().contains("\\application\\")
                        || path.toString().contains("/domain/")
                        || path.toString().contains("/application/"))
                .filter(path -> path.toString().endsWith(".java"))
                .toList();

        for (Path file : files) {
            String content = Files.readString(file);
            assertThat(content)
                    .as(file.toString())
                    .doesNotContain(
                            "org.springframework.web",
                            "org.springframework.cloud.stream",
                            "org.springframework.messaging",
                            "org.springframework.kafka",
                            "software.amazon.awssdk",
                            "S3AsyncClient",
                            "LambdaAsyncClient"
                    );
        }
    }
}
