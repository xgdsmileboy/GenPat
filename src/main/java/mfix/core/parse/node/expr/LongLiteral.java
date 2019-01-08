/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class LongLiteral extends NumLiteral {

	private static final long serialVersionUID = -5464691868940145050L;
	private long _value = 0l;
	
	public LongLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.LLITERAL;
	}
	
	public void setValue(long value){
		_value = value;
	}
	
	public long getValue(){
		return _value;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(String.valueOf(_value));
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(String.valueOf(_value));
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof LongLiteral) {
			LongLiteral literal = (LongLiteral) other;
			match = (_value == literal._value);
		}
		return match;
	}
	
}
