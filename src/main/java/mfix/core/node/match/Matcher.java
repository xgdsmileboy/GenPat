/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.match;

import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Assign;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.ast.expr.Vdf;
import mfix.core.node.ast.stmt.ExpressionStmt;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.ast.stmt.VarDeclarationStmt;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.node.modify.Wrap;
import mfix.core.pattern.Pattern;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import javax.management.relation.Relation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class Matcher {

    public static List<Pair<MethodDeclaration, MethodDeclaration>> match(CompilationUnit src, CompilationUnit tar) {
        List<Pair<MethodDeclaration, MethodDeclaration>> matchPair = new LinkedList<>();
        MethodDeclCollector methodDeclCollector = new MethodDeclCollector();
        methodDeclCollector.init();
        src.accept(methodDeclCollector);
        List<MethodDeclaration> srcMethods = methodDeclCollector.getAllMethDecl();
        methodDeclCollector.init();
        tar.accept(methodDeclCollector);
        List<MethodDeclaration> tarMethods = methodDeclCollector.getAllMethDecl();

//        if (srcMethods.size() != tarMethods.size()) {
//            LevelLogger.info("Different numbers of method declarations for two source files.");
//            return matchPair;
//        }

        boolean noMatch = true;
        for (MethodDeclaration sm : srcMethods) {
            for (int i = 0; i < tarMethods.size(); i++) {
                MethodDeclaration tm = tarMethods.get(i);
                final DiffType diff = sameSignature(sm, tm);
                switch (diff) {
                    case SAME:
                        matchPair.add(new Pair<MethodDeclaration, MethodDeclaration>(sm, tm));
                        tarMethods.remove(tm);
                        noMatch = false;
                        break;
                    default:
                        // LevelLogger.info(diff.toString());
                }
            }
        }
        if (noMatch) {
            LevelLogger.warn("No match for method declaration !!!");
            return new LinkedList<>();
        }
        return matchPair;
    }

    enum DiffType {
        DIFF_MODIFIER("different modifiers"),
        DIFF_NAME("different names"),
        DIFF_RETURN("different return types"),
        DIFF_PARAM("different parameters"),
        SAME("same");

        private String message;

        DiffType(String msg) {
            message = msg;
        }

        public String toString() {
            return message;
        }
    }

    @SuppressWarnings("unchecked")
    private static DiffType sameSignature(MethodDeclaration sm, MethodDeclaration tm) {
        int smdf = sm.getModifiers();
        int tmdf = tm.getModifiers();
        if ((smdf & tmdf) != smdf) return DiffType.DIFF_MODIFIER;
        if (!sm.getName().getFullyQualifiedName().equals(tm.getName().getFullyQualifiedName()))
            return DiffType.DIFF_NAME;
        String sType = sm.getReturnType2() == null ? "?" : sm.getReturnType2().toString();
        String tType = tm.getReturnType2() == null ? "?" : tm.getReturnType2().toString();
        if (!sType.equals(tType)) return DiffType.DIFF_RETURN;
        List<SingleVariableDeclaration> sp = sm.parameters();
        List<SingleVariableDeclaration> tp = tm.parameters();
        if (sp.size() != tp.size()) return DiffType.DIFF_PARAM;
        for (int i = 0; i < sp.size(); i++) {
            if (!sp.get(i).getType().toString().equals(tp.get(i).getType().toString())) {
                return DiffType.DIFF_PARAM;
            }
        }
        return DiffType.SAME;
    }


    static class MethodDeclCollector extends ASTVisitor {

        List<MethodDeclaration> methodDeclarations;

        public MethodDeclCollector() {
        }

        public void init() {
            methodDeclarations = new LinkedList<>();
        }

        public List<MethodDeclaration> getAllMethDecl() {
            return methodDeclarations;
        }

        public boolean visit(MethodDeclaration md) {
            methodDeclarations.add(md);
            return true;
        }
    }

    /**
     * check whether two sets of {@code MethodInv} have intersection
     *
     * @param inPattern : {@code MethodInv}s in pattern code
     * @param inBuggy   : {@code MethodInv}s in buggy code
     * @return : {@code true} if they have intersection, otherwise {@code false}
     */
    private static boolean contains(Set<MethodInv> inPattern, Set<MethodInv> inBuggy) {
        // only consider method name and arguments?
        for (MethodInv methodInv : inPattern) {
            for (MethodInv b : inBuggy) {
                if (methodInv.getArguments().getExpr().size() == b.getArguments().getExpr().size() &&
                        methodInv.getName().getName().equals(b.getName().getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Based on shared {@code MethodInv} to filter patterns
     * that can be applied to the buggy node {@code buggy}
     *
     * @param buggy   : buggy node to check
     * @param pattern : pattern nodes to filter
     * @return : a subset of patterns in {@code pattern}
     */
    public static Set<Pattern> filter(Node buggy, Set<Pattern> pattern) {
        Set<MethodInv> inBuggy = buggy.getUniversalAPIs(new HashSet<>(), false);
        Set<Pattern> nodes = new HashSet<>();
        Set<MethodInv> inPattern;
        for (Pattern p : pattern) {
            inPattern = p.getUniversalAPIs();
            if (contains(inPattern, inBuggy)) {
                nodes.add(p);
            }
        }
        return nodes;
    }

    private static Map<Relation, Integer> mapRelation2LstIndex(List<Relation> relations) {
        Map<Relation, Integer> map = new HashMap<>();
        if (relations != null) {
            for (int i = 0; i < relations.size(); i++) {
                map.put(relations.get(i), i);
            }
        }
        return map;
    }

    private static boolean anyAncestorMatch(Node node) {
        while (node.getParent() != null) {
            node = node.getParent();
            if (node.getBindingNode() != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean greedyMatch(MethDecl src, MethDecl tar) {
        List<Stmt> srcStmt = new ArrayList<>(src.getAllChildStmt(new LinkedList<>()));
        List<Stmt> tarStmt = new ArrayList<>(tar.getAllChildStmt(new LinkedList<>()));

        List<Stmt> srcNotMatched = new ArrayList<>(srcStmt.size());
        List<Stmt> tarNotMatched = new ArrayList<>(tarStmt.size());
        Set<Integer> tarMatched = new HashSet<>();
        for (int i = 0; i < srcStmt.size(); i++) {
            boolean notmatched = true;
            if (anyAncestorMatch(srcStmt.get(i))) {
                notmatched = false;
            } else {
                for (int j = 0; j < tarStmt.size(); j++) {
                    if (!tarMatched.contains(j) && tarStmt.get(j).getBindingNode() == null
                            && srcStmt.get(i).compare(tarStmt.get(j))) {
                        srcStmt.get(i).setBindingNode(tarStmt.get(j));
                        srcStmt.get(i).postAccurateMatch(tarStmt.get(j));
                        tarMatched.add(j);
                        notmatched = false;
                        break;
                    }
                }
            }
            if (notmatched) {
                srcNotMatched.add(srcStmt.get(i));
            }
        }
        for (int i = 0; i < tarStmt.size(); i++) {
            if (tarStmt.get(i).getBindingNode() == null) {
                tarNotMatched.add(tarStmt.get(i));
            }
        }

        if (srcNotMatched.isEmpty() && tarNotMatched.isEmpty()) {
            return false;
        }

        // greedy match sub-expressions
        Map<Integer, Integer> map = greedySimMatch(srcNotMatched, tarNotMatched, 0.6);
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            List<Expr> srcExprs = srcNotMatched.get(entry.getKey()).getAllChildExpr(new ArrayList<>(11), true);
            List<Expr> tarExprs = tarNotMatched.get(entry.getValue()).getAllChildExpr(new ArrayList<>(11), true);
            Set<Integer> matchedIndex = new HashSet<>();
            for (int i = 0; i < srcExprs.size(); i++) {
                if (srcExprs.get(i).getBindingNode() == null) {
                    for (int j = 0; j < tarExprs.size(); j++) {
                        if (!matchedIndex.contains(j) && tarExprs.get(j).getBindingNode() == null
                                && srcExprs.get(i).compare(tarExprs.get(j))) {
                            srcExprs.get(i).setBindingNode(tarExprs.get(j));
                            matchedIndex.add(j);
                            break;
                        }
                    }
                }
            }
        }

        src.postAccurateMatch(tar);
        src.genModifications();
        Set<Modification> modifications = src.getAllModifications(new HashSet<>());
        if (modifications.isEmpty()) {
            return false;
        }
        // System.exit/out/error, Log(),LOG();
        final String string = "(System\\.(exit|out|error).*|(Log|LOG)(\\.|\\().*)";
        java.util.regex.Pattern avoid = java.util.regex.Pattern.compile(string);
        for (Modification m : modifications) {
            if(m instanceof Insertion) {
                Insertion insertion = (Insertion) m;
                if (avoid.matcher(insertion.getInsertedNode().toSrcString().toString()).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

//	public static boolean greedyMatch0(MethDecl src, MethDecl tar) {
//		Pattern p = PatternExtraction.extract(src, true);
//		List<Relation> srcRelations = p.getOldRelations();
//		p = PatternExtraction.extract(tar, true);
//		List<Relation> tarRelations = p.getOldRelations();
//		Map<Relation, Integer> oldR2index = mapRelation2LstIndex(srcRelations);
//		Map<Relation, Integer> newR2index = mapRelation2LstIndex(tarRelations);
//
//		int oldLen = srcRelations.size();
//		int newLen = tarRelations.size();
//
//		int[][] matrix = new int[oldLen][newLen];
//		Map<String, Set<Pair<Integer, Integer>>> loc2dependencies = new HashMap<>();
//		Set<Pair<Relation, Relation>> dependencies = new HashSet<>();
//		Set<Pair<Integer, Integer>> set;
//		for (int i = 0; i < oldLen; i++) {
//			for (int j = 0; j < newLen; j++) {
//				dependencies.clear();
//				if (srcRelations.get(i).match(tarRelations.get(j), dependencies)) {
//					matrix[i][j] = 1;
//					String key = i + "_" + j;
//					set = new HashSet<>();
//					for (Pair<Relation, Relation> pair : dependencies) {
//						set.add(new Pair<>(oldR2index.get(pair.getFirst()), newR2index.get(pair.getSecond())));
//					}
//					loc2dependencies.put(key, set);
//				}
//			}
//		}
//		Z3Solver solver = new Z3Solver();
//		Map<Integer, Integer> oldR2newRidxMap = solver.maxOptimize(matrix, loc2dependencies);
//		if(oldR2newRidxMap.size() * 2 == (srcRelations.size() + tarRelations.size())) {
//			return false;
//		}
//
//		Relation l,r;
//		for (Map.Entry<Integer, Integer> entry : oldR2newRidxMap.entrySet()) {
//			l = srcRelations.get(entry.getKey());
//			l.setMatched(true);
//			r = tarRelations.get(entry.getValue());
//			r.setMatched(true);
//			l.getAstNode().setBindingNode(r.getAstNode());
//		}
//		src.postAccurateMatch(tar);
//		src.genModifications();
//		return true;
//	}

    public static <T extends Node> Map<Integer, Integer> greedySimMatch(List<T> src, List<T> tar, double similar) {
        if (src.isEmpty() || tar.isEmpty()) return new HashMap<>();
        double[][] valueMat = new double[src.size()][tar.size()];
        for (int i = 0; i < src.size(); i++) {
            Object[] delTokens = src.get(i).tokens().toArray();
            if (delTokens.length > 1000) {
                continue;
            }
            Object[] addTokens;
            for (int j = 0; j < tar.size(); j++) {
                addTokens = tar.get(j).tokens().toArray();
                if (addTokens.length > 1000) {
                    continue;
                }
                Map<Integer, Integer> tmpMap = match(delTokens, addTokens);
                double value = ((double) tmpMap.size()) / ((double) delTokens.length);
                if (value > similar) {
                    valueMat[i][j] = value;
                }
            }
        }

        Map<Integer, Integer> finalRlst = new HashMap<>();
        int totalRow = valueMat.length;
        int totalCol = valueMat[0].length;
        double value = 1;
        int row = -1, col = -1;
        while (value > 0) {
            value = 0;
            for (int i = 0; i < totalRow; i++) {
                for (int j = 0; j < totalCol; j++) {
                    if (valueMat[i][j] > value) {
                        value = valueMat[i][j];
                        row = i;
                        col = j;
                    }
                }
            }
            if (value > 0) {
                finalRlst.put(row, col);
                for (int i = 0; i < totalRow; i++) {
                    valueMat[i][col] = 0;
                }
                for (int i = 0; i < totalCol; i++) {
                    valueMat[row][i] = 0;
                }
            }
        }
        return finalRlst;
    }

    public static Map<Integer, Integer> simMatch(List<Node> src, List<Node> tar, double similar) {
        Map<Integer, Integer> map = match(src, tar, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                if (o1.compare(o2)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        return simMatch(map, src, tar, similar);
    }

    public static <T extends Node> Map<Integer, Integer> simMatch(Map<Integer, Integer> exactMatchMap, List<T> src,
                                                                  List<T> tar, double similar) {

        Map<Integer, Integer> result = new HashMap<>();
        List<Integer> left = new ArrayList<>(src.size());
        for (int i = 0; i < src.size(); i++) {
            if (!exactMatchMap.containsKey(i)) {
                left.add(i);
            }
        }
        List<Integer> right = new ArrayList<>(tar.size());
        for (int i = 0; i < tar.size(); i++) {
            if (!exactMatchMap.containsValue(i)) {
                right.add(i);
            }
        }

        int last = 0;
        for (int i = 0; i < left.size(); i++) {
            Object[] delTokens = src.get(left.get(i)).tokens().toArray();
            Object[] addTokens = null;
            for (int j = last; j < right.size(); j++) {
                addTokens = tar.get(right.get(j)).tokens().toArray();
                Map<Integer, Integer> tmpMap = match(delTokens, addTokens);
                double value = ((double) tmpMap.size()) / ((double) delTokens.length);
                if (value > similar) {
                    result.put(left.get(i), right.get(j));
                    last = j + 1;
                    break;
                }
            }
        }

        return result;
    }

    public static Map<Integer, Integer> match(Object[] src, Object[] tar) {
        return match(Arrays.asList(src), Arrays.asList(tar), (o1, o2) -> o1.equals(o2) ? 1 : 0);
    }

    public static Map<Integer, Integer> match(List<Stmt> src, List<Stmt> tar) {
        return match(src, tar, (o1, o2) -> o1.compare(o2) ? 1 : 0);
    }

    enum Direction {
        LEFT,
        UP,
        ANDGLE
    }

    public static <T> Map<Integer, Integer> match(List<T> src, List<T> tar, Comparator<T> comparator) {
        Map<Integer, Integer> map = new HashMap<>();
        int srcLen = src.size();
        int tarLen = tar.size();
        if (srcLen == 0 || tarLen == 0) {
            return map;
        }
        int[][] score = new int[srcLen + 1][tarLen + 1];

        // LCS matching with path retrieval
        Direction[][] path = null;
        try {
            path = new Direction[srcLen + 1][tarLen + 1];
        } catch (OutOfMemoryError e) {
            LevelLogger.error("OutOfMemoryError when matching!");
            return map;
        }
        for (int i = 0; i < srcLen; i++) {
            for (int j = 0; j < tarLen; j++) {
                if (comparator.compare(src.get(i), tar.get(j)) > 0) {
                    score[i + 1][j + 1] = score[i][j] + 1;
                    path[i + 1][j + 1] = Direction.ANDGLE;
                } else {
                    int left = score[i + 1][j];
                    int up = score[i][j + 1];
                    if (left >= up) {
                        score[i + 1][j + 1] = left;
                        path[i + 1][j + 1] = Direction.LEFT;
                    } else {
                        score[i + 1][j + 1] = up;
                        path[i + 1][j + 1] = Direction.UP;
                    }
                }
            }
        }

        for (int i = srcLen, j = tarLen; i > 0 && j > 0; ) {
            switch (path[i][j]) {
                case ANDGLE:
                    map.put(i - 1, j - 1);
                    i--;
                    j--;
                    break;
                case LEFT:
                    j--;
                    break;
                case UP:
                    i--;
                    break;
                default:
                    LevelLogger.error("should not happen!");
                    System.exit(0);
            }
        }

        assert map.size() == score[srcLen][tarLen];
        return map;
    }

    private static Set<String> getDefVars(Node node) {
        Set<String> set = new HashSet<>();
        if (node instanceof VarDeclarationStmt) {
            VarDeclarationStmt vds = (VarDeclarationStmt) node;
            for (Vdf vdf : vds.getFragments()) {
                set.add(vdf.getName());
            }
        } else if (node instanceof ExpressionStmt) {
            ExpressionStmt expressionStmt = (ExpressionStmt) node;
            if (expressionStmt.getExpression() instanceof Assign) {
                Assign assign = (Assign) expressionStmt.getExpression();
                set.add(assign.getLhs().toSrcString().toString());
            }
        }
        return set;
    }

    public static boolean applyNodeListModifications(List<Modification> modifications,
                                                     List<? extends Node> statements,
                                                     Map<Node, List<StringBuffer>> insertionBefore,
                                                     Map<Node, List<StringBuffer>> insertionAfter,
                                                     Map<Integer, List<StringBuffer>> insertAt,
                                                     Map<Node, StringBuffer> changeNodeMap,
                                                     VarScope vars, Map<String, String> exprMap,
                                                     String retType, Set<String> exceptions) {
        StringBuffer tmp;
        for (Modification modification : modifications) {
            if (modification instanceof Wrap) {
                Wrap wrap = (Wrap) modification;
                Node node = wrap.getSrcNode().getBuggyBindingNode();
                assert node != null;
                int index = -1;
                for (int i = 0; i < statements.size(); i++) {
                    if (statements.get(i) == node
                            || statements.get(i) == node.getParentStmt()) {
                        index = i;
                        break;
                    }
                }
                if (index < 0) return false;
                List<Node> toWrap = new LinkedList<>();
                toWrap.add(node);
                Set<String> set = getDefVars(node);
                for (index++; index < statements.size(); index++) {
                    if (statements.get(index).flattenTreeNode(new LinkedList<>())
                            .stream().anyMatch(n -> NodeUtils.isSimpleExpr(n)
                                    && set.contains(n.toSrcString().toString()))) {
                        toWrap.add(statements.get(index));
                        set.addAll(getDefVars(statements.get(index)));
                        changeNodeMap.put(statements.get(index), null);
                    } else {
                        break;
                    }
                }
                tmp = wrap.apply(vars, exprMap, retType, exceptions, toWrap);
                if (tmp == null) return false;
                changeNodeMap.put(node, tmp);
            } else if (modification instanceof Update) {
                Update update = (Update) modification;
                Node node = update.getSrcNode().getBuggyBindingNode();
                assert node != null;
                // map current node to the updated node string
                tmp = update.apply(vars, exprMap, retType, exceptions);
                if (tmp == null) return false;
                changeNodeMap.put(node, tmp);
            } else if (modification instanceof Deletion) {
                Deletion deletion = (Deletion) modification;
                Node node = deletion.getDelNode().getBuggyBindingNode();
                // node to be deleted to should be completely matched
                assert node != null;
                // map deleted node to null
                changeNodeMap.put(node, null);
            } else if (modification instanceof Insertion) {
                Insertion insertion = (Insertion) modification;
                tmp = insertion.apply(vars, exprMap, retType, exceptions);
                if (tmp == null) return false;
                String str = tmp.toString();
                if (str.startsWith("super(") || str.startsWith("this(")) return false;
                Node insNode = insertion.getInsertedNode();
                Set<Node> before = new HashSet<>();
                Set<Node> after = new HashSet<>();
                Set<Node> depends = insNode.recursivelyGetDataDependency(new HashSet<>());
                if (!depends.isEmpty()) {
                    for (Node n : depends) {
                        if (n.getBindingNode() != null && n.getBindingNode().getBuggyBindingNode() != null) {
                            after.add(n.getBindingNode().getBuggyBindingNode());
                        }
                    }
                } else if (insertion.getPrenode() != null && insertion.getPrenode().getBuggyBindingNode() != null) {
                    after.add(insertion.getPrenode().getBuggyBindingNode());
                }
                if (insertion.getNextnode() != null && insertion.getNextnode().getBindingNode() != null) {
                    before.add(insertion.getNextnode().getBuggyBindingNode());
                }

                int afterIndex = -1;
                if (!after.isEmpty()) {
                    for (int i = statements.size() - 1; i >= 0; i--) {
                        final Node node = statements.get(i);
                        if (after.stream().anyMatch(n -> n == node || node.isParentOf(n))) {
                            afterIndex = i;
                            break;
                        }
                    }
                }
                int beforeIndex = -1;
                if (!before.isEmpty()) {
                    for (int i = 0; i < statements.size(); i++) {
                        final Node node = statements.get(i);
                        if (before.stream().anyMatch(n -> n == node || node.isParentOf(n))) {
                            beforeIndex = i;
                            break;
                        }
                    }
                }
                if (afterIndex < beforeIndex || (afterIndex * beforeIndex <= 0)) {
                    if (beforeIndex >= 0) {
                        List<StringBuffer> list = insertionBefore.get(statements.get(beforeIndex));
                        if (list == null) {
                            list = new LinkedList<>();
                            insertionBefore.put(statements.get(beforeIndex), list);
                        }
                        list.add(tmp);
                    } else {
                        List<StringBuffer> list = insertionAfter.get(statements.get(afterIndex));
                        if (list == null) {
                            list = new LinkedList<>();
                            insertionAfter.put(statements.get(afterIndex), list);
                        }
                        list.add(tmp);
                    }
                } else {
                    List<StringBuffer> list = insertAt.get(insertion.getIndex());
                    if (list == null) {
                        list = new LinkedList<>();
                        insertAt.put(insertion.getIndex(), list);
                    }
                    list.add(tmp);
                }
            }
        }
        return true;
    }

}
