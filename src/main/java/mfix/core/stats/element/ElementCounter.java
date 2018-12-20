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
        Integer countNumber = _connector.query(element.toQueryRow(queryType));

        if (queryType.getWithPercent()) {
            Integer allNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
            return allNumber == 0 ? 0 : ((float)countNumber) / allNumber;
        } else {
            return countNumber;
        }
    }
}
