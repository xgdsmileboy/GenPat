/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.stmt.Blk;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.pattern.cluster.NameMapping;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class MethDecl extends Node {

    private static final long serialVersionUID = -4279492715496549645L;
    private List<String> _modifiers = new ArrayList<>(5);
    private String _retTypeStr;
    private transient Type _retType;
    private SName _name;
    private List<Expr> _arguments;
    private Blk _body;
    private List<String> _throws;

    public MethDecl(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, oriNode);
        _nodeType = TYPE.METHDECL;
        _retType = AST.newAST(AST.JLS8).newWildcardType();
        _retTypeStr = _retType.toString();
    }

    public void setModifiers(List<String> modifiers) {
        _modifiers = modifiers;
    }

    public List<String> getModifiers() {
        return _modifiers;
    }

    public void setThrows(List<String> throwTypes) {
        _throws = throwTypes;
    }

    public List<String> getThrows() {
        return _throws;
    }

    public void setRetType(Type type) {
        if (type != null) {
            _retType = type;
            _retTypeStr = type.toString();
        }
    }

    public Type getRetType() {
        return _retType;
    }

    public String getRetTypeStr() {
        return _retTypeStr;
    }

    public void setName(SName name) {
        _name = name;
    }

    public SName getName() {
        return _name;
    }

    public void setArguments(List<Expr> arguments) {
        _arguments = arguments;
    }

    public List<Expr> getArguments() {
        return _arguments;
    }

    public void setBody(Blk body) {
        _body = body;
    }

    public Blk getBody() {
        return _body;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Object modifier : _modifiers) {
            stringBuffer.append(modifier.toString() + " ");
        }
        if (!_retTypeStr.equals("?")) {
            stringBuffer.append(_retTypeStr + " ");
        }
        stringBuffer.append(_name.toSrcString());
        stringBuffer.append("(");
        if (_arguments != null && _arguments.size() > 0) {
            stringBuffer.append(_arguments.get(0).toSrcString());
            for (int i = 1; i < _arguments.size(); i++) {
                stringBuffer.append("," + _arguments.get(i).toSrcString());
            }
        }
        stringBuffer.append(")");
        if (_throws != null && _throws.size() > 0) {
            stringBuffer.append(" throws " + _throws.get(0));
            for (int i = 1; i < _throws.size(); i++) {
                stringBuffer.append("," + _throws.get(i));
            }
        }
        if (_body == null) {
            stringBuffer.append(";");
        } else {
            stringBuffer.append(_body.toSrcString());
        }
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("METHOD");
        stringBuffer.append("(");

        if (_arguments != null && _arguments.size() > 0) {
            List<StringBuffer> strings = new ArrayList<>(_arguments.size());
            for (int i = 0; i < _arguments.size(); i++) {
                if (_arguments.get(i).formalForm(nameMapping, false, keywords) != null) {
                    strings.add(_arguments.get(i).formalForm(nameMapping, false, keywords));
                }
            }
            if (strings.size() > 0) {
                stringBuffer.append(strings.get(0));
                for (int i = 1; i < strings.size(); i++) {
                    stringBuffer.append("," + strings.get(i));
                }
            }
        }
        stringBuffer.append(")");
        if (_body == null) {
            stringBuffer.append(";");
        } else {
            stringBuffer.append(_body.formalForm(nameMapping, false, keywords));
        }
        return stringBuffer;
    }

    @Override
    public boolean patternMatch(Node node, Map<Node, Node> matchedNode) {
        return (node instanceof MethDecl);
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();

        for (Object modifier : _modifiers) {
            _tokens.add(modifier.toString());
        }
        if (!_retType.toString().equals("?")) {
            _tokens.add(_retType.toString());
        }
        _tokens.addAll(_name.tokens());
        _tokens.add("(");
        if (_arguments != null && _arguments.size() > 0) {
            _tokens.addAll(_arguments.get(0).tokens());
            for (int i = 1; i < _arguments.size(); i++) {
                _tokens.addAll(_arguments.get(i).tokens());
            }
        }
        _tokens.add(")");
        if (_body == null) {
            _tokens.add(";");
        } else {
            _tokens.addAll(_body.tokens());
        }
    }

    @Override
    public Stmt getParentStmt() {
        return null;
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
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_arguments);
        if (_body == null) {
            return children;
        } else {
            children.add(_body);
            return children;
        }
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof MethDecl) {
            MethDecl methDecl = (MethDecl) other;
            match = _name.compare(methDecl._name) && (_arguments.size() == methDecl._arguments.size());
            for (int i = 0; match && i < _arguments.size(); i++) {
                match = match && _arguments.get(i).compare(methDecl._arguments.get(i));
            }
            if (_body != null && methDecl.getBody() != null) {
                _body.compare(methDecl.getBody());
            }
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _selfFVector = new FVector();
        _completeFVector = new FVector();
        for (Expr expr : _arguments) {
            _completeFVector.combineFeature(expr.getFeatureVector());
        }
        if (_body != null) {
            _completeFVector.combineFeature(_body.getFeatureVector());
        }
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        MethDecl methDecl = null;
        if (getBindingNode() != null) {
            methDecl = (MethDecl) getBindingNode();
        } else if (node != null  && node instanceof MethDecl) {
            methDecl = (MethDecl) node;
            if (_name.getName().equals(methDecl.getName().getName())
                    && _arguments.size() == methDecl.getArguments().size()) {
                setBindingNode(methDecl);
            } else {
                methDecl = null;
            }
        }
        if (methDecl != null) {
            List<Expr> arguments = methDecl.getArguments();
            _name.postAccurateMatch(methDecl.getName());
            for(int i = 0; i < _arguments.size() && i < arguments.size(); i++) {
                if (!_arguments.get(i).postAccurateMatch(arguments.get(i))) {
                    if (_arguments.get(i).noBinding() && arguments.get(i).noBinding()) {
                        _arguments.get(i).setBindingNode(arguments.get(i));
                    }
                }
            }
            if (_body != null) {
                _body.postAccurateMatch(methDecl.getBody());
            }
        }
        return true;
    }

    @Override
    public boolean genModifications() {
        MethDecl methDecl = null;
        for (Expr expr : _arguments) {
            expr.genModifications();
        }
        if (_body!= null && !_body.genModifications()) {
            if (getBindingNode() != null) {
                methDecl = (MethDecl) getBindingNode();
                Update update = new Update(this, _body, methDecl.getBody());
                _modifications.add(update);
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void greedyMatchBinding(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings) {
        if (node instanceof MethDecl) {
            MethDecl decl = (MethDecl) node;
            getBody().greedyMatchBinding(decl.getBody(), matchedNode, matchedStrings);
        }
    }

    @Override
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
        if (node instanceof MethDecl) {
            MethDecl methDecl = (MethDecl) node;
            if (_name.ifMatch(methDecl.getName(), matchedNode, matchedStrings, level)) {
                if (_body == null) return methDecl._body == null;
                return _body.ifMatch(methDecl.getBody(), matchedNode, matchedStrings, level)
                        && NodeUtils.checkDependency(this, node, matchedNode, matchedStrings, level)
                        && NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
            }
        }
        return false;
    }

    @Override
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
        return null;
    }

    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
        StringBuffer stringBuffer = new StringBuffer();
        for(Object modifier : _modifiers) {
            stringBuffer.append(modifier.toString() + " ");
        }
        if(!_retTypeStr.equals("?")) {
            stringBuffer.append(_retTypeStr + " ");
        }
        stringBuffer.append(_name.toSrcString());
        stringBuffer.append("(");
        StringBuffer tmp;
        if(_arguments != null && _arguments.size() > 0) {
            for(int i = 0; i < _arguments.size(); i++) {
                tmp = _arguments.get(i).adaptModifications(vars, exprMap, retType, exceptions, metric);
                if (tmp == null) return null;
                stringBuffer.append(i == 0 ? "" : ",").append(tmp);
            }
        }
        stringBuffer.append(")");
        if(_throws != null && _throws.size() > 0) {
            stringBuffer.append(" throws " + _throws.get(0));
            for(int i = 1; i < _throws.size(); i++) {
                stringBuffer.append("," + _throws.get(i));
            }
        }

        if (_body == null) {
            stringBuffer.append(";");
        } else {
            Node node = getBuggyBindingNode();
            List<Modification> modifications = node != null ? node.getModifications() : null;
            if (node == null || modifications.isEmpty()) {
                tmp = _body.adaptModifications(vars, exprMap, retType, exceptions, metric);
            } else {
                Update update = (Update) modifications.get(0);
                tmp = update.apply(vars, exprMap, retType, exceptions, metric);
            }
            if (tmp == null) return null;
            stringBuffer.append(tmp);
        }
        return stringBuffer;
    }
}