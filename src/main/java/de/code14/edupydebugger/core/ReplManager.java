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
    private String workingDirectory;
    private List<String> extraPaths = new ArrayList<>();
    private String interpreterPath; // optional: project interpreter

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
        String executable = interpreterPath != null ? interpreterPath : "python3";
        try {
            // prefer python3 if available
            // if no explicit interpreter set, try python3 else python
            cmd.add(executable);
            cmd.add("-i"); // interactive
            cmd.add("-q"); // quiet banner
            GeneralCommandLine gcl = new GeneralCommandLine(cmd);
            if (workingDirectory != null) gcl.withWorkDirectory(workingDirectory);
            gcl.withEnvironment("PYTHONUNBUFFERED", "1");
            if (workingDirectory != null) {
                gcl.withEnvironment("EDUPY_WORKDIR", workingDirectory);
                // prepend to PYTHONPATH if present
                String existing = System.getenv("PYTHONPATH");
                String pp = buildPythonPath(existing);
                gcl.withEnvironment("PYTHONPATH", pp);
                gcl.withEnvironment("EDUPY_EXTRA_PATHS", buildExtraPaths());
            }
            replHandler = new OSProcessHandler(gcl);
            replHandler.startNotify();
            LOGGER.info("Started fallback REPL using '" + executable + "'");
        } catch (Throwable primary) {
            // retry with python
            cmd.clear();
            executable = interpreterPath != null ? interpreterPath : "python";
            cmd.add(executable);
            cmd.add("-i");
            cmd.add("-q");
            GeneralCommandLine gcl2 = new GeneralCommandLine(cmd);
            if (workingDirectory != null) gcl2.withWorkDirectory(workingDirectory);
            gcl2.withEnvironment("PYTHONUNBUFFERED", "1");
            if (workingDirectory != null) {
                gcl2.withEnvironment("EDUPY_WORKDIR", workingDirectory);
                String existing = System.getenv("PYTHONPATH");
                String pp = buildPythonPath(existing);
                gcl2.withEnvironment("PYTHONPATH", pp);
                gcl2.withEnvironment("EDUPY_EXTRA_PATHS", buildExtraPaths());
            }
            replHandler = new OSProcessHandler(gcl2);
            replHandler.startNotify();
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
                "import json, builtins, os, sys",
                "wd = os.environ.get('EDUPY_WORKDIR')",
                "if wd and wd not in sys.path: sys.path.insert(0, wd)",
                "eps = os.environ.get('EDUPY_EXTRA_PATHS','')",
                "for p in eps.split(os.pathsep):\n    p=p.strip()\n    if p and p not in sys.path: sys.path.insert(0,p)",
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

    /** Sets the working directory used for the REPL process and PYTHONPATH. */
    public synchronized void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /** Additional project source roots that should be importierbar. */
    public synchronized void setExtraPythonPaths(List<String> paths) {
        this.extraPaths = new ArrayList<>(paths != null ? paths : List.of());
    }

    private String buildPythonPath(String existing) {
        String sep = java.io.File.pathSeparator;
        StringBuilder sb = new StringBuilder();
        if (workingDirectory != null) sb.append(workingDirectory);
        for (String p : extraPaths) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        if (existing != null && !existing.isEmpty()) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(existing);
        }
        return sb.toString();
    }

    private String buildExtraPaths() {
        String sep = java.io.File.pathSeparator;
        StringBuilder sb = new StringBuilder();
        for (String p : extraPaths) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    /** Use an explicit interpreter (e.g., project SDK/venv python). */
    public synchronized void setInterpreterPath(String interpreterPath) {
        this.interpreterPath = interpreterPath;
    }
}
