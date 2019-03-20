/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.conf.Constant;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		_fIndex = VIndex.STMT_SWTICH;
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
		StringBuffer stringBuffer = new StringBuffer("switch (");
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
	protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
		if (isAbstract() && !isConsidered()) return null;
		StringBuffer exp = _expression.formalForm(nameMapping, isConsidered(), keywords);
		List<StringBuffer> strings = new ArrayList<>(_statements.size());
		StringBuffer b;
		Stmt stmt;
		StringBuffer lastCase = null;
		for (int i = 0; i < _statements.size(); i++) {
			stmt = _statements.get(i);
			b = stmt.formalForm(nameMapping, false, keywords);
			if (b != null) {
				if (lastCase != null) {
					strings.add(lastCase);
					lastCase = null;
				}
				strings.add(b);
			} else if (stmt instanceof SwCase) {
				lastCase = stmt.toSrcString();
			}
		}
		if (isConsidered() || exp != null || !strings.isEmpty()) {
			StringBuffer buffer = new StringBuffer("switch (");
			buffer.append(exp == null ? nameMapping.getExprID(_expression) : exp).append("){");
			if (!strings.isEmpty()) {
				for (int i = 0; i < strings.size(); i++) {
					buffer.append('\n').append(strings.get(i));
				}
				buffer.append("\n}");
			} else {
				buffer.append('}');
			}
			return buffer;
		}
		return null;
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
		_selfFVector = new FVector();
		_selfFVector.inc(FVector.KEY_SWITCH);

		_completeFVector = new FVector();
		_completeFVector.inc(FVector.KEY_SWITCH);
		_completeFVector.combineFeature(_expression.getFeatureVector());
		for(Stmt stmt : _statements) {
			_completeFVector.combineFeature(stmt.getFeatureVector());
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		SwitchStmt switchStmt = null;
		if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
			switchStmt = (SwitchStmt) getBindingNode();
			match = (switchStmt == node);
		} else if(canBinding(node)) {
			switchStmt = (SwitchStmt) node;
			setBindingNode(switchStmt);
			match = true;
		}
		if(switchStmt == null) {
			continueTopDownMatchNull();
		} else {
			_expression.postAccurateMatch(switchStmt.getExpression());
			NodeUtils.greedyMatchListNode(_statements, switchStmt.getStatements());
		}
		return match;
	}

	@Override
	public boolean genModifications() {
		if(super.genModifications()) {
			SwitchStmt switchStmt = (SwitchStmt) getBindingNode();
			if(_expression.getBindingNode() != switchStmt.getExpression()) {
				Update update = new Update(this, _expression, switchStmt.getExpression());
				_modifications.add(update);
			} else {
				_expression.genModifications();
			}
			_modifications.addAll(NodeUtils.genModificationList(this, _statements, switchStmt.getStatements()));
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
	public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
		StringBuffer stringBuffer = super.transfer(vars, exprMap);
		if (stringBuffer == null) {
			stringBuffer = new StringBuffer("switch (");
			StringBuffer tmp = _expression.transfer(vars, exprMap);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Stmt stmt : _statements) {
				tmp = stmt.transfer(vars, exprMap);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(Constant.NEW_LINE);
			}
			stringBuffer.append("}");
		}
		return stringBuffer;
	}

	@Override
	public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
		Node pnode = NodeUtils.checkModification(this);
		if (pnode != null) {
			SwitchStmt switchStmt = (SwitchStmt) pnode;
			StringBuffer expression = null;
			List<Modification> modifications = new LinkedList<>();
			for (Modification modification : switchStmt.getModifications()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == switchStmt._expression) {
						expression = update.apply(vars, exprMap);
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
			Map<Integer, List<StringBuffer>> insertionAt = new HashMap<>();
			Map<Node, StringBuffer> map = new HashMap<>(_statements.size());
			if (!Matcher.applyNodeListModifications(modifications, _statements,
					insertionBefore, insertionAfter, insertionAt, map, vars, exprMap)) {
				return null;
			}
			StringBuffer stringBuffer = new StringBuffer("switch (");
			StringBuffer tmp;
			if (expression == null) {
				tmp = _expression.adaptModifications(vars, exprMap);
				if (tmp == null) return null;
				stringBuffer.append(tmp);
			} else {
				stringBuffer.append(expression);
			}
			stringBuffer.append("){" + Constant.NEW_LINE);

			tmp = NodeUtils.assemble(_statements, insertionBefore, insertionAfter, map, insertionAt,
					vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("}");
			return stringBuffer;

		} else {
			StringBuffer stringBuffer = new StringBuffer("switch (");
			StringBuffer tmp = _expression.adaptModifications(vars, exprMap);
			if (tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("){" + Constant.NEW_LINE);
			for (Stmt stmt : _statements) {
				tmp = stmt.adaptModifications(vars, exprMap);
				if (tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(Constant.NEW_LINE);
			}

			stringBuffer.append("}");
			return stringBuffer;
		}
	}
}
