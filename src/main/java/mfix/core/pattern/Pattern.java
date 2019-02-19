/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.ast.Node;
import mfix.core.pattern.match.PatternMatcher;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class Pattern implements PatternMatcher, Serializable {

    private static final long serialVersionUID = -1487307746482756299L;

    private Node _patternNode;
    private CodeAbstraction _abstraction;

    public Pattern(Node pNode, CodeAbstraction abstraction) {
        _patternNode = pNode;
        _abstraction = abstraction;
    }


    @Override
    public boolean matches(Pattern p) {
        return false;
    }
}
