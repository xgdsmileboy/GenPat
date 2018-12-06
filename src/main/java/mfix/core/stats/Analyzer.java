package mfix.core.stats;

import mfix.core.parse.node.expr.MethodInv;
import mfix.core.parse.node.expr.SName;
import org.eclipse.jdt.core.dom.*;
import mfix.core.parse.node.*;
import mfix.common.util.Pair;

import java.util.HashMap;
import java.util.Set;

/**
 * @author: Luyao Ren
 * @date: 2018/12/6
 */
public class Analyzer {
    private static Analyzer _instance;
    private CompilationUnit _cunit;
    private String _fileName;

    private HashMap<String, Integer> _counter;

    public Analyzer() {
        _counter = new HashMap<String, Integer>();
    }

    public static Analyzer getInstance() {
        if (_instance == null) {
            _instance = new Analyzer();
        }
        return _instance;
    }

    public void analyze(Node curNode) {
        if (curNode instanceof MethodInv) {
            for (String method : curNode.getCalledMethods().keySet()) {
                addName(method);
            }
            for (SName var : curNode.getAllVars()) {
                addName(var.getName());
            }
        }
        for (Node child : curNode.getAllChildren()) {
            analyze(child);
        }
    }

    public Integer getCount(String name) {
        return _counter.get(name);
    }

    @Override
    public String toString(){
        return _counter.toString();
    }

    private void addName(String name) {
        if (!_counter.containsKey(name)) {
            _counter.put(name, 0);
        }
        Integer oldValue = _counter.get(name);
        _counter.put(name, oldValue + 1);
    }
}
