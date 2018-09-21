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
import mfix.core.parse.node.stmt.AnonymousClassDecl;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class ClassInstanceCreate extends Expr {

	private Expr _expression = null;
	private MType _classType = null;
	private ExprList _arguments = null;
	private AnonymousClassDecl _decl = null;

	/**
	 * ClassInstanceCreation: [ Expression . ] new [ < Type { , Type } > ] Type
	 * ( [ Expression { , Expression } ] ) [ AnonymousClassDeclaration ]
	 */
	public ClassInstanceCreate(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.CLASSCREATION;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	public void setClassType(MType classType) {
		_classType = classType;
	}

	public void setArguments(ExprList arguments) {
		_arguments = arguments;
	}

	public void setAnonymousClassDecl(AnonymousClassDecl decl) {
		_decl = decl;
	}

	public MType getClassType() {
		return _classType;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression != null) {
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("new ");
		stringBuffer.append(_classType.toSrcString());
		stringBuffer.append("(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(")");
		if (_decl != null) {
			stringBuffer.append(_decl.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer expression = null;
		StringBuffer classType = null;
		StringBuffer arguments = null;
		StringBuffer decl = null;
		if(_binding != null && _binding instanceof ClassInstanceCreate) {
			ClassInstanceCreate classInstanceCreate = (ClassInstanceCreate) _binding;
			for(Modification modification : classInstanceCreate.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update) modification;
					Node node = update.getSrcNode(); 
					if(node == classInstanceCreate._expression) {
						expression = update.getTarString(exprMap, allUsableVars);
						if(expression == null) return null;
					} else if(node == classInstanceCreate._classType) {
						classType = update.getTarString(exprMap, allUsableVars);
						if(classType == null) return null;
					} else if(node == classInstanceCreate._arguments) {
						arguments = update.getTarString(exprMap, allUsableVars);
						if(arguments == null) return null;
					} else if(node == classInstanceCreate._decl) {
						decl = update.getTarString(exprMap, allUsableVars);
						if(decl == null) return null;
					}
				} else {
					LevelLogger.error("@ClassInstanceCreate Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(expression == null) {
			if (_expression != null) {
				tmp = _expression.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
				stringBuffer.append(".");
			}
		} else {
			stringBuffer.append(expression);
		}
		stringBuffer.append("new ");
		if(classType == null) {
			stringBuffer.append(_classType.applyChange(exprMap, allUsableVars));
		} else {
			stringBuffer.append(classType);
		}
		stringBuffer.append("(");
		if(arguments == null) {
			tmp = _arguments.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(arguments);
		}
		stringBuffer.append(")");
		if(decl == null) {
			if (_decl != null) {
				tmp = _decl.applyChange(exprMap, allUsableVars);
				if(tmp == null) return null;
				stringBuffer.append(tmp);
			}
		} else {
			stringBuffer.append(decl);
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
		StringBuffer tmp = null;
		if (_expression != null) {
			tmp = _expression.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
			stringBuffer.append(".");
		}
		stringBuffer.append("new ");
		stringBuffer.append(_classType.replace(exprMap, allUsableVars));
		stringBuffer.append("(");
		tmp = _arguments.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		stringBuffer.append(")");
		if (_decl != null) {
			tmp = _decl.replace(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			if (_expression != null) {
				stringBuffer.append(_expression.printMatchSketch());
				stringBuffer.append(".");
			}
			stringBuffer.append("new ");
			stringBuffer.append(_classType.printMatchSketch());
			stringBuffer.append("(");
			stringBuffer.append(_arguments.printMatchSketch());
			stringBuffer.append(")");
			if (_decl != null) {
				stringBuffer.append(_decl.printMatchSketch());
			}
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_expression != null) {
			_tokens.addAll(_expression.tokens());
			_tokens.add(".");
		}
		_tokens.add("new");
		_tokens.addAll(_classType.tokens());
		_tokens.add("(");
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");
		if (_decl != null) {
			_tokens.addAll(_decl.tokens());
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof ClassInstanceCreate) {
			ClassInstanceCreate classInstanceCreate = (ClassInstanceCreate) other;
			match = _expression == null ? (classInstanceCreate._expression == null) : _expression.compare(classInstanceCreate._expression);
			match = match && _classType.compare(classInstanceCreate._classType) && _arguments.compare(classInstanceCreate._arguments); 
			if(_decl == null) {
				match = match && (classInstanceCreate._decl == null);
			} else {
				match = match && _decl.compare(classInstanceCreate._decl);
			}
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = new HashMap<>(7);
			if(_expression != null) {
				_keywords.putAll(_expression.getKeywords());
			}
			String key = _classType.toSrcString().toString();
			Set<Node> set = _keywords.get(key);
			if(set == null) {
				set = new HashSet<>();
			}
			set.add(_classType);
			_keywords.put(key, set);
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(4);
		if(_expression != null) {
			children.add(_expression);
		}
		children.add(_classType);
		children.add(_arguments);
		if(_decl != null) {
			children.add(_decl);
		}
		return children;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			if(_expression != null) {
				modifications.addAll(_expression.extractModifications());
			}
			modifications.addAll(_arguments.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof ClassInstanceCreate) {
			ClassInstanceCreate classInstanceCreate = (ClassInstanceCreate) other;
			_matchNodeType = true;
			if((_expression == null && classInstanceCreate._expression != null) || (_decl == null && classInstanceCreate._decl != null)){
				_matchNodeType = false;
				return;
			}
			
			_classType.deepMatch(classInstanceCreate._classType);
			if(!_classType.isNodeTypeMatch()) {
				Update update = new Update(this, _classType, classInstanceCreate._classType);
				_modifications.add(update);
			}
			if(_expression != null && classInstanceCreate._expression != null) {
				_expression.deepMatch(classInstanceCreate._expression);
				if(!_expression.isNodeTypeMatch()) {
					Update update = new Update(this, _expression, classInstanceCreate._expression);
					_modifications.add(update);
				}
			} else if(_expression != null) {
				Update update = new Update(this, _expression, classInstanceCreate._expression);
				_modifications.add(update);
			}
			_arguments.deepMatch(classInstanceCreate._arguments);
			if(!_arguments.isNodeTypeMatch()) {
				Update update = new Update(this, _arguments, classInstanceCreate._arguments);
				_modifications.add(update);
			}
			
			if(_decl != null && classInstanceCreate._decl != null) {
				_decl.deepMatch(classInstanceCreate._decl);
				if(!_decl.isNodeTypeMatch()) {
					Update update = new Update(this, _decl, classInstanceCreate._decl);
					_modifications.add(update);
				}
			} else if(_decl != null) {
				Update update = new Update(this, _decl, classInstanceCreate._decl);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof ClassInstanceCreate) {
			ClassInstanceCreate classInstanceCreate = (ClassInstanceCreate) sketch;
			if(!classInstanceCreate.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, this)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(classInstanceCreate._classType.isKeyPoint()) {
					match = _classType.matchSketch(classInstanceCreate._classType);
				}
				if(classInstanceCreate._arguments.isKeyPoint()) {
					match = match && _arguments.matchSketch(classInstanceCreate._arguments);
				}
			}
			if(match) {
				classInstanceCreate._binding = this;
				_binding = classInstanceCreate;
			}
		}
		if(!match) sketch.resetBinding();
		return match;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		_binding = sketch;
		sketch.setBinding(this);
		if(sketch instanceof ClassInstanceCreate) {
			ClassInstanceCreate classInstanceCreate = (ClassInstanceCreate) sketch;
			if(classInstanceCreate._classType.isKeyPoint()) {
				_classType.bindingSketch(classInstanceCreate._classType);
			}
			if(classInstanceCreate._arguments.isKeyPoint()) {
				_arguments.bindingSketch(classInstanceCreate._arguments);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_classType.resetAllNodeTypeMatch();
		if(_expression != null) {
			_expression.resetAllNodeTypeMatch();
		}
		_arguments.resetAllNodeTypeMatch();
		if(_decl != null) {
			_decl.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_classType.setAllNodeTypeMatch();
		if(_expression != null) {
			_expression.setAllNodeTypeMatch();
		}
		_arguments.setAllNodeTypeMatch();
		if(_decl != null) {
			_decl.setAllNodeTypeMatch();
		}
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.INDEX_MCALL);
		if(_expression != null){
			_fVector.combineFeature(_expression.getFeatureVector());
		}
		_fVector.combineFeature(_arguments.getFeatureVector());
	}
}
