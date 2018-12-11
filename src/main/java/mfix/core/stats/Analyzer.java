package mfix.core.stats;

import mfix.core.parse.node.expr.MethodInv;
import mfix.core.parse.node.expr.SName;
import org.eclipse.jdt.core.dom.*;
import mfix.core.parse.node.*;

import java.util.Set;

/**
 * @author: Luyao Ren
 * @date: 2018/12/06
 */
public class Analyzer {
    private static Analyzer _instance;
    private String _fileName;
    private ElementCounter _elementCounter = null;

    public static Analyzer getInstance() {
        if (_instance == null) {
            _instance = new Analyzer();
            _instance.open();
        }
        return _instance;
    }

    public void open() {
        _elementCounter = new ElementCounter();
        _elementCounter.open();
    }

    public void finish() {
        _elementCounter.close();
    }

    public void analyze(Node curNode) {
        if (curNode instanceof MethodInv) {
            for (Set<Node> methodSet : curNode.getCalledMethods().values()) {
                for (Node method : methodSet) {
                    _elementCounter.add(new Element(method));
                }
            }
            for (SName var : curNode.getAllVars()) {
                _elementCounter.add(new Element(var));
            }
        }
        for (Node child : curNode.getAllChildren()) {
            analyze(child);
        }
    }

    public void setFileName(String fileName) {
        _fileName = fileName;
    }
}
