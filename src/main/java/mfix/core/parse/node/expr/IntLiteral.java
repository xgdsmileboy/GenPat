/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.config.Constant;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.LinkedList;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class IntLiteral extends NumLiteral {

	private int _value = 0;
	
	public IntLiteral(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.INTLITERAL;
	}
	
	public void setValue(int value){
		_value = value;
	}
	
	public int getValue(){
		return _value;
	}
	
	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(String.valueOf(_value));
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if (isKeyPoint()) {
			stringBuffer.append(String.valueOf(_value));
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(String.valueOf(_value));
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof IntLiteral) {
			IntLiteral intLiteral = (IntLiteral) other;
			match = (_value == intLiteral._value);
		}
		return match;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof IntLiteral) {
			_matchNodeType = (_value == ((IntLiteral) other)._value);
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof IntLiteral || sketch instanceof LongLiteral) {
			match = true;
			NumLiteral numLiteral = (NumLiteral) sketch;
			numLiteral.setBinding(this);
			_binding = numLiteral;
		}
		return match;
	}
	
}
