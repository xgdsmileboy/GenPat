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
public class WhileStmt extends Stmt {

	private static final long serialVersionUID = -5763865331484587703L;
	private Expr _expression = null;
	private Stmt _body = null;
	
	/**
	 * WhileStatement:
     *	while ( Expression ) Statement
	 */
	public WhileStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public WhileStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.WHILE;
		_fIndex = VIndex.STMT_WHILE;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression() {
		return _expression;
	}
	
	public void setBody(Stmt body){
		_body = body;
	}
	
	public Stmt getBody() {
		return _body;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("while(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_body.toSrcString());
		return stringBuffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer exp = _expression.formalForm(nameMapping, isConsidered(), keywords);
		StringBuffer body = _body.formalForm(nameMapping, false, keywords);
		if (isConsidered() || exp != null || body != null) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("while(")
					.append(exp == null ? nameMapping.getExprID(_expression) : exp)
					.append(')')
					.append(body == null ? "{}" : body);
			return buffer;
		}
		return null;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("while");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.addAll(_body.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_body);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_body);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof WhileStmt) {
			WhileStmt whileStmt = (WhileStmt) other;
			match = _expression.compare(whileStmt._expression) && _body.compare(whileStmt._body);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.KEY_WHILE);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.KEY_WHILE);
		_completeFVector.combineFeature(_expression.getFeatureVector());
		_completeFVector.combineFeature(_body.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		WhileStmt whileStmt = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			whileStmt = (WhileStmt) getBindingNode();
			match = (whileStmt == node);
		} else if(canBinding(node)) {
			whileStmt = (WhileStmt) node;
			setBindingNode(node);
			match = true;
		}
		if(whileStmt == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(whileStmt.getExpression());
			_body.postAccurateMatch(whileStmt.getBody());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if(super.genModifications()) {
			WhileStmt whileStmt = (WhileStmt) getBindingNode();
			if (_expression.getBindingNode() != whileStmt.getExpression()) {
				Update update = new Update(this, _expression, whileStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			_body.genModifications();
			return true;
		}
		return false;
	}

	@Override
	public void greedyMatchBinding(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof WhileStmt) {
			WhileStmt whileStmt = (WhileStmt) node;
			if (NodeUtils.matchSameNodeType(getExpression(), whileStmt.getExpression(), matchedNode, matchedStrings)
					&& NodeUtils.matchSameNodeType(getBody(), whileStmt.getBody(), matchedNode, matchedStrings)) {
				getExpression().greedyMatchBinding(whileStmt.getExpression(), matchedNode, matchedStrings);
			}
		}
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
		if (node instanceof WhileStmt) {
			WhileStmt whileStmt = (WhileStmt) node;
			return _expression.ifMatch(whileStmt.getExpression(), matchedNode, matchedStrings, level)
					&& _body.ifMatch(whileStmt.getBody(), matchedNode, matchedStrings, level)
					&& super.ifMatch(node, matchedNode, matchedStrings, level);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer("while(");
			StringBuffer tmp = _expression.transfer(vars, exprMap, retType, exceptions);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(")");
			tmp = _body.transfer(vars, exprMap, retType, exceptions);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions) {
		StringBuffer expression = null;
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			WhileStmt whileStmt = (WhileStmt) pnode;
			for (Modification modification : whileStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == whileStmt._expression) {
						expression = update.apply(vars, exprMap, retType, exceptions);
						if (expression == null) return null;
					}
				} else {
					LevelLogger.error("WhileStmt Should not be this kind of modification : " + modification);
				}
			}
		}

		StringBuffer stringBuffer = new StringBuffer("while(");
		StringBuffer tmp;
		if (expression == null) {
			tmp = _expression.adaptModifications(vars, exprMap, retType, exceptions);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append(")");
		tmp = _body.adaptModifications(vars, exprMap, retType, exceptions);
		if (tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
}

