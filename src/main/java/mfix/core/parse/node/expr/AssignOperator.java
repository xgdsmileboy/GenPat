/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class AssignOperator extends Operator implements Serializable {

	private static final long serialVersionUID = 2573726544838821670L;
	private String _operatorStr;
	private transient Assignment.Operator _operator;
	
	public AssignOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.ASSIGNOPERATOR;
	}
	
	public void setOperator(Assignment.Operator operator) {
		this._operator = operator;
		this._operatorStr = operator.toString();
	}
	
	public Assignment.Operator getOperator() {
		return _operator;
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof AssignOperator) {
			return _operatorStr.equals(((AssignOperator) other)._operatorStr);
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operatorStr);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		return toSrcString();
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		String result = exprMap.get(_operatorStr);
		if(result != null) {
			return new StringBuffer(result);
		} else {
			return toSrcString();
		}
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operatorStr);
		return stringBuffer;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof AssignOperator && _operatorStr.equals(((AssignOperator) other)._operatorStr)) {
			_matchNodeType = true;
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof AssignOperator) {
			match = true;
			AssignOperator assignOperator = (AssignOperator) sketch;
			assignOperator._binding = this;
			_binding = assignOperator;
		}
		return match;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operatorStr);
	}

}
