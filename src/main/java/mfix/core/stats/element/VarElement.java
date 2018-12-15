package mfix.core.stats.element;

/**
 * @author: Luyao Ren
 * @date: 2018/12/14
 */
public class VarElement extends Element {
    protected String _varType = null;

    public VarElement(String name, String fileName) {
        super(ElementType.VAR, name, fileName);
    }

    public VarElement(String name, String type, String fileName) {
        super(ElementType.VAR, name, fileName);
        _varType = type;
    }

    public void setVarType(String varType) {
        _varType = varType;
    }
}
