package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.jetbrains.python.debugger.PyDebugValue;
import com.jetbrains.python.debugger.PyDebuggerException;
import com.jetbrains.python.debugger.PyFrameAccessor;
import com.jetbrains.python.debugger.PyStackFrame;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class VariableAnalyzerTests {

    private PyStackFrame mockStackFrame;
    private PyDebugValue mockValue;
    private VariableAnalyzer variableAnalyzer;

    @Before
    public void setUp() {
        mockStackFrame = mock(PyStackFrame.class);
        mockValue = mock(PyDebugValue.class);
        variableAnalyzer = new VariableAnalyzer(Collections.singletonList(mockStackFrame));
    }

    @Test
    public void testAnalyzeVariables() throws PyDebuggerException {
        // Mock the stack frame and value interactions
        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(mockValue);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(mockValue.getName()).thenReturn("testVariable");
        when(mockValue.getType()).thenReturn("int");
        when(mockValue.getValue()).thenReturn("42");

        PyFrameAccessor mockFrameAccessor = mock(PyFrameAccessor.class);
        when(mockValue.getFrameAccessor()).thenReturn(mockFrameAccessor);

        // Simulate the ID retrieval and scope determination
        when(mockFrameAccessor.evaluate(eq("__builtins__.id(testVariable)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "1", false, null, false, false, false, null, mockFrameAccessor));
        when(mockFrameAccessor.evaluate(eq("locals().get('testVariable', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "true", false, null, false, false, false, null, mockFrameAccessor));
        when(mockFrameAccessor.evaluate(eq("globals().get('testVariable', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, mockFrameAccessor));

        // Perform the analysis
        variableAnalyzer.analyzeVariables();

        // Verify the results
        Map<String, List<String>> variables = variableAnalyzer.getVariables();
        assertNotNull(variables);
        assertEquals(1, variables.size());

        List<String> variableInfo = variables.get("1");
        assertNotNull(variableInfo);
        assertEquals(4, variableInfo.size());
        assertEquals("testVariable", variableInfo.get(0));
        assertEquals("int", variableInfo.get(1));
        assertEquals("42", variableInfo.get(2));
        assertEquals("local", variableInfo.get(3));
    }

    @Test
    public void testAnalyzeVariablesHandlesGlobalScope() throws PyDebuggerException {
        // Mock the stack frame and value interactions
        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(mockValue);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(mockValue.getName()).thenReturn("testVariable");
        when(mockValue.getType()).thenReturn("int");
        when(mockValue.getValue()).thenReturn("42");

        PyFrameAccessor mockFrameAccessor = mock(PyFrameAccessor.class);
        when(mockValue.getFrameAccessor()).thenReturn(mockFrameAccessor);

        // Simulate the ID retrieval and scope determination
        when(mockFrameAccessor.evaluate(eq("__builtins__.id(testVariable)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "2", false, null, false, false, false, null, mockFrameAccessor));
        when(mockFrameAccessor.evaluate(eq("locals().get('testVariable', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "false", false, null, false, false, false, null, mockFrameAccessor));
        when(mockFrameAccessor.evaluate(eq("globals().get('testVariable', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "true", false, null, false, false, false, null, mockFrameAccessor));

        // Perform the analysis
        variableAnalyzer.analyzeVariables();

        // Verify the results
        Map<String, List<String>> variables = variableAnalyzer.getVariables();
        assertNotNull(variables);
        assertEquals(1, variables.size());

        List<String> variableInfo = variables.get("2");
        assertNotNull(variableInfo);
        assertEquals(4, variableInfo.size());
        assertEquals("testVariable", variableInfo.get(0));
        assertEquals("int", variableInfo.get(1));
        assertEquals("42", variableInfo.get(2));
        assertEquals("global", variableInfo.get(3));
    }

    @Test
    public void testAnalyzeVariablesHandlesUnknownScope() throws PyDebuggerException {
        // Mock the stack frame and value interactions
        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(mockValue);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(mockValue.getName()).thenReturn("testVariable");
        when(mockValue.getType()).thenReturn("int");
        when(mockValue.getValue()).thenReturn("42");

        PyFrameAccessor mockFrameAccessor = mock(PyFrameAccessor.class);
        when(mockValue.getFrameAccessor()).thenReturn(mockFrameAccessor);

        // Simulate the ID retrieval and scope determination
        when(mockFrameAccessor.evaluate(eq("__builtins__.id(testVariable)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "3", false, null, false, false, false, null, mockFrameAccessor));
        when(mockFrameAccessor.evaluate(eq("locals().get('testVariable', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "false", false, null, false, false, false, null, mockFrameAccessor));
        when(mockFrameAccessor.evaluate(eq("globals().get('testVariable', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, mockFrameAccessor));

        // Perform the analysis
        variableAnalyzer.analyzeVariables();

        // Verify the results
        Map<String, List<String>> variables = variableAnalyzer.getVariables();
        assertNotNull(variables);
        assertEquals(1, variables.size());

        List<String> variableInfo = variables.get("3");
        assertNotNull(variableInfo);
        assertEquals(4, variableInfo.size());
        assertEquals("testVariable", variableInfo.get(0));
        assertEquals("int", variableInfo.get(1));
        assertEquals("42", variableInfo.get(2));
        assertEquals("unknown", variableInfo.get(3));
    }

    @Test
    public void testAnalyzeVariables_enrichesGlobalsEvenWhenLocalScope() throws PyDebuggerException {
        // Arrange: one local variable to provide an evaluation context
        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(mockValue);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(mockValue.getName()).thenReturn("localVar");
        when(mockValue.getType()).thenReturn("int");
        when(mockValue.getValue()).thenReturn("1");

        PyFrameAccessor mockAccessor = mock(PyFrameAccessor.class);
        when(mockValue.getFrameAccessor()).thenReturn(mockAccessor);

        // Local id/scope
        when(mockAccessor.evaluate(eq("__builtins__.id(localVar)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "11", false, null, false, false, false, null, mockAccessor));
        when(mockAccessor.evaluate(eq("locals().get('localVar', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "true", false, null, false, false, false, null, mockAccessor));
        when(mockAccessor.evaluate(eq("globals().get('localVar', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, mockAccessor));

        // Global enrichment: globals list contains 'g' and a built-in-like name that should be skipped
        when(mockAccessor.evaluate(eq("','.join([k for k in globals().keys()])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("globals", "str", null, "g,__name__", false, null, false, false, false, null, mockAccessor));

        // Evaluate global 'g'
        PyDebugValue gVal = new PyDebugValue("g", "int", null, "7", false, null, false, false, false, null, mockAccessor);
        when(mockAccessor.evaluate(eq("globals().get('g', None)"), anyBoolean(), anyBoolean())).thenReturn(gVal);
        // ID for global g
        when(mockAccessor.evaluate(eq("__builtins__.id(globals()['g'])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "77", false, null, false, false, false, null, mockAccessor));

        // '__name__' is skipped by type/module
        when(mockAccessor.evaluate(eq("globals().get('__name__', None)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("__name__", "module", null, "<module>", false, null, false, false, false, null, mockAccessor));

        // Act
        variableAnalyzer.analyzeVariables();

        // Assert: contains both local (11) and global (77)
        Map<String, List<String>> vars = variableAnalyzer.getVariables();
        assertEquals(2, vars.size());
        assertEquals("local", vars.get("11").get(3));
        assertEquals("global", vars.get("77").get(3));
        assertEquals("7", vars.get("77").get(2));
    }

    @Test
    public void testAnalyzeVariables_skipsSystemAndDunderGlobals() throws PyDebuggerException {
        // Arrange
        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(mockValue); // local provides eval context
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(mockValue.getName()).thenReturn("dummyLocal");
        when(mockValue.getType()).thenReturn("int");
        when(mockValue.getValue()).thenReturn("0");

        PyFrameAccessor acc = mock(PyFrameAccessor.class);
        when(mockValue.getFrameAccessor()).thenReturn(acc);

        // local id + scope
        when(acc.evaluate(eq("__builtins__.id(dummyLocal)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "1", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("locals().get('dummyLocal', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "true", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("globals().get('dummyLocal', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, acc));

        // globals list includes various system names that must be skipped
        when(acc.evaluate(eq("','.join([k for k in globals().keys()])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("globals", "str", null,
                        "__name__,__file__,__package__,__loader__,__doc__,__builtins__,__py_debug_temp_var_123,user",
                        false, null, false, false, false, null, acc));

        // user is a real global to be included
        PyDebugValue userVal = new PyDebugValue("user", "int", null, "99", false, null, false, false, false, null, acc);
        when(acc.evaluate(eq("globals().get('user', None)"), anyBoolean(), anyBoolean())).thenReturn(userVal);
        when(acc.evaluate(eq("__builtins__.id(globals()['user'])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "99", false, null, false, false, false, null, acc));

        variableAnalyzer.analyzeVariables();

        Map<String, List<String>> vars = variableAnalyzer.getVariables();
        // Should contain only the local and 'user' global, not the dunders/debug temp
        assertEquals(2, vars.size());
        assertTrue(vars.containsKey("1"));
        assertTrue(vars.containsKey("99"));
    }

    @Test
    public void testAnalyzeVariables_globalsContainersHaveReprWhenPausedLocally() throws PyDebuggerException {
        // Arrange an eval context
        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(mockValue);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(mockValue.getName()).thenReturn("local");
        when(mockValue.getType()).thenReturn("int");
        when(mockValue.getValue()).thenReturn("0");

        PyFrameAccessor acc = mock(PyFrameAccessor.class);
        when(mockValue.getFrameAccessor()).thenReturn(acc);

        when(acc.evaluate(eq("__builtins__.id(local)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "10", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("locals().get('local', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "true", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("globals().get('local', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, acc));

        when(acc.evaluate(eq("','.join([k for k in globals().keys()])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("globals", "str", null, "nums", false, null, false, false, false, null, acc));

        PyDebugValue gv = new PyDebugValue("nums", "list", null, null, false, null, false, false, false, null, acc);
        when(acc.evaluate(eq("globals().get('nums', None)"), anyBoolean(), anyBoolean())).thenReturn(gv);
        when(acc.evaluate(eq("__builtins__.id(globals()['nums'])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "555", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("repr(globals()['nums'])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("repr", "str", null, "[1, 2, 3]", false, null, false, false, false, null, acc));

        variableAnalyzer.analyzeVariables();
        Map<String, List<String>> vars = variableAnalyzer.getVariables();
        String idForNums = null;
        for (Map.Entry<String, List<String>> e : vars.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty() && e.getValue().get(0).contains("nums")) {
                idForNums = e.getKey();
                break;
            }
        }
        assertNotNull(idForNums);
        assertEquals("[1, 2, 3]", vars.get(idForNums).get(2).replace("~", ", "));
        assertEquals("global", vars.get(idForNums).get(3));
    }

    @Test
    public void testAnalyzeVariables_skipsDebuggerInjectedLocals() throws PyDebuggerException {
        PyDebugValue sys = mock(PyDebugValue.class);
        PyDebugValue user = mock(PyDebugValue.class);

        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(sys);
            childrenList.add(user);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(sys.getName()).thenReturn("__py_deb..."); // truncated style name
        when(sys.getType()).thenReturn("int");
        when(sys.getValue()).thenReturn("123");

        when(user.getName()).thenReturn("x");
        when(user.getType()).thenReturn("int");
        when(user.getValue()).thenReturn("5");

        PyFrameAccessor acc = mock(PyFrameAccessor.class);
        when(user.getFrameAccessor()).thenReturn(acc);
        when(acc.evaluate(eq("__builtins__.id(x)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "9", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("locals().get('x', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "true", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("globals().get('x', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, acc));

        variableAnalyzer.analyzeVariables();
        Map<String, List<String>> vars = variableAnalyzer.getVariables();
        assertEquals(1, vars.size());
        assertTrue(vars.containsKey("9"));
    }

    @Test
    public void testAnalyzeVariables_skipsHighlyTruncatedPyNames() throws PyDebuggerException {
        PyStackFrame frame = mock(PyStackFrame.class);
        VariableAnalyzer analyzer = new VariableAnalyzer(Collections.singletonList(frame));

        PyDebugValue sys = mock(PyDebugValue.class);
        PyDebugValue user = mock(PyDebugValue.class);

        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(sys);
            childrenList.add(user);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(frame).computeChildren(any(XCompositeNode.class));

        when(sys.getName()).thenReturn("__py...");
        when(sys.getType()).thenReturn("int");
        when(sys.getValue()).thenReturn("123");

        when(user.getName()).thenReturn("ok");
        when(user.getType()).thenReturn("int");
        when(user.getValue()).thenReturn("5");

        PyFrameAccessor acc = mock(PyFrameAccessor.class);
        when(user.getFrameAccessor()).thenReturn(acc);
        when(acc.evaluate(eq("__builtins__.id(ok)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "77", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("locals().get('ok', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "true", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("globals().get('ok', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, acc));

        analyzer.analyzeVariables();
        Map<String, List<String>> vars = analyzer.getVariables();
        assertEquals(1, vars.size());
        assertTrue(vars.containsKey("77"));
    }

    @Test
    public void testAnalyzeVariables_skipsUnderscoreGlobal() throws PyDebuggerException {
        // Arrange an eval context with a local dummy and globals containing '_' and 'user'
        PyDebugValue dummyLocal = mock(PyDebugValue.class);
        doAnswer(invocation -> {
            XValueChildrenList childrenList = new XValueChildrenList();
            childrenList.add(dummyLocal);
            invocation.getArgument(0, XCompositeNode.class).addChildren(childrenList, true);
            return null;
        }).when(mockStackFrame).computeChildren(any(XCompositeNode.class));

        when(dummyLocal.getName()).thenReturn("dummyLocal");
        when(dummyLocal.getType()).thenReturn("int");
        when(dummyLocal.getValue()).thenReturn("0");

        PyFrameAccessor acc = mock(PyFrameAccessor.class);
        when(dummyLocal.getFrameAccessor()).thenReturn(acc);

        when(acc.evaluate(eq("__builtins__.id(dummyLocal)"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "1", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("locals().get('dummyLocal', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isLocal", "bool", null, "true", false, null, false, false, false, null, acc));
        when(acc.evaluate(eq("globals().get('dummyLocal', None) is not None"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("isGlobal", "bool", null, "false", false, null, false, false, false, null, acc));

        // Globals contain '_' (should be skipped) and 'user' (should be included)
        when(acc.evaluate(eq("','.join([k for k in globals().keys()])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("globals", "str", null, "_,user", false, null, false, false, false, null, acc));

        PyDebugValue userVal = new PyDebugValue("user", "int", null, "99", false, null, false, false, false, null, acc);
        when(acc.evaluate(eq("globals().get('user', None)"), anyBoolean(), anyBoolean())).thenReturn(userVal);
        when(acc.evaluate(eq("__builtins__.id(globals()['user'])"), anyBoolean(), anyBoolean()))
                .thenReturn(new PyDebugValue("id", "int", null, "99", false, null, false, false, false, null, acc));

        variableAnalyzer.analyzeVariables();
        Map<String, List<String>> vars = variableAnalyzer.getVariables();
        assertEquals(2, vars.size());
        assertTrue(vars.containsKey("1")); // dummyLocal
        assertTrue(vars.containsKey("99")); // user
    }
}
