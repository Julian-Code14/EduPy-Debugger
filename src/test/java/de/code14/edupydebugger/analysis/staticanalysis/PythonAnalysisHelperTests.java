package de.code14.edupydebugger.analysis.staticanalysis;

import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


public class PythonAnalysisHelperTests {

    @Mock
    private TypeEvalContext mockContext;

    @Mock
    private PyTargetExpression mockTargetExpression;

    @Mock
    private PyNamedParameter mockNamedParameter;

    @Mock
    private PyFunction mockFunction;

    @Mock
    private PyType mockType;

    @Mock
    private PyParameterList mockParameterList;

    @Mock
    private PyDecoratorList mockDecoratorList;

    @Mock
    private PyParameter mockParameter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetTypeString_TargetExpression() {
        when(mockContext.getType(mockTargetExpression)).thenReturn(mockType);
        when(mockType.getName()).thenReturn("int");

        String result = PythonAnalysisHelper.getTypeString(mockTargetExpression, mockContext);
        assertEquals("int", result);
    }

    @Test
    public void testGetTypeString_NamedParameter() {
        when(mockContext.getType(mockNamedParameter)).thenReturn(mockType);
        when(mockType.getName()).thenReturn("str");

        String result = PythonAnalysisHelper.getTypeString(mockNamedParameter, mockContext);
        assertEquals("str", result);
    }

    @Test
    public void testGetTypeString_Function() {
        when(mockContext.getReturnType(mockFunction)).thenReturn(mockType);
        when(mockType.getName()).thenReturn("bool");

        String result = PythonAnalysisHelper.getTypeString(mockFunction, mockContext);
        assertEquals("bool", result);
    }

    @Test
    public void testGetTypeString_UnknownElement() {
        PyElement unknownElement = mock(PyElement.class);

        String result = PythonAnalysisHelper.getTypeString(unknownElement, mockContext);
        assertEquals("?", result);
    }

    @Test
    public void testDetermineVisibility_Public() {
        String result = PythonAnalysisHelper.determineVisibility("publicMethod");
        assertEquals("+", result);
    }

    @Test
    public void testDetermineVisibility_Protected() {
        String result = PythonAnalysisHelper.determineVisibility("_protectedMethod");
        assertEquals("#", result);
    }

    @Test
    public void testDetermineVisibility_Private() {
        String result = PythonAnalysisHelper.determineVisibility("__privateMethod");
        assertEquals("-", result);
    }

    @Test
    public void testGetMethodSignature_NoParams() {
        when(mockFunction.getName()).thenReturn("testMethod");
        when(mockFunction.getParameterList()).thenReturn(mockParameterList);
        when(mockParameterList.getParameters()).thenReturn(new PyParameter[0]);
        when(mockContext.getReturnType(mockFunction)).thenReturn(mockType);
        when(mockType.getName()).thenReturn("void");

        String result = PythonAnalysisHelper.getMethodSignature(mockFunction, mockContext);
        assertEquals("+testMethod() : void", result);
    }

    @Test
    public void testGetMethodSignature_WithParams() {
        when(mockFunction.getName()).thenReturn("testMethod");
        when(mockFunction.getParameterList()).thenReturn(mockParameterList);
        when(mockParameterList.getParameters()).thenReturn(new PyParameter[]{mockParameter});
        when(mockParameter.getName()).thenReturn("param");
        when(mockContext.getReturnType(mockFunction)).thenReturn(mockType);
        when(mockType.getName()).thenReturn("void");

        String result = PythonAnalysisHelper.getMethodSignature(mockFunction, mockContext);
        assertEquals("+testMethod(param) : void", result);

        verify(mockContext).getReturnType(mockFunction);
    }

    @Test
    public void testGetMethodSignature_StaticMethod() {
        when(mockFunction.getName()).thenReturn("staticMethod");
        when(mockFunction.getParameterList()).thenReturn(mockParameterList);
        when(mockParameterList.getParameters()).thenReturn(new PyParameter[0]);
        when(mockFunction.getDecoratorList()).thenReturn(mockDecoratorList);
        when(mockDecoratorList.findDecorator("staticmethod")).thenReturn(mock(PyDecorator.class));
        when(mockContext.getReturnType(mockFunction)).thenReturn(mockType);
        when(mockType.getName()).thenReturn("void");

        String result = PythonAnalysisHelper.getMethodSignature(mockFunction, mockContext);
        assertEquals("{static} +staticMethod() : void", result);
    }

    @Test
    public void testGetMethodSignature_AbstractMethod() {
        when(mockFunction.getName()).thenReturn("abstractMethod");
        when(mockFunction.getParameterList()).thenReturn(mockParameterList);
        when(mockParameterList.getParameters()).thenReturn(new PyParameter[0]);
        when(mockFunction.getDecoratorList()).thenReturn(mockDecoratorList);
        when(mockDecoratorList.findDecorator("abstractmethod")).thenReturn(mock(PyDecorator.class));
        when(mockContext.getReturnType(mockFunction)).thenReturn(mockType);
        when(mockType.getName()).thenReturn("void");

        String result = PythonAnalysisHelper.getMethodSignature(mockFunction, mockContext);
        assertEquals("{abstract} +abstractMethod() : void", result);
    }

}

