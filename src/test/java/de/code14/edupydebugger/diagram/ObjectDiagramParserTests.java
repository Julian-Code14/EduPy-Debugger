package de.code14.edupydebugger.diagram;

import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ObjectDiagramParserTests {

    @Test
    public void testGenerateObjectCardsWithNoObjects() {
        // Arrange
        Map<String, ObjectInfo> emptyObjects = Collections.emptyMap();

        // Act
        Map<String, String> result = ObjectDiagramParser.generateObjectCards(emptyObjects);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGenerateObjectCardsWithSingleObject() {
        // Arrange
        AttributeInfo attribute1 = mock(AttributeInfo.class);
        when(attribute1.name()).thenReturn("attribute1");
        when(attribute1.value()).thenReturn("value1");
        when(attribute1.visibility()).thenReturn("public");

        ObjectInfo objectInfo = mock(ObjectInfo.class);
        when(objectInfo.attributes()).thenReturn(List.of(attribute1));
        when(objectInfo.references()).thenReturn(List.of("TestObject"));

        Map<String, ObjectInfo> objects = Map.of("1", objectInfo);

        // Act
        Map<String, String> result = ObjectDiagramParser.generateObjectCards(objects);

        // Assert
        assertTrue(result.containsKey("1"));
        String plantUml = result.get("1");
        assertTrue(plantUml.contains("object \"TestObject\" as o1"));
        assertTrue(plantUml.contains("attribute1 = value1"));
    }

    @Test
    public void testGenerateObjectCardsWithRefidLink() {
        // Arrange
        AttributeInfo attribute1 = mock(AttributeInfo.class);
        when(attribute1.name()).thenReturn("attribute1");
        when(attribute1.value()).thenReturn("refid:2");
        when(attribute1.visibility()).thenReturn("public");

        ObjectInfo objectInfo = mock(ObjectInfo.class);
        when(objectInfo.attributes()).thenReturn(List.of(attribute1));
        when(objectInfo.references()).thenReturn(List.of("TestObjectWithRef"));

        Map<String, ObjectInfo> objects = Map.of("1", objectInfo);

        // Act
        Map<String, String> result = ObjectDiagramParser.generateObjectCards(objects);

        // Assert
        assertTrue(result.containsKey("1"));
        String plantUml = result.get("1");
        assertTrue(plantUml.contains("object \"TestObjectWithRef\" as o1"));
        assertTrue(plantUml.contains("attribute1 = 2 [[[localhost:8026/2]]]"));
    }

    @Test
    public void testGenerateObjectDiagramWithNoObjects() {
        // Arrange
        Map<String, ObjectInfo> emptyObjects = Collections.emptyMap();

        // Act
        String result = ObjectDiagramParser.generateObjectDiagram(emptyObjects);

        // Assert
        assertTrue(result.contains("@startuml"));
        assertTrue(result.contains("@enduml"));
    }

    @Test
    public void testGenerateObjectDiagramWithAssociations() {
        // Arrange
        AttributeInfo attribute1 = mock(AttributeInfo.class);
        when(attribute1.name()).thenReturn("attribute1");
        when(attribute1.value()).thenReturn("refid:2");
        when(attribute1.visibility()).thenReturn("public");

        ObjectInfo objectInfo1 = mock(ObjectInfo.class);
        when(objectInfo1.attributes()).thenReturn(List.of(attribute1));
        when(objectInfo1.references()).thenReturn(List.of("Object1"));

        ObjectInfo objectInfo2 = mock(ObjectInfo.class);
        when(objectInfo2.attributes()).thenReturn(Collections.emptyList());
        when(objectInfo2.references()).thenReturn(List.of("Object2"));

        Map<String, ObjectInfo> objects = Map.of(
                "1", objectInfo1,
                "2", objectInfo2
        );

        // Act
        String result = ObjectDiagramParser.generateObjectDiagram(objects);

        // Assert
        assertTrue(result.contains("object \"Object1\" as o1"));
        assertTrue(result.contains("object \"Object2\" as o2"));
        assertTrue(result.contains("o1 --> o2"));
    }
}
