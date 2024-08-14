package de.code14.edupydebugger.analysis.dynamicanalysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class ObjectInfoTests {

    @Test
    public void testObjectInfoInitialization() {
        // Test data
        List<String> references = List.of("ref1", "ref2");
        List<AttributeInfo> attributes = List.of(
                new AttributeInfo("attr1", "String", "value1", "public"),
                new AttributeInfo("attr2", "int", "42", "private")
        );

        // Create ObjectInfo instance
        ObjectInfo objectInfo = new ObjectInfo(references, attributes);

        // Validate initialization
        assertNotNull(objectInfo.references(), "References should not be null");
        assertNotNull(objectInfo.attributes(), "Attributes should not be null");

        assertEquals(2, objectInfo.references().size(), "References size should be 2");
        assertEquals(2, objectInfo.attributes().size(), "Attributes size should be 2");

        assertEquals("ref1", objectInfo.references().get(0), "First reference should be 'ref1'");
        assertEquals("ref2", objectInfo.references().get(1), "Second reference should be 'ref2'");

        assertEquals("attr1", objectInfo.attributes().get(0).name(), "First attribute name should be 'attr1'");
        assertEquals("value1", objectInfo.attributes().get(0).value(), "First attribute value should be 'value1'");
        assertEquals("String", objectInfo.attributes().get(0).type(), "First attribute type should be 'String'");
        assertEquals("public", objectInfo.attributes().get(0).visibility(), "First attribute visibility should be 'public'");

        assertEquals("attr2", objectInfo.attributes().get(1).name(), "Second attribute name should be 'attr2'");
        assertEquals("42", objectInfo.attributes().get(1).value(), "Second attribute value should be '42'");
        assertEquals("int", objectInfo.attributes().get(1).type(), "Second attribute type should be 'int'");
        assertEquals("private", objectInfo.attributes().get(1).visibility(), "Second attribute visibility should be 'private'");
    }

}
