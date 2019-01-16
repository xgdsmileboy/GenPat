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
}

