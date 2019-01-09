package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class Insertion extends Modification {

    private Node _parent;
    private int _index;
    private String _insert;
    private boolean _wrap;

    public Insertion(Node parent, int index, String insert, boolean wrap) {
        _parent = parent;
        _index = index;
        _insert = insert;
        _wrap = wrap;
    }

    @Override
    public String toString() {
        if(_wrap) {
            return String.format("[INS]WRAP CHILD {%d} WITH {%s} UNDER {%s}", _index, _insert, _parent);
        } else {
            return String.format("[INS]INSERT {%s} UNDER {%s} As {%d} CHILD", _insert, _parent, _index);
        }
    }
}
