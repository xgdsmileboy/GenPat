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
public class AryAcc extends Expr {

    private static final long serialVersionUID = 3197483700688117500L;
    private Expr _index = null;
    private Expr _array = null;

    /**
     * ArrayAccess:
     * Expression [ Expression ]
     */
    public AryAcc(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.ARRACC;
        _fIndex = VIndex.EXP_ARRAY_ACC;
    }

    public void setArray(Expr array) {
        _array = array;
    }

    public Expr getArray() {
        return _array;
    }

    public void setIndex(Expr index) {
        _index = index;
    }

    public Expr getIndex() {
        return _index;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(2);
        children.add(_array);
        children.add(_index);
        return children;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_array.toSrcString());
        stringBuffer.append("[");
        stringBuffer.append(_index.toSrcString());
        stringBuffer.append("]");
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        StringBuffer array, index;
        boolean consider = parentConsidered || isConsidered();
        array = _array.formalForm(nameMapping, consider, keywords);
        index = _index.formalForm(nameMapping, consider, keywords);
        if (array == null && index == null) {
            return super.toFormalForm0(nameMapping, parentConsidered, keywords);
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(array == null ? nameMapping.getExprID(_array) : array)
                .append("[")
                .append(index == null ? nameMapping.getExprID(_index) : index)
                .append("]");

        return buffer;
    }

    @Override
    public void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.addAll(_array.tokens());
        _tokens.add("[");
        _tokens.addAll(_index.tokens());
        _tokens.add("]");
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof AryAcc) {
            match = _array.compare(((AryAcc) other)._array) && _index.compare(((AryAcc) other)._index);
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
        _fVector.inc(FVector.E_AACC);
        _fVector.inc(FVector.BRAKET_SQL);
        _fVector.inc(FVector.BRAKET_SQR);
        _fVector.combineFeature(_array.getFeatureVector());
        _fVector.combineFeature(_index.getFeatureVector());
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        boolean match = false;
        AryAcc aryAcc = null;
        if(compare(node)) {
            aryAcc = (AryAcc) node;
            setBindingNode(node);
            match = true;
        } else if (getBindingNode() != null) {
            aryAcc = (AryAcc) getBindingNode();
            match = (aryAcc == node);
        } else if (canBinding(node)) {
            aryAcc = (AryAcc) node;
            setBindingNode(node);
            match = true;
        }

        if (aryAcc == null) {
            continueTopDownMatchNull();
        } else {
            _index.postAccurateMatch(aryAcc.getIndex());
            _array.postAccurateMatch(aryAcc.getArray());
        }

        return match;
    }

    @Override
    public boolean genModifications() {
        AryAcc bind = (AryAcc) getBindingNode();
        if (_index.getBindingNode() != bind.getIndex()) {
            Update update = new Update(this, _index, bind.getIndex());
            _modifications.add(update);
        } else {
            _index.genModifications();
        }
        if (_array.getBindingNode() != bind.getArray()) {
            Update update = new Update(this, _array, bind.getArray());
            _modifications.add(update);
        } else {
            _array.genModifications();
        }
        return true;
    }

    @Override
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
            StringBuffer tmp = _array.transfer(vars, exprMap);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append("[");
            tmp = _index.transfer(vars, exprMap);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append("]");
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer array = null;
        StringBuffer index = null;
        Node node = NodeUtils.checkModification(this);
        if (node != null) {
            AryAcc aryAcc = (AryAcc) node;
            for (Modification modification : aryAcc.getModifications()) {
                if (modification instanceof Update) {
                    Update update = (Update) modification;
                    if (update.getSrcNode() == aryAcc._array) {
                        array = update.apply(vars, exprMap);
                        if (array == null) return null;
                    } else {
                        index = update.apply(vars, exprMap);
                        if (index == null) return null;
                    }
                } else {
                    LevelLogger.error("@ArrayAcc Should not be this kind of modification : " + modification.toString());
                }
            }
        }
        StringBuffer tmp;
        if(array == null) {
            tmp = _array.adaptModifications(vars, exprMap);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(array);
        }
        stringBuffer.append("[");
        if(index == null) {
            tmp = _index.adaptModifications(vars, exprMap);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(index);
        }
        stringBuffer.append("]");
        return stringBuffer;
    }
}
