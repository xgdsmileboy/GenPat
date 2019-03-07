/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.cluster;

import mfix.core.pattern.Pattern;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2019-03-07
 */
public class Group {
    private Pattern _pattern;
    private List<String> _others;

    public Group(Pattern p) {
        _pattern = p;
        _others = new LinkedList<>();
    }

    public void addIsomorphicPatternPath(String fileName) {
        _pattern.incFrequency(1);
        _others.add(fileName);
    }

    public void addIsomorphicPatternPaths(List<String> fileNames) {
        _pattern.incFrequency(fileNames.size());
        _others.addAll(fileNames);
    }

    public List<String> getIsomorphicPatternPath() {
        return _others;
    }

    public String getRepresentPatternFile() {
        return _pattern.getPatternName();
    }

    public Pattern getRepresentPattern() {
        return _pattern;
    }

    public boolean matches(Group group) {
        return _pattern.matches(group.getRepresentPattern());
    }

}
