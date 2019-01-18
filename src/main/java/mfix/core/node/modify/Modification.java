package mfix.core.node.modify;

import mfix.core.node.ast.Node;

import java.io.Serializable;

public abstract class Modification implements Serializable {

    private static final long serialVersionUID = 3007384743034824570L;

    private Node _parent;

    protected Modification(Node parent) {
        _parent = parent;
    }

    public Node getParent() {
        return _parent;
    }

}
