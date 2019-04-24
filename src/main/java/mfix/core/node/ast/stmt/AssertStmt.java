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
public class AssertStmt extends Stmt {

	private static final long serialVersionUID = 8494694375316529776L;
	private Expr _expresion;
	private Expr _message;
	// TODO: assert statement should be added
	/**
	 * AssertStatement:
     *	assert Expression [ : Expression ] ;
	 */
	public AssertStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public AssertStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.ASSERT;
		_fIndex = VIndex.STMT_ASSERT;
	}

	public void setExpression(Expr expression) {
		_expresion = expression;
	}

	public Expr getExpression() {
		return _expresion;
	}

	public void setMessage(Expr message) {
		_message = message;
	}

	public Expr getMessage() {
		return _message;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("assert ")
				.append(_expresion.toSrcString());
		if (_message != null) {
			buffer.append(":").append(_message.toSrcString());
		}
		buffer.append(";");
		return buffer;
	}

	@Override
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer expression = _expresion.formalForm(nameMapping, isConsidered(), keywords);
		StringBuffer message = _message == null ? null : _message.formalForm(nameMapping, isConsidered(), keywords);
		if (expression == null && message == null) {
			if (isConsidered()) {
				return new StringBuffer("assert ")
						.append(nameMapping.getExprID(_expresion)).append(';');
			}
			return null;
		}
		StringBuffer buffer = new StringBuffer("assert ");
		if (expression != null) {
			buffer.append(expression);
		} else {
			buffer.append(nameMapping.getExprID(_expresion));
		}
		if (message != null) {
			buffer.append(":").append(message);
		}
		buffer.append(";");
		return buffer;
	}

	@Override
	public boolean patternMatch(Node node, Map<Node, Node> matchedNode) {
		return node != null && isConsidered() == node.isConsidered()
				&& node.getNodeType() == TYPE.ASSERT
				&& NodeUtils.patternMatch(this, node, matchedNode);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("assert");
		_tokens.addAll(_expresion.tokens());
		if (_message != null) {
			_tokens.addAll(_message.tokens());
		}
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof AssertStmt) {
			AssertStmt assertStmt = (AssertStmt) other;
			if (_expresion.compare(assertStmt.getExpression())) {
				if (_message == null) {
					return assertStmt.getMessage() == null;
				} else {
					return _message.compare(assertStmt.getMessage());
				}
			}
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> nodes = new ArrayList<>(2);
		nodes.add(_expresion);
		if (_message != null) {
			nodes.add(_message);
		}
		return nodes;
	}

	@Override
	public void computeFeatureVector() {
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.KEY_ASSERT);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.KEY_ASSERT);
		_completeFVector.combineFeature(_expresion.getFeatureVector());
		if (_message != null) {
			_completeFVector.combineFeature(_message.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		AssertStmt assertStmt = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			assertStmt = (AssertStmt) getBindingNode();
			match = (assertStmt == node);
		} else if (canBinding(node)) {
			assertStmt = (AssertStmt) node;
			setBindingNode(assertStmt);
			match = true;
		}

		if (assertStmt != null) {
			_expresion.postAccurateMatch(assertStmt.getExpression());
			if (_message != null && assertStmt.getMessage() != null) {
				_message.postAccurateMatch(assertStmt.getMessage());
			}
		} else {
			continueTopDownMatchNull();
		}

		return match;
	}

	@Override
	public boolean genModifications() {
		if (super.genModifications()) {
			AssertStmt assertStmt = (AssertStmt) getBindingNode();
			if (_expresion.getBindingNode() != assertStmt.getExpression()) {
				Update update = new Update(this, _expresion, assertStmt.getExpression());
				_modifications.add(update);
			} else {
				_expresion.genModifications();
			}
			if (_message == null) {
				if (assertStmt.getMessage() != null) {
					Update update = new Update(this, _message, assertStmt.getMessage());
					_modifications.add(update);
				}
			} else if (_message.getBindingNode() != assertStmt.getMessage()) {
				Update update = new Update(this, _message, assertStmt.getMessage());
				_modifications.add(update);
			} else {
				_message.genModifications();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
		if(node instanceof AssertStmt) {
			AssertStmt assertStmt = (AssertStmt) node;
			return getExpression().ifMatch(assertStmt.getExpression(), matchedNode, matchedStrings, level)
					|| super.ifMatch(node, matchedNode, matchedStrings, level);
		}
		return false;
	}

	@Override
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer("assert ");
			StringBuffer tmp = _expresion.transfer(vars, exprMap, retType, exceptions);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
			if (_message != null) {
				tmp = _message.transfer(vars, exprMap, retType, exceptions);
				if (tmp == null) return null;
				stringBuffer.append(":").append(tmp);
			}
			stringBuffer.append(";");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions) {
		StringBuffer expression = null;
		StringBuffer message = null;
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			AssertStmt assertStmt = (AssertStmt) pnode;
			for (Modification modification : assertStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					Node changedNode = update.getSrcNode();
					if (changedNode == assertStmt.getExpression()) {
						expression = update.apply(vars, exprMap, retType, exceptions);
						if (expression == null) return null;
					} else if (changedNode == assertStmt.getMessage()) {
						message = update.apply(vars, exprMap, retType, exceptions);
						if (message == null) return null;
					}
				} else {
					LevelLogger.error("@AssertStmt Should not be this kind of modification : " + modification);
				}
			}
		}

		StringBuffer stringBuffer = new StringBuffer("assert ");
		StringBuffer tmp;
		if(expression == null) {
			tmp = _expresion.adaptModifications(vars, exprMap, retType, exceptions);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(expression);
		}

		if (message == null) {
			if (_message != null) {
				tmp = _message.adaptModifications(vars, exprMap, retType, exceptions);
				if (tmp == null) return null;
				stringBuffer.append(":").append(tmp);
			}
		} else {
			stringBuffer.append(":").append(message);
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
}