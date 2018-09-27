/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.core.parse.Matcher;
import mfix.core.parse.NodeParser;
import mfix.core.parse.diff.Diff;
import mfix.core.parse.match.metric.FVector;
import mfix.core.parse.node.Node;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class SimMethodSearch {


    public static Map<Node, Pair<Double, Double>> searchSimMethod(final String searchfile, final String oriFile,
                                                                  final MethodDeclaration method,
                                                                  final double simThreshold) {
        CompilationUnit sunit = JavaFile.genASTFromFileWithType(searchfile);
        CompilationUnit ounit = JavaFile.genASTFromFileWithType(oriFile);
        final NodeParser parser = NodeParser.getInstance();
        parser.setCompilationUnit(oriFile, ounit);
        Node node = parser.process(method);
        return searchSimMethod(sunit, node, simThreshold);
    }

//    public static Map<Node, Pair<Double, Double>> searchSimMethod(final CompilationUnit unit,
//                                                                  final CompilationUnit oriUnit,
//                                                                  final MethodDeclaration method,
//                                                                  final double simThreshold) {
//        final NodeParser parser = NodeParser.getInstance();
//        parser.setCompilationUnit(oriUnit);
//        Node node = parser.process(method);
//        return searchSimMethod(unit, node, simThreshold);
//    }

    public static Map<Node, Pair<Double, Double>> searchSimMethod(final CompilationUnit unit, final Node node,
                                                                  final double simThreshold) {
        final Map<Node, Pair<Double, Double>> map = new HashMap<>();
        final NodeParser parser = NodeParser.getInstance();
        parser.setCompilationUnit(null, unit);
        final FVector fVector = node.getFeatureVector();
        if (fVector == null) return map;
        final double biggetDis = 1.0 - simThreshold;
        unit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (node.getBody() == null) return true;
                Node sim = parser.process(node);
                if (sim != null) {
                    double norm = fVector.computeSimilarity(sim.getFeatureVector(), FVector.ALGO.NORM_2);
                    double cosine = fVector.computeSimilarity(sim.getFeatureVector(), FVector.ALGO.COSINE);
                    if (norm < biggetDis && cosine > simThreshold) {
                        map.put(sim, new Pair<Double, Double>(norm, cosine));
                    }
                }
                return true;
            }
        });

        return map;
    }

    public static <T> Map<Pair<Node, Diff<T>>, Pair<Double, Double>> searchSimFixedMethod(final String buggyFile,
                                                                                          final String fixedFile,
                                                                                          final Node node, Class<?
            extends Diff<T>> diff, final double simThreshold) {
        final Map<Pair<Node, Diff<T>>, Pair<Double, Double>> map = new HashMap<>();
        final FVector fVector = node.getFeatureVector();
        if (fVector == null) return map;
        Set<Pair<Node, Diff<T>>> candidates = null;
        try {
            candidates = filterChangedMethods(buggyFile, fixedFile, diff);
        } catch (Exception e) {
            return map;
        }
        double biggetDis = 1.0 - simThreshold;
        for (Pair<Node, Diff<T>> pair : candidates) {
            Node sim = pair.getFirst();
            double norm = fVector.computeSimilarity(sim.getFeatureVector(), FVector.ALGO.NORM_2);
            double cosine = fVector.computeSimilarity(sim.getFeatureVector(), FVector.ALGO.COSINE);
            if (norm < biggetDis && cosine > simThreshold) {
                map.put(pair, new Pair<Double, Double>(norm, cosine));
            }
        }

        return map;
    }

    private static <T> Set<Pair<Node, Diff<T>>> filterChangedMethods(final String buggyFile, final String fixedFile,
                                                                     Class<? extends Diff<T>> diffclazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        CompilationUnit buggyUnit = JavaFile.genASTFromFileWithType(buggyFile);
        CompilationUnit fixedUnit = JavaFile.genASTFromFileWithType(fixedFile);
        Set<Pair<Node, Diff<T>>> set = new HashSet<>();
        List<Pair<MethodDeclaration, MethodDeclaration>> pairs = Matcher.match(buggyUnit, fixedUnit);
        NodeParser parser = NodeParser.getInstance();
        for (Pair<MethodDeclaration, MethodDeclaration> pair : pairs) {
            Node buggyNode = parser.setCompilationUnit(buggyFile, buggyUnit).process(pair.getFirst());
            Node fixedNode = parser.setCompilationUnit(fixedFile, fixedUnit).process(pair.getSecond());
            if (buggyNode.toSrcString().toString().equals(fixedNode.toSrcString().toString())) continue;
            Diff<T> diff = diffclazz.getConstructor().newInstance(buggyNode, fixedNode);
            set.add(new Pair<Node, Diff<T>>(buggyNode, diff));
        }
        return set;
    }
}
