/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.Svd;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
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
public class EnhancedForStmt extends Stmt {

	private static final long serialVersionUID = 8332915530003880205L;
	private Svd _varDecl = null;
	private Expr _expression = null;
	private Stmt _statement = null;
	
	
	/**
	 * EnhancedForStatement:
     *	for ( FormalParameter : Expression )
     *	                   Statement
	 */
	public EnhancedForStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public EnhancedForStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.EFOR;
		_fIndex = VIndex.STMT_ENHANCEDFOR;
	}
	
	public void setParameter(Svd varDecl){
		_varDecl = varDecl;
	}
	
	public Svd getParameter() {
		return _varDecl;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression() {
		return _expression;
	}
	
	public void setBody(Stmt statement){
		_statement = statement;
	}
	
	public Stmt getBody() {
		return _statement;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("for(");
		stringBuffer.append(_varDecl.toSrcString());
		stringBuffer.append(" : ");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_statement.toSrcString());
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract()) return null;
		StringBuffer var = _varDecl.formalForm(nameMapping, isConsidered(), keywords);
		StringBuffer exp = _expression.formalForm(nameMapping, isConsidered(), keywords);
		StringBuffer body = _statement.formalForm(nameMapping, false, keywords);
		if (var == null && exp == null && body == null) {
			if (isConsidered()) {
				return new StringBuffer("for(")
						.append(nameMapping.getTypeID(_varDecl.getDeclType()))
						.append(' ')
						.append(nameMapping.getExprID(_varDecl.getName()))
						.append(nameMapping.getExprID(_expression))
						.append("){}");
			} else {
				return null;
			}
		}
		StringBuffer buffer = new StringBuffer("for(");
		if (var == null) {
			buffer.append(nameMapping.getTypeID(_varDecl.getDeclType())).append(' ');
			buffer.append(nameMapping.getExprID(_varDecl.getName()));
		} else {
			buffer.append(var);
		}
		buffer.append(exp == null ? nameMapping.getExprID(_expression) : exp);
		body.append(')');
		buffer.append(body == null ? "{}" : body);
		return buffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("for");
		_tokens.add("(");
		_tokens.addAll(_varDecl.tokens());
		_tokens.add(":");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.addAll(_statement.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_varDecl);
		children.add(_expression);
		children.add(_statement);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_statement);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof EnhancedForStmt) {
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) other;
			match = _varDecl.compare(enhancedForStmt._varDecl) && _expression.compare(enhancedForStmt._expression) && _statement.compare(enhancedForStmt._statement);
		}
		return match;
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_ENFOR);
		_fVector.combineFeature(_varDecl.getFeatureVector());
		_fVector.combineFeature(_expression.getFeatureVector());
		_fVector.combineFeature(_statement.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		EnhancedForStmt enhancedForStmt = null;
		if(getBindingNode() != null) {
			enhancedForStmt = (EnhancedForStmt) getBindingNode();
			_expression.postAccurateMatch(enhancedForStmt.getExpression());
			match = (enhancedForStmt == node);
		} else if(canBinding(node)) {
			enhancedForStmt = (EnhancedForStmt) node;
			if(_expression.postAccurateMatch(enhancedForStmt.getExpression())) {
				setBindingNode(node);
				match = true;
			} else {
				enhancedForStmt = null;
			}
		}

		if(enhancedForStmt == null) {
			continueTopDownMatchNull();
		} else {
			_varDecl.postAccurateMatch(enhancedForStmt.getParameter());
			_statement.postAccurateMatch(enhancedForStmt.getBody());
		}

		return match;
	}

	@Override
	public boolean genModifications() {
		if(super.genModifications()) {
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) getBindingNode();
			if(_varDecl.getBindingNode() != enhancedForStmt.getParameter()) {
				Update update = new Update(this, _varDecl, enhancedForStmt.getParameter());
				_modifications.add(update);
			} else {
				_varDecl.genModifications();
			}
			if(_expression.getBindingNode() != enhancedForStmt.getExpression()) {
				Update update = new Update(this, _expression, enhancedForStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			_statement.genModifications();
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if(node instanceof EnhancedForStmt) {
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) node;
			return _varDecl.ifMatch(enhancedForStmt.getParameter(), matchedNode, matchedStrings)
					&& _expression.ifMatch(enhancedForStmt.getExpression(), matchedNode, matchedStrings)
					&& _statement.ifMatch(enhancedForStmt.getBody(), matchedNode, matchedStrings)
					&& super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			StringBuffer tmp;
			stringBuffer.append("for(");
			tmp = _varDecl.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(" : ");
			tmp = _expression.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(")");
			tmp = _statement.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer varDecl = null;
		StringBuffer expression = null;
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			EnhancedForStmt enhancedForStmt = (EnhancedForStmt) pnode;
			for (Modification modification : enhancedForStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if (node == enhancedForStmt._varDecl) {
						varDecl = update.apply(vars, exprMap);
						if (varDecl == null) return null;
					} else if (node == enhancedForStmt._expression) {
						expression = update.apply(vars, exprMap);
						if (expression == null) return null;
					}
				} else {
					LevelLogger.error("@EnhancedForStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp;
		stringBuffer.append("for(");
		if (varDecl == null) {
			tmp = _varDecl.adaptModifications(vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(varDecl);
		}
		stringBuffer.append(" : ");
		if (expression == null) {
			tmp = _expression.adaptModifications(vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(")");
		tmp = _statement.adaptModifications(vars, exprMap);
		if (tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
}
