package de.code14.edupydebugger.analysis.dynamicanalysis;

import java.util.List;

/**
 * The ObjectInfo record encapsulates information about an object in a Python debugging session.
 * It contains references to other objects and a list of attributes associated with the object.
 *
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public record ObjectInfo(List<String> references, List<AttributeInfo> attributes) {

    /**
     * Constructs an ObjectInfo record with the specified references and attributes.
     *
     * @param references a list of references to other objects associated with this object
     * @param attributes a list of attributes that describe this object
     */
    public ObjectInfo {}

    /**
     * Returns the list of references to other objects associated with this object.
     *
     * @return a list of strings representing references to other objects
     */
    @Override
    public List<String> references() {
        return references;
    }

    /**
     * Returns the list of attributes that describe this object.
     *
     * @return a list of {@link AttributeInfo} objects representing the attributes of this object
     */
    @Override
    public List<AttributeInfo> attributes() {
        return attributes;
    }

}
