/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class AryInitializer extends Expr {

	private static final long serialVersionUID = 5694794734726396689L;
	private List<Expr> _expressions = null;

	/**
	 * ArrayInitializer: { [ Expression { , Expression} [ , ]] }
	 */
	public AryInitializer(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.ARRINIT;
	}

	public void setExpressions(List<Expr> expressions) {
		_expressions = expressions;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_expressions.size());
		children.addAll(_expressions);
		return children;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("{");
		if (_expressions.size() > 0) {
			stringBuffer.append(_expressions.get(0).toSrcString());
			for (int i = 1; i < _expressions.size(); i++) {
				stringBuffer.append(",");
				stringBuffer.append(_expressions.get(i).toSrcString());
			}
		}
		stringBuffer.append("}");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("{");
		if (_expressions.size() > 0) {
			_tokens.addAll(_expressions.get(0).tokens());
			for (int i = 1; i < _expressions.size(); i++) {
				_tokens.add(",");
				_tokens.addAll(_expressions.get(i).tokens());
			}
		}
		_tokens.add("}");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof AryInitializer) {
			AryInitializer aryInitializer = (AryInitializer) other;
			match = (_expressions.size() == aryInitializer._expressions.size());
			for(int i = 0; match && i < _expressions.size(); i ++) {
				match = match && _expressions.get(i).compare(aryInitializer._expressions.get(i));
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_AINIT);
		if(_expressions != null){
			for(Expr expr : _expressions){
				_fVector.combineFeature(expr.getFeatureVector());
			}
		}
	}
	
}
