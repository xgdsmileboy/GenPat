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

    private static void process(D4jSubject subject, String codeBase) {
        String outPath = Utils.join(Constant.SEP, Constant.TMP_OUT, subject.getName(), String.valueOf(subject.getId()));
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        Set<String> ignoreKeys = new HashSet<>();
        ignoreKeys.add("fixed-version");
        List<File> files = JavaFile.ergodic(new File(codeBase), new LinkedList<File>(), ignoreKeys, ".java");
        int locationID = 1;
        for(Location location : locations) {
            String file = Utils.join(Constant.SEP, subject.getHome() + subject.getSsrc(), location.getRelClazzFile());
            MethodDeclaration method = ExtractFaultyCode.extractFaultyMethod(file, location.getLine());
            CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
            NodeParser parser = NodeParser.getInstance();
            parser.setCompilationUnit(unit);
            Node fnode = parser.process(method);
            Utils.log(Utils.join(Constant.SEP, outPath, String.valueOf(locationID), "faulty.log"), file,
                    fnode.getStartLine(), fnode.getEndLine(), method.toString(), false);
            int count = 1;
            for(File buggy : files) {
                String fixed = buggy.getAbsolutePath().replace("buggy-version", "fixed-version");
                Map<Pair<Node, Diff<Line>>, Pair<Double, Double>> candidates =
                        SimMethodSearch.searchSimFixedMethod(buggy.getAbsolutePath(), fixed, fnode, TextDiff.class,
                                0.7);
                for(Map.Entry<Pair<Node, Diff<Line>>, Pair<Double, Double>> entry : candidates.entrySet()) {
                    Node node = entry.getKey().getFirst();
                    Diff<Line> diff = entry.getKey().getSecond();
                    Pair<Double, Double> similarity = entry.getValue();
                    Utils.log(Utils.join(Constant.SEP, outPath, java.lang.String.valueOf(count), ".log"),
                            buggy.getAbsolutePath(), node.getStartLine(),
                            node.getEndLine(), similarity.getFirst(), similarity.getSecond(),
                            node.toSrcString().toString(),
                            diff, false);
                }
            }
        }
    }

    public static void main(String[] args) {
        String base = Utils.join(Constant.SEP, Constant.HOME, "resources", "forTest");
        String codeBase = "/home/lee/Xia/GitHubData/MissSome/2011/V1";
        D4jSubject subject = new D4jSubject(base, "chart", 1);
        process(subject, codeBase);
    }

}
