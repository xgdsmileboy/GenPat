/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class StrLiteral extends Expr {

	private static final long serialVersionUID = -8857803991178178009L;
	private String _value = null;
	
	/**
	 * String literal nodes.
	 */
	public StrLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SLITERAL;
	}
	
	public void setValue(String value){
		_value = value.replace("\\", "\\\\").replace("\'", "\\'").replace("\"", "\\\"").replace("\n", "\\n")
				.replace("\b", "\\b").replace("\t", "\\t").replace("\r", "\\r").replace("\f", "\\f")
				.replace("\0", "\\0");
	}

	public String getValue() {
		return _value;
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
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.E_STR);
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		if(getBindingNode() == node) return false;
		if(getBindingNode() == null && canBinding(node)) {
			setBindingNode(node);
			return true;
		}
		return false;
	}
}
