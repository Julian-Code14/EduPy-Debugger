package de.code14.edupydebugger.core;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyThreadInfo;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class DebugSessionListenerTests {

    private DebugSessionListener debugSessionListener;

    @Mock
    private XDebugSession mockSession;

    @Mock
    private PyDebugProcess mockPyDebugProcess;

    @Mock
    private PyThreadInfo mockThreadInfo;

    @Mock
    private Project mockProject;

    @Before
    public void setUp() {
        // Initialize mocks annotated with @Mock
        MockitoAnnotations.openMocks(this);

        // Mock the behavior of the PyDebugProcess to return a mock XDebugSession
        when(mockPyDebugProcess.getSession()).thenReturn(mockSession);

        // Mock the behavior of getThreads() to return a list with a mock PyThreadInfo
        when(mockPyDebugProcess.getThreads()).thenReturn(Collections.singletonList(mockThreadInfo));

        // Mock the behavior of the Project to return a valid base path when getBasePath() is called
        when(mockProject.getBasePath()).thenReturn("/path/to/project");

        // Initialize the DebugSessionListener instance with the mocked PyDebugProcess
        debugSessionListener = new DebugSessionListener(mockPyDebugProcess);
    }

    // TODO: testStackFrameChangedCallsMethods

    @Test
    public void testGenerateAndSendDiagramHandlesObjectCards() throws Exception {
        // Arrange
        String mockDiagram = "mock diagram";
        String base64Diagram = "mockBase64";

        try (MockedStatic<PlantUMLDiagramGenerator> plantUMLDiagramGeneratorMock = mockStatic(PlantUMLDiagramGenerator.class);
             MockedStatic<DebugServerEndpoint> debugServerEndpointMock = mockStatic(DebugServerEndpoint.class)) {

            plantUMLDiagramGeneratorMock.when(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64(mockDiagram))
                    .thenReturn(base64Diagram);

            // Access the private method generateAndSendDiagram
            Method method = DebugSessionListener.class.getDeclaredMethod("generateAndSendDiagram", String.class, String.class);
            method.setAccessible(true);

            // Act: Call the private method via reflection
            method.invoke(debugSessionListener, mockDiagram, "objectCards");

            // Access the private static field OBJECT_CARDS_PREFIX
            Field field = DebugSessionListener.class.getDeclaredField("OBJECT_CARDS_PREFIX");
            field.setAccessible(true);
            String objectCardsPrefix = (String) field.get(null);

            // Assert: Verify the calls
            plantUMLDiagramGeneratorMock.verify(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64(mockDiagram), times(1));
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.setObjectCardsPlantUmlImage(base64Diagram), times(1));
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.sendDebugInfo(objectCardsPrefix + base64Diagram), times(1));
        }
    }

    @Test
    public void testGenerateAndSendDiagramHandlesObjectDiagrams() throws Exception {
        // Arrange
        String mockDiagram = "mock diagram";
        String base64Diagram = "mockBase64";

        try (MockedStatic<PlantUMLDiagramGenerator> plantUMLDiagramGeneratorMock = mockStatic(PlantUMLDiagramGenerator.class);
             MockedStatic<DebugServerEndpoint> debugServerEndpointMock = mockStatic(DebugServerEndpoint.class)) {

            plantUMLDiagramGeneratorMock.when(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64(mockDiagram))
                    .thenReturn(base64Diagram);

            // Access the private method generateAndSendDiagram
            Method method = DebugSessionListener.class.getDeclaredMethod("generateAndSendDiagram", String.class, String.class);
            method.setAccessible(true);

            // Act: Call the private method via reflection
            method.invoke(debugSessionListener, mockDiagram, "objectDiagram");

            // Access the private static field OBJECT_DIAGRAM_PREFIX
            Field field = DebugSessionListener.class.getDeclaredField("OBJECT_DIAGRAM_PREFIX");
            field.setAccessible(true);
            String objectDiagramPrefix = (String) field.get(null);

            // Assert: Verify the calls
            plantUMLDiagramGeneratorMock.verify(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64(mockDiagram), times(1));
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.setObjectDiagramPlantUmlImage(base64Diagram), times(1));
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.sendDebugInfo(objectDiagramPrefix + base64Diagram), times(1));
        }
    }

}
