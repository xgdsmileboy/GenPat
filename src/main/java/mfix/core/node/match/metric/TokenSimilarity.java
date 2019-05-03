/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match.metric;

import mfix.core.node.ast.Node;
import mfix.core.node.match.Matcher;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-04-11
 */
public class TokenSimilarity extends AbsSimilarity {

    public TokenSimilarity(double weight) {
        super(weight);
    }

    @Override
    public double computeScore(Map<Node, Node> nodeMap, Map<String, String> strMap) {
        double total = 0;
        for (Map.Entry<Node, Node> entry : nodeMap.entrySet()) {
            Node src = entry.getKey();
            Node tar = entry.getValue();
            List<String> srcTokens = camelSplit(src.tokens());
            List<String> tarTokens = camelSplit(tar.tokens());
            Map<Integer, Integer> map = Matcher.match(srcTokens, tarTokens, (o1, o2) -> o1.equals(o2) ? 1 : 0);
            total += (map.size() * 2.0) / (double) (srcTokens.size() + tarTokens.size());
        }
        total = total / (double) nodeMap.size();
        return total;
    }

    private List<String> camelSplit(List<String> tokens) {
        List<String> result = new LinkedList<>();
        int index ;
        for (String s : tokens) {
            if (s.isEmpty()) continue;
            if (!Character.isDigit(s.charAt(0))) {
                for (index = s.length() - 1; index >= 0; index--) {
                    if (!Character.isDigit(s.charAt(index))) {
                        break;
                    }
                }
                s = s.substring(0, index + 1);
            }
            int lower = 0;
            for(int i = 0; i < s.length(); i++){
                if(Character.isUpperCase(s.charAt(i))){
                    String subName = s.substring(lower, i);
                    lower = i;
                    result.add(subName.toLowerCase());
                } else if(s.charAt(i) == '_'){
                    String subName = s.substring(lower, i);
                    lower = i + 1;
                    result.add(subName.toLowerCase());
                }
            }
            if (lower < s.length()) {
                result.add(s.substring(lower).toLowerCase());
            }
        }
        return result;
    }
}
