/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Svd;
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
public class CatClause extends Node {

	private static final long serialVersionUID = 8636697940678019414L;
	private Svd _exception = null;
	private Blk _blk = null; 
	
	/**
	 * CatchClause
	 *    catch ( SingleVariableDeclaration ) Block
	 */
	public CatClause(String fileName, int startLine, int endLine, ASTNode oriNode) {
		this(fileName, startLine, endLine, oriNode, null);
	}
	
	public CatClause(String fileName, int startLine, int endLine, ASTNode oriNode, Node parent) {
		super(fileName, startLine, endLine, oriNode, parent);
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
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_CATCH);
		_fVector.combineFeature(_exception.getFeatureVector());
		_fVector.combineFeature(_blk.getFeatureVector());
	}

	@Override
	public boolean postAccurateMatch(Node node) {
		CatClause catClause = null;
		boolean match = false;
		if(getBindingNode() != null) {
			catClause = (CatClause) getBindingNode();
			match = (catClause == node);
		} else if(canBinding(node)) {
			catClause = (CatClause) node;
			setBindingNode(node);
			match = true;
		}
		if(catClause == null) {
			continueTopDownMatchNull();
		} else {
			_exception.postAccurateMatch(catClause.getException());
			_blk.postAccurateMatch(catClause.getBody());
		}
		return match;
	}

	@Override
	public boolean genModidications() {
		if (getBindingNode() != null) {
			CatClause catClause = (CatClause) getBindingNode();
			if(_exception.getBindingNode() != catClause.getException()) {
				Update update = new Update(this, _exception, catClause.getException());
				_modifications.add(update);
			} else {
				_exception.genModidications();
			}
			_blk.genModidications();
			return true;
		}
		return false;
	}
}
