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
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.parser.NodeParser;
import mfix.core.pattern.Pattern;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-04-24
 */
public class NewD4JTest extends TestCase {

    @Test
    public void test_jsoup_23() throws Exception {
        temp_pattern("jsoup-23", "indexInList");
    }

    @Test
    public void test_mockito_22() throws Exception {
        temp_pattern("mockito-22", "areEqual");
    }

    @Test
    public void test_CommonsJXPath_1() throws Exception {
        temp_pattern("CommonsJXPath-1", "prepare");
    }

    @Test //closure-93 is the same
    public void test_closure_92() throws Exception {
        temp_pattern("closure-92", "replace");
    }

    @Test //closure-93 is the same
    public void test_closure_2() throws Exception {
        temp_pattern("closure-2", "checkInterfaceConflictProperties");
    }

    @Test
    public void test_lang_16() throws Exception {
        temp_pattern("lang-16", "createNumber");
    }

    @Test
    public void test_chart_4_() throws Exception {
        temp_pattern("chart-4", "getDataRange");
    }

    public void temp_pattern(final String bugId, final String method) throws Exception {
        Pattern p = (Pattern) Utils.deserialize(Constant.HOME + "/tmp/" + bugId + ".pattern");
        System.out.println(p.getFileName());
        String buggy = Constant.HOME + "/tmp/" + bugId + ".java";
        Map<Integer, VarScope> varMaps = NodeUtils.getUsableVariables(buggy);

        CompilationUnit unit = JavaFile.genASTFromFileWithType(buggy);
        final Set<MethodDeclaration> methods = new HashSet<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (node.getName().getIdentifier().equals(method)) {
                    methods.add(node);
                    return false;
                }
                return true;
            }
        });

        List<Integer> need2Match = null;//new ArrayList<>(Arrays.asList());
        NodeParser parser = new NodeParser();
        parser.setCompilationUnit(buggy, unit);
        RepairMatcher matcher = new RepairMatcher();
        for (MethodDeclaration m : methods) {
            Node node = parser.process(m);
            List<MatchInstance> set = matcher.tryMatch(node, p, need2Match);
            for (MatchInstance matchInstance : set) {
                matchInstance.apply();
                StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), matchInstance.getStrMap(),
                        "Class", new HashSet<>());
                if (buffer != null) {
                    TextDiff diff = new TextDiff(node.toSrcString().toString(), buffer.toString());
                    System.out.println(diff.toString());
                }
                matchInstance.reset();
            }
        }
    }
}
