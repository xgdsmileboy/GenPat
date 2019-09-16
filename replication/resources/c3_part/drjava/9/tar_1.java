/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is part of DrJava.  Download the current version of this project:
 * http://sourceforge.net/projects/drjava/ or http://www.drjava.org/
 *
 * DrJava Open Source License
 *
 * Copyright (C) 2001-2003 JavaPLT group at Rice University (javaplt@rice.edu)
 * All rights reserved.
 *
 * Developed by:   Java Programming Languages Team
 *                 Rice University
 *                 http://www.cs.rice.edu/~javaplt/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal with the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimers.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimers in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the names of DrJava, the JavaPLT, Rice University, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this Software without specific prior written permission.
 *     - Products derived from this software may not be called "DrJava" nor
 *       use the term "DrJava" as part of their names without prior written
 *       permission from the JavaPLT group.  For permission, write to
 *       javaplt@rice.edu.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS WITH THE SOFTWARE.
 *
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model.junit;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import edu.rice.cs.util.FileOps;
import edu.rice.cs.drjava.model.GlobalModel;
import edu.rice.cs.drjava.model.IGetDocuments;
import edu.rice.cs.drjava.model.OpenDefinitionsDocument;
import edu.rice.cs.drjava.model.FileMovedException;
import edu.rice.cs.drjava.model.FileOpenSelector;
import edu.rice.cs.drjava.model.OperationCanceledException;
import edu.rice.cs.drjava.model.AlreadyOpenException;
import edu.rice.cs.drjava.model.repl.newjvm.MainJVM;
import edu.rice.cs.drjava.model.compiler.CompilerModel;
import edu.rice.cs.drjava.model.definitions.DefinitionsDocument;
import edu.rice.cs.drjava.model.definitions.ClassNameNotFoundException;
import edu.rice.cs.drjava.model.definitions.InvalidPackageException;
import edu.rice.cs.util.ExitingNotAllowedException;

// TODO: remove swing dependency!
import javax.swing.text.StyledDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;

/**
 * Manages unit testing via JUnit.
 *
 * TODO: Remove dependence on GlobalModel
 *
 * @version $Id$
 */
public class DefaultJUnitModel implements JUnitModel, JUnitModelCallback {

  /**
   * Manages listeners to this model.
   */
  private final JUnitEventNotifier _notifier = new JUnitEventNotifier();

  /**
   * Used by CompilerErrorModel to open documents that have errors.
   */
  private final IGetDocuments _getter;

  /**
   * RMI interface to a secondary JVM for running tests.
   * Using a second JVM prevents tests from disrupting normal usage of DrJava.
   */
  private final MainJVM _jvm;

  /**
   * Compiler model, used as a lock to prevent simultaneous test and compile.
   * Typed as an Object to prevent usage as anything but a lock.
   */
  private final Object _compilerModel;

  /**
   * GlobalModel, used only for getSourceFile.
   */
  private final GlobalModel _model;

  /**
   * The error model containing all current JUnit errors.
   */
  private JUnitErrorModel _junitErrorModel;

  /**
   * State flag to prevent losing results of a test in progress.
   */
  private boolean _isTestInProgress = false;

  /**
   * The document used to display JUnit test results.
   * TODO: why is this here?
   */
  private final StyledDocument _junitDoc = new DefaultStyledDocument();

  /**
   * Main constructor.
   * @param getter source of documents for this JUnitModel
   * @param jvm RMI interface to a secondary JVM for running tests
   * @param compilerModel the CompilerModel, used only as a lock to prevent
   *                      simultaneous test and compile
   * @param model used only for getSourceFile
   */
  public DefaultJUnitModel(IGetDocuments getter, MainJVM jvm,
      CompilerModel compilerModel, GlobalModel model) {
    _getter = getter;
    _jvm = jvm;
    _compilerModel = compilerModel;
    _model = model;
    _junitErrorModel = new JUnitErrorModel(new JUnitError[0], getter, false);
  }

  //-------------------------- Listener Management --------------------------//

  /**
   * Add a JUnitListener to the model.
   * @param listener a listener that reacts to JUnit events
   */
  public void addListener(JUnitListener listener) {
    _notifier.addListener(listener);
  }

  /**
   * Remove a JUnitListener from the model.  If the listener is not currently
   * listening to this model, this method has no effect.
   * @param listener a listener that reacts to JUnit events
   */
  public void removeListener(JUnitListener listener) {
    _notifier.removeListener(listener);
  }

  /**
   * Removes all JUnitListeners from this model.
   */
  public void removeAllListeners() {
    _notifier.removeAllListeners();
  }

  //-------------------------------- Triggers --------------------------------//

  public StyledDocument getJUnitDocument() {
    return _junitDoc;
  }

  /**
   * Creates a JUnit test suite over all currently open documents and runs it.
   * If the class file associated with a file is not a test case, it will be
   * ignored.  Synchronized against the compiler model to prevent testing and
   * compiling at the same time, which would create invalid results.
   */
  public void junitAll() {
        junitDocs(_getter.getDefinitionsDocuments());
  }

  /**
   * Creates a JUnit test suite over all currently open documents and runs it.
   * If the class file associated with a file is not a test case, it will be
   * ignored.  Synchronized against the compiler model to prevent testing and
   * compiling at the same time, which would create invalid results.
   */
  public void junitProject() {
    LinkedList<OpenDefinitionsDocument> lod = new LinkedList<OpenDefinitionsDocument>();
    
    Iterator<OpenDefinitionsDocument> it =
      _getter.getDefinitionsDocuments().iterator();
    while (it.hasNext()) {
      OpenDefinitionsDocument doc = it.next();
      if (doc.isInProjectPath() || doc.isAuxiliaryFile()) {
        lod.add(doc);
      }
    }
    junitDocs(lod);
  }
  
  /**
   * forwards the classnames and files to the test manager to test all of them
   * does not notify since we don't have ODD's to send out with the notification of junit start
   * @param a list of all the qualified class names to test
   * @param a list of their source files in the same order as qualified class names
   */
  public void junitAll(List<String> qualifiedClassnames, List<File> files){
      _notifier.junitAllStarted();
      List<String> tests = _jvm.runTestSuite(qualifiedClassnames, files, true);
      _isTestInProgress = true;
//      _notifier.junitEnded();
  }

  
  public void junitDocs(List<OpenDefinitionsDocument> lod){
    synchronized (_compilerModel) {
      //reset the JUnitErrorModel, fixes bug #907211 "Test Failures Not Cleared Properly".
      _junitErrorModel = new JUnitErrorModel(new JUnitError[0], null, false);
      
      Iterator<OpenDefinitionsDocument> it = lod.iterator();
//        _getter.getDefinitionsDocuments().iterator();
      HashMap<String,OpenDefinitionsDocument> classNamesToODDs =
        new HashMap<String,OpenDefinitionsDocument>();
      ArrayList<String> classNames = new ArrayList<String>();
      ArrayList<File> files = new ArrayList<File>();
      while (it.hasNext()) {
        try {
          OpenDefinitionsDocument doc = it.next();
          String cn = doc.getQualifiedClassName();
          classNames.add(cn);
          File f;
          try {
            f = doc.getFile();
          }
          catch (FileMovedException fme) {
            f = fme.getFile();
          }
          files.add(f);
          classNamesToODDs.put(cn, doc);
        }
        catch (ClassNameNotFoundException cnnfe) {
          // don't add it to the test suite
        }
      }
      List<String> tests = _jvm.runTestSuite(classNames, files, true);
      ArrayList<OpenDefinitionsDocument> odds =
        new ArrayList<OpenDefinitionsDocument>();
      Iterator<String> it2 = tests.iterator();
      while (it2.hasNext()) {
        odds.add(classNamesToODDs.get(it2.next()));
      }
      _notifier.junitStarted(odds);
      _isTestInProgress = true;
    }
  }

  
  /**
   * Runs JUnit on the current document. Used to compile all open documents
   * before testing but have removed that requirement in order to allow the
   * debugging of test cases. If the classes being tested are out of
   * sync, a message is displayed.
   */
  public void junit(OpenDefinitionsDocument doc)
      throws ClassNotFoundException, IOException {
      synchronized(_compilerModel) {
        //JUnit started, so throw out all JUnitErrorModels now, regardless of whether
        //  the tests succeed, etc.

        // if a test is running, don't start another one
        if (_isTestInProgress) {
          return;
        }

        //reset the JUnitErrorModel
        _junitErrorModel = new JUnitErrorModel(new JUnitError[0], null, false);

        // Compile and save before proceeding.
//        saveAllBeforeProceeding(GlobalModelListener.JUNIT_REASON);
//        if (areAnyModifiedSinceSave()) {
//          return;
//        }
        try {
          File testFile = doc.getFile();

//          compileAll();
//          if(getNumErrors() > 0) {
//            _notifier.notifyListeners(new EventNotifier.Notifier() {
//              public void notifyListener(GlobalModelListener l) {
//                l.compileErrorDuringJUnit();
//              }
//            });
//            return;
//          }

          ArrayList<OpenDefinitionsDocument> thisList = new ArrayList<OpenDefinitionsDocument>();
          thisList.add(doc);
          _notifier.junitStarted(thisList);

          try {
            // TODO: should this happen here, or should we make a "clear" method?
            StyledDocument junitDoc = getJUnitDocument();
            junitDoc.remove(0, junitDoc.getLength() - 1);
          }
          catch (BadLocationException e) {
            nonTestCase(false);
            return;
          }

          String testFilename = testFile.getName();
          String lowerCaseName = testFilename.toLowerCase();
          if (lowerCaseName.endsWith(".java")) {
            testFilename = testFilename.substring(0, testFilename.length() - 5);
          }
          else if (lowerCaseName.endsWith(".dj0") || lowerCaseName.endsWith(".dj1") || lowerCaseName.endsWith(".dj2")) {
            testFilename = testFilename.substring(0, testFilename.length() - 4);
          }
          else {
            nonTestCase(false);
            return;
          }
          String packageName;
          try {
            packageName = doc.getPackageName();
          }
          catch (InvalidPackageException e) {
            nonTestCase(false);
            return;
          }
          if(!packageName.equals("")) {
            testFilename = packageName + "." + testFilename;
          }
          ArrayList<String> classNames = new ArrayList<String>();
          classNames.add(testFilename);
          ArrayList<File> files = new ArrayList<File>();
          files.add(testFile);
          _jvm.runTestSuite(classNames, files, false);

          // Assign _isTestInProgress after calling runTest because we know at
          // this point that the interpreterJVM has registered itself. We also
          // know that the testFinished cannot be entered before this because
          // it has to acquire the same lock as this method.
          _isTestInProgress = true;
        }
        catch (IllegalStateException e) {
          // No file exists, don't try to compile and test
          nonTestCase(false);
          return;
        }
        catch (NoClassDefFoundError e) {
          // Method getTest in junit.framework.BaseTestRunner can throw a
          // NoClassDefFoundError (via reflection).
          _isTestInProgress = false;
          _notifier.junitEnded();
          throw e;
        }
        catch (ExitingNotAllowedException enae) {
          _isTestInProgress = false;
          _notifier.junitEnded();
          throw enae;
        }
      }
  }

  //-------------------------------- Helpers --------------------------------//

  //----------------------------- Error Results -----------------------------//

  /**
   * Gets the JUnitErrorModel, which contains error info for the last test run.
   */
  public JUnitErrorModel getJUnitErrorModel() {
    return _junitErrorModel;
  }

  /**
   * Resets the junit error state to have no errors.
   */
  public void resetJUnitErrors() {
    _junitErrorModel = new JUnitErrorModel(new JUnitError[0], _getter, false);
  }

  //---------------------------- Model Callbacks ----------------------------//

  /**
   * Called from the JUnitTestManager if its given className is not a test case.
   * TODO: Why is this sync'ed?
   * @param isTestAll whether or not it was a use of the test all button
   */
  public void nonTestCase(final boolean isTestAll) {
    synchronized(_compilerModel) {
      _isTestInProgress = false;
      _notifier.nonTestCase(isTestAll);
      _notifier.junitEnded();
    }
  }

  /**
   * Called to indicate that a suite of tests has started running.
   * TODO: Why is this sync'ed?
   * @param numTests The number of tests in the suite to be run.
   */
  public void testSuiteStarted(final int numTests) {
    synchronized(_compilerModel) {
      _notifier.junitSuiteStarted(numTests);
    }
  }

  /**
   * Called when a particular test is started.
   * TODO: Why is this sync'ed?
   * @param testName The name of the test being started.
   */
  public void testStarted(final String testName) {
    synchronized(_compilerModel) {
      _notifier.junitTestStarted(testName);
    }
  }

  /**
   * Called when a particular test has ended.
   * TODO: Why is this sync'ed?
   * @param testName The name of the test that has ended.
   * @param wasSuccessful Whether the test passed or not.
   * @param causedError If not successful, whether the test caused an error
   *  or simply failed.
   */
  public void testEnded(final String testName, final boolean wasSuccessful,
                        final boolean causedError)
  {
    synchronized(_compilerModel) {
      _notifier.junitTestEnded(testName, wasSuccessful, causedError);
    }
  }

  /**
   * Called when a full suite of tests has finished running.
   * TODO: Why is this sync'ed?
   * @param errors The array of errors from all failed tests in the suite.
   */
  public void testSuiteEnded(JUnitError[] errors) {
    synchronized(_compilerModel) {
      if (_isTestInProgress) {
        _junitErrorModel = new JUnitErrorModel(errors, _getter, true);

        _isTestInProgress = false;
        _notifier.junitEnded();
      }
    }
  }

  /**
   * Called when the JUnitTestManager wants to open a file that is not
   * currently open.
   * TODO: this is the only call to _model
   *       - remove it to remove GlobalModel dependence
   * @param className the name of the class for which we want to find the file
   * @return the file associated with the given class
   */
  public File getFileForClassName(String className) {
    return _model.getSourceFile(className + ".java");
  }

  /**
   * Returns the current classpath in use by the JUnit JVM,
   * in the form of a path-separator delimited string.
   */
  public String getClasspathString() {
    return _jvm.getClasspathString();
  }

  /**
   * Called when the JVM used for unit tests has registered.
   */
  public void junitJVMReady() {
    if (_isTestInProgress) {
      JUnitError[] errors = new JUnitError[1];
      errors[0] = new JUnitError("Previous test was interrupted", true, "");
      _junitErrorModel = new JUnitErrorModel(errors, _getter, true);

      _isTestInProgress = false;
      _notifier.junitEnded();
    }
  }
}
