package de.code14.edupydebugger.diagram;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import de.code14.edupydebugger.analysis.ClassInfo;
import de.code14.edupydebugger.analysis.PythonAnalyzer;

import java.util.List;
import java.util.Map;

/**
 * The ClassDiagramParser class is responsible for generating class diagrams in PlantUML format
 * based on the analysis of Python files within a project. It utilizes the PythonAnalyzer
 * to gather the necessary class details such as attributes, methods, references, and superclasses.
 *
 * @author julian
 * @version 1.0
 * @since 16.07.24
 */
public class ClassDiagramParser {

    private static final Logger LOGGER = Logger.getInstance(ClassDiagramParser.class);

    private final PythonAnalyzer pythonAnalyzer;

    /**
     * Constructs a ClassDiagramParser with the specified PythonAnalyzer.
     *
     * @param pythonAnalyzer the analyzer used to extract class details from Python files
     */
    public ClassDiagramParser(PythonAnalyzer pythonAnalyzer) {
        this.pythonAnalyzer = pythonAnalyzer;
    }


    /**
     * Generates a class diagram in PlantUML format based on the analysis of Python files
     * within the given project.
     *
     * @param project the project containing the Python files to be analyzed
     * @return a String containing the PlantUML representation of the class diagram
     */
    public String generateClassDiagram(Project project) {
        // Analyze Python files in the project to extract class details
        pythonAnalyzer.analyzePythonFiles(project);
        Map<String, ClassInfo> classDetails = pythonAnalyzer.getClassDetails();

        StringBuilder plantUML = new StringBuilder();
        plantUML.append("@startuml\n");
        plantUML.append("!pragma layout smetana\n");

        // Iterate through each class and its details to build the PlantUML diagram
        for (Map.Entry<String, ClassInfo> entry : classDetails.entrySet()) {
            String className = entry.getKey();
            ClassInfo details = entry.getValue();
            List<String> attributes = details.attributes();
            List<String> methods = details.methods();
            List<String> references = details.references();
            List<String> superClasses = details.superClasses();

            // Mark class as abstract if it extends ABC (Abstract Base Class)
            if (superClasses.contains("ABC")) {
                plantUML.append("abstract ");
            }
            plantUML.append("class ").append(className).append(" {\n");

            // Add class attributes
            for (String attribute : attributes) {
                plantUML.append("  ").append(attribute).append("\n");
            }

            // Add class methods
            for (String method : methods) {
                plantUML.append("  ").append(method).append("\n");
            }

            plantUML.append("}\n");

            // Add references (associations) to other classes
            for (String reference : references) {
                plantUML.append(className).append(" ..> ").append(reference).append("\n");
            }

            // Add inheritance relationships
            for (String superClass : superClasses) {
                if (!superClasses.contains("ABC")) {
                    plantUML.append(className).append(" --|> ").append(superClass).append("\n");
                }
            }
        }

        plantUML.append("@enduml");

        LOGGER.debug(plantUML.toString());

        return plantUML.toString();
    }

}
