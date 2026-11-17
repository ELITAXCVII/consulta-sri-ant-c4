package ec.consultas.api.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.consultas.api.config.AppProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPuntosStore implements PuntosStore {

    private static final Logger log = LoggerFactory.getLogger(RedisPuntosStore.class);

    static final String FRESH_PREFIX = "ant:puntos:";
    static final String BACKUP_PREFIX = "ant:backup:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final AppProperties.Ant config;

    public RedisPuntosStore(StringRedisTemplate redis, ObjectMapper mapper, AppProperties props) {
        this.redis = redis;
        this.mapper = mapper;
        this.config = props.ant();
    }

    @Override
    public Optional<PuntosCacheEntry> getFresh(String cedula) {
        return read(FRESH_PREFIX + cedula);
    }

    @Override
    public Optional<PuntosCacheEntry> getBackup(String cedula) {
        return read(BACKUP_PREFIX + cedula);
    }

    @Override
    public void save(PuntosCacheEntry entry) {
        try {
            String json = mapper.writeValueAsString(entry);
            redis.opsForValue().set(FRESH_PREFIX + entry.cedula(), json, config.freshTtl());
            // Cada consulta exitosa renueva el respaldo: dura 7 días desde la ÚLTIMA vez que la ANT respondió bien.
            redis.opsForValue().set(BACKUP_PREFIX + entry.cedula(), json, config.backupTtl());
        } catch (Exception e) {
            log.warn("No se pudo guardar en Redis (se continúa sin caché): {}", e.toString());
        }
    }

    private Optional<PuntosCacheEntry> read(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(mapper.readValue(json, PuntosCacheEntry.class));
        } catch (Exception e) {
            log.warn("No se pudo leer {} de Redis: {}", key, e.toString());
            return Optional.empty();
        }
    }
}
