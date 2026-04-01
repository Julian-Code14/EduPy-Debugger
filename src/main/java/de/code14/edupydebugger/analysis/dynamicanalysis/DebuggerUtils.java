package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.python.debugger.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for working with the debugger and extracting stack frames.
 * Provides methods to retrieve all stack frames from a given debugging session.
 */
public class DebuggerUtils {

    private static final Logger LOGGER = Logger.getInstance(DebuggerUtils.class);


    private static final int INITIAL_FRAME_INDEX = 1;

    /**
     * Retrieves a list of {@link PyThreadInfo} objects representing all threads managed by the given
     * PyCharm debug session.
     * <p>
     * Internally, it casts the session's debug process to {@link PyDebugProcess} and invokes
     * {@link PyDebugProcess#getThreads()} to obtain the underlying thread information. The resulting
     * collection is then converted into a list for convenient use.
     *
     * @param debugSession the active debug session from which to extract thread information
     * @return a new {@link ArrayList} containing the thread info for each thread in the session
     */
    public static List<PyThreadInfo> getThreads(@NotNull XDebugSession debugSession) {
        PyDebugProcess debugProcess = (PyDebugProcess) debugSession.getDebugProcess();
        Collection<PyThreadInfo> threadInfos = debugProcess.getThreads();
        return new ArrayList<>(threadInfos);
    }

    /**
     * Retrieves stack frames for each thread separately.
     * Returns a map where each key is a {@link PyThreadInfo} and the value is the list of {@link PyStackFrame}
     * belonging to that thread.
     *
     * @param debugSession the current debugging session
     * @return a map of thread info objects to their respective lists of stack frames
     */
    public static Map<PyThreadInfo, List<PyStackFrame>> getStackFramesPerThread(XDebugSession debugSession) {
        PyDebugProcess debugProcess = (PyDebugProcess) debugSession.getDebugProcess();
        Collection<PyThreadInfo> threadInfos = debugProcess.getThreads();

        Map<PyThreadInfo, List<PyStackFrame>> perThreadFrames = new HashMap<>();

        // Iterate through each thread, extract its frames, and store them in the map
        for (PyThreadInfo threadInfo : threadInfos) {
            // Prüfen, ob der Thread suspended ist
            if (threadInfo.getState() == PyThreadInfo.State.SUSPENDED) {
                PyExecutionStack executionStack = new PyExecutionStack(debugProcess, threadInfo);
                List<PyStackFrame> stackFrames = extractStackFrames(executionStack);
                perThreadFrames.put(threadInfo, stackFrames);
            } else if (threadInfo.getState() == PyThreadInfo.State.RUNNING) {
                LOGGER.debug("Still running thread: " + threadInfo.getName()
                        + " (state=" + threadInfo.getState() + ")");
            } else {
                LOGGER.debug("Skipping stack frames for thread: " + threadInfo.getName()
                        + " (state=" + threadInfo.getState() + ")");
            }
        }

        return perThreadFrames;
    }

    /**
     * Extracts all stack frames from a given execution stack.
     * This method starts with the top frame and asynchronously retrieves the remaining frames.
     *
     * @param executionStack the execution stack to extract frames from
     * @return a list of {@link PyStackFrame} objects representing the stack frames in the execution stack
     */
    private static @NotNull List<PyStackFrame> extractStackFrames(PyExecutionStack executionStack) {
        List<PyStackFrame> stackFrames = new CopyOnWriteArrayList<>();

        // Add the top stack frame (most recent frame), ensuring it's not null
        PyStackFrame topFrame = executionStack.getTopFrame();
        if (topFrame != null) {
            stackFrames.add(topFrame);
        }

        // Asynchronously retrieve the remaining stack frames
        executionStack.computeStackFrames(INITIAL_FRAME_INDEX, new XExecutionStack.XStackFrameContainer() {
            @Override
            public void addStackFrames(@NotNull List<? extends XStackFrame> xFrames, boolean last) {
                for (XStackFrame frame : xFrames) {
                    if (frame instanceof PyStackFrame) {
                        stackFrames.add((PyStackFrame) frame);
                    }
                }
            }

            @Override
            public void errorOccurred(@NotNull String errorMessage) {
                // Log the error or handle it appropriately
                LOGGER.error("Error occurred while fetching stack frames: " + errorMessage);
            }

            @Override
            public boolean isObsolete() {
                // Implement logic to determine if the container is obsolete, if necessary
                return false;
            }
        });

        return stackFrames;
    }

    /**
     * Formats a human-readable call stack list like "func(a=1, b='x')" for each frame.
     * Falls back to "name()" if argument inspection is not available.
     */
    public static List<String> formatCallstackFrames(List<PyStackFrame> frames) {
        if (frames == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        Map<String, Integer> nameOccurrence = new HashMap<>();
        for (int idx = 0; idx < frames.size(); idx++) {
            PyStackFrame f = frames.get(idx);
            String base = f.getName();
            if (base == null || base.isEmpty()) base = "<module>";
            String[] holder = new String[]{base + "()"};
            CountDownLatch latch = new CountDownLatch(1);
            final int depth = idx;
            final String baseName = base;
            final int occurrence = nameOccurrence.merge(base, 1, Integer::sum) - 1; // 0-based for this name
            try {
                f.computeChildren(new XCompositeNode() {
                    @Override
                    public void addChildren(@NotNull XValueChildrenList children, boolean last) {
                        try {
                            if (children.size() > 0 && children.getValue(0) instanceof PyDebugValue) {
                                PyDebugValue ctx = (PyDebugValue) children.getValue(0);
                                // Build both depth-based and name-based expressions and prefer depth-based
                                String safeName = baseName.replace("'", "\\'");
                                // Only take real parameter names from the frame (args + *varargs + **kwargs);
                                // do NOT merge general locals here to avoid showing non-parameters.
                                String namesExprDepth = String.format(
                                        "(lambda _sys,_ins,_n: (lambda _av: ','.join([a for a in (list(_av.args)+([] if _av.varargs is None else [_av.varargs])+([] if _av.keywords is None else [_av.keywords])) if a and not a.startswith('__') and not a.startswith('_pydev_') and not a.startswith('__py')]))(_ins.getargvalues(_sys._getframe(_n))))(__import__('sys'), __import__('inspect'), %d)",
                                        depth);
                                String namesExprByName = String.format(
                                        "(lambda __ins: (lambda __matches: ('' if len(__matches)<=%d else ','.join(__ins.getargvalues(__matches[%d].frame).args)))([fi for fi in __ins.stack() if getattr(fi,'function','')=='%s']))(__import__('inspect'))",
                                        occurrence, occurrence, safeName);
                                // No locals fallback here either; if name-based selection fails, we show name() only.
                                if ("<module>".equals(baseName)) {
                                    holder[0] = baseName + "()"; // avoid dumping module locals
                                } else {
                                    // Try depth-based parameter names first
                                    PyDebugValue namesVal = ctx.getFrameAccessor().evaluate(namesExprDepth, false, true);
                                    String namesCsv = namesVal != null && namesVal.getValue() != null ? namesVal.getValue() : "";
                                    // Fallback to name-based selection if still empty
                                    if (namesCsv.isEmpty()) {
                                        try {
                                            PyDebugValue nv = ctx.getFrameAccessor().evaluate(namesExprByName, false, true);
                                            namesCsv = nv != null && nv.getValue() != null ? nv.getValue() : "";
                                        } catch (Throwable ignore) {}
                                    }
                                    List<String> parts = new ArrayList<>();
                                    if (!namesCsv.isEmpty()) {
                                        // Deduplicate while preserving order
                                        LinkedHashSet<String> ordered = new LinkedHashSet<>();
                                        for (String s : namesCsv.split(",")) { String t = s.trim(); if (!t.isEmpty()) ordered.add(t); }
                                        ordered.remove("_sys"); ordered.remove("_ins"); ordered.remove("_n");
                                        String[] names = ordered.toArray(new String[0]);
                                        int limit = Math.min(names.length, 12); // safeguard against pathological cases
                                        for (int i = 0; i < limit; i++) {
                                            String an = names[i].trim(); if (an.isEmpty()) continue;
                                            if ("_sys".equals(an) || "_ins".equals(an) || "_n".equals(an)) continue;
                                            String anEsc = an.replace("'", "\\'");
                                            // Prefer depth-based value lookup; fallback to name-based frame if needed
                                            String valExpr = String.format(
                                                    "(lambda _sys,_ins,_n,_a: (lambda __av: ((('refid:'+str(__builtins__.id(__av.locals.get(_a, None))))) if (not isinstance(__av.locals.get(_a, None),(int,float,str,bool,list,dict,tuple,set))) else repr(__av.locals.get(_a, None))))(_ins.getargvalues(_sys._getframe(_n))))(__import__('sys'), __import__('inspect'), %d, '%s')",
                                                    depth, anEsc);
                                            String valExprByName = String.format(
                                                    "(lambda __ins: (lambda __matches: ('' if len(__matches)<=%d else (lambda __av: ((('refid:'+str(__builtins__.id(__av.locals.get('%s', None))))) if (not isinstance(__av.locals.get('%s', None),(int,float,str,bool,list,dict,tuple,set))) else repr(__av.locals.get('%s', None))))(__ins.getargvalues(__matches[%d].frame))))([fi for fi in __ins.stack() if getattr(fi,'function','')=='%s']))(__import__('inspect'))",
                                                    occurrence, anEsc, anEsc, anEsc, occurrence, safeName);
                                            try {
                                                PyDebugValue rv = ctx.getFrameAccessor().evaluate(valExpr, false, true);
                                                String vv = rv != null && rv.getValue() != null ? rv.getValue() : "";
                                                if (vv.isEmpty()) {
                                                    PyDebugValue rv2 = ctx.getFrameAccessor().evaluate(valExprByName, false, true);
                                                    vv = rv2 != null && rv2.getValue() != null ? rv2.getValue() : vv;
                                                }
                                                if (vv.length() > 120) vv = vv.substring(0, 120) + " …";
                                                parts.add(an + "=" + vv);
                                            } catch (Throwable te) {
                                                parts.add(an + "=");
                                            }
                                        }
                                    }
                                    String args = String.join(", ", parts);
                                    if (args.length() > 200) args = args.substring(0, 200) + " …";
                                    holder[0] = baseName + "(" + args + ")";
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.debug("formatCallstackFrames: evaluation failed, falling back", e);
                        } finally {
                            if (last) latch.countDown();
                        }
                    }

                    @Override public void tooManyChildren(int remaining) {}
                    @Override public void tooManyChildren(int remaining, @NotNull Runnable addNextChildren) {}
                    @Override public void setAlreadySorted(boolean alreadySorted) {}
                    @Override public void setErrorMessage(@NotNull String errorMessage) { if (latch.getCount() > 0) latch.countDown(); }
                    @Override public void setErrorMessage(@NotNull String errorMessage, @org.jetbrains.annotations.Nullable XDebuggerTreeNodeHyperlink link) { if (latch.getCount() > 0) latch.countDown(); }
                    @Override public void setMessage(@NotNull String message, @org.jetbrains.annotations.Nullable javax.swing.Icon icon, @NotNull SimpleTextAttributes attributes, @org.jetbrains.annotations.Nullable XDebuggerTreeNodeHyperlink link) {}
                });
                // Wait a little longer to reliably receive children in real sessions
                // (still bounded to keep tests fast when mocks don't call back)
                latch.await(250, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                LOGGER.debug("formatCallstackFrames: computeChildren failed, falling back", t);
            }
            out.add(holder[0]);
        }
        return out;
    }

}
