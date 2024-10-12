package de.code14.edupydebugger.analysis.dynamicanalysis;

/**
 * The ObjectInfo record encapsulates information about an object in a Python debugging session.
 * It stores details such as the object's name, type, value, and visibility.
 *
 * @param name       the name of the object
 * @param type       the type of the object
 * @param value      the current value of the object
 * @param visibility the visibility of the object (e.g., public, protected, private, or static)
 *
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public record AttributeInfo(String name, String type, String value, String visibility) {

    /**
     * Constructs an ObjectInfo instance with the specified name, type, value, and visibility.
     *
     * @param name       the name of the object
     * @param type       the type of the object
     * @param value      the current value of the object
     * @param visibility the visibility of the object (e.g., public, protected, private, or static)
     */
    public AttributeInfo {
    }

    /**
     * Returns the name of the object.
     *
     * @return the name of the object
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the type of the object.
     *
     * @return the type of the object
     */
    @Override
    public String type() {
        return type;
    }

    /**
     * Returns the current value of the object.
     *
     * @return the current value of the object
     */
    @Override
    public String value() {
        return value;
    }

    /**
     * Returns the visibility of the object.
     *
     * @return the visibility of the object (e.g., public, protected, private, or static)
     */
    @Override
    public String visibility() {
        return visibility;
    }
}