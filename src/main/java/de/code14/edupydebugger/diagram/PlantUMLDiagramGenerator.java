package de.code14.edupydebugger.diagram;

import com.intellij.openapi.diagnostic.Logger;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for generating PlantUML diagrams.
 * This class provides methods to generate diagrams from PlantUML source code and return them as Base64-encoded strings.
 * <p>
 * The diagrams are generated in SVG format and returned as Base64-encoded strings for further processing or display.
 * The class ensures that the generated Base64 string is correctly encoded and validated.
 * </p>
 *
 * <p>
 * The diagrams are encoded in UTF-8 format before being converted into Base64 strings.
 * </p>
 *
 * @author julian
 * @version 0.2.0
 * @since 0.1.0
 */
public class PlantUMLDiagramGenerator {

    private final static Logger LOGGER = Logger.getInstance(PlantUMLDiagramGenerator.class);

    /**
     * Generates a PlantUML diagram and returns it as a Base64-encoded string in SVG format.
     * <p>
     * This method creates a PlantUML diagram from the provided PlantUML source string and encodes the resulting image
     * in SVG format. The resulting SVG is then encoded in Base64 format. The method also validates the generated Base64
     * string to ensure that it is correctly encoded.
     * </p>
     *
     * <p>
     * The generated SVG diagram is first encoded in UTF-8 before being converted to a Base64 string.
     * </p>
     *
     * @param plantUmlSource the PlantUML source code as a string
     * @return the Base64-encoded string of the generated SVG diagram
     * @throws IOException if an error occurs during diagram generation, encoding, or validation
     */
    public static String generateDiagramAsBase64(String plantUmlSource) throws IOException {
        String base64EncodedSvg;
        SourceStringReader reader = new SourceStringReader(plantUmlSource);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Generate the diagram in SVG format
            String desc = reader.generateImage(baos, new FileFormatOption(FileFormat.SVG));
            if (desc == null) {
                throw new IOException("Error generating SVG diagram.");
            }
            byte[] svgBytes = baos.toString(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
            base64EncodedSvg = Base64.getEncoder().encodeToString(svgBytes);
            LOGGER.debug("Encoded diagram (SVG, base64): " + base64EncodedSvg);
        }
        // Validate Base64-String
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedSvg);
            LOGGER.debug("Base64 decoded successfully. Base64 string validated.");
            return base64EncodedSvg;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid Base64 encoding: " + e.getMessage());
            throw new IOException("Invalid Base64 encoding", e);
        }
    }

}
