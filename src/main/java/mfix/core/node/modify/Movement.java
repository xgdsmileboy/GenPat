/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.modify;

import mfix.core.node.ast.Node;

/**
 * @author: Jiajun
 * @date: 2019-01-14
 */
public class Movement extends Modification {
    private int _oriIndex;
    private int _tarIndex;
    private Node _movedNode;

    public Movement(Node parent, int oriIndex, int tarIndex, Node movedNode) {
        super(parent);
        _oriIndex = oriIndex;
        _tarIndex = tarIndex;
        _movedNode = movedNode;
    }

    @Override
    public String toString() {
        return String.format("[MOV] %s FROM %d TO %d", _movedNode, _oriIndex, _tarIndex);
    }
}
