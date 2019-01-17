package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class Insertion extends Modification {

    private int _index;
    private Node _insert;

    protected Insertion(Node parent) {
        super(parent);
    }

    public Insertion(Node parent, int index, Node insert) {
        super(parent);
        _index = index;
        _insert = insert;
    }

    public int getIndex() {
        return _index;
    }

    public Node getInsertedNode() {
        return _insert;
    }

    @Override
    public boolean apply() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("[INS]INSERT %s UNDER %s AS {%d} CHILD", _insert, getParent(), _index);
    }
}
