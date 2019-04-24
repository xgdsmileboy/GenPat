package mfix.core.node.modify;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Update extends Modification {

    private static final long serialVersionUID = -4006265328894276618L;
    private Node _srcNode;
    private Node _tarNode;

    public Update(Node parent, Node srcNode, Node tarNode) {
        super(parent, VIndex.MOD_UPDATE);
        _srcNode = srcNode;
        _tarNode= tarNode;
        if (_srcNode != null) {
            _srcNode.setChanged();
        }
        if (_tarNode != null) {
            _tarNode.setChanged();
        }
    }

    public Node getSrcNode() {
        return _srcNode;
    }

    public Node getTarNode() {
        return _tarNode;
    }

    public StringBuffer apply(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
        if (_tarNode == null) {
            return new StringBuffer();
        }
        return _tarNode.transfer(vars, exprMap, retType, exceptions);
    }

    @Override
    public boolean patternMatch(Modification m, Map<Node, Node> matchedNode) {
        if (m instanceof Update) {
            Update update = (Update) m;
            if (getSrcNode() == null) {
                if (update.getSrcNode() != null) {
                    return false;
                }
            } else if (!getSrcNode().patternMatch(update.getSrcNode(), matchedNode)) {
                return false;
            }
            if (getTarNode() == null) {
                return update.getTarNode() == null;
            }
            return getTarNode().patternMatch(update.getTarNode(), matchedNode);
        }
        return false;
    }

    @Override
    public String formalForm() {
        return "[UPD]" + _srcNode.formalForm(new NameMapping(), false, new HashSet<>())
                + " TO " + _tarNode.formalForm(new NameMapping(), false, new HashSet<>());
    }

    @Override
    public String toString() {
        return "[UPD]" + (_srcNode == null ? "NULL" : _srcNode) + " TO "
                + (_tarNode == null ? "NULL" : _tarNode.toString());
    }
}
