/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PrefixExpression;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class PrefixOperator extends Operator implements Serializable {

	private static final long serialVersionUID = -7394935189908328110L;
	private PrefixExpression.Operator _operator;
	private static Map<String, Integer> _operatorMap;
	
	static {
		_operatorMap = new HashMap<>();
		_operatorMap.put("++", 0);
		_operatorMap.put("--", 0);
		_operatorMap.put("+", 1);
		_operatorMap.put("-", 1);
		_operatorMap.put("~", 2);
		_operatorMap.put("!", 3);
	}

	public PrefixOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.PREFIXOPERATOR;
	}

	public void setOperator(PrefixExpression.Operator operator) {
		this._operator = operator;
	}

	public PrefixExpression.Operator getOperator() {
		return _operator;
	}

	@Override
	public boolean compare(Node other) {
		if (other instanceof PrefixOperator) {
			return _operator.toString().equals(((PrefixOperator) other)._operator.toString());
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toString());
		return stringBuffer;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if (other instanceof PrefixOperator) {
			_matchNodeType = true;
			if (!_operator.toString().equals(((PrefixOperator) other)._operator.toString())) {
				_matchNodeType = false;
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		if(sketch instanceof PrefixOperator) {
			PrefixOperator prefixOperator = (PrefixOperator) sketch;
			return (PrefixOperator._operatorMap.get(_operator.toString()) == PrefixOperator._operatorMap
					.get(prefixOperator._operator.toString()));
		}
		return false;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operator.toString());
	}

}
