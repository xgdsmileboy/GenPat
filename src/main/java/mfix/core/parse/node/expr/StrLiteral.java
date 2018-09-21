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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class StrLiteral extends Expr {

	private String _value = null;
	
	/**
	 * String literal nodes.
	 */
	public StrLiteral(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.SLITERAL;
	}
	
	public void setValue(String value){
		_value = value.replace("\\", "\\\\").replace("\'", "\\'").replace("\"", "\\\"").replace("\n", "\\n")
				.replace("\b", "\\b").replace("\t", "\\t").replace("\r", "\\r").replace("\f", "\\f")
				.replace("\0", "\\0");
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("\"");
		stringBuffer.append(_value);
		stringBuffer.append("\"");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		return toSrcString();
	}
	
	public StringBuffer replace(Map<String,String> exprMap, Set<String> allUsableVars) {
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
			stringBuffer.append("\"" + _value + "\"");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("\"" + _value + "\"");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof StrLiteral) {
			StrLiteral strLiteral = (StrLiteral) other;
			match = _value.equals(strLiteral._value);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(0);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		return new LinkedList<>();
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof StrLiteral) {
			StrLiteral strLiteral = (StrLiteral) other;
			_matchNodeType = _value.equals(strLiteral._value);
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		if(sketch instanceof StrLiteral) {
			((StrLiteral) sketch)._binding = this;
			_binding = sketch;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if(sketch instanceof StrLiteral) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_LITERAL);
	}

}
