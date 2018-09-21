/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node;

import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.node.expr.Expr;
import mfix.core.parse.node.expr.SName;
import mfix.core.parse.node.stmt.Blk;
import mfix.core.parse.node.stmt.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class MethDecl extends Node {

	private List<Modifier> _modifiers = new ArrayList<>(5);
	private Type _retType;
	private SName _name;
	private List<Expr> _arguments;
	private Blk _body;
	private List<Object> _throws;
	
	public MethDecl(int startLine, int endLine, ASTNode oriNode) {
		super(startLine, endLine, oriNode);
		_nodeType = TYPE.METHDECL;
		_retType = AST.newAST(AST.JLS8).newWildcardType(); 
	}
	
	public void setModifiers(List<Modifier> modifiers) {
		_modifiers = modifiers;
	}
	
	public List<Modifier> getModifiers() {
		return _modifiers;
	}
	
	public void setThrows(List<Object> throwTypes) {
		_throws = throwTypes;
	}
	
	public void setRetType(Type type) {
		if(type != null) {
			_retType = type;
		}
	}
	
	public Type getRetType() {
		return _retType;
	}
	
	public void setName(SName name) {
		_name = name; 
	}
	
	public SName getName() {
		return _name;
	}
	
	public void setArguments(List<Expr> arguments) {
		_arguments = arguments;
	}
	
	public List<Expr> getArguments() {
		return _arguments;
	}
	
	public void setBody(Blk body) {
		_body = body;
	}
	
	public Blk getBody() {
		return _body;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		for(Object modifier : _modifiers) {
			stringBuffer.append(modifier.toString() + " ");
		}
		if(!_retType.toString().equals("?")) {
			stringBuffer.append(_retType + " ");
		}
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		if(_arguments != null && _arguments.size() > 0) {
			stringBuffer.append(_arguments.get(0).toSrcString());
			for(int i = 1; i < _arguments.size(); i++) {
				stringBuffer.append("," + _arguments.get(i).toSrcString());
			}
		}
		stringBuffer.append(")");
		if(_throws != null && _throws.size() > 0) {
			stringBuffer.append(" throws " + _throws.get(0).toString());
			for(int i = 1; i < _throws.size(); i++) {
				stringBuffer.append("," + _throws.get(i).toString());
			}
		}
		if(_body == null) {
			stringBuffer.append(";");
		} else {
			stringBuffer.append(_body.toSrcString());
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> usableVars) {
		StringBuffer stringBuffer = new StringBuffer();
		for(Object modifier : _modifiers) {
			stringBuffer.append(modifier.toString() + " ");
		}
		if(!_retType.toString().equals("?")) {
			stringBuffer.append(_retType + " ");
		}
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		if(_arguments != null && _arguments.size() > 0) {
			stringBuffer.append(_arguments.get(0).toSrcString());
			for(int i = 1; i < _arguments.size(); i++) {
				stringBuffer.append("," + _arguments.get(i).toSrcString());
			}
		}
		stringBuffer.append(")");
		if(_throws != null && _throws.size() > 0) {
			stringBuffer.append(" throws " + _throws.get(0).toString());
			for(int i = 1; i < _throws.size(); i++) {
				stringBuffer.append("," + _throws.get(i).toString());
			}
		}
		if(_body == null) {
			stringBuffer.append(";");
		} else {
			StringBuffer tmp = _body.applyChange(exprMap, usableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		}
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		return null;
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		
		for(Object modifier : _modifiers) {
			_tokens.add(modifier.toString());
		}
		if(!_retType.toString().equals("?")) {
			_tokens.add(_retType.toString());
		}
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		if(_arguments != null && _arguments.size() > 0) {
			_tokens.addAll(_arguments.get(0).tokens());
			for(int i = 1; i < _arguments.size(); i++) {
				_tokens.addAll(_arguments.get(i).tokens());
			}
		}
		_tokens.add(")");
		if(_body == null) {
			_tokens.add(";");
		} else {
			_tokens.addAll(_body.tokens());
		}
	}
	
	@Override
	public Stmt getParentStmt() {
		return null;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		for(Object modifier : _modifiers) {
			stringBuffer.append(modifier.toString() + " ");
		}
		if(!_retType.toString().equals("?")) {
			stringBuffer.append(_retType + " ");
		}
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		if(_arguments != null && _arguments.size() > 0) {
			stringBuffer.append(_arguments.get(0).toSrcString());
			for(int i = 1; i < _arguments.size(); i++) {
				stringBuffer.append("," + _arguments.get(i).toSrcString());
			}
		}
		stringBuffer.append(")");
		if(_body == null) {
			stringBuffer.append(";");
		} else {
			stringBuffer.append(_body.printMatchSketch());
		}
		return stringBuffer;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		if(_body != null) {
			children.add(_body);
		}
		return children;
	}
	
	@Override
	public List<Node> getAllChildren() {
		if(_body == null) {
			return new ArrayList<>(0);
		} else {
			List<Node> children = new ArrayList<>(1);
			children.add(_body);
			return children;
		}
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other instanceof MethDecl) {
			MethDecl methDecl = (MethDecl) other;
			match = _name.compare(methDecl._name) && (_arguments.size() == methDecl._arguments.size());
			for(int i = 0; match && i < _arguments.size(); i++) {
				match = match && _arguments.get(i).compare(methDecl._arguments.get(i));
			}
			if(_body != null && methDecl.getBody() != null){
				_body.compare(methDecl.getBody());
			}
		}
		return match;
	}
	
	@Override
	public void deepMatch(Node other) {
		// TODO :
		_tarNode = other;
		if(other instanceof MethDecl) {
			_matchNodeType = true;
			MethDecl methDecl = (MethDecl) other;
			_body.deepMatch(methDecl._body);
		} else {
			_matchNodeType = false;
			Update update = new Update(null, this, other);
			_modifications.add(update);
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		if(sketch instanceof MethDecl) {
			MethDecl methDecl = (MethDecl) sketch;
			if(_body != null && methDecl != null) {
				if(_body.matchSketch(methDecl._body)){
					methDecl._binding = this;
					_binding = methDecl;
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean bindingSketch(Node sketch) {
		return false;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			if(_body != null) {
				_keywords = _body.getKeywords();
			} else {
				_keywords = new HashMap<>(0);
			}
		}
		return _keywords;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		modifications.addAll(_modifications);
		if(_matchNodeType) {
			modifications.addAll(_body.extractModifications());
		}
		return modifications;
	}

	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_name.resetAllNodeTypeMatch();
		for(Expr expr : _arguments) {
			expr.resetAllNodeTypeMatch();
		}
		if(_body != null) {
			_body.resetAllNodeTypeMatch();
		}
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_name.setAllNodeTypeMatch();
		for(Expr expr : _arguments) {
			expr.setAllNodeTypeMatch();
		}
		if(_body != null) {
			_body.setAllNodeTypeMatch();
		}
	}

	@Override
	public void computeFeatureVector() {
		// TODO Auto-generated method stub
		
	}

}
