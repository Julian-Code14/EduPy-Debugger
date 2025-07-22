package de.code14.edupydebugger.analysis.staticanalysis;

import com.intellij.mock.MockProject;
import com.intellij.mock.MockProjectEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.*;
import com.intellij.testFramework.fixtures.*;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class PythonAnalyzerTests extends LightPlatformTestCase {

    // TODO: write the tests for PythonAnalyzerTests class

}


