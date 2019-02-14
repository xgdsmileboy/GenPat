/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node;

import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.match.metric.FVector.ALGO;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class NodeUtils {

    public static Set<String> IGNORE_METHOD_INVOKE = new HashSet<String>(Arrays.asList("toString", "equals",
            "hashCode"));

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
            Insertion insertion = new Insertion(pNode, i, tar.get(i));
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
                    if (d.getIndex() == insertion.getIndex() || binding.isParentOf(insertion.getInsertedNode())
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
            if (nodes.isEmpty()) {
                Node insertNode = ins.getInsertedNode();
                Node parent = insertNode.getParent();
                List<Node> children = parent.getAllChildren();
                for (int index = 0; index < children.size(); index++) {
                    if (insertNode == children.get(index)) {
                        if (index > 0) {
                            children.get(index - 1).setInsertDepend(true);
                            ins.setPrenode(children.get(index - 1));
                        }
                        if (index < children.size() - 1) {
                            children.get(index + 1).setInsertDepend(true);
                            ins.setNextnode(children.get(index + 1));
                        }
                    }
                }
            } else {
                for (Node node : nodes) {
                    if (node.getBindingNode() != null) {
                        node.getBindingNode().setInsertDepend(true);
                    }
                }
            }
            modifications.add(ins);
        }
        return modifications;

    }


    public static boolean matchNode(Node sketch, Node candidate) {
//		FVector fVector = sketch.getParentStmt().getFeatureVector();
//		FVector otherVector = candidate.getParentStmt().getFeatureVector();
        FVector fVector = sketch.getParent().getFeatureVector();
        FVector otherVector = candidate.getParent().getFeatureVector();
        if (fVector.computeSimilarity(otherVector, ALGO.COSINE) > 0.8 && fVector.computeSimilarity(otherVector,
                ALGO.NORM_2) < 0.5) {
            return true;
        }
//		Map<String, Set<Node>> map = sketch.getCalledMethods();
//		Map<String, Set<Node>> thisKeys = candidate.getCalledMethods();
//		for(Entry<String, Set<Node>> entry : map.entrySet()) {
//			if(!thisKeys.containsKey(entry.getKey())) {
//				return false;
//			}
//		}
        LevelLogger.debug("----Similarity filter");
        return false;
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

    public static Map<Integer, Set<String>> getUsableVarTypes(String file) {
        CompilationUnit unit = JavaFile.genAST(file);
        VariableVisitor variableVisitor = new VariableVisitor(unit);
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
