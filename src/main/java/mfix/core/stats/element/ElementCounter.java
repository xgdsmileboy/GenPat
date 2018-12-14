package mfix.core.stats.element;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class ElementCounter {
    private DatabaseConnector _connector = null;

    public void open() {
        _connector = new DatabaseConnector();
        _connector.open();
    }

    public void close() {
        _connector.close();
    }

    public void add(Element element) {
        _connector.add(elementToRow(element));
    }

    public Integer count(Element element, QueryType queryType) {
        return _connector.query(AddQuery(elementToRow(element), queryType));
    }

    private Map<String, String> AddQuery(Map<String, String> KeyToValue, QueryType query) {
        if (!query._withType) {
            if (KeyToValue.get("table") == "VarTable") {
                KeyToValue.remove("varType");
            } else if (KeyToValue.get("table") == "MethodTable") {
                KeyToValue.remove("retType");
                KeyToValue.remove("objType");
                KeyToValue.remove("argsType");
                KeyToValue.remove("argsNumber");
            }
        }

        if (query._countType != QueryType.CountType.IN_FILE) {
            KeyToValue.remove("sourceFile");
        }

        if (query._countType == QueryType.CountType.COUNT_FILES) {
            KeyToValue.put("countElement", "distinct sourceFile");
        }
        return KeyToValue;
    }

    private Map<String, String> elementToRow(Element element) {
        Map<String, String> KeyToValue = new HashMap<String, String>();

        KeyToValue.put("elementName", element._elementName);
        KeyToValue.put("sourceFile", element._sourceFile);
        if (element._elementType == Element.ElementType.VAR) {
            VarElement var = (VarElement) element;
            KeyToValue.put("table", "VarTable");
            if ((var._varType != null) && (var._varType != "?")) {
                KeyToValue.put("varType", var._varType);
            }
        } else if (element._elementType == Element.ElementType.METHOD) {
            MethodElement method = (MethodElement) element;
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
}
