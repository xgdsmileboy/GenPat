/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation.op;

/**
 * @author: Jiajun
 * @date: 2018/12/6
 */
public class OperationFactory {

    public static AbsOperation createOperation(String op) {
        switch(op) {
            case"+":
                return new AoPlus();
            case "-":
                return new AopMinus();
            case "*":
                return new AopMul();
            case "/":
                return new AopDiv();
            case "%":
                return new AopMod();
            case "++":
                return new AopInc();
            case "--":
                return new AopDec();
            case "==":
                return new RopEq();
            case "!=":
                return new RopNeq();
            case ">":
                return new RopGT();
            case "<":
                return new RopLT();
            case ">=":
                return new RopGE();
            case "<=":
                return new RopLE();
            case "&":
                return new BopAnd();
            case "|":
                return new BopOr();
            case "^":
                return new BopXor();
            case "~":
                return new BopComplmt();
            case "<<":
                return new BopShl();
            case ">>":
                return new BopShr();
            case ">>>":
                return new BopSShr();
            case "&&":
                return new LopAnd();
            case "||":
                return new LopOr();
            case "!":
                return new LopNot();
            case ":?":
                return new CopCond();
            case "instanceof":
                return new CopInstof();
            case "[]":
                return new OpArrayAcc();
            default:
                throw new IllegalArgumentException("Illegal arithmetic operator");
        }
    }

}
