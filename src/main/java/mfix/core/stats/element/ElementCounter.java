package mfix.core.stats.element;

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
        _connector.add(element.toInsertRow());
    }

    public Integer count(Element element, ElementQueryType queryType) {
        return _connector.query(element.toQueryRow(queryType));
    }
}
