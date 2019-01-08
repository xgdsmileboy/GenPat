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
public class CharLiteral extends Expr implements Serializable {

	private static final long serialVersionUID = 995719993109521913L;
	private char _value = ' ';
	private String _valStr = null;

	/**
	 * Character literal nodes.
	 */
	public CharLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.CLITERAL;
	}

	public void setValue(char value) {
		_value = value;
		_valStr = "" + _value;
		_valStr = _valStr.replace("\\", "\\\\").replace("\'", "\\'").replace("\"", "\\\"").replace("\n", "\\n")
				.replace("\b", "\\b").replace("\t", "\\t").replace("\r", "\\r").replace("\f", "\\f")
				.replace("\0", "\\0");
	}

	public String getStringValue() {
		return toString().toString();
	}

	public char getValue() {
		return _value;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer("\'" + _valStr + "\'");
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("\'" + _valStr + "\'");
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof CharLiteral) {
			CharLiteral charLiteral = (CharLiteral) other;
			match = (_value == charLiteral._value);
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_CHAR);
	}
}
