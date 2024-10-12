package de.code14.edupydebugger.analysis.dynamicanalysis;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class AttributeInfoTests {

    @Test
    public void testAttributeInfoCreation() {
        // Arrange
        String expectedName = "attributeName";
        String expectedType = "String";
        String expectedValue = "value";
        String expectedVisibility = "public";

        // Act
        AttributeInfo attributeInfo = new AttributeInfo(expectedName, expectedType, expectedValue, expectedVisibility);

        // Assert
        assertEquals(expectedName, attributeInfo.name());
        assertEquals(expectedType, attributeInfo.type());
        assertEquals(expectedValue, attributeInfo.value());
        assertEquals(expectedVisibility, attributeInfo.visibility());
    }

    @Test
    public void testNameMethod() {
        // Arrange
        String expectedName = "attributeName";
        AttributeInfo attributeInfo = new AttributeInfo(expectedName, "String", "value", "public");

        // Act & Assert
        assertEquals(expectedName, attributeInfo.name());
    }

    @Test
    public void testTypeMethod() {
        // Arrange
        String expectedType = "String";
        AttributeInfo attributeInfo = new AttributeInfo("attributeName", expectedType, "value", "public");

        // Act & Assert
        assertEquals(expectedType, attributeInfo.type());
    }

    @Test
    public void testValueMethod() {
        // Arrange
        String expectedValue = "value";
        AttributeInfo attributeInfo = new AttributeInfo("attributeName", "String", expectedValue, "public");

        // Act & Assert
        assertEquals(expectedValue, attributeInfo.value());
    }

    @Test
    public void testVisibilityMethod() {
        // Arrange
        String expectedVisibility = "public";
        AttributeInfo attributeInfo = new AttributeInfo("attributeName", "String", "value", expectedVisibility);

        // Act & Assert
        assertEquals(expectedVisibility, attributeInfo.visibility());
    }
}

