package mfix.core.stats;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class Element {
    protected String _name;
    Element(String name) {
        _name = name;
    }

    @Override
    public String toString() {
        return _name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Element) {
            return _name.equals(((Element) obj)._name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

}
