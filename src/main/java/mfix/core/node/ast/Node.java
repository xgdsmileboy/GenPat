/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast;

import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.comp.NodeComparator;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Movement;
import mfix.core.pattern.relation.Relation;
import mfix.core.stats.element.ElementCounter;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Node implements NodeComparator, Serializable {

    private static final long serialVersionUID = -6995771051040337618L;
    /**
     * source file name (with absolute path)
     */
    protected String _fileName;
    /**
     * start line number of current node in the source file
     */
    protected int _startLine;
    /**
     * end line number of current node in the source file
     */
    protected int _endLine;
    /**
     * parent node in the abstract syntax tree
     */
    protected Node _parent;
    /**
     * enum type of node, for easy comparison
     */
    protected TYPE _nodeType = TYPE.UNKNOWN;
    /**
     * feature vector to represent current node
     */
    protected FVector _fVector = null;
    /**
     * data dependency
     */
    private Node _datadependency;
    /**
     * control dependency
     */
    protected Node _controldependency;
    /**
     * current variable is used by {@code Node} {@code _preUseChain} used previously
     * NOTE: not null for variables only (e.g., Name, FieldAcc, and AryAcc etc.)
     */
    private Node _preUseChain;
    /**
     * current variable will be used by {@code Node} {@code _nextUseChain} used next
     * NOTE: not null for variables only (e.g., Name, FieldAcc, and AryAcc etc.)
     */
    private Node _nextUseChain;
    /**
     * original AST node in the JDT abstract tree model
     * NOTE: AST node dose not support serialization
     */
    protected transient ASTNode _oriNode;
    /**
     * tokenized representation of node
     * NOTE: includes all symbols, e.g., '[', but omit ';'.
     */
    protected transient LinkedList<String> _tokens = null;

    /**
     * @param fileName : source file name (with absolute path)
     * @param startLine : start line number of the node in the original source file
     * @param endLine : end line number of the node in the original source file
     * @param oriNode : original abstract syntax tree node in the JDT model
     */
    public Node(String fileName, int startLine, int endLine, ASTNode oriNode) {
        this(fileName, startLine, endLine, oriNode, null);
    }

    /**
     * @param fileName : source file name (with absolute path)
     * @param startLine : start line number of the node in the original source file
     * @param endLine : end line number of the node in the original source file
     * @param oriNode : original abstract syntax tree node in the JDT model
     * @param parent : parent node in the abstract syntax tree
     */
    public Node(String fileName, int startLine, int endLine, ASTNode oriNode, Node parent) {
        _fileName = fileName;
        _startLine = startLine;
        _endLine = endLine;
        _oriNode = oriNode;
        _parent = parent;
    }

    /**
     * get the start line number of node in the original source file
     * @return : line number
     */
    public int getStartLine() {
        return _startLine;
    }

    /**
     * get the end line number of node in the original source file
     * @return : line number
     */
    public int getEndLine() {
        return _endLine;
    }

    /**
     * set current node type, {@code Node.TYPE.UNKNOWN} as default
     * @param nodeType : node type
     */
    public void setNodeType(TYPE nodeType) {
        this._nodeType = nodeType;
    }

    /**
     * get node type (see {@code Node.TYPE})
     * @return : current node type
     */
    public TYPE getNodeType() {
        return _nodeType;
    }

    /**
     * set the parent node in the abstract syntax tree
     * @param parent : parent node
     */
    public void setParent(Node parent) {
        this._parent = parent;
    }

    /**
     * get parent node in the abstract syntax tree
     * @return : parent node
     */
    public Node getParent() {
        return _parent;
    }

    /**
     * set data dependency of node
     * @param dependency : dependent node, can be {@code null}
     */
    public void setDataDependency(Node dependency) {
        _datadependency = dependency;
    }

    /**
     * get data dependency
     * @return : data dependent node, can be {@code null}
     */
    public Node getDataDependency() {
        return _datadependency;
    }

    /**
     * set control dependency of node
     * @param dependency : dependent node, can be {@code null}
     */
    public void setControldependency(Node dependency) {
        _controldependency = dependency;
    }

    /**
     * get control dependency
     * @return
     */
    public Node getControldependency() {
        if(getParentStmt() == null) return null;
        return getParentStmt()._controldependency;
    }

    public void setPreUsed(Node node) {
        _preUseChain = node;
        if(node != null) {
            node.setNextUsed(this);
        }
    }

    public Node getPreUsed() {
        return _preUseChain;
    }

    public void setNextUsed(Node node) {
        _nextUseChain = node;
        if(node != null) {
            node.setPreUsed(this);
        }
    }

    public Node getNextUsed() {
        return _nextUseChain;
    }

    /**
     * traverse the complete sub-tree with the given {@code visitor}
     * @param visitor : traverser (visitor pattern)
     */
    public final void accept(NodeVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("visitor should not be null!");
        }
        visitor.preVisit(this);

        if (visitor.preVisit(this)) {
            accept0(visitor);
        }
        // end with the generic post-visit
        visitor.postVisit(this);
    }

    /**
     * traverse the sub-tree downwards, used internally only
     * @param visitor : traverser (visitor pattern)
     */
    protected final void accept0(NodeVisitor visitor) {
        if (visitor.visit(this)) {
            for (Node node : getAllChildren()) {
                if (node != null) {
                    node.accept(visitor);
                }
            }
        }
        visitor.endVisit(this);
    }

    /**
     * compute the feature vector for current node
     * @return : feature vector representation
     */
    public FVector getFeatureVector() {
        if (_fVector == null) {
            computeFeatureVector();
        }
        return _fVector;
    }

    /**
     * obtain the tokens representation of current node
     * @return
     */
    public List<String> tokens() {
        if (_tokens == null) {
            tokenize();
        }
        return _tokens;
    }

    /**
     * obtain all defined variables in the sub-tree
     * @return : all variable definition node (see {@code SName})
     */
    public Set<SName> getAllVars() {
        Set<SName> set = new HashSet<>();
        if (this instanceof SName) {
            set.add((SName) this);
        }
        for (Node node : getAllChildren()) {
            set.addAll(node.getAllVars());
        }
        return set;
    }

    /**
     * output source code with string format
     * @return : source code string
     */
    public abstract StringBuffer toSrcString();

    /**
     * get (non-direct) parent node that is {@code Stmt} type, maybe itself
     * @return : parent node if exist, otherwise {@code null}
     */
    public abstract Stmt getParentStmt();

    /**
     * get all {@code Stmt} child node, does not include itself
     * NOTE: empty for all {@code Expr} node
     * @return : all child statement
     */
    public abstract List<Stmt> getChildren();

    /**
     * return all child node, does not include itself
     * @return : child node
     */
    public abstract List<Node> getAllChildren();

    /**
     * compute the feature vector for current node recursively
     * and cache the result
     */
    public abstract void computeFeatureVector();

    /**
     * recursively tokenize the sub abstract syntax tree downwards
     * and cache the result
     */
    protected abstract void tokenize();

    /*********************************************************/
    /******* record matched information for change ***********/
    /*********************************************************/
    /**
     * bind the target node in the fixed version
     */
    private Node _bindingNode;
    /**
     * tag whether the node is expanded or not
     */
    private boolean _expanded = false;
    /**
     * tag whether the node is changed or not after fix
     */
    private boolean _changed = false;
    /**
     * list of modifications bound to the node
     */
    protected List<Modification> _modifications = new LinkedList<>();

    public boolean isChanged() {
        return _changed;
    }

    public boolean isConsidered() {
        return _expanded || _bindingNode == null;
    }

    public void setConsidered(boolean considered) {
        _expanded = considered;
    }

    public void setBindingNode(Node binding) {
        _bindingNode = binding;
        if(_bindingNode != null) {
            binding._bindingNode = this;
        }
    }

    public Node getBindingNode() {
        return _bindingNode;
    }

    /**
     * obtain the considered node patterns
     * i.e., all nodes considered based on the data/control dependency
     * and the structure information (children and parent)
     * @param nodes : all nodes to be considered
     * @param includeExpanded : tag whether consider the expanded node
     * @return : a set of nodes
     */
    public Set<Node> getConsideredNodesRec(Set<Node> nodes, boolean includeExpanded) {
        if(_bindingNode == null) {
            nodes.add(this);
        } else {
            boolean notAdded = true;
            if((includeExpanded && _expanded)) {
                nodes.add(this);
                notAdded = false;
            }

            if(notAdded && _changed) {
                nodes.add(this);
                notAdded = false;
            }

            if(notAdded) {
                // data dependency changed
                if (getDataDependency() == null) {
                    if (_bindingNode.getDataDependency() != null) {
                        nodes.add(this);
                        notAdded = false;
                    }
                } else if (getDataDependency().getBindingNode() != _bindingNode.getDataDependency()) {
                    nodes.add(this);
                    notAdded = false;
                }
            }

            if(notAdded) {
                // control dependency changed
                if(getControldependency() == null) {
                    if(_bindingNode.getControldependency() != null) {
                        nodes.add(this);
                    }
                } else if(getControldependency().getBindingNode() != _bindingNode.getControldependency()){
                    nodes.add(this);
                }
            }
        }

        for(Node node : getAllChildren()) {
            node.getConsideredNodesRec(nodes, includeExpanded);
        }
        return nodes;
    }

    /**
     * expand node considered for match
     * @param nodes : considered node set
     * @return : a set of nodes
     */
    public Set<Node> expand(Set<Node> nodes) {
        expandDependency(nodes);
        expandBottomUp(nodes);
        expandTopDown(nodes);
        return nodes;
    }

    /**
     * expand pattern with dependency relations
     * @param nodes : considered node set
     */
    private void expandDependency(Set<Node> nodes) {
        if(_datadependency != null) {
            _datadependency.setConsidered(true);
            nodes.add(_datadependency);
        }
        if(_controldependency != null) {
            _controldependency.setConsidered(true);
            nodes.add(_controldependency);
        }
    }

    /**
     * expand children based on syntax
     * @param nodes : considered node set
     */
    private void expandTopDown(Set<Node> nodes) {
        for(Node node : getAllChildren()) {
            node.setConsidered(true);
        }
        nodes.addAll(getAllChildren());
    }

    /**
     * expand parent based on syntax
     * @param nodes : considered node set
     */
    private void expandBottomUp(Set<Node> nodes) {
        if(_parent != null) {
            _parent.setConsidered(true);
            nodes.add(_parent);
        }
    }

    /**
     * judging whether the given {@code node} is compatible or not
     * @param node : given node
     * @return : {@code true} is compatible, otherwise {@code false}
     */
    protected boolean canBinding(Node node) {
        return node != null && node.getNodeType() == _nodeType && node.getBindingNode() == null;
    }

    /**
     * based on the node binding info, continue to match the child nodes
     * when parent nodes are not matched
     */
    protected void continueTopDownMatchNull() {
        for(Node node : getAllChildren()) {
            node.postAccurateMatch(null);
        }
    }

    /**
     * match two list of nodes greedily
     * @param lst1 : first list
     * @param lst2 : second list
     */
    protected void greedyMatchListNode(List<? extends Node> lst1, List<? extends  Node> lst2) {
        Set<Node> set = new HashSet<>();
        for (Node node : lst1) {
            for (Node other : lst2) {
                if(!set.contains(other) && node.postAccurateMatch(other)) {
                    set.add(other);
                }
            }
        }
    }

    public Set<Modification> getAllModifications(Set<Modification> modifications) {
        modifications.addAll(_modifications);
        for(Node node : getAllChildren()) {
            node.getAllModifications(modifications);
        }
        return modifications;
    }
    /**
     * match node after constraint solving
     * @param node : node to match
     * @return : {@code true} is current node matches {@code node}, otherwise {@code false}
     */
    public abstract boolean postAccurateMatch(Node node);

    /**
     * based on the matching result, generate modifications
     */
    public abstract boolean genModidications();

    protected void genModificationList(List<? extends Node> src, List<? extends Node> tar) {
        genModificationList(src, tar, true);
    }

    protected void genModificationList(List<? extends Node> src, List<? extends Node> tar, boolean move) {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < src.size(); i++) {
            boolean notmatch = true;
            for (int j = 0; j < tar.size(); j++) {
                if (set.contains(j)) continue;
                if (src.get(i).getBindingNode() == tar.get(j)) {
                    set.add(j);
                    src.get(i).genModidications();
                    if (i != j && move) {
                        Movement movement = new Movement(this, i, j, src.get(i));
                        _modifications.add(movement);
                    }
                    notmatch = false;
                    break;
                }
            }
            if (notmatch) {
                Deletion deletion = new Deletion(this, src.get(i));
                _modifications.add(deletion);
            }
        }
        for (int i = 0; i < tar.size(); i++) {
            if (set.contains(i)) continue;
            Insertion insertion = new Insertion(this, i, tar.get(i));
            _modifications.add(insertion);
        }
    }

    protected boolean childMatch(Node curNode) {
        for(Node node : getAllChildren()) {
            if (node.getBindingNode() == curNode || node.childMatch(node)) {
                return true;
            }
        }
        return false;
    }


    /*********************************************************/
    /*********** interaction with relation model *************/
    /*********************************************************/

    private Relation _binding;
    public void setBindingRelation(Relation r) {
        _binding = r;
    }

    public Relation getBindingRelation() {
        return _binding;
    }

    /*********************************************************/
    /**************** pattern abstraction ********************/
    /*********************************************************/
    protected boolean _abstract = true;

    public boolean isAbstract() {
        return _abstract;
    }

    public void doAbstraction(ElementCounter counter) {
        for(Node node : getAllChildren()) {
            node.doAbstraction(counter);
        }
    }

    @Override
    public String toString() {
        return toSrcString().toString();
    }

    /**
     * all types of abstract syntax tree node considered currently
     */
    public enum TYPE {

        METHDECL("MethodDeclaration"),
        ARRACC("ArrayAccess"),
        ARRCREAT("ArrayCreation"),
        ARRINIT("ArrayInitilaization"),
        ASSIGN("Assignment"),
        BLITERAL("BooleanLiteral"),
        CAST("CastExpression"),
        CLITERAL("CharacterLiteral"),
        CLASSCREATION("ClassInstanceCreation"),
        COMMENT("Annotation"),
        CONDEXPR("ConditionalExpression"),
        DLITERAL("DoubleLiteral"),
        FIELDACC("FieldAccess"),
        FLITERAL("FloatLiteral"),
        INFIXEXPR("InfixExpression"),
        INSTANCEOF("InstanceofExpression"),
        INTLITERAL("IntLiteral"),
        LABEL("Name"),
        LLITERAL("LongLiteral"),
        MINVOCATION("MethodInvocation"),
        NULL("NullLiteral"),
        NUMBER("NumberLiteral"),
        PARENTHESISZED("ParenthesizedExpression"),
        EXPRSTMT("ExpressionStatement"),
        POSTEXPR("PostfixExpression"),
        PREEXPR("PrefixExpression"),
        QNAME("QualifiedName"),
        SNAME("SimpleName"),
        SLITERAL("StringLiteral"),
        SFIELDACC("SuperFieldAccess"),
        SMINVOCATION("SuperMethodInvocation"),
        SINGLEVARDECL("SingleVariableDeclation"),
        THIS("ThisExpression"),
        TLITERAL("TypeLiteral"),
        VARDECLEXPR("VariableDeclarationExpression"),
        VARDECLFRAG("VariableDeclarationFragment"),
        ANONYMOUSCDECL("AnonymousClassDeclaration"),
        ASSERT("AssertStatement"),
        BLOCK("Block"),
        BREACK("BreakStatement"),
        CONSTRUCTORINV("ConstructorInvocation"),
        CONTINUE("ContinueStatement"),
        DO("DoStatement"),
        EFOR("EnhancedForStatement"),
        FOR("ForStatement"),
        IF("IfStatement"),
        RETURN("ReturnStatement"),
        SCONSTRUCTORINV("SuperConstructorInvocation"),
        SWCASE("SwitchCase"),
        SWSTMT("SwitchStatement"),
        SYNC("SynchronizedStatement"),
        THROW("ThrowStatement"),
        TRY("TryStatement"),
        CATCHCLAUSE("CatchClause"),
        TYPEDECL("TypeDeclarationStatement"),
        VARDECLSTMT("VariableDeclarationStatement"),
        WHILE("WhileStatement"),
        POSTOPERATOR("PostExpression.Operator"),
        INFIXOPERATOR("InfixExpression.Operator"),
        PREFIXOPERATOR("PrefixExpression.Operator"),
        ASSIGNOPERATOR("Assignment.Operator"),
        TYPE("Type"),
        EXPRLST("ExpressionList"),
        UNKNOWN("Unknown");

        private String _name;

        TYPE(String name) {
            _name = name;
        }

        @Override
        public String toString() {
            return _name;
        }
    }

}
