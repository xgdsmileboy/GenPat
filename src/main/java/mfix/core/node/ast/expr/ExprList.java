/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ExprList extends Node {

	private static final long serialVersionUID = -1155629329446419826L;
	private List<Expr> _exprs;
	
	public ExprList(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.EXPRLST;
	}
	
	public void setExprs(List<Expr> exprs) {
		this._exprs = exprs;
	}

	public List<Expr> getExpr() {
		return _exprs;
	}

	@Override
	public boolean compare(Node other) {
		if(other instanceof ExprList) {
			ExprList exprList = (ExprList) other;
			if(_exprs.size() == exprList._exprs.size()) {
				for(int i = 0; i < _exprs.size(); i++) {
					if(!_exprs.get(i).compare(exprList._exprs.get(i))) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_exprs.size() > 0) {
			stringBuffer.append(_exprs.get(0).toSrcString());
			for(int i = 1; i < _exprs.size(); i++) {
				stringBuffer.append(",");
				stringBuffer.append(_exprs.get(i).toSrcString());
			}
		}
		return stringBuffer;
	}
	
	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_exprs.size());
		children.addAll(_exprs);
		return children;
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		for(Expr expr : _exprs) {
			_fVector.combineFeature(expr.getFeatureVector());
		}
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_exprs.size() > 0) {
			_tokens.addAll(_exprs.get(0).tokens());
			for(int i = 1; i < _exprs.size(); i++) {
				_tokens.add(",");
				_tokens.addAll(_exprs.get(i).tokens());
			}
		}
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		ExprList exprList = null;
		boolean match = false;
		if(getBindingNode() != null) {
			exprList = (ExprList) getBindingNode();
			match = (exprList == node);
		} else if(canBinding(node)) {
			exprList = (ExprList) node;
			setBindingNode(node);
			match = true;
		}

		if(exprList == null) {
			continueTopDownMatchNull();
		} else {
			greedyMatchListNode(_exprs, exprList.getExpr());
		}
		return match;
	}
}
