/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SwCase extends Stmt {

	private static final long serialVersionUID = 3371970934436172117L;
	private Expr _expression = null;
	
	/**
	 * SwitchCase:
     *           case Expression  :
     *           default :
	 */
	public SwCase(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public SwCase(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SWCASE;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression(){
		return _expression;
	}
	
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression == null) {
			stringBuffer.append("default :\n");
		} else {
			stringBuffer.append("case ");
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(" :\n");
		}

		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_expression == null) {
			_tokens.add("default");
		} else {
			_tokens.add("case");
			_tokens.addAll(_expression.tokens());
		}
		_tokens.add(":");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		if(_expression != null) {
			children.add(_expression);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof SwCase) {
			match = true;
			SwCase swCase = (SwCase) other;
			if(_expression != null) {
				return _expression.compare(swCase._expression);
			} else {
				return swCase._expression == null;
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_CASE);
		if(_expression != null) {
			_fVector.combineFeature(_expression.getFeatureVector());
		}
	}

}
