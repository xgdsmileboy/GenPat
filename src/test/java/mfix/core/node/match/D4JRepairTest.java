/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.TestCase;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.parser.NodeParser;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-06
 */
public class D4JRepairTest extends TestCase {

    String base = Utils.join(Constant.SEP, Constant.RES_DIR, "d4j-info", "buggy_fix");


    @Test
    public void test_chart_1() {
        test("chart", 1);
    }

    @Test
    public void test_math_1() {
        test("math", 1);
    }

    @Test
    public void test_chart_4() {
        test("chart", 4);
    }

    @Test
    public void test_chart_6() {
        test("chart", 6);
    }


    private List<Pair<String, String>> buildFilePairs(String d4jproj, int id) {
        List<Pair<String, String>> list = new LinkedList<>();
        String bhome = "buggy/" + d4jproj + "/" + d4jproj + "_" + id + "_buggy";
        String fhome = "fixed/" + d4jproj + "/" + d4jproj + "_" + id + "_fixed";
        List<String> files = JavaFile.ergodic(Utils.join(Constant.SEP, base, bhome), new LinkedList<>());
        File tmpFile;
        for (String f : files) {
            tmpFile = new File(f.replace(bhome, fhome));
            if (tmpFile.exists()) {
                list.add(new Pair<>(f, tmpFile.getAbsolutePath()));
            }
        }
        return list;
    }

    public void test(String d4jproj, int id) {
        List<Pair<String, String>> pairs = buildFilePairs(d4jproj, id);

        for (Pair<String, String> pair : pairs) {
            String srcFile = pair.getFirst();
            String tarFile = pair.getSecond();

            PatternExtractor extractor = new PatternExtractor();
            Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

            String buggy = srcFile;
            Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

            CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
            final Set<MethodDeclaration> methods = new HashSet<>();
            unit.accept(new ASTVisitor() {
                public boolean visit(MethodDeclaration node) {
                        methods.add(node);
                    return true;
                }
            });

            for (Pattern pattern : patterns) {
                System.out.println(pattern.formalForm());
            }

            NodeParser parser = new NodeParser();
            parser.setCompilationUnit(buggy, unit);
            for (MethodDeclaration m : methods) {
                Node node = parser.process(m);
                VarScope scope = varMaps.getOrDefault(node.getStartLine(), new VarScope());
                Set<String> already = new HashSet<>();
                int count = 0;
                for (Pattern p : patterns) {
                    scope.reset(p.getNewVars());
                    Set<MatchInstance> set = Matcher.tryMatch(node, p);
                    for (MatchInstance matchInstance : set) {
                        matchInstance.apply();
                        StringBuffer buffer = node.adaptModifications(scope, new HashMap<>());
                        if (buffer != null) {
                            String tmp = buffer.toString();
                            if (!already.contains(tmp)) {
                                already.add(tmp);
                                count ++;
                                TextDiff diff = new TextDiff(node.toSrcString().toString(), tmp);
                                System.out.println(diff.toString());
                            }
                        }
                        matchInstance.reset();
                    }
                }
                System.out.println(count);
            }
        }
    }



}
