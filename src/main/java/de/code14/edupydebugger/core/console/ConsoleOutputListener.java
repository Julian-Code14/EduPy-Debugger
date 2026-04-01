package de.code14.edupydebugger.core.console;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import de.code14.edupydebugger.core.publish.PayloadPublisher;
import de.code14.edupydebugger.core.snapshot.NormalizedSnapshot;
import de.code14.edupydebugger.core.snapshot.ReplSnapshotAdapter;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ConsolePayload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Forwards console output and parses REPL snapshots to publish variables/objects.
 */
public class ConsoleOutputListener {

    private static final Logger LOGGER = Logger.getInstance(ConsoleOutputListener.class);
    private final ProcessHandler processHandler;
    private boolean startupLineSuppressed = false;
    private static final String SNAPSHOT_PREFIX = "__EDUPY_SNAPSHOT__";
    private final StringBuilder snapshotBuffer = new StringBuilder();
    private boolean capturingSnapshot = false;

    // --- Python Traceback Parsing ---
    private static final String TRACEBACK_PREFIX = "Traceback (most recent call last):";
    private final StringBuilder tracebackBuffer = new StringBuilder();
    private boolean capturingTraceback = false;
    private static final Pattern TB_FILE_LINE = Pattern.compile("\\s*File \"([^\"]+)\", line (\\d+), in .*");
    private static final Pattern TB_LAST_LINE = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*(?:Error|Exception)|KeyboardInterrupt)(?::\\s*(.*))?$");

    // Minimal, generische Schüler-Erklärungen je Exception-Typ (deutsch)
    private static final Map<String, String> EXPLANATION = new HashMap<>();
    private static final Map<String, String> TIP = new HashMap<>();

    static {
        EXPLANATION.put("IndentationError", "Dein Code ist nicht korrekt eingerückt.");
        TIP.put("IndentationError", "Achte auf die Einrückung. Verwende keine Leerzeichen, sondern nur die Tabulator-Taste.");

        EXPLANATION.put("SyntaxError", "Python kann diese Zeile nicht lesen.");
        TIP.put("SyntaxError", "Prüfe Klammern, Anführungszeichen und Doppelpunkte.");

        EXPLANATION.put("NameError", "Ein Name (Variable/Funktion) ist unbekannt.");
        TIP.put("NameError", "Prüfe die Schreibweise und ob der Name vorher definiert wurde.");

        EXPLANATION.put("TypeError", "Etwas hat einen unpassenden Typ.");
        TIP.put("TypeError", "Schau nach, welche Art (Zahl, Text, Liste …) erwartet wird.");

        EXPLANATION.put("IndexError", "Du greifst außerhalb einer Liste/Zeichenkette zu.");
        TIP.put("IndexError", "Verwende gültige Indizes von 0 bis Länge-1.");

        EXPLANATION.put("KeyError", "Der Schlüssel existiert nicht im Wörterbuch (dict).");
        TIP.put("KeyError", "Prüfe vorhandene Schlüssel und die genaue Schreibweise.");

        EXPLANATION.put("AttributeError", "Das Objekt hat dieses Attribut/diese Methode nicht.");
        TIP.put("AttributeError", "Prüfe den Objekttyp und den korrekten Methodennamen.");

        EXPLANATION.put("ValueError", "Ein Wert ist ungültig oder passt nicht.");
        TIP.put("ValueError", "Prüfe Eingaben und Umwandlungen (z. B. int(), float()).");

        EXPLANATION.put("ZeroDivisionError", "Es wurde durch 0 geteilt.");
        TIP.put("ZeroDivisionError", "Prüfe, ob der Divisor 0 ist, bevor du teilst.");

        EXPLANATION.put("FileNotFoundError", "Die Datei wurde nicht gefunden.");
        TIP.put("FileNotFoundError", "Prüfe Pfad/Dateiname und ob die Datei existiert.");

        EXPLANATION.put("ModuleNotFoundError", "Ein Modul konnte nicht gefunden werden.");
        TIP.put("ModuleNotFoundError", "Prüfe Modulname, Installation (pip) und Suchpfad.");

        EXPLANATION.put("ImportError", "Beim Import ist ein Fehler aufgetreten.");
        TIP.put("ImportError", "Stimmt der Modul-/Funktionsname und ist es installiert?");

        EXPLANATION.put("RecursionError", "Die Rekursion ist zu tief (Ende fehlt?).");
        TIP.put("RecursionError", "Füge eine Abbruchbedingung hinzu oder nutze eine Schleife.");
    }

    public ConsoleOutputListener(@NotNull ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public void attachConsoleListeners() {
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                String text = event.getText();
                if (shouldSuppressStartupLine(text)) {
                    startupLineSuppressed = true;
                    LOGGER.info("Suppressed debugger startup line");
                    return;
                }

                if (text != null) {
                    if (capturingSnapshot) {
                        snapshotBuffer.append(text);
                        if (flushSnapshotIfComplete()) return;
                    } else {
                        int idx = text.indexOf(SNAPSHOT_PREFIX);
                        if (idx >= 0) {
                            capturingSnapshot = true;
                            snapshotBuffer.setLength(0);
                            snapshotBuffer.append(text.substring(idx));
                            if (flushSnapshotIfComplete()) return;
                            return;
                        }
                    }

                    // Traceback-Erkennung und -Aufbereitung (Schüler-freundlich)
                    if (capturingTraceback) {
                        tracebackBuffer.append(text);
                        if (flushTracebackIfComplete()) return;
                        // solange die Exception nicht vollständig ist, keine Ausgabe
                        return;
                    } else {
                        int tIdx = text.indexOf(TRACEBACK_PREFIX);
                        if (tIdx >= 0) {
                            // falls vor dem Traceback noch normaler Text steht, weiterleiten
                            if (tIdx > 0) {
                                ConsolePayload pfx = new ConsolePayload();
                                pfx.text = text.substring(0, tIdx);
                                DebugServerEndpoint.sendDebugMessage("console", pfx);
                            }
                            capturingTraceback = true;
                            tracebackBuffer.setLength(0);
                            tracebackBuffer.append(text.substring(tIdx));
                            if (flushTracebackIfComplete()) return;
                            return;
                        }
                    }
                }

                LOGGER.info("Console Output: " + text);
                ConsolePayload payload = new ConsolePayload();
                payload.text = text;
                DebugServerEndpoint.sendDebugMessage("console", payload);
            }
        });
    }

    private boolean shouldSuppressStartupLine(String text) {
        if (startupLineSuppressed || text == null) return false;
        String t = text.trim();
        if (t.isEmpty()) return false;
        boolean containsPydevd = t.contains("pydev/pydevd.py") || t.contains("pydevd.py");
        boolean hasArg = (t.contains("--client") || t.contains("--port")) && t.contains("--file");
        return containsPydevd && hasArg;
    }

    private void publishVariablesFromSnapshot(String json) throws java.io.IOException {
        NormalizedSnapshot snapshot = ReplSnapshotAdapter.fromJson(json);
        PayloadPublisher.publishVariablesWithSnippet(snapshot.variables(), snapshot.objects());
        PayloadPublisher.publishObjects(snapshot.objects());
    }

    private boolean flushSnapshotIfComplete() {
        int nl = snapshotBuffer.indexOf("\n");
        if (nl < 0) return false;
        String line = snapshotBuffer.substring(0, nl);
        capturingSnapshot = false;
        try {
            if (line.startsWith(SNAPSHOT_PREFIX)) {
                publishVariablesFromSnapshot(line.substring(SNAPSHOT_PREFIX.length()));
                return true;
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to handle REPL snapshot line", t);
        }
        return false;
    }

    /**
     * Prüft, ob der gesammelte Traceback vollständig ist (letzte Zeile mit Exception-Typ vorhanden).
     * Sobald vollständig, wird eine kurze, verständliche Erklärung erzeugt und als eine konsolidierte
     * Konsolenzeile veröffentlicht. Die rohen Traceback-Zeilen werden nicht weitergeleitet.
     */
    private boolean flushTracebackIfComplete() {
        String buf = tracebackBuffer.toString();
        if (buf.isEmpty()) return false;

        // Aufteilen in Zeilen und letzte nicht-leere Zeile suchen
        String[] lines = buf.split("\r?\n");
        int i = lines.length - 1;
        while (i >= 0 && lines[i].trim().isEmpty()) i--;
        if (i < 0) return false;
        String last = lines[i].trim();

        Matcher lastLine = TB_LAST_LINE.matcher(last);
        if (!lastLine.find()) {
            return false; // noch nicht vollständig
        }

        String excName = lastLine.group(1);
        String excMsg  = lastLine.groupCount() >= 2 ? lastLine.group(2) : null;

        // Letzte File/Line-Zeile finden
        String filePath = null; String lineNo = null;
        for (int j = i - 1; j >= 0; j--) {
            Matcher m = TB_FILE_LINE.matcher(lines[j]);
            if (m.matches()) {
                filePath = m.group(1);
                lineNo   = m.group(2);
                break;
            }
        }

        // Freundliche Erklärung/Tipp bestimmen
        String explanation = EXPLANATION.getOrDefault(excName, "Python hat einen Fehler gemeldet.");
        String tip = TIP.getOrDefault(excName, "Lies die Meldung genau und prüfe die genannte Zeile.");

        StringBuilder out = new StringBuilder();
        out.append("Fehler erkannt: ").append(excName).append(" — ").append(explanation).append('\n');
        if (filePath != null && lineNo != null) {
            out.append("Ort: ").append(new File(filePath).getName()).append(":").append(lineNo).append('\n');
        }
        out.append("Tipp: ").append(tip);
        if (excMsg != null && !excMsg.isEmpty()) {
            out.append("\nPython: ").append(excName).append(": ").append(excMsg);
        }

        ConsolePayload payload = new ConsolePayload();
        payload.text = out.toString();
        DebugServerEndpoint.sendDebugMessage("console", payload);

        // zurücksetzen
        capturingTraceback = false;
        tracebackBuffer.setLength(0);
        return true;
    }
}
