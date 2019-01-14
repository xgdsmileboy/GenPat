package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class Deletion extends Modification {

    private Node _parent;
    private Node _node2Del;

    public Deletion(Node parent, Node node) {
        _parent = parent;
        _node2Del = node;
    }

    @Override
    public String toString() {
        return "[DEL]" + _node2Del.toSrcString().toString();
    }
}
