/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match.metric;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;

import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-04-02
 */
public class NodeSimilarity implements ISimilarity {

    @Override
    public double computeSimilarity(Map<Node, Node> nodeMap, Map<String, String> strMap) {
        double score = 0;
        Node src, tar;
        Expr srcExpr, tarExpr;
        for (Map.Entry<Node, Node> entry : nodeMap.entrySet()) {
            src = entry.getKey();
            tar = entry.getValue();
            if (src.getNodeType() == tar.getNodeType()) {
                score += 1;
            } else if (src instanceof Expr && tar instanceof Expr) {
                srcExpr = (Expr) src;
                tarExpr = (Expr) tar;
                if (!"?".equals(srcExpr.getTypeStr())
                        && srcExpr.getTypeStr().equals(tarExpr.getTypeStr())) {
                    score += 1;
                }
            }
        }
        score /= (double) nodeMap.size();
        return score;
    }
}
