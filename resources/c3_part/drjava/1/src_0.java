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

package edu.rice.cs.drjava.model;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.util.List;

import edu.rice.cs.drjava.model.definitions.indent.Indenter;

/**
 * Tests the indenting functionality on the level of the GlobalModel.
 * Not only are we testing that the document turns out right, but also
 * that the cursor position in the document is consistent with a standard.
 * @version $Id$
 */
public final class GlobalIndentTest extends GlobalModelTestCase {
  private static final String FOO_EX_1 = "public class Foo {\n";
  private static final String FOO_EX_2 = "int foo;\n";
  private static final String BAR_CALL_1 = "bar(monkey,\n";
  private static final String BAR_CALL_2 = "banana)\n";
//  private static final String BEAT_1 = "void beat(Horse dead,\n";
//  private static final String BEAT_2 = "          Stick pipe)\n";

  /**
   * Tests indent that increases the size of the tab when the
   * cursor is at the start of the line.  When the cursor is in the
   * whitespace before the first word on a line, indent always
   * moves the cursor up to the beginning of the first non-whitespace
   * character.
   * @throws BadLocationException
   */
  public void testIndentGrowTabAtStart()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();
    
    openDoc.insertString(0, FOO_EX_1, null);
    openDoc.insertString(FOO_EX_1.length(), " " + FOO_EX_2, null);
    openDoc.setCurrentLocation(FOO_EX_1.length());
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_1 + "  " + FOO_EX_2, openDoc);
    _assertLocation(FOO_EX_1.length() + 2, openDoc);
  }

  /**
   * Tests indent that increases the size of the tab when the
   * cursor is in the middle of the line.  The cursor stays in the
   * same place.
   * @throws BadLocationException
   */
  public void testIndentGrowTabAtMiddle()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();
    
    openDoc.insertString(0, FOO_EX_1, null);
    openDoc.insertString(FOO_EX_1.length(), " " + FOO_EX_2, null);
    openDoc.setCurrentLocation(FOO_EX_1.length() + 5);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_1 + "  " + FOO_EX_2, openDoc);
    _assertLocation(FOO_EX_1.length() + 6, openDoc);
  }

  /**
   * Tests indent that increases the size of the tab when the
   * cursor is at the end of the line.  The cursor stays in the
   * same place.
   * @throws BadLocationException
   */
  public void testIndentGrowTabAtEnd()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();
    
    openDoc.insertString(0, FOO_EX_1, null);
    openDoc.insertString(FOO_EX_1.length(), " " + FOO_EX_2, null);
    openDoc.setCurrentLocation(openDoc.getLength() - 1);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_1 + "  " + FOO_EX_2, openDoc);
    _assertLocation(openDoc.getLength() - 1, openDoc);
  }

  /**
   * Tests indent that increases the size of the tab when the
   * cursor is at the start of the line.  When the cursor is in the
   * whitespace before the first word on a line, indent always
   * moves the cursor up to the beginning of the first non-whitespace
   * character.
   * @throws BadLocationException
   */
  public void testIndentShrinkTabAtStart()
      throws BadLocationException, OperationCanceledException{
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, FOO_EX_1, null);
    openDoc.insertString(FOO_EX_1.length(), "   " + FOO_EX_2, null);
    openDoc.setCurrentLocation(FOO_EX_1.length());
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_1 + "  " + FOO_EX_2, openDoc);
    _assertLocation(FOO_EX_1.length() + 2, openDoc);
  }

  /**
   * Tests indent that increases the size of the tab when the
   * cursor is in the middle of the line.  The cursor stays in the
   * same place.
   * @throws BadLocationException
   */
  public void testIndentShrinkTabAtMiddle()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, FOO_EX_1, null);
    openDoc.insertString(FOO_EX_1.length(), "   " + FOO_EX_2, null);
    openDoc.setCurrentLocation(FOO_EX_1.length() + 5);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_1 + "  " + FOO_EX_2, openDoc);
    _assertLocation(FOO_EX_1.length() + 4, openDoc);
  }

  /**
   * Tests indent that increases the size of the tab when the
   * cursor is at the end of the line.  The cursor stays in the
   * same place.
   * @throws BadLocationException
   */
  public void testIndentShrinkTabAtEnd()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, FOO_EX_1, null);
    openDoc.insertString(FOO_EX_1.length(), "   " + FOO_EX_2, null);
    openDoc.setCurrentLocation(openDoc.getLength() - 1);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_1 + "  " + FOO_EX_2, openDoc);
    _assertLocation(openDoc.getLength() - 1, openDoc);
  }

  /**
   * Do an indent that should match up with the indent on the line above.
   * The cursor is at the start of the line.
   * @exception BadLocationException
   */
  public void testIndentSameAsLineAboveAtStart()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, FOO_EX_2, null);
    openDoc.insertString(FOO_EX_2.length(), "   " + FOO_EX_2, null);
    openDoc.setCurrentLocation(FOO_EX_2.length());
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_2 + FOO_EX_2, openDoc);
    _assertLocation(FOO_EX_2.length(), openDoc);
  }

  /**
   * Do an indent that should match up with the indent on the line above.
   * The cursor is at the end of the line.
   * @exception BadLocationException
   */
  public void testIndentSameAsLineAboveAtEnd()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, FOO_EX_2, null);
    openDoc.insertString(FOO_EX_2.length(), "   " + FOO_EX_2, null);
    openDoc.setCurrentLocation(openDoc.getLength() - 1);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_2 + FOO_EX_2, openDoc);
    _assertLocation(openDoc.getLength() - 1, openDoc);
  }

  /**
   * Do an indent that follows the behavior in line with parentheses.
   * The cursor is at the start of the line.
   * @exception BadLocationException
   */
  public void testIndentInsideParenAtStart()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, BAR_CALL_1, null);
    openDoc.insertString(BAR_CALL_1.length(), BAR_CALL_2, null);
    openDoc.setCurrentLocation(BAR_CALL_1.length());
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(BAR_CALL_1 + "    " + BAR_CALL_2, openDoc);
    _assertLocation(BAR_CALL_1.length() + 4, openDoc);
  }

  /**
   * Do an indent that follows the behavior in line with parentheses.
   * The cursor is at the end of the line.
   * @exception BadLocationException
   */
  public void testIndentInsideParenAtEnd()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, BAR_CALL_1, null);
    openDoc.insertString(BAR_CALL_1.length(), BAR_CALL_2, null);
    openDoc.setCurrentLocation(openDoc.getLength() - 1);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(BAR_CALL_1 + "    " + BAR_CALL_2, openDoc);
    _assertLocation(openDoc.getLength() - 1, openDoc);
  }

  /**
   * Indent does nothing to change the document when everything is in place.
   */
  public void testIndentDoesNothing()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, FOO_EX_2 + FOO_EX_2, null);
    openDoc.setCurrentLocation(openDoc.getLength() - 1);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc, Indenter.OTHER, null);
    _assertContents(FOO_EX_2 + FOO_EX_2, openDoc);
    _assertLocation(openDoc.getLength() - 1, openDoc);
  }


  /**
   * The quintessential "make the squiggly go to the start, even though
   * method arguments extend over two lines" test.  This behavior is not
   * correctly followed yet, so until it is, leave this method commented.
   * @exception BadLocationException
   *
  public void testIndentSquigglyAfterTwoLines()
      throws BadLocationException, OperationCanceledException {
    OpenDefinitionsDocument openDoc = _getOpenDoc();

    openDoc.insertString(0, BEAT_1, null);
    openDoc.insertString(BEAT_1.length(), BEAT_2, null);
    openDoc.insertString(openDoc.getLength(), "{", null);
    int loc = openDoc.getCurrentLocation();
    openDoc.indentLinesInDefinitions(loc, loc);
    _assertContents(BEAT_1 + BEAT_2 + "{", openDoc);
    _assertLocation(openDoc.getLength(), openDoc);
  }
*/

  /**
   * Indents block comments with stars as they should.
   * Uncomment this method when the correct functionality is implemented.
   */
//  public void testIndentBlockCommentStar()
//      throws BadLocationException, OperationCanceledException {
//    OpenDefinitionsDocument openDoc = _getOpenDoc();
//    openDoc.insertString(0, "/*\n*\n*/\n " + FOO_EX_2, null);
//    int loc = openDoc.getCurrentLocation();
//    openDoc.indentLinesInDefinitions(0, openDoc.getLength());
//    _assertContents("/*\n *\n */\n" + FOO_EX_2, openDoc);
//    _assertLocation(openDoc.getLength(), openDoc);
//  }

  /**
   * Get the only open definitions document.
   */
  private OpenDefinitionsDocument _getOpenDoc() {
    _assertNumOpenDocs(0);
    OpenDefinitionsDocument doc = _model.newFile();
    doc.setDefinitionsIndent(2);
    List<OpenDefinitionsDocument> docs = _model.getDefinitionsDocuments();
    _assertNumOpenDocs(1);
    return docs.get(0);
  }

  private void _assertNumOpenDocs(int num) {
    assertEquals("number of open documents",
                 num,
                 _model.getDefinitionsDocuments().size());
  }

  private void _assertContents(String expected, Document document)
    throws BadLocationException
  {
    assertEquals("document contents", expected,
                 document.getText(0, document.getLength()));
  }

  private void _assertLocation(int loc, OpenDefinitionsDocument openDoc) {
    assertEquals("current def'n loc", loc,
                 openDoc.getCurrentLocation());
  }
}
