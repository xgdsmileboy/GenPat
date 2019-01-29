/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node;

import mfix.core.node.ast.Node;

import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-01-16
 */
public class MatchInstance {

    private Map<Node, Node> _nodeMap;
    private Map<String, String> _strMap;

    public MatchInstance(Map<Node, Node> nodeMap, Map<String, String> strMap) {
        _nodeMap = nodeMap;
        _strMap = strMap;
    }

    public Map<Node, Node> getNodeMap() {
        return _nodeMap;
    }

    public Map<String, String> getStrMap() {
        return _strMap;
    }

    public void apply() {
        for(Map.Entry<Node, Node> entry : _nodeMap.entrySet()) {
            entry.getKey().setBuggyBindingNode(entry.getValue());
        }
    }

    public void reset() {
        for(Map.Entry<Node, Node> entry : _nodeMap.entrySet()) {
            entry.getKey().resetBuggyBinding();
            entry.getValue().resetBuggyBinding();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MatchInstance)) {
            return false;
        }
        MatchInstance instance = (MatchInstance) obj;
        Map<Node, Node> nodeMap = instance.getNodeMap();

        if (_nodeMap.size() != nodeMap.size()) {
            return false;
        }

        for (Map.Entry<Node, Node> entry : _nodeMap.entrySet()) {
            if (nodeMap.get(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }
}

