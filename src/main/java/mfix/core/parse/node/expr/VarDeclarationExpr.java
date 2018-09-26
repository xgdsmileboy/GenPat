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
import mfix.core.parse.Matcher;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class VarDeclarationExpr extends Expr {

	private MType _declType = null;
	private List<Vdf> _vdfs = null;
	
	
	/**
	 * VariableDeclarationExpression:
     *	{ ExtendedModifier } Type VariableDeclarationFragment
     *	    { , VariableDeclarationFragment }
	 */
	public VarDeclarationExpr(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.VARDECLEXPR;
	}
	
	public void setDeclType(MType declType){
		_declType = declType;
	}
	
	public void setVarDeclFrags(List<Vdf> vdfs){
		_vdfs = vdfs;
	}

	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_declType.toSrcString());
		stringBuffer.append(" ");
		stringBuffer.append(_vdfs.get(0).toSrcString());
		for (int i = 1; i < _vdfs.size(); i++) {
			stringBuffer.append(",");
			stringBuffer.append(_vdfs.get(i).toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer declType = null;
		if(_binding != null && _binding instanceof VarDeclarationExpr) {
			VarDeclarationExpr varDeclarationExpr = (VarDeclarationExpr) _binding;
			for(Modification modification : varDeclarationExpr.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					if(update.getSrcNode() == varDeclarationExpr._declType) {
						declType = update.getTarString(exprMap, allUsableVars);
						if(declType == null) return null;
					} else {
						LevelLogger.error("@VarDeclarationExpr ERROR");
					}
				} else {
					LevelLogger.error("@VarDeclarationExpr Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(declType == null) {
			tmp = _declType.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(declType);
		}
		stringBuffer.append(" ");
		tmp = _vdfs.get(0).applyChange(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		for (int i = 1; i < _vdfs.size(); i++) {
			stringBuffer.append(",");
			tmp = _vdfs.get(i).applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		String result = exprMap.get(toSrcString().toString());
		if(result != null) {
			return new StringBuffer(result);
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = _declType.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(" ");
		tmp = _vdfs.get(0).replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		for (int i = 1; i < _vdfs.size(); i++) {
			stringBuffer.append(",");
			tmp = _vdfs.get(i).replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_declType.printMatchSketch());
			stringBuffer.append(" ");
			stringBuffer.append(_vdfs.get(0).printMatchSketch());
			for (int i = 1; i < _vdfs.size(); i++) {
				stringBuffer.append(",");
				stringBuffer.append(_vdfs.get(i).printMatchSketch());
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_declType.tokens());
		_tokens.addAll(_vdfs.get(0).tokens());
		for(int i = 1; i < _vdfs.size(); i++ ) {
			_tokens.add(",");
			_tokens.addAll(_vdfs.get(i).tokens());
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof VarDeclarationExpr) {
			VarDeclarationExpr varDeclarationExpr = (VarDeclarationExpr) other;
			match = _declType.compare(varDeclarationExpr._declType);
			match = match && (_vdfs.size() == varDeclarationExpr._vdfs.size());
			for(int i = 0; match && i < _vdfs.size(); i++) {
				match = match && _vdfs.get(i).compare(varDeclarationExpr._vdfs.get(i));
			}
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_vdfs.size() + 1);
		children.add(_declType);
		children.addAll(_vdfs);
		return children;
	}
	
	@Override
	public Map<String, Set<Node>> getCalledMethods() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			for(Vdf vdf : _vdfs) {
				avoidDuplicate(_keywords, vdf);
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			for(Vdf vdf : _vdfs) {
				modifications.addAll(vdf.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof VarDeclarationExpr) {
			_matchNodeType = true;
			VarDeclarationExpr vde = (VarDeclarationExpr) other;
			if(!Matcher.matchNodeList(this, _vdfs, vde._vdfs).isEmpty()){
				_matchNodeType = false;
				return;
			}
			_declType.deepMatch(vde._declType);
			if(!_declType.isNodeTypeMatch()){
				Update update = new Update(this, _declType, vde._declType);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof VarDeclarationExpr) {
			match = true;
			VarDeclarationExpr varDeclarationExpr = (VarDeclarationExpr) sketch;
			if(!varDeclarationExpr.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(varDeclarationExpr._declType.isKeyPoint()) {
					match = _declType.matchSketch(varDeclarationExpr._declType);
				}
				if(match) {
					Set<Integer> alreadyMatch = new HashSet<>();
					for(Vdf vdf : varDeclarationExpr._vdfs) {
						if(vdf.isKeyPoint()) {
							boolean singleMatch = false;
							for(int i = 0; i < _vdfs.size(); i++) {
								if(alreadyMatch.contains(i)) {
									continue;
								}
								if(_vdfs.get(i).matchSketch(vdf)) {
									alreadyMatch.add(i);
									singleMatch = true;
									break;
								}
							}
							if(!singleMatch) {
								return false;
							}
						}
					}
				}
			}
			if(match) {
				varDeclarationExpr._binding = this;
				_binding = varDeclarationExpr;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		boolean match = false;
		_binding = sketch;
		sketch.setBinding(this);
		if (sketch instanceof VarDeclarationExpr) {
			match = true;
			VarDeclarationExpr varDeclarationExpr = (VarDeclarationExpr) sketch;
			if (varDeclarationExpr._declType.isKeyPoint()) {
				_declType.bindingSketch(varDeclarationExpr._declType);
			}
			Set<Integer> alreadyMatch = new HashSet<>();
			for (Vdf vdf : varDeclarationExpr._vdfs) {
				if (vdf.isKeyPoint()) {
					for (int i = 0; i < _vdfs.size(); i++) {
						if (alreadyMatch.contains(i)) {
							continue;
						}
						if (_vdfs.get(i).bindingSketch(vdf)) {
							alreadyMatch.add(i);
							break;
						} else {
							vdf.resetBinding();
						}
					}
				}
			}
		}
		return match;
	}

	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_declType.resetAllNodeTypeMatch();
		for(Vdf expr : _vdfs) {
			expr.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_declType.setAllNodeTypeMatch();
		for(Vdf expr : _vdfs) {
			expr.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.combineFeature(_declType.getFeatureVector());
		for(Vdf vdf : _vdfs) {
			_fVector.combineFeature(vdf.getFeatureVector());
		}
	}
}
