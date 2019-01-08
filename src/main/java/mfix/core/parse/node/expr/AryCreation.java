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
public class AryCreation extends Expr implements Serializable {

	private static final long serialVersionUID = -8863515069590314916L;
	private MType _type = null;
	private List<Expr> _dimension = null;
	private AryInitializer _initializer = null;

	/**
	 * ArrayCreation: new PrimitiveType [ Expression ] { [ Expression ] } { [ ]
	 * } new TypeName [ < Type { , Type } > ] [ Expression ] { [ Expression ] }
	 * { [ ] } new PrimitiveType [ ] { [ ] } ArrayInitializer new TypeName [ <
	 * Type { , Type } > ] [ ] { [ ] } ArrayInitializer
	 */
	public AryCreation(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.ARRCREAT;
	}

	public void setArrayType(MType type) {
		_type = type;
	}

	public void setDimension(List<Expr> dimension) {
		_dimension = dimension;
	}

	public void setInitializer(AryInitializer initializer) {
		_initializer = initializer;
	}

	public MType getElementType() {
		return _type;
	}

	public List<Expr> getDimention() {
		return _dimension;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_dimension.size() + 1);
		children.addAll(_dimension);
		if(_initializer != null) {
			children.add(_initializer);
		}
		return children;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("new ");
		stringBuffer.append(_type.toSrcString());
		for(Expr expr : _dimension) {
			stringBuffer.append("[");
			stringBuffer.append(expr.toSrcString());
			stringBuffer.append("]");
		}
		if(_initializer != null) {
			stringBuffer.append("=");
			stringBuffer.append(_initializer.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("new");
		_tokens.addAll(_type.tokens());
		for(Expr expr : _dimension) {
			_tokens.add("[");
			_tokens.addAll(expr.tokens());
			_tokens.add("]");
		}
		if(_initializer != null) {
			_tokens.add("=");
			_tokens.addAll(_initializer.tokens());
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof AryCreation) {
			AryCreation aryCreation = (AryCreation) other;
			match = _type.compare(aryCreation._type);
			if(match) {
				match = match && (_dimension.size() == aryCreation._dimension.size());
				for(int i = 0; match && i < _dimension.size(); i++) {
					match = match && _dimension.get(i).compare(aryCreation._dimension.get(i));
				}
				if(_initializer == null) {
					match = match && (aryCreation._initializer == null);
				} else {
					match = match && _initializer.compare(aryCreation._initializer);
				}
			}
		}
		return match;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_NEW);
		_fVector.inc(FVector.E_ACREAR);
		if(_dimension != null){
			for(Expr expr : _dimension){
				_fVector.combineFeature(expr.getFeatureVector());
			}
		}
		if(_initializer != null){
			_fVector.combineFeature(_initializer.getFeatureVector());
		}
	}

}
