package de.code14.edupydebugger.server;

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
        SourceStringReader reader = new SourceStringReader(plantUmlSource);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            reader.outputImage(baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }

}
