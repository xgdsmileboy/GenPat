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
public class Svd extends Expr implements Serializable {

	private static final long serialVersionUID = 3849439897999091912L;
	private MType _decType = null;
	private SName _name = null;
	private Expr _initializer = null;
	
	/**
	 * { ExtendedModifier } Type {Annotation} [ ... ] Identifier { Dimension } [ = Expression ]
	 * "..." should not be appear since it is only used in method declarations
	 */
	public Svd(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SINGLEVARDECL;
	}
	
	public void setDecType(MType decType){
		_decType = decType;
	}
	
	public void setName(SName name){
		_name = name;
	}

	public MType getDeclType() {
		return _decType;
	}

	public Expr getInitializer() {
		return _initializer;
	}

	public SName getName(){
		return _name;
	}
	
	public void setInitializer(Expr initializer){
		_initializer = initializer;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_decType.toSrcString());
		stringBuffer.append(" ");
		stringBuffer.append(_name.toSrcString());
		if(_initializer != null){
			stringBuffer.append("=");
			stringBuffer.append(_initializer.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_decType.tokens());
		_tokens.addAll(_name.tokens());
		if(_initializer != null) {
			_tokens.addFirst("=");
			_tokens.addAll(_initializer.tokens());
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof Svd) {
			Svd svd	= (Svd) other;
			match = _decType.compare(svd._decType);
			match = match && _name.compare(svd._name);
			if(_initializer == null) {
				match = match && (svd._initializer == null); 
			} else {
				match = match && _initializer.compare(svd._initializer);
			}
		}
		
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_decType);
		children.add(_name);
		if(_initializer != null) {
			children.add(_initializer);
		}
		return children;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_decType.getFeatureVector());
		_fVector.combineFeature(_name.getFeatureVector());
		if(_initializer != null){
			_fVector.inc(FVector.ARITH_ASSIGN);
			_fVector.combineFeature(_initializer.getFeatureVector());
		}
	}

}
