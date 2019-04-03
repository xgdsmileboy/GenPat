/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.modify;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-01-14
 */
public class Wrap extends Update {

    private static final long serialVersionUID = 4871112003593684776L;
    private List<Node> _nodes;

    public Wrap(Node parent, Node del, Node wrapper, List<Node> wrapped) {
        super(parent, del, wrapper);
        _nodes = wrapped;
    }

    public StringBuffer apply(VarScope vars, Map<String, String> exprMap, String retType,
                              Set<String> exceptions, List<Node> nodes) {
        if(getTarNode() == null) {
            return new StringBuffer("null");
        } else {
            return getTarNode().transfer(vars, exprMap, retType, exceptions, nodes);
        }
    }

    @Override
    public String toString() {
        return "[WRP]" + getSrcNode() + " TO " + getTarNode();
    }
}
