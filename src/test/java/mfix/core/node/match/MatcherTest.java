/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.match;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.TestCase;
import mfix.core.node.MatchInstance;
import mfix.core.node.NodeUtils;
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.ast.stmt.ReturnStmt;
import mfix.core.node.ast.stmt.SwCase;
import mfix.core.node.ast.stmt.SwitchStmt;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.node.parser.NodeParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/25
 */
public class MatcherTest extends TestCase {

    @Test
    public void test_match_dont() {
        String srcFile_change_retType = testbase + Constant.SEP + "src_Intersect.java";
        String tarFile_change_retType = testbase + Constant.SEP + "tar_Intersect.java";
        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile_change_retType);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile_change_retType);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);

        // The method signature cannot match
        // TODO: should permit some methods failing to match
        Assert.assertTrue(matchMap.isEmpty());
    }

    @Test
    public void test_match_do() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile);
        CompilationUnit tarUnit = JavaFile.genASTFromFileWithType(tarFile);
        List<Pair<MethodDeclaration, MethodDeclaration>> matchMap = Matcher.match(srcUnit, tarUnit);

        // all methods are well matched
        Assert.assertTrue(matchMap.size() == 108);
    }

    @Test
    public void test_modification_generation() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Node> patterns = extractor.extractPattern(srcFile, tarFile);

        // there is only one method changed
        Assert.assertTrue(patterns.size() == 1);

        Node node = patterns.iterator().next();
        // there should be only one modification, which surrounds
        // a method invocation with an if statement
        Assert.assertTrue(node.getAllModifications(new HashSet<>()).size() == 1);
        Modification modification = node.getAllModifications(new HashSet<>()).iterator().next();
        Assert.assertTrue(modification instanceof Update);
    }

    @Test
    public void test_match_demo_fix() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Node> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = testbase + Constant.SEP + "buggy_SimpleSecureBrowser.java";
        Map<Integer, Set<String>> varMaps = NodeUtils.getUsableVarTypes(buggy);

        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if ("onPostExecute".equals(node.getName().getIdentifier())
                        && node.parameters().size() == 1) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });

        Assert.assertTrue(methods.size() == 1);
        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(buggy, unit);
        Node node = parser.process(methods.iterator().next());
        Assert.assertTrue(node != null);

        String target = "@Override protected void onPostExecute(AnswerObject result){\n" +
                "if(outerCallingListActivity==null){\n" +
                "dialog.setTitle(outerCallingActivity.getResources().getString(R.string.ui_calc));\n" +
                "outerCallingActivity.onPostExecute(result);\n" +
                "}else {\n" +
                "dialog.setTitle(outerCallingListActivity.getResources().getString(R.string.ui_calc));\n" +
                "outerCallingListActivity.onPostExecute(result);\n" +
                "}\n" +
                "if(dialog.isShowing()){\n" +
                "dialog.dismiss();\n" +
                "}\n" +
                "}";

        Set<Node> matched = Matcher.filter(node, patterns);
        Assert.assertTrue(matched.size() == 1);

        Set<MatchInstance> set = Matcher.tryMatch(node, matched.iterator().next());
        Assert.assertTrue(set.size() == 1);

        MatchInstance instance = set.iterator().next();
        instance.apply();
        StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), instance.getStrMap());

        Assert.assertTrue(buffer != null);
        Assert.assertTrue(target.equals(buffer.toString()));
        instance.reset();
    }

    @Test
    public void test_match_switch_stmt() {
        String srcFile = testbase + Constant.SEP + "src_insert_under_switch.java";
        String tarFile = testbase + Constant.SEP + "tar_insert_under_switch.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Node> patterns = extractor.extractPattern(srcFile, tarFile);
        Node node = patterns.iterator().next();
        List<Modification> modifications = new ArrayList<>(node.getAllModifications(new HashSet<>()));

        Assert.assertTrue(modifications.size() == 1);
        Assert.assertTrue(modifications.get(0) instanceof Insertion);

        Insertion insertion = (Insertion) modifications.get(0);
        Assert.assertTrue(insertion.getInsertedNode() instanceof SwCase);
        Assert.assertTrue(insertion.getParent() instanceof SwitchStmt);
        Assert.assertTrue(insertion.getPrenode() instanceof ReturnStmt);
        Assert.assertTrue(insertion.getNextnode() instanceof SwCase);

    }

    @Test
    public void test_node_serialization_deserialization() throws Exception {
        String srcFile = testbase + Constant.SEP + "src_registrationActivity.java";
        String tarFile = testbase + Constant.SEP + "tar_registrationActivity.java";

        PatternExtractor extractor = new PatternExtractor();
        List<Node> patterns = new ArrayList<>(extractor.extractPattern(srcFile, tarFile));
        String path = "/tmp/";
        int index = 0;
        for (; index < patterns.size(); index++) {
            Utils.serialize(patterns.get(index), path + index + ".pattern");
        }
        for (int j = 0; j < index; j++) {
            Node node = (Node) Utils.deserialize(path + j + ".pattern");
            Assert.assertTrue(patterns.get(j).toSrcString().toString().equals(node.toSrcString().toString()));
            new File(path + j + ".pattern").delete();
        }
    }

    @Test
    public void test_match_demo_fix_fail() {
        String srcFile = testbase + Constant.SEP + "src_false_dismiss.java";
        String tarFile = testbase + Constant.SEP + "tar_false_dismiss.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Node> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = testbase + Constant.SEP + "buggy_SimpleSecureBrowser.java";

        Map<Integer, Set<String>> varMaps = NodeUtils.getUsableVarTypes(buggy);

        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                methods.add(node);
                return true;
            }
        });

        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(buggy, unit);
        for (MethodDeclaration m : methods) {
            Node node = parser.process(m);
            Set<Node> matched = Matcher.filter(node, patterns);
            for (Node p : matched) {
                Set<MatchInstance> set = Matcher.tryMatch(node, p);
                for (MatchInstance matchInstance : set) {
                    matchInstance.apply();
                    Assert.assertTrue(node.adaptModifications(varMaps.get(node.getStartLine()),
                            matchInstance.getStrMap()) == null);
                    matchInstance.reset();
                }
            }
        }
    }

//    @Test
    public void temp() {
        String srcFile = "/Users/Jiajun/Desktop/buggy.java";
        String tarFile = "/Users/Jiajun/Desktop/fixed.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Node> patterns = extractor.extractPattern(srcFile, tarFile);

        for (Node pattern : patterns) {

            Set<Modification> modifications = pattern.getAllModifications(new HashSet<>());
            for (Modification m : modifications) {
                System.out.println(m);
            }

            Set<MethodInv> APIs = pattern.getUniversalAPIs(new HashSet<>(), true);
            for (MethodInv api : APIs) {
                System.out.println(api.getName().getName());
            }
        }
    }

}
