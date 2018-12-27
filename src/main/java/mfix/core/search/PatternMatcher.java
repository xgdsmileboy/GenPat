/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.core.parse.relation.Pattern;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018-12-25
 */
public class PatternMatcher {

    public static Set<Pattern> match(Pattern pattern, Set<Pattern> patterns) {
        Set<String> apis = pattern.getRelatedAPIs();
        Set<Pattern> matched = new HashSet<>();
        for(Pattern p : patterns) {
            Set<String> apinp = p.getRelatedAPIs();
            for(String string : apinp) {
                if(apis.contains(string)) {
                    matched.add(p);
                    break;
                }
            }
        }
        return matched;
    }

}
