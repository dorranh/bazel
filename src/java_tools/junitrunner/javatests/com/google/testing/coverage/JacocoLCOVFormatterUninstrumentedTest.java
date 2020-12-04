// Copyright 2020 The Bazel Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.testing.coverage;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests the uninstrumented class processing logic in {@link JacocoLCOVFormatter}.
 */
@RunWith(JUnit4.class)
public class JacocoLCOVFormatterUninstrumentedTest {

  private StringWriter writer;
  private IBundleCoverage mockBundle;

  private static IClassCoverage mockIClassCoverage(String className, String packageName, String sourceFileName) {
    IClassCoverage mocked = mock(IClassCoverage.class);
    when(mocked.getName()).thenReturn(className);
    when(mocked.getPackageName()).thenReturn(packageName);
    when(mocked.getSourceFileName()).thenReturn(sourceFileName);
    return mocked;
  }

  private Description createSuiteDescription(String name) {
    Description suite = Description.createSuiteDescription(name);
    suite.addChild(Description.createTestDescription(Object.class, "child"));
    return suite;
  }

  @Before
  public void setupTest() {
    // Initialize writer for storing coverage report outputs
    writer = new StringWriter();
    // Initialize mock Jacoco bundle containing the mock coverage
    // Classes
    List<IClassCoverage> mockClassCoverages = Arrays.asList(
      mockIClassCoverage("Foo", "com/example", "Foo.java")
    );
    // Package
    IPackageCoverage mockPackageCoverage = mock(IPackageCoverage.class);
    when(mockPackageCoverage.getClasses()).thenReturn(mockClassCoverages);
    // Bundle
    mockBundle = mock(IBundleCoverage.class);
    when(mockBundle.getPackages()).thenReturn(Arrays.asList(mockPackageCoverage));
  }

  @Test
  public void testVisitBundleWithSimpleUnixPath() {
    // Paths
    ImmutableSet<String> execPaths = ImmutableSet.of(
      "/parent/dir/com/example/Foo.java"
    );
    JacocoLCOVFormatter formatter = new JacocoLCOVFormatter(execPaths);
    IReportVisitor visitor = formatter.createVisitor(new PrintWriter(writer), new TreeMap<String, BranchCoverageDetail>());
    try {
      visitor.visitBundle(mockBundle, mock(ISourceFileLocator.class));
      visitor.visitEnd();
    } catch (IOException e) {
      Assert.fail(Throwables.getStackTraceAsString(e));
    }
    String coverageOutput = writer.toString();
    for (String sourcePath : execPaths) {
      assertThat(coverageOutput).contains(sourcePath);
    }
  }

  @Test
  public void testVisitBundleWithSimpleWindowsPath() {
    // Paths
    ImmutableSet<String> execPaths = ImmutableSet.of(
      "C:/parent/dir/com/example/Foo.java"
    );
    JacocoLCOVFormatter formatter = new JacocoLCOVFormatter(execPaths);
    IReportVisitor visitor = formatter.createVisitor(new PrintWriter(writer), new TreeMap<String, BranchCoverageDetail>());
    try {
      visitor.visitBundle(mockBundle, mock(ISourceFileLocator.class));
      visitor.visitEnd();
    } catch (IOException e) {
      Assert.fail(Throwables.getStackTraceAsString(e));
    }
    String coverageOutput = writer.toString();
    for (String sourcePath : execPaths) {
      assertThat(coverageOutput).contains(sourcePath);
    }
  }

  @Test
  public void testVisitBundleWithMappedUnixPath() {
    // Paths
    String srcPath = "/some/other/dir/Foo.java";
    ImmutableSet<String> execPaths = ImmutableSet.of(
      String.format("%s%s%s", srcPath, JacocoLCOVFormatter.EXEC_PATH_DELIMITER, "/com/example/Foo.java")
    );
    JacocoLCOVFormatter formatter = new JacocoLCOVFormatter(execPaths);
    IReportVisitor visitor = formatter.createVisitor(new PrintWriter(writer), new TreeMap<String, BranchCoverageDetail>());
    try {
      visitor.visitBundle(mockBundle, mock(ISourceFileLocator.class));
      visitor.visitEnd();
    } catch (IOException e) {
      Assert.fail(Throwables.getStackTraceAsString(e));
    }
    String coverageOutput = writer.toString();
    assertThat(coverageOutput).contains(srcPath);
  }

  @Test
  public void testVisitBundleWithMappedWindowsPath() {
    // Paths
    String srcPath = "C:/some/other/dir/Foo.java";
    ImmutableSet<String> execPaths = ImmutableSet.of(
      String.format("%s%s%s", srcPath, JacocoLCOVFormatter.EXEC_PATH_DELIMITER, "/com/example/Foo.java")
    );
    JacocoLCOVFormatter formatter = new JacocoLCOVFormatter(execPaths);
    IReportVisitor visitor = formatter.createVisitor(new PrintWriter(writer), new TreeMap<String, BranchCoverageDetail>());
    try {
      visitor.visitBundle(mockBundle, mock(ISourceFileLocator.class));
      visitor.visitEnd();
    } catch (IOException e) {
      Assert.fail(Throwables.getStackTraceAsString(e));
    }
    String coverageOutput = writer.toString();
    assertThat(coverageOutput).contains(srcPath);
  }

  @Test
  public void testVisitBundleWithNoMatchHasEmptyOutput() {
    // Paths
    ImmutableSet<String> execPaths = ImmutableSet.of(
      "/path/does/not/match/anything.txt"
    );
    JacocoLCOVFormatter formatter = new JacocoLCOVFormatter(execPaths);
    IReportVisitor visitor = formatter.createVisitor(new PrintWriter(writer), new TreeMap<String, BranchCoverageDetail>());
    try {
      visitor.visitBundle(mockBundle, mock(ISourceFileLocator.class));
      visitor.visitEnd();
    } catch (IOException e) {
      Assert.fail(Throwables.getStackTraceAsString(e));
    }
    String coverageOutput = writer.toString();
    assertThat(coverageOutput).isEmpty();
  }
}
