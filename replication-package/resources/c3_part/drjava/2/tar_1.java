/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2008, JavaPLT group at Rice University (drjava@rice.edu)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of DrJava, the JavaPLT group, Rice University, nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software is Open Source Initiative approved Open Source Software.
 * Open Source Initative Approved is a trademark of the Open Source Initiative.
 * 
 * This file is part of DrJava.  Download the current version of this project
 * from http://www.drjava.org/ or http://sourceforge.net/projects/drjava/
 * 
 * END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.ui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.Version;
import edu.rice.cs.drjava.config.OptionConstants;
import edu.rice.cs.drjava.platform.*;
import edu.rice.cs.util.MutableReference;
import edu.rice.cs.util.swing.Utilities;
import edu.rice.cs.util.swing.UneditableTableModel;
import edu.rice.cs.util.swing.BorderlessScrollPane;

/** Asks whether DrJava may contact the DrJava developers and send information about
  * the operating system and the Java version used.
  * @version $Id$
  */
public class DrJavaSurveyPopup extends JDialog {
  /** the keys of the system properties that we want to send */
  public static final String[] DRJAVA_SURVEY_KEYS = new String[] {"os.name","os.version","java.version","java.vendor"};
  
  /** the no button */
  private JButton _noButton;
  /** the yes button */
  private JButton _yesButton;
  /** the parent frame */
  private MainFrame _mainFrame;
  /** the version information pane */
  private JOptionPane _questionPanel;
  /** the table with the information that DrJava will send */
  private JTable _propertiesTable;
  /** the panel with the buttons and combobox */
  private JPanel _bottomPanel;
  /** don't ask user again */
  private JCheckBox _neverAskAgain;
  
  /** Creates a window to display whether a new version of DrJava is available. */
  public DrJavaSurveyPopup(MainFrame parent) {
    super(parent, "Send System Information to DrJava Developers");
    setResizable(false);
    setSize(550,350);
    _mainFrame = parent;

    _yesButton = new JButton(_yesAction);
    _noButton = new JButton(_noAction);
    _neverAskAgain = new JCheckBox("Never ask me again",
                                   !DrJava.getConfig().getSetting(OptionConstants.DIALOG_DRJAVA_SURVEY_ENABLED).booleanValue());
    _neverAskAgain.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        DrJava.getConfig().setSetting(OptionConstants.DIALOG_DRJAVA_SURVEY_ENABLED, !_neverAskAgain.isSelected());
      }
    });

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(_neverAskAgain);
    buttonPanel.add(_yesButton);
    buttonPanel.add(_noButton);

    _questionPanel = new JOptionPane("May DrJava anonymously send the information\nbelow to the DrJava developers?",
                                     JOptionPane.QUESTION_MESSAGE,JOptionPane.DEFAULT_OPTION,null,
                                     new Object[0]);
    int size = DRJAVA_SURVEY_KEYS.length;
    String[][] rowData = new String[size][2];
    int rowNum = 0;
    for(String k: DRJAVA_SURVEY_KEYS) {
      rowData[rowNum][0] = k;
      rowData[rowNum][1] = System.getProperty(k);
      ++rowNum;
    }
    java.util.Arrays.sort(rowData,new java.util.Comparator<String[]>() {
      public int compare(String[] o1, String[] o2) {
        return o1[0].compareTo(o2[0]);
      }
    });
    String[] nvStrings = new String[] {"Name","Value"};
    UneditableTableModel model = new UneditableTableModel(rowData, nvStrings);
    _propertiesTable = new JTable(model);
    JScrollPane scroller = new BorderlessScrollPane(_propertiesTable);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(_questionPanel, BorderLayout.NORTH);
    centerPanel.add(scroller, BorderLayout.CENTER);
    
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(centerPanel, BorderLayout.CENTER);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    _mainFrame.setPopupLoc(this);
  }
  
  /* Close the window. */
  private Action _noAction = new AbstractAction("No") {
    public void actionPerformed(ActionEvent e) { noAction(); }
  };

  /** Close this window, but display the full DrJava Errors window. */
  private Action _yesAction = new AbstractAction("Yes") {
    public void actionPerformed(ActionEvent e) { yesAction(); }
  };

  protected void noAction() {
    // set the date we asked even if the user pressed no
    // so the user won't be bothered the next time he starts DrJava
    // next popup will occur in DRJAVA_SURVEY_DAYS (91) days.
    DrJava.getConfig().setSetting(OptionConstants.LAST_DRJAVA_SURVEY, new Date().getTime());
    setVisible(false);
    dispose();
  }

  public static final edu.rice.cs.util.Log LOG = new edu.rice.cs.util.Log("survey.txt",false);

  protected void yesAction() {
    try {
      final String DRJAVA_SURVEY_PAGE = "http://www.drjava.org/submit-usage.php?";
      StringBuilder sb = new StringBuilder();
      sb.append(DRJAVA_SURVEY_PAGE);
//    sb.append("date=");
//    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
//    sb.append(sdf.format(new Date()));
      boolean first = true;
      for(String k: DRJAVA_SURVEY_KEYS) {
        if (first) { first = false; } else { sb.append('&'); }
        sb.append(k);
        sb.append('=');
        sb.append(System.getProperty(k));
      }
      LOG.log(sb.toString());
      String result = sb.toString().replaceAll(" ","%20");
      LOG.log(result);
      
      // check how many days have passed since the last survey
      int days = DrJava.getConfig().getSetting(OptionConstants.DRJAVA_SURVEY_DAYS);
      java.util.Date nextCheck = 
        new java.util.Date(DrJava.getConfig().getSetting(OptionConstants.LAST_DRJAVA_SURVEY)
                             + days * 24L * 60 * 60 * 1000); // x days after last check; 24L ensures long accumulation
      if (!(new java.util.Date().after(nextCheck)) &&
          (DrJava.getConfig().getSetting(OptionConstants.LAST_DRJAVA_SURVEY_RESULT).equals(result))) {
        // not enough days have passed, and the configuration has not changed, quietly terminate
        return;
      }
      
      BufferedReader br = null;
      try {
        URL url = new URL(result);
        InputStream urls = url.openStream();
        InputStreamReader is = new InputStreamReader(urls);
        br = new BufferedReader(is);
        String line;
        sb.setLength(0);
        while((line = br.readLine()) != null) { sb.append(line); sb.append(System.getProperty("line.separator")); }
        LOG.log(sb.toString());
        DrJava.getConfig().setSetting(OptionConstants.LAST_DRJAVA_SURVEY, new Date().getTime());
        DrJava.getConfig().setSetting(OptionConstants.LAST_DRJAVA_SURVEY_RESULT, result);
      }
      catch(IOException e) {
        // could not open URL using Java, try web browser
        LOG.log("Could not open URL using Java", e);
        try {
          PlatformFactory.ONLY.openURL(new URL(result));
          DrJava.getConfig().setSetting(OptionConstants.LAST_DRJAVA_SURVEY_RESULT, result);
        }
        catch(IOException e2) {
          // could not open using Java or web browser, ignore
          LOG.log("Could not open URL using web browser", e2);
        }
      }
      finally { // close open input stream
        try { if (br!=null) br.close(); }
        catch(IOException e) { /* ignore */ }
      }
    }
    finally {
      noAction();
    }
  }
  
  /** Lambda doing nothing. */
  protected final edu.rice.cs.util.Lambda<Void,WindowEvent> NO_OP 
    = new edu.rice.cs.util.Lambda<Void,WindowEvent>() {
    public Void apply(WindowEvent e) {
      return null;
    }
  };
  
  /** Lambda that calls noAction. */
  protected final edu.rice.cs.util.Lambda<Void,WindowEvent> NO
    = new edu.rice.cs.util.Lambda<Void,WindowEvent>() {
    public Void apply(WindowEvent e) {
      noAction();
      return null;
    }
  };

 
  /** Toggle visibility of this frame. Warning, it behaves like a modal dialog. */
  public void setVisible(boolean vis) {
    assert EventQueue.isDispatchThread();
    validate();
    if (vis) {
      _mainFrame.hourglassOn();
      _mainFrame.installModalWindowAdapter(this, NO_OP, NO);
    }
    else {
      _mainFrame.removeModalWindowAdapter(this);
      _mainFrame.hourglassOff();
      _mainFrame.toFront();
    }
    super.setVisible(vis);
  }
}
