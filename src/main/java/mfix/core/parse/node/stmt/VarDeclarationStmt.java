/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import mfix.common.config.Constant;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.Matcher;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Assign;
import mfix.core.parse.node.expr.MType;
import mfix.core.parse.node.expr.Vdf;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class VarDeclarationStmt extends Stmt {

	private String _modifier = null;
	private MType _declType = null;
	private List<Vdf> _fragments = null;

	/**
	 * VariableDeclarationStatement: { ExtendedModifier } Type
	 * VariableDeclarationFragment { , VariableDeclarationFragment } ;
	 */
	public VarDeclarationStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}

	public VarDeclarationStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
		_nodeType = TYPE.VARDECLSTMT;
	}

	/**
	 * @param modifier
	 *            the modifier to set
	 */
	public void setModifier(String modifier) {
		this._modifier = modifier;
	}

	public void setDeclType(MType declType) {
		_declType = declType;
	}

	public void setFragments(List<Vdf> fragments) {
		_fragments = fragments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_modifier != null) {
			stringBuffer.append(_modifier + " ");
		}
		stringBuffer.append(_declType.toSrcString());
		stringBuffer.append(" ");
		stringBuffer.append(_fragments.get(0).toSrcString());
		for (int i = 1; i < _fragments.size(); i++) {
			stringBuffer.append(",");
			stringBuffer.append(_fragments.get(i).toSrcString());
		}
		stringBuffer.append(";");
		return stringBuffer;
	}

	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		if (_binding != null && _binding instanceof VarDeclarationStmt) {
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) _binding;
			StringBuffer declType = null;
			List<Modification> modifications = new LinkedList<>();
			for (Modification modification : varDeclarationStmt.getNodeModification()) {
				if (modification instanceof Update) {
					Update update = (Update) modification;
					if (update.getSrcNode() == varDeclarationStmt._declType) {
						declType = update.getTarString(exprMap, allUsableVars);
						if (declType == null)
							return null;
					} else {
						modifications.add(update);
					}
				} else {
					modifications.add(modification);
				}
			}

			Map<Node, List<StringBuffer>> insertionPositionMap = new HashMap<>();
			Map<Node, StringBuffer> map = new HashMap<>(_fragments.size());
			if (!Matcher.applyStmtList(modifications, _fragments, this, exprMap, insertionPositionMap, map,
					allUsableVars)) {
				return null;
			}

			StringBuffer stringBuffer = new StringBuffer();
			StringBuffer tmp = null;
			if (_modifier != null) {
				stringBuffer.append(_modifier + " ");
			}
			if(declType == null) {
				tmp = _declType.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			} else {
				stringBuffer.append(declType);
			}
			stringBuffer.append(" ");
			
			boolean first = true;
			for(int index = 0; index < _fragments.size(); index ++) {
				Node node = _fragments.get(index);
				List<StringBuffer> list = insertionPositionMap.get(node);
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						if(!first) {
							stringBuffer.append(",");
						}
						first = false;
						stringBuffer.append(list.get(i));
					}
				}
				if (map.containsKey(node)) {
					StringBuffer update = map.get(node);
					if (update != null) {
						if(!first) {
							stringBuffer.append(",");
						}
						first = false;
						stringBuffer.append(update);
					}
				} else {
					if(!first) {
						stringBuffer.append(",");
					}
					first = false;
					tmp = node.applyChange(exprMap, allUsableVars);
					if(tmp == null) return null;
					stringBuffer.append(tmp);
				}
			}
			List<StringBuffer> list = insertionPositionMap.get(this);
			if(list != null) {
				for(int i = 0; i < list.size(); i ++) {
					if(!first) {
						stringBuffer.append(",");
					}
					first = false;
					stringBuffer.append(list.get(i));
				}
			}
			stringBuffer.append(";");
			return stringBuffer;
		} else {
			StringBuffer stringBuffer = new StringBuffer();
			StringBuffer tmp = null;
			if (_modifier != null) {
				stringBuffer.append(_modifier + " ");
			}
			tmp = _declType.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(" ");
			tmp = _fragments.get(0).applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for (int i = 1; i < _fragments.size(); i++) {
				stringBuffer.append(",");
				tmp = _fragments.get(i).applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			stringBuffer.append(";");
			return stringBuffer;
		}
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		if (_modifier != null) {
			stringBuffer.append(_modifier + " ");
		}
		StringBuffer tmp = _declType.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(" ");
		tmp = _fragments.get(0).replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		for (int i = 1; i < _fragments.size(); i++) {
			stringBuffer.append(",");
			tmp = _fragments.get(i).replace(exprMap, allUsableVars);
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
			if (_modifier != null) {
				stringBuffer.append(_modifier + " ");
			}
			stringBuffer.append(_declType.printMatchSketch());
			stringBuffer.append(" ");
			stringBuffer.append(_fragments.get(0).printMatchSketch());
			for (int i = 1; i < _fragments.size(); i++) {
				stringBuffer.append(",");
				stringBuffer.append(_fragments.get(i).printMatchSketch());
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
		if (_modifier != null) {
			_tokens.add(_modifier);
		}
		_tokens.addAll(_declType.tokens());
		_tokens.addAll(_fragments.get(0).tokens());
		for (int i = 1; i < _fragments.size(); i++) {
			_tokens.add(",");
			_tokens.addAll(_fragments.get(i).tokens());
		}
		_tokens.add(";");
	}
	
	@Override
	public Set<String> getNewVars() {
		Set<String> vars = new HashSet<>();
		for(Vdf vdf : _fragments) {
			vars.add(vdf.getName());
		}
		return vars;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_fragments.size() + 1);
		children.add(_declType);
		children.addAll(_fragments);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof VarDeclarationStmt) {
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) other;
			match = _declType.compare(varDeclarationStmt._declType);
			if (_modifier == null) {
				match = match && (varDeclarationStmt._modifier == null);
			} else {
				if (varDeclarationStmt._modifier == null) {
					match = false;
				} else {
					match = match && _modifier.equals(varDeclarationStmt._modifier);
				}
			}
			match = match && (_fragments.size() == varDeclarationStmt._fragments.size());
			for (int i = 0; match && i < _fragments.size(); i++) {
				match = match && _fragments.get(i).compare(varDeclarationStmt._fragments.get(i));
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			for(Vdf vdf : _fragments) {
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
			for(Vdf vdf : _fragments) {
				modifications.addAll(vdf.extractModifications());
			}
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof VarDeclarationStmt) {
			_matchNodeType = true;
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) other;
			if (_modifier != null && varDeclarationStmt._modifier != null) {
				_matchNodeType = _modifier.equals(varDeclarationStmt._modifier);
			} else if(_modifier != null && varDeclarationStmt._modifier != null) {
				_matchNodeType = false;
			}
			if(_matchNodeType) {
				_declType.deepMatch(varDeclarationStmt._declType);
				if(!_declType.isNodeTypeMatch()) {
					Update update = new Update(this, _declType, varDeclarationStmt._declType);
					_modifications.add(update);
				}
				_modifications.addAll(Matcher.matchNodeList(this, _fragments, varDeclarationStmt._fragments));
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof VarDeclarationStmt) {
			match = true;
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) sketch;
			if(!varDeclarationStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(varDeclarationStmt._declType.isKeyPoint()) {
					match = _declType.matchSketch(varDeclarationStmt._declType);
				}
				if(match) {
					Set<Integer> alreadyMatch = new HashSet<>();
					for(Vdf vdf : varDeclarationStmt._fragments) {
						if(vdf.isKeyPoint()) {
							boolean singleMatch = false;
							for(int i = 0; i < _fragments.size(); i++) {
								if(alreadyMatch.contains(i)) continue;
								if(_fragments.get(i).matchSketch(vdf)) {
									singleMatch = true;
									alreadyMatch.add(i);
									break;
								}
							}
							if(!singleMatch) {
								match = false;
								break;
							}
						}
					}
				}
			}
			if(match) {
				varDeclarationStmt._binding = this;
				_binding = varDeclarationStmt;
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
		if (sketch instanceof VarDeclarationStmt) {
			match = true;
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) sketch;
			if (varDeclarationStmt._declType.isKeyPoint()) {
				_declType.bindingSketch(varDeclarationStmt._declType);
			}
			Set<Integer> alreadyMatch = new HashSet<>();
			for (Vdf vdf : varDeclarationStmt._fragments) {
				if (vdf.isKeyPoint()) {
					for (int i = 0; i < _fragments.size(); i++) {
						if (alreadyMatch.contains(i))
							continue;
						if (_fragments.get(i).bindingSketch(vdf)) {
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
	public Node bindingNode(Node patternNode) {
		if ((patternNode instanceof VarDeclarationStmt) || (patternNode instanceof ExpressionStmt
				&& ((ExpressionStmt) patternNode).getExpression() instanceof Assign)) {
			Map<String, Set<Node>> map = patternNode.getKeywords();
			Map<String, Set<Node>> thisKeys = getKeywords();
			for(Entry<String, Set<Node>> entry : map.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					return null;
				}
			}
			return this;
		}
		return null;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_declType.resetAllNodeTypeMatch();
		for(Vdf vdf : _fragments) {
			vdf.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_declType.setAllNodeTypeMatch();
		for(Vdf vdf : _fragments) {
			vdf.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		for(Vdf vdf : _fragments) {
			_fVector.combineFeature(vdf.getFeatureVector());
		}
	}
	
}
