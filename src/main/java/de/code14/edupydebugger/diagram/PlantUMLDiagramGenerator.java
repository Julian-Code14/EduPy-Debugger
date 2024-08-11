package de.code14.edupydebugger.diagram;

import com.intellij.openapi.diagnostic.Logger;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
     * Generates a PlantUML diagram and saves it to the specified file path.
     * <p>
     * This method creates a PlantUML diagram from the provided PlantUML source string and writes the resulting image
     * to the specified file path. If the directory for the file does not exist, it will be created.
     * </p>
     *
     * @param plantUmlSource   the PlantUML source code as a string
     * @param outputFilePath   the path where the output image file should be saved
     * @throws IOException     if an I/O error occurs during diagram generation or file writing
     */
    public static void generateDiagram(String plantUmlSource, String outputFilePath) throws IOException {
        File outputFile = new File(outputFilePath);

        SourceStringReader reader = new SourceStringReader(plantUmlSource);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Write the first image to output stream
            reader.outputImage(baos);

            // Ensure the directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Save the diagram to a file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                baos.writeTo(fos);
            }
        }
    }

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
