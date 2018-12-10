/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation.op;

/**
 * @author: Jiajun
 * @date: 2018/12/5
 */
public abstract class AbsOperation {

    private Op _operator;

    protected AbsOperation(Op operator) {
        _operator = operator;
    }

    public Op getOperator() {
        return _operator;
    }

    public boolean match(AbsOperation operation) {
        return _operator == operation.getOperator();
    }

    public enum Op{
        // Arithmetic
        A_PLUS("+"),
        A_MINUS("-"),
        A_MUL("*"),
        A_DIV("/"),
        A_MOD("%"),
        A_INC("++"),
        A_DEC("--"),

        // Relational
        R_EQ("=="),
        R_NEQ("!="),
        R_GT(">"),
        R_LT("<"),
        R_GE(">="),
        R_LE("<="),

        // Bitwise
        B_AND("&"),
        B_OR("|"),
        B_XOR("^"),
        B_COMPLIMENT("~"),
        B_SHL("<<"),
        B_SHR(">>"),
        B_SSHR(">>>"),

        // Logical
        L_AND("&&"),
        L_OR("||"),
        L_NOT("!"),

        //Conditional
        C_COND(":?"),
        C_INSTOF("instanceof"),

        //array access
        ARRY_ACC("[]");

        private String _value;
        Op(String v) {_value = v;}

        @Override
        public String toString() {
            return _value;
        }
    }
}
