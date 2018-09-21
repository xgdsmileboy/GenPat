/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.Matcher;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.VarDeclarationExpr;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class TryStmt extends Stmt {

	private List<VarDeclarationExpr> _resource = null;
	private Blk _blk = null;
	private List<CatClause> _catches = null;
	private Blk _finallyBlk = null;
	
	/**
	 * TryStatement:
     *	try [ ( Resources ) ]
     *	    Block
     *	    [ { CatchClause } ]
     *	    [ finally Block ]
	 */
	public TryStmt(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
	}

	public TryStmt(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
		_nodeType = TYPE.TRY;
	}
	
	public void setResource(List<VarDeclarationExpr> resource) {
		_resource = resource;
	}
	
	public void setBody(Blk blk){
		_blk = blk;
	}
	
	public void setCatchClause(List<CatClause> catches) {
		_catches = catches;
	}
	
	public void setFinallyBlock(Blk finallyBlk) {
		_finallyBlk = finallyBlk;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("try");
		if(_resource != null && _resource.size() > 0) {
			stringBuffer.append("(");
			stringBuffer.append(_resource.get(0).toSrcString());
			for(int i = 1; i < _resource.size(); i++) {
				stringBuffer.append(";");
				stringBuffer.append(_resource.get(i).toSrcString());
			}
			stringBuffer.append(")");
		}
		stringBuffer.append(_blk.toSrcString());
		if(_catches != null) {
			for(CatClause catClause : _catches) {
				stringBuffer.append(catClause.toSrcString());
			}
		}
		if(_finallyBlk != null) {
			stringBuffer.append("finally");
			stringBuffer.append(_finallyBlk.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		if(_binding != null && _binding instanceof TryStmt) {
			TryStmt tryStmt = (TryStmt) _binding;
			List<Modification> resourceModifications = new LinkedList<>();
			List<Modification> catchModifications = new LinkedList<>();
			StringBuffer finallyBlock = null;
			for(Modification modification : tryStmt.getNodeModification()) {
				if(modification instanceof Update){
					Update update = (Update) modification;
					Node node = update.getSrcNode();
					if(node == tryStmt._finallyBlk) {
						finallyBlock = update.getTarString(exprMap, allUsableVars);
						if(finallyBlock == null) return null;
					} else if(node.getParentStmt() == tryStmt){
						resourceModifications.add(modification);
					} else {
						catchModifications.add(modification);
					}
				} else{
					if(modification.getParent().getParentStmt() == tryStmt){
						resourceModifications.add(modification);
					} else {
						catchModifications.add(modification);
					}
				}
			}
			
			StringBuffer stringBuffer = new StringBuffer("try");
			StringBuffer tmp = null;
			if(_resource != null && _resource.size() > 0) {
				if(resourceModifications.size() > 0) {
					Map<Node, List<StringBuffer>> insertionPositionMap = new HashMap<>();
					Map<Node, StringBuffer> map = new HashMap<>(_resource.size());
					if (!Matcher.applyStmtList(resourceModifications, _resource, this, exprMap, insertionPositionMap,
							map, allUsableVars)) {
						return null;
					}
					
					stringBuffer.append("(");
					boolean first = true;
					for(int index = 0; index < _resource.size(); index ++) {
						Node node = _resource.get(index);
						List<StringBuffer> list = insertionPositionMap.get(node);
						if (list != null) {
							for (int i = 0; i < list.size(); i++) {
								if(!first) {
									stringBuffer.append(";");
								}
								first = false;
								stringBuffer.append(list.get(i));
								stringBuffer.append(Constant.NEW_LINE);
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
								stringBuffer.append(Constant.NEW_LINE);
							}
						} else {
							if(!first) {
								stringBuffer.append(",");
							}
							first = false;
							tmp = node.applyChange(exprMap, allUsableVars);
							if(tmp == null) return null;
							stringBuffer.append(tmp);
							stringBuffer.append(Constant.NEW_LINE);
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
					stringBuffer.append(")");
				} else {
					stringBuffer.append("(");
					tmp = _resource.get(0).applyChange(exprMap, allUsableVars);
					if(tmp == null) return null;
					stringBuffer.append(tmp);
					for(int i = 1; i < _resource.size(); i++) {
						stringBuffer.append(";");
						tmp = _resource.get(i).applyChange(exprMap, allUsableVars);
						if(tmp == null) return null;
						stringBuffer.append(tmp);
					}
					stringBuffer.append(")");
				}
			}
			
			tmp = _blk.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			
			
			if(_catches != null && _catches.size() > 0) {
				if(catchModifications.size() > 0) {
					Map<Node, List<StringBuffer>> insertionPositionMap = new HashMap<>();
					Map<Node, StringBuffer> map = new HashMap<>(_catches.size());
					if (!Matcher.applyStmtList(catchModifications, _catches, this, exprMap, insertionPositionMap, map,
							allUsableVars)) {
						return null;
					}
					for (Node node : _catches) {
						List<StringBuffer> list = insertionPositionMap.get(node);
						if (list != null) {
							for (int i = 0; i < list.size(); i++) {
								stringBuffer.append(list.get(i));
								stringBuffer.append(Constant.NEW_LINE);
							}
						}
						if (map.containsKey(node)) {
							StringBuffer update = map.get(node);
							if (update != null) {
								stringBuffer.append(update);
								stringBuffer.append(Constant.NEW_LINE);
							}
						} else {
							tmp = node.applyChange(exprMap, allUsableVars);
							if(tmp == null) return null;
							stringBuffer.append(tmp);
							stringBuffer.append(Constant.NEW_LINE);
						}
					}
					List<StringBuffer> list = insertionPositionMap.get(this);
					if(list != null) {
						for(int i = 0; i < list.size(); i ++) {
							stringBuffer.append(list.get(i));
						}
					}
				} else {
					for(CatClause catClause : _catches) {
						tmp = catClause.applyChange(exprMap, allUsableVars);
						if(tmp == null) return null;
						stringBuffer.append(tmp);
					}
				}
			}
			if(finallyBlock == null) {
				if(_finallyBlk != null) {
					stringBuffer.append("finally");
					tmp = _finallyBlk.applyChange(exprMap, allUsableVars);
					if(tmp == null) return null;
					stringBuffer.append(tmp);
				}
			} else {
				stringBuffer.append("finally");
				stringBuffer.append(finallyBlock);
			}
			return stringBuffer;
			
		} else {
			StringBuffer stringBuffer = new StringBuffer("try");
			StringBuffer tmp = null;
			if(_resource != null && _resource.size() > 0) {
				stringBuffer.append("(");
				tmp = _resource.get(0).applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				for(int i = 1; i < _resource.size(); i++) {
					stringBuffer.append(";");
					tmp = _resource.get(i).applyChange(exprMap, allUsableVars);
					if(tmp == null) return null;
					stringBuffer.append(tmp);
				}
				stringBuffer.append(")");
			}
			tmp = _blk.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			if(_catches != null) {
				for(CatClause catClause : _catches) {
					tmp = catClause.applyChange(exprMap, allUsableVars);
					if(tmp == null) return null;
					stringBuffer.append(tmp);
				}
			}
			if(_finallyBlk != null) {
				stringBuffer.append("finally");
				tmp = _finallyBlk.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			return stringBuffer;
		}
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer stringBuffer = new StringBuffer("try");
		StringBuffer tmp = null;
		if(_resource != null && _resource.size() > 0) {
			stringBuffer.append("(");
			tmp = _resource.get(0).replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			for(int i = 1; i < _resource.size(); i++) {
				stringBuffer.append(";");
				tmp = _resource.get(i).replace(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
			stringBuffer.append(")");
		}
		tmp = _blk.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		if(_catches != null) {
			for(CatClause catClause : _catches) {
				tmp = catClause.replace(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		}
		if(_finallyBlk != null) {
			stringBuffer.append("finally");
			tmp = _finallyBlk.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append("try");
			if(_resource != null && _resource.size() > 0) {
				stringBuffer.append("(");
				stringBuffer.append(_resource.get(0).printMatchSketch());
				for(int i = 1; i < _resource.size(); i++) {
					stringBuffer.append(";");
					stringBuffer.append(_resource.get(i).printMatchSketch());
				}
				stringBuffer.append(")");
			}
			stringBuffer.append(_blk.printMatchSketch());
			if(_catches != null) {
				for(CatClause catClause : _catches) {
					stringBuffer.append(catClause.printMatchSketch());
				}
			}
			if(_finallyBlk != null) {
				stringBuffer.append("finally");
				stringBuffer.append(_finallyBlk.printMatchSketch());
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER + ";");
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("try");
		if(_resource != null && _resource.size() > 0) {
			_tokens.add("(");
			_tokens.addAll(_resource.get(0).tokens());
			for(int i = 1; i < _resource.size(); i++) {
				_tokens.add(";");
				_tokens.addAll(_resource.get(i).tokens());
			}
			_tokens.add(")");
		}
		_tokens.addAll(_blk.tokens());
		if(_catches != null) {
			for(CatClause catClause : _catches) {
				_tokens.addAll(catClause.tokens());
			}
		}
		if(_finallyBlk != null) {
			_tokens.add("finally");
			_tokens.addAll(_finallyBlk.tokens());
		}
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>();
		if(_resource != null) {
			children.addAll(_resource);
		}
		children.add(_blk);
		if(_catches != null) {
			children.addAll(_catches);
		}
		if(_finallyBlk != null) {
			children.add(_finallyBlk);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_blk);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof TryStmt) {
			TryStmt tryStmt = (TryStmt) other;
			if(_resource == null) {
				match = (tryStmt._resource == null);
			} else {
				if(tryStmt._resource == null) {
					match = false;
				} else {
					match = (_resource.size() == tryStmt._resource.size());
					for(int i = 0; match && i < _resource.size(); i++) {
						match = match && _resource.get(i).compare(tryStmt._resource.get(i));
					}
				}
			}
			// body
			match = match && _blk.compare(tryStmt._blk);
			// catch clause
			if(_catches != null) {
				if(tryStmt._catches != null) {
					match = match && (_catches.size() == tryStmt._catches.size());
					for(int i = 0; match && i < _catches.size(); i ++) {
						match = match && _catches.get(i).compare(tryStmt._catches.get(i));
					}
				} else {
					match = false;
				}
			} else {
				match = match && (tryStmt._catches == null);
			}
			// finally block
			if(_finallyBlk == null) {
				match = match && (tryStmt._finallyBlk == null);
			} else {
				if(tryStmt._finallyBlk == null) {
					match = false;
				} else {
					match = match && _finallyBlk.compare(tryStmt._finallyBlk);
				}
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = _blk.getKeywords();
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_blk.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof TryStmt) {
			_matchNodeType = true;
			TryStmt tryStmt = (TryStmt) other;
			if ((_resource == null && tryStmt._resource != null) || (_catches == null && tryStmt._catches != null)
					|| (_finallyBlk == null && tryStmt._finallyBlk != null)) {
				_matchNodeType = false;
				return;
			}
			if (_resource != null && tryStmt._resource != null) {
				_modifications.addAll(Matcher.matchNodeList(this, _resource, tryStmt._resource));
			} else if(_resource != null || tryStmt._resource != null){
				_matchNodeType = false;
			}
			if(_matchNodeType) {
				// body
				_blk.deepMatch(tryStmt._blk);
				// catch clause
				if(_catches != null && tryStmt._catches != null) {
					_modifications.addAll(Matcher.matchNodeList(this, _catches, tryStmt._catches));
				} else if(_catches != null || tryStmt._catches != null){
					_matchNodeType = false;
				}
				if(_matchNodeType) {
					// finally block
					if(_finallyBlk != null && tryStmt._finallyBlk != null) {
						_finallyBlk.deepMatch(tryStmt._finallyBlk);
					} else if(_finallyBlk != null || tryStmt._finallyBlk != null) {
						Update update = new Update(this, _finallyBlk, tryStmt._finallyBlk);
						_modifications.add(update);
					}
				}
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof TryStmt) {
			match = true;
			TryStmt tryStmt = (TryStmt) sketch;
			if(!tryStmt.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					match = false;
				} else {
					bindingSketch(sketch);
				}
			} else {
				// match resource 
				if(tryStmt._resource != null && tryStmt._resource.size() > 0) {
					Set<Integer> alreadyMatch = new HashSet<>();
					for(VarDeclarationExpr varDeclarationExpr : tryStmt._resource) {
						if(varDeclarationExpr.isKeyPoint()) {
							if(_resource == null || _resource.size() <= 0) {
								match = false;
								break;
							}
							boolean singleMatch = false;
							for(int i = 0; i < _resource.size(); i++) {
								if(alreadyMatch.contains(i)) {
									continue;
								}
								if(_resource.get(i).matchSketch(varDeclarationExpr)) {
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
				// match body
				if(match && tryStmt._blk.isKeyPoint()) {
					match = _blk.matchSketch(tryStmt._blk);
				}
				
				if(tryStmt._catches != null && tryStmt._catches.size() > 0) {
					Set<Integer> alreadyMatch = new HashSet<>();
					for(CatClause catClause : tryStmt._catches) {
						if(catClause.isKeyPoint()) {
							if(_catches == null || _catches.size() <= 0) {
								match = false;
								break;
							}
							boolean singleMatch = false;
							for(int i = 0; i < _catches.size(); i++) {
								if(alreadyMatch.contains(i)) {
									continue;
								}
								if(_catches.get(i).matchSketch(catClause)){
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
				
				if(match && tryStmt._finallyBlk != null && tryStmt._finallyBlk.isKeyPoint()) {
					if(_finallyBlk == null) {
						match = false;
					} else {
						match = _finallyBlk.matchSketch(tryStmt._finallyBlk);
					}
				}
			}
			if(match) {
				tryStmt._binding = this;
				_binding = tryStmt;
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
		if (sketch instanceof TryStmt) {
			match = true;
			TryStmt tryStmt = (TryStmt) sketch;
			// bind resource
			if (tryStmt._resource != null && tryStmt._resource.size() > 0) {
				Set<Integer> alreadyMatch = new HashSet<>();
				for (VarDeclarationExpr varDeclarationExpr : tryStmt._resource) {
					if (varDeclarationExpr.isKeyPoint()) {
						if (_resource == null || _resource.size() <= 0) {
							break;
						}
						for (int i = 0; i < _resource.size(); i++) {
							if (alreadyMatch.contains(i)) {
								continue;
							}
							if (_resource.get(i).bindingSketch(varDeclarationExpr)) {
								alreadyMatch.add(i);
								break;
							} else {
								varDeclarationExpr.resetBinding();
							}
						}
					}
				}
			}
			// bind body
			if (tryStmt._blk.isKeyPoint()) {
				_blk.bindingSketch(tryStmt._blk);
			}

			if (tryStmt._catches != null && tryStmt._catches.size() > 0) {
				Set<Integer> alreadyMatch = new HashSet<>();
				for (CatClause catClause : tryStmt._catches) {
					if (catClause.isKeyPoint()) {
						if (_catches == null || _catches.size() <= 0) {
							break;
						}
						for (int i = 0; i < _catches.size(); i++) {
							if (alreadyMatch.contains(i)) {
								continue;
							}
							if (_catches.get(i).bindingSketch(catClause)) {
								alreadyMatch.add(i);
								break;
							} else {
								catClause.resetBinding();
							}
						}
					}
				}
			}

			if (tryStmt._finallyBlk != null && tryStmt._finallyBlk.isKeyPoint()) {
				if (_finallyBlk != null) {
					_finallyBlk.bindingSketch(tryStmt._finallyBlk);
				}
			}
		}
		return match;
	}
	
	@Override
	public Node bindingNode(Node patternNode) {
		boolean match = false;
		if(patternNode instanceof TryStmt) {
			match = true;
			Map<String, Set<Node>> map = patternNode.getKeywords();
			Map<String, Set<Node>> thisKeys = getKeywords();
			for(Entry<String, Set<Node>> entry : map.entrySet()) {
				if(!thisKeys.containsKey(entry.getKey())) {
					match = false;
					break;
				}
			}
		}
		if(match) {
			return this;
		} else {
			Node node = _blk.bindingNode(patternNode);
			if(node == null && _finallyBlk != null) {
				node = _finallyBlk.bindingNode(patternNode);
			}
			return node;
			
		}
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		if(_resource != null) {
			for(VarDeclarationExpr expr : _resource) {
				expr.resetAllNodeTypeMatch();
			}
		}
		_blk.resetAllNodeTypeMatch();
		if(_catches != null) {
			for(CatClause catClause : _catches) {
				catClause.resetAllNodeTypeMatch();
			}
		}
		if(_finallyBlk != null) {
			_finallyBlk.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		if(_resource != null) {
			for(VarDeclarationExpr expr : _resource) {
				expr.setAllNodeTypeMatch();
			}
		}
		_blk.setAllNodeTypeMatch();
		if(_catches != null) {
			for(CatClause catClause : _catches) {
				catClause.setAllNodeTypeMatch();
			}
		}
		if(_finallyBlk != null) {
			_finallyBlk.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_STRUCT_TRY);
		if(_resource != null) {
			for(VarDeclarationExpr vdExpr : _resource) {
				_fVector.combineFeature(vdExpr.getFeatureVector());
			}
		}
		_fVector.combineFeature(_blk.getFeatureVector());
		if(_catches != null) {
			for(CatClause catClause : _catches) {
				_fVector.combineFeature(catClause.getFeatureVector());
			}
		}
		if(_finallyBlk != null) {
			_fVector.combineFeature(_finallyBlk.getFeatureVector());
		}
	}
	
	
}
