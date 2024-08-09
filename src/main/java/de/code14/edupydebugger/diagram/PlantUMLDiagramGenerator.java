package de.code14.edupydebugger.diagram;

import com.intellij.openapi.diagnostic.Logger;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * @author julian
 * @version 1.0
 * @since 06.07.24
 */
public class PlantUMLDiagramGenerator {

    private final static Logger LOGGER = Logger.getInstance(PlantUMLDiagramGenerator.class);


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
