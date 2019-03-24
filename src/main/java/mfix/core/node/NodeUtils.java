/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Utils;
import mfix.core.node.ast.LineRange;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.Variable;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.MType;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.ast.expr.SuperMethodInv;
import mfix.core.node.ast.stmt.IfStmt;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.node.modify.Wrap;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class NodeUtils {

    public static Set<String> IGNORE_METHOD_INVOKE = new HashSet<String>(Arrays.asList("toString", "equals",
            "hashCode"));

    public static StringBuffer assemble(List<? extends Node> _statements,
                                        Map<Node, List<StringBuffer>> insertionBefore,
                                        Map<Node, List<StringBuffer>> insertionAfter,
                                        Map<Node, StringBuffer> map,
                                        Map<Integer, List<StringBuffer>> insertionAt,
                                        VarScope vars, Map<String, String> exprMap,
                                        String retType, Set<String> exceptions) {

        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer tmp;
        for (int index = 0; index < _statements.size(); index ++) {
            Node node = _statements.get(index);
            List<StringBuffer> list = insertionBefore.get(node);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    stringBuffer.append(list.get(i)).append(Constant.NEW_LINE);
                }
            }
            int start = index;
            list = insertionAt.get(start);
            while ( list != null) {
                for (int i = 0; i < list.size(); i++) {
                    stringBuffer.append(list.get(i)).append(Constant.NEW_LINE);
                }
                insertionAt.remove(start);
                start ++;
                list = insertionAt.get(start);
            }
            if (map.containsKey(node)) {
                StringBuffer update = map.get(node);
                if (update != null) {
                    stringBuffer.append(update).append(Constant.NEW_LINE);
                }
            } else {
                tmp = node.adaptModifications(vars, exprMap, retType, exceptions);
                if (tmp == null) return null;
                stringBuffer.append(tmp).append(Constant.NEW_LINE);
            }
            list = insertionAfter.get(node);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    stringBuffer.append(list.get(i)).append(Constant.NEW_LINE);
                }
            }
        }
        if (!insertionAt.isEmpty()) {
            List<Map.Entry<Integer, List<StringBuffer>>> list = new ArrayList<>(insertionAt.entrySet());
            list.stream().sorted(Comparator.comparingInt(Map.Entry::getKey));
            for (Map.Entry<Integer, List<StringBuffer>> entry : list) {
                for (StringBuffer s : entry.getValue()) {
                    stringBuffer.append(s).append(Constant.NEW_LINE);
                }
            }
        }
        return stringBuffer;
    }

    public static String distillBasicType(MType type) {
        String s = type.toSrcString().toString();
        return distillBasicType(s);
    }

    public static String distillBasicType(String s) {
        if (s == null) return null;
        if (s.startsWith("java.lang.")) {
            s = s.substring(10/*java.lang.*/);
        }
        int index = s.indexOf('<');
        return index > 0 ? s.substring(0, index) : s;
    }

    public static boolean patternMatch(Node fst, Node snd, Map<Node, Node> matchedNode, boolean skipFormalCmp) {
        if (fst.isConsidered() != snd.isConsidered()) return false;
        if ((skipFormalCmp || Utils.safeBufferEqual(fst.getFormalForm(), snd.getFormalForm()))
                && fst.getModifications().size() == snd.getModifications().size()) {
            Node dp1 = fst.getDataDependency();
            Node dp2 = snd.getDataDependency();
            if (dp1 != null && dp1.isConsidered()) {
                if (dp2 == null || !dp1.patternMatch(dp2, matchedNode)) {
                    return false;
                }
            }

            boolean exist = matchedNode.containsKey(fst);
            if (exist && matchedNode.get(fst) == snd) return true;
            if (Utils.checkCompatibleBidirectionalPut(fst, snd, matchedNode)) {
                Set<Modification> matched = new HashSet<>();
                for (Modification m : fst.getModifications()) {
                    boolean match = false;
                    for (Modification o : snd.getModifications()) {
                        if (!matched.contains(o) && m.patternMatch(o, matchedNode)) {
                            matched.add(o);
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        if (!exist) {
                            matchedNode.remove(fst);
                            matchedNode.remove(snd);
                        }
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * match two list of nodes greedily
     *
     * @param lst1 : first list
     * @param lst2 : second list
     */
    public static void greedyMatchListNode(List<? extends Node> lst1, List<? extends Node> lst2) {
        Set<Node> set = new HashSet<>();
        for (Node node : lst1) {
            for (Node other : lst2) {
                if (!set.contains(other) && node.postAccurateMatch(other)) {
                    set.add(other);
                    break;
                }
            }
        }
    }

    public static boolean isMethodName(Node node) {
        Node parent = node.getParent();
        if (parent instanceof MethodInv) {
            MethodInv methodInv = (MethodInv) parent;
            return methodInv.getName() == node;
        } else if (parent instanceof SuperMethodInv) {
            SuperMethodInv methodInv = (SuperMethodInv) parent;
            return methodInv.getMethodName() == node;
        }
        return false;
    }

    public static boolean possibleClassName(String name) {
        return Character.isUpperCase(name.charAt(0));
    }

    /**
     * check whether the given node is a simple node,
     * which usually represents a one token
     * @param node
     * @return
     */
    public static boolean isSimpleExpr(Node node) {
        switch (node.getNodeType()) {
            case SNAME:
            case QNAME:
            case NUMBER:
            case INTLITERAL:
            case FLITERAL:
            case DLITERAL:
            case NULL:
            case ASSIGNOPERATOR:
            case POSTOPERATOR:
            case PREFIXOPERATOR:
            case INFIXOPERATOR:
            case TYPE:
            case SLITERAL:
            case THIS:
            case BLITERAL:
            case CLITERAL:
            case TLITERAL:
                return true;
            default:
        }
        return false;
    }

    /**
     * check whether the bound buggy node contains any modifications
     *
     * @param node : the node for checking
     * @return : null if {@code node} does not have binding node
     * or the binding node has no modification, otherwise, the binding node
     * will be returned
     */
    public static Node checkModification(Node node) {
        if (node.getBuggyBindingNode() != null && !node.getBuggyBindingNode().getModifications().isEmpty()) {
            return node.getBuggyBindingNode();
        }
        return null;
    }

    /**
     * check whether the data dependencies of nodes {@code node} and {@code other}
     * match each other or not
     *
     * @param node           : source node for match
     * @param other          : target node for match
     * @param matchedNode    : map of nodes that already matches
     * @param matchedStrings : map of string that already matches
     * @return : true if their data dependencies match each other, otherwise false
     */
    public static boolean checkDependency(Node node, Node other, Map<Node, Node> matchedNode,
                                          Map<String, String> matchedStrings) {
        if (node.getDataDependency() != null && other.getDataDependency() != null) {
            if (node.getDataDependency().ifMatch(other.getDataDependency(), matchedNode, matchedStrings)) {
                return true;
            }
            return false;
        }
        return node.getDataDependency() == other.getDataDependency();
    }

    /**
     * check whether the source code of {@code node} and {@code other} can match
     * each other or not according to existing matching result {@code matchedStrings}.
     * If they can match each other, the mapping relation will be added to both
     * {@code matchedNode} and {@code matchedStrings}
     *
     * @param node           : source node to match
     * @param other          : target node to match
     * @param matchedNode    : already matched nodes
     * @param matchedStrings : already matched strings
     * @return : true if they can match each other, otherwise false
     */
    public static boolean matchSameNodeType(Node node, Node other, Map<Node, Node> matchedNode,
                                            Map<String, String> matchedStrings) {
        if (Utils.checkCompatiblePut(node.toString(), other.toString(), matchedStrings)) {
            matchedNode.put(node, other);
            return true;
        }
        return false;
    }

    /**
     * match two lists of nodes and return a list of modifications
     * according to the matching result
     *
     * @param pNode : parent node of nodes in {@code src}
     * @param src   : list of nodes for match
     * @param tar   : list of nodes for match
     * @return : a list of modifications
     */
    public static List<Modification> genModificationList(Node pNode, List<? extends Node> src,
                                                         List<? extends Node> tar) {
        return genModificationList(pNode, src, tar, false);
    }

    /**
     * match two list ast nodes and generate modifications
     *
     * @param pNode : parent node of src
     * @param src   : a list of source nodes
     * @param tar   : a list of target nodes
     * @param move  : permit move operation
     */
    public static List<Modification> genModificationList(Node pNode, List<? extends Node> src,
                                                         List<? extends Node> tar, boolean move) {
        Set<Integer> set = new HashSet<>();
        List<Deletion> deletions = new LinkedList<>();
        List<Insertion> insertions = new LinkedList<>();
        for (int i = 0; i < src.size(); i++) {
            boolean notmatch = true;
            for (int j = 0; j < tar.size(); j++) {
                if (set.contains(j)) continue;
                if (src.get(i).getBindingNode() == tar.get(j)) {
                    set.add(j);
                    src.get(i).genModifications();
                    notmatch = false;
                    break;
                }
            }
            if (notmatch) {
                Deletion deletion = new Deletion(pNode, src.get(i), i);
                deletions.add(deletion);
            }
        }
        for (int i = 0; i < tar.size(); i++) {
            if (set.contains(i)) continue;
            Node n = tar.get(i);
            Insertion insertion = new Insertion(pNode, i, n);
            insertions.add(insertion);

        }

        List<Modification> modifications = new LinkedList<>();
        Set<Integer> matched = new HashSet<>();
        Insertion insertion;
        Update update;
        for (Deletion d : deletions) {
            Node binding = d.getDelNode().getBindingNode();
            update = null;
            if (binding != null) {
                for (int i = 0; i < insertions.size(); i++) {
                    if (matched.contains(i)) {
                        continue;
                    }
                    insertion = insertions.get(i);
                    Node insNode = insertion.getInsertedNode();
                    List<Node> wrap = insNode.wrappedNodes();
                    if (wrap != null && wrap.contains(d.getDelNode())) {
                        matched.add(i);
                        update = new Wrap(d.getParent(), d.getDelNode(), insNode, wrap);
                    } else if (d.getIndex() == insertion.getIndex()
                            || binding.isParentOf(insertion.getInsertedNode())
                            || insertion.getInsertedNode().isParentOf(binding)) {
                        matched.add(i);
                        update = new Update(d.getParent(), d.getDelNode(), insertion.getInsertedNode());
                    }
                }
            }
            if (update == null) {
                modifications.add(d);
            } else {
                modifications.add(update);
            }
        }
        for (int i = 0; i < insertions.size(); i++) {
            if (matched.contains(i)) {
                continue;
            }
            Insertion ins = insertions.get(i);
            Set<Node> nodes = ins.getInsertedNode().recursivelyGetDataDependency(new HashSet<>());
            if (!nodes.isEmpty()) {
                for (Node node : nodes) {
                    if (node.getBindingNode() != null) {
                        node.getBindingNode().setInsertDepend(true);
                    }
                }
            }
            if (ins.getInsertedNode().getNodeType() == Node.TYPE.IF) {
                IfStmt insertNode = (IfStmt) ins.getInsertedNode();
                Set<String> vars = insertNode.getCondition()
                        .flattenTreeNode(new LinkedList<>()).stream()
                        .filter(n->n.getNodeType()== Node.TYPE.SNAME)
                        .map(n->n.toSrcString().toString())
                        .collect(Collectors.toSet());
                ins.setNextnode(findNextDepend(insertNode, vars));
                if (nodes.isEmpty()) {
                    ins.setPrenode(findPreDepend(insertNode, vars));
                }
            }
            modifications.add(ins);
        }
        return modifications;
    }

    private static Node findNextDepend(IfStmt insertNode, Set<String> vars) {
        if (!vars.isEmpty()) {
            Node parent = insertNode.getParent();
            List<Node> children = parent.getAllChildren();
            boolean tag = false;
            for (int index = 0; index < children.size(); index++) {
                if (insertNode == children.get(index)) {
                    tag = true;
                } else if (tag) {
                    Set<Node> nodes = children.get(index)
                            .flattenTreeNode(new LinkedList<>()).stream()
                            .filter(n -> n.getNodeType() == Node.TYPE.SNAME
                                    && vars.contains(n.toSrcString().toString()))
                            .collect(Collectors.toSet());
                    Node dep = getBoundedCommonParent(children.get(index), nodes);
                    if (dep != null && dep.getBindingNode() != null) {
                        return dep.getBindingNode();
                    }
                }
            }
        }
        return null;
    }

    private static Node findPreDepend(Node insertNode, Set<String> vars) {
        if (!vars.isEmpty()) {
            Node parent = insertNode.getParent();
            List<Node> children = parent.getAllChildren();
            boolean tag = false;
            for (int index = children.size() - 1; index >= 0; index--) {
                if (insertNode == children.get(index)) {
                    tag = true;
                } else if (tag) {
                    Set<Node> nodes = children.get(index)
                            .flattenTreeNode(new LinkedList<>()).stream()
                            .filter(n -> n.getNodeType() == Node.TYPE.SNAME
                                    && vars.contains(n.toSrcString().toString()))
                            .collect(Collectors.toSet());
                    Node dep = getBoundedCommonParent(children.get(index), nodes);
                    if (dep != null && dep.getBindingNode() != null) {
                        return dep.getBindingNode();
                    }
                }
            }
        }
        return null;
    }

    private static Node getBoundedCommonParent(Node parent, Set<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) return null;
        for (Node child : parent.getAllChildren()) {
            boolean isCommonParent = true;
            for (Node n : nodes) {
                if (!child.isParentOf(n)) {
                    isCommonParent = false;
                    break;
                }
            }
            if (isCommonParent && child.getBindingNode() != null) {
                return child;
            }
        }
        return parent;
    }

    public static String getDefaultValue(String type){
        if (type == null) return null;
        switch(type){
            case "?":
                return "";
            case "Boolean":
            case "boolean":
                return "false";
            case "Short":
            case "short":
            case "Integer":
            case "int":
                return "0";
            case "Float":
            case "float":
                return "0f";
            case "Double":
            case "double":
                return "0d";
            case "Long":
            case "long":
                return "0l";
            case "Character":
            case "char":
                return "' '";
            default:
                return "null";
        }

    }

    public static boolean isLegalVar(String var) {
        Pattern p = Pattern.compile("[\\w|_][\\w|\\d|_]*(\\[.*\\])?");
        return p.matcher(var).matches();
    }

    public static Type parseExprType(Expr left, String operator, Expr right) {
        if (left == null) {
            return parsePreExprType(right, operator);
        }

        if (right == null) {
            return parsePostExprType(left, operator);
        }

        AST ast = AST.newAST(AST.JLS8);
        switch (operator) {
            case "*":
            case "/":
            case "+":
            case "-":
                Type type = union(left.getType(), right.getType());
                if (type == null) {
                    type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
                }
                return type;
            case "%":
            case "<<":
            case ">>":
            case ">>>":
            case "^":
            case "&":
            case "|":
                return ast.newPrimitiveType(PrimitiveType.INT);
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "==":
            case "!=":
            case "&&":
            case "||":
                return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            default:
                return null;
        }
    }

    private static Type union(Type ty1, Type ty2) {
        if (ty1 == null) {
            return ty2;
        } else if (ty2 == null) {
            return ty1;
        }

        if (!ty1.isPrimitiveType() || !ty2.isPrimitiveType()) {
            return null;
        }

        String ty1String = ty1.toString().toLowerCase().replace("integer", "int");
        String ty2String = ty2.toString().toLowerCase().replace("integer", "int");

        AST ast = AST.newAST(AST.JLS8);
        if (ty1String.equals("double") || ty2String.equals("double")) {

            return ast.newPrimitiveType(PrimitiveType.DOUBLE);

        } else if (ty1String.equals("float") || ty2String.equals("float")) {

            return ast.newPrimitiveType(PrimitiveType.FLOAT);

        } else if (ty1String.equals("long") || ty2String.equals("long")) {

            return ast.newPrimitiveType(PrimitiveType.LONG);

        } else if (ty1String.equals("int") || ty2String.equals("int")) {

            return ast.newPrimitiveType(PrimitiveType.INT);

        } else if (ty1String.equals("short") || ty2String.equals("short")) {

            return ast.newPrimitiveType(PrimitiveType.SHORT);

        } else {

            return ast.newPrimitiveType(PrimitiveType.BYTE);

        }

    }

    private static Type parsePostExprType(Expr expr, String operator) {
        // ++/--
        AST ast = AST.newAST(AST.JLS8);
        return ast.newPrimitiveType(PrimitiveType.INT);
    }

    private static Type parsePreExprType(Expr expr, String operator) {
        AST ast = AST.newAST(AST.JLS8);
        switch (operator) {
            case "++":
            case "--":
                return ast.newPrimitiveType(PrimitiveType.INT);
            case "+":
            case "-":
                return expr.getType();
            case "~":
            case "!":
                return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            default:
                return null;
        }
    }

    @Deprecated
    public static Map<Integer, Set<String>> getUsableVarTypes(String file) {
        CompilationUnit unit = JavaFile.genAST(file);
        VariableVisitor variableVisitor = new VariableVisitor(unit);
        unit.accept(variableVisitor);
        return variableVisitor.getVars();
    }

    public static Map<Integer, VarScope> getUsableVariables(String file) {
        CompilationUnit unit = JavaFile.genAST(file);
        NewVariableVisitor variableVisitor = new NewVariableVisitor(unit);
        unit.accept(variableVisitor);
        return variableVisitor.getVars();
    }

}

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
class VariableVisitor extends ASTVisitor {
    private MethodVisitor _methodVisitor = new MethodVisitor();
    private Map<Integer, Set<String>> _vars = new HashMap<>();
    private Set<String> _fields = new HashSet<>();
    private CompilationUnit _unit;

    public VariableVisitor(CompilationUnit unit) {
        _unit = unit;
    }

    public boolean visit(FieldDeclaration node) {
        for (Object object : node.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
            _fields.add(vdf.getName().toString());
        }
        return true;
    }

    public Map<Integer, Set<String>> getVars() {
        for (Entry<Integer, Set<String>> entry : _vars.entrySet()) {
            entry.getValue().addAll(_fields);
        }
        return _vars;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int start = _unit.getLineNumber(node.getStartPosition());
        Set<String> localVars = new HashSet<>();
        for (Object object : node.parameters()) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) object;
            localVars.add(svd.getName().toString());
        }

        if (node.getBody() != null) {
            _methodVisitor.reset();
            node.getBody().accept(_methodVisitor);
            localVars.addAll(_methodVisitor.getVars());
        }
        _vars.put(start, localVars);
        return true;
    }

    class MethodVisitor extends ASTVisitor {

        private Set<String> vars = new HashSet<>();

        public void reset() {
            vars.clear();
        }

        public Set<String> getVars() {
            return vars;
        }

        public boolean visit(VariableDeclarationStatement node) {
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                vars.add(vdf.getName().getFullyQualifiedName());
            }
            return true;
        }

        public boolean visit(VariableDeclarationExpression node) {
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                vars.add(vdf.getName().getFullyQualifiedName());
            }
            return true;
        }

        public boolean visit(SingleVariableDeclaration node) {
            vars.add(node.getName().getFullyQualifiedName());
            return true;
        }

    }

}

class NewVariableVisitor extends ASTVisitor {
    private MethodVisitor _methodVisitor = new MethodVisitor();
    private Map<Integer, VarScope> _vars = new HashMap<>();
    private Set<Variable> _fields = new HashSet<>();
    private CompilationUnit _unit;

    public NewVariableVisitor(CompilationUnit unit) {
        _unit = unit;
    }

    public boolean visit(FieldDeclaration node) {
        String type = node.getType().toString();
        for (Object object : node.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
            _fields.add(new Variable(vdf.getName().getIdentifier(), type));
        }
        return true;
    }

    public Map<Integer, VarScope> getVars() {
        for (Entry<Integer, VarScope> entry : _vars.entrySet()) {
            entry.getValue().setGlobalVars(_fields);
        }
        return _vars;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int start = _unit.getLineNumber(node.getStartPosition());
        int end = _unit.getLineNumber(node.getStartPosition() + node.getLength());
        VarScope scope = new VarScope();
        _methodVisitor.reset(scope, start, end);
        node.accept(_methodVisitor);
        _vars.put(start, scope);
        return true;
    }

    class MethodVisitor extends ASTVisitor {

        private VarScope _scope;
        private int _end;
        public void reset(VarScope scope, int start, int end) {
            _scope = scope;
            _end = end;
        }

        public boolean visit(VariableDeclarationStatement node) {
            String type = node.getType().toString();
            int start = _unit.getLineNumber(node.getStartPosition());
            int end = getParentEnd(node);
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                _scope.addLocalVar(new Variable(vdf.getName().getIdentifier(), type), new LineRange(start, end));
            }
            return true;
        }

        public boolean visit(VariableDeclarationExpression node) {
            String type = node.getType().toString();
            int start = _unit.getLineNumber(node.getStartPosition());
            int end = getParentEnd(node);
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                _scope.addLocalVar(new Variable(vdf.getName().getIdentifier(), type), new LineRange(start, end));
            }
            return true;
        }

        public boolean visit(SingleVariableDeclaration node) {
            int start = _unit.getLineNumber(node.getStartPosition());
            int end = getParentEnd(node);
            String type = node.getType().toString();
            _scope.addLocalVar(new Variable(node.getName().getIdentifier(), type), new LineRange(start, end));
            return true;
        }

        private int getParentEnd(ASTNode node) {
            while(node != null) {
                if (node instanceof MethodDeclaration || node instanceof Block
                        || node instanceof IfStatement || node instanceof WhileStatement
                        || node instanceof ForStatement || node instanceof EnhancedForStatement
                        || node instanceof DoStatement) {
                    return _unit.getLineNumber(node.getStartPosition() + node.getLength());
                }
                node = node.getParent();
            }
            return _end;
        }

    }

}
