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

    protected CodeInsightTestFixture myFixture;
    protected Project project;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Erstellen des Fixtures mit dem Fixture-Builder
        IdeaTestFixtureFactory fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = fixtureFactory.createFixtureBuilder(getName());

        myFixture = fixtureFactory.createCodeInsightFixture(fixtureBuilder.getFixture());

        // Setzen des Fixtures, initialisieren der Umgebung
        myFixture.setUp();

        myFixture.setTestDataPath("src/test/testData/");
        myFixture.copyDirectoryToProject("dummy_project", "dummy_project");

        ApplicationManager.getApplication().invokeAndWait(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                XBreakpointManager breakpointManager = XDebuggerManager.getInstance(getProject()).getBreakpointManager();
                VirtualFile pythonFile = myFixture.findFileInTempDir("dummy_project/contact_address.py");
                PsiTestUtil.addSourceRoot(getModule(), pythonFile.getParent());
                project = getProject();

                PyLineBreakpointType pyLineBreakpointType = new PyLineBreakpointType();
                XLineBreakpoint<?> breakpoint = breakpointManager.addLineBreakpoint(pyLineBreakpointType, pythonFile.getUrl(), 10, null);
                assertNotNull(breakpoint);
            });
        }, ModalityState.defaultModalityState());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // Entsorge das SDK nach dem Test, um Lecks zu vermeiden
            Sdk sdk = findOrCreateVirtualEnvSdk();
            if (sdk != null) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    ProjectJdkTable.getInstance().removeJdk(sdk);
                });
            }
        } finally {
            // Fixture abbauen und weitere Aufräumarbeiten
            myFixture.tearDown();
            super.tearDown();
        }
    }

    @Override
    public String getName() {
        return "DebuggerFixtureTestCase";
    }

    protected PythonRunConfiguration createDebugConfiguration(Project project) {
        // Hole den PythonRunConfigurationType
        PythonConfigurationType pythonRunConfigType = PythonConfigurationType.getInstance();

        // Erstelle eine Run-Konfiguration
        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings settings = runManager.createConfiguration("Debug contact_address.py", pythonRunConfigType.getFactory());

        PythonRunConfiguration config = (PythonRunConfiguration) settings.getConfiguration();

        // Setze das Skript, das debuggt werden soll
        config.setScriptName(myFixture.findFileInTempDir("dummy_project/contact_address.py").getPath());

        // Lade das SDK aus dem .venv Ordner und weise es der Run-Konfiguration zu
        Sdk sdk = findOrCreateVirtualEnvSdk();
        if (sdk != null) {
            config.setSdk(sdk);
        } else {
            fail("Python SDK from the virtual environment (.venv) could not be found.");
        }

        return config;
    }

    private Sdk findOrCreateVirtualEnvSdk() {
        // Hole den Pfad zur virtuellen Umgebung
        VirtualFile venvFolder = myFixture.findFileInTempDir("dummy_project/.venv");
        assertNotNull("Virtual environment folder not found", venvFolder);

        if (venvFolder.exists()) {
            VirtualFile pythonInterpreter = venvFolder.findFileByRelativePath("bin/python");
            if (pythonInterpreter == null) {
                pythonInterpreter = venvFolder.findFileByRelativePath("Scripts/python.exe");
            }

            assertNotNull("Python interpreter not found in virtual environment", pythonInterpreter);

            String sdkHomePath = pythonInterpreter.getPath();

            // Erstelle einen eindeutigen Schlüssel für das SDK
            String sdkKey = "Python SDK: " + sdkHomePath;

            // Überprüfe, ob ein SDK mit diesem Schlüssel bereits existiert
            Sdk existingSdk = ProjectJdkTable.getInstance().findJdk(sdkKey);
            if (existingSdk != null) {
                return existingSdk;
            }

            // SDK erstellen und registrieren
            Sdk sdk = new ProjectJdkImpl(sdkKey, PythonSdkType.getInstance(), sdkHomePath, null);
            SdkModificator sdkModificator = sdk.getSdkModificator();
            sdkModificator.setVersionString(PythonSdkType.getInstance().getVersionString(sdkHomePath));
            sdkModificator.commitChanges();

            ApplicationManager.getApplication().runWriteAction(() -> {
                ProjectJdkTable.getInstance().addJdk(sdk);
            });

            return sdk;
        }
        return null;
    }

    protected void startDebugProcess(Project project) {
        try {
            // Erstelle den Debug Executor
            Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();

            // Hole die Debug-Konfiguration
            PythonRunConfiguration config = createDebugConfiguration(project);

            // Erstelle die Ausführungsumgebung
            ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(executor, config);
            ExecutionEnvironment env = builder.build();

            // Finde den passenden ProgramRunner für die gegebene RunConfiguration
            ProgramRunner<?> runner = ProgramRunner.getRunner(executor.getId(), config);

            if (runner != null) {
                // Verwende die neuere API: Starte den Debug-Prozess mit dem Executor und der Umgebung
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
                // Starte den Debugger
                startDebugProcess(project);

                // Warte darauf, dass der Breakpoint getroffen wird
                XBreakpointManager breakpointManager = XDebuggerManager.getInstance(getProject()).getBreakpointManager();
                assertTrue(breakpointManager.getAllBreakpoints().length > 0);

                // Simuliere, dass der Breakpoint im Debugger getroffen wird (Dummy-Aktion)
            } catch (Exception e) {
                fail("Debugger start failed: " + e.getMessage());
            }
        });
    }

}
