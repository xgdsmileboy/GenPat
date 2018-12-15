package mfix.core.stats.element;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class Element {
    public enum ElementType {
        VAR, METHOD
    };

    protected ElementType _elementType;
    protected String _elementName;
    protected String _sourceFile;

    public Element(ElementType elementType, String name, String sourceFile) {
        _elementType = elementType;
        _elementName = name;
        _sourceFile = sourceFile;
    }

    Element(Element element) {
        _elementType = element._elementType;
        _elementName = element._elementName;
        _sourceFile = element._sourceFile;
    }

    public Map<String, String> toInsertRow() {
        Map<String, String> KeyToValue = new HashMap<String, String>();

        KeyToValue.put("elementName", _elementName);
        KeyToValue.put("sourceFile", _sourceFile);
        if (_elementType == Element.ElementType.VAR) {
            VarElement var = (VarElement) this;
            KeyToValue.put("table", "VarTable");
            if (var._varType != null) {
                KeyToValue.put("varType", var._varType);
            }
        } else if (_elementType == Element.ElementType.METHOD) {
            MethodElement method = (MethodElement) this;
            KeyToValue.put("table", "MethodTable");
            if (method._retType != null) {
                KeyToValue.put("retType", method._retType);
            }
            if (method._objType != null) {
                KeyToValue.put("objType", method._objType);
            }
            if (method._argsType != null) {
                KeyToValue.put("argsType", method._argsType);
            }
            if (method._argsNumber != null) {
                KeyToValue.put("argsNumber", method._argsNumber.toString());
            }
        }

        return KeyToValue;
    }

    public Map<String, String> toQueryRow(ElementQueryType query) {
        Map<String, String> KeyToValue = toInsertRow();

        if (!query.getWithType()) {
            if (_elementType == Element.ElementType.VAR) {
                KeyToValue.remove("varType");
            } else if (_elementType == Element.ElementType.METHOD) {
                KeyToValue.remove("retType");
                KeyToValue.remove("objType");
                KeyToValue.remove("argsType");
                KeyToValue.remove("argsNumber");
            }
        }

        if (query.getCountType() != ElementQueryType.CountType.IN_FILE) {
            KeyToValue.remove("sourceFile");
        }

        if (query.getCountType() == ElementQueryType.CountType.COUNT_FILES) {
            KeyToValue.put("countColumn", "distinct sourceFile");
        }

        return KeyToValue;
    }
}
