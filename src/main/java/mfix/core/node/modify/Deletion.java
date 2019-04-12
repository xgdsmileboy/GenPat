package mfix.core.node.modify;

import mfix.core.node.ast.Node;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;

import java.util.HashSet;
import java.util.Map;

public class Deletion extends Modification {

    private static final long serialVersionUID = 4063889250515342335L;
    private Node _node2Del;
    private int _index;

    public Deletion(Node parent, Node node, int index) {
        super(parent, VIndex.MOD_DELETE);
        _node2Del = node;
        _index = index;
        if (_node2Del != null) {
            _node2Del.setChanged();
        }
    }

    public int getIndex() {
        return _index;
    }

    public Node getDelNode() {
        return _node2Del;
    }

    @Override
    public boolean patternMatch(Modification m, Map<Node, Node> matchedNode) {
        if (m instanceof Deletion) {
            Deletion deletion = (Deletion) m;
            if (getDelNode() == null) {
                return deletion.getDelNode() == null;
            }
            return getDelNode().patternMatch(deletion.getDelNode(), matchedNode);
        }
        return false;
    }

    @Override
    public String formalForm() {
        return "[DEL]" + _node2Del.formalForm(new NameMapping(), false, new HashSet<>());
    }

    @Override
    public String toString() {
        return "[DEL]" + _node2Del + " FROM " + getParent();
    }
}
