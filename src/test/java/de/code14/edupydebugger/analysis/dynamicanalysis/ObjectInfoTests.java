package de.code14.edupydebugger.analysis.dynamicanalysis;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
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
        assertNotNull("References should not be null", objectInfo.references());
        assertNotNull("Attributes should not be null", objectInfo.attributes());

        assertEquals("References size should be 2", 2, objectInfo.references().size());
        assertEquals("Attributes size should be 2", 2, objectInfo.attributes().size());

        assertEquals("First reference should be 'ref1'", "ref1", objectInfo.references().get(0));
        assertEquals("Second reference should be 'ref2'", "ref2", objectInfo.references().get(1));

        assertEquals("First attribute name should be 'attr1'", "attr1", objectInfo.attributes().get(0).name());
        assertEquals("First attribute value should be 'value1'", "value1", objectInfo.attributes().get(0).value());
        assertEquals("First attribute type should be 'String'", "String", objectInfo.attributes().get(0).type());
        assertEquals("First attribute visibility should be 'public'", "public", objectInfo.attributes().get(0).visibility());

        assertEquals("Second attribute name should be 'attr2'", "attr2", objectInfo.attributes().get(1).name());
        assertEquals("Second attribute value should be '42'", "42", objectInfo.attributes().get(1).value());
        assertEquals("Second attribute type should be 'int'", "int", objectInfo.attributes().get(1).type());
        assertEquals("Second attribute visibility should be 'private'", "private", objectInfo.attributes().get(1).visibility());
    }
}

