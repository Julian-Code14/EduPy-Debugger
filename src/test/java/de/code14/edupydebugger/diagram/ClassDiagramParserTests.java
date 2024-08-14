package de.code14.edupydebugger.diagram;

import com.intellij.openapi.project.Project;
import de.code14.edupydebugger.analysis.staticanalysis.ClassInfo;
import de.code14.edupydebugger.analysis.staticanalysis.PythonAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class ClassDiagramParserTests {

    @Mock
    private PythonAnalyzer mockPythonAnalyzer;

    @Mock
    private Project mockProject;

    private ClassDiagramParser classDiagramParser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        classDiagramParser = new ClassDiagramParser(mockPythonAnalyzer);
    }

    @Test
    void testGenerateClassDiagramWithNoClasses() {
        // Arrange
        when(mockPythonAnalyzer.getClassDetails()).thenReturn(Collections.emptyMap());

        // Act
        String result = classDiagramParser.generateClassDiagram(mockProject);

        // Assert
        verify(mockPythonAnalyzer, times(1)).analyzePythonFiles(mockProject);
        assertTrue(result.contains("@startuml"));
        assertTrue(result.contains("@enduml"));
    }

    @Test
    void testGenerateClassDiagramWithSingleClass() {
        // Arrange
        Map<String, ClassInfo> classDetails = new HashMap<>();
        classDetails.put("TestClass", new ClassInfo(
                List.of("attribute1", "attribute2"),
                List.of("method1()", "method2()"),
                List.of("OtherClass"),
                List.of("SuperClass")
        ));
        when(mockPythonAnalyzer.getClassDetails()).thenReturn(classDetails);

        // Act
        String result = classDiagramParser.generateClassDiagram(mockProject);

        // Assert
        verify(mockPythonAnalyzer, times(1)).analyzePythonFiles(mockProject);
        assertTrue(result.contains("class TestClass"));
        assertTrue(result.contains("attribute1"));
        assertTrue(result.contains("method1()"));
        assertTrue(result.contains("TestClass ..> OtherClass"));
        assertTrue(result.contains("TestClass --|> SuperClass"));
    }

    @Test
    void testGenerateClassDiagramWithAbstractClass() {
        // Arrange
        Map<String, ClassInfo> classDetails = new HashMap<>();
        classDetails.put("AbstractClass", new ClassInfo(
                List.of(),
                List.of(),
                List.of(),
                List.of("ABC")
        ));
        when(mockPythonAnalyzer.getClassDetails()).thenReturn(classDetails);

        // Act
        String result = classDiagramParser.generateClassDiagram(mockProject);

        // Assert
        verify(mockPythonAnalyzer, times(1)).analyzePythonFiles(mockProject);
        assertTrue(result.contains("abstract class AbstractClass"));
    }

}
