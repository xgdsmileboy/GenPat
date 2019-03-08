/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast;

import mfix.common.util.Pair;
import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.ast.expr.Operator;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.ast.visitor.NodeVisitor;
import mfix.core.node.comp.NodeComparator;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.Vector;
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

    public String getFileName() {
        return _fileName;
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
     *
     * @param nodes : list contains the {@code Node}
     * @return : a list of {@code Node}s after flattening
     */
    public List<Node> flattenTreeNode(List<Node> nodes) {
        nodes.add(this);
        for (Node node : getAllChildren()) {
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
    public List<Expr> getAllChildExpr(List<Expr> nodes, boolean filterName) {
        for (Node node : getAllChildren()) {
            if (node instanceof Expr) {
                if (!filterName || !NodeUtils.isSimpleExpr(node)) {
                    nodes.add((Expr) node);
                }
            }
        }
        for (Node node : getAllChildren()) {
            node.getAllChildExpr(nodes, filterName);
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

    public void setChanged() {
        _changed = true;
    }

    public boolean isConsidered() {
        return _expanded || _changed || _insertDepend || _bindingNode == null;
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
                != _bindingNode.getControldependency()
                || _bindingNode.getControldependency() == null) {
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
//        if (_controldependency != null) {
//            _controldependency.setConsidered(true);
//            nodes.add(_controldependency);
//        }
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
    public abstract boolean genModifications();


    /*********************************************************/
    /**************** pattern abstraction ********************/
    /*********************************************************/
    /**
     * denoting the node is on the path from the root node
     * to the concrete node if it is false. If it is true,
     * all the nodes in the subtree rooted this node is abstract
     * i.e., when it is true, the node can be abstract node as well
     */
    protected boolean _abstract = true;
    // to avoid duplicate object;
    private final static StringBuffer BUFFER = new StringBuffer();
    /**
     * to record the formal form of the pattern
     */
    private transient StringBuffer _formalFormCache = BUFFER;
    private transient boolean _deserialize_tag = false;

    public boolean isAbstract() {
        return _abstract;
    }

    /**
     * abstract node with the given {@code counter}
     * NOTE : this method is designed for API abstraction
     * and will not be used later
     *
     * @param counter : ElementCounter for node abstraction
     */
    public void doAbstraction(ElementCounter counter) {
        for (Node node : getAllChildren()) {
            node.doAbstraction(counter);
        }
    }

    /**
     * abstract node with the given {@code abstraction} object
     * this method performs token level abstraction
     *
     * @param abstraction : the object to abstract node
     */
    public void doAbstractionNew(CodeAbstraction abstraction) {
        if (this instanceof Operator) {
            _abstract = true;
        } else {
            // is the node is simple/leaf node, we perform node abstraction
            // otherwise, whether the node is abstract or not depends on the
            // nodes in the subtree
            if (NodeUtils.isSimpleExpr(this)) {
                _abstract = abstraction.shouldAbstract(this);
            } else {
                _abstract = true;
                for (Node node : getAllChildren()) {
                    node.doAbstractionNew(abstraction);
                    _abstract = _abstract && node.isAbstract();
                }
            }
        }
    }

    public StringBuffer getFormalForm() {
        return _formalFormCache;
    }

    /**
     * this method if used to build the formal form of the pattern node
     * NOTE: this method should be invoked later than the method
     * {@code Node.doAbstractionNew(CodeAbstraction)}, since it depends
     * on the field of {@code _abstract}
     *
     * @param nameMapping      : record the name mapping relation from the concrete expression to its abstract name
     * @param parentConsidered : denotes whether any parent node is considered for pattern matching
     * @param keywords         : a set of keywords that are not abstracted in the formal form
     * @return : the formal form of the pattern with {@code StringBuffer} form
     */
    public StringBuffer formalForm(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (!_deserialize_tag) {
            _deserialize_tag = true;
            _formalFormCache = toFormalForm0(nameMapping, parentConsidered, keywords);
        }
        return _formalFormCache;
    }

    /**
     * the real method that builds pattern formal form
     *
     * @param nameMapping      : record name mapping relation
     * @param parentConsidered : if any parent node is considered for pattern matching
     * @param keywords         : a set of keywords that are not abstracted
     * @return : formal form
     */
    protected abstract StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered,
                                                  Set<String> keywords);

    public Set<Pair<String, Integer>> getUniversalAPIs(boolean isPattern, Set<Pair<String, Integer>> set) {
        Set<MethodInv> mSet = getUniversalAPIs(new HashSet<>(), isPattern);
        for (MethodInv m : mSet) {
            set.add(new Pair<>(m.getName().getName(), m.getArguments().getExpr().size()));
        }
        return set;
    }

    public Set<Pair<Pair<String, Integer>, String>> getUniversalAPIsWithType(boolean isPattern,
                                                                             Set<Pair<Pair<String, Integer>, String>> set) {
        Set<MethodInv> mSet = getUniversalAPIs(new HashSet<>(), isPattern);
        for (MethodInv m : mSet) {
            set.add(new Pair<>(new Pair<>(m.getName().getName(), m.getArguments().getExpr().size()),
                    m.getExpression().getTypeString()));
        }
        return set;
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
    /***************** pattern clustering ********************/
    /*********************************************************/

    private Vector _patternVec;
    protected int _fIndex = -1;

    public Vector getPatternVector() {
        if (_patternVec == null) {
            computePatternVector();
        }
        return _patternVec;
    }

    // TODO : need to rewrite
    private void computePatternVector() {
        _patternVec = new Vector();
        for (Node node : getAllChildren()) {
            _patternVec.or(node.getPatternVector());
        }
        if (!_abstract && _fIndex >= 0) {
            _patternVec.set(_fIndex);
        }
        if (!_modifications.isEmpty()) {
            for (Modification modification : _modifications) {
                _patternVec.set(modification.getFeatureIndex());
            }
        }
    }

    public abstract boolean patternMatch(Node node);


    /*********************************************************/
    /**************** matching buggy code ********************/
    /*********************************************************/

    private Node _buggyBinding;

    public void setBuggyBindingNode(Node node) {
        _buggyBinding = node;
        if (node != null) {
            node._buggyBinding = this;
        }
    }

    public void resetBuggyBinding() {
        _buggyBinding = null;
    }

    public Node getBuggyBindingNode() {
        return _buggyBinding;
    }

    public boolean isBoundBuggy() {
        return _buggyBinding != null;
    }

    public abstract boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings);


    public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
        if (getBindingNode() != null && getBindingNode().getBuggyBindingNode() != null) {
            return getBindingNode().getBuggyBindingNode().toSrcString();
        } else if (exprMap.containsKey(toSrcString().toString())) {
            return new StringBuffer(exprMap.get(toSrcString().toString()));
        }
        return null;
    }

    public abstract StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap);


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
