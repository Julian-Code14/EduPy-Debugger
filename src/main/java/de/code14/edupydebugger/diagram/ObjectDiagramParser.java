package de.code14.edupydebugger.diagram;

import com.intellij.openapi.diagnostic.Logger;
import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * The ObjectDiagramParser class is responsible for generating PlantUML diagrams representing objects and their associations.
 *
 * @author julian
 * @version 0.3.0
 * @since 0.1.0
 */
public class ObjectDiagramParser {

    private static final Logger LOGGER = Logger.getInstance(ObjectDiagramParser.class);

    /**
     * Generates a PlantUML diagram representing object cards with attributes.
     *
     * @param objects the map of object IDs to lists of ObjectInfo, representing object details
     * @return a map of strings containing the keys and the PlantUML syntax for the object cards
     */
    public static Map<String, String> generateObjectCards(Map<String, ObjectInfo> objects) {
        Map<String, String> plantUmlStrings = new HashMap<>();

        objects.forEach((key, objectInfo) -> {
            StringBuilder plantUML = new StringBuilder();
            plantUML.append("@startuml\n");
            plantUML.append("!pragma layout smetana\n");

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
                plantUML.append(attribute.name())
                        .append(" = ");
                if (attribute.value().startsWith("refid:")) {
                    plantUML.append(attribute.value().replace("refid:", ""))
                            .append(" ")
                            .append("[[[localhost:8026/")
                            .append(attribute.value().replace("refid:", ""))
                            .append("]]]");
                } else {
                    // Check if the attribute value is too long to show it
                    if (attribute.value().length() > 20) {
                        plantUML.append(attribute.value(), 0, 20).append(" [...]");
                    } else {
                        plantUML.append(attribute.value());
                    }
                }

                plantUML.append("\n");
            }

            plantUML.append("}\n");

            plantUML.append("@enduml");

            LOGGER.info(plantUML.toString());

            plantUmlStrings.put(key, plantUML.toString());
        });

        return plantUmlStrings;
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

        Map<String, String> associations = new HashMap<>();

        objects.forEach((key, objectInfo) -> {
            if (objectInfo == null) {
                return;
            }

            // Assume the first reference in the list represents the object
            String reference = objectInfo.references().get(0);
            plantUML.append("object \"").append(reference).append("\" as o").append(key).append(" {\n");
            LOGGER.debug("object \"" + reference + "\" as o" + key + " {\n");

            for (AttributeInfo attribute : objectInfo.attributes()) {
                if ("static".equals(attribute.visibility())) {
                    plantUML.append("{static} ");
                }
                if (attribute.value().startsWith("refid:")) {
                    associations.put("o" + key, attribute.value().replace("refid:", ""));
                }
                plantUML.append(attribute.name()).append(" = ").append(attribute.value()).append("\n");
            }
            plantUML.append("}\n");
        });

        associations.forEach((referencing, referenced) -> plantUML.append(referencing).append(" --> o").append(referenced).append("\n"));

        plantUML.append("@enduml");

        LOGGER.info(plantUML.toString());

        return plantUML.toString();
    }

}
