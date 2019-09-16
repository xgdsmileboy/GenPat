/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is a part of DrJava. Current versions of this project are available
 * at http://sourceforge.net/projects/drjava
 *
 * Copyright (C) 2001-2002 JavaPLT group at Rice University (javaplt@rice.edu)
 *
 * DrJava is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrJava is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * or see http://www.gnu.org/licenses/gpl.html
 *
 * In addition, as a special exception, the JavaPLT group at Rice University
 * (javaplt@rice.edu) gives permission to link the code of DrJava with
 * the classes in the gj.util package, even if they are provided in binary-only
 * form, and distribute linked combinations including the DrJava and the
 * gj.util package. You must obey the GNU General Public License in all
 * respects for all of the code used other than these classes in the gj.util
 * package: Dictionary, HashtableEntry, ValueEnumerator, Enumeration,
 * KeyEnumerator, Vector, Hashtable, Stack, VectorEnumerator.
 *
 * If you modify this file, you may extend this exception to your version of the
 * file, but you are not obligated to do so. If you do not wish to
 * do so, delete this exception statement from your version. (However, the
 * present version of DrJava depends on these classes, so you'd want to
 * remove the dependency first!)
 *
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.ui;

import java.util.Arrays;
import java.util.Hashtable;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.*;
import java.awt.*;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.model.junit.*;
import edu.rice.cs.util.UnexpectedException;
import edu.rice.cs.util.swing.HighlightManager;
import edu.rice.cs.drjava.model.GlobalModel;
import edu.rice.cs.drjava.model.OpenDefinitionsDocument;
import edu.rice.cs.drjava.config.*;

/**
 * The panel which displays all the testing errors.
 * In the future, it may also contain a progress bar.
 *
 * @version $Id$
 */
public class JUnitPanel extends TabbedPanel 
  implements OptionConstants{

  /** Highlight painter for selected list items. */
  private static DefaultHighlighter.DefaultHighlightPainter
    _listHighlightPainter
      = new DefaultHighlighter.DefaultHighlightPainter(DrJava.CONFIG.getSetting(COMPILER_ERROR_COLOR));

  private static final SimpleAttributeSet NORMAL_ATTRIBUTES = _getNormalAttributes();
  private static final SimpleAttributeSet BOLD_ATTRIBUTES = _getBoldAttributes();

  private static final SimpleAttributeSet _getBoldAttributes() {
    SimpleAttributeSet s = new SimpleAttributeSet();
    StyleConstants.setBold(s, true);
    return s;
  }

  private static final SimpleAttributeSet _getNormalAttributes() {
    SimpleAttributeSet s = new SimpleAttributeSet();
    return s;
  }


  /** The total number of errors in the list */
  private int _numErrors;
  private final SingleDisplayModel _model;
  private final JUnitErrorListPane _errorListPane;
  
  private JCheckBox _showHighlightsCheckBox;
  
  /**
   * Constructor.
   * @param model SingleDisplayModel in which we are running
   * @param frame MainFrame in which we are displayed
   */
  public JUnitPanel(SingleDisplayModel model, MainFrame frame) {
    super(frame, "Test Output");
    _model = model;
    _errorListPane = new JUnitErrorListPane();

    _mainPanel.setLayout(new BorderLayout());

    // We make the vertical scrollbar always there.
    // If we don't, when it pops up it cuts away the right edge of the
    // text. Very bad.
    JScrollPane scroller =
      new BorderlessScrollPane(_errorListPane,
                      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


    _mainPanel.add(scroller, BorderLayout.CENTER);
    
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BorderLayout());
    
    _showHighlightsCheckBox = new JCheckBox( "Highlight source", true);
    _showHighlightsCheckBox.addChangeListener( new ChangeListener() {
      public void stateChanged (ChangeEvent ce) {
        DefinitionsPane lastDefPane = _errorListPane.getLastDefPane();
        
        if (_showHighlightsCheckBox.isSelected()) {
          //lastDefPane.setCaretPosition( lastDefPane.getCaretPosition());
          _errorListPane.switchToError(_errorListPane.getSelectedIndex());
          lastDefPane.requestFocus();
        }
        else {
          lastDefPane.removeErrorHighlight();
        }
      }
    });
    
    buttonPane.add(_showHighlightsCheckBox, BorderLayout.SOUTH);
    _mainPanel.add(buttonPane, BorderLayout.EAST);

  }
  
  /**
   * Returns the ErrorListPane that this panel manages.
   */
  public JUnitErrorListPane getJUnitErrorListPane() {
    return _errorListPane;
  }

  /** Changes the font of the error list. */
  public void setListFont(Font f) {
    StyleConstants.setFontFamily(NORMAL_ATTRIBUTES, f.getFamily());
    StyleConstants.setFontSize(NORMAL_ATTRIBUTES, f.getSize());

    StyleConstants.setFontFamily(BOLD_ATTRIBUTES, f.getFamily());
    StyleConstants.setFontSize(BOLD_ATTRIBUTES, f.getSize());
  }

  /** Called when compilation begins. */
  public void setJUnitInProgress() {
    _errorListPane.setJUnitInProgress();
  }
  
  /**
   * Clean up when the tab is closed.
   */
  protected void _close() {
    super._close();
    _model.getActiveDocument().setJUnitErrorModel(new JUnitErrorModel());
    _frame.updateErrorListeners();
    reset();
  }

  /**
   * Reset the errors to the current error information.
   * @param errors the current error information
   */
  public void reset() {
    JUnitErrorModel juem = _model.getActiveDocument().getJUnitErrorModel();
    boolean testsHaveRun = false;
    if (juem != null) {
      _numErrors = juem.getErrorsWithoutPositions().length + juem.getErrorsWithPositions().length;
      testsHaveRun = juem.haveTestsRun();
    } else {
      _numErrors = 0;
    }
    _errorListPane.updateListPane(testsHaveRun);
    _resetEnabledStatus();
  }

  private void _showAllErrors() {
  }

  /**
   * Reset the enabled status of the "next", "previous", and "show all"
   * buttons in the compiler error panel.
   */
  private void _resetEnabledStatus() {
  }



  /**
   * A pane to show JUnit errors. It acts a bit like a listbox (clicking
   * selects an item) but items can each wrap, etc.
   */
  public class JUnitErrorListPane extends JEditorPane {

    /**
     * Index into _errorListPositions of the currently selected error.
     */
    private int _selectedIndex;

    /**
     * The start position of each error in the list. This position is the place
     * where the error starts in the error list, as opposed to the place where
     * the error exists in the source.
     */
    private Position[] _errorListPositions;

    /**
     * Table mapping Positions in the error list to JUnitErrors.
     */
    private final Hashtable _errorTable;

    /**
     * The DefinitionsPane with the current error highlight.
     * (Initialized to the current pane.)
     */
    private DefinitionsPane _lastDefPane;

    // when we create a highlight we get back a tag we can use to remove it
    private HighlightManager.HighlightInfo _listHighlightTag = null;

    // on mouse click, highlight the error in the list and also in the source
    /**private MouseAdapter _mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        JUnitError error = _errorAtPoint(e.getPoint());

        if (error == null) {
          selectNothing();
        }
        else {
          _errorListPane.switchToError(error);
        }
      }
    };*/
    
    private class PopupAdapter extends MouseAdapter {
      
      private JUnitError _error = null;
      
      public void mouseClicked(MouseEvent e) {
        maybeShowPopup(e);
      }
      
      public void mouseReleased(MouseEvent e) {
        _error = _errorAtPoint(e.getPoint());

        if (_error == null) {
          selectNothing();
        }
        else {
          _errorListPane.switchToError(_error);
        }
        maybeShowPopup(e);      
      }
      
      private void maybeShowPopup(MouseEvent e) {
        
        //if (SwingUtilities.isRightMouseButton(e)) {
        if (e.isPopupTrigger()) {
          _popMenu.show(e.getComponent(),
                        e.getX(), e.getY());
        }
      }
      //}
        
      public JUnitError getError() {
        return _error;
      }
      
    }
    
    private JPopupMenu _popMenu;
    private PopupAdapter _popupAdapter;
    private Window _stackFrame = null;
    private JTextArea _stackTextArea;
    private final JLabel _errorLabel = new JLabel(),
      _testLabel = new JLabel(), _fileLabel = new JLabel();
    
    private HighlightManager _highlightManager = new HighlightManager(this);
    
    /**
     * Constructs the ErrorListPane.
     */
    public JUnitErrorListPane() {
      // If we set this pane to be of type text/rtf, it wraps based on words
      // as opposed to based on characters.
      super("text/rtf", "");
      
      _createPopupMenu();
      _popupAdapter = new PopupAdapter();
      addMouseListener(_popupAdapter);
      //addMouseListener(_mouseListener);

      _selectedIndex = 0;
      _errorListPositions = new Position[0];
      _errorTable = new Hashtable();
      _lastDefPane = _frame.getCurrentDefPane();
      //System.out.println("lastDefPane = " + _lastDefPane);

      JUnitErrorListPane.this.setFont(new Font("Courier", 0, 20));
      
      // We set the editor pane disabled so it won't get keyboard focus,
      // which makes it uneditable, and so you can't select text inside it.
      setEnabled(false);
      
      DrJava.CONFIG.addOptionListener( OptionConstants.COMPILER_ERROR_COLOR, new CompilerErrorColorOptionListener());    
    }
    
    
    private void _createPopupMenu() {
      
      _popMenu = new JPopupMenu();
      JMenuItem stackTraceItem = new JMenuItem("Show Stack Trace");
      stackTraceItem.addActionListener ( new AbstractAction() {
        public void actionPerformed( ActionEvent ae) {
          JUnitError error = _popupAdapter.getError();
          if (error != null) {
            if (_stackFrame == null) {
              _setupStackTraceFrame();
            }
            _displayStackTrace(_popupAdapter.getError());
          }
        }
      });
      _popMenu.add(stackTraceItem);
      
    }
        
    private void _setupStackTraceFrame() {
      
      //DrJava.consoleOut().println("Stack Trace for Error: \n"+ e.stackTrace());
      JDialog _dialog = new JDialog(_frame,"JUnit Error Stack Trace",false);
      _stackFrame = _dialog;
      _stackTextArea = new JTextArea();
      _stackTextArea.setEditable(false);
      _stackTextArea.setLineWrap(false);
      JScrollPane scroll = new 
        BorderlessScrollPane(_stackTextArea, 
                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      
      ActionListener closeListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {        
          _stackFrame.hide();
        }
      };
      JButton closeButton = new JButton("Close");
      closeButton.addActionListener(closeListener);
      JPanel closePanel = new JPanel(new BorderLayout());
      closePanel.setBorder(new EmptyBorder(5,5,0,0));
      closePanel.add(closeButton, BorderLayout.EAST);
      JPanel cp = new JPanel(new BorderLayout());
      _dialog.setContentPane(cp);
      cp.setBorder(new EmptyBorder(5,5,5,5));
      cp.add(scroll, BorderLayout.CENTER);
      cp.add(closePanel, BorderLayout.SOUTH);
      JPanel topPanel = new JPanel(new GridLayout(0,1,0,5));
      topPanel.setBorder(new EmptyBorder(0,0,5,0));
      topPanel.add(_fileLabel);
      topPanel.add(_testLabel);
      topPanel.add(_errorLabel);
      cp.add(topPanel, BorderLayout.NORTH);
      _dialog.setSize(600, 500);
      /**
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension frameSize = _stackFrame.getSize();
      _stackFrame.setLocation((screenSize.width - frameSize.width) / 2,
                              (screenSize.height - frameSize.height) / 2);
      **/
      // initial location is relative to parent (MainFrame)
      _dialog.setLocationRelativeTo(_frame);
      //_stackFrame.setResizable(false);
      
    }
    
    private void _displayStackTrace (JUnitError e) {
      _errorLabel.setText((e.isWarning() ? "Error: " : "Failure: ") +
                          e.message());
      _fileLabel.setText("File: "+e.file().getName());
      _testLabel.setText("Test: "+e.testName());
      _stackTextArea.setText(e.stackTrace());
      _stackTextArea.setCaretPosition(0);
      _stackFrame.show();
    }
    
    /**
     * Returns true if the errors should be highlighted in the source
     * @return the status of the JCheckBox _showHighlightsCheckBox
     */
    public boolean shouldShowHighlightsInSource() {
      return _showHighlightsCheckBox.isSelected();
    }
    
    /**
     * Get the index of the current error in the error array.
     */
    public int getSelectedIndex() { return _selectedIndex; }

    /**
     * Allows the ErrorListPane to remember which DefinitionsPane
     * currently has an error highlight.
     */
    public void setLastDefPane(DefinitionsPane pane) {
      _lastDefPane = pane;
    }

    /**
     * Gets the last DefinitionsPane with an error highlight.
     */
    public DefinitionsPane getLastDefPane() {
      return _lastDefPane;
    }

    /**
     * Returns JUnitError associated with the given visual coordinates.
     * Returns null if none.
     */
    private JUnitError _errorAtPoint(Point p) {
      int modelPos = viewToModel(p);

      if (modelPos == -1)
        return null;

      // Find the first error whose position preceeds this model position
      int errorNum = -1;
      for (int i = 0; i < _errorListPositions.length; i++) {
        if (_errorListPositions[i].getOffset() <= modelPos) {
          errorNum = i;
        }
        else { // we've gone past the correct error; the last value was right
          break;
        }
      }

      if (errorNum >= 0) {
        return (JUnitError) _errorTable.get(_errorListPositions[errorNum]);
      }
      else {
        return null;
      }
    }

    /**
     * Returns the index into _errorListPositions corresponding
     * to the given JUnitError.
     */
    private int _getIndexForError(JUnitError error) {
      if (error == null) {
        throw new IllegalArgumentException("Couldn't find index for null error");
      }

      for (int i = 0; i < _errorListPositions.length; i++) {
        JUnitError e = (JUnitError)
          _errorTable.get(_errorListPositions[i]);

        if (error.equals(e)) {
          return i;
        }
      }

      throw new IllegalArgumentException("Couldn't find index for error " + error);
    }

    /**
     * Update the pane which holds the list of errors for the viewer.
     */
    public void updateListPane(boolean haveTestsRun) {
      try {
        _errorListPositions = new Position[_numErrors];
        _errorTable.clear();

        if (_numErrors == 0) {
          _updateNoErrors(haveTestsRun);
        }
        else {
          _updateWithErrors();
        }
      }
      catch (BadLocationException e) {
        throw new UnexpectedException(e);
      }

      // Force UI to redraw
      revalidate();
    }

    /** Puts the error pane into "compilation in progress" state. */
    public void setJUnitInProgress() {
      _errorListPositions = new Position[0];

      DefaultStyledDocument doc = new DefaultStyledDocument();

      try {
        doc.insertString(0,
                         "Testing in progress, please wait ...",
                         NORMAL_ATTRIBUTES);
      }
      catch (BadLocationException ble) {
        throw new UnexpectedException(ble);
      }

      setDocument(doc);

      selectNothing();
    }

    /**
     * Used to show that the last compile was successful.
     */
    private void _updateNoErrors(boolean haveTestsRun) throws BadLocationException {
      DefaultStyledDocument doc = new DefaultStyledDocument();
      
      String msg = (haveTestsRun) ? "All tests completed successfully." : "";
      
      doc.insertString(0,
                       msg,
                       NORMAL_ATTRIBUTES);
      setDocument(doc);

      selectNothing();
    }

    /**
     * Used to show that the last compile was unsuccessful.
     */
    private void _updateWithErrors() throws BadLocationException {
      DefaultStyledDocument doc = new DefaultStyledDocument();
      int errorNum = 0;

      // Show errors for each file
      OpenDefinitionsDocument openDoc = _model.getActiveDocument();
      JUnitErrorModel errorModel = openDoc.getJUnitErrorModel();
      JUnitError[] errorsWithPositions = errorModel.getErrorsWithPositions();
      JUnitError[] errorsWithoutPositions = errorModel.getErrorsWithoutPositions();
      
      if ((errorsWithoutPositions.length > 0) ||
            (errorsWithPositions.length > 0)) {

        // Grab filename for this set of errors
        String filename = openDoc.getFilename();
        
        // Show errors without source locations
        for (int j = 0; j < errorsWithoutPositions.length; j++, errorNum++) {
          int startPos = doc.getLength();
          
          doc.insertString(doc.getLength(), "================\n", NORMAL_ATTRIBUTES);
          
          doc.insertString(doc.getLength(), "File: ", BOLD_ATTRIBUTES);
          doc.insertString(doc.getLength(), filename + "\n", NORMAL_ATTRIBUTES);

          _insertErrorText(errorsWithoutPositions, j, doc);
          
          // Note to user that there is no source info for this error
          doc.insertString(doc.getLength(),
                           " (no source location)",
                           NORMAL_ATTRIBUTES);
          doc.insertString(doc.getLength(), "\n", NORMAL_ATTRIBUTES);
          
          Position pos = doc.createPosition(startPos);
          _errorListPositions[errorNum] = pos;
          _errorTable.put(pos, errorsWithoutPositions[j]);
        }
        
        
        // Show errors with source locations
        for (int j = 0; j < errorsWithPositions.length; j++, errorNum++) {
          int startPos = doc.getLength();
          JUnitError currError = errorsWithPositions[j];
          
          //WARNING: the height of the highlight box in JUnitError panel is dependent on the 
          // presence of this extra line. If removed, code must be changed in order to account for its
          // absence.
          doc.insertString(doc.getLength(), "================\n", NORMAL_ATTRIBUTES);
          
          // Show file
          doc.insertString(doc.getLength(), "File: ", BOLD_ATTRIBUTES);
          String fileAndLineNumber = filename + "  [line: " + (currError.lineNumber()+1) + "]";
          doc.insertString(doc.getLength(), fileAndLineNumber + "\n", NORMAL_ATTRIBUTES);

          // Show error
          _insertErrorText(errorsWithPositions, j, doc);
          doc.insertString(doc.getLength(), "\n", NORMAL_ATTRIBUTES);
          Position pos = doc.createPosition(startPos);
          _errorListPositions[errorNum] = pos;
          _errorTable.put(pos, errorsWithPositions[j]);
        }
      }

      setDocument(doc);

      // Select the first error
      _errorListPane.switchToError(0);
    }

    /**
     * Puts an error message into the array of errors at the specified index.
     * @param array the array of errors
     * @param i the index at which the message will be inserted
     * @param doc the document in the error pane
     */
    private void _insertErrorText(JUnitError[] array, int i, Document doc)
      throws BadLocationException
      {
        JUnitError error = array[i];

        doc.insertString(doc.getLength(), "Test: ", BOLD_ATTRIBUTES);
        doc.insertString(doc.getLength(), error.testName(), NORMAL_ATTRIBUTES);
        doc.insertString(doc.getLength(), "\n", NORMAL_ATTRIBUTES);

        //TO DO: change isWarning to isError
        if (error.isWarning()) {
          doc.insertString(doc.getLength(), "Error: ", BOLD_ATTRIBUTES);
        }
        else {
          doc.insertString(doc.getLength(), "Failure: ", BOLD_ATTRIBUTES);
        }
        
        doc.insertString(doc.getLength(), error.message(), NORMAL_ATTRIBUTES);
        
      }

    /**
     * When the selection of the current error changes, remove
     * the highlight in the error pane.
     */
    private void _removeListHighlight() {
      //System.out.println("_removeHighlight():  _listHighlightTag == "+_listHighlightTag);
      if (_listHighlightTag != null) {
       _listHighlightTag.remove();
        _listHighlightTag = null;
      }
    }

    /**
     * Don't select any errors in the error pane.
     */
    public void selectNothing() {
      _selectedIndex = -1;
      _removeListHighlight();
      _resetEnabledStatus();

      // Remove highlight from the defPane that has it
      _lastDefPane.removeErrorHighlight();
    }

    /**
     * Selects the given error inside the error list pane.
     */
    public void selectItem(JUnitError error) {
      // Find corresponding index
      int i = _getIndexForError(error);

      _selectedIndex = i;
      _removeListHighlight();

      int startPos = _errorListPositions[i].getOffset() + 16; //16 ='s for the beginning line
      
      // end pos is either the end of the document (if this is the last error)
      // or the char where the next error starts
      int endPos;
      if (i + 1 >= (_numErrors)) {
        endPos = getDocument().getLength();
      }
      else {
        endPos = _errorListPositions[i + 1].getOffset();
      }

      try {
        _listHighlightTag =
          _highlightManager.addHighlight(startPos,
                                         endPos,
                                         _listHighlightPainter);

        // Scroll to make sure this item is visible
        Rectangle startRect = modelToView(startPos);
        Rectangle endRect = modelToView(endPos - 1);

        //System.err.println("error = " + error + " i = " + i + " startPos = " + startPos + " startRect = " + startRect);
        
        // Add the end rect onto the start rect to make a rectangle
        // that encompasses the entire error
        startRect.add(endRect);

        //System.err.println("scrll vis: " + startRect);

        scrollRectToVisible(startRect);

      }
      catch (BadLocationException badBadLocation) {}

      _resetEnabledStatus();
    }

    /**
     * Change all state to select a new error, including moving the
     * caret to the error, if a corresponding position exists.
     * @param doc OpenDefinitionsDocument containing this error
     * @param errorNum Error number, which is either in _errorsWithoutPositions
     * (if errorNum < _errorsWithoutPositions.length) or in _errors (otherwise).
     * If it's in _errors, we need to subtract _errorsWithoutPositions.length
     * to get the index into the array.
     */
    void switchToError(JUnitError error) {
      if (error == null) return;

      // check and see if this error is without source info, and
      // if so don't try to highlight source info!
      boolean errorHasLocation = (error.lineNumber() > -1);
      
      try {
      
        OpenDefinitionsDocument doc = _model.getDocumentForFile(error.file());
        JUnitErrorModel errorModel = doc.getJUnitErrorModel();
        
        if (errorHasLocation) {
          JUnitError[] errorsWithPositions = errorModel.getErrorsWithPositions();
          //System.out.println("error has location" +error.lineNumber() + " " + error);
          //System.out.println("error size: " + errors.length);
          //for (int i=0; i< errors.length; i++) {
          //  System.out.println("errors [" + i + "] " + errors[i].lineNumber() + " " + errors[i]);
          //}
          
          int index = Arrays.binarySearch(errorsWithPositions, error);
          //System.out.println("index of error: " + index);
          if (index >= 0) {
            _gotoErrorSourceLocation(doc, index);
          }

        }
        
        else {
          // Remove last highlight
          _lastDefPane.removeErrorHighlight();
          
          DefinitionsPane defPane = _frame.getCurrentDefPane();
          defPane.getJUnitErrorCaretListener().shouldHighlight(false);
          
          // still switch to document despite the fact that the error has no lineNum
          _model.setActiveDocument(doc);
          _removeListHighlight();
        }
      }
      catch (IOException ioe) {
        // Don't highlight the source if file can't be opened
      }
      
      // Select item wants the error, which is what we were passed
      _errorListPane.selectItem(error);
      
    }

    /**
     * Another interface to switchToError.
     * @param index Index into the array of positions in the ErrorListPane
     */
    void switchToError(int index) {
      if ((index >= 0) && (index < _errorListPositions.length)) {
        Position pos = _errorListPositions[index];
        JUnitError error = (JUnitError) _errorTable.get(pos);
        switchToError(error);
      }
    }

    /**
     * Jumps to error location in source
     * @param doc OpenDefinitionsDocument containing the error
     * @param idx Index into _errors array
     */
    private void _gotoErrorSourceLocation(OpenDefinitionsDocument doc,
                                          final int idx) {
      JUnitErrorModel errorModel = doc.getJUnitErrorModel();
      Position[] positions = errorModel.getPositions();
         
      //System.out.println("index clicked: " + idx);
      
      if ((idx < 0) || (idx >= positions.length)) return;
      //System.out.println("index clicked: " + idx);
      //System.out.println("position: " + errors[idx].lineNumber());
      
      int errPos = positions[idx].getOffset();
 
      //set the active document (implicit call to updateHighlight() )
      _model.setActiveDocument(doc);
     
      //set caret then grab focus
       // switch to correct def pane, and inform it that it should highlight
      DefinitionsPane defPane = _frame.getCurrentDefPane();
      defPane.getJUnitErrorCaretListener().shouldHighlight(true);
      defPane.setCaretPosition(errPos);
      defPane.grabFocus();
      defPane.getJUnitErrorCaretListener().updateHighlight(errPos);
    }
    
     /**
     * The OptionListener for compiler COMPILER_ERROR_COLOR 
     */
    private class CompilerErrorColorOptionListener implements OptionListener<Color> {
      
      public void optionChanged(OptionEvent<Color> oce) {

        _listHighlightPainter
          =  new DefaultHighlighter.DefaultHighlightPainter(oce.value);
       
        if (_listHighlightTag != null) {
          _listHighlightTag.refresh(_listHighlightPainter);
        }
      }
    }
    
  }

}
