/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.ast.stmt;

import mfix.common.conf.Constant;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MatchLevel;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.modify.Adaptee;
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
public class Blk extends Stmt {

    private static final long serialVersionUID = -8152168560236365788L;
    private List<Stmt> _statements = null;

    /**
     * Block:
     * { { Statement } }
     */
    public Blk(String fileName, int startLine, int endLine, ASTNode node) {
        this(fileName, startLine, endLine, node, null);
    }

    public Blk(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.BLOCK;
        _fIndex = VIndex.STMT_BLK;
    }

    public void setStatement(List<Stmt> statements) {
        _statements = statements;
    }

    public List<Stmt> getStatement() {
        return _statements;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("{" + Constant.NEW_LINE);
        for (int i = 0; i < _statements.size(); i++) {
            stringBuffer.append(_statements.get(i).toSrcString());
            stringBuffer.append(Constant.NEW_LINE);
        }
        stringBuffer.append("}");
        return stringBuffer;
    }

    @Override
    protected StringBuffer toFormalForm0(NameMapping nameMapping, boolean parentConsidered, Set<String> keywords) {
        if (isAbstract() && !isConsidered()) return null;
        List<StringBuffer> strings = new ArrayList<>(_statements.size());
        StringBuffer b;
        for (int i = 0; i < _statements.size(); i++) {
            b = _statements.get(i).formalForm(nameMapping, false, keywords);
            if (b != null) {
                strings.add(b);
            }
        }
        if (strings.isEmpty()) {
            return isConsidered() ? new StringBuffer("{}") : null;
        } else {
            StringBuffer buffer = new StringBuffer("{");
            for (int i = 0; i < strings.size(); i++) {
                buffer.append('\n').append(strings.get(i));
            }
            buffer.append(strings.isEmpty() ? '}' : "\n}");
            return buffer;
        }
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("{");
        for (int i = 0; i < _statements.size(); i++) {
            _tokens.addAll(_statements.get(i).tokens());
        }
        _tokens.add("}");
    }

    @Override
    public List<Stmt> getChildren() {
        return _statements;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_statements.size());
        children.addAll(_statements);
        return children;
    }

    @Override
    public Stmt getParentStmt() {
        if (getParent() != null) {
            return getParent().getParentStmt();
        }
        return this;
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof Blk) {
            Blk blk = (Blk) other;
            match = (_statements.size() == blk._statements.size());
            for (int i = 0; match && i < _statements.size(); i++) {
                match = match && _statements.get(i).compare(blk._statements.get(i));
            }
        }
        return match;
    }

    @Override
    public void computeFeatureVector() {
        _selfFVector = new FVector();

        _completeFVector = new FVector();
        for (Stmt stmt : _statements) {
            _completeFVector.combineFeature(stmt.getFeatureVector());
        }
    }

    @Override
    public boolean postAccurateMatch(Node node) {
        Blk blk = null;
        boolean match = false;
        if (getParent() instanceof MethDecl && node != null) {
            if (node.getParent() instanceof MethDecl) {
                blk = (Blk) node;
                setBindingNode(node);
                match = true;
            }
        } else {
            if (getBindingNode() != null && (getBindingNode() == node || !compare(node))) {
                blk = (Blk) getBindingNode();
                if (node != null && getParent().getBindingNode() == node.getParent() && node instanceof Blk) {
                    blk.setBindingNode(null);
                    setBindingNode(node);
                    blk = (Blk) node;
                }
                match = blk == node;
            } else if (canBinding(node)) {
                blk = (Blk) node;
                setBindingNode(node);
                match = true;
            }
        }
        if (blk == null) {
            continueTopDownMatchNull();
        } else {
            NodeUtils.greedyMatchListNode(_statements, blk.getStatement());
        }
        return match;
    }

    @Override
    public boolean genModifications() {
        if(super.genModifications()) {
            Blk blk = (Blk) getBindingNode();
            _modifications = NodeUtils.genModificationList(this, _statements, blk.getStatement());
            return true;
        }
        return false;
    }

    @Override
    public boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings, MatchLevel level) {
        if(node instanceof Blk) {
            return super.ifMatch(node, matchedNode, matchedStrings, level);
        }
        return false;
    }

    @Override
    public StringBuffer transfer(VarScope vars, Map<String, String> exprMap, String retType, Set<String> exceptions,
                                 Adaptee metric) {
        StringBuffer stringBuffer = super.transfer(vars, exprMap, retType, exceptions, metric);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
            StringBuffer tmp;
            stringBuffer.append("{" + Constant.NEW_LINE);
            for (int i = 0; i < _statements.size(); i++) {
                tmp = _statements.get(i).transfer(vars, exprMap, retType, exceptions, metric);
                if(tmp == null) return null;
                stringBuffer.append(tmp);
                stringBuffer.append(Constant.NEW_LINE);
            }
            stringBuffer.append("}");
        }
        return stringBuffer;
    }

    @Override
    public StringBuffer adaptModifications(VarScope vars, Map<String, String> exprMap, String retType,
                                           Set<String> exceptions, Adaptee metric) {
        Node pnode = NodeUtils.checkModification(this);
        if (pnode != null) {
            Blk blk = (Blk) pnode;
            Map<Node, List<StringBuffer>> insertBefore = new HashMap<>();
            Map<Node, List<StringBuffer>> insertAfter = new HashMap<>();
            Map<Integer, List<StringBuffer>> insertAt = new HashMap<>();
            Map<Node, StringBuffer> map = new HashMap<>(_statements.size());
            if (!Matcher.applyNodeListModifications(blk.getModifications(), _statements, insertBefore,
                    insertAfter, insertAt, map, vars, exprMap, retType, exceptions, metric)) {
                return null;
            }
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("{" + Constant.NEW_LINE);
            StringBuffer tmp = NodeUtils.assemble(_statements, insertBefore, insertAfter, map, insertAt,
                    vars, exprMap, retType, exceptions, metric);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append("}");
            return stringBuffer;

        } else {
            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer tmp;
            stringBuffer.append("{" + Constant.NEW_LINE);
            for (int i = 0; i < _statements.size(); i++) {
                tmp = _statements.get(i).adaptModifications(vars, exprMap, retType, exceptions, metric);
                if(tmp == null) return null;
                stringBuffer.append(tmp);
                stringBuffer.append(Constant.NEW_LINE);
            }
            stringBuffer.append("}");
            return stringBuffer;
        }

    }
}
