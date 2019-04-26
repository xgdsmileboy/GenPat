/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.common.conf.Constant;
import mfix.core.node.ast.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-01-16
 */
public class MatchList {
    private Node _pNode;
    private List<MatchNode> _matchNodes;

    public MatchList(Node node) {
        _pNode = node;
    }

    public MatchList setMatchedNodes(List<MatchNode> nodes) {
        _matchNodes = new ArrayList<>(nodes);
        final int number = _pNode.getAllChildren().size() > 5 ?
                Constant.MAX_INSTANCE_PER_PATTERN : nodes.size();
            // complex expression can be filtered ahead of time
        _matchNodes = _matchNodes.stream()
                .sorted(Comparator.comparingDouble(MatchNode::getScore).reversed())
                .limit(number).collect(Collectors.toList());
        return this;
    }

    public Node getNode() {
        return _pNode;
    }

    public List<MatchNode> getMatchedNodes() {
        return _matchNodes;
    }

    public int nodeSize() {
        return _pNode.getAllChildren().size();
    }

    public int matchSize() {
        return _matchNodes.size();
    }

    public Iterator<MatchNode> getIterator() {
        return _matchNodes.iterator();
    }
}
