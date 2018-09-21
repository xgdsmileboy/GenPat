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
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Svd;
import org.eclipse.jdt.core.dom.ASTNode;

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
public class CatClause extends Node {

	private Svd _exception = null;
	private Blk _blk = null; 
	
	/**
	 * CatchClause
	 *    catch ( SingleVariableDeclaration ) Block
	 */
	public CatClause(int startLine, int endLine, ASTNode oriNode) {
		this(startLine, endLine, oriNode, null);
	}
	
	public CatClause(int startLine, int endLine, ASTNode oriNode, Node parent) {
		super(startLine, endLine, oriNode, parent);
		_nodeType = TYPE.CATCHCLAUSE;
	}
	
	public void setException(Svd svd) {
		_exception = svd;
	}
	
	public Svd getException() {
		return _exception;
	}
	
	public void setBody(Blk blk) {
		_blk = blk;
	}
	
	public Blk getBody() {
		return _blk;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("catch(");
		stringBuffer.append(_exception.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_blk.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer exception = null;
		StringBuffer blk = null;
		if(_binding != null && _binding instanceof CatClause) {
			CatClause catClause = (CatClause) _binding;
			for(Modification modification : catClause.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == catClause._exception) {
						exception = update.getTarString(exprMap, allUsableVars);
						if(exception == null) return null;
					} else {
						blk = update.getTarString(exprMap, allUsableVars);
						if(blk == null) return null;
					}
				} else {
					LevelLogger.error("@CatClause Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		stringBuffer.append("catch(");
		if(exception == null) {
			tmp = _exception.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(exception);
		}
		stringBuffer.append(")");
		if(blk == null) {
			tmp = _blk.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(blk);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		stringBuffer.append("catch(");
		tmp = _exception.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		tmp = _blk.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("catch(");
			stringBuffer.append(_exception.printMatchSketch());
			stringBuffer.append(")");
			stringBuffer.append(_blk.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("catch");
		_tokens.add("(");
		_tokens.addAll(_exception.tokens());
		_tokens.add(")");
		_tokens.addAll(_blk.tokens());
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_blk);
		return children;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_exception);
		children.add(_blk);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		if(other instanceof CatClause) {
			CatClause catClause = (CatClause) other;
			return _exception.compare(catClause._exception) && _blk.compare(catClause._blk);
		}
		return false;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_exception.getKeywords());
			avoidDuplicate(_keywords, _blk);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_exception.extractModifications());
			modifications.addAll(_blk.extractModifications());
		}
		return modifications;
	}

	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof CatClause) {
			_matchNodeType = true;
			CatClause catClause = (CatClause) other;
			_exception.deepMatch(catClause._exception);
			if(!_exception.isNodeTypeMatch()) {
				Update update = new Update(this, _exception, catClause._exception);
				_modifications.add(update);
			}
			_blk.deepMatch(catClause._blk);
			if(!_blk.isNodeTypeMatch()) {
				Update update = new Update(this, _blk, catClause._blk);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof CatClause) {
			match = true;
			CatClause catClause = (CatClause) sketch;
			if(!catClause.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					match = false;
				} else {
					bindingSketch(sketch);
				}
			} else {
				if(catClause._exception.isKeyPoint()) {
					match = _exception.matchSketch(catClause._exception);
				}
				if(match && catClause._blk.isKeyPoint()) {
					match = _blk.matchSketch(catClause._blk);
				}
			}
			if(match) {
				catClause._binding = this;
				_binding = catClause;
			}
		}
		if(!match) {
			match = _blk.matchSketch(sketch);
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof CatClause) {
			match = true;
			CatClause catClause = (CatClause) sketch;
			if (catClause._exception.isKeyPoint()) {
				_exception.bindingSketch(catClause._exception);
			}
			if (catClause._blk.isKeyPoint()) {
				_blk.bindingSketch(catClause._blk);
			}
		}
		return match;
	}

	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_exception.resetAllNodeTypeMatch();
		_blk.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_exception.setAllNodeTypeMatch();
		_blk.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_exception.getFeatureVector());
		_fVector.combineFeature(_blk.getFeatureVector());
	}
}
