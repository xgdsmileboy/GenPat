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

package koala.dynamicjava.interpreter;

import java.lang.reflect.*;
import java.util.*;

import koala.dynamicjava.interpreter.context.*;
import koala.dynamicjava.interpreter.error.*;
import koala.dynamicjava.interpreter.modifier.*;
import koala.dynamicjava.interpreter.throwable.*;
import koala.dynamicjava.tree.*;
import koala.dynamicjava.tree.visitor.*;
import koala.dynamicjava.util.*;
import koala.dynamicjava.parser.wrapper.*;

import junit.framework.TestCase;

import edu.rice.cs.drjava.model.repl.*;

/**
 * This test class tests only those methods that were modified in order to ensure 
 * that the wrapper classes involved in autoboxing/unboxing are allowed.&nbsp; The 
 * methods that were changed pertained to those sections of the JLS that were 
 * modified by Sun when introducing this new feature.
 * <P>Involved Wrapper Classes:</P>
 * <UL>
 *   <LI>Boolean
 *   <LI>Byte
 *   <LI>Character
 *   <LI>Short
 *   <LI>Integer
 *   <LI>Long
 *   <LI>Float
 *   <LI>Double</LI></UL>
 * Involved Operations
 * <UL>
 *   <LI>Assignment
 *   <LI>Method Invocation
 *   <LI>Casting
 *   <LI>Numeric Promotions (Unary and Binary)
 *   <LI>The <CODE>if</CODE> Statement (<CODE>if-then</CODE> and <CODE>if-then-else</CODE>)
 *   <LI>The <CODE>switch</CODE> Statement
 *   <LI>The <CODE>while</CODE> Statement
 *   <LI>The <CODE>do</CODE> Statement
 *   <LI>The <CODE>for</CODE> Statement
 *   <LI>Array Creation
 *   <LI>Unary Operations:</LI>
 *   <UL>
 *     <LI>Postfix Decrement Operator <CODE>--</CODE>
 *     <LI>Postfix Decrement Operator <CODE>--</CODE>
 *     <LI>Prefix Increment Operator <CODE>++</CODE>
 *     <LI>Prefix Decrement Operator <CODE>--</CODE>
 *     <LI>Plus Operator <CODE>+</CODE>
 *     <LI>Minus Operator <CODE>-</CODE>
 *     <LI>Bitwise Complement Operator <CODE>~</CODE>
 *     <LI>Logical Complement Operator <CODE>!</CODE></LI></UL>
 *   <LI>Binary Operators</LI>
 *   <UL>
 *     <LI>Multiplicative Operators <CODE>*, /, %</CODE>
 *     <LI>Additive Operators <CODE>+, -</CODE>
 *     <LI>Shift Operators <CODE>&lt;&lt;, &gt;&gt;, &gt;&gt;&gt;</CODE>
 *     <LI>Numerical Comparison Operators <CODE>&lt;, &lt;=, &gt;, and &gt;=</CODE>
 *     <LI>Integer Bitwise Operators <CODE>&amp;, ^, and |</CODE>
 *     <LI>Boolean Logical Operators <CODE>&amp;, ^, and |</CODE>
 *     <LI>Conditional Operators <CODE>&amp;&amp;, ||</CODE>
 *     <LI>Conditional Operator <CODE>? :</CODE></LI></UL>
 * </UL>
 * NOTE: Though not explicitly stated in the changed sections of the JLS, the methods 
 * associated with the assignment operators (<CODE>+=, -=, *=, /=, %=, &lt;&lt;=, &gt;&gt;&gt;=, 
 * &gt;&gt;&gt;=, &amp;=, ^=, |=</CODE>) must also be modified and thus tested
 */
public class TypeCheckerTest extends TestCase {
  
  ////// Internal Initialization ////////////////////////
  
  /**
   * The global context we are using.
   */
  private GlobalContext _globalContext;
  
  /**
   * The type checker we are testing.
   */
  private TypeChecker _typeChecker;
  
  /**
   * The interpreter we are using to test our modifications of the ASTs.
   */
  private JavaInterpreter _interpreter;
  
  /**
   * Sets up the tests for execution.
   */
  public void setUp() {
    _globalContext = new GlobalContext(new TreeInterpreter(new JavaCCParserFactory()));
    _globalContext.define("x", int.class);
    _globalContext.define("B", Boolean.class);
    _globalContext.define("b", boolean.class);
    _typeChecker = new TypeChecker(_globalContext);
    _interpreter = new DynamicJavaAdapter();
  }
  
  /**
   * Parses the given string and returns the list of Nodes.
   * @param code the code to parse
   * @return the list of Nodes
   */
  private List<Node> _parseCode(String code) {
    JavaCCParserFactory parserFactory = new JavaCCParserFactory();
    SourceCodeParser parser = parserFactory.createParser(new java.io.StringReader(code), "");
    return parser.parseStream();
  }
  
  ////// Control Statements /////////////////////////////
  
  /**
   * Tests the While statement's condition statement
   */
  public void testVisitWhileStatement() throws ExceptionReturnedException {
    String text = "while (B) { }";
    Node stmt = _parseCode(text).get(0);
    
    stmt.acceptVisitor(_typeChecker);

    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: booleanValue null (koala.dynamicjava.tree.QualifiedName: B))";
    String actual = ((WhileStatement)stmt).getCondition().toString();
    assertEquals("Should have autounboxed", expected, actual);
    
    _interpreter.interpret("Boolean B = Boolean.FALSE; " + text);
  }
  
  /**
   * Tests the do-while loop's condition statement
   */
  public void testVisitDoStatement() throws ExceptionReturnedException {
    String text = "do { } while(B);";
    Node stmt = _parseCode(text).get(0);
    
    stmt.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: booleanValue null (koala.dynamicjava.tree.QualifiedName: B))";
    String actual = ((DoStatement)stmt).getCondition().toString();
    assertEquals("Should have autounboxed", expected, actual);
    
    _interpreter.interpret("Boolean B = Boolean.FALSE; " + text);
  }  
  
  /**
   * Tests the for loop's condition statement
   */
  public void testVisitForStatement() throws ExceptionReturnedException {
    String text = "for(int i=0; new Boolean(i<1); i++);";
    Node stmt = _parseCode(text).get(0);
    
    stmt.acceptVisitor(_typeChecker);

    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: booleanValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Boolean) [(koala.dynamicjava.tree.LessExpression: (koala.dynamicjava.tree.QualifiedName: i) (koala.dynamicjava.tree.IntegerLiteral: 1 1 int))]))";
    String actual = ((ForStatement)stmt).getCondition().toString();
    assertEquals("Should have autounboxed", expected, actual);

    _interpreter.interpret(text);
  }
  
  public void testSwitchStatement() throws ExceptionReturnedException {
    String text = "switch (new Integer(1)) { }";
    SwitchStatement stmt = (SwitchStatement)_parseCode(text).get(0);
    
    stmt.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 1 1 int)]))";
    String actual = stmt.getSelector().toString();
    assertEquals("Should have autounboxed", expected, actual);
    
    _interpreter.interpret(text);
  }
  

  public void testIfThenStatement() throws ExceptionReturnedException {
    String text = "if (B) { }";
    IfThenStatement stmt = (IfThenStatement) _parseCode(text).get(0);
    
    stmt.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: booleanValue null (koala.dynamicjava.tree.QualifiedName: B))";
    String actual = stmt.getCondition().toString();
    assertEquals("Should have autounboxed", expected, actual);
    
    _interpreter.interpret("Boolean B = Boolean.TRUE;" + text);
  }
  
  /**
   * Tests the if-then-else statement for auto-unboxing.
   */
  public void testIfThenElseStatement() throws ExceptionReturnedException {
    String text = "if (B) { } else if (B) { }";
    IfThenStatement stmt = (IfThenStatement) _parseCode(text).get(0);
    
    stmt.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: booleanValue null (koala.dynamicjava.tree.QualifiedName: B))";
    String actual = stmt.getCondition().toString();
    assertEquals("Should have autounboxed", expected, actual);
    
    _interpreter.interpret("Boolean B = Boolean.TRUE;" + text);
  }
  
  //////////// Addititve Bin Ops ////////////////////////
  /**
   * Tests adding two Integers.
   */
  public void testAddTwoIntegers() throws ExceptionReturnedException {
    String text = "new Integer(1) + new Integer(2);";
    AddExpression exp = (AddExpression) _parseCode(text).get(0);
    
    exp.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 1 1 int)]))";
    String actual = exp.getLeftExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 2 2 int)]))";
    actual = exp.getRightExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    _interpreter.interpret(text);
  }
  
  /**
   * Tests substracting two Integers.
   */
  public void testSubtractingTwoIntegers() throws ExceptionReturnedException {
    String text = "new Integer(1) - new Integer(2);";
    BinaryExpression exp = (BinaryExpression)_parseCode(text).get(0);
    
    exp.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 1 1 int)]))";
    String actual = exp.getLeftExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 2 2 int)]))";
    actual = exp.getRightExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    _interpreter.interpret(text);
  }
    
  ///////////// Additive Assignemt //////////////////////
  
  /**
   * Tests the += operation.
   */
  public void testPlusEquals() {
    Node exp = _parseCode("x += new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests the -= operation.
   */
  public void testMinusEquals() {
    Node exp = _parseCode("x -= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  
  //////////// Multiplicitive Bin Ops ///////////////////
  
  /**
   * Tests multiplying two Integers.
   */
  public void testMultiplyingTwoIntegers() throws ExceptionReturnedException {
    String text = "new Integer(1) * new Integer(2);";
    BinaryExpression exp = (BinaryExpression)_parseCode(text).get(0);
    
    exp.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 1 1 int)]))";
    String actual = exp.getLeftExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 2 2 int)]))";
    actual = exp.getRightExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    _interpreter.interpret(text);
  }
  
  /**
   * Tests dividing two Integers.
   */
  public void testDividingTwoIntegers() throws ExceptionReturnedException {
    String text = "new Integer(1) / new Integer(2);";
    BinaryExpression exp = (BinaryExpression)_parseCode(text).get(0);
    
    exp.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 1 1 int)]))";
    String actual = exp.getLeftExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 2 2 int)]))";
    actual = exp.getRightExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    _interpreter.interpret(text);
  }
  
  /**
   * Tests dividing two Integers.
   */
  public void testModingTwoIntegers() throws ExceptionReturnedException {
    String text = "new Integer(1) % new Integer(2);";
    BinaryExpression exp = (BinaryExpression)_parseCode(text).get(0);
    
    exp.acceptVisitor(_typeChecker);
    
    String expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 1 1 int)]))";
    String actual = exp.getLeftExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    expected = "(koala.dynamicjava.tree.ObjectMethodCall: intValue null (koala.dynamicjava.tree.SimpleAllocation: (koala.dynamicjava.tree.ReferenceType: Integer) [(koala.dynamicjava.tree.IntegerLiteral: 2 2 int)]))";
    actual = exp.getRightExpression().toString();
    assertEquals("Should have unboxed correctly.", expected, actual);
    
    _interpreter.interpret(text);
  }
  //////////// Multiplicitive Assignments ///////////////
  
  /**
   * Tests the *= operation.
   */
  public void testMultEquals() {
    Node exp = _parseCode("x *= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests the /= operation.
   */
  public void testDivideEquals() {
    Node exp = _parseCode("x /= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests the %= operation.
   */
  public void testModEquals() {
    Node exp = _parseCode("x %= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  //////////// Shift Bin Ops ////////////////////////////
  
  
  /**
   * Tests Shift Right on two Shorts
   */
  public void testShiftRight() {
    Node exp = _parseCode("(new Short(1) >> new Short(2));").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Shift Left on two Shorts
   */
  public void testShiftLeft() {
    Node exp = _parseCode("new Short(-10) << new Short(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Unsigned Shift on two longs
   */
  public void testUShiftRight() {
    Node exp = _parseCode("new Long(-1) >>> new Long(1);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  //////////// Shift Assignments ////////////////////////
  
  /**
   * Tests the <<= operation.
   */
  public void testLeftShiftEquals() {
    Node exp = _parseCode("x <<= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests the >>= operation.
   */
  public void testRightShiftEquals() {
    Node exp = _parseCode("x >>= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests the >>>= operation.
   */
  public void testUnsignedRightShiftEquals() {
    Node exp = _parseCode("x >>>= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  


  //////////// Bitwise Bin Ops //////////////////////////
  
  /**
   * Tests XORing two Booleans.
   */
  public void testXOringTwoBooleans() {
    Node exp = _parseCode("new Boolean(true) ^ new Boolean(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Bitwise AND on Booleans.
   */
  public void testBooleanBitwiseAnd() {
    Node exp = _parseCode("new Boolean(true) & new Boolean(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Bitwise OR on Booleans.
   */
  public void testBooleanBitwiseOr() {
    Node exp = _parseCode("new Boolean(true) | new Boolean(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Bitwise AND on Integers.
   */
  public void testNumericBitwiseAnd() {
    Node exp = _parseCode("new Integer(true) & new Integer(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Bitwise OR on Integers.
   */
  public void testNumericBitwiseOr() {
    Node exp = _parseCode("new Integer(true) | new Integer(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  //////////// Bitwise Assignments //////////////////////
  
  /**
   * Tests the &= operation.
   */
  public void testAndEquals() {
    Node exp = _parseCode("x &= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests the ^= operation.
   */
  public void testXorEquals() {
    Node exp = _parseCode("x ^= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests the |= operation.
   */
  public void testOrEquals() {
    Node exp = _parseCode("x |= new Integer(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  
  //////////// Boolean/Comparative Bin Ops //////////////
  
  /**
   * Tests ANDing two Booleans.
   */
  public void testAndingTwoBooleans() {
    Node exp = _parseCode("new Boolean(true) && new Boolean(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests ORing two Booleans.
   */
  public void testOringTwoBooleans() {
    Node exp = _parseCode("new Boolean(true) || new Boolean(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
    
  /**
   * Tests GreaterThan with two Doubles
   */
  public void testGreaterThan() {
    Node exp = _parseCode("new Double(1) > new Double(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
    
  /**
   * Tests GreaterThan or Equal to with two Floats
   */
  public void testGreaterThanEqual() {
    Node exp = _parseCode("new Float(1) >= new Float(2);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
        
  /**
   * Tests LessThan to with two Longs
   */
  public void testLessThan() {
    Node exp = _parseCode("new Long(12) < new Long(32);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
        
  /**
   * Tests LessThan Or Equal to with two Integers
   */
  public void testLessThanEqual() {
    Node exp = _parseCode("new Integer(12) <= new Integer(32);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  //////////// Compliment Unary Op //////////////////////
  
  /**
   * Tests Complimenting an Integer.
   */
  public void testComplimentingOneBoolean() {
    Node exp = _parseCode("~new Integer(24);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Negating a Boolean.
   */
  public void testNegatingOneBoolean() {
    Node exp = _parseCode("!new Boolean(false);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Plus Operator.
   */
  public void testPlusOperator() {
    Node exp = _parseCode("+new Double(10);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  /**
   * Tests Minus Operator.
   */
  public void testMinusOperator() {
    Node exp = _parseCode("-new Integer(10);").get(0);
    
    try {
      exp.acceptVisitor(_typeChecker);
      fail("Should have thrown an excpetion.");
    }
    catch (ExecutionError ee) {
    }
  }
  
  
  
  /**
   * To Add:
   * 
   * # If/Switch
   * 
   * # VariableDeclaration
   * - Method Decl/Calls
   * - Function Calls
   * 
   * - checkCastStaticRules
   * - casts
   * 
   * x Compliment :: ~
   * x Not :: !
   * # private Class visitUnaryOperation
   * # Plus (unary) :: +num
   * # Minus (unary) :: -num
   * 
   * # private static Class visitNumericExpression(...)
   * 
   * - private static void checkEqualityStaticRules(...)
   * - private Class visitRelationalExpression(...)
   * # Less/greater than [equals] :: <, >, <=, >=
   * 
   * # private Class visitBitwiseExpression(...)
   * # Bit AND :: b & b
   * # Bit OR  :: b | b
   * # Bit AND :: n & n
   * # Bit OR  :: n | n
   * 
   * # private Class visitShiftExpression(...)
   * # Shift Left/Right :: >>, <<
   * # UShift Right :: >>>
   * 
   * - Conditional :: cond ? exp : exp
   * # Array Declaration
   */
}
