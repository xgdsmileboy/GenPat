package mfix.core.node.modify;

import mfix.core.node.ast.Node;

import java.io.Serializable;

public abstract class Modification implements Serializable {

    private static final long serialVersionUID = 3007384743034824570L;

    private Node _parent;
    private int _fIndex;

    protected Modification(Node parent, int fIndex) {
        _parent = parent;
        _fIndex = fIndex;
    }

    public Node getParent() {
        return _parent;
    }

    public int getFeatureIndex() {
        return _fIndex;
    }

}
