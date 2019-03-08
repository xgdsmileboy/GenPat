package mfix.core.node.modify;

import mfix.core.node.ast.Node;
import mfix.core.pattern.cluster.VIndex;

import java.util.Map;
import java.util.Set;

public class Insertion extends Modification {

    private int _index;
    private Node _preNode;
    private Node _nexNode;
    private Node _insert;

    protected Insertion(Node parent, int fIndex) {
        super(parent, fIndex);
    }

    public Insertion(Node parent, int index, Node insert) {
        super(parent, VIndex.MOD_INSERT);
        _index = index;
        _insert = insert;
        _insert.setChanged();
    }

    public void setPrenode(Node node) {
        _preNode = node;
        if (_preNode != null) {
            _preNode.setInsertDepend(true);
        }
    }

    public Node getPrenode() {
        return _preNode;
    }

    public void setNextnode(Node node) {
        _nexNode = node;
        if (_nexNode != null) {
            _nexNode.setInsertDepend(true);
        }
    }

    public Node getNextnode() {
        return _nexNode;
    }

    public int getIndex() {
        return _index;
    }

    public Node getInsertedNode() {
        return _insert;
    }

    public StringBuffer apply(Set<String> vars, Map<String, String> exprMap) {
        if(_insert == null) {
            return new StringBuffer("null");
        } else {
            return _insert.transfer(vars, exprMap);
        }
    }

    @Override
    public boolean patternMatch(Modification m) {
        if (m instanceof Insertion) {
            Insertion insertion = (Insertion) m;
            return getParent().patternMatch(insertion.getParent())
                    && getInsertedNode().patternMatch(insertion.getInsertedNode());
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("[INS]INSERT %s UNDER %s AS {%d} CHILD", _insert, getParent(), _index);
    }
}
