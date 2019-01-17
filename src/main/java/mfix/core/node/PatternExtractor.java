/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.match.Matcher;
import mfix.core.node.parser.NodeParser;
import mfix.core.stats.element.ElementCounter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-01-15
 */
public class PatternExtractor {

    public static Set<Node> extractPattern(Set<Pair<String, String>> fixPairs) {
        Set<Node> nodes = new HashSet<>();
        for(Pair<String, String> pair : fixPairs) {
            nodes.addAll(extractPattern(pair.getFirst(), pair.getSecond()));
        }
        return nodes;
    }

    public static Set<Node> extractPattern(String srcFile, String tarFile) {
        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = NodeParser.getInstance();
        Set<Node> patterns = new HashSet<>();

        ElementCounter counter = new ElementCounter();
        counter.open();
        try {
            counter.loadCache(Constant.DB_CACHE_FILE);
        } catch (Exception e) {}

        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());

            if(srcNode.toSrcString().toString().equals(tarNode.toSrcString().toString())) {
                continue;
            }

            if(Matcher.greedyMatch((MethDecl) srcNode, (MethDecl) tarNode)) {
                Set<Node> nodes = tarNode.getConsideredNodesRec(new HashSet<>(), false);
                Set<Node> temp;
                for(Node node : nodes) {
                    temp = node.expand(new HashSet<>());
                    for(Node n : temp) {
                        if (n.getBindingNode() != null) {
                            n.getBindingNode().setConsidered(true);
                        }
                    }
                }
//                nodes = srcNode.getConsideredNodesRec(new HashSet<>(), true);
//                for (Node node : nodes) {
//                    System.out.println(node);
//                }
                srcNode.doAbstraction(counter);
//                for (Modification modification : srcNode.getAllModifications(new HashSet<>())) {
//                    System.out.println(modification);
//                }
                patterns.add(srcNode);
            }
        }

        counter.close();
        return patterns;
    }
}
