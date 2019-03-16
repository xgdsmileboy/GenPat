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
import mfix.core.node.ast.expr.ClassInstCreation;
import mfix.core.node.ast.expr.Expr;
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
public class ThrowStmt extends Stmt {

	private Expr _expression = null;
	
	/**
	 * ThrowStatement:
     *	throw Expression ;
	 */
	public ThrowStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public ThrowStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.THROW;
		_fIndex = VIndex.STMT_THROW;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}

	public Expr getExpression() { return _expression; }

	public String getExceptionType(){
		if(_expression instanceof ClassInstCreation){
			return ((ClassInstCreation)_expression).getClassType().toString();
		} else {
			return _expression.getType().toString();
		}
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("throw ");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(";");
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer exp = _expression.formalForm(nameMapping, isConsidered(), keywords);
		if (isConsidered() || exp != null) {
			StringBuffer buffer = new StringBuffer("throw ");
			buffer.append(exp == null ? nameMapping.getExprID(_expression) : exp).append(';');
			return buffer;
		}
		return null;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("throw");
		_tokens.addAll(_expression.tokens());
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_expression);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ThrowStmt) {
			ThrowStmt throwStmt = (ThrowStmt) other;
			match = _expression.compare(throwStmt._expression);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_THROW);
		_fVector.combineFeature(_expression.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		ThrowStmt throwStmt = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			throwStmt = (ThrowStmt) getBindingNode();
			match = (throwStmt == node);
		} else if(canBinding(node)) {
			throwStmt = (ThrowStmt) node;
			setBindingNode(throwStmt);
			match = true;
		}

		if(throwStmt == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(throwStmt.getExpression());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			ThrowStmt throwStmt = (ThrowStmt) getBindingNode();
			if(_expression.getBindingNode() != throwStmt.getExpression()) {
				Update update = new Update(this, _expression, throwStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof ThrowStmt) {
			ThrowStmt throwStmt = (ThrowStmt) node;
			return _expression.ifMatch(throwStmt.getExpression(), matchedNode, matchedStrings)
					&& super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer();
			stringBuffer.append("throw ");
			StringBuffer tmp = _expression.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(";");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
		StringBuffer expression = null;
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			ThrowStmt throwStmt = (ThrowStmt) pnode;
			for (Modification modification : throwStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == throwStmt._expression) {
						expression = update.apply(vars, exprMap);
						if (expression == null) return null;
					} else {
						LevelLogger.error("ThrowStmt ERROR");
					}
				} else {
					LevelLogger.error("@ThrowStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("throw ");
		if (expression == null) {
			StringBuffer tmp = _expression.adaptModifications(vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
}
