/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.SName;
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
public class BreakStmt extends Stmt implements Serializable {

	private static final long serialVersionUID = 228415180803512647L;
	private SName _identifier = null;
	
	/**
	 * BreakStatement:
     *	break [ Identifier ] ;
	 */
	public BreakStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public BreakStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.BREACK;
	}
	
	public void setIdentifier(SName identifier){
		_identifier = identifier;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("break");
		if(_identifier != null){
			stringBuffer.append(" ");
			stringBuffer.append(_identifier.toSrcString());
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer identifier = null;
		if(_binding != null && _binding instanceof BreakStmt) {
			BreakStmt breakStmt = (BreakStmt) _binding;
			for(Modification modification : breakStmt.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == breakStmt._identifier) {
						identifier = update.getTarString(exprMap, allUsableVars);
						if(identifier == null) return null;
					} else {
						LevelLogger.error("@BreakStmt ERROR");
					}
				} else {
					LevelLogger.error("@BreakStmt Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer("break");
		if(identifier == null) {
			if(_identifier != null){
				stringBuffer.append(" ");
				StringBuffer tmp = _identifier.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append(" " + identifier);
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("break");
		if(_identifier != null){
			stringBuffer.append(" ");
			StringBuffer tmp = _identifier.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		stringBuffer.append(";");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("break");
			if(_identifier != null){
				stringBuffer.append(" ");
				stringBuffer.append(_identifier.printMatchSketch());
			}
			stringBuffer.append(";");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_identifier != null) {
			_tokens.addAll(_identifier.tokens());
		}
		_tokens.add(";");
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		if(_identifier != null) {
			children.add(_identifier);
		}
		return children;
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof BreakStmt) {
			BreakStmt breakStmt = (BreakStmt) other;
			match = _identifier == null ? (breakStmt._identifier == null) : _identifier.compare(breakStmt._identifier);
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
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
		}
		return modifications;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof BreakStmt) {
			_matchNodeType = true;
			BreakStmt breakStmt = (BreakStmt) other;
			if(_identifier == null && breakStmt._identifier != null) {
				_matchNodeType = false;
				return;
			}
			if(_identifier != null && breakStmt._identifier != null) {
				_identifier.deepMatch(breakStmt._identifier);
				if(!_identifier.isNodeTypeMatch()) {
					Update update = new Update(this, _identifier, breakStmt._identifier);
					_modifications.add(update);
				}
			} else if(_identifier != null) {
				Update update = new Update(this, _identifier, breakStmt._identifier);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		if(sketch instanceof BreakStmt) {
			((BreakStmt) sketch)._binding = this;
			_binding = sketch;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if(sketch instanceof BreakStmt) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public Node bindingNode(Node patternNode) {
		if(patternNode instanceof BreakStmt) {
			return this;
		}
		return null;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		if(_identifier != null) {
			_identifier.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		if(_identifier != null) {
			_identifier.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_BREAK);
		if(_identifier != null) {
            _fVector.combineFeature(_identifier.getFeatureVector());
        }
	}

}
