package ec.consultas.api;

import static org.assertj.core.api.Assertions.assertThat;

import ec.consultas.api.gateway.AntGateway.AntPuntos;
import ec.consultas.api.parser.AntHtmlParser;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AntHtmlParserTest {

    @Test
    void extraePuntosYNombreDeTablaEtiquetaValor() {
        String html = "<html><body><table>"
                + "<tr><td>Nombres:</td><td>JUAN CARLOS PÉREZ</td></tr>"
                + "<tr><td>Puntos</td><td>27</td></tr>"
                + "</table></body></html>";
        Optional<AntPuntos> r = AntHtmlParser.parse(html);
        assertThat(r).isPresent();
        assertThat(r.get().puntos()).isEqualTo(27);
        assertThat(r.get().nombreConductor()).isEqualTo("JUAN CARLOS PÉREZ");
    }

    @Test
    void extraeEtiquetaYValorEnLaMismaCelda() {
        String html = "<div><span>Apellidos y Nombres: MARIA LOPEZ</span><span>Puntos en la licencia: 30</span></div>";
        Optional<AntPuntos> r = AntHtmlParser.parse(html);
        assertThat(r).isPresent();
        assertThat(r.get().puntos()).isEqualTo(30);
        assertThat(r.get().nombreConductor()).isEqualTo("MARIA LOPEZ");
    }

    @Test
    void extraeValorEnTextoSueltoTrasEtiquetaEnNegrita() {
        String html = "<table><tr><td><b>Puntos:</b> 24</td></tr><tr><td><b>Nombres:</b> ANA RUIZ</td></tr></table>";
        Optional<AntPuntos> r = AntHtmlParser.parse(html);
        assertThat(r).isPresent();
        assertThat(r.get().puntos()).isEqualTo(24);
        assertThat(r.get().nombreConductor()).isEqualTo("ANA RUIZ");
    }

    @Test
    void respaldoPorTextoPlanoCuandoNoHayEtiqueta() {
        Optional<AntPuntos> r = AntHtmlParser.parse("<html><body><p>Resumen: puntos disponibles 22 de 30</p></body></html>");
        assertThat(r).isPresent();
        assertThat(r.get().puntos()).isEqualTo(22);
    }

    @Test
    void noInventaDatosSiElTextoNoSigueNingunPatron() {
        assertThat(AntHtmlParser.parse("<html><body><p>Su licencia tiene 18 puntos vigentes</p></body></html>")).isEmpty();
    }

    @Test
    void htmlVacioOSinPuntosDevuelveVacio() {
        assertThat(AntHtmlParser.parse("")).isEmpty();
        assertThat(AntHtmlParser.parse("   ")).isEmpty();
        assertThat(AntHtmlParser.parse("<html><body></body></html>")).isEmpty();
        assertThat(AntHtmlParser.parse("<html><body><p>Servicio en mantenimiento</p></body></html>")).isEmpty();
    }

    @Test
    void ignoraNumerosFueraDeRango() {
        assertThat(AntHtmlParser.parse("<table><tr><td>Puntos</td><td>150</td></tr></table>")).isEmpty();
    }
}
