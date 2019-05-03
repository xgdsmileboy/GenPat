/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import mfix.core.pattern.cluster.VIndex;
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
public class CastExpr extends Expr {

    private static final long serialVersionUID = -8485318151476589525L;
    private MType _castType = null;
    private Expr _expression = null;

    /**
     * CastExpression:
     * ( Type ) Expression
     */
    public CastExpr(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.CAST;
        _fIndex = VIndex.EXP_CAST;
    }

    public void setCastType(MType type) {
        _castType = type;
    }

    public void setExpression(Expr expression) {
        _expression = expression;
    }

    public MType getCastType() {
        return _castType;
    }

    public Expr getExpresion() {
        return _expression;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("(");
        stringBuffer.append(_castType.toSrcString());
        stringBuffer.append(")");
        stringBuffer.append(_expression.toSrcString());
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
//        boolean consider = isConsidered() || parentConsidered;
        boolean consider = isConsidered();
        StringBuffer type = _castType.formalForm(nameMapping, consider, keywords);
        StringBuffer exp = _expression.formalForm(nameMapping, consider, keywords);
        if (type == null && exp == null) {
            return super.toFormalForm0(nameMapping, parentConsidered, keywords);
        }
        StringBuffer buffer = new StringBuffer("(");
        buffer.append(type == null ? nameMapping.getTypeID(_castType) : type)
                .append(')')
                .append(exp == null ? nameMapping.getExprID(_expression) : exp);
        return buffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("(");
        _tokens.addAll(_castType.tokens());
        _tokens.add(")");
        _tokens.addAll(_expression.tokens());
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(2);
        children.add(_castType);
        children.add(_expression);
        return children;
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof CastExpr) {
            CastExpr castExpr = (CastExpr) other;
            match = _castType.compare(castExpr._castType);
            match = match && _expression.compare(castExpr._expression);
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _selfFVector = new FVector();
        _selfFVector.inc(FVector.KEY_CAST);

        _completeFVector = new FVector();
        _completeFVector.combineFeature(_selfFVector);
        _completeFVector.combineFeature(_expression.getFeatureVector());
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        boolean match = false;
        CastExpr castExpr = null;
        if (compare(node)) {
            castExpr = (CastExpr) node;
            setBindingNode(node);
            match = true;
        } else if (getBindingNode() != null) {
            castExpr = (CastExpr) getBindingNode();
            match = (castExpr == node);
        } else if (canBinding(node)) {
            castExpr = (CastExpr) node;
            setBindingNode(node);
            match = true;
        }
        if (castExpr == null) {
            continueTopDownMatchNull();
        } else {
            _castType.postAccurateMatch(castExpr._castType);
            _expression.postAccurateMatch(castExpr._expression);
        }
        return match;
    }

    @Override
    public boolean genModifications() {
        if (super.genModifications()) {
            CastExpr castExpr = (CastExpr) getBindingNode();
            if (_castType.getBindingNode() != castExpr.getCastType()
                    || !_castType.typeStr().equals(castExpr.getCastType().typeStr())) {
                Update update = new Update(this, _castType, castExpr.getCastType());
                _modifications.add(update);
            }
            if (_expression.getBindingNode() != castExpr.getExpresion()) {
                Update update = new Update(this, _expression, castExpr.getExpresion());
                _modifications.add(update);
            }
        }
        return true;
    }

    @Override
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
            StringBuffer tmp;
            stringBuffer.append("(");
            tmp = _castType.transfer(vars, exprMap, retType, exceptions, metric);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append(")");
            tmp = _expression.transfer(vars, exprMap, retType, exceptions, metric);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
        StringBuffer castType = null;
        StringBuffer expression = null;
        Node node = NodeUtils.checkModification(this);
        if (node != null) {
            CastExpr castExpr = (CastExpr) node;
            for (Modification modification : castExpr.getModifications()) {
                if (modification instanceof Update) {
                    Update update = (Update) modification;
                    if (update.getSrcNode() == _castType) {
                        castType = update.apply(vars, exprMap, retType, exceptions, metric);
                        if (castType == null) return null;
                    } else {
                        expression = update.apply(vars, exprMap, retType, exceptions, metric);
                        if (expression == null) return null;
                    }
                } else {
                    LevelLogger.error("@CastExpr Should not ");
                }
            }
        }
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer tmp;
        stringBuffer.append("(");
        if (castType == null) {
            tmp = _castType.adaptModifications(vars, exprMap, retType, exceptions, metric);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(castType);
        }
        stringBuffer.append(")");
        if(expression == null) {
            tmp = _expression.adaptModifications(vars, exprMap, retType, exceptions, metric);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(expression);
        }
        return stringBuffer;
    }
}