package de.code14.edupydebugger.ui;

import com.intellij.openapi.project.Project;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyTargetExpression;
import com.jetbrains.python.psi.types.TypeEvalContext;
import de.code14.edupydebugger.debugger.PythonAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author julian
 * @version 1.0
 * @since 16.07.24
 */
public class ClassDiagramParser {

    public static String generateClassDiagram(Project project) {
        PythonAnalyzer.analyzePythonFile(project);
        Map<String, Object[]> classDetails = PythonAnalyzer.getClassDetails();

        StringBuilder plantUML = new StringBuilder();
        plantUML.append("@startuml\n");
        plantUML.append("!pragma layout smetana\n");

        // Klassen und ihre Beziehungen
        for (Map.Entry<String, Object[]> entry : classDetails.entrySet()) {
            String className = entry.getKey();
            Object[] details = entry.getValue();
            List<String> attributes = (List<String>) details[0];
            List<String> methods = (List<String>) details[1];

            plantUML.append("class ").append(className).append(" {\n");

            // Attribute der Klasse hinzufügen
            for (String attribute : attributes) {
                plantUML.append("  ").append(attribute).append("\n");
            }

            // Methoden der Klasse hinzufügen
            for (String method : methods) {
                plantUML.append("  ").append(method).append("\n");
            }

            plantUML.append("}\n");
        }

        plantUML.append("@enduml");

        System.out.println(plantUML);

        return plantUML.toString();
    }

}
