/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.modify;

import mfix.core.node.ast.Node;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2019-01-14
 */
public class Wrap extends Insertion {

    private Node _wrapper;
    private List<Node> _nodes;
    public Wrap(Node parent, Node wrapper, List<Node> wrapped) {
        super(parent);
        _wrapper = wrapper;
        _nodes = wrapped;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Node node : _nodes) {
            stringBuffer.append(node.toString() + "\n");
        }
        return String.format("[WRP]USING %s WRAP %s", _wrapper, stringBuffer.toString());
    }
}
