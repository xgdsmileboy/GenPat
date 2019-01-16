/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node;

import mfix.core.node.ast.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2019-01-16
 */
public class MatchList {
    private Node _pNode;
    private List<MatchedNode> _matchedNodes;

    public MatchList(Node node) {
        _pNode = node;
    }

    public MatchList setMatchedNodes(List<MatchedNode> nodes) {
        _matchedNodes = new ArrayList<>(nodes);
        return this;
    }

    public Node getNode() {
        return _pNode;
    }

    public List<MatchedNode> getMatchedNodes() {
        return _matchedNodes;
    }

    public Iterator<MatchedNode> getIterator() {
        return _matchedNodes.iterator();
    }

}
