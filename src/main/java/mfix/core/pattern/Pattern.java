/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.modify.Modification;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.Vector;
import mfix.core.pattern.match.PatternMatcher;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
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
    private Set<String> _imports;
    private transient NameMapping _nameMapping;
    private transient Set<String> _keywords;
    private transient Set<String> _targetKeywords;

    public Pattern(Node pNode) {
        this(pNode, new HashSet<>());
    }

    public Pattern(Node pNode, Set<String> imports) {
        _patternNode = pNode;
        _imports = imports;
    }

    public String getFileName() {
        return _patternNode.getFileName();
    }

    public Node getPatternNode() {
        return _patternNode;
    }

    public Set<String> getImports() {
        return _imports;
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

    public Set<String> getTargetKeywords() {
        if (_targetKeywords == null) {
            _targetKeywords = new LinkedHashSet<>();
            _patternNode.getBindingNode().formalForm(_nameMapping, false, _targetKeywords);
        }
        return _targetKeywords;
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
        if (_keywords == null) {
            _keywords = new LinkedHashSet<>();
        }
        if (_nameMapping == null) {
            _nameMapping = new NameMapping();
        }
        return _patternNode.formalForm(_nameMapping, false, _keywords);
    }

    public Set<Node> getConsideredNodes() {
        return _patternNode.getConsideredNodesRec(new HashSet<>(), true);
    }

    @Override
    public boolean matches(Pattern p) {
        Set<String> srcKey = getKeywords();
        Set<String> psKey = p.getKeywords();

        if (!Utils.safeCollectionEqual(srcKey, psKey)) {
            return false;
        }

        srcKey = getTargetKeywords();
        psKey = p.getTargetKeywords();

        if (!Utils.safeCollectionEqual(srcKey, psKey)) {
            return false;
        }

        if (getAllModifications().size() != p.getAllModifications().size()) {
            return false;
        }

        Set<Node> nodes = getConsideredNodes();
        Set<Node> others = p.getConsideredNodes();

        if (nodes.size() != others.size()) {
            return false;
        }

        boolean match;
        for (Node node : nodes) {
            match = false;
            for (Iterator<Node> iter = others.iterator(); iter.hasNext();) {
                if (node.patternMatch(iter.next())) {
                    match = true;
                    iter.remove();
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }
}
