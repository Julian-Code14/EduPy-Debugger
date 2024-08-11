package de.code14.edupydebugger.analysis.staticanalysis;

import java.util.List;

/**
 * Record class to hold details about a Python class.
 * It contains information about the class attributes, methods, references, and superclasses.
 *
 * @param attributes    List of attribute descriptions of the class.
 * @param methods       List of method signatures of the class.
 * @param references    List of reference types used by the class.
 * @param superClasses  List of superclasses of the class.
 *
 * @author julian
 * @version 1.0
 * @since 11.08.24
 */
public record ClassInfo(List<String> attributes, List<String> methods, List<String> references, List<String> superClasses) {

}
