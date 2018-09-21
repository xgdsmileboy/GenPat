/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import mfix.common.config.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.comp.Modification;
import mfix.core.comp.Update;
import mfix.core.parse.NodeUtils;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.Expr;
import mfix.core.parse.node.expr.ExprList;

/**
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class ForStmt extends Stmt {

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
    public ForStmt(int startLine, int endLine, ASTNode node) {
        this(startLine, endLine, node, null);
        _nodeType = TYPE.FOR;
    }

    public ForStmt(int startLine, int endLine, ASTNode node, Node parent) {
        super(startLine, endLine, node, parent);
    }

    public void setCondition(Expr condition) {
        _condition = condition;
    }

    protected Expr getCondition() {
        return _condition;
    }

    public void setInitializer(ExprList initializers) {
        _initializers = initializers;
    }

    protected ExprList getInitializer() {
        return _initializers;
    }

    public void setUpdaters(ExprList updaters) {
        _updaters = updaters;
    }

    protected ExprList getUpdaters() {
        return _updaters;
    }

    public void setBody(Stmt body) {
        _body = body;
    }

    protected Stmt getBody() {
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
    public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
        StringBuffer initializer = null;
        StringBuffer condition = null;
        StringBuffer updater = null;
        StringBuffer body = null;
        if (_binding != null) {
            if (_binding instanceof ForStmt) {
                ForStmt forStmt = (ForStmt) _binding;
                for (Modification modification : forStmt.getNodeModification()) {
                    if (modification instanceof Update) {
                        Update update = (Update) modification;
                        Node node = update.getSrcNode();
                        if (node == forStmt._initializers) {
                            initializer = update.getTarString(exprMap, allUsableVars);
                            if (initializer == null) return null;
                        } else if (node == forStmt._condition) {
                            condition = update.getTarString(exprMap, allUsableVars);
                            if (condition == null) return null;
                        } else if (node == forStmt._updaters) {
                            updater = update.getTarString(exprMap, allUsableVars);
                            if (updater == null) return null;
                        } else {
                            LevelLogger.error("@ForStmt ERROR");
                        }
                    } else {
                        LevelLogger.error("@ForStmt Should not be this kind of modification : " + modification);
                    }
                }
            } else if (_binding instanceof EnhancedForStmt) {
                EnhancedForStmt enhancedForStmt = (EnhancedForStmt) _binding;
                for (Modification modification : enhancedForStmt.getNodeModification()) {
                    if (modification instanceof Update) {
                        Update update = (Update) modification;
                        if (update.getSrcNode() == enhancedForStmt.getBody()) {
                            body = update.getTarString(exprMap, allUsableVars);
                            if (body == null) return null;
                        } else {
                            LevelLogger.error("@ForStmt ERROR");
                        }
                    } else {
                        LevelLogger.error("@ForStmt Should not be this kind of modification : " + modification);
                    }
                }
            } else if (_binding instanceof DoStmt) {
                DoStmt doStmt = (DoStmt) _binding;
                for (Modification modification : doStmt.getNodeModification()) {
                    if (modification instanceof Update) {
                        Update update = (Update) modification;
                        if (update.getSrcNode() == doStmt.getExpression()) {
                            condition = update.getTarString(exprMap, allUsableVars);
                            if (condition == null) return null;
                        } else if (update.getSrcNode() == doStmt.getBody()) {
                            body = update.getTarString(exprMap, allUsableVars);
                            if (body == null) return null;
                        } else {
                            LevelLogger.error("@ForStmt ERROR");
                        }
                    } else {
                        LevelLogger.error("@ForStmt Should not be this kind of modification : " + modification);
                    }
                }
            } else if (_binding instanceof WhileStmt) {
                WhileStmt whileStmt = (WhileStmt) _binding;
                for (Modification modification : whileStmt.getNodeModification()) {
                    if (modification instanceof Update) {
                        Update update = (Update) modification;
                        if (update.getSrcNode() == whileStmt.getExpression()) {
                            condition = update.getTarString(exprMap, allUsableVars);
                            if (condition == null) return null;
                        } else {
                            body = update.getTarString(exprMap, allUsableVars);
                            if (body == null) return null;
                        }
                    } else {
                        LevelLogger.error("@ForStmt Should not be this kind of modification : " + modification);
                    }
                }
            }
        }
        StringBuffer stringBuffer = new StringBuffer("for(");
        StringBuffer tmp = null;
        if (initializer == null) {
            tmp = _initializers.applyChange(exprMap, allUsableVars);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(initializer);
        }
        stringBuffer.append(";");
        if (condition == null) {
            if (_condition != null) {
                tmp = _condition.applyChange(exprMap, allUsableVars);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
            }
        } else {
            stringBuffer.append(condition);
        }
        stringBuffer.append(";");
        if (updater == null) {
            tmp = _updaters.applyChange(exprMap, allUsableVars);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(updater);
        }
        stringBuffer.append(")");
        if (body == null) {
            tmp = _body.applyChange(exprMap, allUsableVars);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(body);
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
        StringBuffer stringBuffer = new StringBuffer("for(");
        StringBuffer tmp = null;
        tmp = _initializers.replace(exprMap, allUsableVars);
        if (tmp == null) return null;
        stringBuffer.append(tmp);
        stringBuffer.append(";");
        if (_condition != null) {
            tmp = _condition.replace(exprMap, allUsableVars);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        }
        stringBuffer.append(";");
        tmp = _updaters.replace(exprMap, allUsableVars);
        if (tmp == null) return null;
        stringBuffer.append(tmp);
        stringBuffer.append(")");
        tmp = _body.replace(exprMap, allUsableVars);
        if (tmp == null) return null;
        stringBuffer.append(tmp);
        return stringBuffer;
    }

    @Override
    public StringBuffer printMatchSketch() {
        StringBuffer stringBuffer = new StringBuffer();
        if (isKeyPoint()) {
            stringBuffer.append("for(");
            stringBuffer.append(_initializers.printMatchSketch());
            stringBuffer.append(";");
            if (_condition != null) {
                stringBuffer.append(_condition.printMatchSketch());
            }
            stringBuffer.append(";");
            stringBuffer.append(_updaters.printMatchSketch());
            stringBuffer.append(")");
            stringBuffer.append(_body.printMatchSketch());
        } else {
            stringBuffer.append(Constant.PLACE_HOLDER + ";");
        }
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
            match = true;
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
    public Map<String, Set<Node>> getKeywords() {
        if (_keywords == null) {
            _keywords = new HashMap<>(7);
            _keywords.putAll(_body.getKeywords());
            avoidDuplicate(_keywords, _initializers);
            avoidDuplicate(_keywords, _condition);
            avoidDuplicate(_keywords, _updaters);
        }
        return _keywords;
    }

    @Override
    public List<Modification> extractModifications() {
        List<Modification> modifications = new LinkedList<>();
        if (_matchNodeType) {
            modifications.addAll(_modifications);
            modifications.addAll(_initializers.extractModifications());
            if (_condition != null) {
                modifications.addAll(_condition.extractModifications());
            }
            modifications.addAll(_updaters.extractModifications());
            modifications.addAll(_body.extractModifications());
        }
        return modifications;
    }

    @Override
    public void deepMatch(Node other) {
        _tarNode = other;
        if (other instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) other;
            _matchNodeType = true;
            if (_condition == null && forStmt._condition != null) {
                _matchNodeType = false;
                return;
            }
            // initializer
            _initializers.deepMatch(forStmt._initializers);
            if (!_initializers.isNodeTypeMatch()) {
                Update update = new Update(this, _initializers, forStmt._initializers);
                _modifications.add(update);
            }
            // condition
            if (_condition != null && forStmt._condition != null) {
                _condition.deepMatch(forStmt._condition);
                if (!_condition.isNodeTypeMatch()) {
                    Update update = new Update(this, _condition, forStmt._condition);
                    _modifications.add(update);
                }
            } else if (_condition != null) {
                Update update = new Update(this, _condition, forStmt._condition);
                _modifications.add(update);
            }
            // updater
            _updaters.deepMatch(forStmt._updaters);
            if (!_updaters.isNodeTypeMatch()) {
                Update update = new Update(this, _updaters, forStmt._updaters);
                _modifications.add(update);
            }
            _body.deepMatch(forStmt._body);
            if (!_body.isNodeTypeMatch()) {
                Update update = new Update(this, _body, forStmt._body);
                _modifications.add(update);
            }
        } else {
            _matchNodeType = false;
        }
    }

    @Override
    public boolean matchSketch(Node sketch) {
        boolean match = false;
        if (sketch instanceof ForStmt) {
            match = true;
            ForStmt forStmt = (ForStmt) sketch;
            if (!forStmt.isNodeTypeMatch()) {
                if (!NodeUtils.matchNode(sketch, this)) {
                    match = false;
                } else {
                    bindingSketch(sketch);
                }
            } else {
                if (forStmt._initializers.isKeyPoint()) {
                    match = _initializers.matchSketch(forStmt._initializers);
                }
                if (match && forStmt._condition != null && forStmt._condition.isKeyPoint()) {
                    if (_condition == null) {
                        match = false;
                    } else {
                        match = _condition.matchSketch(forStmt._condition);
                    }
                }
                if (match && forStmt._updaters.isKeyPoint()) {
                    match = _updaters.matchSketch(forStmt._updaters);
                }
                if (match && forStmt._body.isKeyPoint()) {
                    match = _body.matchSketch(forStmt._body);
                }
            }
            if (match) {
                forStmt._binding = this;
                _binding = forStmt;
            }
        } else if (sketch instanceof EnhancedForStmt) {
            match = true;
            EnhancedForStmt enhancedForStmt = (EnhancedForStmt) sketch;
            if (!enhancedForStmt.isNodeTypeMatch()) {
                match = false;
            } else {
                if (enhancedForStmt.getParameter().isKeyPoint()) {
                    match = false;
                } else if (enhancedForStmt.getExpression().isKeyPoint()) {
                    match = false;
                } else if (enhancedForStmt.getBody().isKeyPoint()) {
                    match = _body.matchSketch(enhancedForStmt.getBody());
                }
            }
            if (match) {
                enhancedForStmt.setBinding(this);
                _binding = enhancedForStmt;
            }
        } else if (sketch instanceof DoStmt) {
            match = true;
            DoStmt doStmt = (DoStmt) sketch;
            if (!doStmt.isNodeTypeMatch()) {
                match = false;
            } else {
                if (doStmt.getExpression().isKeyPoint()) {
                    if (_condition == null) {
                        match = false;
                    } else {
                        match = _condition.matchSketch(doStmt.getExpression());
                    }
                }
                if (match && doStmt.getBody().isKeyPoint()) {
                    match = _body.matchSketch(doStmt.getBody());
                }
            }
            if (match) {
                doStmt.setBinding(this);
                _binding = doStmt;
            }
        } else if (sketch instanceof WhileStmt) {
            match = true;
            WhileStmt whileStmt = (WhileStmt) sketch;
            if (!whileStmt.isNodeTypeMatch()) {
                match = false;
            } else {
                if (whileStmt.getExpression().isKeyPoint()) {
                    if (_condition == null) {
                        match = false;
                    } else {
                        match = _condition.matchSketch(whileStmt.getExpression());
                    }
                }
                if (match && whileStmt.getBody().isKeyPoint()) {
                    match = _body.matchSketch(whileStmt.getBody());
                }
            }
            if (match) {
                whileStmt.setBinding(this);
                _binding = whileStmt;
            }
        }
        if (!match) {
            match = _body.matchSketch(sketch);
        }
        if (!match) sketch.resetBinding();
        return match;
    }

    @Override
    public boolean bindingSketch(Node sketch) {
        boolean match = false;
        _binding = sketch;
        sketch.setBinding(this);
        if (sketch instanceof ForStmt) {
            match = true;
            ForStmt forStmt = (ForStmt) sketch;
            if (forStmt._initializers.isKeyPoint()) {
                _initializers.bindingSketch(forStmt._initializers);
            }
            if (forStmt._condition != null && forStmt._condition.isKeyPoint()) {
                if (_condition != null) {
                    _condition.bindingSketch(forStmt._condition);
                }
            }
            if (forStmt._updaters.isKeyPoint()) {
                _updaters.bindingSketch(forStmt._updaters);
            }
            if (forStmt._body.isKeyPoint()) {
                _body.bindingSketch(forStmt._body);
            }
        }
        return match;
    }

    @Override
    public Node bindingNode(Node patternNode) {
        if (patternNode instanceof ForStmt) {
            Map<String, Set<Node>> map = patternNode.getKeywords();
            Map<String, Set<Node>> thisKeys = getKeywords();
            boolean containsAllKeys = true;
            for (Entry<String, Set<Node>> entry : map.entrySet()) {
                if (!thisKeys.containsKey(entry.getKey())) {
                    containsAllKeys = false;
                    break;
                }
            }
            if (containsAllKeys) {
                return this;
            } else {
                return null;
            }
        } else {
            return _body.bindingNode(patternNode);
        }
    }

    @Override
    public void resetAllNodeTypeMatch() {
        _matchNodeType = false;
        _initializers.resetAllNodeTypeMatch();
        if (_condition != null) {
            _condition.resetAllNodeTypeMatch();
        }
        _updaters.resetAllNodeTypeMatch();
        _body.resetAllNodeTypeMatch();
    }

    @Override
    public void setAllNodeTypeMatch() {
        _matchNodeType = true;
        _initializers.setAllNodeTypeMatch();
        if (_condition != null) {
            _condition.setAllNodeTypeMatch();
        }
        _updaters.setAllNodeTypeMatch();
        _body.setAllNodeTypeMatch();
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
        _fVector.inc(FVector.INDEX_STRUCT_FOR);
        _fVector.combineFeature(_initializers.getFeatureVector());
        if (_condition != null) {
            _fVector.combineFeature(_condition.getFeatureVector());
        }
        _fVector.combineFeature(_updaters.getFeatureVector());
        _fVector.combineFeature(_body.getFeatureVector());
    }
}
