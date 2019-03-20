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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class AryCreation extends Expr {

    private static final long serialVersionUID = -8863515069590314916L;
    private MType _type = null;
    private List<Expr> _dimension = null;
    private AryInitializer _initializer = null;
    private int _dim = 0;

    /**
     * ArrayCreation: new PrimitiveType [ Expression ] { [ Expression ] } { [ ]
     * } new TypeName [ < Type { , Type } > ] [ Expression ] { [ Expression ] }
     * { [ ] } new PrimitiveType [ ] { [ ] } ArrayInitializer new TypeName [ <
     * Type { , Type } > ] [ ] { [ ] } ArrayInitializer
     */
    public AryCreation(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.ARRCREAT;
        _fIndex = VIndex.EXP_ARRAY_CRT;
    }

    public void setArrayType(MType type) {
        _type = type;
        setDimension(type.getDimension());
    }

    public void setDimension(int dimension) {
        _dim = dimension;
    }

    public void setDimension(List<Expr> dimension) {
        _dimension = dimension;
    }

    public void setInitializer(AryInitializer initializer) {
        _initializer = initializer;
    }

    public MType getElementType() {
        return _type;
    }

    public List<Expr> getDimention() {
        return _dimension;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_dimension.size() + 1);
        children.addAll(_dimension);
        if (_initializer != null) {
            children.add(_initializer);
        }
        return children;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("new ");
        stringBuffer.append(_type.toSrcString());
        for (Expr expr : _dimension) {
            stringBuffer.append("[");
            stringBuffer.append(expr.toSrcString());
            stringBuffer.append("]");
        }
        for (int i = _dim - _dimension.size(); i > 0; i--) {
            stringBuffer.append("[]");
        }
        if (_initializer != null) {
            stringBuffer.append(_initializer.toSrcString());
        }
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        boolean consider = parentConsidered || isConsidered();
        StringBuffer typeStr = _type.formalForm(nameMapping, consider, keywords);
        StringBuffer dimension = new StringBuffer();
        boolean contain = false;
        for (Expr expr : _dimension) {
            dimension.append("[");
            if(expr.formalForm(nameMapping, consider, keywords) != null) {
                dimension.append(expr.formalForm(nameMapping, consider, keywords));
                contain = true;
            } else {
                dimension.append(nameMapping.getExprID(expr));
            }
            dimension.append("]");
        }
        StringBuffer initializer = null;
        if (_initializer != null) {
            initializer = _initializer.formalForm(nameMapping, consider, keywords);
        }

        if (typeStr == null && !contain && initializer == null) {
            return super.toFormalForm0(nameMapping, parentConsidered, keywords);
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append("new ");
        if (typeStr == null) {
            buffer.append(nameMapping.getTypeID(_type));
        } else {
            buffer.append(typeStr);
        }
        buffer.append(dimension);
        if (_initializer != null) {
            if (initializer == null) {
                buffer.append("{}");
            } else {
                buffer.append(initializer);
            }
        }

        return buffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("new");
        _tokens.addAll(_type.tokens());
        for (Expr expr : _dimension) {
            _tokens.add("[");
            _tokens.addAll(expr.tokens());
            _tokens.add("]");
        }
        if (_initializer != null) {
            _tokens.add("=");
            _tokens.addAll(_initializer.tokens());
        }
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof AryCreation) {
            AryCreation aryCreation = (AryCreation) other;
            match = _type.compare(aryCreation._type);
            if (match) {
                match = match && (_dimension.size() == aryCreation._dimension.size());
                for (int i = 0; match && i < _dimension.size(); i++) {
                    match = match && _dimension.get(i).compare(aryCreation._dimension.get(i));
                }
                if (_initializer == null) {
                    match = match && (aryCreation._initializer == null);
                } else {
                    match = match && _initializer.compare(aryCreation._initializer);
                }
            }
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _selfFVector = new FVector();
        _selfFVector.inc(FVector.KEY_NEW);
        _selfFVector.inc(FVector.E_ACREAR);

        _completeFVector = new FVector();
        _completeFVector.combineFeature(_selfFVector);
        if (_dimension != null) {
            for (Expr expr : _dimension) {
                _completeFVector.combineFeature(expr.getFeatureVector());
            }
        }
        if (_initializer != null) {
            _completeFVector.combineFeature(_initializer.getFeatureVector());
        }
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        boolean match = false;
        AryCreation aryCreation = null;
        if(compare(node)) {
            aryCreation = (AryCreation) node;
            setBindingNode(node);
            match = true;
        } else if (getBindingNode() != null) {
            aryCreation = (AryCreation) getBindingNode();
            match = (aryCreation == node);
        } else if (canBinding(node)) {
            aryCreation = (AryCreation) node;
            setBindingNode(node);
            match = true;
        }

        if (aryCreation == null) {
            continueTopDownMatchNull();
        } else {
            NodeUtils.greedyMatchListNode(_dimension, aryCreation.getDimention());
            if (_initializer != null && aryCreation._initializer != null) {
                _initializer.postAccurateMatch(aryCreation._initializer);
            }
        }

        return match;
    }

    @Override
    public boolean genModifications() {
        if (super.genModifications()) {
            AryCreation aryCreation = (AryCreation) getBindingNode();
            List<Expr> exprs = aryCreation.getDimention();
            if (exprs.size() == _dimension.size()) {
                for (int i = 0; i < _dimension.size(); i++) {
                    if (_dimension.get(i).getBindingNode() != exprs.get(i)) {
                        Update update = new Update(this, _dimension.get(i), exprs.get(i));
                        _modifications.add(update);
                    } else {
                        _dimension.get(i).genModifications();
                    }
                }
            }
            if (_initializer == null) {
                if (aryCreation._initializer != null) {
                    Update update = new Update(this, _initializer, aryCreation._initializer);
                    _modifications.add(update);
                }
            } else {
                if (aryCreation._initializer == null
                        || _initializer.getBindingNode() != aryCreation._initializer) {
                    Update update = new Update(this, _initializer, aryCreation._initializer);
                    _modifications.add(update);
                } else {
                    _initializer.genModifications();
                }
            }
        }
        return true;
    }

    @Override
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
            StringBuffer tmp;
            stringBuffer.append("new ");
            stringBuffer.append(_type.transfer(vars, exprMap));
            for (Expr expr : _dimension) {
                stringBuffer.append("[");
                tmp = expr.transfer(vars, exprMap);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
                stringBuffer.append("]");
            }
            if (_initializer != null) {
                stringBuffer.append("=");
                tmp = _initializer.transfer(vars, exprMap);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
            }
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = new StringBuffer();
        Map<Integer, StringBuffer> dimensionMap = new HashMap<>();
        StringBuffer initializer = null;
        Node node = NodeUtils.checkModification(this);
        if (node != null) {
            AryCreation aryCreation = (AryCreation) node;
            for (Modification modification : aryCreation.getModifications()) {
                if (modification instanceof Update) {
                    Update update = (Update) modification;
                    if (update.getSrcNode() == aryCreation._initializer) {
                        initializer = update.apply(vars, exprMap);
                        if (initializer == null) return null;
                    } else {
                        for (int i = 0; i < aryCreation._dimension.size(); i++) {
                            if (update.getSrcNode() == aryCreation._dimension.get(i)) {
                                StringBuffer buffer = update.apply(vars, exprMap);
                                if (buffer == null) return null;
                                dimensionMap.put(i, buffer);
                            }
                        }
                    }
                } else {
                    LevelLogger.error("@ArrayCreate Should not be this kind of modification : " + modification.toString());
                }
            }
        }
        stringBuffer.append("new ");
        StringBuffer tmp;
        stringBuffer.append(_type.transfer(vars, exprMap));
        for(int i = 0; i < _dimension.size(); i++) {
            stringBuffer.append("[");
            tmp = dimensionMap.get(i);
            if (tmp == null) {
                tmp = _dimension.get(i).adaptModifications(vars, exprMap);
            }
            if(tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append("]");
        }
        if(initializer == null) {
            if(_initializer != null) {
                stringBuffer.append("=");
                tmp = _initializer.adaptModifications(vars, exprMap);
                if(tmp == null) return null;
                stringBuffer.append(tmp);
            }
        } else if (!initializer.toString().isEmpty()){
            stringBuffer.append("=");
            stringBuffer.append(initializer);
        }
        return stringBuffer;
    }

}