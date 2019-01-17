package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class Deletion extends Modification {

    private Node _node2Del;
    private int _index;

    public Deletion(Node parent, Node node, int index) {
        super(parent);
        _node2Del = node;
        _index = index;
    }

    public int getIndex() {
        return _index;
    }

    public Node getDelNode() {
        return _node2Del;
    }

    @Override
    public boolean apply() {
        return false;
    }

    @Override
    public String toString() {
        return "[DEL]" + _node2Del.toSrcString().toString();
    }
}
