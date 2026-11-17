package ec.consultas.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuración tipada (prefijo {@code app}). Los valores por defecto viven en application.yml. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull @Valid Sri sri,
        @NotNull @Valid Ant ant,
        @NotNull Cors cors) {

    public record Sri(@NotBlank String baseUrl, @Positive int timeoutMs) {
    }

    public record Ant(
            @NotBlank String baseUrl,
            @Positive int timeoutMs,
            boolean serveFreshCache,
            @NotNull Duration freshTtl,
            @NotNull Duration backupTtl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
