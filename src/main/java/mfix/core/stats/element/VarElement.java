package mfix.core.stats.element;

import mfix.core.parse.node.expr.SName;

/**
 * @author: Luyao Ren
 * @date: 2018/12/14
 */
public class VarElement extends Element {
    protected String _varType;

    public VarElement(SName var) {
        super(ElementType.VAR, var.getName(), var.getFileName());
        _varType = "?";
        if (var.getType() != null) {
            _varType = var.getType().toString();
        }
    }

    public VarElement(String name, String type, String fileName) {
        super(ElementType.VAR, name, fileName);
        _varType = type;
    }
}
