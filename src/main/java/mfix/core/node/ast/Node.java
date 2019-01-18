/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast;

import mfix.common.util.Utils;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.comp.NodeComparator;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Movement;
import mfix.core.node.modify.Update;
import mfix.core.pattern.relation.Relation;
import mfix.core.stats.element.ElementCounter;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
     * @param fileName  : source file name (with absolute path)
     * @param startLine : start line number of the node in the original source file
     * @param endLine   : end line number of the node in the original source file
     * @param oriNode   : original abstract syntax tree node in the JDT model
     */
    public Node(String fileName, int startLine, int endLine, ASTNode oriNode) {
        this(fileName, startLine, endLine, oriNode, null);
    }

    /**
     * @param fileName  : source file name (with absolute path)
     * @param startLine : start line number of the node in the original source file
     * @param endLine   : end line number of the node in the original source file
     * @param oriNode   : original abstract syntax tree node in the JDT model
     * @param parent    : parent node in the abstract syntax tree
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
     *
     * @return : line number
     */
    public int getStartLine() {
        return _startLine;
    }

    /**
     * get the end line number of node in the original source file
     *
     * @return : line number
     */
    public int getEndLine() {
        return _endLine;
    }

    /**
     * set current node type, {@code Node.TYPE.UNKNOWN} as default
     *
     * @param nodeType : node type
     */
    public void setNodeType(TYPE nodeType) {
        this._nodeType = nodeType;
    }

    /**
     * get node type (see {@code Node.TYPE})
     *
     * @return : current node type
     */
    public TYPE getNodeType() {
        return _nodeType;
    }

    /**
     * set the parent node in the abstract syntax tree
     *
     * @param parent : parent node
     */
    public void setParent(Node parent) {
        this._parent = parent;
    }

    /**
     * get parent node in the abstract syntax tree
     *
     * @return : parent node
     */
    public Node getParent() {
        return _parent;
    }

    /**
     * set data dependency of node
     *
     * @param dependency : dependent node, can be {@code null}
     */
    public void setDataDependency(Node dependency) {
        _datadependency = dependency;
    }

    /**
     * get data dependency
     *
     * @return : data dependent node, can be {@code null}
     */
    public Node getDataDependency() {
        return _datadependency;
    }

    public Set<Node> recursivelyGetDataDependency(Set<Node> nodes) {
        if (_datadependency != null) {
            nodes.add(_datadependency);
        }
        for (Node node : getAllChildren()) {
            node.recursivelyGetDataDependency(nodes);
        }
        return nodes;
    }

    /**
     * set control dependency of node
     *
     * @param dependency : dependent node, can be {@code null}
     */
    public void setControldependency(Node dependency) {
        _controldependency = dependency;
    }

    /**
     * get control dependency
     *
     * @return
     */
    public Node getControldependency() {
        if (getParentStmt() == null) return null;
        return getParentStmt()._controldependency;
    }

    public void setPreUsed(Node node) {
        _preUseChain = node;
        if (node != null) {
            node.setNextUsed(this);
        }
    }

    public Node getPreUsed() {
        return _preUseChain;
    }

    public void setNextUsed(Node node) {
        _nextUseChain = node;
        if (node != null) {
            node.setPreUsed(this);
        }
    }

    public Node getNextUsed() {
        return _nextUseChain;
    }

    /**
     * traverse the complete sub-tree with the given {@code visitor}
     *
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
     *
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
     *
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
     *
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
     *
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

    public boolean isParentOf(Node node) {
        while (node != null) {
            if (node.getParent() == this) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    public boolean isDataDependOn(Node node) {
        return getDataDependency() == node;
    }

    public boolean isControlDependOn(Node node) {
        return getControldependency() == node;
    }

    /**
     * flatten abstract tree as a list of {@code Node}
     * @param nodes : list contains the {@code Node}
     * @return : a list of {@code Node}s after flattening
     */
    public List<Node> flattenTreeNode(List<Node> nodes) {
        nodes.add(this);
        for(Node node : getAllChildren()) {
            node.flattenTreeNode(nodes);
        }
        return nodes;
    }

    /**
     * recursively get all child {@code Stmt} node
     *
     * @param nodes : a list of child {@code Stmt} node
     * @return : a list of child {@code Stmt} node
     */
    public List<Stmt> getAllChildStmt(List<Stmt> nodes) {
        for (Node node : getAllChildren()) {
            if (node instanceof Stmt) {
                nodes.add((Stmt) node);
                node.getAllChildStmt(nodes);
            }
        }
        return nodes;
    }

    /**
     * recursively get all child {@code Expr} node
     *
     * @param nodes : a list of child {@code Expr} node
     * @return : a list of child {@code Expr} node
     */
    public List<Expr> getAllChildExpr(List<Expr> nodes) {
        for (Node node : getAllChildren()) {
            if (node instanceof Expr) {
                nodes.add((Expr) node);
            }
        }
        for (Node node : getAllChildren()) {
            node.getAllChildExpr(nodes);
        }
        return nodes;
    }

    /**
     * output source code with string format
     *
     * @return : source code string
     */
    public abstract StringBuffer toSrcString();

    /**
     * get (non-direct) parent node that is {@code Stmt} type, maybe itself
     *
     * @return : parent node if exist, otherwise {@code null}
     */
    public abstract Stmt getParentStmt();

    /**
     * get all {@code Stmt} child node, does not include itself
     * NOTE: empty for all {@code Expr} node
     *
     * @return : all child statement
     */
    public abstract List<Stmt> getChildren();

    /**
     * return all child node, does not include itself
     *
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

    @Override
    public String toString() {
        return toSrcString().toString();
    }

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
     *
     */
    private boolean _insertDepend = false;
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

    public void setInsertDepend(boolean insertDepend) {
        _insertDepend = insertDepend;
    }

    public void setBindingNode(Node binding) {
        _bindingNode = binding;
        if (_bindingNode != null) {
            binding._bindingNode = this;
        }
    }

    public Node getBindingNode() {
        return _bindingNode;
    }

    public List<Modification> getModifications() {
        return _modifications;
    }

    /**
     * obtain the considered node patterns
     * i.e., all nodes considered based on the data/control dependency
     * and the structure information (children and parent)
     *
     * @param nodes           : all nodes to be considered
     * @param includeExpanded : tag whether consider the expanded node
     * @return : a set of nodes
     */
    public Set<Node> getConsideredNodesRec(Set<Node> nodes, boolean includeExpanded) {
        if (_bindingNode == null) {
            nodes.add(this);
        } else {
            if ((includeExpanded && _expanded) || _changed || _insertDepend
                    || dataDependencyChanged() || controlDependencyChanged()) {
                nodes.add(this);
            }
        }

        for (Node node : getAllChildren()) {
            node.getConsideredNodesRec(nodes, includeExpanded);
        }
        return nodes;
    }

    private boolean dataDependencyChanged() {
        if (getDataDependency() == null) {
            if (_bindingNode.getDataDependency() != null) {
                return true;
            }
        } else if (getDataDependency().getBindingNode()
                != _bindingNode.getDataDependency()) {
            return true;
        }
        return false;
    }

    private boolean controlDependencyChanged() {
        if (getControldependency() == null) {
            if (_bindingNode.getControldependency() != null) {
                return true;
            }
        } else if (getControldependency().getBindingNode()
                != _bindingNode.getControldependency()) {
            return true;
        }
        return false;
    }

    /**
     * expand node considered for match
     *
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
     *
     * @param nodes : considered node set
     */
    private void expandDependency(Set<Node> nodes) {
        if (_datadependency != null) {
            _datadependency.setConsidered(true);
            nodes.add(_datadependency);
        }
        if (_controldependency != null) {
            _controldependency.setConsidered(true);
            nodes.add(_controldependency);
        }
    }

    /**
     * expand children based on syntax
     *
     * @param nodes : considered node set
     */
    private void expandTopDown(Set<Node> nodes) {
        for (Node node : getAllChildren()) {
            node.setConsidered(true);
        }
        nodes.addAll(getAllChildren());
    }

    /**
     * expand parent based on syntax
     *
     * @param nodes : considered node set
     */
    private void expandBottomUp(Set<Node> nodes) {
        if (_parent != null) {
            _parent.setConsidered(true);
            nodes.add(_parent);
        }
    }

    /**
     * judging whether the given {@code node} is compatible or not
     *
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
        for (Node node : getAllChildren()) {
            node.postAccurateMatch(null);
        }
    }

    /**
     * match two list of nodes greedily
     *
     * @param lst1 : first list
     * @param lst2 : second list
     */
    protected void greedyMatchListNode(List<? extends Node> lst1, List<? extends Node> lst2) {
        Set<Node> set = new HashSet<>();
        for (Node node : lst1) {
            for (Node other : lst2) {
                if (!set.contains(other) && node.postAccurateMatch(other)) {
                    set.add(other);
                }
            }
        }
    }

    /**
     * return all modifications bound to the ast node
     *
     * @param modifications : a set of to preserve the modifications
     * @return : a set of modifications
     */
    public Set<Modification> getAllModifications(Set<Modification> modifications) {
        modifications.addAll(_modifications);
        for (Node node : getAllChildren()) {
            node.getAllModifications(modifications);
        }
        return modifications;
    }

    /**
     * match node after constraint solving
     *
     * @param node : node to match
     * @return : {@code true} is current node matches {@code node}, otherwise {@code false}
     */
    public abstract boolean postAccurateMatch(Node node);

    /**
     * based on the matching result, generate modifications
     */
    public abstract boolean genModidications();

    /**
     * match two list ast nodes and generate modifications
     *
     * @param src  : a list of source nodes
     * @param tar  : a list of target nodes
     * @param move : permit move operation
     */
    protected void genModificationList(List<? extends Node> src, List<? extends Node> tar, boolean move) {
        Set<Integer> set = new HashSet<>();
        List<Deletion> deletions = new LinkedList<>();
        List<Insertion> insertions = new LinkedList<>();
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
                Deletion deletion = new Deletion(this, src.get(i), i);
                deletions.add(deletion);
            }
        }
        for (int i = 0; i < tar.size(); i++) {
            if (set.contains(i)) continue;
            Insertion insertion = new Insertion(this, i, tar.get(i));
            insertions.add(insertion);
        }

        Set<Integer> matched = new HashSet<>();
        Insertion insertion;
        Update update;
        for (Deletion d : deletions) {
            Node binding = d.getDelNode().getBindingNode();
            update = null;
            if (binding != null) {
                for (int i = 0; i < insertions.size(); i++) {
                    if (matched.contains(i)) {
                        continue;
                    }
                    insertion = insertions.get(i);
                    if (d.getIndex() == insertion.getIndex() || binding.isParentOf(insertion.getInsertedNode())
                            || insertion.getInsertedNode().isParentOf(binding)) {
                        matched.add(i);
                        update = new Update(d.getParent(), d.getDelNode(), insertion.getInsertedNode());
                    }
                }
            }
            if (update == null) {
                _modifications.add(d);
            } else {
                _modifications.add(update);
            }
        }
        for (int i = 0; i < insertions.size(); i++) {
            if (matched.contains(i)) {
                continue;
            }
            Insertion ins = insertions.get(i);
            Set<Node> nodes = ins.getInsertedNode().recursivelyGetDataDependency(new HashSet<>());
            if (nodes.isEmpty()) {
                Node insertNode = ins.getInsertedNode();
                Node parent = insertNode.getParent();
                List<Node> children = parent.getAllChildren();
                for (int index = 0; index < children.size(); index ++) {
                    if (insertNode == children.get(index)) {
                        if (index > 0) {
                            children.get(index - 1).setInsertDepend(true);
                            ins.setPrenode(children.get(index - 1));
                        }
                        if (index < children.size() - 1) {
                            children.get(index + 1).setInsertDepend(true);
                            ins.setNextnode(children.get(index + 1));
                        }
                    }
                }
            } else {
                for (Node node : nodes) {
                    if (node.getBindingNode() != null) {
                        node.getBindingNode().setInsertDepend(true);
                    }
                }
            }
            _modifications.add(ins);
        }

    }


    /*********************************************************/
    /*********** interaction with relation model *************/
    /*********************************************************/
    // TODO: this part can be removed if use greedy matching
    // process rather than the MaxSolver

    private Relation _RelationBinding;

    public void setBindingRelation(Relation r) {
        _RelationBinding = r;
    }

    public Relation getBindingRelation() {
        return _RelationBinding;
    }

    /*********************************************************/
    /**************** pattern abstraction ********************/
    /*********************************************************/
    protected boolean _abstract = true;

    public boolean isAbstract() {
        return _abstract;
    }

    public void doAbstraction(ElementCounter counter) {
        for (Node node : getAllChildren()) {
            node.doAbstraction(counter);
        }
    }

    public Set<MethodInv> getUniversalAPIs(Set<MethodInv> set, boolean isPattern) {
        if (!isPattern || (isConsidered() && !isAbstract())) {
            if (this instanceof MethodInv) {
                set.add((MethodInv) this);
            }
        }
        for (Node node : getAllChildren()) {
            node.getUniversalAPIs(set, isPattern);
        }
        return set;
    }

    /*********************************************************/
    /**************** matching buggy code ********************/
    /*********************************************************/

    private Node _buggyBinding;

    public void setBuggyBindingNode(Node node) {
        _buggyBinding = node;
        if(node != null) {
            node._buggyBinding = this;
        }
    }

    public Node getBuggyBindingNode() {
        return _buggyBinding;
    }

    public boolean isBoundBuggy(){
        return _buggyBinding != null;
    }

    public abstract boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings);

    protected boolean checkDependency(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        if(getDataDependency() != null && node.getDataDependency() != null) {
            if(getDataDependency().ifMatch(node.getDataDependency(), matchedNode, matchedStrings)) {
                return true;
            }
            return false;
        }
        return true;
    }

    protected boolean matchSameNodeType(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        if(Utils.checkCompatiblePut(toString(), node.toString(), matchedStrings)) {
            matchedNode.put(this, node);
            return true;
        }
        return false;
    }

    public StringBuffer transfer() {
        if (getBindingNode() != null && getBindingNode().getBuggyBindingNode() != null) {
            return getBindingNode().getBuggyBindingNode().toSrcString();
        }
        return null;
    }

    public Node checkModification() {
        if (getBuggyBindingNode() != null && !getBuggyBindingNode().getModifications().isEmpty()) {
            return getBuggyBindingNode();
        }
        return null;
    }

    public abstract StringBuffer adaptModifications();


    /******************************************************************************************/
    /********************* The following are node type model **********************************/
    /******************************************************************************************/

    //all types of abstract syntax tree node considered currently
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
