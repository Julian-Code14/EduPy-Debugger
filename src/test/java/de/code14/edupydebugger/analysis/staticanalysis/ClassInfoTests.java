package de.code14.edupydebugger.analysis.staticanalysis;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class ClassInfoTests {

    @Test
    public void testClassInfoCreation() {
        List<String> attributes = List.of("attr1", "attr2");
        List<String> methods = List.of("method1()", "method2()");
        List<String> references = List.of("ref1", "ref2");
        List<String> superClasses = List.of("SuperClass1", "SuperClass2");

        ClassInfo classInfo = new ClassInfo(attributes, methods, references, superClasses);

        assertEquals(attributes, classInfo.attributes());
        assertEquals(methods, classInfo.methods());
        assertEquals(references, classInfo.references());
        assertEquals(superClasses, classInfo.superClasses());
    }

    @Test
    public void testClassInfoEquality() {
        List<String> attributes = List.of("attr1", "attr2");
        List<String> methods = List.of("method1()", "method2()");
        List<String> references = List.of("ref1", "ref2");
        List<String> superClasses = List.of("SuperClass1", "SuperClass2");

        ClassInfo classInfo1 = new ClassInfo(attributes, methods, references, superClasses);
        ClassInfo classInfo2 = new ClassInfo(attributes, methods, references, superClasses);

        assertEquals(classInfo1, classInfo2);
        assertEquals(classInfo1.hashCode(), classInfo2.hashCode());
    }

    @Test
    public void testClassInfoToString() {
        List<String> attributes = List.of("attr1", "attr2");
        List<String> methods = List.of("method1()", "method2()");
        List<String> references = List.of("ref1", "ref2");
        List<String> superClasses = List.of("SuperClass1", "SuperClass2");

        ClassInfo classInfo = new ClassInfo(attributes, methods, references, superClasses);

        String expectedToString = "ClassInfo[attributes=[attr1, attr2], methods=[method1(), method2()], references=[ref1, ref2], superClasses=[SuperClass1, SuperClass2]]";
        assertEquals(expectedToString, classInfo.toString());
    }

}

