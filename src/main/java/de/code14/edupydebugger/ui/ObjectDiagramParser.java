package de.code14.edupydebugger.ui;

import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author julian
 * @version 1.0
 * @since 06.08.24
 */
public class ObjectDiagramParser {

    private static final Logger LOGGER = Logger.getInstance(ObjectDiagramParser.class);


    public static String generateObjectCards(Map<String, List<Object>[]> objects) {
        StringBuilder plantUML = new StringBuilder();
        plantUML.append("@startuml\n");
        plantUML.append("!pragma layout smetana\n");

        objects.forEach((key, value) -> {
            List<String> references = new ArrayList<>();
            for (Object object : value[0]) {
                if (object instanceof String) {
                    references.add((String) object);
                }
            }
            List<List<String>> attributes = new ArrayList<>();
            for (Object object : value[1]) {
                if (object instanceof List<?>) {
                    List<?> tempList = (List<?>) object;
                    List<String> attributeList = new ArrayList<>();
                    for (Object attribute : tempList) {
                        if (attribute instanceof String) {
                            attributeList.add((String) attribute);
                        }
                    }
                    attributes.add(attributeList);
                }
            }

            plantUML.append("object \"").append(references.get(0)).append("\" as o").append(key).append(" {\n");
            for (List<String> attribute : attributes) {
                plantUML.append(attribute.get(0)).append(" = ").append(attribute.get(2)).append("\n");
            }
            plantUML.append("}\n");

            // Erste Liste (Array[0]) in ein Komma-getrenntes String-Format konvertieren
            /*String firstListString = String.join(",", references);

            // Zweite Liste (Array[1]) in das gewünschte Format konvertieren
            StringBuilder secondListString = new StringBuilder();
            for (List<String> list : attributes) {
                secondListString.append(String.join(",", list)).append(";");
            }
            // Entferne das letzte Semikolon
            if (secondListString.length() > 0) {
                secondListString.setLength(secondListString.length() - 1);
            }

            // Füge den formatierten String für diesen Eintrag hinzu
            plantUML.append(key)
                    .append("=")
                    .append(firstListString)
                    .append("$")
                    .append(secondListString)
                    .append("§");*/
        });

        // Entferne das letzte §-Zeichen, falls vorhanden
        /*if (plantUML.length() > 0 && plantUML.charAt(plantUML.length() - 1) == '§') {
            plantUML.setLength(plantUML.length() - 1);
        }*/

        plantUML.append("@enduml");

        LOGGER.info(plantUML.toString());

        return plantUML.toString();
    }

}
