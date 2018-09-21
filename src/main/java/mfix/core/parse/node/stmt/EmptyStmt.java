/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

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
public class EmptyStmt extends Stmt {

	/**
	 * EmptyStatement:
     *	;
	 */
	public EmptyStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}
	
	public EmptyStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(";");
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		return toSrcString();
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		return toSrcString();
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(";");
		} else {
//			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof EmptyStmt) {
			return true;
		}
		return false;
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
		if(other instanceof EmptyStmt) {
			_matchNodeType = true;
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		if(sketch instanceof EmptyStmt) {
			((EmptyStmt) sketch)._binding = this;
			_binding = sketch;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if(sketch instanceof EmptyStmt) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public Node bindingNode(Node patternNode) {
		if(patternNode instanceof EmptyStmt) {
			return this;
		}
		return null;
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
	}
}
