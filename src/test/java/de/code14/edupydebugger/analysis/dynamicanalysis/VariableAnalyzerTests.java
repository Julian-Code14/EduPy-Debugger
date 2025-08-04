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
}

