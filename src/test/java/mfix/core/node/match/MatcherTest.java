/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.TestCase;
import mfix.core.node.MatchList;
import mfix.core.node.MatchedNode;
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.Node;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class MatcherTest extends TestCase {

    @Test
    public void test_match_dont() {
        String srcFile_change_retType = testbase + Constant.SEP + "src_Intersect.java";
        String tarFile_change_retType = testbase + Constant.SEP + "tar_Intersect.java";
        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile_change_retType);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile_change_retType);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);

        // The method signature cannot match
        // TODO: should permit some methods failing to match
        Assert.assertTrue(matchMap.isEmpty());
    }

    @Test
    public void test_match_do() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);

        // all methods are well matched
        Assert.assertTrue(matchMap.size() == 108);
    }

    @Test
    public void test_match_demo() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        Set<Node> patterns = PatternExtractor.extractPattern(srcFile, tarFile);

        String buggy = testbase + Constant.SEP + "buggy_SimpleSecureBrowser.java";
        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                methods.add(node);
                return true;
            }
        });

        NodeParser parser = NodeParser.getInstance();
        parser.setCompilationUnit(buggy, unit);
        for(MethodDeclaration m : methods) {
            Node node = parser.process(m);
            Set<Node> matched = Matcher.filter(node, patterns);
            for(Node p : matched) {
                tryMatch(node, p);
            }
        }
    }

    private boolean tryMatch(Node buggy, Node pattern) {
        List<Node> bNodes = new ArrayList<>(buggy.flattenTreeNode(new LinkedList<>()));
        List<Node> pNodes = new ArrayList<>(pattern.getConsideredNodesRec(new HashSet<>(), true));

        int bSize = bNodes.size();
        int pSize = pNodes.size();

        ArrayList<MatchList> matchList = new ArrayList<>(pSize);
        Map<Node, Node> nodeMap;
        Map<String, String> strMap;
        for (int i = 0; i < pSize; i++) {
            List<MatchedNode> matchedNodes = new LinkedList<>();
            for (int j = 0; j < bSize; j++) {
                nodeMap = new HashMap<>();
                strMap = new HashMap<>();
                if (pNodes.get(i).ifMatch(bNodes.get(j), nodeMap, strMap)) {
                    matchedNodes.add(new MatchedNode(bNodes.get(j), nodeMap, strMap));
                }
            }
            if (matchedNodes.isEmpty()) {
                return false;
            }
            matchList.add(new MatchList(pNodes.get(i)).setMatchedNodes(matchedNodes));
        }

        Collections.sort(matchList, new Comparator<MatchList>() {
            @Override
            public int compare(MatchList o1, MatchList o2) {
                return o1.getMatchedNodes().size() - o2.getMatchedNodes().size();
            }
        });

        matchNext(new HashMap<>(), matchList, 0, new HashSet<>());
        return true;

    }

    private void matchNext(Map<Node, MatchedNode> matchedNodeMap, List<MatchList> list, int i,
                           Set<Node> alreadyMatched) {
        System.out.println("---------- " + i + " -----------");
        if (i == list.size()) {
            System.out.println("FIND ONE MATCH : ");
            System.out.println(matchedNodeMap);
        } else {
            Iterator<MatchedNode> itor = list.get(i).getIterator();
            Node toMatch, curNode = list.get(i).getNode();
            MatchedNode curMatchedNode;
            while (itor.hasNext()) {
                curMatchedNode = itor.next();
                toMatch = curMatchedNode.getNode();
                if(alreadyMatched.contains(toMatch)) {
                    continue;
                }
                if (checkCompatibility(matchedNodeMap, curNode, curMatchedNode)) {
                    matchedNodeMap.put(curNode, curMatchedNode);
                    alreadyMatched.add(toMatch);
                    matchNext(matchedNodeMap, list, i + 1, alreadyMatched);
                    matchedNodeMap.remove(curNode);
                    alreadyMatched.remove(toMatch);
                }
            }
        }
    }

    private boolean checkCompatibility(Map<Node, MatchedNode> matchedNodeMap, Node curNode,
                                       MatchedNode curMatchedNode) {
        Node node, previous;
        MatchedNode matchedNode;
        Node toMatch = curMatchedNode.getNode();
        for (Map.Entry<Node, MatchedNode> entry : matchedNodeMap.entrySet()) {
            node = entry.getKey();
            matchedNode = entry.getValue();
            previous = matchedNode.getNode();
            if ((node.isParentOf(curNode) && !previous.isParentOf(toMatch))
                    || (curNode.isParentOf(node) && !toMatch.isParentOf(previous))
                    || (node.isDataDependOn(curNode) && !previous.isDataDependOn(toMatch))
                    || (curNode.isDataDependOn(node) && !toMatch.isDataDependOn(previous))){
//                    || node.isControlDependOn(curNode) != previous.isControlDependOn(toMatch)){
                return false;
            }
            Map<Node, Node> nodeMap = matchedNode.getNodeMap();
            for (Map.Entry<Node, Node> inner : curMatchedNode.getNodeMap().entrySet()) {
                if (nodeMap.containsKey(inner.getKey()) && nodeMap.get(inner.getKey()) != inner.getValue()) {
                    return false;
                }
            }
            Map<String, String> strMap = matchedNode.getStrMap();
            for (Map.Entry<String, String> inner : curMatchedNode.getStrMap().entrySet()) {
                if (strMap.containsKey(inner.getKey()) && !strMap.get(inner.getKey()).equals(inner.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

}
