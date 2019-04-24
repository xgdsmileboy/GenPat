/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.ExprList;
import mfix.core.node.ast.expr.MType;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SuperConstructorInv extends Stmt {

	private static final long serialVersionUID = -6063679105312839664L;
	private Expr _expression = null;
	private MType _superType = null;
	private ExprList _arguments = null;
	
	/**
	 * SuperConstructorInvocation:
     *	[ Expression . ]
     *	    [ < Type { , Type } > ]
     *	    super ( [ Expression { , Expression } ] ) ;
	 */
	public SuperConstructorInv(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public SuperConstructorInv(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SCONSTRUCTORINV;
		_fIndex = VIndex.STMT_SUPER_CONSTRUCTOR;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setSuperType(MType type){
		_superType = type;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}

	public Expr getExpression() { return _expression; }

	public MType getSuperType() { return _superType; }

	public ExprList getArgument() { return _arguments; }

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_expression != null){
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(");");
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer exp = _expression == null ? null : _expression.formalForm(nameMapping, isConsidered(), keywords);
		StringBuffer arg = _arguments.formalForm(nameMapping, isConsidered(), keywords);
		if (isConsidered() || exp != null || arg == null) {
			StringBuffer buffer = new StringBuffer();
			if (_expression != null) {
				buffer.append(exp == null ? nameMapping.getExprID(_expression) : exp).append('.');
			}
			buffer.append("super(").append(arg == null ? "" : arg).append(");");
			return buffer;
		}
		return null;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_expression != null){
			_tokens.addAll(_expression.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add("(");
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		if(_expression != null) {
			children.add(_expression);
		}
		children.add(_arguments);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof SuperConstructorInv) {
			SuperConstructorInv superConstructorInv = (SuperConstructorInv) other;
			if(_expression == null) {
				match = (superConstructorInv._expression == null);
			} else {
				match = _expression.compare(superConstructorInv._expression);
			}
			
//			if(_superType == null) {
//				match = match && (superConstructorInv._superType == null);
//			} else {
//				match = match && _superType.toString().equals(superConstructorInv._superType.toString());
//			}
			match = match && _arguments.compare(superConstructorInv._arguments);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
        _selfFVector = new FVector();
        _selfFVector.inc(FVector.KEY_SUPER);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.KEY_SUPER);
		if(_expression != null) {
			_completeFVector.combineFeature(_expression.getFeatureVector());
		}
		if(_superType != null) {
			_completeFVector.combineFeature(_superType.getFeatureVector());
		}
		_completeFVector.combineFeature(_arguments.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		SuperConstructorInv superConstructorInv = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			superConstructorInv = (SuperConstructorInv) getBindingNode();
			match = (superConstructorInv == node);
		} else if(canBinding(node)) {
			superConstructorInv = (SuperConstructorInv) node;
			setBindingNode(superConstructorInv);
			match = true;
		}

		if(superConstructorInv == null) {
			continueTopDownMatchNull();
		} else {
			if(_expression != null) {
				_expression.postAccurateMatch(superConstructorInv.getArgument());
			}
			_arguments.postAccurateMatch(superConstructorInv.getArgument());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if(super.genModifications()) {
			SuperConstructorInv superConstructorInv = (SuperConstructorInv) getBindingNode();
			if(_expression == null) {
				if(superConstructorInv.getExpression() != null) {
					Update update = new Update(this, _expression, superConstructorInv.getExpression());
					_modifications.add(update);
				}
			} else if(_expression.getBindingNode() != superConstructorInv.getExpression()) {
				Update update = new Update(this, _expression, superConstructorInv.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			if(_superType != null) {
				if(superConstructorInv.getSuperType() == null || !_superType.compare(superConstructorInv.getSuperType())) {
					Update update = new Update(this, _superType, superConstructorInv.getSuperType());
					_modifications.add(update);
				}
			}
			if(_arguments.getBindingNode() != superConstructorInv.getArgument()) {
				Update update = new Update(this, _arguments, superConstructorInv.getArgument());
				_modifications.add(update);
			} else {
				_arguments.genModifications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
		if (node instanceof SuperConstructorInv) {
			SuperConstructorInv superConstructorInv = (SuperConstructorInv) node;
			boolean match = true;
			if (_expression != null && superConstructorInv.getExpression() != null) {
				match = match && _expression.ifMatch(superConstructorInv.getExpression(),
						matchedNode, matchedStrings, level);
			}
			match = match && _arguments.ifMatch(superConstructorInv.getArgument(), matchedNode, matchedStrings, level);
			return match && super.ifMatch(node, matchedNode, matchedStrings, level);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			if(_expression != null){
				tmp = _expression.transfer(vars, exprMap, retType, exceptions);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
			stringBuffer.append("super(");
			tmp = _arguments.transfer(vars, exprMap, retType, exceptions);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(");");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions) {
		StringBuffer expression = null;
		StringBuffer superType = null;
		StringBuffer argument = null;
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			SuperConstructorInv superConstructorInv = (SuperConstructorInv) pnode;
			for(Modification modification : superConstructorInv.getModifications()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == superConstructorInv._expression) {
						expression = update.apply(vars, exprMap, retType, exceptions);
						if(expression == null) return null;
					} else if (update.getSrcNode() == superConstructorInv._superType) {
						superType = update.apply(vars, exprMap, retType, exceptions);
						if (superType == null) return null;
					} else {
						argument = update.apply(vars, exprMap, retType, exceptions);
						if(argument == null) return null;
					}
				} else {
					LevelLogger.error("@SuperConstructorInv Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp ;
		if(expression == null) {
			if(_expression != null){
				tmp = _expression.adaptModifications(vars, exprMap, retType, exceptions);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append("super(");
		if(argument == null) {
			tmp = _arguments.adaptModifications(vars, exprMap, retType, exceptions);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(argument);
		}
		stringBuffer.append(");");
		return stringBuffer;
	}
}
