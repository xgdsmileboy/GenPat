package mfix.core.stats;

import mfix.core.parse.node.expr.SName;
import mfix.core.parse.node.Node;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class Element {
    protected String _name;
    protected String _type;
    protected String _sourceFile;

    Element(Element element) {
        _name = element._name;
        _type = element._type;
        _sourceFile = element._sourceFile;
    }

    Element(Node node) {
        _name = node.toString();
        _type = node.getNodeType().toString();
        _sourceFile = null;
    }

    Element(SName var) {
        _name = var.getName();
        _type = var.getType().toString();
        _sourceFile = null;
    }

    Element(String name, String type) {
        _name = name;
        _type = type;
        _sourceFile = null;
    }

    public void setSourceFile(String sourceFile) {
        _sourceFile = sourceFile;
    }
}
