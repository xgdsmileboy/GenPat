/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.relation.op;

import mfix.core.parse.relation.RArg;

import java.util.List;

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

    public String getExprString(List<RArg> args) {
        switch(_operator) {
            case A_PLUS:
            case A_MINUS:
            case A_MUL:
            case A_DIV:
            case A_MOD:
            case R_EQ:
            case R_NEQ:
            case R_GT:
            case R_LT:
            case R_GE:
            case R_LE:
            case B_AND:
            case B_OR:
            case B_XOR:
            case B_SHL:
            case B_SHR:
            case B_SSHR:
            case L_AND:
            case L_OR:
            case C_INSTOF:
                assert args.size() == 2;
                RArg lhs, rhs;
                if(args.get(0).getIndex() == BinaryOp.POSITION_LHS) {
                    lhs = args.get(0);
                    rhs = args.get(1);
                } else {
                    lhs = args.get(1);
                    rhs = args.get(0);
                }
                return String.format("(%s)%s(%s)", lhs.getExprString(), _operator.toString(), rhs.getExprString());
            case A_INC:
            case A_DEC:
                assert args.size() == 1;
                if(args.get(0).getIndex() == BinaryOp.POSITION_LHS) {
                    return String.format("(%s)%s", args.get(0).getExprString(), _operator.toString());
                } else {
                    return String.format("%s(%s)", _operator.toString(), args.get(0).getExprString());
                }
            case B_COMPLIMENT:
            case L_NOT:
                assert args.size() == 1;
                return String.format("%s(%s)", _operator.toString(), args.get(0).getExprString());
            case C_COND:
                assert args.size() == 3;
                RArg cond = null, then = null, els = null;
                for(RArg arg : args) {
                    switch (arg.getIndex()) {
                        case CopCond.POSITION_CONDITION:
                            cond = arg;
                            break;
                        case CopCond.POSITION_THEN:
                            then = arg;
                            break;
                        case CopCond.POSITION_ELSE:
                            els = arg;
                    }
                }
                assert cond != null && then != null && els != null;
                return String.format("(%)?(%s):(%s)", cond.getExprString(), then.getExprString(), els.getExprString());
            case ARRY_ACC:
                assert args.size() == 2;
                RArg ary, idx;
                if(args.get(0).getIndex() == BinaryOp.POSITION_LHS) {
                    ary = args.get(0);
                    idx = args.get(1);
                } else {
                    ary = args.get(1);
                    idx = args.get(0);
                }
                return String.format("%s[%s]", ary.getExprString(), idx.getExprString());
            default:
        }
        return "[OP]";
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
        C_COND("?:"),
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

    @Override
    public String toString() {
        return _operator.toString();
    }
}
