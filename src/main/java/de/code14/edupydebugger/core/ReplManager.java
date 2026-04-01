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
            LOGGER.info("Started fallback REPL (retry) using '" + executable + "'");
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
        // Robust one-liner using exec() to avoid interactive block/blank-line issues
        String sb =
                "import json, os, sys;exec(\"" +
                "def __edupy_bootstrap():\\n" +
                "    wd = os.environ.get('EDUPY_WORKDIR')\\n" +
                "    if wd and wd not in sys.path: sys.path.insert(0, wd)\\n" +
                "    eps = os.environ.get('EDUPY_EXTRA_PATHS','')\\n" +
                "    for p in eps.split(os.pathsep):\\n        p = p.strip()\\n        if p and p not in sys.path: sys.path.insert(0, p)\\n" +
                "    try:\\n        sys.ps1=''\\n        sys.ps2=''\\n    except Exception:\\n        pass\\n" +
                "__edupy_bootstrap()\\n" +
                "del __edupy_bootstrap\\n" +
                "def _is_primitive(obj):\\n    return type(obj).__name__ in {'int','float','str','bool','list','dict','tuple','set'}\\n" +
                "def _is_noise(obj):\\n    tn=type(obj).__name__\\n    return tn in {'module','function','builtin_function_or_method','method','type'} or callable(obj)\\n" +
                "def _safe_repr(v):\\n    try:\\n        s=repr(v)\\n        return s if len(s)<=120 else s[:120]+' [...]'\\n    except Exception:\\n        return '<error>'\\n" +
                "def _edupy__snapshot():\\n" +
                "    vars_out=[]\\n" +
                "    objects={}\\n" +
                "    gs=globals()\\n" +
                "    def ensure_obj(o, ref_label):\\n" +
                "        oid=str(id(o))\\n" +
                "        if oid not in objects:\\n            objects[oid]={'ref': ref_label, 'attrs': []}\\n" +
                "        return oid\\n" +
                "    for k,v in list(gs.items()):\\n" +
                "        if k.startswith('_') or k in ('__name__','__builtins__'):\\n            continue\\n" +
                "        if _is_noise(v):\\n            continue\\n" +
                "        try:\\n" +
                "            t=type(v).__name__\\n" +
                "            if _is_primitive(v):\\n" +
                "                vars_out.append({'id': str(id(v)), 'name': k, 'type': t, 'repr': _safe_repr(v), 'scope': 'global'})\\n" +
                "            else:\\n" +
                "                oid=ensure_obj(v, f'{k}: {t}')\\n" +
                "                # attributes (best-effort)\\n" +
                "                try:\\n" +
                "                    for a in dir(v):\\n" +
                "                        if a.startswith('_'):\\n                            continue\\n" +
                "                        try:\\n" +
                "                            av=getattr(v,a)\\n" +
                "                            at=type(av).__name__\\n" +
                "                            if _is_noise(av):\\n                                continue\\n" +
                "                            if _is_primitive(av):\\n" +
                "                                objects[oid]['attrs'].append({'name': a, 'type': at, 'value': _safe_repr(av), 'visibility': 'public'})\\n" +
                "                            else:\\n" +
                "                                rid=ensure_obj(av, f'{t}.{a}: {at}')\\n" +
                "                                objects[oid]['attrs'].append({'name': a, 'type': at, 'value': 'refid:'+rid, 'visibility': 'public'})\\n" +
                "                        except Exception:\\n                            pass\\n" +
                "                except Exception:\\n                    pass\\n" +
                "                vars_out.append({'id': oid, 'name': k, 'type': t, 'repr': _safe_repr(v), 'scope': 'global'})\\n" +
                "        except Exception:\\n            pass\\n" +
                "    return json.dumps({'variables': vars_out, 'objects': objects})\\n" +
                "\")\n";
        os.write(sb.getBytes());
        os.flush();
        bootstrapped = true;
    }

    /** Requests a variables snapshot from the REPL (parsed by the Java side via ConsoleOutputListener). */
    public synchronized void requestSnapshot() throws Exception {
        if (replHandler == null || replHandler.isProcessTerminated()) return;
        ensureBootstrapInjected();
        var os = replHandler.getProcessInput();
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
        java.util.StringJoiner joiner = new java.util.StringJoiner(sep);
        if (workingDirectory != null) joiner.add(workingDirectory);
        for (String p : extraPaths) joiner.add(p);
        if (existing != null && !existing.isEmpty()) joiner.add(existing);
        return joiner.toString();
    }

    private String buildExtraPaths() {
        String sep = java.io.File.pathSeparator;
        java.util.StringJoiner joiner = new java.util.StringJoiner(sep);
        for (String p : extraPaths) joiner.add(p);
        return joiner.toString();
    }

    /** Use an explicit interpreter (e.g., project SDK/venv python). */
    public synchronized void setInterpreterPath(String interpreterPath) {
        this.interpreterPath = interpreterPath;
    }
}
