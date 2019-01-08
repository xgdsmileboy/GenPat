/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern.relation;

import mfix.common.util.Pair;
import mfix.core.node.ast.Node;
import mfix.core.stats.element.ElementCounter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/11/29
 */
public abstract class Relation implements Serializable {

    private static final long serialVersionUID = 706186378892330947L;

    /**
     * types of relations.
     */
    public enum RelationKind {
        ARGUMENT("Argument"),
        OPERATION("Operation"),
        DEFINE("VarDefinition"),
        ASSIGN("Assignment"),
        MCALL("MethodCall"),
        RETURN("Return"),
        STRUCTURE("Structure"),
        CHILD("Child"),

        VIRTUALDEFINE("VirtualDefinition");

        private String _value;

        private RelationKind(String value) {
            _value = value;
        }

        @Override
        public String toString() {
            return _value;
        }}

    /**
     * Designed to boost relation matching process.
     * <p>
     * This field denotes the type of the relation,
     * which is used for relation matching.
     * NOTE: this kind of relation is coarse-grained
     * since different concrete expressions may
     * have the same relation kind, the matching relation
     * should rely on more concrete matching process.
     */
    private RelationKind _relationKind;
    /**
     * This is used to record the original ast node
     */
    private transient Node _node;
    /**
     * a relation may rely on some other relation
     * Especially, this list record the define use relation
     */
    protected Set<ObjRelation> _dataDependon;

    /**
     *
     */
    protected RStruct _controlDependon;

    /**
     * used to store the parent relation
     * similar to the parent node in AST (syntactic)
     */
    private Relation _parent;

    /**
     * record this relation is used by
     * which relations
     */
    protected Set<Relation> _usedBy;

    /**
     * Label whether a relation is matched before
     * and after repair, initially all relations are
     * not matched.
     */
    protected boolean _matched = false;

    /**
     * Label whether a relation is considered in
     * the matching procedure, if >= 0, considered
     * otherwise, not considered. Its value denotes
     * the expanded level. Initially, All relations
     * are considered and the expanded value are 0;
     */
    protected int _expandedLevel = 0;

    /**
     * Label this relation is abstract or not.
     */
    protected boolean _isAbstract = false;
    // used to avoid repeat abstraction
    private transient boolean _visited = false;

    protected Relation(Node node, RelationKind kind) {
        _node = node;
        _relationKind = kind;
        _dataDependon = new HashSet<>();
        _usedBy = new HashSet<>();
    }

    public RelationKind getRelationKind() {
        return _relationKind;
    }

    public Node getAstNode() {
        return _node;
    }

    public void setParent(Relation parent) {
        _parent = parent;
    }

    public Relation getParent() {
        return _parent;
    }

    public void setMatched(boolean matched) {
        _matched = matched;
        _expandedLevel = matched ? -1 : 0;
    }

    public boolean isMatched() {
        return _matched;
    }

    public void setExpendedLevel(int expandedLevel) {
        if (!isConcerned()) {
            _expandedLevel = expandedLevel;
        }
    }

    public boolean isConcerned() {
        return _expandedLevel >= 0;
    }

    public int getExpandedLevel() {
        return _expandedLevel;
    }

    public boolean isAbstract() {
        return _isAbstract;
    }

    public void addDependency(ObjRelation relation) {
        if (relation != null) {
            _dataDependon.add(relation);
        }
    }

    public Set<ObjRelation> getDependencies() {
        return _dataDependon;
    }

    public void usedBy(Relation relation) {
        if (relation != null) {
            _usedBy.add(relation);
        }
    }

    public Set<Relation> getUsedBy() {
        return _usedBy;
    }


    /**
     * generic API for MCall and Operation
     * @param arg
     */
    public void addArg(RArg arg) {
    }

    /**
     * Expand current relations downwards
     *
     * @param set :
     * @return
     */
    public Set<Relation> expandDownward(Set<Relation> set) {
        set.addAll(_dataDependon);
        return expandDownward0(set);
    }

    /**
     * record control dependencies
     * @param rstruct : structure of the control flow
     * @param controls : controled components under the structure
     */
    protected void setControlDependency(RStruct rstruct, Set<Relation> controls){}

    /**
     * Return the expression string format.
     * This is for user-friendly debugging
     *
     * @return
     */
    public String getExprString() {
        return "";
    }

    /**
     * Expand the changed relations downwards
     *
     * @param set : set to add the newly added relations
     * @return : a set of newly added relations
     */
    protected abstract Set<Relation> expandDownward0(Set<Relation> set);

    /**
     * Perform object abstraction in the relation
     */
    public void doAbstraction(ElementCounter counter) {
        if (isConcerned() && !_visited) {
            _visited = true;
            doAbstraction0(counter);
        }
    }

    protected abstract void doAbstraction0(ElementCounter counter);

    /**
     * The matched relation cannot be {@code null}
     * and the relation type should be the same
     *
     * @param relation : relation to be matched
     * @return {@code true} if {@code relation} match current
     * relation, {@code false} otherwise.
     */
    public boolean match(Relation relation, Set<Pair<Relation, Relation>> dependencies) {
        if (relation == null || relation.getRelationKind() != _relationKind) {
            return false;
        }
        return true;
    }


    //**************************************************************//
    //************ FOR REPAIR **************************************//
    //**************************************************************//
    protected Relation _matchedBinding = null;
    protected Relation _foldMatchBinding = null;

    public boolean alreadyMatched() {
        return _matchedBinding != null;
    }

    public Relation getBindingRelation() {
        return _matchedBinding;
    }

    /**
     * Perform the core pattern matching algorithm when given a potential buggy pattern
     *
     * @param r            : relation in a potential buggy pattern, waiting for repair
     * @param dependencies : dependencies to match current relations
     * @param varMapping   : variable mapping relation
     * @return true of matches, false otherwise
     */
    public boolean greedyMatch(Relation r, Map<Relation, Relation> dependencies,
                                        Map<String, String> varMapping) {
        if(r == null) return false;
        if(alreadyMatched() || r.alreadyMatched()) {
            if(dependencies.get(this) != r) {
                return false;
            }
        }
        if(r.isConcerned() && r.getRelationKind() == getRelationKind()) {
            return true;
        }
        return false;
    }

    /**
     * @param varMapping
     * @return
     */
    public abstract boolean foldMatching(Map<String, String> varMapping);


    protected boolean matchList(List<Relation> left, List<Relation> right, Map<Relation, Relation> dependencies,
                                Map<String, String> varMapping) {
        Set<Integer> matched = new HashSet<>();
        for (int i = 0; i < left.size(); i++) {
            for (int j = 0; j < right.size(); j++) {
                if (matched.contains(j)) continue;
                if (left.get(i).greedyMatch(right.get(j), dependencies, varMapping)) {
                    matched.add(j);
                }
            }
        }
        return !matched.isEmpty();
    }

}
