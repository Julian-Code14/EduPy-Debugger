package de.code14.edupydebugger;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.TestDataPath;
import com.intellij.util.PsiErrorElementUtil;

/**
 * @author julian
 * @version 1.0
 * @since 11.03.24
 */
@TestDataPath("$CONTENT_ROOT/src/test/testData")
public class MyPluginTest extends BasePlatformTestCase {

    public void testXMLFile() {
        var psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>");
        var xmlFile = assertInstanceOf(psiFile, XmlFile.class);

        assertFalse(PsiErrorElementUtil.hasErrors(getProject(), xmlFile.getVirtualFile()));

        assertNotNull(xmlFile.getRootTag());

        var rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            assertEquals("foo", rootTag.getName());
            assertEquals("bar", rootTag.getValue().getText());
        }
    }

    public void testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2");
    }

    public void testProjectService() {
        //MyProjectService projectService = ServiceManager.getService(getProject(), MyProjectService.class);

        //assertNotSame(projectService.getRandomNumber(), projectService.getRandomNumber());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData/rename";
    }
}
