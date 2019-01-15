/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.VarDeclarationExpr;
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
public class TryStmt extends Stmt {

	private static final long serialVersionUID = -4283593302346047974L;
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
	public TryStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public TryStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
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

	public List<VarDeclarationExpr> getResource() {
		return _resource;
	}

	public Blk getBody() {
		return _blk;
	}

	public List<CatClause> getCatches() {
		return _catches;
	}

	public Blk getFinally() {
		return _finallyBlk;
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
	public void computeFeatureVector() {
		_fVector = new FVector();
		_fVector.inc(FVector.KEY_TRY);
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

	@Override
	public boolean postAccurateMatch(Node node) {
		boolean match = false;
		TryStmt tryStmt = null;
		if(getBindingNode() != null) {
			tryStmt = (TryStmt) getBindingNode();
			match = (tryStmt == node);
		} else if(canBinding(node)) {
			tryStmt = (TryStmt) node;
			setBindingNode(tryStmt);
			match = true;
		}
		if(tryStmt == null) {
			continueTopDownMatchNull();
		} else {
			if(_resource != null && tryStmt.getResource() != null) {
				greedyMatchListNode(_resource, tryStmt.getResource());
			}
			_blk.postAccurateMatch(tryStmt.getBody());
			if(_catches != null && tryStmt.getCatches() != null) {
			    greedyMatchListNode(_catches, tryStmt.getCatches());
            }
			if(_finallyBlk != null) {
			    _finallyBlk.postAccurateMatch(tryStmt.getFinally());
            }
		}

		return match;
	}

	@Override
	public boolean genModidications() {
		if (super.genModidications()) {
            TryStmt tryStmt = (TryStmt) getBindingNode();
            _blk.genModidications();
            genModificationList(_catches, tryStmt.getCatches(), true);
            if (_finallyBlk == null) {
                if (tryStmt.getFinally() != null) {
                    Update update = new Update(this, _finallyBlk, tryStmt.getFinally());
                    _modifications.add(update);
                }
            } else if (_finallyBlk.getBindingNode() != tryStmt.getFinally()) {
                Update update = new Update(this, _finallyBlk, tryStmt.getFinally());
                _modifications.add(update);
            } else {
                _finallyBlk.genModidications();
            }
            return true;
        }
		return false;
	}
}
