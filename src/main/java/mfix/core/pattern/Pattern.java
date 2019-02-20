/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.modify.Modification;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.Vector;
import mfix.core.pattern.match.PatternMatcher;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class Pattern implements PatternMatcher, Serializable {

    private static final long serialVersionUID = -1487307746482756299L;

    private int _frequency = 1;
    private Node _patternNode;
    private NameMapping _nameMapping;
    private Set<String> _keywords;

    public Pattern(Node pNode) {
        _patternNode = pNode;
        _nameMapping = new NameMapping();
        _keywords = new LinkedHashSet<>();
    }

    public String getFileName() {
        return _patternNode.getFileName();
    }

    public Node getPatternNode() {
        return _patternNode;
    }

    public int getFrequency() {
        return _frequency;
    }

    public void incFrequency(int frequency) {
        _frequency += frequency;
    }

    public Set<String> getKeywords() {
        if (_keywords == null) {
            formalForm();
        }
        return _keywords;
    }

    public Set<Modification> getAllModifications() {
        return _patternNode.getAllModifications(new LinkedHashSet<>());
    }

    public Set<MethodInv> getUniversalAPIs() {
        return _patternNode.getUniversalAPIs(new HashSet<>(), true);
    }

    public Vector getPatternVector(){
        return _patternNode.getPatternVector();
    }

    public StringBuffer formalForm() {
        _keywords = new LinkedHashSet<>();
        return _patternNode.formalForm(_nameMapping, false, _keywords);
    }

    public Set<Node> getConsideredNodes() {
        return _patternNode.getConsideredNodesRec(new HashSet<>(), true);
    }

    @Override
    public boolean matches(Pattern p) {
        return false;
    }
}
