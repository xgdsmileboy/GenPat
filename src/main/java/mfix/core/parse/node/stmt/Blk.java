/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.node.stmt;

import mfix.common.util.Constant;
import mfix.core.comp.Modification;
import mfix.core.parse.Matcher;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.MethDecl;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jiajun
 * @date Oct 8, 2017
 */
public class Blk extends Stmt {

    private List<Stmt> _statements = null;

    /**
     * Block:
     * { { Statement } }
     */
    public Blk(int startLine, int endLine, ASTNode node) {
        this(startLine, endLine, node, null);
        _nodeType = TYPE.BLOCK;
    }

    public Blk(int startLine, int endLine, ASTNode node, Node parent) {
        super(startLine, endLine, node, parent);
    }

    public void setStatement(List<Stmt> statements) {
        _statements = statements;
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
    public StringBuffer applyChange(Map<String, String> exprMap, Set<String> allUsableVars) {
        if (_binding != null && _binding instanceof Blk) {
            Blk blk = (Blk) _binding;
            Map<Node, List<StringBuffer>> insertionPositionMap = new HashMap<>();
            Map<Node, StringBuffer> map = new HashMap<>(_statements.size());
            if (!Matcher.applyStmtList(blk.getNodeModification(), _statements, this, exprMap, insertionPositionMap, map,
                    allUsableVars)) {
                return null;
            }
            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer tmp = null;
            stringBuffer.append("{" + Constant.NEW_LINE);
            for (Node node : _statements) {
                List<StringBuffer> list = insertionPositionMap.get(node);
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        stringBuffer.append(list.get(i));
                        stringBuffer.append(Constant.NEW_LINE);
                    }
                }
                if (map.containsKey(node)) {
                    StringBuffer update = map.get(node);
                    if (update != null) {
                        stringBuffer.append(update);
                        stringBuffer.append(Constant.NEW_LINE);
                    }
                } else {
                    tmp = node.applyChange(exprMap, allUsableVars);
                    if (tmp == null) return null;
                    stringBuffer.append(tmp);
                    stringBuffer.append(Constant.NEW_LINE);
                }
            }
            List<StringBuffer> list = insertionPositionMap.get(this);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    stringBuffer.append(list.get(i));
                    stringBuffer.append(Constant.NEW_LINE);
                }
            }
            stringBuffer.append("}");
            return stringBuffer;

        } else {
            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer tmp = null;
            stringBuffer.append("{" + Constant.NEW_LINE);
            for (int i = 0; i < _statements.size(); i++) {
                tmp = _statements.get(i).applyChange(exprMap, allUsableVars);
                if (tmp == null) return null;
                stringBuffer.append(tmp);
                stringBuffer.append(Constant.NEW_LINE);
            }
            stringBuffer.append("}");
            return stringBuffer;
        }
    }

    @Override
    public StringBuffer replace(Map<String, String> exprMap, Set<String> allUsableVars) {
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer tmp = null;
        stringBuffer.append("{" + Constant.NEW_LINE);
        for (int i = 0; i < _statements.size(); i++) {
            tmp = _statements.get(i).replace(exprMap, allUsableVars);
            if (tmp == null) return null;
            stringBuffer.append(tmp);
            stringBuffer.append(Constant.NEW_LINE);
        }
        stringBuffer.append("}");
        return stringBuffer;
    }

    @Override
    public StringBuffer printMatchSketch() {
        StringBuffer stringBuffer = new StringBuffer();
        if (isKeyPoint()) {
            stringBuffer.append("{" + Constant.NEW_LINE);
            for (int i = 0; i < _statements.size(); i++) {
                stringBuffer.append(_statements.get(i).printMatchSketch());
                stringBuffer.append(Constant.NEW_LINE);
            }
            stringBuffer.append("}");
        } else {
            stringBuffer.append(Constant.PLACE_HOLDER + ";");
        }
        return stringBuffer;
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
        if (other instanceof Blk) {
            Blk blk = (Blk) other;
            match = (_statements.size() == blk._statements.size());
            for (int i = 0; match && i < _statements.size(); i++) {
                match = match && _statements.get(i).compare(blk._statements.get(i));
            }
        }
        return match;
    }

    @Override
    public Map<String, Set<Node>> getKeywords() {
        if (_keywords == null) {
            _keywords = new HashMap<>();
            for (Stmt stmt : _statements) {
                avoidDuplicate(_keywords, stmt);
            }
        }
        return _keywords;
    }

    @Override
    public List<Modification> extractModifications() {
        List<Modification> modifications = new LinkedList<>();
        if (_matchNodeType) {
            modifications.addAll(_modifications);
            for (Stmt stmt : _statements) {
                modifications.addAll(stmt.extractModifications());
            }
        }
        return modifications;
    }

    @Override
    public void deepMatch(Node other) {
        _tarNode = other;
        if (other instanceof Blk) {
            _matchNodeType = true;
            Blk blk = (Blk) other;
            _modifications.addAll(Matcher.matchNodeList(this, _statements, blk._statements));
        } else {
            _matchNodeType = false;
        }
    }

    @Override
    public boolean matchSketch(Node sketch) {
        boolean match = false;
        boolean topDown = true;
        if (sketch instanceof Blk && sketch.getParent() instanceof MethDecl) {
            topDown = false;
            for (Stmt stmt : _statements) {
                if (stmt.matchSketch(sketch)) {
                    match = true;
                    break;
                }
            }
        }
        if (!match) {
            if (sketch instanceof Blk) {
                match = true;
                Blk blk = (Blk) sketch;
                if (blk.isNodeTypeMatch()) {
                    Set<Integer> alreadyMatch = new HashSet<>();
                    for (Stmt stmt : blk._statements) {
                        if (stmt.isKeyPoint()) {
                            boolean singleMatch = false;
                            for (int i = 0; i < _statements.size(); i++) {
                                if (alreadyMatch.contains(i)) {
                                    continue;
                                }
                                if (_statements.get(i).matchSketch(stmt)) {
                                    singleMatch = true;
                                    alreadyMatch.add(i);
                                    break;
                                }
                            }
                            if (!singleMatch) {
                                match = false;
                                break;
                            }
                        }
                    }
                } else {
                    bindingSketch(sketch);
                }
                if (match) {
                    blk._binding = this;
                    _binding = blk;
                }
            }
        }
        if (!match && topDown) {
            for (Stmt stmt : _statements) {
                if (stmt.matchSketch(sketch)) {
                    match = true;
                    break;
                }
            }
        }
        if (!match) sketch.resetBinding();
        return match;
    }

    @Override
    public boolean bindingSketch(Node sketch) {
        boolean match = false;
        _binding = sketch;
        sketch.setBinding(this);
        if (sketch instanceof Blk) {
            match = true;
            Blk blk = (Blk) sketch;
            Set<Integer> alreadyMatch = new HashSet<>();
            for (Stmt stmt : blk._statements) {
                if (stmt.isKeyPoint()) {
                    for (int i = 0; i < _statements.size(); i++) {
                        if (alreadyMatch.contains(i)) {
                            continue;
                        }
                        if (_statements.get(i).bindingSketch(stmt)) {
                            alreadyMatch.add(i);
                            break;
                        } else {
                            stmt.resetBinding();
                        }
                    }
                }
            }
        }
        return match;
    }

    @Override
    public void resetAllNodeTypeMatch() {
        _matchNodeType = false;
        for (Stmt stmt : _statements) {
            stmt.resetAllNodeTypeMatch();
        }
    }

    @Override
    public void setAllNodeTypeMatch() {
        _matchNodeType = true;
        for (Stmt stmt : _statements) {
            stmt.setAllNodeTypeMatch();
        }
    }

    @Override
    public void computeFeatureVector() {
        _fVector = new FVector();
        for (Stmt stmt : _statements) {
            _fVector.combineFeature(stmt.getFeatureVector());
        }
    }

    @Override
    public Node bindingNode(Node patternNode) {
        if (patternNode instanceof Blk) {
            return this;
        } else {
            Node binding = null;
            for (Stmt stmt : _statements) {
                binding = stmt.bindingNode(patternNode);
                if (binding != null) {
                    return binding;
                }
            }
        }
        return null;
    }
}
