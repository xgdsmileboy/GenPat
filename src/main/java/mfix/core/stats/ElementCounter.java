package mfix.core.stats;

import java.util.HashMap;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class ElementCounter<T> {
    private HashMap<T, Integer> _counter;

    public ElementCounter() {
        _counter = new HashMap<T, Integer>();
    }

    public Integer count(T element) {
        return _counter.getOrDefault(element, 0);
    }

    @Override
    public String toString(){
        return _counter.toString();
    }

    public void add(T element) {
        if (!_counter.containsKey(element)) {
            _counter.put(element, 0);
        }
        Integer oldValue = _counter.get(element);
        _counter.put(element, oldValue + 1);
    }
}
