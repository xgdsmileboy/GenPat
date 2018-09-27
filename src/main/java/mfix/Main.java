/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix;

import mfix.common.java.D4jSubject;
import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.locator.D4JManualLocator;
import mfix.core.locator.Location;
import mfix.core.parse.NodeParser;
import mfix.core.parse.diff.Diff;
import mfix.core.parse.diff.TextDiff;
import mfix.core.parse.diff.text.Line;
import mfix.core.parse.node.Node;
import mfix.core.search.ExtractFaultyCode;
import mfix.core.search.SimMethodSearch;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/19
 */
public class Main {

    private static void process(D4jSubject subject, Set<String> codeBase) {
        String outPath = Utils.join(Constant.SEP, Constant.TMP_OUT, subject.getName(), String.valueOf(subject.getId()));
        String infoFile = outPath + Constant.SEP + "info.log";

        // log start info
        Utils.log(infoFile, "START: " + new Date().toString() + Constant.NEW_LINE, false);
        Utils.log(infoFile, "SUBJECT: " + subject.getName() + "_" + subject.getId() + Constant.NEW_LINE, false);

        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        Set<String> ignoreKeys = new HashSet<>();
        ignoreKeys.add("fixed-version");
        int locationID = 1;
        for (Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome() + subject.getSsrc(), location.getRelClazzFile());
            MethodDeclaration method = ExtractFaultyCode.extractFaultyMethod(file, location.getLine());
            CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
            NodeParser parser = NodeParser.getInstance();
            parser.setCompilationUnit(file, unit);
            Node fnode = parser.process(method);

            // record faulty node info
            Utils.log(Utils.join(Constant.SEP, outPath, String.valueOf(locationID++), "faulty.log"), file,
                    fnode.getStartLine(), fnode.getEndLine(), method.toString(), false);
            // log current location info
            Utils.log(infoFile, "LOCATION : " + location.getRelClazzFile() + "#" +
                    location.getLine() + Constant.NEW_LINE, true);

            int similarCount = 1;
            for (String base : codeBase) {
                List<File> files = JavaFile.ergodic(new File(base), new LinkedList<File>(), ignoreKeys, ".java");
                int size = files.size();

                // log file size for searching
                Utils.log(infoFile, "CODEBASE : " + base + "(FILE: " + size + ")", true);

                for (File buggy : files) {
                    System.out.println(size--);
                    String fixed = buggy.getAbsolutePath().replace("buggy-version", "fixed-version");
                    Map<Pair<Node, Diff<Line>>, Pair<Double, Double>> candidates =
                            SimMethodSearch.searchSimFixedMethod(buggy.getAbsolutePath(), fixed, fnode, TextDiff.class,
                                    0.7);
                    for (Map.Entry<Pair<Node, Diff<Line>>, Pair<Double, Double>> entry : candidates.entrySet()) {
                        Node node = entry.getKey().getFirst();
                        Diff<Line> diff = entry.getKey().getSecond();
                        Pair<Double, Double> similarity = entry.getValue();

                        Utils.log(Utils.join(Constant.SEP, outPath, java.lang.String.valueOf(similarCount++), ".log"),
                                buggy.getAbsolutePath(), node.getStartLine(),
                                node.getEndLine(), similarity.getFirst(), similarity.getSecond(),
                                node.toSrcString().toString(),
                                diff, false);
                    }
                }
            }
        }
        Utils.log(infoFile, "FINISH : " + new Date().toString() + Constant.NEW_LINE, true);

    }

    public static void main(String[] args) {
        String base = Utils.join(Constant.SEP, Constant.HOME, "resources", "forTest");
        Set<String> codeBase = new HashSet<>();
        Map<String, Integer> path2version = new HashMap<>();
        path2version.put("2011", 5);
        path2version.put("2012-2014", 69);
        path2version.put("2015", 40);
        path2version.put("2016", 49);
        path2version.put("2017", 60);

        for (Map.Entry<String, Integer> entry : path2version.entrySet()) {
            for (int i = 1; i < entry.getValue(); i++) {
                codeBase.add("/home/lee/Xia/GitHubData/MissSome/" + entry.getKey() + "/V" + i);
            }
        }
        D4jSubject subject = new D4jSubject(base, "chart", 1);
        process(subject, codeBase);
    }

}
