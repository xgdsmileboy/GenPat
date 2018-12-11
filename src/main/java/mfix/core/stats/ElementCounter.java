package mfix.core.stats;

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

    private Map<String, String> elementToRow(Element element) {
        Map<String, String> KeyToValue = new HashMap<String, String>();

        KeyToValue.put("elementName", element._name);
        KeyToValue.put("typeName", element._type);
        KeyToValue.put("sourceFile", element._sourceFile);

        if (element instanceof QueryElement){
            if (!((QueryElement) element)._withType) {
                KeyToValue.remove("typeName");
            }
            if (((QueryElement) element)._queryType != QueryElement.QueryType.IN_FILE) {
                KeyToValue.remove("sourceFile");
            }
            if (((QueryElement) element)._queryType == QueryElement.QueryType.COUNT_FILES) {
                KeyToValue.put("countElement", "sourceFile");
            }
        }

        return KeyToValue;
    }

    public void add(Element element) {
        _connector.add(elementToRow(element));
    }

    public Integer count(QueryElement element) {
        return _connector.query(elementToRow(element));
    }
}
