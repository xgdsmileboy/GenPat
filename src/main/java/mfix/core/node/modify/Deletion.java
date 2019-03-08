package mfix.core.node.modify;

import mfix.core.node.ast.Node;
import mfix.core.pattern.cluster.VIndex;
import sun.util.resources.cldr.pa.CurrencyNames_pa;

public class Deletion extends Modification {

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
    public boolean patternMatch(Modification m) {
        if (m instanceof Deletion) {
            Deletion deletion = (Deletion) m;
            if (getDelNode() == null) {
                return deletion.getDelNode() == null;
            }
            return getDelNode().patternMatch(deletion.getDelNode());
        }
        return false;
    }

    @Override
    public String toString() {
        return "[DEL]" + _node2Del + " FROM " + getParent();
    }
}
