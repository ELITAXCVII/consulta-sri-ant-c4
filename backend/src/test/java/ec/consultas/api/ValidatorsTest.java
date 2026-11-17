package ec.consultas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.consultas.api.exception.ApiException;
import ec.consultas.api.util.Validators;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ValidatorsTest {

    @Test
    void rucValido() {
        assertThat(Validators.ruc("1712345678001")).isEqualTo("1712345678001");
    }

    @Test
    void rucInvalidoDevuelve400() {
        for (String malo : new String[] {"1712345678002", "171234567800", "17123456780011", "17123ABC78001", "", null}) {
            assertThatThrownBy(() -> Validators.ruc(malo))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCode()).isEqualTo("INVALID_RUC");
                    });
        }
    }

    @Test
    void cedulaAceptaRucYTomaDiezDigitos() {
        assertThat(Validators.cedula("1712345678001")).isEqualTo("1712345678");
        assertThat(Validators.cedula("1712345678")).isEqualTo("1712345678");
        assertThatThrownBy(() -> Validators.cedula("12345")).isInstanceOf(ApiException.class);
    }

    @Test
    void placasValidasSeNormalizan() {
        assertThat(Validators.placa("abc1234")).isEqualTo("ABC1234");
        assertThat(Validators.placa("ABC-0123")).isEqualTo("ABC0123");
        assertThat(Validators.placa("ABC123")).isEqualTo("ABC123");
        assertThat(Validators.placa("AB123C")).isEqualTo("AB123C");
    }

    @Test
    void placasInvalidas() {
        for (String mala : new String[] {"AB1234", "ABCD123", "1234ABC", "ABC12", "ABC12345", ""}) {
            assertThatThrownBy(() -> Validators.placa(mala)).isInstanceOf(ApiException.class);
        }
    }

    @Test
    void placaOpcionalVaciaEsPermitida() {
        assertThat(Validators.placaOpcional(null)).isEmpty();
        assertThat(Validators.placaOpcional("  ")).isEmpty();
    }
}
