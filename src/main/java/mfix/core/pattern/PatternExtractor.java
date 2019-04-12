/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Method;
import mfix.common.util.Pair;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.abs.TermFrequency;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.Variable;
import mfix.core.node.ast.expr.Svd;
import mfix.core.node.ast.expr.Vdf;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.Matcher;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-01-15
 */
public class PatternExtractor {

    public Set<Pattern> extractPattern(String srcFile, String tarFile) {
        return extractPattern(srcFile, tarFile, Constant.FILTER_MAX_CHANGE_LINE);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, Set<Method> focus) {
        return extractPattern(srcFile, tarFile, focus, Constant.FILTER_MAX_CHANGE_LINE);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, int maxChangeLine) {
        return extractPattern(srcFile, tarFile, null, Constant.FILTER_MAX_CHANGE_LINE);
    }

    public Set<Pattern> extractPattern(String srcFile, String tarFile, Set<Method> focus, int maxChangeLine) {
        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile, null);
        Set<String> imports = new HashSet<>();
        Set<String> srcImports = new HashSet<>();
        for (Object o : srcUnit.imports()) {
            srcImports.add(o.toString());
        }
        for (Object o : tarUnit.imports()) {
            String s = o.toString();
            if (!srcImports.contains(s)) {
                imports.add(s);
            }
        }
        Set<Pattern> patterns = new HashSet<>();
        if (srcUnit == null || tarUnit == null) {
            return patterns;
        }
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);
        NodeParser nodeParser = new NodeParser();

//        CodeAbstraction abstraction = new TF_IDF(srcFile, Constant.TF_IDF_FREQUENCY);
        CodeAbstraction abstraction = new TermFrequency(Constant.TOKEN_FREQENCY);
//        ElementCounter counter = new ElementCounter();
//        counter.open();
//        counter.loadCache();

        for (Pair<MethodDeclaration, MethodDeclaration> pair : matchMap) {
            if (focus != null) {
                boolean contain = false;
                for (Method method : focus) {
                    if (method.same(pair.getFirst())) {
                        contain = true;
                        break;
                    }
                }
                if (!contain) {
                    continue;
                }
            }
            nodeParser.setCompilationUnit(srcFile, srcUnit);
            Node srcNode = nodeParser.process(pair.getFirst());
            nodeParser.setCompilationUnit(tarFile, tarUnit);
            Node tarNode = nodeParser.process(pair.getSecond());

            if(srcNode.toSrcString().toString().equals(tarNode.toSrcString().toString())) {
                continue;
            }

            TextDiff diff = new TextDiff(srcNode, tarNode);
            if (diff.getMiniDiff().size() > maxChangeLine) {
                continue;
            }

            if(Matcher.greedyMatch((MethDecl) srcNode, (MethDecl) tarNode)) {
                Set<Node> nodes = tarNode.getConsideredNodesRec(new HashSet<>(), false);
                Set<Variable> newVars = getVars(nodes);
                Set<Node> temp;
                for(Node node : nodes) {
                    if (node.getBindingNode() != null) {
                        node.getBindingNode().setExpanded();
                    }
                    temp = node.expand(new HashSet<>());
                    for(Node n : temp) {
                        if (n.getBindingNode() != null) {
                            n.getBindingNode().setExpanded();
                        }
                    }
                }

//                nodes = srcNode.getConsideredNodesRec(new HashSet<>(), true);
//                for (Node n : nodes) {
//                    System.out.println(n);
//                }

//                srcNode.doAbstraction(counter);
                srcNode.doAbstraction(abstraction.lazyInit());
                tarNode.doAbstraction(abstraction);
                // serialize feature vector
                Pattern pattern = new Pattern(srcNode, imports);
                pattern.getFeatureVector();
                pattern.addNewVars(newVars);
                patterns.add(pattern);
            }
        }
//        counter.close();
        return patterns;
    }

    private Set<Variable> getVars(Set<Node> nodes) {
        Set<Variable> vars = new HashSet<>();
        Queue<Node> queue = new LinkedList<>(nodes);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getNodeType()) {
                case VARDECLFRAG:
                    Vdf vdf = (Vdf) node;
                    String type = vdf.getType() == null ? "?" : vdf.getType().typeStr();
                    vars.add(new Variable(vdf.getName(), type));
                    break;
                case SINGLEVARDECL:
                    Svd svd = (Svd) node;
                    vars.add(new Variable(svd.getName().getName(), svd.getDeclType().typeStr()));
                default:
            }
            queue.addAll(node.getAllChildren());
        }
        return vars;
    }
}
