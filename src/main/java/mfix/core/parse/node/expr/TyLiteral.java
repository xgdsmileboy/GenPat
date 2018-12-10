/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
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
public class TyLiteral extends Expr implements Serializable {

	private static final long serialVersionUID = 5518643646465944075L;
	private MType _type = null;
	
	/**
	 * TypeLiteral:
     *	( Type | void ) . class
	 */
	public TyLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.TLITERAL;
	}
	
	public void setValue(MType type){
		_type = type;
	}

	public MType getDeclType() {
		return _type;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_type.toSrcString());
		stringBuffer.append(".class");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		return toSrcString();
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		String result = exprMap.get(toSrcString().toString());
		if(result != null) {
			return new StringBuffer(result);
		}
		return toSrcString();
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_type.printMatchSketch());
			stringBuffer.append(".class");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_type.tokens());
		_tokens.add(".");
		_tokens.add("class");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof TyLiteral) {
			TyLiteral literal = (TyLiteral) other;
			match = _type.compare(literal._type);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(0);
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public List<Modification> extractModifications() {
		return new LinkedList<>();
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof TyLiteral) {
			_matchNodeType = _type.compare(((TyLiteral) other)._type); 
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		if(sketch instanceof TyLiteral) {
			((TyLiteral) sketch)._binding = this;
			_binding = sketch;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if(sketch instanceof TyLiteral) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_type.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_type.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_TYPE);
	}

}
