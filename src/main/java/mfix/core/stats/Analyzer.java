package mfix.core.stats;

import mfix.core.parse.node.expr.MethodInv;
import mfix.core.parse.node.expr.SName;
import mfix.core.stats.element.MethodElement;
import mfix.core.stats.element.VarElement;
import mfix.core.parse.node.*;

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
            _elementCounter.add(new MethodElement((MethodInv)curNode));
        } else if (curNode instanceof SName) {
            _elementCounter.add(new VarElement((SName)curNode));
        }
        for (Node child : curNode.getAllChildren()) {
            analyze(child);
        }
    }

    public void setFileName(String fileName) {
        _fileName = fileName;
    }
}
