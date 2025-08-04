package de.code14.edupydebugger.diagram;

import org.junit.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.*;


public class PlantUMLDiagramGeneratorTests {

    @Test
    public void testGenerateDiagramAsBase64ProducesValidSvg() throws Exception {
        String src = """
                @startuml
                class Foo
                @enduml
                """;

        String base64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(src);
        assertNotNull(base64);

        byte[] decoded = Base64.getDecoder().decode(base64);
        String svg     = new String(decoded, StandardCharsets.UTF_8);

        assertTrue("SVG muss <svg …> enthalten", svg.contains("<svg"));
        assertTrue(svg.contains("Foo"));
    }
}