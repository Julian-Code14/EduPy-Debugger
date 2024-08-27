package de.code14.edupydebugger;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.jetbrains.python.debugger.PyLineBreakpointType;
import com.jetbrains.python.run.PythonConfigurationType;
import com.jetbrains.python.run.PythonRunConfiguration;
import com.jetbrains.python.sdk.*;
import org.junit.Test;

/**
 * @author julian
 * @version 1.0
 * @since 21.08.24
 */
public class DebuggerFixtureTestCase extends HeavyPlatformTestCase {

    // TODO: write the tests for DebuggerFixtureTestCase class
    /*protected CodeInsightTestFixture myFixture;
    protected Project project;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Setup the fixture
        IdeaTestFixtureFactory fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = fixtureFactory.createFixtureBuilder(getName());

        myFixture = fixtureFactory.createCodeInsightFixture(fixtureBuilder.getFixture());
        myFixture.setUp();

        myFixture.setTestDataPath("src/test/testData/");
        myFixture.copyDirectoryToProject("dummy_project", "dummy_project");
        myFixture.copyDirectoryToProject("dummy_project/.venv", "dummy_project/.venv");


        ApplicationManager.getApplication().invokeAndWait(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                XBreakpointManager breakpointManager = XDebuggerManager.getInstance(getProject()).getBreakpointManager();
                VirtualFile pythonFile = myFixture.findFileInTempDir("dummy_project/contact_address.py");
                PsiTestUtil.addSourceRoot(getModule(), pythonFile.getParent());
                project = getProject();

                PyLineBreakpointType pyLineBreakpointType = new PyLineBreakpointType();
                XLineBreakpoint<?> breakpoint = breakpointManager.addLineBreakpoint(pyLineBreakpointType, pythonFile.getUrl(), 10, null);
                assertNotNull("Breakpoint not set", breakpoint);
            });
        }, ModalityState.defaultModalityState());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // Clean up the SDK after the test
            Sdk sdk = findOrCreateVirtualEnvSdk();
            if (sdk != null) {
                ApplicationManager.getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().removeJdk(sdk));
            }
        } finally {
            myFixture.tearDown();
            super.tearDown();
        }
    }

    @Override
    public String getName() {
        return "DebuggerFixtureTestCase";
    }

    protected PythonRunConfiguration createDebugConfiguration(Project project) {
        PythonConfigurationType pythonRunConfigType = PythonConfigurationType.getInstance();

        // Create a run configuration
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createConfiguration(
                "Debug contact_address.py", pythonRunConfigType.getFactory());

        PythonRunConfiguration config = (PythonRunConfiguration) settings.getConfiguration();
        config.setScriptName(myFixture.findFileInTempDir("dummy_project/contact_address.py").getPath());

        // Assign the SDK from the local .venv
        Sdk sdk = findOrCreateVirtualEnvSdk();
        if (sdk != null) {
            config.setSdk(sdk);
        } else {
            fail("Python SDK from the virtual environment (.venv) could not be found.");
        }

        return config;
    }

    private Sdk findOrCreateVirtualEnvSdk() {
        // Locate the virtual environment folder
        VirtualFile venvFolder = myFixture.findFileInTempDir("dummy_project/.venv");
        assertNotNull("Virtual environment folder not found", venvFolder);

        if (venvFolder.exists()) {
            VirtualFile pythonInterpreter = venvFolder.findFileByRelativePath("bin/python");
            if (pythonInterpreter == null) {
                pythonInterpreter = venvFolder.findFileByRelativePath("Scripts/python.exe");
            }

            assertNotNull("Python interpreter not found in virtual environment", pythonInterpreter);

            String sdkHomePath = pythonInterpreter.getPath();
            String sdkKey = "Python SDK: " + sdkHomePath;

            // Check if SDK already exists
            Sdk existingSdk = ProjectJdkTable.getInstance().findJdk(sdkKey);
            if (existingSdk != null) {
                return existingSdk;
            }

            // Create and register the SDK
            Sdk sdk = new ProjectJdkImpl(sdkKey, PythonSdkType.getInstance(), sdkHomePath, null);
            SdkModificator sdkModificator = sdk.getSdkModificator();
            sdkModificator.setVersionString(PythonSdkType.getInstance().getVersionString(sdkHomePath));
            sdkModificator.commitChanges();

            ApplicationManager.getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().addJdk(sdk));

            return sdk;
        }

        return null;
    }

    protected void startDebugProcess(Project project) {
        try {
            Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
            PythonRunConfiguration config = createDebugConfiguration(project);

            // Create the execution environment
            ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(executor, config);
            ExecutionEnvironment env = builder.build();

            // Find and execute the appropriate program runner
            ProgramRunner<?> runner = ProgramRunner.getRunner(executor.getId(), config);
            if (runner != null) {
                runner.execute(env);
            } else {
                fail("No suitable ProgramRunner found for the configuration.");
            }

        } catch (ExecutionException e) {
            fail("Failed to start debug process: " + e.getMessage());
        }
    }

    @Test
    public void testDebugger() {
        EdtTestUtil.runInEdtAndWait(() -> {
            try {
                startDebugProcess(project);

                // Wait for the breakpoint to be hit
                XBreakpointManager breakpointManager = XDebuggerManager.getInstance(getProject()).getBreakpointManager();
                assertTrue("No breakpoints set", breakpointManager.getAllBreakpoints().length > 0);

            } catch (Exception e) {
                fail("Debugger start failed: " + e.getMessage());
            }
        });
    }*/
}
