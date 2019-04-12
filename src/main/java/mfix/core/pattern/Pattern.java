/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.Variable;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Wrap;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.Vector;
import mfix.core.pattern.match.PatternMatcher;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public class Pattern implements PatternMatcher, Serializable {

    private static final long serialVersionUID = -1487307746482756299L;

    private int _frequency = 1;
    private Node _patternNode;
    private FVector _fVector;
    private Set<Variable> _newVars;
    private Set<String> _imports;
    private transient String _patternName;
    private transient NameMapping _nameMapping;
    private transient Set<String> _keywords;
    private transient Set<String> _targetKeywords;
    private transient Set<Modification> _modifications;

    public Pattern(Node pNode) {
        this(pNode, new HashSet<>());
    }

    public Pattern(Node pNode, Set<String> imports) {
        _patternNode = pNode;
        _imports = imports;
        _newVars = new HashSet<>();
    }

    public String getFileName() {
        return _patternNode.getFileName();
    }

    public Node getPatternNode() {
        return _patternNode;
    }

    public FVector getFeatureVector() {
        if (_fVector == null) {
            computeFeatureVector();
        }
        return _fVector;
    }

    public String getPatternName() {
        return _patternName;
    }

    public void setPatternName(String name) {
        _patternName = name;
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

    public void addNewVars(Set<Variable> vars) {
        _newVars.addAll(vars);
    }

    public Set<Variable> getNewVars() {
        return _newVars;
    }

    public Set<String> getTargetKeywords() {
        if (_targetKeywords == null) {
            _targetKeywords = new LinkedHashSet<>();
            _patternNode.getBindingNode().formalForm(_nameMapping, false, _targetKeywords);
        }
        return _targetKeywords;
    }

    public Set<Modification> getAllModifications() {
        if (_modifications == null) {
            _modifications = _patternNode.getAllModifications(new LinkedHashSet<>());
        }
        return _modifications;
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

    public List<String> formalModifications() {
        List<String> strings = new LinkedList<>();
        formalForm();
        for (Modification m : getAllModifications()) {
            strings.add(m.formalForm());
        }
        return strings;
    }

    public Set<Node> getConsideredNodes() {
        return _patternNode.getConsideredNodesRec(new HashSet<>(), true);
    }

    private void computeFeatureVector() {
        _patternNode.getFeatureVector();
        _fVector = new FVector();
        for (Node node : getConsideredNodes()) {
            _fVector.combineFeature(node.getSingleFeatureVector());
        }
    }

    private boolean possibleSameModification(Pattern p) {
        Set<Modification> modifications = getAllModifications();
        Set<Modification> pmodifications = p.getAllModifications();

        if (modifications.size() != modifications.size()) {
            return false;
        }

        int insCount = 0, delCount = 0, updCount = 0, wrapCount = 0;
        for (Modification m : modifications) {
            if (m instanceof Wrap) {
                wrapCount ++;
            } else if (m instanceof Insertion) {
                insCount ++;
            } else if (m instanceof Deletion) {
                delCount ++;
            } else {
                updCount ++;
            }
        }

        for (Modification m : pmodifications) {
            if (m instanceof Wrap) {
                wrapCount --;
            } else if (m instanceof Insertion) {
                insCount --;
            } else if (m instanceof Deletion) {
                delCount --;
            } else {
                updCount --;
            }
        }
        return insCount == 0 && delCount == 0 && updCount == 0 && wrapCount == 0;
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

        if (!possibleSameModification(p)) {
            return false;
        }

        Set<Node> nodes = getConsideredNodes();
        Set<Node> others = p.getConsideredNodes();

        if (nodes.size() != others.size()) {
            return false;
        }

        boolean match;
        Map<Node, Node> map = new Hashtable<>();
        Map<Node, Node> temp = new Hashtable<>();
        for (Node node : nodes) {
            match = false;
            for (Iterator<Node> iter = others.iterator(); iter.hasNext();) {
                temp.clear();
                temp.putAll(map);
                if (node.patternMatch(iter.next(), temp)) {
                    map.putAll(temp);
                    match = true;
                    iter.remove();
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }
}
