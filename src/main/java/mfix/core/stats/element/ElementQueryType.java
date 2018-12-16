package mfix.core.stats.element;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class ElementQueryType {
    public enum CountType {
        IN_FILE, COUNT_FILES, ALL
    };
    protected boolean _withType = false;
    protected CountType _countType = CountType.ALL;

    public ElementQueryType(boolean withType, CountType countType) {
        _withType = withType;
        _countType = countType;
    }

    public boolean getWithType() {
        return _withType;
    }

    public CountType getCountType() {
        return _countType;
    }
}

