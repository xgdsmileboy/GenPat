/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match.metric;

import mfix.core.node.ast.Node;

import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-04-02
 */
public interface IScore {

    double getWeight();

    void setWeight(double weight);

    double computeScore(Map<Node, Node> nodeMap, Map<String, String> strMap);

}
