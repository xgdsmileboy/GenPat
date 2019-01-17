package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class Update extends Modification {

    private Node _srcNode;
    private Node _tarNode;

    public Update(Node parent, Node srcNode, Node tarNode) {
        super(parent);
        _srcNode = srcNode;
        _tarNode= tarNode;
    }

    @Override
    public String toString() {
        return "[UPD]" + _srcNode + " TO " + _tarNode;
    }
}
