/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node;

import mfix.core.comp.Modification;
import mfix.core.comp.NodeComparator;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.expr.Operator;
import mfix.core.parse.node.expr.SName;
import mfix.core.parse.node.stmt.Stmt;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public abstract class Node implements NodeComparator, Serializable {

    private static final long serialVersionUID = -6995771051040337618L;
    protected String _fileName;
	protected int _startLine;
	protected int _endLine;
	protected Node _parent;
    protected TYPE _nodeType = TYPE.UNKNOWN;
	protected ASTNode _oriNode;
	protected FVector _fVector = null;

	protected transient LinkedList<String> _tokens = null;
	protected transient boolean _matchNodeType = false;
	protected transient Node _tarNode = null;
	protected transient List<Modification> _modifications = new LinkedList<>();
	protected transient Map<String, Set<Node>> _keywords = null;
	protected transient boolean _keyPoint = false;
	protected transient Node _binding = null;

	public Node(String fileName, int startLine, int endLine, ASTNode oriNode) {
		this(fileName, startLine, endLine, oriNode, null);
	}
	
	public int getStartLine() {
		return _startLine;
	}
	
	public int getEndLine() {
		return _endLine;
	}
	
	public Node(String fileName, int startLine, int endLine, ASTNode oriNode, Node parent) {
		_fileName = fileName;
	    _startLine = startLine;
		_endLine = endLine;
		_oriNode = oriNode;
		_parent = parent;
	}
	
	/**
	 * @param parent the _parent to set
	 */
	public void setParent(Node parent) {
		this._parent = parent;
	}
	
	public Node getParent() {
		return _parent;
	}
	
	public void setBinding(Node binding) {
		this._binding = binding;
	}
	
	public Node getBinding() {
		return _binding;
	}
	
	public void resetBinding(){
		if(_binding != null) {
			_binding.setBinding(null);
		}
		_binding = null;
		for(Node node : getAllChildren()) {
			node.resetBinding();
		}
	}
	
	public boolean isKeyPoint() {
		return _keyPoint;
	}
	
	public void setAsKeyPointBottomUp(boolean keyPoint) {
		_keyPoint = keyPoint;
		if(_parent != null) {
			_parent.setAsKeyPointBottomUp(keyPoint);
		}
	}
	
	public void setAsKeyPointTopDown(boolean keyPoint) {
		_keyPoint = keyPoint;
		for(Node node : getAllChildren()) {
			node.setAsKeyPointTopDown(keyPoint);
		}
	}
	
	public boolean isNodeTypeMatch() {
		return _matchNodeType;
	}
	
	public boolean isAllBinding(boolean ignoreOprator) {
		if(_binding == null) return false;
		for(Node child : getAllChildren()) {
			if(!child.isAllBinding(ignoreOprator)) {
				return false;
			} else if(!ignoreOprator && child instanceof Operator) {
				if(!child.toSrcString().toString().equals(child.getBinding().toSrcString().toString())) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * @param nodeType the _nodeType to set
	 */
	public void setNodeType(TYPE nodeType) {
		this._nodeType = nodeType;
	}
	
	public TYPE getNodeType() {
		return _nodeType;
	}
	
	public FVector getFeatureVector(){
		if(_fVector == null){
			computeFeatureVector();
		}
		return _fVector;
	}

	public List<String> tokens() {
		if(_tokens == null) {
			tokenize();
		}
		return _tokens;
	}

	public List<Modification> getNodeModification() {
		return _modifications;
	}
	
	public Map<String, String> extractExprMapping(){
		Map<String, String> map = new HashMap<>();
		if (isKeyPoint() && _binding != null && !(this instanceof Stmt) && !(this instanceof mfix.core.parse.node.MethDecl)) {
			String key = toSrcString().toString();
			String tar = _binding.toSrcString().toString();
			if (!key.isEmpty() && !tar.isEmpty()) {
				map.put(key, tar);
			}
		}
		for(Node node : getAllChildren()) {
			map.putAll(node.extractExprMapping());
		}
		return map;
	}
	
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if(this instanceof SName) {
			set.add((SName) this);
		}
		for(Node node : getAllChildren()) {
			set.addAll(node.getAllVars());
		}
		return set;
	}
	
	public Set<String> getNewVars() {
		return new HashSet<>();
	}
	
	public abstract StringBuffer toSrcString();
	public abstract Stmt getParentStmt();
	public abstract List<Stmt> getChildren();
	public abstract List<Node> getAllChildren();
	public abstract void computeFeatureVector();
	public abstract void resetAllNodeTypeMatch();
	public abstract void setAllNodeTypeMatch();
	public abstract void deepMatch(Node other);
	public abstract StringBuffer printMatchSketch();
	public abstract boolean matchSketch(Node sketch);
	public abstract boolean bindingSketch(Node sketch);
	public abstract StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars);
	public abstract StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars);
	public abstract List<Modification> extractModifications();
	public abstract Map<String, Set<Node>> getCalledMethods();

	protected abstract void tokenize();

	protected void avoidDuplicate(Map<String, Set<Node>> map, Node node) {
		if(node != null) {
			Set<Node> set = null;
			for(Entry<String, Set<Node>> entry : node.getCalledMethods().entrySet()) {
				set = map.get(entry.getKey());
				if(set == null) {
					set = new HashSet<>();
				}
				set.addAll(entry.getValue());
				map.put(entry.getKey(), set);
			}
		}
	}
	
	
	@Override
	public String toString(){
		return toSrcString().toString();
	}
	
	public static enum TYPE{
		
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
		UNKNOWN("Unknown");
		
		private String _name = null;
		private TYPE(String name){
			_name = name;
		}
		
		@Override
		public String toString() {
			return _name;
		}
	}

}
