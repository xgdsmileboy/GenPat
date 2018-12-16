package mfix.core.stats.element;

/**
 * @author: Luyao Ren
 * @date: 2018/12/14
 */
public class MethodElement extends Element {
    protected String _retType = null;
    protected String _objType = null;
    protected String _argsType = null;
    protected Integer _argsNumber = null;

    public MethodElement(String name, String fileName) { super(name, fileName); }

    public void setRetType(String retType) {
        _retType = retType;
    }

    public void setObjType(String objType) {
        _objType = objType;
    }

    public void setArgsType(String argsType) {
        _argsType = argsType;
    }

    public void setArgsNumber(Integer argsNumber) {
        _argsNumber = argsNumber;
    }
}
