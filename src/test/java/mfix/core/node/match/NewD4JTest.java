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
import mfix.core.node.modify.Adaptee;
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
    public void test_CommonsJXPath_1() throws Exception {
        temp_pattern("CommonsJXPath-1", "prepare");
    }

    @Test
    public void test_mockito_22() throws Exception {
        temp_pattern("mockito-22", "areEqual");
    }

    @Test
    public void test_closure_2() throws Exception {
        temp_pattern("closure-2", "checkInterfaceConflictProperties");
    }

    @Test //closure-63 is the same
    public void test_closure_62() throws Exception {
        temp_pattern("closure-62", "format");
    }

    @Test //closure-93 is the same
    public void test_closure_73() throws Exception {
        temp_pattern("closure-73", "strEscape");
    }

    @Test
    public void test_closure_86() throws Exception {
        temp_pattern("closure-86", "evaluatesToLocalValue");
    }

    @Test //closure-93 is the same
    public void test_closure_92() throws Exception {
        temp_pattern("closure-92", "replace");
    }

    @Test
    public void test_closure_115() throws Exception {
        temp_pattern("closure-115", "canInlineReferenceDirectly");
    }

    @Test
    public void test_lang_16() throws Exception {
        temp_pattern("lang-16", "createNumber");
    }

    @Test
    public void test_lang_21() throws Exception {
        temp_pattern("lang-21", "isSameLocalTime");
    }

    @Test
    public void test_lang_33() throws Exception {
        temp_pattern("lang-33", "toClass");
    }

    @Test
    public void test_lang_47() throws Exception {
//        temp_pattern("lang-47", "appendFixedWidthPadLeft");
        temp_pattern("lang-47", "appendFixedWidthPadRight");
    }

    @Test
    public void test_lang_60() throws Exception {
        temp_pattern("lang-60", "contains");
        temp_pattern("lang-60", "indexOf");
    }

    @Test
    public void test_chart_1() throws Exception {
        temp_pattern("chart-1", "getLegendItems");
    }

    @Test
    public void test_chart_4() throws Exception {
        temp_pattern("chart-4", "getDataRange");
    }

    @Test
    public void test_chart_11() throws Exception {
        temp_pattern("chart-11", "equal");
    }

    @Test
    public void test_chart_24() throws Exception {
        temp_pattern("chart-24", "getPaint");
    }

    @Test // the second place is the same
    public void test_math_4() throws Exception {
        temp_pattern("math-4", "intersection");
    }

    @Test
    public void test_math_22() throws Exception {
        temp_pattern("math-22-1", "isSupportLowerBoundInclusive");
        temp_pattern("math-22-2", "isSupportUpperBoundInclusive");
    }

    @Test
    public void test_math_70() throws Exception {
        temp_pattern("math-70", "solve");
    }

    public void temp_pattern(final String bugId, final String method) throws Exception {
        Pattern p = (Pattern) Utils.deserialize(Constant.HOME + "/tmp/observe/" + bugId + ".pattern");
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
        Adaptee adaptee = null;
        for (MethodDeclaration m : methods) {
            Node node = parser.process(m);
            List<MatchInstance> set = matcher.tryMatch(node, p, need2Match);
            for (MatchInstance matchInstance : set) {
                matchInstance.apply();
                adaptee = new Adaptee(0);
                StringBuffer buffer = node.adaptModifications(varMaps.get(node.getStartLine()), matchInstance.getStrMap(),
                        "Class", new HashSet<>(), adaptee);
                if (buffer != null) {
                    TextDiff diff = new TextDiff(node.toSrcString().toString(), buffer.toString());
                    System.out.println(diff.toString());
                    System.out.println("TOTAL CHANGE : " + adaptee.getAll());
                    System.out.println("INS : " + adaptee.getIns());
                    System.out.println("UPD : " + adaptee.getUpd());
                    System.out.println("DEL : " + adaptee.getDel());
                }
                matchInstance.reset();
            }
        }
    }
}
