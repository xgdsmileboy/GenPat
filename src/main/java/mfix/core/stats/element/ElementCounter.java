package mfix.core.stats.element;

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

    public void add(Element element) throws ElementException {
        _connector.add(element.toInsertRow());
    }

    public float count(Element element, ElementQueryType queryType) throws ElementException {
        Map<String, String> queryRow = element.toQueryRow(queryType);
        Integer countNumber = _connector.query(queryRow);

        if (queryType.getWithPercent()) {
            queryRow.remove(Element.DBKEY_ELEMENT_NAME);
            Integer allNumber = _connector.query(queryRow);
            if (allNumber == 0) {
                return 0;
            } else {
                return ((float)countNumber) / allNumber;
            }
        } else {
            return countNumber;
        }
    }
}
