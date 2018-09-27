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
import mfix.common.util.Utils;
import mfix.core.locator.D4JManualLocator;
import mfix.core.locator.Location;
import mfix.core.parse.NodeParser;
import mfix.core.parse.node.Node;
import mfix.core.search.ExtractFaultyCode;
import mfix.core.search.SimMethodSearch;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/19
 */
public class Main {

    public static void main(String[] args) {
        String base = Utils.join(Constant.SEP, Constant.HOME, "resources", "forTest");
        String codeBase = "/home/lee/Xia/GitHubData/MissSome/2011/V1";
        String tempFile = Constant.HOME + Constant.SEP + "similar_";
        Set<String> ignoreKeys = new HashSet<>();
        ignoreKeys.add("fixed-version");
        D4jSubject subject = new D4jSubject(base, "chart", 1);
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(100);
        List<File> files = JavaFile.ergodic(new File(codeBase), new LinkedList<File>(), ignoreKeys, ".java");
        System.out.println("Total file : " + files.size());
        int id = 0;
        for (Location location : locations) {
            id ++;
            String file = Utils.join(Constant.SEP, subject.getHome() + subject.getSsrc(), location.getRelClazzFile());
            MethodDeclaration method = ExtractFaultyCode.extractFaultyMethod(file, location.getLine());
            CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
            NodeParser parser = NodeParser.getInstance();
            parser.setCompilationUnit(unit);
            Node fnode = parser.process(method);
            JavaFile.writeStringToFile(tempFile + id + ".log", fnode.toSrcString().toString() + "\n ---------\n", false);
            int fileSize = files.size();
            for (File f : files) {
                System.out.println(fileSize --);
                unit = JavaFile.genASTFromFileWithType(f);
                Set<Node> nodes = SimMethodSearch.searchSimMethod(unit, fnode, 0.95);
                for (Node node : nodes) {
                    JavaFile.writeStringToFile(tempFile + id + ".log", node.toSrcString().toString() + "\n", true);
                }
            }

        }
    }

}
