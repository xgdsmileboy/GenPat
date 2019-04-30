/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.modify;

import mfix.core.node.ast.Node;

import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-01-14
 */
@Deprecated
public class Movement extends Modification {

    private static final long serialVersionUID = -4187409198171276499L;
    private int _oriIndex;
    private int _tarIndex;
    private Node _movedNode;

    public Movement(Node parent, int oriIndex, int tarIndex, Node movedNode) {
        super(parent, -1);
        _oriIndex = oriIndex;
        _tarIndex = tarIndex;
        _movedNode = movedNode;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean patternMatch(Modification m, Map<Node, Node> matchedNode) {
        return false;
    }

    @Override
    public String formalForm() {
        return "";
    }

    @Override
    public String toString() {
        return String.format("[MOV] %s FROM %d TO %d", _movedNode, _oriIndex, _tarIndex);
    }
}
