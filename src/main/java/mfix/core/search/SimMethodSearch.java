/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.search;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.io.IOException;
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
        if(ounit == null) return new HashMap<>();
        final NodeParser parser = new NodeParser();
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
        if(unit == null) return map;
        final NodeParser parser = new NodeParser();
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

    public static Map<Pair<Node, TextDiff>, Pair<Double, Double>> searchSimFixedMethod(final String buggyFile,
                                                                                          final String fixedFile,
                                                                                          final Node node, final double simThreshold) {
        final Map<Pair<Node, TextDiff>, Pair<Double, Double>> map = new HashMap<>();
        final FVector fVector = node.getFeatureVector();
        if (fVector == null) return map;
        double biggetDis = 1.0 - simThreshold;

        CompilationUnit buggyUnit = JavaFile.genASTFromFileWithType(buggyFile);
        CompilationUnit fixedUnit = JavaFile.genASTFromFileWithType(fixedFile);
        if(buggyUnit == null || fixedUnit == null) return map;
        Set<Pair<Node, TextDiff>> set = new HashSet<>();
        Matcher matcher = new Matcher();
        List<Pair<MethodDeclaration, MethodDeclaration>> pairs = matcher.match(buggyUnit, fixedUnit);
        NodeParser parser = new NodeParser();
        File file = new File(buggyFile);
        String name = file.getName();
        String path = file.getParentFile().getParentFile().getAbsolutePath() + Constant.SEP + "serial";
        File base = new File(path);
        if (!base.exists()) {
            base.mkdirs();
        }
        int count = 1;
        for (Pair<MethodDeclaration, MethodDeclaration> pair : pairs) {
            String bNode = path + Constant.SEP + name + "_" + count + "b";
            String fNode = path + Constant.SEP + name + "_" + count + "f";
            count ++;
            Node buggyNode = null;
            Node fixedNode = null;
            if (new File(bNode).exists() && new File(fNode).exists()) {
//                try {
//                    buggyNode = (Node) Utils.deserialize(bNode);
//                    fixedNode = (Node) Utils.deserialize(fNode);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
            }
            if (buggyNode == null || fixedNode == null) {
                buggyNode = parser.setCompilationUnit(buggyFile, buggyUnit).process(pair.getFirst());
                fixedNode = parser.setCompilationUnit(fixedFile, fixedUnit).process(pair.getSecond());
                try {
                    Utils.serialize(buggyNode, bNode);
                    Utils.serialize(fixedNode, fNode);
                } catch (IOException e) {
                }
            }
            if (buggyNode.toSrcString().toString().equals(fixedNode.toSrcString().toString())) continue;

            double norm = fVector.computeSimilarity(buggyNode.getFeatureVector(), FVector.ALGO.NORM_2);
            double cosine = fVector.computeSimilarity(buggyNode.getFeatureVector(), FVector.ALGO.COSINE);
            if (norm < biggetDis && cosine > simThreshold) {
                TextDiff diff = new TextDiff(buggyNode, fixedNode);
                map.put(new Pair<Node, TextDiff>(buggyNode, diff), new Pair<Double, Double>(norm, cosine));
            }
        }

        return map;
    }

}
