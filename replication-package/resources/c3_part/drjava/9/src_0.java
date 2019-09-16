/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is part of DrJava.  Download the current version of this project from http://www.drjava.org/
 * or http://sourceforge.net/projects/drjava/
 *
 * DrJava Open Source License
 * 
 * Copyright (C) 2001-2005 JavaPLT group at Rice University (javaplt@rice.edu).  All rights reserved.
 *
 * Developed by:   Java Programming Languages Team, Rice University, http://www.cs.rice.edu/~javaplt/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the "Software"), to deal with the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 *     - Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 *       following disclaimers.
 *     - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
 *       following disclaimers in the documentation and/or other materials provided with the distribution.
 *     - Neither the names of DrJava, the JavaPLT, Rice University, nor the names of its contributors may be used to 
 *       endorse or promote products derived from this Software without specific prior written permission.
 *     - Products derived from this software may not be called "DrJava" nor use the term "DrJava" as part of their 
 *       names without prior written permission from the JavaPLT group.  For permission, write to javaplt@rice.edu.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * WITH THE SOFTWARE.
 * 
 *END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.ui;

import edu.rice.cs.drjava.model.OpenDefinitionsDocument;
import edu.rice.cs.drjava.model.GlobalModel;
import edu.rice.cs.drjava.model.definitions.InvalidPackageException;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.jar.JarBuilder;
import edu.rice.cs.util.jar.ManifestWriter;
import edu.rice.cs.util.swing.FileChooser;
import edu.rice.cs.util.swing.FileSelectorStringComponent;
import edu.rice.cs.util.swing.FileSelectorComponent;
import edu.rice.cs.util.swing.SwingWorker;
import edu.rice.cs.util.swing.Utilities;
import edu.rice.cs.util.newjvm.ExecJVM;
import edu.rice.cs.util.StreamRedirectThread;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

public class JarOptionsDialog extends JFrame {
  /** Class to save the frame state, i.e. location. */
  public static class FrameState {
    private Point _loc;
    public FrameState(Point l) {
      _loc = l;
    }
    public FrameState(String s) {
      StringTokenizer tok = new StringTokenizer(s);
      try {
        int x = Integer.valueOf(tok.nextToken());
        int y = Integer.valueOf(tok.nextToken());
        _loc = new Point(x, y);
      }
      catch(NoSuchElementException nsee) {
        throw new IllegalArgumentException("Wrong FrameState string: " + nsee);
      }
      catch(NumberFormatException nfe) {
        throw new IllegalArgumentException("Wrong FrameState string: " + nfe);
      }
    }
    public FrameState(JarOptionsDialog comp) {
      _loc = comp.getLocation();
    }
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(_loc.x);
      sb.append(' ');
      sb.append(_loc.y);
      return sb.toString();
    }
    public Point getLocation() { return _loc; }
  }
  
  /** Bitflags for default selection. */
  public static final int JAR_CLASSES = 1;
  public static final int JAR_SOURCES = 2;
  public static final int MAKE_EXECUTABLE = 4;
  
  /** Determines whether class files should be jar-ed. */
  private JCheckBox _jarClasses; 
  /** Determines whether source files should be jar-ed. */
  private JCheckBox _jarSources;
  /** Determines whether the jar file should be made executable. */
  private JCheckBox _makeExecutable;
  /** File selector for the jar output file. */
  private FileSelectorComponent _jarFileSelector;
  /** Text field for the main class. */
  private FileSelectorStringComponent _mainClassField;
  /** Label for main class. */
  private JLabel _mainClassLabel;
  /** OK button. */
  private JButton _okButton;
  /** Cancel button. */
  private JButton _cancelButton;
  /** Main frame. */
  private MainFrame _mainFrame;
  /** Model. */
  private GlobalModel _model;
  /** Label explaining why classes can't be jar-ed. */
  private JLabel _cantJarClassesLabel;
  /** Root of the chooser. */
  private File _rootFile;
  /** Processing dialog. */
  private ProcessingFrame _processingFrame;  
  /** Last frame state. It can be stored and restored. */
  private FrameState _lastState = null;

  
  /** Returns the last state of the frame, i.e. the location and dimension.
   *  @return frame state
   */
  public FrameState getFrameState() { return _lastState; }
  
  /** Sets state of the frame, i.e. the location and dimension of the frame for the next use.
   *  @param ds State to update to, or {@code null} to reset
   */
  public void setFrameState(FrameState ds) {
    _lastState = ds;
    if (_lastState!=null) {
      setLocation(_lastState.getLocation());
      validate();
    }
  }  
  
  /** Sets state of the frame, i.e. the location and dimension of the frame for the next use.
   *  @param s  State to update to, or {@code null} to reset
   */
  public void setFrameState(String s) {
    try { _lastState = new FrameState(s); }
    catch(IllegalArgumentException e) { _lastState = null; }
    if (_lastState!=null) setLocation(_lastState.getLocation());
    else MainFrame.setPopupLoc(this, _mainFrame);
    validate();
  }
  
  /** Frame that gets displayed when the program is processing data. */
  private static class ProcessingFrame extends JFrame {
    private Component _parent;
    public ProcessingFrame(Component parent, String title, String label) {
      super(title);
      _parent = parent;
      setSize(350, 150);
      MainFrame.setPopupLoc(this, parent);
      JLabel waitLabel = new JLabel(label, SwingConstants.CENTER);
      getRootPane().setLayout(new BorderLayout());
      getRootPane().add(waitLabel, BorderLayout.CENTER);
    }
    public void setVisible(boolean vis) {
      MainFrame.setPopupLoc(this, _parent);
      super.setVisible(vis);
    }
  }

  /** Create a configuration diaglog
   *  @param mf the instance of mainframe to query into the project
   */
  public JarOptionsDialog(MainFrame mf) {
    super("Create Jar File from Project");
    _mainFrame = mf;
    _model = mf.getModel();
    initComponents();
  }

  /** Load the initial state from the previous files or with defaults. */
  private void _loadSettings() {
    int f = _model.getCreateJarFlags();
    _jarClasses.setSelected(((f & JAR_CLASSES) != 0));
    _jarSources.setSelected(((f & JAR_SOURCES) != 0));
    _makeExecutable.setSelected(((f & MAKE_EXECUTABLE) != 0));
    
    boolean outOfSync = true;
    if (_model.getBuildDirectory() != null) {
      outOfSync = _model.hasOutOfSyncDocuments();
    }
    if ((_model.getBuildDirectory() == null) || (outOfSync)) {
      _jarClasses.setSelected(false);
      _jarClasses.setEnabled(false);
      String s;
      if ((_model.getBuildDirectory() == null) && (outOfSync)) {
        s = "<html><center>A build directory must be specified in order to jar class files,<br>and the project needs to be compiled.</center></html>";
      }
      else
      if (_model.getBuildDirectory() == null) {
        s = "<html>A build directory must be specified in order to jar class files.</html>";
      }
      else {
        s = "<html>The project needs to be compiled.</html>";
      }
      _cantJarClassesLabel.setText(s);
    }
    else {
      _jarClasses.setEnabled(true);
      _cantJarClassesLabel.setText(" ");

      // Main class
      _rootFile = _model.getBuildDirectory();
      try {
        _rootFile = _rootFile.getCanonicalFile();
      } catch(IOException e) { }
    
      FileChooser chooser = new FileChooser(_rootFile);
      chooser.setDialogTitle("Select Main Class");
//      chooser.setTopMessage("Select the main class for the executable jar file:");
      chooser.setApproveButtonText("Select");
      FileFilter filter = new FileFilter() {
        public boolean accept(File f) {
          String name = f.getName();
          return  !f.isDirectory() && name.endsWith(".class");
        }
        public String getDescription() { return "Class Files (*.class)"; }
      };
      chooser.addChoosableFileFilter(filter);
//      chooser.addChoosableFileFilter(filter);
//      chooser.setShowFiles(true);
//      chooser.setFileDisplayManager(MainFrame.getFileDisplayManager20());
      _mainClassField.setFileChooser(chooser);
      
      final File mc = _model.getMainClass();
      if (mc == null)  _mainClassField.setText("");
      else {
        try {
          OpenDefinitionsDocument mcDoc = _model.getDocumentForFile(mc);
          _mainClassField.setText(mcDoc.getQualifiedClassName());
        }
        catch(IOException ioe) { _mainClassField.setText(""); }
        catch(edu.rice.cs.drjava.model.definitions.ClassNameNotFoundException e) { _mainClassField.setText(""); }
      }
    }
    
    _jarFileSelector.setFileField(_model.getCreateJarFile());
    
    _okButton.setEnabled(_jarSources.isSelected() || _jarClasses.isSelected());
    _setEnableExecutable(_jarClasses.isSelected());
  }

  /** Build the dialog. */
  private void initComponents() {
    JPanel main = _makePanel();
    super.getContentPane().setLayout(new BorderLayout());
    super.getContentPane().add(main, BorderLayout.NORTH);

    Action okAction = new AbstractAction("OK") {
      public void actionPerformed(ActionEvent e) {
        _ok();
      }
    };
    _okButton = new JButton(okAction);

    Action cancelAction = new AbstractAction("Cancel") {
      public void actionPerformed(ActionEvent e) {
        _cancel();
      }
    };
    _cancelButton = new JButton(cancelAction);

    // Add buttons
    JPanel bottom = new JPanel();
    bottom.setBorder(new EmptyBorder(5, 5, 5, 5));
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
    bottom.add(Box.createHorizontalGlue());
    bottom.add(_okButton);
    bottom.add(_cancelButton);
    bottom.add(Box.createHorizontalGlue());

    super.getContentPane().add(bottom, BorderLayout.SOUTH);
    super.setResizable(false);
    pack();
    
    MainFrame.setPopupLoc(this, _mainFrame);    
  }

  /** Make the options panel. 
   * @return The panel with the options for jarring a project
   */
  private JPanel _makePanel() {
    JPanel panel = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    panel.setLayout(gridbag);
    c.fill = GridBagConstraints.HORIZONTAL;
    Insets labelInsets = new Insets(5, 10, 0, 10);
    c.weightx = 0.0;
    c.gridwidth = 1;
    c.insets = labelInsets;

    // Jar class files
    c.weightx = 1.0;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.insets = labelInsets;
    c.fill = GridBagConstraints.HORIZONTAL;

    JPanel jarClassesPanel = _makeClassesPanel();
    gridbag.setConstraints(jarClassesPanel, c);
    panel.add(jarClassesPanel);

    _cantJarClassesLabel = new JLabel("<html><center>A build directory must be specified in order to jar class files,<br>and the project needs to be compiled.</center></html>",  SwingConstants.CENTER);
    c.gridx = 0;
    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.HORIZONTAL;
    gridbag.setConstraints(jarClassesPanel, c);
    panel.add(_cantJarClassesLabel);
    
    // Jar Sources
    _jarSources = new JCheckBox(new AbstractAction("Jar source files") {
      public void actionPerformed(ActionEvent e) {
        _okButton.setEnabled(_jarSources.isSelected() || _jarClasses.isSelected());
      }
    });

    c.weightx = 0.0;
    c.gridwidth = 1;
    c.insets = labelInsets;

    gridbag.setConstraints(_jarSources, c);
    panel.add(_jarSources);

    // Output file
    c.gridx = 0;
    c.gridwidth = 1;
    c.insets = labelInsets;
    JLabel label = new JLabel("Jar File");
    label.setToolTipText("The file that the jar should be written to.");
    gridbag.setConstraints(label, c);
    panel.add(label);

    c.weightx = 1.0;
    c.gridx = 0;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.insets = labelInsets;

    JPanel jarFilePanel = _makeJarFileSelector();
    gridbag.setConstraints(jarFilePanel, c);
    panel.add(jarFilePanel);

    return panel;
  }

  /** Make the panel that is enabled when you are going to jar class files
   *  @return the panel containing the sub-options to the jarring classes option
   */
  private JPanel _makeClassesPanel() {
    JPanel panel = new JPanel();
    GridBagConstraints gridBagConstraints;
    panel.setLayout(new GridBagLayout());
    
    _jarClasses = new JCheckBox(new AbstractAction("Jar classes") {
      public void actionPerformed(ActionEvent e) {
        _toggleClassOptions();
        _okButton.setEnabled(_jarSources.isSelected() || _jarClasses.isSelected());
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panel.add(_jarClasses, gridBagConstraints);

    JPanel addclasses = new JPanel();
    addclasses.setLayout(new GridBagLayout());
    _makeExecutable = new JCheckBox(new AbstractAction("Make executable") {
      public void actionPerformed(ActionEvent e) {
        _toggleMainClass();        
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    addclasses.add(_makeExecutable, gridBagConstraints);
    
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(0, 20, 0, 0);
    addclasses.add(_makeMainClassSelectorPanel(), gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(0, 25, 0, 0);
    panel.add(addclasses, gridBagConstraints);

    return panel;
  }
 
  /** Make the panel that lets you select the jar's main class.
   *  @return the panel containing the label and the selector for the main class.
   */
  private JPanel _makeMainClassSelectorPanel() {
    _mainClassField = new FileSelectorStringComponent(this, null, 20, 12f) {
        public File convertStringToFile(String s) { 
          s = s.trim().replace('.', java.io.File.separatorChar) + ".class";
          if (s.equals("")) return null;
          else return new File(_rootFile, s);
        }
        
        public String convertFileToString(File f) {
          if (f == null)  return "";
          else {
            try {
              String s = edu.rice.cs.util.FileOps.makeRelativeTo(f, _rootFile).toString();
              s = s.substring(0, s.lastIndexOf(".class"));
              s = s.replace(java.io.File.separatorChar, '.').replace('$', '.');
              int pos = 0;
              boolean ok = true;
              while((pos = s.indexOf('.', pos)) >= 0) {
                if ((s.length() <= pos + 1) || (Character.isDigit(s.charAt(pos + 1)))) {
                  ok = false;
                  break;
                }
                ++pos;
              }
              if (ok) return s;
              return "";
            }
            catch(Exception e) { return ""; }
          }
        }
    };
    _mainClassField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) { setEnabled(); }
      public void removeUpdate(DocumentEvent e) { setEnabled(); }
      public void changedUpdate(DocumentEvent e) { setEnabled(); }
      private void setEnabled() { Utilities.invokeLater(new Runnable() { public void run() { _okButton.setEnabled(true); } }); }
    });
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    _mainClassLabel = new JLabel("Main class:  ");
    _mainClassLabel.setLabelFor(_mainClassField);
    p.add(_mainClassLabel, BorderLayout.WEST);
    p.add(_mainClassField, BorderLayout.CENTER);
    return p;
  }


  /** Create a file selector to select the output jar file
   *  @return The JPanel that contains the selector
   */
  private JPanel _makeJarFileSelector() {
    JFileChooser fileChooser = new JFileChooser(_model.getBuildDirectory());
    fileChooser.setDialogTitle("Select Jar Output File");
    fileChooser.setApproveButtonText("Select");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false);
    fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

    _jarFileSelector = new FileSelectorComponent(this, fileChooser, 20, 12f, false);
    _jarFileSelector.setFileFilter(new FileFilter() {
      public boolean accept(File f) { return f.getName().endsWith(".jar") || f.isDirectory(); }
      public String getDescription() { return "Java Archive Files (*.jar)"; }
    });

    return _jarFileSelector;
  }

  /** Modifies state for when the executable check box is selected */
  private void _setEnableExecutable(boolean b) {
    _makeExecutable.setEnabled(b);
    _toggleMainClass();
  }
 
  /** Method to run when the jar class file is selected */
  private void _toggleClassOptions() {
    _setEnableExecutable(_jarClasses.isSelected());
  }

  /** Method to call when the 'Make Executable' check box is clicked. */
  private void _toggleMainClass() {
    _mainClassField.setEnabled(_makeExecutable.isSelected() && _jarClasses.isSelected());
    _mainClassLabel.setEnabled(_makeExecutable.isSelected() && _jarClasses.isSelected());
  }

  /** Method that handels the Cancel button */
  private void _cancel() {
    _lastState = new FrameState(this);
    this.setVisible(false);
  }

  /** Do the Jar. */
  private void _ok() {
    // Always apply and save settings
    _saveSettings();

    File jarOut = _jarFileSelector.getFileFromField();
    if (jarOut == null) {
      JOptionPane.showMessageDialog(JarOptionsDialog.this,
                                    "You must specify an output file",
                                    "Error: No File Specified",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    else if (jarOut.exists()) {
      if (JOptionPane.showConfirmDialog(JarOptionsDialog.this,
                                        "Are you sure you want to overwrite the file '" + jarOut.getPath() + "'?",
                                        "Overwrite file?",
                                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
        // I want to focus back to the dialog
        return;
      }
    }

    setEnabled(false);
    _processingFrame = new ProcessingFrame(this, "Creating Jar File", "Processing, please wait.");
    _processingFrame.setVisible(true);
    SwingWorker worker = new SwingWorker() {
      boolean _success = false;

      /**
       * Takes input of a file which is a directory and compresses all the class files in it
       * into a jar file
       *
       * @param dir     the File object representing the directory
       * @param jarFile the JarBuilder that the data should be written to
       * @return true on success, false on failure
       */
      private boolean jarBuildDirectory(File dir, JarBuilder jarFile) throws IOException {
        java.io.FileFilter classFilter = new java.io.FileFilter() {
          public boolean accept(File f) {
            return f.isDirectory() || f.getName().endsWith(".class");
          }
        };

        File[] files = dir.listFiles(classFilter);
        if (files!=null) { // listFiles may return null if there's an IO error
          for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
              jarFile.addDirectoryRecursive(files[i], files[i].getName(), classFilter);
            }
            else {
              jarFile.addFile(files[i], "", files[i].getName());
            }
          }
        }

        return true;
      }

      /**
       * Takes the model and the jar and writes all the sources to the jar
       *
       * @param model the GlobalModel that the files are to come out of
       * @param jar   the JarBuilder that the data should be written to
       * @return true on success, false on failure
       */
      private boolean jarSources(GlobalModel model, JarBuilder jar) {
        List<OpenDefinitionsDocument> srcs = model.getProjectDocuments();

        Iterator<OpenDefinitionsDocument> iter = srcs.iterator();
        while (iter.hasNext()) {
          OpenDefinitionsDocument doc = iter.next();
          if (doc.inProject() && ! doc.isAuxiliaryFile()) {
            try {
              // Since the file compiled without any errors, this shouldn't have any problems
              jar.addFile(doc.getFile(), packageNameToPath(doc.getPackageName()), doc.getFileName());
            }
            catch (IOException e) {
              e.printStackTrace();
              throw new UnexpectedException(e);
            }
          }
        }
        return true;
      }

      /** Helper function to convert a package name to its path form
       *  @param packageName the name of the package
       *  @return the String which is should be the directory that it should be contained within
       */
      private String packageNameToPath(String packageName) {
        return packageName.replaceAll("\\.", System.getProperty("file.separator").replaceAll("\\\\", "\\\\\\\\"));
      }
      /** The method to perform the work
       *  @return null
       */
      public Object construct() {
        try {
          File jarOut = _jarFileSelector.getFileFromField();
          if (!jarOut.exists()) {
            jarOut.createNewFile();
          }
          
          if (_jarClasses.isSelected() && _jarSources.isSelected()) {
            JarBuilder mainJar = null;
            if (_makeExecutable.isSelected()) {
              ManifestWriter mw = new ManifestWriter();
              mw.setMainClass(_mainClassField.getText());
              mainJar = new JarBuilder(jarOut, mw.getManifest());
            }
            else {
              mainJar = new JarBuilder(jarOut);
            }
            
            jarBuildDirectory(_model.getBuildDirectory(), mainJar);
            
            File sourceJarFile = File.createTempFile(_model.getBuildDirectory().getName(), ".jar");
            JarBuilder sourceJar = new JarBuilder(sourceJarFile);
            jarSources(_model, sourceJar);
            sourceJar.close();
            mainJar.addFile(sourceJarFile, "", "source.jar");
            
            mainJar.close();
            sourceJarFile.delete();
          }
          else if (_jarClasses.isSelected()) {
            JarBuilder jb;
            if (_makeExecutable.isSelected()) {
              ManifestWriter mw = new ManifestWriter();
              mw.setMainClass(_mainClassField.getText());
              jb = new JarBuilder(jarOut, mw.getManifest());
            }
            else {
              jb = new JarBuilder(jarOut);
            }
            jarBuildDirectory(_model.getBuildDirectory(), jb);
            jb.close();
          }
          else {
            JarBuilder jb = new JarBuilder(jarOut);
            jarSources(_model, jb);
            jb.close();
          }
          _success = true;
        }
        catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
      public void finished() {
        _processingFrame.setVisible(false);
        _processingFrame.dispose();
        JarOptionsDialog.this.setEnabled(true);
        if (_success) {
          if (_makeExecutable.isSelected()) {
             Object[] options = { "OK", "Run" };
             int res = JOptionPane.showOptionDialog(JarOptionsDialog.this, "Jar file successfully written to '"+_jarFileSelector.getFileFromField().getName()+"'",
                                                    "Jar Creation Successful", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                                                    null, options, options[0]);
             JarOptionsDialog.this.setVisible(false);
             if (1==res) {
               SwingWorker jarRunner = new SwingWorker() {
                 public Object construct() {
                   try {
                     Process jarFileProcess = ExecJVM.runJVM(_mainClassField.getText(), // mainClass
                                                             new String[] {}, // classParams,
                                                             new String[] { _jarFileSelector.getFileFromField().getAbsolutePath() }, // classPath,
                                                             new String[] {}, // jvmParams,
                                                             _jarFileSelector.getFileFromField().getParentFile());
                                                             
                     StreamRedirectThread errThread = new StreamRedirectThread("error reader", jarFileProcess.getErrorStream(), System.err);
                     StreamRedirectThread outThread = new StreamRedirectThread("output reader", jarFileProcess.getInputStream(), System.out);
                     errThread.start();
                     outThread.start();
                     boolean notDead = true;
                     while(notDead) {
                       try {
                         errThread.join();
                         outThread.join();
                         notDead = false;
                       }
                       catch (InterruptedException exc) {
                         // ignore, we don't interrupt
                       }
                     }
                     jarFileProcess.waitFor();
                     JOptionPane.showMessageDialog(JarOptionsDialog.this,"Execution of jar file terminated (exit value = "+
                                                   jarFileProcess.exitValue()+")", "Execution terminated.",
                                                   JOptionPane.INFORMATION_MESSAGE);
                   }
                   catch(Exception e) {
                     JOptionPane.showMessageDialog(JarOptionsDialog.this, "An error occured while running the jar file: \n"+e, "Error", JOptionPane.ERROR_MESSAGE);
                   }
                   finally {
                     JarOptionsDialog.this.setVisible(false);
                   }
                   return null;
                 }
               };
               jarRunner.start();
             }
          }
          else {
            JOptionPane.showMessageDialog(JarOptionsDialog.this, "Jar file successfully written to '" + _jarFileSelector.getFileFromField().getName() + "'", "Jar Creation Successful", JOptionPane.INFORMATION_MESSAGE);
            JarOptionsDialog.this.setVisible(false);
          }
        }
        else {
          JOptionPane.showMessageDialog(JarOptionsDialog.this, "An error occured while creating the jar file. This could be because the file that you are writing to or the file you are reading from could not be opened.", "Error: File Access", JOptionPane.ERROR_MESSAGE);
          JarOptionsDialog.this.setVisible(false);
        }
      }
    };
    worker.start();
  }

  /** Save the settings for this dialog. */
  private boolean _saveSettings() {
    _lastState = new FrameState(this);
    if ((_model.getCreateJarFile() == null) ||
        (!_model.getCreateJarFile().getName().equals(_jarFileSelector.getFileFromField().getName()))) {
      _model.setCreateJarFile(_jarFileSelector.getFileFromField());
    }
    int f = 0;
    if (_jarClasses.isSelected()) f |= JAR_CLASSES;
    if (_jarSources.isSelected()) f |= JAR_SOURCES;
    if (_makeExecutable.isSelected()) f |= MAKE_EXECUTABLE;
    if (f!=_model.getCreateJarFlags()) {
      _model.setCreateJarFlags(f);
    }
    return true;
  }

  /** Toggle visibility of this frame. Warning, it behaves like a modal dialog. */
  public void setVisible(boolean vis) {
    assert EventQueue.isDispatchThread();
    validate();
    if (vis) {
      _mainFrame.hourglassOn();
      ProcessingFrame pf = new ProcessingFrame(this, "Checking class files", "Processing, please wait.");
      pf.setVisible(true);
      _loadSettings();
      pf.setVisible(false);
      pf.dispose();
    }
    else {
      _mainFrame.hourglassOff();
      _mainFrame.toFront();
    }
    super.setVisible(vis);
  }  
}
