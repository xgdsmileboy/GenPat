/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match.metric;

import mfix.core.node.ast.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-04-02
 */
public class LocationScore implements IScore {

    private double _weight;
    private List<Integer> _buggyLines;

    public LocationScore(double weight, List<Integer> buggyLines) {
        _weight = weight;
        _buggyLines = buggyLines;
    }

    @Override
    public double getWeight() {
        return _weight;
    }

    @Override
    public void setWeight(double weight) {
        _weight = weight;
    }

    @Override
    public double computeScore(Map<Node, Node> nodeMap, Map<String, String> strMap) {
        if (_buggyLines == null || _buggyLines.isEmpty()) {
            return 1.0;
        }
        Set<Integer> lines = new HashSet<>();
        for (Map.Entry<Node, Node> entry : nodeMap.entrySet()) {
            lines.add(entry.getValue().getStartLine());
        }
        for (int i = 0; i < _buggyLines.size(); i++) {
            if (lines.contains(_buggyLines.get(i))) {
                return 1.0 / Math.sqrt((i + 1.0));
            }
        }
        return 0;
    }
}
