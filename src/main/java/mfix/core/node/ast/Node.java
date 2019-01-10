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
import mfix.core.node.modify.Modification;
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
    private Node _controldependency;

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
        return _controldependency;
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
    private Node _bindingNode;
    private boolean _considered = false;
    protected List<Modification> _modifications;

    public boolean isConsidered() {
        return _considered || _bindingNode == null;
    }

    public void setConsidered(boolean considered) {
        _considered = considered;
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

    public Set<Node> getConsideredNodesRec(Set<Node> nodes, boolean includeExpanded) {
        if((includeExpanded && _considered) || _bindingNode == null) {
            nodes.add(this);
        }
        for(Node node : getAllChildren()) {
            node.getConsideredNodesRec(nodes, includeExpanded);
        }
        return nodes;
    }

    public Set<Node> expand(Set<Node> nodes) {
        expandDependency(nodes);
        expandBottomUp(nodes);
        expandTopDown(nodes);
        return nodes;
    }

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

    private void expandTopDown(Set<Node> nodes) {
        for(Node node : getAllChildren()) {
            node.setConsidered(true);
        }
        nodes.addAll(getAllChildren());
    }

    private void expandBottomUp(Set<Node> nodes) {
        if(_parent != null) {
            _parent.setConsidered(true);
            nodes.add(_parent);
        }
    }

    protected boolean canBinding(Node node) {
        return node != null && node.getNodeType() == _nodeType && node.getBindingNode() == null;
    }

    protected void continueTopDownMatchNull() {
        for(Node node : getAllChildren()) {
            node.postAccurateMatch(null);
        }
    }

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

    public abstract boolean postAccurateMatch(Node node);


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
