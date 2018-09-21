/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.NodeUtils;
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
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ArrayAcc extends Expr {

	private Expr _index = null;
	private Expr _array = null;

	/**
	 * ArrayAccess: Expression [ Expression ]
	 */
	public ArrayAcc(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.ARRACC;
	}

	public void setArray(Expr array) {
		_array = array;
	}

	public void setIndex(Expr index) {
		_index = index;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_array);
		children.add(_index);
		return children;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_array.toSrcString());
		stringBuffer.append("[");
		stringBuffer.append(_index.toSrcString());
		stringBuffer.append("]");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer array = null;
		StringBuffer index = null;
		if(_binding != null && _binding instanceof ArrayAcc) {
			ArrayAcc arrayAcc = (ArrayAcc) _binding;
			List<Modification> modifications = arrayAcc.getNodeModification();
			for(Modification modification : modifications) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == arrayAcc._array) {
						array = update.getTarString(exprMap, allUsableVars);
						if(array == null) return null;
					} else {
						index = update.getTarString(exprMap, allUsableVars);
						if(index == null) return null;
					}
				} else {
					LevelLogger.error("@ArrayAcc Should not be this kind of modification : " + modification.toString());
				}
			}
		}
		StringBuffer tmp = null;
		if(array == null) {
			tmp = _array.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(array);
		}
		stringBuffer.append("[");
		if(index == null) {
			tmp = _index.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(index);
		}
		stringBuffer.append("]");
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		String result = exprMap.get(toSrcString().toString());
		if(result == null) {
			StringBuffer stringBuffer = new StringBuffer();
			StringBuffer tmp = _array.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("[");
			tmp = _index.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append("]");
			return stringBuffer;
		} else {
			return new StringBuffer(result);
		}
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_array.printMatchSketch());
			stringBuffer.append("[");
			stringBuffer.append(_index.printMatchSketch());
			stringBuffer.append("]");
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	public void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_array.tokens());
		_tokens.add("[");
		_tokens.addAll(_index.tokens());
		_tokens.add("]");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ArrayAcc) {
			match = _array.compare(((ArrayAcc) other)._array) && _index.compare(((ArrayAcc) other)._index); 
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			_keywords.putAll(_array.getKeywords());
			avoidDuplicate(_keywords, _index);
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_array.extractModifications());
			modifications.addAll(_index.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ArrayAcc) {
			_matchNodeType = true;
			ArrayAcc arrayAcc = (ArrayAcc) other;
			_array.deepMatch(arrayAcc._array);
			_index.deepMatch(arrayAcc._index);
			if(!_array.isNodeTypeMatch()) {
				Update update = new Update(this, _array, arrayAcc._array);
				_modifications.add(update);
			}
			if(!_index.isNodeTypeMatch()) {
				Update update = new Update(this, _index, arrayAcc);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ArrayAcc) {
			match = true;
			ArrayAcc arrayAcc = (ArrayAcc) sketch;
			// find change node
			if(!arrayAcc.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)){
					return false;
				}
				bindingSketch(sketch);
			} else {
				// match array if it is match point
				if(arrayAcc._array.isKeyPoint()) {
					match = _array.matchSketch(arrayAcc._array);
				}
				// match another part
				if (match) {
					if(arrayAcc._index.isKeyPoint()){
						match = _index.matchSketch(arrayAcc._index);
					}
				}
			}
			if(match) {
				arrayAcc._binding = this;
				_binding = arrayAcc;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof ArrayAcc) {
			ArrayAcc arrayAcc = (ArrayAcc) sketch;
			// match array if it is match point
			if (arrayAcc._array.isKeyPoint()) {
				_array.bindingSketch(arrayAcc._array);
			}
			// match another part
			if (arrayAcc._index.isKeyPoint()) {
				_index.bindingSketch(arrayAcc._index);
			}
			return true;
		}
		return false;
	}
	
//	@Override
//	public Map<Expr, Expr> searchPattern(Node pattern) {
//		if(pattern instanceof ArrayAcc) {
//			ArrayAcc arrayAcc = (ArrayAcc) pattern;
//			Map<Expr, Expr> map = _array.searchPattern(arrayAcc._array);
//			Map<Expr, Expr> map2 = _index.searchPattern(arrayAcc._index);
//		}
//		return null;
//	}
	
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_index.resetAllNodeTypeMatch();
		_array.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_index.setAllNodeTypeMatch();
		_array.setAllNodeTypeMatch();
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_array.getFeatureVector());
		_fVector.combineFeature(_index.getFeatureVector());
		_fVector.inc(FVector.INDEX_OP_ACC);
	}
	
}
