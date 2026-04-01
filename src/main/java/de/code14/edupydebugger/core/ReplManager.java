package de.code14.edupydebugger.core;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a lightweight fallback Python REPL (outside of a debug session).
 * If the user types into the console without an active debug process, we
 * start a plain Python interpreter and wire its IO into the existing console
 * bridge so the UX behaves like a minimal REPL.
 */
public class ReplManager {

    private static final Logger LOGGER = Logger.getInstance(ReplManager.class);
    private static final ReplManager INSTANCE = new ReplManager();

    private OSProcessHandler replHandler;
    private boolean bootstrapped;

    public static ReplManager getInstance() {
        return INSTANCE;
    }

    /**
     * Ensures a REPL process is running and returns its ProcessHandler.
     * Tries "python3" first, then "python".
     */
    public synchronized ProcessHandler ensureReplStarted() throws Exception {
        if (replHandler != null && !replHandler.isProcessTerminated()) {
            return replHandler;
        }

        List<String> cmd = new ArrayList<>();
        String executable = "python3";
        try {
            // prefer python3 if available
            executable = "python3";
            cmd.add(executable);
            cmd.add("-i"); // interactive
            cmd.add("-q"); // quiet banner
            GeneralCommandLine gcl = new GeneralCommandLine(cmd);
            gcl.withEnvironment("PYTHONUNBUFFERED", "1");
            replHandler = new OSProcessHandler(gcl);
            LOGGER.info("Started fallback REPL using '" + executable + "'");
        } catch (Throwable primary) {
            // retry with python
            cmd.clear();
            executable = "python";
            cmd.add(executable);
            cmd.add("-i");
            cmd.add("-q");
            GeneralCommandLine gcl2 = new GeneralCommandLine(cmd);
            gcl2.withEnvironment("PYTHONUNBUFFERED", "1");
            replHandler = new OSProcessHandler(gcl2);
            LOGGER.info("Started fallback REPL using '" + executable + "'");
        }
        bootstrapped = false;
        try { ensureBootstrapInjected(); } catch (Throwable t) { LOGGER.warn("REPL bootstrap failed", t); }
        return replHandler;
    }

    /** Stops the REPL if it is running. */
    public synchronized void stopRepl() {
        if (replHandler != null) {
            try {
                replHandler.destroyProcess();
            } catch (Throwable t) {
                // ignore; best-effort
            } finally {
                replHandler = null;
                bootstrapped = false;
            }
        }
    }

    /** Injects helper functions into the REPL to snapshot variables as JSON. */
    public synchronized void ensureBootstrapInjected() throws Exception {
        if (replHandler == null || replHandler.isProcessTerminated() || bootstrapped) return;
        var os = replHandler.getProcessInput();
        if (os == null) return;
        String bootstrap = String.join("\n",
                "import json, builtins",
                "_EDUPY_PRIMS = {'int','float','str','bool','list','dict','tuple','set'}",
                "def _edupy__snapshot():",
                "    out = []",
                "    gs = globals()",
                "    for k,v in list(gs.items()):",
                "        if k.startswith('_') or k in ('__name__','__builtins__'):",
                "            continue",
                "        try:",
                "            t = type(v).__name__",
                "            if t in _EDUPY_PRIMS:",
                "                reprv = repr(v)",
                "            else:",
                "                attrs = []",
                "                try:",
                "                    for a in dir(v):",
                "                        if a.startswith('_'): continue",
                "                        try:",
                "                            av = getattr(v, a)",
                "                            s = repr(av)",
                "                            if len(s) > 20: s = s[:20] + ' [...]'",
                "                            attrs.append(f'{a}: {s}')",
                "                        except Exception:",
                "                            pass",
                "                except Exception:",
                "                    pass",
                "                reprv = '\\n'.join(attrs[:10])",
                "            out.append({'id': str(id(v)), 'name': k, 'type': t, 'repr': reprv, 'scope': 'global'})",
                "        except Exception:",
                "            pass",
                "    return json.dumps(out)",
                ""
        ) + "\n";
        os.write(bootstrap.getBytes());
        os.flush();
        bootstrapped = true;
    }

    /** Requests a variables snapshot from the REPL (parsed by the Java side via ConsoleOutputListener). */
    public synchronized void requestSnapshot() throws Exception {
        if (replHandler == null || replHandler.isProcessTerminated()) return;
        ensureBootstrapInjected();
        var os = replHandler.getProcessInput();
        if (os == null) return;
        String cmd = "print(\"__EDUPY_SNAPSHOT__\"+_edupy__snapshot())\n";
        os.write(cmd.getBytes());
        os.flush();
    }
}
