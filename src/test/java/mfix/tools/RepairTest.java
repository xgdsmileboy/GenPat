/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.TestCase;
import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import mfix.core.locator.D4JManualLocator;
import mfix.core.locator.Location;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.parser.NodeParser;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-15
 */
public class RepairTest extends TestCase {

    private String base = Utils.join(Constant.SEP, testbase, "pattern4d4j");
    private String d4jHome = Utils.join(Constant.SEP, Constant.HOME, "projects");

    @Test
    public void test_chart_14() {
        test("chart", 14);
    }

    @Test
    public void test_chart_25() {
        test("chart", 25);
    }

    @Test
    public void test_closure_2() {
        test("closure", 2);
    }


    private void test(String proj, int id) {
        String buggyFile = Utils.join(Constant.SEP, base, proj + "_" + id, "buggy.java");
        String fixedFile = Utils.join(Constant.SEP, base, proj + "_" + id, "fixed.java");
        PatternExtractor extractor = new PatternExtractor();
        Set<Pattern> patterns = extractor.extractPattern(buggyFile, fixedFile);
        D4jSubject subject = new D4jSubject(d4jHome, proj, id);
        subject.configFailedTestCases();
        try {
            subject.backup();
        } catch (IOException e) {
            LevelLogger.error("Backup source file failed!", e);
        }
        D4JManualLocator locator = new D4JManualLocator(subject);
        List<Location> locations = locator.getLocations(3);
        Repair repair = new Repair(subject, null);
        Timer timer = new Timer(Constant.MAX_REPAIR_TIME);
        repair.setTimer(timer);
        timer.start();
        for (Location location : locations) {
            final String file = subject.getHome() + subject.getSsrc() + Constant.SEP + location.getRelClazzFile();
            final String clazzFile = subject.getHome() + subject.getSbin() + Constant.SEP +
                    location.getRelClazzFile().replace(".java", ".class");
            Map<Integer, Set<String>> varMaps = NodeUtils.getUsableVarTypes(file);
            Node node = getBuggyNode(file, location.getLine());
            List<Pattern> list = new LinkedList<>(patterns);//filter(node, patterns);
            Set<String> vars = new HashSet<>();
            for (Pattern p : list) {
                vars.clear();
                vars.addAll(p.getNewVars());
                vars.addAll(varMaps.getOrDefault(node.getStartLine(), new HashSet<>()));
                repair.tryFix(node, p, vars, clazzFile);
            }
        }
        try {
            subject.restore();
        } catch (IOException e) {
            LevelLogger.error("Restore source file failed!", e);
        }

    }

    private List<Pattern> filter(Node node, Set<Pattern> patterns) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);
        Set<String> keys = new HashSet<>();
        while(!queue.isEmpty()) {
            Node n = queue.poll();
            queue.addAll(n.getAllChildren());
            if (NodeUtils.isSimpleExpr(n)) {
                keys.add(n.toSrcString().toString());
            }
        }
        List<Pattern> result = new LinkedList<>();
        for (Pattern p : patterns) {
            boolean containAll = true;
            for (String s : p.getKeywords()) {
                if (!keys.contains(s)) {
                    containAll = false;
                    break;
                }
            }
            if (containAll) {
                result.add(p);
            }
        }
        return result;
    }

    private Node getBuggyNode(String file, final int line) {
        final CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        final List<MethodDeclaration> lst = new ArrayList<>(1);
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                int start = unit.getLineNumber(node.getStartPosition());
                int end = unit.getLineNumber(node.getStartPosition() + node.getLength());
                if (start <= line && line <= end) {
                    lst.add(node);
                    return false;
                }
                return true;
            }
        });
        if (lst.isEmpty()) return null;
        NodeParser parser = new NodeParser();
        return parser.setCompilationUnit(file, unit).process(lst.get(0));
    }


}
