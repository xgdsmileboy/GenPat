/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.core.node.ast.Node;

import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-01-16
 */
public class MatchNode {
    private Node _node;
    private Map<Node, Node> _nodeMap;
    private Map<String, String> _strMap;

    public MatchNode(Node node, Map<Node, Node> nodeMap, Map<String, String> strMap) {
        _node = node;
        _nodeMap = nodeMap;
        _strMap = strMap;
    }

    public Node getNode() {
        return _node;
    }

    public Map<Node, Node> getNodeMap() {
        return _nodeMap;
    }

    public Map<String, String> getStrMap() {
        return _strMap;
    }

    @Override
    public String toString() {
        return _node.toString();
    }
}
