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

package edu.rice.cs.drjava.model.cache;

import junit.framework.TestCase;

import java.io.*;
import javax.swing.text.*;

import edu.rice.cs.drjava.model.*;
import edu.rice.cs.drjava.model.definitions.*;

/**
 * 
 */
public class DocumentCacheTest extends GlobalModelTestCase {
  
  private DocumentCache _cache;
  
  
  private int _doc_made;
  private int _doc_saved;
  
  public void setUp() throws IOException {
    super.setUp();
    _cache = _model.getDocumentCache();
    _cache.setCacheSize(4);
    _doc_made = 0;
    _doc_saved = 0;
  }
  
  protected OpenDefinitionsDocument openFile(final File f) throws IOException{
    try{
      OpenDefinitionsDocument doc = _model.openFile(new FileOpenSelector(){        
        public File getFile() {
          return f;
        }
        
        public File[] getFiles() {
          return new File[] {f};
        }
      });
      return doc;
    }catch(AlreadyOpenException e){
      throw new IOException(e.getMessage());
    }catch(OperationCanceledException e){
      throw new IOException(e.getMessage());
    }
  }
  /**
   * a good warmup test case
   */
  public void testCacheSize() {
    _cache.setCacheSize(6);
    assertEquals("Wrong cache size", 6, _cache.getCacheSize());
  }
  
  public void testDocumentsInAndOutOfTheCache() throws BadLocationException, IOException {
    assertEquals("Wrong Cache Size", 4, _cache.getCacheSize());
    
    // The documents should not be activated upon creation
    OpenDefinitionsDocument doc1 =  _model.newFile();
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc1));
    assertEquals("There should be 0 documents in the cache", 0, _cache.getNumInCache());
    OpenDefinitionsDocument doc2 =  _model.newFile();
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc2));
    assertEquals("There should be 0 documents in the cache", 0, _cache.getNumInCache());
    OpenDefinitionsDocument doc3 =  _model.newFile();
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc3));
    assertEquals("There should be 0 documents in the cache", 0, _cache.getNumInCache());
    OpenDefinitionsDocument doc4 =  _model.newFile();
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc4));
    assertEquals("There should be 0 documents in the cache", 0, _cache.getNumInCache());
    OpenDefinitionsDocument doc5 =  _model.newFile();
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc5));
    assertEquals("There should be 0 documents in the cache", 0, _cache.getNumInCache());
    OpenDefinitionsDocument doc6 =  _model.newFile();
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc6));
    assertEquals("There should be 0 documents in the cache", 0, _cache.getNumInCache());
    
    // checkin isModifiedSinceSave shouldn't activate the documents
    assertFalse("Document 1 shouldn't be modified", doc1.isModifiedSinceSave());
    assertFalse("Document 2 shouldn't be modified", doc2.isModifiedSinceSave());
    assertFalse("Document 3 shouldn't be modified", doc3.isModifiedSinceSave());
    assertFalse("Document 4 shouldn't be modified", doc4.isModifiedSinceSave());
    assertFalse("Document 5 shouldn't be modified", doc5.isModifiedSinceSave());
    assertFalse("Document 6 shouldn't be modified", doc6.isModifiedSinceSave());
    
    assertEquals("There should still be 0 documents in the cache", 0, _cache.getNumInCache());
    
    // Activate all documents and make sure that the right ones get kicked out
    doc1.getLength();
    doc2.getLength();
    doc3.getLength();
    doc4.getLength();
    doc5.getLength();
    doc6.getLength();
    doc1.getLength();
    doc2.getLength();
    doc3.getLength();
    doc4.getLength();
    assertFalse("The document 5 should have been kicked out of the cache", _cache.isDDocInCache(doc5));
    assertFalse("The document 6 should have been kicked out of the cache", _cache.isDDocInCache(doc6));
    
    assertEquals("There should be 4 documents in the cache", 4, _cache.getNumInCache());
    
    // Test the LRU to make sure the documents are kicked out in the right order
    doc5.getLength();
    assertFalse("doc1 should have been kicked out first", _cache.isDDocInCache(doc1));
    doc6.getLength();
    assertFalse("doc2 should have been kicked out first", _cache.isDDocInCache(doc2));
  }
  
  public void testGetDDocFromCache() throws BadLocationException, IOException, OperationCanceledException {
    File file1 = tempFile(1);
    File file2 = tempFile(2);
    File file3 = tempFile(3);
    File file4 = tempFile(4);
    File file5 = tempFile(5);
    File file6 = tempFile(6);
    
    // opening a document should set it as active
    OpenDefinitionsDocument doc1 = openFile(file1);
    assertTrue("The document should not start out in the cache", _cache.isDDocInCache(doc1));
    assertEquals("There should be 1 documents in the cache", 1, _cache.getNumInCache());
    OpenDefinitionsDocument doc2 = openFile(file2);
    assertTrue("The document should not start out in the cache", _cache.isDDocInCache(doc2));
    assertEquals("There should be 2 documents in the cache", 2, _cache.getNumInCache());
    OpenDefinitionsDocument doc3 = openFile(file3);
    assertTrue("The document should not start out in the cache", _cache.isDDocInCache(doc3));
    assertEquals("There should be 3 documents in the cache", 3, _cache.getNumInCache());
    OpenDefinitionsDocument doc4 = openFile(file4);
    assertTrue("The document should not start out in the cache", _cache.isDDocInCache(doc4));
    assertEquals("There should be 4 documents in the cache", 4, _cache.getNumInCache());
    OpenDefinitionsDocument doc5 = openFile(file5);
    assertTrue("The document should not start out in the cache", _cache.isDDocInCache(doc5));
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc1));
    assertEquals("There should be 4 documents in the cache", 4, _cache.getNumInCache());
    OpenDefinitionsDocument doc6 = openFile(file6);
    assertTrue("The document should not start out in the cache", _cache.isDDocInCache(doc6));
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc2));
    assertEquals("There should be 4 documents in the cache", 4, _cache.getNumInCache());
  }
  
  public void testReconstructor() throws IOException{
    final int i = 0;
    DDReconstructor d = new DDReconstructor(){
      public DefinitionsDocument make(){
        _doc_made++;
        return null;
      }
      public void saveDocInfo(DefinitionsDocument doc){
        _doc_saved++;
      }
    };
    
    OpenDefinitionsDocument doc1 =  _model.newFile();
    assertFalse("The document should not start out in the cache", _cache.isDDocInCache(doc1));
    _cache.update(doc1, d);
    assertFalse("The document should not be in the cache after an update", _cache.isDDocInCache(doc1));
    
    _cache.get(doc1); // force the cache to reconstruct the document.

    assertEquals("The make in the reconstructor was called 1nce", 1, _doc_made);
    assertEquals("The save in the reconstructor was not called", 0, _doc_saved);
  }
  
  
  public void testNoDDocInCache(){
   OpenDefinitionsDocument doc1 = _model.newFile();
   assertTrue("The document should now be closed", _model.closeFile(doc1));
   try{
     doc1.getLength();
     fail("the open defintions document should not be in the cache");
   }catch(NoSuchDocumentException e){     
   }
  }


  public void testNumListeners(){
   OpenDefinitionsDocument doc1 = _model.newFile();
   OpenDefinitionsDocument doc2 = _model.newFile();
   OpenDefinitionsDocument doc3 = _model.newFile();
   OpenDefinitionsDocument doc4 = _model.newFile();
   OpenDefinitionsDocument doc5 = _model.newFile();

   int numDocListeners = doc1.getDocumentListeners().length;
   int numUndoListeners = doc1.getUndoableEditListeners().length;
   
   doc1.getLength();
   doc2.getLength();
   doc3.getLength();
   doc4.getLength();

   // this will kick document one out of the cache
   doc5.getLength();
 
   // this will reconstruct document 1
   doc1.getLength();
   
   assertEquals("the number of document listeners is the same after reconstruction", numDocListeners, doc1.getDocumentListeners().length);
   assertEquals("the number of undoableEditListeners is the same after reconstruction", numUndoListeners, doc1.getUndoableEditListeners().length);


   
  }
}
