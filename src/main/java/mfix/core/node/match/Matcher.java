/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.match;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MatchLevel;
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
import mfix.core.node.match.metric.ISimilarity;
import mfix.core.node.match.metric.NodeSimilarity;
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
import java.util.*;
import java.util.stream.Collectors;

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

        if (srcMethods.size() != tarMethods.size()) {
            LevelLogger.info("Different numbers of method declarations for two source files.");
            return matchPair;
        }

        for (MethodDeclaration sm : srcMethods) {
            boolean noMatch = true;
            for (int i = 0; noMatch && i < tarMethods.size(); i++) {
                MethodDeclaration tm = tarMethods.get(i);
                final DiffType diff = sameSignature(sm, tm);
                switch (diff) {
                    case SAME:
                        matchPair.add(new Pair<MethodDeclaration, MethodDeclaration>(sm, tm));
                        tarMethods.remove(tm);
                        noMatch = false;
                        break;
                    default:
                        LevelLogger.info(diff.toString());
                }
            }
            if (noMatch) {
                LevelLogger.warn("No match for method declaration !!!");
                return new LinkedList<>();
            }
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

    /**
     * Try to figure out all possible matching solutions between
     * buggy node {@code buggy} and pattern node {@code pattern}.
     *
     * @param buggy   : buggy node
     * @param pattern : pattern node
     * @return : a set of possible solutions
     */
    public static List<MatchInstance> tryMatch(Node buggy, Pattern pattern) {
        return tryMatch(buggy, pattern, null);
    }

    public static List<MatchInstance> tryMatch(Node buggy, Pattern pattern, Set<Integer> needToMatch) {
        return tryMatch(buggy, pattern, needToMatch, Constant.MAX_INSTANCE_PER_PATTERN, MatchLevel.ALL);
    }

    public static List<MatchInstance> tryMatch(Node buggy, Pattern pattern, Set<Integer> needToMatch,
                                               int topk, MatchLevel level) {
        List<Node> bNodes = new ArrayList<>(buggy.flattenTreeNode(new LinkedList<>()));
        List<Node> pNodes = new ArrayList<>(pattern.getConsideredNodes());

        int bSize = bNodes.size();
        int pSize = pNodes.size();

        ArrayList<MatchList> matchLists = new ArrayList<>(pSize);
        Map<Node, Node> nodeMap;
        Map<String, String> strMap;
        for (int i = 0; i < pSize; i++) {
            List<MatchNode> matchNodes = new LinkedList<>();
            for (int j = 0; j < bSize; j++) {
                nodeMap = new HashMap<>();
                strMap = new HashMap<>();
                if (pNodes.get(i).ifMatch(bNodes.get(j), nodeMap, strMap, level)) {
                    matchNodes.add(new MatchNode(bNodes.get(j), nodeMap, strMap));
                }
            }
            if (matchNodes.isEmpty()) {
                return new LinkedList<>();
            }
            matchLists.add(new MatchList(pNodes.get(i)).setMatchedNodes(matchNodes));
        }

        // match instance with fewer matching node first for better performance
        // fewer back-tracking
        matchLists.sort(Comparator.comparingInt(MatchList::matchSize));

        List<MatchInstance> matches = new LinkedList<>(permutePossibleMatches(matchLists));
        if (needToMatch != null && !needToMatch.isEmpty()) {
            // matched node exists in the given lines (line level fault localization)
            matches = matches.stream().filter(in -> in.modifyAny(needToMatch))
                    .sorted(Comparator.comparingDouble(MatchInstance::similarity).reversed())
                    .limit(topk).collect(Collectors.toList());
        } else {
            // sort matching instance descending by similarity
            matches = matches.stream().sorted(Comparator.comparingDouble(MatchInstance::similarity).reversed())
                    .limit(topk).collect(Collectors.toList());
        }
        return matches;
    }

    /**
     * Given the node matching result {@code matchLists},
     * permute all possible matching solutions.
     *
     * @param matchLists : node matching result
     * @return : all possible matching solutions
     */
    private static Set<MatchInstance> permutePossibleMatches(ArrayList<MatchList> matchLists) {
        Set<MatchInstance> results = new HashSet<>();
        matchNext(new HashMap<>(), matchLists, 0, new HashSet<>(), results);
        return results;
    }

    private final static List<ISimilarity> similarities;
    static {
        // similarity metrics used for match instance sorting
        similarities = new LinkedList<>();
        similarities.add(new NodeSimilarity());
    }

    /**
     * recursively match each node in the pattern based on the matching result in {@code list}
     *
     * @param matchedNodeMap : already matched node maps
     * @param list           : contains all nodes can be matched for each node in the pattern
     * @param i              : index of pattern node in {@code list} to match
     * @param alreadyMatched : a set of node to contain already matched node in the buggy code
     *                       to avoid duplicate matching.
     * @param instances      : contains possible matching solutions
     */
    private static void matchNext(Map<Node, MatchNode> matchedNodeMap, List<MatchList> list, int i,
                                  Set<Node> alreadyMatched, Set<MatchInstance> instances) {
        if (i == list.size()) {
            Map<Node, Node> nodeMap = new HashMap<>();
            Map<String, String> strMap = new HashMap<>();
            for (Map.Entry<Node, MatchNode> entry : matchedNodeMap.entrySet()) {
                nodeMap.putAll(entry.getValue().getNodeMap());
                strMap.putAll(entry.getValue().getStrMap());
            }
            MatchInstance matchInstance = new MatchInstance(nodeMap, strMap);
            matchInstance.computeSimilarity(similarities);
            instances.add(matchInstance);
        } else {
            Iterator<MatchNode> itor = list.get(i).getIterator();
            Node toMatch, curNode = list.get(i).getNode();
            MatchNode curMatchNode;
            while (itor.hasNext()) {
                curMatchNode = itor.next();
                toMatch = curMatchNode.getNode();
                if (alreadyMatched.contains(toMatch)) {
                    continue;
                }
                if (checkCompatibility(matchedNodeMap, curNode, curMatchNode)) {
                    matchedNodeMap.put(curNode, curMatchNode);
                    alreadyMatched.add(toMatch);
                    matchNext(matchedNodeMap, list, i + 1, alreadyMatched, instances);
                    matchedNodeMap.remove(curNode);
                    alreadyMatched.remove(toMatch);
                }
            }
        }
    }

    /**
     * Given already matched node pairs {@code matchedNodeMap},
     * checking syntax relation and dependency relation compatibility
     * for the new match between {@code curNode} and {@code curMatchNode}.
     *
     * @param matchedNodeMap : already matched node pairs
     * @param curNode        : current node under match (in pattern)
     * @param curMatchNode   : the node to match (in buggy code)
     * @return : {@code true} if node {@code curNode} can match {@code curMatchNode},
     * otherwise {@code false}
     */
    private static boolean checkCompatibility(Map<Node, MatchNode> matchedNodeMap, Node curNode,
                                              MatchNode curMatchNode) {
        Node node, previous;
        MatchNode matchNode;
        Node toMatch = curMatchNode.getNode();
        for (Map.Entry<Node, MatchNode> entry : matchedNodeMap.entrySet()) {
            node = entry.getKey();
            matchNode = entry.getValue();
            previous = matchNode.getNode();
            if ((node.isParentOf(curNode) && !previous.isParentOf(toMatch))
                    || (curNode.isParentOf(node) && !toMatch.isParentOf(previous))
                    || (dataDependOn(node, curNode) && !dataDependOn(previous, toMatch))
                    || (dataDependOn(curNode, node) && !dataDependOn(toMatch, previous))) {
//                    || (node.isDataDependOn(curNode) && !previous.isDataDependOn(toMatch))
//                    || (curNode.isDataDependOn(node) && !toMatch.isDataDependOn(previous))) {
//                    || node.isControlDependOn(curNode) != previous.isControlDependOn(toMatch)){
                return false;
            }
            Map<Node, Node> nodeMap = matchNode.getNodeMap();
            for (Map.Entry<Node, Node> inner : curMatchNode.getNodeMap().entrySet()) {
                if (nodeMap.containsKey(inner.getKey()) && nodeMap.get(inner.getKey()) != inner.getValue()) {
                    return false;
                }
            }
            Map<String, String> strMap = matchNode.getStrMap();
            for (Map.Entry<String, String> inner : curMatchNode.getStrMap().entrySet()) {
                if (strMap.containsKey(inner.getKey()) && !strMap.get(inner.getKey()).equals(inner.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean dataDependOn(Node src, Node dep) {
        Set<Node> nodes = src.recursivelyGetDataDependency(new HashSet<>());
        return nodes.contains(dep);
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
        // System.exit(0);
        java.util.regex.Pattern avoid = java.util.regex.Pattern.compile("System\\.exit\\(\\d+\\);");
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
        return match(Arrays.asList(src), Arrays.asList(tar), new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                if (o1.equals(o2)) {
                    return 1;
                } else {
                    return 0;
                }
            }

            ;
        });
    }

    public static Map<Integer, Integer> match(List<Stmt> src, List<Stmt> tar) {
        return match(src, tar, new Comparator<Stmt>() {
            @Override
            public int compare(Stmt o1, Stmt o2) {
                if (o1.compare(o2)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
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
                Set<String> set = getDefVars(node);
                for (index ++; index < statements.size(); index ++) {
                    if (statements.get(index).flattenTreeNode(new LinkedList<>())
                            .stream().anyMatch(n-> NodeUtils.isSimpleExpr(n)
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
                } else if (before.isEmpty() && after.isEmpty()){
                    List<StringBuffer> list = insertAt.get(insertion.getIndex());
                    if (list == null) {
                        list = new LinkedList<>();
                        insertAt.put(insertion.getIndex(), list);
                    }
                    list.add(tmp);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

}
