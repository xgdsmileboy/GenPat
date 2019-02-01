/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.AnonymousClassDecl;
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
public class ClassInstCreation extends Expr {

    private static final long serialVersionUID = -2405461094348344933L;
    private Expr _expression = null;
    private MType _classType = null;
    private ExprList _arguments = null;
    private AnonymousClassDecl _decl = null;

    /**
     * ClassInstanceCreation: [ Expression . ] new [ < Type { , Type } > ] Type
     * ( [ Expression { , Expression } ] ) [ AnonymousClassDeclaration ]
     */
    public ClassInstCreation(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.CLASSCREATION;
    }

    public void setExpression(Expr expression) {
        _expression = expression;
    }

    public void setClassType(MType classType) {
        _classType = classType;
    }

    public void setArguments(ExprList arguments) {
        _arguments = arguments;
    }

    public void setAnonymousClassDecl(AnonymousClassDecl decl) {
        _decl = decl;
    }

    public MType getClassType() {
        return _classType;
    }

    public Expr getExpression() {
        return _expression;
    }

    public ExprList getArguments() {
        return _arguments;
    }

    public AnonymousClassDecl getDecl() {
        return _decl;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (_expression != null) {
            stringBuffer.append(_expression.toSrcString());
            stringBuffer.append(".");
        }
        stringBuffer.append("new ");
        stringBuffer.append(_classType.toSrcString());
        stringBuffer.append("(");
        stringBuffer.append(_arguments.toSrcString());
        stringBuffer.append(")");
        if (_decl != null) {
            stringBuffer.append(_decl.toSrcString());
        }
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        if (_expression != null) {
            _tokens.addAll(_expression.tokens());
            _tokens.add(".");
        }
        _tokens.add("new");
        _tokens.addAll(_classType.tokens());
        _tokens.add("(");
        _tokens.addAll(_arguments.tokens());
        _tokens.add(")");
        if (_decl != null) {
            _tokens.addAll(_decl.tokens());
        }
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other instanceof ClassInstCreation) {
            ClassInstCreation classInstCreation = (ClassInstCreation) other;
            match = _expression == null ? (classInstCreation._expression == null) :
					_expression.compare(classInstCreation._expression);
            match =
					match && _classType.compare(classInstCreation._classType) && _arguments.compare(classInstCreation._arguments);
            if (_decl == null) {
                match = match && (classInstCreation._decl == null);
            } else {
                match = match && _decl.compare(classInstCreation._decl);
            }
        }
        return match;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(4);
        if (_expression != null) {
            children.add(_expression);
        }
        children.add(_classType);
        children.add(_arguments);
        if (_decl != null) {
            children.add(_decl);
        }
        return children;
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
        _fVector.inc(FVector.KEY_NEW);
        _fVector.inc(FVector.E_CLASS);
        if (_expression != null) {
            _fVector.combineFeature(_expression.getFeatureVector());
        }
        _fVector.combineFeature(_arguments.getFeatureVector());
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        boolean match = false;
        ClassInstCreation classInstCreation = null;
        if (getBindingNode() != null) {
            classInstCreation = (ClassInstCreation) getBindingNode();
            match = (classInstCreation == node);
        } else if (canBinding(node)) {
            classInstCreation = (ClassInstCreation) node;
            setBindingNode(node);
            match = true;
        }

        if (classInstCreation == null) {
            continueTopDownMatchNull();
        } else {
            if (_expression != null) {
                _expression.postAccurateMatch(classInstCreation.getExpression());
            }
            _classType.postAccurateMatch(classInstCreation.getClassType());
            _arguments.postAccurateMatch(classInstCreation.getArguments());
            if (_decl != null) {
                _decl.postAccurateMatch(classInstCreation.getDecl());
            }

        }
        return match;
    }

    @Override
    public boolean genModidications() {
        if (super.genModidications()) {
            ClassInstCreation classInstCreation = (ClassInstCreation) getBindingNode();
            if (_expression == null) {
                if (classInstCreation.getExpression() != null) {
                    Update update = new Update(this, _expression, classInstCreation.getExpression());
                    _modifications.add(update);
                }
            } else if (_expression.getBindingNode() != classInstCreation.getExpression()) {
                Update update = new Update(this, _expression, classInstCreation.getExpression());
                _modifications.add(update);
            } else {
                _expression.genModidications();
            }
            if (_classType.getBindingNode() != classInstCreation.getClassType()
                    || !_classType.typeStr().equals(classInstCreation.getClassType().typeStr())) {
                Update update = new Update(this, _classType, classInstCreation.getClassType());
                _modifications.add(update);
            }
            if (_arguments.getBindingNode() != classInstCreation.getArguments()) {
                Update update = new Update(this, _arguments, classInstCreation.getArguments());
                _modifications.add(update);
            } else {
                _arguments.genModidications();
            }
        }

        return true;
    }

    @Override
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        if(node instanceof Expr) {
            if(isAbstract()) {
                return checkDependency(node, matchedNode, matchedStrings)
                        && matchSameNodeType(node, matchedNode, matchedStrings);
            } else if (node instanceof ClassInstCreation){
                ClassInstCreation classInstCreation = (ClassInstCreation) node;
                List<Expr> exprs = _arguments.getExpr();
                List<Expr> others = classInstCreation.getArguments().getExpr();
                if (_classType.compare(classInstCreation.getClassType()) && exprs.size() == others.size()) {
                    matchedNode.put(_classType, classInstCreation.getClassType());
                    matchedNode.put(this, node);
                    matchedStrings.put(toString(), node.toString());
                    if(_expression != null && classInstCreation.getExpression() != null) {
                        matchedNode.put(_expression, classInstCreation.getExpression());
                        matchedStrings.put(_expression.toString(), classInstCreation.getExpression().toString());
                    }
                    for(int i = 0; i < exprs.size(); i++) {
                        matchedNode.put(exprs.get(i), others.get(i));
                        matchedStrings.put(exprs.get(i).toString(), others.get(i).toString());
                    }
                    return true;
                }
                return false;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public StringBuffer transfer(Set<String> vars, Map<String, String> exprMap) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
            StringBuffer tmp;
            if (_expression != null) {
                tmp = _expression.transfer(vars, exprMap);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
                stringBuffer.append(".");
            }
            stringBuffer.append("new ");
            stringBuffer.append(_classType.transfer(vars, exprMap));
            stringBuffer.append("(");
            tmp = _arguments.transfer(vars, exprMap);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append(")");
            if (_decl != null) {
                tmp = _decl.transfer(vars, exprMap);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
            }
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(Set<String> vars, Map<String, String> exprMap) {
        StringBuffer expression = null;
        StringBuffer classType = null;
        StringBuffer arguments = null;
        Node node = checkModification();
        if (node != null) {
            ClassInstCreation classInstCreation = (ClassInstCreation) node;
            for (Modification modification : classInstCreation.getModifications()) {
                if (modification instanceof Update) {
                    Update update = (Update) modification;
                    Node changedNode = update.getSrcNode();
                    if (changedNode == classInstCreation._expression) {
                        expression = update.apply(vars, exprMap);
                        if (expression == null) return null;
                    } else if (changedNode == classInstCreation._classType) {
                        classType = update.apply(vars, exprMap);
                        if (classType == null) return null;
                    } else if (changedNode == classInstCreation._arguments) {
                        arguments = update.apply(vars, exprMap);
                        if (arguments == null) return null;
                    }
                } else {
                    LevelLogger.error("@ClassInstanceCreate Should not be this kind of modification : " + modification);
                }
            }
        }
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer tmp;
        if (expression == null) {
            if (_expression != null) {
                tmp = _expression.adaptModifications(vars, exprMap);
                if(tmp == null) return null;
                stringBuffer.append(tmp);
                stringBuffer.append(".");
            }
        } else {
            stringBuffer.append(expression);
        }
        stringBuffer.append("new ");
        if(classType == null) {
            tmp = _classType.adaptModifications(vars, exprMap);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(classType);
        }
        stringBuffer.append("(");
        if(arguments == null) {
            tmp = _arguments.adaptModifications(vars, exprMap);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
        } else {
            stringBuffer.append(arguments);
        }
        stringBuffer.append(")");
        if (_decl != null) {
            tmp = _decl.adaptModifications(vars, exprMap);
            if(tmp == null) return null;
            stringBuffer.append(tmp);
        }
        return stringBuffer;
    }
}
