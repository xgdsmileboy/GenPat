/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.IScore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-01-16
 */
public class MatchInstance {

    private Map<Node, Node> _nodeMap;
    private Map<String, String> _strMap;
    private double _matchSimilarity = -1;

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

    public boolean modifyAny(Set<Integer> lines) {
        for (Map.Entry<Node, Node> entry : _nodeMap.entrySet()) {
            if (lines.contains(entry.getValue().getStartLine())) {
                return true;
            }
        }
        return false;
    }

    public void computeSimilarity(final List<IScore> similarities) {
        _matchSimilarity = 0;
        for (IScore similarity : similarities) {
            _matchSimilarity += similarity.computeScore(_nodeMap, _strMap) * similarity.getWeight();
        }
    }

    public double similarity() {
        return _matchSimilarity;
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

