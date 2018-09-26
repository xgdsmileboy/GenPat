/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class DoubleLiteral extends NumLiteral {

	private double _value = 0;
	private final double EPSILON = 1e-7;
	
	
	public DoubleLiteral(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.DLITERAL;
	}
	
	public void setValue(double value){
		_value = value;
	}
	
	public double getValue(){
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
		if(other instanceof DoubleLiteral) {
			return (Math.abs(_value - ((DoubleLiteral) other)._value) < EPSILON);
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
		if(other instanceof DoubleLiteral) {
			_matchNodeType = Math.abs(_value - ((DoubleLiteral) other)._value) < EPSILON;
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof DoubleLiteral) {
			match = true;
			((DoubleLiteral) sketch)._binding = this;
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
