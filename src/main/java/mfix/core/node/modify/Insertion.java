package mfix.core.node.modify;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Insertion extends Modification {

    private static final long serialVersionUID = -3606760167363150327L;
    protected int _index;
    protected Node _preNode;
    protected Node _nexNode;
    protected Node _insert;

    protected Insertion(Node parent, int fIndex) {
        super(parent, fIndex);
    }

    public Insertion(Node parent, int index, Node insert) {
        super(parent, VIndex.MOD_INSERT);
        _index = index;
        _insert = insert;
        _insert.setChanged();
    }

    public void setPrenode(Node node) {
        _preNode = node;
        if (_preNode != null) {
            _preNode.setInsertDepend(true);
        }
    }

    public Node getPrenode() {
        return _preNode;
    }

    public void setNextnode(Node node) {
        _nexNode = node;
        if (_nexNode != null) {
            _nexNode.setInsertDepend(true);
        }
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

    @Override
    public int size() {
        Queue<Node> nodes = new LinkedList<>();
        nodes.add(_insert);
        Node node;
        int size = 0;
        while(!nodes.isEmpty()) {
            node = nodes.poll();
            if (NodeUtils.isSimpleExpr(node)) {
                size ++;
            }
            nodes.addAll(node.getAllChildren());
        }
        return size;
    }

    public StringBuffer apply(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
        if(_insert == null) {
            return new StringBuffer("null");
        } else {
            return _insert.transfer(vars, exprMap, retType, exceptions);
        }
    }

    @Override
    public boolean patternMatch(Modification m, Map<Node, Node> matchedNode) {
        if (m instanceof Insertion) {
            Insertion insertion = (Insertion) m;
            return (matchedNode.get(getParent()) == insertion.getParent()
                    || getParent().patternMatch(insertion.getParent(), matchedNode))
                    && getInsertedNode().patternMatch(insertion.getInsertedNode(), matchedNode);
        }
        return false;
    }

    @Override
    public String formalForm() {
        return "[INS]" + _insert.formalForm(new NameMapping(), false, new HashSet<>());
    }

    @Override
    public String toString() {
        return String.format("[INS]INSERT %s UNDER %s AS {%d} CHILD", _insert, getParent(), _index);
    }
}
