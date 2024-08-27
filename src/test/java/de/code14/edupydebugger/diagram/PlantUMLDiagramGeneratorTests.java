package de.code14.edupydebugger.diagram;

import net.sourceforge.plantuml.SourceStringReader;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class PlantUMLDiagramGeneratorTests {

    @Test
    public void testGenerateDiagramAsBase64_ValidSource() throws IOException {
        // Arrange
        String plantUmlSource = "@startuml\nAlice -> Bob: Hello\n@enduml";
        String expectedBase64Prefix = "iVBORw0KGgo";  // The expected prefix for a PNG file encoded in Base64

        // Act
        String base64String = PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUmlSource);

        // Assert
        assertNotNull(base64String);
        assertTrue(base64String.startsWith(expectedBase64Prefix));  // Ensure the string looks like a valid PNG file
    }

    // TODO: Make the tests work for invalidBase64 and when throwing an IOException
    /*@Test
    public void testGenerateDiagramAsBase64_InvalidBase64() throws IOException {
        // Arrange
        String plantUmlSource = "@startuml\nAlice -> Bob: Hello\n@enduml";

        try (MockedStatic<Base64> base64Mock = Mockito.mockStatic(Base64.class);
             ByteArrayOutputStream baos = mock(ByteArrayOutputStream.class)) {

            // Stub für den Base64-Encoder, der einen ungültigen Base64-String zurückgibt
            base64Mock.when(() -> Base64.getEncoder().encodeToString(any(byte[].class)))
                    .thenReturn("InvalidBase64String!");

            // Simuliere die Generierung eines Bildes als Byte-Array
            when(baos.toByteArray()).thenReturn(new byte[]{});

            // Act & Assert
            assertThrows(IOException.class, () -> {
                PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUmlSource);
            });
        }
    }

    @Test
    public void testGenerateDiagramAsBase64_IOExceptionOnGeneration() throws IOException {
        // Arrange
        String plantUmlSource = "@startuml\nAlice -> Bob: Hello\n@enduml";

        try (MockedStatic<SourceStringReader> readerMock = mockStatic(SourceStringReader.class)) {

            SourceStringReader mockReader = mock(SourceStringReader.class);
            readerMock.when(() -> new SourceStringReader(plantUmlSource)).thenReturn(mockReader);

            // Verwende doThrow, um eine IOException zu simulieren
            doThrow(new IOException("IO error")).when(mockReader).outputImage(any(ByteArrayOutputStream.class));

            // Act & Assert
            assertThrows(IOException.class, () -> PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUmlSource));
        }
    }*/

}
