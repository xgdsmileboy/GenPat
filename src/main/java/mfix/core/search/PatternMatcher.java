/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.core.parse.Pattern;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018-12-25
 */
public class PatternMatcher {


    /**
     * when given a potentially faulty pattern {@code pattern}, we
     * first filter out non-relevant repair pattern in {@code patterns}
     * based on the concrete APIs used in two patterns
     *
     * @param pattern : potentially faulty pattern
     * @param patterns : repair patterns to filter
     * @return : a set of patterns can be used to make further match
     */
    public static Set<Pattern> filter(Pattern pattern, Set<Pattern> patterns) {
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

    /**
     * adapt the code of {@code buggyPattern} to generate fix
     * based on the {@code absPattern}
     * @param buggyPattern : pattern of potentially buggy code
     * @param absPattern : abstract repair patterns to generate fix
     * @return : {@code String} format of a method declaration after fixing, can
     * be null if the adaptation failed.
     */
    public static String adaptation(Pattern buggyPattern, Pattern absPattern) {
        return null;
    }

}
