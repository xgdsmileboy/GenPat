package mfix.core.stats.element;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class Element {
    public enum ElementType {
        VAR, METHOD, CLASS
    };

    protected ElementType _elementType;
    protected String _elementName;
    protected String _sourceFile;

    Element(ElementType elementType, String name, String sourceFile) {
        _elementType = elementType;
        _elementName = name;
        _sourceFile = sourceFile;
    }

    Element(Element element) {
        _elementType = element._elementType;
        _elementName = element._elementName;
        _sourceFile = element._sourceFile;
    }
}
