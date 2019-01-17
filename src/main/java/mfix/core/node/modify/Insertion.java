package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class Insertion extends Modification {

    private int _index;
    private Node _preNode;
    private Node _nexNode;
    private Node _insert;

    protected Insertion(Node parent) {
        super(parent);
    }

    public Insertion(Node parent, int index, Node insert) {
        super(parent);
        _index = index;
        _insert = insert;
    }

    public void setPrenode(Node node) {
        _preNode = node;
    }

    public Node getPrenode() {
        return _preNode;
    }

    public void setNextnode(Node node) {
        _nexNode = node;
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

    public StringBuffer apply() {
        if(_insert == null) {
            return new StringBuffer("null");
        } else {
            return _insert.transfer();
        }
    }

    @Override
    public String toString() {
        return String.format("[INS]INSERT %s UNDER %s AS {%d} CHILD", _insert, getParent(), _index);
    }
}
