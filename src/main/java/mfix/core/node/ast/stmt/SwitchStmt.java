/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.util.Constant;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SwitchStmt extends Stmt {

	private static final long serialVersionUID = 242211567501322520L;
	private Expr _expression = null;
	private List<Stmt> _statements = null;
	
	/**
	 * SwitchStatement:
     *           switch ( Expression )
     *                   { { SwitchCase | Statement } }
 	 * SwitchCase:
     *           case Expression  :
     *           default :
	 */
	public SwitchStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public SwitchStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SWSTMT;
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setStatements(List<Stmt> statements){
		_statements = statements;
	}

	public Expr getExpression() { return _expression; }

	public List<Stmt> getStatements() { return _statements; }
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("swtich (");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append("){" + Constant.NEW_LINE);
		for (Stmt stmt : _statements) {
			stringBuffer.append(stmt.toSrcString());
			stringBuffer.append(Constant.NEW_LINE);
		}
		stringBuffer.append("}");
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("switch");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.add("{");
		for(Stmt stmt : _statements) {
			_tokens.addAll(stmt.tokens());
		}
		_tokens.add("}");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_statements.size() + 1);
		children.add(_expression);
		children.addAll(_statements);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return _statements;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof SwitchStmt) {
			SwitchStmt switchStmt = (SwitchStmt) other;
			match = _expression.compare(switchStmt._expression)
					&& (_statements.size() == switchStmt._statements.size());
			for (int i = 0; match && i < _statements.size(); i++) {
				match = match && _statements.get(i).compare(switchStmt._statements.get(i));
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_SWITCH);
		_fVector.combineFeature(_expression.getFeatureVector());
		for(Stmt stmt : _statements) {
			_fVector.combineFeature(stmt.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		SwitchStmt switchStmt = null;
		if(getBindingNode() != null) {
			switchStmt = (SwitchStmt) getBindingNode();
			match = (switchStmt == node);
		} else if(canBinding(node)) {
			switchStmt = (SwitchStmt) node;
			match = true;
		}
		if(switchStmt == null) {
			continueTopDownMatchNull();
		} else {
			greedyMatchListNode(_statements, switchStmt.getStatements());
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if(super.genModidications()) {
			SwitchStmt switchStmt = (SwitchStmt) getBindingNode();
			if(_expression.getBindingNode() != switchStmt.getExpression()) {
				Update update = new Update(this, _expression, switchStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModidications();
			}
			genModificationList(_statements, switchStmt.getStatements(), true);
			return true;
		}
		return false;
	}

	@Override
	public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
		if (node instanceof SwitchStmt) {
			SwitchStmt switchStmt = (SwitchStmt) node;
			return _expression.ifMatch(switchStmt.getExpression(), matchedNode, matchedStrings)
					&& super.ifMatch(node, matchedNode, matchedStrings);
		}
		return false;
	}

	@Override
	public StringBuffer transfer() {
		StringBuffer stringBuffer = super.transfer();
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer("switch (");
			StringBuffer tmp = _expression.transfer();
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Stmt stmt : _statements) {
				tmp = stmt.transfer();
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(Constant.NEW_LINE);
			}
			stringBuffer.append("}");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications() {
		Node pnode = checkModification();
		if (pnode != null) {
			SwitchStmt switchStmt = (SwitchStmt) pnode;
			StringBuffer expression = null;
			List<Modification> modifications = new LinkedList<>();
			for (Modification modification : switchStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == switchStmt._expression) {
						expression = update.apply();
						if (expression == null) return null;
					} else {
						modifications.add(update);
					}
				} else {
					modifications.add(modification);
				}
			}

			Map<Node, List<StringBuffer>> insertionBefore = new HashMap<>();
			Map<Node, List<StringBuffer>> insertionAfter = new HashMap<>();
			Map<Node, StringBuffer> map = new HashMap<>(_statements.size());
			if (!Matcher.applyNodeListModifications(modifications, _statements, insertionBefore, insertionAfter,
					map)) {
				return null;
			}
			StringBuffer stringBuffer = new StringBuffer("swtich (");
			StringBuffer tmp;
			if (expression == null) {
				tmp = _expression.adaptModifications();
				if (tmp == null) return null;
				stringBuffer.append(tmp);
			} else {
				stringBuffer.append(expression);
			}
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Node node : _statements) {
				List<StringBuffer> list = insertionBefore.get(node);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						stringBuffer.append(list.get(i));
						stringBuffer.append(Constant.NEW_LINE);
					}
				}
				if (map.containsKey(node)) {
					StringBuffer update = map.get(node);
					if (update != null) {
						stringBuffer.append(update);
						stringBuffer.append(Constant.NEW_LINE);
					}
				} else {
					tmp = node.adaptModifications();
					if (tmp == null) return null;
					stringBuffer.append(tmp);
					stringBuffer.append(Constant.NEW_LINE);
				}
				list = insertionAfter.get(node);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						stringBuffer.append(list.get(i));
						stringBuffer.append(Constant.NEW_LINE);
					}
				}
			}
			stringBuffer.append("}");
			return stringBuffer;

		} else {
			StringBuffer stringBuffer = new StringBuffer("switch (");
			StringBuffer tmp = _expression.adaptModifications();
			if (tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Stmt stmt : _statements) {
				tmp = stmt.adaptModifications();
				if (tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(Constant.NEW_LINE);
			}

			stringBuffer.append("}");
			return stringBuffer;
		}
	}
}
