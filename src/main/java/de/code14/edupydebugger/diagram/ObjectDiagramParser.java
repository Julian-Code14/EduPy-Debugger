package de.code14.edupydebugger.diagram;

import com.intellij.openapi.diagnostic.Logger;
import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The ObjectDiagramParser class is responsible for generating PlantUML diagrams representing objects and their associations.
 *
 * @author julian
 * @version 1.0
 * @since 06.08.24
 */
public class ObjectDiagramParser {

    private static final Logger LOGGER = Logger.getInstance(ObjectDiagramParser.class);

    /**
     * Generates a PlantUML diagram representing object cards with attributes.
     *
     * @param objects the map of object IDs to lists of ObjectInfo, representing object details
     * @return a string containing the PlantUML syntax for the object cards diagram
     */
    public static String generateObjectCards(Map<String, ObjectInfo> objects) {
        StringBuilder plantUML = new StringBuilder();
        plantUML.append("@startuml\n");
        plantUML.append("!pragma layout smetana\n");

        objects.forEach((key, objectInfo) -> {
            if (objectInfo == null) {
                return;
            }

            // Assume the first reference in the list represents the object
            String reference = objectInfo.references().get(0);
            plantUML.append("object \"").append(reference).append("\" as o").append(key).append(" {\n");

            for (AttributeInfo attribute : objectInfo.attributes()) {
                if ("static".equals(attribute.visibility())) {
                    plantUML.append("{static} ");
                }
                plantUML.append(attribute.name()).append(" = ").append(attribute.value()).append("\n");
            }

            plantUML.append("}\n");
        });

        plantUML.append("@enduml");

        LOGGER.info(plantUML.toString());

        return plantUML.toString();
    }

    /**
     * Generates a PlantUML diagram representing objects and their associations.
     *
     * @param objects the map of object IDs to lists of ObjectInfo, representing object details
     * @return a string containing the PlantUML syntax for the object diagram
     */
    public static String generateObjectDiagram(Map<String, ObjectInfo> objects) {
        StringBuilder plantUML = new StringBuilder();
        plantUML.append("@startuml\n");
        plantUML.append("!pragma layout smetana\n");

        objects.forEach((key, objectInfo) -> {
            if (objectInfo == null) {
                return;
            }

            // Assume the first reference in the list represents the object
            String reference = objectInfo.references().get(0);
            plantUML.append("object \"").append(reference).append("\" as o").append(key).append(" {\n");

            List<String> associations = new ArrayList<>();
            for (AttributeInfo attribute : objectInfo.attributes()) {
                if ("static".equals(attribute.visibility())) {
                    plantUML.append("{static} ");
                }
                if (attribute.value().startsWith("refid:")) {
                    associations.add(attribute.value().replace("refid:", ""));
                }
                plantUML.append(attribute.name()).append(" = ").append(attribute.value()).append("\n");
            }
            plantUML.append("}\n");

            for (String association : associations) {
                plantUML.append("o").append(key).append(" --> o").append(association).append("\n");
            }
        });

        plantUML.append("@enduml");

        LOGGER.info(plantUML.toString());

        return plantUML.toString();
    }

}
