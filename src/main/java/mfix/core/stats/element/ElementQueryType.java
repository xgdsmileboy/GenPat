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
        // the number of elements in all files.
        ALL
    };
    protected boolean _withType = false;
    protected boolean _withPercent = false;
    protected CountType _countType = CountType.ALL;

    public ElementQueryType(boolean withType, boolean withPercent, CountType countType) {
        _withType = withType;
        _withPercent = withPercent;
        _countType = countType;
    }

    public boolean getWithType() {
        return _withType;
    }

    public boolean getWithPercent() { return _withPercent; }

    public CountType getCountType() {
        return _countType;
    }
}

