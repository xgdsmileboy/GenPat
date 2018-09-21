/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.expr;

import mfix.common.config.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class QName extends Label {

	private Label _name = null;
	private SName _sname = null;
	
	/**
	 * QualifiedName:
     *	Name . SimpleName
	 */
	public QName(int startLine, int endLine, ASTNode node) {
		super(startLine, endLine, node);
		_nodeType = TYPE.QNAME;
	}
	
	public void setName(Label namee, SName sname){
		_name = namee;
		_sname = sname;
	}
	
	public SName getSName() {
		return _sname;
	}
	
	public String getIdentifier(){
		return _sname.getName();
	}
	
	public String getLabel(){
		return _name.toSrcString().toString();
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append(".");
		stringBuffer.append(_sname.toSrcString());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
		StringBuffer name = null;
		if(_binding != null && _binding instanceof QName) {
			QName qName = (QName) _binding;
			for(Modification modification : qName.getNodeModification()) {
				if(modification instanceof Update) {
					Update update = (Update)modification;
					if(update.getSrcNode() == qName._name) {
						name = update.getTarString(exprMap, allUsableVars);
						if(name == null) return null;
					} else {
						LevelLogger.error("@QName ERROR");
					}
				} else {
					LevelLogger.error("@QName Should not be this kind of modification : " + modification);
				}
			}
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = null;
		if(name == null) {
			tmp = _name.applyChange(exprMap, allUsableVars);
			if(tmp == null) return null;
			stringBuffer.append(tmp);
		} else {
			stringBuffer.append(name);
		}
		stringBuffer.append(".");
		stringBuffer.append(_sname.getName());
		return stringBuffer;
	}
	
	@Override
	public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
		String result = exprMap.get(toSrcString().toString());
		if(result != null) {
			return new StringBuffer(result);
		}
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer tmp = _name.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(".");
		tmp = _sname.replace(exprMap, allUsableVars);
		if(tmp == null) return null;
		stringBuffer.append(tmp);
		return stringBuffer;
	}
	
	@Override
	public StringBuffer printMatchSketch() {
		StringBuffer stringBuffer = new StringBuffer();
		if(isKeyPoint()) {
			stringBuffer.append(_name.printMatchSketch());
			stringBuffer.append(".");
			stringBuffer.append(_sname.printMatchSketch());
		} else {
			stringBuffer.append(Constant.PLACE_HOLDER);
		}
		return stringBuffer;
	}
	
	@Override
	public Set<SName> getAllVars() {
		return _name.getAllVars();
	}
	
	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_name.tokens());
		_tokens.add(".");
		_tokens.addAll(_sname.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other instanceof QName) {
			QName qName = (QName) other;
			match = _name.compare(qName._name) && _sname.compare(qName._sname);
		}
		return match;
	}
	
	@Override
	public Map<String, Set<Node>> getKeywords() {
		if(_keywords == null) {
			_keywords = _name.getKeywords();
		}
		return _keywords;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_name);
		children.add(_sname);
		return children;
	}
	
	@Override
	public List<Modification> extractModifications() {
		List<Modification> modifications = new LinkedList<>();
		if(_matchNodeType) {
			modifications.addAll(_modifications);
			modifications.addAll(_name.extractModifications());
			modifications.addAll(_sname.extractModifications());
		}
		return modifications;
	}
	
	@Override
	public void deepMatch(Node other) {
		_tarNode = other;
		if(other instanceof QName) {
			_matchNodeType = true;
			QName qName = (QName) other;
			if(!_sname.getName().equals(qName._sname.getName())) {
				_matchNodeType = false;
				return;
			}
			_name.deepMatch(qName._name);
			if(!_name.isNodeTypeMatch()) {
				Update update = new Update(this, _name, qName._name);
				_modifications.add(update);
			}
		} else {
			_matchNodeType = false;
		}
	}
	
	@Override
	public boolean matchSketch(Node sketch) {
		boolean match = false;
		if(sketch instanceof QName) {
			match = true;
			QName qName = (QName) sketch;
			if(!qName.isNodeTypeMatch()) {
				if(!NodeUtils.matchNode(sketch, qName)) {
					return false;
				}
				bindingSketch(sketch);
			} else {
				if(qName._name.isKeyPoint()) {
					match = _name.matchSketch(qName._name);
				}
				if(match && qName._sname.isKeyPoint()) {
					match = _sname.matchSketch(qName._sname);
				}
			}
			if(match) {
				qName._binding = this;
				_binding = qName;
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
		if (sketch instanceof QName) {
			match = true;
			QName qName = (QName) sketch;
			if (qName._name.isKeyPoint()) {
				_name.bindingSketch(qName._name);
			}
			if (qName._sname.isKeyPoint()) {
				_sname.bindingSketch(qName._sname);
			}
		}
		return match;
	}
	
	@Override
	public void resetAllNodeTypeMatch() {
		_matchNodeType = false;
		_sname.resetAllNodeTypeMatch();
		_name.resetAllNodeTypeMatch();
	}

	@Override
	public void setAllNodeTypeMatch() {
		_matchNodeType = true;
		_sname.setAllNodeTypeMatch();
		_name.setAllNodeTypeMatch();
	}
	
	
	@Override
	public void computeFeatureVector() {
		_fVector = new FVector();
		String name = _name.toString();
		String sname = _sname.toString();
		if(_name instanceof SName && Character.isUpperCase(name.charAt(0)) && sname.toUpperCase().equals(sname)){
			_fVector.inc(FVector.INDEX_LITERAL);
		} else {
			_fVector.combineFeature(_name.getFeatureVector());
			_fVector.combineFeature(_sname.getFeatureVector());
		}
	}

}
