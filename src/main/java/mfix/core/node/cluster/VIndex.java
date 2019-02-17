/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.cluster;

/**
 * @author: Jiajun
 * @date: 2019-02-11
 */
public class VIndex {

    // expressions
    /**
     * array access
     */
    public final static int EXP_ARRAY_ACC = 0;
    /**
     * array creation
     */
    public final static int EXP_ARRAY_CRT = 1;
    /**
     * array initializer
     */
    public final static int EXP_ARRAY_INT = 2;
    /**
     * assignment
     */
    public final static int EXP_ASSIGN = 3;
    /**
     * assignment operator
     */
    public final static int EXP_ASSIGN_OP = 4;
    /**
     * boolean literal
     */
    public final static int EXP_BOOL_LIT = 5;
    /**
     * cast expression
     */
    public final static int EXP_CAST = 6;
    /**
     * chart literal
     */
    public final static int EXP_CHAR_LIT = 7;
    /**
     * class instance creation
     */
    public final static int EXP_CLASSINS_CRT = 8;
    /**
     * conditional expression
     */
    public final static int EXP_CONDITIONAL = 9;
    /**
     * double literal
     */
    public final static int EXP_DOUBLE_LIT = 10;
    /**
     * expression list
     */
    public final static int EXP_LIST = 11;
    /**
     * field access
     */
    public final static int EXP_FIELD_ACC = 12;
    /**
     * float literal
     */
    public final static int EXP_FLOAT_LIT = 13;
    /**
     * infix expression
     */
    public final static int EXP_INFIX = 14;
    /**
     * infix operator
     */
    public final static int EXP_INFIX_OP = 15;
    /**
     * instanceof expression
     */
    public final static int EXP_INSTANCEOF = 16;
    /**
     * int literal
     */
    public final static int EXP_INT_LIT = 17;
    /**
     * name (label)
     */
    public final static int EXP_LABEL = 18;
    /**
     * long literal
     */
    public final static int EXP_LONG_LIT = 19;
    /**
     * method invocation
     */
    public final static int EXP_METHOD_INV = 20;
    /**
     * type
     */
    public final static int EXP_TYPE = 21;
    /**
     * null literal
     */
    public final static int EXP_NULL_LIT = 22;
    /**
     * number literal
     */
    public final static int EXP_NUM_LIT = 23;
    /**
     * parenthesized expression
     */
    public final static int EXP_PARENTHESISED = 24;
    /**
     * postfix expression
     */
    public final static int EXP_POSTFIX = 25;
    /**
     * postfix operation
     */
    public final static int EXP_POSTFIX_OP = 26;
    /**
     * prefix expression
     */
    public final static int EXP_PREFIX = 27;
    /**
     * prefix operation
     */
    public final static int EXP_PREFIX_OP = 28;
    /**
     * qualified name
     */
    public final static int EXP_QNAME = 29;
    /**
     * simple name
     */
    public final static int EXP_SNAME = 30;
    /**
     * string literal
     */
    public final static int EXP_STR_LIT = 31;
    /**
     * super field access
     */
    public final static int EXP_SUPER_FIELD_ACC = 32;
    /**
     * super method invocation
     */
    public final static int EXP_SUPER_METHOD_INV = 33;
    /**
     * single variable declaration
     */
    public final static int EXP_SVD = 34;
    /**
     * this expression
     */
    public final static int EXP_THIS = 35;
    /**
     * type literal
     */
    public final static int EXP_TYPE_LIT = 36;
    /**
     * variable declaration expression
     */
    public final static int EXP_VAR_DEC = 37;
    /**
     * variable declaration fragment
     */
    public final static int EXP_VAR_FRAG = 38;


    //statements
    /**
     * assert statement
     */
    public final static int STMT_ASSERT = 39;
    /**
     * block
     */
    public final static int STMT_BLK = 40;
    /**
     * break statement
     */
    public final static int STMT_BREAK = 41;
    /**
     * catch statement
     */
    public final static int STMT_CATCH = 42;
    /**
     * constructor invocation
     */
    public final static int STMT_CONSTRUCTOR = 43;
    /**
     * continue statement
     */
    public final static int STMT_CONTINUE = 44;
    /**
     * do statement
     */
    public final static int STMT_DO = 45;
    /**
     * enhancedfor statement
     */
    public final static int STMT_ENHANCEDFOR = 46;
    /**
     * for statement
     */
    public final static int STMT_FOR = 47;
    /**
     * if statement
     */
    public final static int STMT_IF = 48;
    /**
     * labeled statement
     */
    public final static int STMT_LABEL = 49;
    /**
     * return statement
     */
    public final static int STMT_RETURN = 50;
    /**
     * super constructor invocation
     */
    public final static int STMT_SUPER_CONSTRUCTOR = 51;
    /**
     * switch case
     */
    public final static int STMT_CASE = 52;
    /**
     * switch statement
     */
    public final static int STMT_SWTICH = 53;
    /**
     * synchronized statement
     */
    public final static int STMT_SYNC = 54;
    /**
     * throw statement
     */
    public final static int STMT_THROW = 55;
    /**
     * try statement
     */
    public final static int STMT_TRY = 56;
    /**
     * variable declaration statement
     */
    public final static int STMT_VAR_DECL = 57;
    /**
     * while statement
     */
    public final static int STMT_WHILE = 58;

    // modifications
    /**
     * update modification
     */
    public final static int MOD_UPDATE = 59;
    /**
     * insert modification
     */
    public final static int MOD_INSERT = 60;
    /**
     * delete modification
     */
    public final static int MOD_DELETE = 61;

    /**
     * represent the abstract expression
     */
    public final static int ABS_EXPRESSION = 62;
    /**
     * represent the abstract statement
     */
    public final static int ABS_STATEMENT = 63;


    public final static int LENGTH = 64;
}
