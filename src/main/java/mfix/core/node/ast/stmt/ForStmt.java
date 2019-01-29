/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.ExprList;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class ForStmt extends Stmt {

    private static final long serialVersionUID = -377100625221024477L;
    private ExprList _initializers = null;
    private ExprList _updaters = null;
    private Expr _condition = null;
    private Stmt _body = null;

    /**
     * for (
     * [ ForInit ];
     * [ Expression ] ;
     * [ ForUpdate ] )
     * Statement
     * ForInit:
     * Expression { , Expression }
     * ForUpdate:
     * Expression { , Expression }
     */
    public ForStmt(String fileName, int startLine, int endLine, ASTNode node) {
        this(fileName, startLine, endLine, node, null);
    }

    public ForStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.FOR;
    }

    public void setCondition(Expr condition) {
        _condition = condition;
    }

    public Expr getCondition() {
        return _condition;
    }

    public void setInitializer(ExprList initializers) {
        _initializers = initializers;
    }

    public ExprList getInitializer() {
        return _initializers;
    }

    public void setUpdaters(ExprList updaters) {
        _updaters = updaters;
    }

    public ExprList getUpdaters() {
        return _updaters;
    }

    public void setBody(Stmt body) {
        _body = body;
    }

    public Stmt getBody() {
        return _body;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer("for(");
        stringBuffer.append(_initializers.toSrcString());
        stringBuffer.append(";");
        if (_condition != null) {
            stringBuffer.append(_condition.toSrcString());
        }
        stringBuffer.append(";");
        stringBuffer.append(_updaters.toSrcString());
        stringBuffer.append(")");
        stringBuffer.append(_body.toSrcString());
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.addAll(_initializers.tokens());
        _tokens.add(";");
        if (_condition != null) {
            _tokens.addAll(_condition.tokens());
        }
        _tokens.add(";");
        _tokens.addAll(_updaters.tokens());
        _tokens.add(")");
        _tokens.addAll(_body.tokens());
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(4);
        children.add(_initializers);
        if (_condition != null) {
            children.add(_condition);
        }
        children.add(_updaters);
        children.add(_body);
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        List<Stmt> children = new ArrayList<>(1);
        if (_body != null) {
            children.add(_body);
        }
        return children;
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) other;
            match = _initializers.compare(forStmt._initializers);
            if (_condition != null) {
                match = match && _condition.compare(forStmt._condition);
            } else {
                match = match && (forStmt._condition == null);
            }
            match = match && _updaters.compare(forStmt._updaters);
            match = match && (forStmt._body == null);
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
        _fVector.inc(FVector.KEY_FOR);
        _fVector.combineFeature(_initializers.getFeatureVector());
        if (_condition != null) {
            _fVector.combineFeature(_condition.getFeatureVector());
        }
        _fVector.combineFeature(_updaters.getFeatureVector());
        _fVector.combineFeature(_body.getFeatureVector());
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        boolean match = false;
        ForStmt forStmt = null;
        if(getBindingNode() != null) {
            forStmt = (ForStmt) getBindingNode();
            if(_condition != null) {
                _condition.postAccurateMatch(forStmt.getCondition());
            }
            _initializers.postAccurateMatch(forStmt.getInitializer());
            _updaters.postAccurateMatch(forStmt.getUpdaters());
            match = (forStmt == node);
        } else if(canBinding(node)) {
            forStmt = (ForStmt) node;
            boolean flag = false;
            if(_condition != null) {
                flag = _condition.postAccurateMatch(forStmt.getCondition()) || flag;
            }
            flag = _initializers.postAccurateMatch(forStmt.getInitializer()) || flag;
            flag = _updaters.postAccurateMatch(forStmt.getUpdaters()) || flag;
            if(flag){
                setBindingNode(node);
                match = true;
            } else {
                forStmt = null;
            }
        }
        if(forStmt == null) {
            continueTopDownMatchNull();
        } else {
            _body.postAccurateMatch(forStmt.getBody());
        }

        return match;
    }

    @Override
    public boolean genModidications() {
        if (super.genModidications()) {
            ForStmt forStmt = (ForStmt) getBindingNode();
            if(_initializers.getBindingNode() != forStmt.getInitializer()) {
                Update update = new Update(this, _initializers, forStmt.getInitializer());
                _modifications.add(update);
            } else {
                _initializers.genModidications();
            }
            if(_condition == null) {
                if (forStmt.getCondition() != null) {
                    Update update = new Update(this, _condition, forStmt.getCondition());
                    _modifications.add(update);
                }
            } else if(_condition.getBindingNode() != forStmt.getCondition()) {
                Update update = new Update(this, _condition, forStmt.getCondition());
                _modifications.add(update);
            } else {
                _condition.genModidications();
            }
            if(_updaters.getBindingNode() != forStmt.getUpdaters()) {
                Update update = new Update(this, _updaters, forStmt.getUpdaters());
                _modifications.add(update);
            } else {
                _updaters.genModidications();
            }
            _body.genModidications();
            return true;
        }
        return false;
    }

    @Override
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        if (node instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) node;
            boolean match = _initializers.ifMatch(forStmt.getInitializer(), matchedNode, matchedStrings);
            if(_condition != null && forStmt.getCondition() != null) {
                match = match && _condition.ifMatch(forStmt.getCondition(), matchedNode, matchedStrings);
            }
            match = match && _updaters.ifMatch(forStmt.getUpdaters(), matchedNode, matchedStrings);
            match = match && _body.ifMatch(forStmt.getBody(), matchedNode, matchedStrings);
            return match && super.ifMatch(node, matchedNode, matchedStrings);
        }
        return false;
    }

    @Override
    public StringBuffer transfer(Set<String> vars) {
        StringBuffer stringBuffer = super.transfer(vars);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer("for(");
            StringBuffer tmp;
            tmp = _initializers.transfer(vars);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append(";");
            if (_condition != null) {
                tmp = _condition.transfer(vars);
                if(tmp == null) return null;
                stringBuffer.append(tmp);
            }
            stringBuffer.append(";");
            tmp = _updaters.transfer(vars);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append(")");
            tmp = _body.transfer(vars);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(Set<String> vars) {
        StringBuffer initializer = null;
        StringBuffer condition = null;
        StringBuffer updater = null;
        Node pnode = checkModification();
        if (pnode != null) {
            ForStmt forStmt = (ForStmt) pnode;
            for (Modification modification : forStmt.getModifications()) {
                if (modification instanceof Update) {
                    Update update = (Update) modification;
                    Node node = update.getSrcNode();
                    if (node == forStmt._initializers) {
                        initializer = update.apply(vars);
                        if (initializer == null) return null;
                    } else if (node == forStmt._condition) {
                        condition = update.apply(vars);
                        if (condition == null) return null;
                    } else if (node == forStmt._updaters) {
                        updater = update.apply(vars);
                        if (updater == null) return null;
                    } else {
                        LevelLogger.error("@ForStmt ERROR");
                    }
                } else {
                    LevelLogger.error("@ForStmt Should not be this kind of modification : " + modification);
                }
            }
        }

        StringBuffer stringBuffer = new StringBuffer("for(");
        StringBuffer tmp;
        if (initializer == null) {
            tmp = _initializers.adaptModifications(vars);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(initializer);
        }
        stringBuffer.append(";");
        if (condition == null) {
            if (_condition != null) {
                tmp = _condition.adaptModifications(vars);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
            }
        } else {
            stringBuffer.append(condition);
        }
        stringBuffer.append(";");
        if (updater == null) {
            tmp = _updaters.adaptModifications(vars);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(updater);
        }
        stringBuffer.append(")");
        tmp = _body.adaptModifications(vars);
        if (tmp == null) return null;
        stringBuffer.append(tmp);
        return stringBuffer;
    }
}
