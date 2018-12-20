package mfix.core.stats.element;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class ElementQueryType {
    public enum CountType {
        // the number appeared in only one file
        // specifically, the file name should be given
        IN_FILE,
        // number of files that contains the element
        COUNT_FILES,
        // percent of files that contains the element,
        COUNT_FILES_PERCENT,
        // the number of elements in all files.
        ALL
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

