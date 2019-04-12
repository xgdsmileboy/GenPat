/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

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
    private List<MatchNode> _matchNodes;

    public MatchList(Node node) {
        _pNode = node;
    }

    public MatchList setMatchedNodes(List<MatchNode> nodes) {
        _matchNodes = new ArrayList<>(nodes);
        return this;
    }

    public Node getNode() {
        return _pNode;
    }

    public List<MatchNode> getMatchedNodes() {
        return _matchNodes;
    }

    public int matchSize() {
        return _matchNodes.size();
    }

    public Iterator<MatchNode> getIterator() {
        return _matchNodes.iterator();
    }

}
