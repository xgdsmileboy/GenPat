package mfix.core.stats.element;

import mfix.core.parse.node.expr.MethodInv;
import mfix.core.parse.node.expr.*;

/**
 * @author: Luyao Ren
 * @date: 2018/12/14
 */
public class MethodElement extends Element {
    protected String _retType;
    protected String _objType;
    protected String _argsType;
    protected Integer _argsNumber;

    public MethodElement(MethodInv inv) {
        super(ElementType.METHOD, inv.getName().getName(), inv.getFileName());
        _retType = inv.getTypeString();
        _objType = "?";
        if (inv.getExpression() != null) {
            _objType = inv.getExpression().getTypeString();
        }
        ExprList list = inv.getArguments();
        for (Expr expr : list.getExpr()) {
            _argsType += expr.getTypeString() + ",";
        }
        _argsNumber = list.getExpr().size();
    }

    public MethodElement(String name, String fileName) {
        super(ElementType.METHOD, name, fileName);
    }
}
