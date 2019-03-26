/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.pattern;

import mfix.TestCase;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.match.MatchInstance;
import mfix.core.node.match.Matcher;
import mfix.core.node.modify.Modification;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-25
 */
public class MuBenchPatternTest extends TestCase {

    private String base = Utils.join(Constant.SEP, testbase, "pattern4Mubench");

    @Test
    public void test_aclang() {
        test("aclang", 1);
    }

    @Test
    public void test_acmath_1() {
        test("acmath", 1);
    }

    @Test
    public void test_acmath_2() {
        test("acmath", 2);
    }

    @Test
    public void test_adempiere() {
        test("adempiere", 1);
    }

    @Test
    public void test_alibaba_druid_1() {
        test("alibaba-druid", 1);
    }

    @Test
    public void test_apache_gora() {
        test("apache-gora", 1);
    }

    @Test
    public void test_apdpat_1() {
        test("apdpat", 1);
    }

    private void test(String name, int id) {
        String srcFile = base + Constant.SEP  + name + Constant.SEP + id + "/buggy.java";
        String tarFile = base + Constant.SEP  + name + Constant.SEP + id + "/fixed.java";
        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);
        for (Pattern p : patterns) {
            System.out.println(p.formalForm());
            for (Modification modification : p.getAllModifications()) {
                System.out.println(modification);
            }

            for (Node node : p.getConsideredNodes()) {
                System.out.println(node);
            }
        }

        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(srcFile);

        CompilationUnit unit = JavaFile.genASTFromFileWithType(srcFile);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                methods.add(node);
                return true;
            }
        });

        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(srcFile, unit);
        for (MethodDeclaration m : methods) {
            MethDecl node = (MethDecl)parser.process(m);
            String retType = node.getRetTypeStr();
            Set<String> exceptions = new HashSet<>(node.getThrows());
            VarScope scope = varMaps.getOrDefault(node.getStartLine(), new VarScope());
            Set<String> already = new HashSet<>();
            int count = 0;
            for (Pattern p : patterns) {
                scope.reset(p.getNewVars());
                Set<MatchInstance> set = Matcher.tryMatch(node, p);
                for (MatchInstance matchInstance : set) {
                    matchInstance.apply();
                    StringBuffer buffer = node.adaptModifications(scope, new HashMap<>(), retType, exceptions);
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
