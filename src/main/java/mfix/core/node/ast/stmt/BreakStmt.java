/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class BreakStmt extends Stmt {

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
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_BREAK);
		if(_identifier != null) {
            _fVector.combineFeature(_identifier.getFeatureVector());
        }
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		BreakStmt breakStmt = null;
		if(getBindingNode() != null) {
			breakStmt = (BreakStmt) getBindingNode();
			match = (breakStmt == node);
		} else if(canBinding(node)) {
			breakStmt = (BreakStmt) node;
			setBindingNode(node);
			match = true;
		}
		if(breakStmt != null && _identifier != null) {
			_identifier.postAccurateMatch(breakStmt._identifier);
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if (super.genModidications()) {
			BreakStmt breakStmt = (BreakStmt) getBindingNode();
			if (_identifier == null) {
				if (breakStmt._identifier != null) {
					Update update = new Update(this, _identifier, breakStmt._identifier);
					_modifications.add(update);
				}
			} else if (_identifier.getBindingNode() != breakStmt._identifier){
				Update update = new Update(this, _identifier, breakStmt._identifier);
				_modifications.add(update);
			} else {
				_identifier.genModidications();
			}
			return true;
		}
		return false;
	}
}
