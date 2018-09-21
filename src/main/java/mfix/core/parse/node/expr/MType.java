package mfix.core.parse.node.expr;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.stmt.Stmt;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MType extends Node {

	private org.eclipse.jdt.core.dom.Type _type;
	
	public MType(int startLine, int endLine, ASTNode oriNode) {
		super(startLine, endLine, oriNode);
		_nodeType = TYPE.TYPE;
	}
	
	public void setType(org.eclipse.jdt.core.dom.Type type) {
		if(type == null) {
			type = AST.newAST(AST.JLS8).newWildcardType();
		}
		this._type = type;
	}
	
	public org.eclipse.jdt.core.dom.Type type() {
		return _type;
	}
	
	public boolean isArrayType() {
		return _type.isArrayType();
	}
	
	public Type getElementType() {
		if(isArrayType()) {
			return ((ArrayType) _type).getElementType();
		} else {
			return _type;
		}
	}

	@Override
	public boolean compare(Node other) {
		if(other instanceof MType) {
			return _type.toString().equals(((MType) other)._type.toString());
		}
		return false;
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
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_type.toString());
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
			stringBuffer.append(_type.toString());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
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
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof MType && _type.toString().equals(((MType) other)._type.toString())) {
			_matchNodeType = true;
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof MType) {
			if(equals(sketch)) {
				((MType) sketch)._binding = this;
				_binding = sketch;
				return true;
			}
		}
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if(sketch instanceof MType)	{
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_type.toString());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof MType)) {
			return false;
		}
		MType mType = (MType) obj;
		return _type.toString().equals(mType._type.toString());
	}

}
