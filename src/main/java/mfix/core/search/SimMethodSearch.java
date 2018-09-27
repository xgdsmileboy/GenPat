/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.common.util.JavaFile;
import mfix.core.parse.NodeParser;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SimMethodSearch {


    public static Set<Node> searchSimMethod(final String searchfile, final String oriFile,
                                            final MethodDeclaration method, final double simThreshold) {
        CompilationUnit sunit = JavaFile.genASTFromFileWithType(searchfile);
        CompilationUnit ounit = JavaFile.genASTFromFileWithType(oriFile);
        final NodeParser parser = NodeParser.getInstance();
        parser.setCompilationUnit(ounit);
        Node node = parser.process(method);
        return searchSimMethod(sunit, node, simThreshold);
    }

    public static Set<Node> searchSimMethod(final CompilationUnit unit, final CompilationUnit oriUnit,
                                            final MethodDeclaration method, final double simThreshold) {
        final NodeParser parser = NodeParser.getInstance();
        parser.setCompilationUnit(oriUnit);
        Node node = parser.process(method);
        return searchSimMethod(unit, node, simThreshold);
    }

    public static Set<Node> searchSimMethod(final CompilationUnit unit, final Node node, final double simThreshold) {
        final Set<Node> set = new HashSet<>();
        final NodeParser parser = NodeParser.getInstance();
        parser.setCompilationUnit(unit);
        final FVector fVector = node.getFeatureVector();
        if (fVector == null) return set;
        final double biggetDis = 1.0 - simThreshold;
        unit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (node.getBody() == null) return true;
                Node sim = parser.process(node);
                if (sim != null && fVector.computeSimilarity(sim.getFeatureVector(), FVector.ALGO.NORM_2) <= biggetDis
                        && fVector.computeSimilarity(sim.getFeatureVector(), FVector.ALGO.COSINE) >= simThreshold) {
                    set.add(sim);
                }
                return true;
            }
        });

        return set;
    }
}
