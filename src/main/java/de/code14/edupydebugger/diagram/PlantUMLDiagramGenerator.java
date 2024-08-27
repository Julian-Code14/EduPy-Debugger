package de.code14.edupydebugger.diagram;

import com.intellij.openapi.diagnostic.Logger;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility class for generating PlantUML diagrams.
 * This class provides methods to generate diagrams from PlantUML source code and save them either as image files
 * or as Base64-encoded strings.
 * <p>
 * The diagrams can be generated and saved to a file or returned as a Base64 string for further processing or display.
 * </p>
 *
 * @author julian
 * @version 1.0
 * @since 06.07.24
 */
public class PlantUMLDiagramGenerator {

    private final static Logger LOGGER = Logger.getInstance(PlantUMLDiagramGenerator.class);


    /**
     * Generates a PlantUML diagram and returns it as a Base64-encoded string.
     * <p>
     * This method creates a PlantUML diagram from the provided PlantUML source string and encodes the resulting image
     * in Base64 format. The method also validates the generated Base64 string to ensure that it is correctly encoded.
     * </p>
     *
     * @param plantUmlSource   the PlantUML source code as a string
     * @return                 the Base64-encoded string of the generated diagram image
     * @throws IOException     if an error occurs during diagram generation, encoding, or validation
     */
    public static String generateDiagramAsBase64(String plantUmlSource) throws IOException {
        String base64EncodedImage;
        SourceStringReader reader = new SourceStringReader(plantUmlSource);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            reader.outputImage(baos);
            byte[] imageBytes = baos.toByteArray();
            base64EncodedImage = Base64.getEncoder().encodeToString(imageBytes);
            LOGGER.debug("Encoded diagram image (base64): " + base64EncodedImage);
        }
        // Validate Base64-String
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedImage);
            LOGGER.debug("Base64 decoded successfully. Base64 string validated.");
            return base64EncodedImage;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid Base64 encoding: " + e.getMessage());
            throw new IOException("Invalid Base64 encoding", e);
        }
    }

}
