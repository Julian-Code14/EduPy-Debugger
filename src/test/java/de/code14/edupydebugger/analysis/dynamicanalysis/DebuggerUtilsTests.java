package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.IconUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.jetbrains.python.debugger.*;
import de.code14.edupydebugger.DebuggerFixtureTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import javax.swing.*;

/**
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class DebuggerUtilsTests extends DebuggerFixtureTestCase {

    // TODO: write the tests for DebuggerUtilsTests class
    @Test
    public void testDummy() {
        assertEmpty("");
    }

    /*private Project project;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();


    }*/


    /*@Mock
    private XDebugSession mockDebugSession;

    @Mock
    private PyDebugProcess mockDebugProcess;

    @Mock
    private PyThreadInfo mockThreadInfo1;

    @Mock
    private PyThreadInfo mockThreadInfo2;

    @Mock
    private PyExecutionStack mockExecutionStack1;

    @Mock
    private PyExecutionStack mockExecutionStack2;

    @Mock
    private PyStackFrame mockStackFrame1;

    @Mock
    private PyStackFrame mockStackFrame2;

    @Mock
    private PyExecutionStackFactory mockStackFactory;

    private Collection<PyThreadInfo> threadInfos;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up mock debug session and debug process
        when(mockDebugSession.getDebugProcess()).thenReturn(mockDebugProcess);

        // Create list of threadInfos
        threadInfos = Arrays.asList(mockThreadInfo1, mockThreadInfo2);
        when(mockDebugProcess.getThreads()).thenReturn(threadInfos);

        // Mock the execution stack creation from the factory
        when(mockStackFactory.create(mockDebugProcess, mockThreadInfo1)).thenReturn(mockExecutionStack1);
        when(mockStackFactory.create(mockDebugProcess, mockThreadInfo2)).thenReturn(mockExecutionStack2);

        // Mock top stack frames for both execution stacks
        when(mockExecutionStack1.getTopFrame()).thenReturn(mockStackFrame1);
        when(mockExecutionStack2.getTopFrame()).thenReturn(mockStackFrame2);
    }

    @Test
    void testGetAllStackFrames_WithMultipleThreads() {
        // Arrange
        doAnswer(invocation -> {
            XExecutionStack.XStackFrameContainer container = invocation.getArgument(1);
            container.addStackFrames(Collections.emptyList(), true);
            return null;
        }).when(mockExecutionStack1).computeStackFrames(anyInt(), any());
        doAnswer(invocation -> {
            XExecutionStack.XStackFrameContainer container = invocation.getArgument(1);
            container.addStackFrames(Collections.emptyList(), true);
            return null;
        }).when(mockExecutionStack2).computeStackFrames(anyInt(), any());

        // Act
        List<PyStackFrame> result = DebuggerUtils.getAllStackFrames(mockDebugSession);

        // Assert
        assertEquals(2, result.size());
        assertEquals(mockStackFrame1, result.get(0));
        assertEquals(mockStackFrame2, result.get(1));
    }

    @Test
    void testGetAllStackFrames_WithEmptyStackFrames() {
        // Arrange
        when(mockExecutionStack1.getTopFrame()).thenReturn(null);
        when(mockExecutionStack2.getTopFrame()).thenReturn(null);

        doAnswer(invocation -> {
            XExecutionStack.XStackFrameContainer container = invocation.getArgument(1);
            container.addStackFrames(Collections.emptyList(), true);
            return null;
        }).when(mockExecutionStack1).computeStackFrames(anyInt(), any());
        doAnswer(invocation -> {
            XExecutionStack.XStackFrameContainer container = invocation.getArgument(1);
            container.addStackFrames(Collections.emptyList(), true);
            return null;
        }).when(mockExecutionStack2).computeStackFrames(anyInt(), any());

        // Act
        List<PyStackFrame> result = DebuggerUtils.getAllStackFrames(mockDebugSession);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void testGetAllStackFrames_WithAdditionalFrames() {
        // Arrange
        PyStackFrame additionalFrame1 = mock(PyStackFrame.class);
        PyStackFrame additionalFrame2 = mock(PyStackFrame.class);

        when(mockExecutionStack1.getTopFrame()).thenReturn(mockStackFrame1);
        when(mockExecutionStack2.getTopFrame()).thenReturn(mockStackFrame2);

        doAnswer(invocation -> {
            XExecutionStack.XStackFrameContainer container = invocation.getArgument(1);
            container.addStackFrames(Collections.singletonList(additionalFrame1), true);
            return null;
        }).when(mockExecutionStack1).computeStackFrames(anyInt(), any());

        doAnswer(invocation -> {
            XExecutionStack.XStackFrameContainer container = invocation.getArgument(1);
            container.addStackFrames(Collections.singletonList(additionalFrame2), true);
            return null;
        }).when(mockExecutionStack2).computeStackFrames(anyInt(), any());

        // Act
        List<PyStackFrame> result = DebuggerUtils.getAllStackFrames(mockDebugSession);

        // Assert
        assertEquals(4, result.size());
        assertEquals(mockStackFrame1, result.get(0));
        assertEquals(additionalFrame1, result.get(1));
        assertEquals(mockStackFrame2, result.get(2));
        assertEquals(additionalFrame2, result.get(3));
    }*/

}
