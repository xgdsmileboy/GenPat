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
import mfix.core.node.ast.stmt.ReturnStmt;
import mfix.core.node.ast.stmt.SwCase;
import mfix.core.node.ast.stmt.SwitchStmt;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.node.parser.NodeParser;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        // there is only one method changed
        Assert.assertTrue(patterns.size() == 1);

        Pattern pattern = patterns.iterator().next();
        // there should be only one modification, which surrounds
        // a method invocation with an if statement
        Assert.assertTrue(pattern.getAllModifications().size() == 1);
        Modification modification = pattern.getAllModifications().iterator().next();
        Assert.assertTrue(modification instanceof Update);
    }

    @Test
    public void test_match_demo_fix() {
        String srcFile = testbase + Constant.SEP + "src_CustomSelectionPopUp.java";
        String tarFile = testbase + Constant.SEP + "tar_CustomSelectionPopUp.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = testbase + Constant.SEP + "buggy_SimpleSecureBrowser.java";
        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

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

        Set<Pattern> matched = Matcher.filter(node, patterns);
        Assert.assertTrue(matched.size() == 1);

        List<MatchInstance> set = Matcher.tryMatch(node, matched.iterator().next());
        Assert.assertTrue(set.size() == 1);

        MatchInstance instance = set.get(0);
        instance.apply();
        StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), instance.getStrMap(), "void",
                new HashSet<>());

        Assert.assertTrue(buffer != null);
        Assert.assertTrue(target.equals(buffer.toString()));
        instance.reset();
    }

    @Test
    public void test_match_switch_stmt() {
        String srcFile = testbase + Constant.SEP + "src_insert_under_switch.java";
        String tarFile = testbase + Constant.SEP + "tar_insert_under_switch.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);
        Pattern pattern = patterns.iterator().next();
        List<Modification> modifications = new ArrayList<>(pattern.getAllModifications());

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
        List<Pattern> patterns = new ArrayList<>(extractor.extractPattern(srcFile, tarFile));
        String path = "/tmp/";
        int index = 0;
        for (; index < patterns.size(); index++) {
            Utils.serialize(patterns.get(index), path + index + ".pattern");
        }
        for (int j = 0; j < index; j++) {
            Pattern pattern = (Pattern) Utils.deserialize(path + j + ".pattern");
            Node deserialized = pattern.getPatternNode();
            Node original = patterns.get(j).getPatternNode();
            Assert.assertTrue(deserialized.toSrcString().toString().equals(original.toSrcString().toString()));
            new File(path + j + ".pattern").delete();
        }
    }

    @Test
    public void test_match_demo_fix_fail() {
        String srcFile = testbase + Constant.SEP + "src_false_dismiss.java";
        String tarFile = testbase + Constant.SEP + "tar_false_dismiss.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = testbase + Constant.SEP + "buggy_SimpleSecureBrowser.java";

        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

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
            Set<Pattern> matched = Matcher.filter(node, patterns);
            for (Pattern p : matched) {
                List<MatchInstance> set = Matcher.tryMatch(node, p);
                for (MatchInstance matchInstance : set) {
                    matchInstance.apply();
                    Assert.assertTrue(node.adaptModifications(varMaps.get(node.getStartLine()),
                            matchInstance.getStrMap(), "void", new HashSet<>()) == null);
                    matchInstance.reset();
                }
            }
        }
    }

    @Test
    public void test_formal_form() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String tarFile = testbase + Constant.SEP + "tar_Project.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        /*  // Pattern form
        METHOD(){
            TYPE_0 listeners=EXPR_1;
            for(EXPR_4;i<listeners.size();EXPR_5){}
        }
        */

        String p1str = "METHOD\\(\\)\\{\n" +
                "TYPE(_\\d+)?\\s{1}listeners=EXPR(_\\d+)?;\n" +
                "for\\(EXPR(_\\d+)?;i<listeners.size\\(\\);EXPR(_\\d+)?\\)\\{\\}\n" +
                "\\}";
        String p2str = "METHOD\\(\\)\\{\n" +
                "TYPE(_\\d+)?\\s{1}listeners=EXPR(_\\d+)?;\n" +
                "synchronized\\(EXPR(_\\d+)?\\)\\{\n" +
                "for\\(EXPR(_\\d+)?;i<listeners.size\\(\\);EXPR(_\\d+)?\\)\\{\\}\n" +
                "\\}\n" +
                "\\}";

        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(p1str);
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(p2str);

        for (Pattern p : patterns) {
            String formal = p.formalForm().toString();
            Assert.assertTrue(p1.matcher(formal).matches() || p2.matcher(formal).matches());
        }
    }

    @Test
    public void test_secure_add_import() {
        String srcFile = testbase + Constant.SEP + "src_security_insert_depend_add_import.java";
        String tarFile = testbase + Constant.SEP + "tar_security_insert_depend_add_import.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = testbase + Constant.SEP + "test_security_insert_depend_add_import.java";
        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

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
            for (Pattern p : patterns) {
                StringBuffer b = new StringBuffer();
                for (String ip : p.getImports()) {
                    b.append(ip).append(Constant.NEW_LINE);
                }
                List<MatchInstance> set = Matcher.tryMatch(node, p);
                for (MatchInstance matchInstance : set) {
                    matchInstance.apply();
                    StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()),
                            matchInstance.getStrMap(), "void", new HashSet<>());
                    if (buffer != null) {
                        TextDiff diff = new TextDiff(node.toSrcString().toString(),
                                new StringBuffer(b).append(buffer).toString());
                        System.out.println(diff.toString());
                    }
                    matchInstance.reset();
                }
            }
        }
    }

    @Test //three matches
    public void test_sef_repair_without_filtering() {
        String srcFile = "resources/d4j-info/buggy_fix/buggy/chart/chart_1_buggy" +
                "/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
        String tarFile = "resources/d4j-info/buggy_fix/fixed/chart/chart_1_fixed" +
                "/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = srcFile;
        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if ("getLegendItems".equals(node.getName().getIdentifier())) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });

        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(buggy, unit);
        for (MethodDeclaration m : methods) {
            Node node = parser.process(m);
            for (Pattern p : patterns) {
                List<MatchInstance> set = Matcher.tryMatch(node, p);
                for (MatchInstance matchInstance : set) {
                    matchInstance.apply();
                    StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), new HashMap<>(),
                            "Object", new HashSet<>());
                    if (buffer != null) {
                        TextDiff diff = new TextDiff(node.toSrcString().toString(), buffer.toString());
                        System.out.println(diff.toString());
                    }
                    matchInstance.reset();
                }
            }
        }
    }

    @Test  // only one matches
    public void test_self_repair_with_line_filtering() {
        String srcFile = "resources/d4j-info/buggy_fix/buggy/chart/chart_1_buggy" +
                "/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
        String tarFile = "resources/d4j-info/buggy_fix/fixed/chart/chart_1_fixed" +
                "/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = srcFile;
        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if ("getLegendItems".equals(node.getName().getIdentifier())) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });

        Set<Integer> need2Match = new HashSet<>(Arrays.asList(1797));
        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(buggy, unit);
        for (MethodDeclaration m : methods) {
            Node node = parser.process(m);
            for (Pattern p : patterns) {
                List<MatchInstance> set = Matcher.tryMatch(node, p, need2Match);
                for (MatchInstance matchInstance : set) {
                    matchInstance.apply();
                    StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), new HashMap<>(),
                            "Object", new HashSet<>());
                    if (buffer != null) {
                        TextDiff diff = new TextDiff(node.toSrcString().toString(), buffer.toString());
                        System.out.println(diff.toString());
                    }
                    matchInstance.reset();
                }
            }
        }
    }

//    @Test
    public void temp() {
        String srcFile = "resources/d4j-info/buggy_fix/buggy/chart/chart_1_buggy" +
                "/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";
        String tarFile = "resources/d4j-info/buggy_fix/fixed/chart/chart_1_fixed" +
                "/source/org/jfree/chart/renderer/category/AbstractCategoryItemRenderer.java";

        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(srcFile, tarFile);

        String buggy = srcFile;
        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if ("getLegendItems".equals(node.getName().getIdentifier())) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });

        Set<Integer> need2Match = new HashSet<>(Arrays.asList(1797));
        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(buggy, unit);
        for (MethodDeclaration m : methods) {
            Node node = parser.process(m);
            for (Pattern p : patterns) {
                List<MatchInstance> set = Matcher.tryMatch(node, p, need2Match);
                for (MatchInstance matchInstance : set) {
                    matchInstance.apply();
                    StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), new HashMap<>(),
                            "Object", new HashSet<>());
                    if (buffer != null) {
                        TextDiff diff = new TextDiff(node.toSrcString().toString(), buffer.toString());
                        System.out.println(diff.toString());
                    }
                    matchInstance.reset();
                }
            }
        }
    }
}
