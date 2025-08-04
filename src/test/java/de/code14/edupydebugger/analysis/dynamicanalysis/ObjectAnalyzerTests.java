package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.xdebugger.frame.*;
import com.jetbrains.python.debugger.*;
import org.junit.*;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class ObjectAnalyzerTests {

    @Test
    public void testAnalyzeObjectsCollectsUserDefinedInstances() throws PyDebuggerException {
        // ---------- 1) PyDebugValue "foo" mit Deep-Stub ----------------
        PyDebugValue fooValue = mock(PyDebugValue.class, RETURNS_DEEP_STUBS);
        when(fooValue.getName()).thenReturn("foo");
        when(fooValue.getType()).thenReturn("Foo");
        when(fooValue.getValue()).thenReturn("Foo object");

        // id(foo)   -> "42"
        when(fooValue.getFrameAccessor()
                .evaluate(eq("id(foo)"), anyBoolean(), anyBoolean())
                .getValue()).thenReturn("42");
        // dir(foo)  -> "['x', '__str__']"
        when(fooValue.getFrameAccessor()
                .evaluate(eq("dir(foo)"), anyBoolean(), anyBoolean())
                .getValue()).thenReturn("['x', '__str__']");
        when(fooValue.getFrameAccessor()
                .evaluate(eq("isinstance(foo, object) and not isinstance(foo, (int, float, str, bool, list, dict, tuple, set))"),
                        anyBoolean(), anyBoolean())
                .getValue()).thenReturn("True");

        // foo.x     -> int-Wert 7
        PyDebugValue attrVal = mock(PyDebugValue.class);
        when(attrVal.getType()).thenReturn("int");
        when(attrVal.getValue()).thenReturn("7");
        when(fooValue.getFrameAccessor()
                .evaluate(eq("foo.x"), anyBoolean(), anyBoolean()))
                .thenReturn(attrVal);

        // ---------- 2) Children-Liste ---------------------------------
        XValueChildrenList children = new XValueChildrenList();
        children.add(fooValue);

        // ---------- 3) Mock-Stack-Frame liefert obige Children --------
        PyStackFrame pyFrame = mock(PyStackFrame.class);
        doAnswer(inv -> {                     // computeChildren(node)
            XCompositeNode node = inv.getArgument(0);
            node.addChildren(children, true);
            return null;
        }).when(pyFrame).computeChildren(any());

        // ---------- Ausführen -----------------------------------------
        ObjectAnalyzer analyzer = new ObjectAnalyzer(List.of(pyFrame));
        analyzer.analyzeObjects();

        Map<String, ObjectInfo> objs = analyzer.getObjects();
        assertEquals(1, objs.size());

        ObjectInfo info = objs.values().iterator().next();
        assertEquals(List.of("foo:Foo"), info.references());

        AttributeInfo attr = info.attributes().get(0);
        assertEquals("x",   attr.name());
        assertEquals("int", attr.type());
        assertEquals("7",   attr.value());
    }
}