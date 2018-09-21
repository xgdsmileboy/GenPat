/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.config.Constant;
import mfix.core.comp.Modification;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class FloatLiteral extends NumLiteral {

	private float _value = .0f;
	private final double EPSILON = 1e-5;
	
	public FloatLiteral(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.FLITERAL;
	}
	
	public void setValue(float value){
		_value = value;
	}
	
	public float getValue(){
		return _value;
	}
	
	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(String.valueOf(_value));
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
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
		if(other instanceof FloatLiteral) {
			return (Math.abs(_value - ((FloatLiteral) other)._value) < EPSILON);
		}
		return false;
	}
	
	@Override
	public List<Modification> extractModifications() {
		return new LinkedList<>();
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof FloatLiteral) {
			_matchNodeType = (Math.abs(_value - ((FloatLiteral) other)._value) < EPSILON);
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof FloatLiteral || sketch instanceof DoubleLiteral) {
			match = true;
			NumLiteral numLiteral = (NumLiteral) sketch;
			numLiteral.setBinding(this);
			_binding = sketch;
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
	}
	
}
