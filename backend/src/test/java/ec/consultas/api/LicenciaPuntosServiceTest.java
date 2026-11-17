package ec.consultas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.consultas.api.cache.PuntosCacheEntry;
import ec.consultas.api.cache.PuntosStore;
import ec.consultas.api.config.AppProperties;
import ec.consultas.api.dto.PuntosLicenciaResponse;
import ec.consultas.api.exception.AntUnavailableException;
import ec.consultas.api.exception.ApiException;
import ec.consultas.api.gateway.AntGateway;
import ec.consultas.api.gateway.AntGateway.AntPuntos;
import ec.consultas.api.service.LicenciaPuntosService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LicenciaPuntosServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-10T15:30:00Z");

    /** Almacén en memoria; "fresh" y "backup" separados como en Redis. */
    static class FakeStore implements PuntosStore {
        final Map<String, PuntosCacheEntry> fresh = new HashMap<>();
        final Map<String, PuntosCacheEntry> backup = new HashMap<>();

        public Optional<PuntosCacheEntry> getFresh(String c) { return Optional.ofNullable(fresh.get(c)); }
        public Optional<PuntosCacheEntry> getBackup(String c) { return Optional.ofNullable(backup.get(c)); }
        public void save(PuntosCacheEntry e) { fresh.put(e.cedula(), e); backup.put(e.cedula(), e); }
    }

    private static AppProperties props(boolean serveFresh) {
        return new AppProperties(
                new AppProperties.Sri("http://sri", 1000),
                new AppProperties.Ant("http://ant", 1000, serveFresh, Duration.ofHours(1), Duration.ofDays(7)),
                new AppProperties.Cors(java.util.List.of()));
    }

    private static LicenciaPuntosService service(AntGateway ant, FakeStore store, Instant now, boolean serveFresh) {
        return new LicenciaPuntosService(ant, store, Clock.fixed(now, ZoneOffset.UTC), props(serveFresh));
    }

    private static final AntGateway ANT_CAIDA = (c, p) -> { throw new AntUnavailableException("timeout"); };

    @Test
    void antOkDevuelveEnVivoYGuardaAmbasClaves() {
        FakeStore store = new FakeStore();
        PuntosLicenciaResponse r = service((c, p) -> new AntPuntos("JUAN", 28), store, T0, false)
                .consultar("1712345678", null);

        assertThat(r.origen()).isEqualTo("ANT_EN_VIVO");
        assertThat(r.esDatoReciente()).isTrue();
        assertThat(r.puntos()).isEqualTo(28);
        assertThat(r.mensaje()).isNull();
        assertThat(store.fresh).containsKey("1712345678");
        assertThat(store.backup).containsKey("1712345678");
    }

    @Test
    void antCaidaConRespaldoDevuelve200ConAdvertencia() {
        FakeStore store = new FakeStore();
        store.save(new PuntosCacheEntry("1712345678", "JUAN", 26, T0.minus(Duration.ofDays(3)), "ANT_EN_VIVO"));
        store.fresh.clear(); // la clave de 1 h ya expiró; el respaldo de 7 d sigue

        PuntosLicenciaResponse r = service(ANT_CAIDA, store, T0, false).consultar("1712345678", null);

        assertThat(r.origen()).isEqualTo("CACHE_RESPALDO");
        assertThat(r.esDatoReciente()).isFalse();
        assertThat(r.puntos()).isEqualTo(26);
        assertThat(r.fechaConsultaOriginal()).isEqualTo(T0.minus(Duration.ofDays(3)));
        assertThat(r.mensaje()).startsWith("El servicio de la ANT no está disponible en este momento. "
                + "Mostrando el último registro guardado del 07/11/2026");
    }

    @Test
    void antCaidaSinRespaldoDevuelve503() {
        assertThatThrownBy(() -> service(ANT_CAIDA, new FakeStore(), T0, false).consultar("1712345678", null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getCode()).isEqualTo("ANT_UNAVAILABLE");
                });
    }

    @Test
    void conServeFreshCacheNoLlamaALaAntSiHayDatoFresco() {
        FakeStore store = new FakeStore();
        store.save(new PuntosCacheEntry("1712345678", "JUAN", 30, T0.minusSeconds(600), "ANT_EN_VIVO"));

        PuntosLicenciaResponse r = service(ANT_CAIDA, store, T0, true).consultar("1712345678", null);

        assertThat(r.origen()).isEqualTo("CACHE_FRESCA");
        assertThat(r.esDatoReciente()).isTrue();
    }

    @Test
    void cedulaInvalidaDevuelve400() {
        assertThatThrownBy(() -> service(ANT_CAIDA, new FakeStore(), T0, false).consultar("abc", null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
