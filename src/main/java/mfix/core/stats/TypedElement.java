package mfix.core.stats;

import mfix.core.parse.node.Node.TYPE;
import mfix.core.parse.node.expr.SName;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class TypedElement extends Element{
    protected String _elementTypeName;

    TypedElement(SName var) {
        super(var.getName());
        _elementTypeName = var.getType().toString();
    }

    TypedElement(String name, TYPE type) {
        super(name);
        _elementTypeName = type.toString();
    }

    TypedElement(String name, String typeName) {
        super(name);
        _elementTypeName = typeName;
    }


    @Override
    public String toString() {
        return '[' + _elementTypeName + ']' + super.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypedElement) {
            return super.equals(obj) && _elementTypeName.equals(((TypedElement) obj)._elementTypeName);
        }
        if (obj instanceof Element) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

