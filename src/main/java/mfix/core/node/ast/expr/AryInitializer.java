/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.cluster.NameMapping;
import mfix.core.node.cluster.VIndex;
import mfix.core.node.match.metric.FVector;
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
public class AryInitializer extends Expr {

    private static final long serialVersionUID = 5694794734726396689L;
    private List<Expr> _expressions = null;

    /**
     * ArrayInitializer:
     *      { [ Expression { , Expression} [ , ]] }
     */
    public AryInitializer(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.ARRINIT;
        _fIndex = VIndex.EXP_ARRAY_INT;
    }

    public void setExpressions(List<Expr> expressions) {
        _expressions = expressions;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_expressions.size());
        children.addAll(_expressions);
        return children;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer("{");
        if (_expressions.size() > 0) {
            stringBuffer.append(_expressions.get(0).toSrcString());
            for (int i = 1; i < _expressions.size(); i++) {
                stringBuffer.append(",");
                stringBuffer.append(_expressions.get(i).toSrcString());
            }
        }
        stringBuffer.append("}");
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered) {
        boolean consider = isConsidered() || parentConsidered;
        if (_expressions.size() > 0) {
            List<StringBuffer> strings = new ArrayList<>(_expressions.size());
            for (int i = 0; i < _expressions.size(); i++) {
                if (_expressions.get(i).formalForm(nameMapping, consider) != null) {
                    strings.add(_expressions.get(i).formalForm(nameMapping, consider));
                }
            }
            if (!strings.isEmpty()) {
                StringBuffer stringBuffer = new StringBuffer("{");
                stringBuffer.append(strings.get(0));
                for (int i = 1; i < strings.size(); i++) {
                    stringBuffer.append(",");
                    stringBuffer.append(strings.get(i));
                }
                stringBuffer.append("}");
                return stringBuffer;
            } else {
                return super.toFormalForm0(nameMapping, parentConsidered);
            }
        }
        return null;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("{");
        if (_expressions.size() > 0) {
            _tokens.addAll(_expressions.get(0).tokens());
            for (int i = 1; i < _expressions.size(); i++) {
                _tokens.add(",");
                _tokens.addAll(_expressions.get(i).tokens());
            }
        }
        _tokens.add("}");
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof AryInitializer) {
            AryInitializer aryInitializer = (AryInitializer) other;
            match = (_expressions.size() == aryInitializer._expressions.size());
            for (int i = 0; match && i < _expressions.size(); i++) {
                match = match && _expressions.get(i).compare(aryInitializer._expressions.get(i));
            }
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
        _fVector.inc(FVector.E_AINIT);
        if (_expressions != null) {
            for (Expr expr : _expressions) {
                _fVector.combineFeature(expr.getFeatureVector());
            }
        }
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        boolean match = false;
        AryInitializer aryInitializer = null;
        if (compare(node)) {
            aryInitializer = (AryInitializer) node;
            setBindingNode(node);
            match = true;
        } else if (getBindingNode() != null) {
            aryInitializer = (AryInitializer) getBindingNode();
            match = (aryInitializer == node);
        } else if (canBinding(node)) {
            aryInitializer = (AryInitializer) node;
            setBindingNode(node);
            match = true;
        }

        if (aryInitializer == null) {
            continueTopDownMatchNull();
        } else {
            NodeUtils.greedyMatchListNode(_expressions, aryInitializer._expressions);
        }

        return match;
    }

    @Override
    public boolean genModifications() {
        if (super.genModifications()) {
            AryInitializer aryInitializer = (AryInitializer) getBindingNode();
            _modifications = NodeUtils.genModificationList(this, _expressions, aryInitializer._expressions);
        }
        return true;
    }

    @Override
    public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer("{");
            StringBuffer tmp;
            if (_expressions.size() > 0) {
                tmp = _expressions.get(0).transfer(vars, exprMap);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
                for (int i = 1; i < _expressions.size(); i++) {
                    stringBuffer.append(",");
                    tmp = _expressions.get(i).transfer(vars, exprMap);
                    if (tmp == null) return null;
                    stringBuffer.append(tmp);
                }
            }
            stringBuffer.append("}");
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = new StringBuffer("{");
        StringBuffer tmp;
        // not consider modification
        if (_expressions.size() > 0) {
            tmp = _expressions.get(0).adaptModifications(vars, exprMap);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
            for (int i = 1; i < _expressions.size(); i++) {
                stringBuffer.append(",");
                tmp = _expressions.get(i).adaptModifications(vars, exprMap);
                stringBuffer.append(tmp);
            }
        }
        stringBuffer.append("}");
        return stringBuffer;
    }

}
