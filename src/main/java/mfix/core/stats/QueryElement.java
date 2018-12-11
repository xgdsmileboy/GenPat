package mfix.core.stats;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class QueryElement extends Element {
    public enum QueryType {
        IN_FILE, COUNT_FILES, ALL
    };

    protected boolean _withType = false;
    protected QueryType _queryType = QueryType.ALL;

    public QueryElement(Element element, boolean withType, QueryType queryType) {
        super(element);
        _withType = withType;
        _queryType = queryType;
    }
}

