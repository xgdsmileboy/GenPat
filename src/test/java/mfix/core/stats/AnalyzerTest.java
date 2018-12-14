package mfix.core.stats;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.core.TestCase;
import mfix.core.parse.NodeParser;
import mfix.core.stats.element.*;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTVisitor;


public class AnalyzerTest extends TestCase {
    static class MethodDeclCollector extends ASTVisitor {

        List<MethodDeclaration> methodDeclarations;

        public MethodDeclCollector() {
        }

        public void init() {
            methodDeclarations = new LinkedList<>();
        }

        public List<MethodDeclaration> getAllMethDecl() {
            return methodDeclarations;
        }

        public boolean visit(MethodDeclaration md) {
            methodDeclarations.add(md);
            return true;
        }
    }

    @Test
    public void test_parse() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";

        CompilationUnit srcUnit = JavaFile.genASTFromFileWithType(srcFile, null);

        // Create a new table for test.
        DatabaseConnector connector = new DatabaseConnector();
        connector.setAsTestMode();
        connector.open();
        connector.dropTable(); // Drop the table if exist.
        connector.createTable();

        Analyzer nodeAnalyzer = Analyzer.getInstance();

        MethodDeclCollector methodDeclCollector = new MethodDeclCollector();
        methodDeclCollector.init();
        srcUnit.accept(methodDeclCollector);
        List<MethodDeclaration> srcMethods = methodDeclCollector.getAllMethDecl();

        NodeParser nodePaser = NodeParser.getInstance();

        for(MethodDeclaration method: srcMethods) {
            nodePaser.setCompilationUnit(srcFile, srcUnit);
            nodeAnalyzer.setFileName(srcFile);
            nodeAnalyzer.analyze(nodePaser.process(method));
        }
        nodeAnalyzer.finish();

        ElementCounter counter = new ElementCounter();
        counter.open();

        Element VarElementA = new VarElement("path", "org.apache.tools.ant.Path", srcFile);
        Element VarElementB = new VarElement("path", "java.lang.StringBuffer", srcFile);
        Element VarElementC = new VarElement("noThisVar", "java.lang.StringBuffer", srcFile);
        Element methodElementD = new MethodElement("close", srcFile);

        ElementQueryType sameTypeInAllFiles = new ElementQueryType(true, ElementQueryType.CountType.ALL);
        ElementQueryType withoutTypeInAllFiles = new ElementQueryType(false, ElementQueryType.CountType.ALL);
        ElementQueryType withoutTypeCountFiles = new ElementQueryType(false, ElementQueryType.CountType.COUNT_FILES);

        Assert.assertTrue(counter.count(VarElementA, sameTypeInAllFiles) == 1);
        Assert.assertTrue(counter.count(VarElementA, withoutTypeInAllFiles) == 6);
        Assert.assertTrue(counter.count(VarElementA, withoutTypeCountFiles) == 1);

        Assert.assertTrue(counter.count(VarElementB, sameTypeInAllFiles) == 5);

        Assert.assertTrue(counter.count(VarElementC, sameTypeInAllFiles) == 0);
        Assert.assertTrue(counter.count(VarElementC, withoutTypeInAllFiles) == 0);
        Assert.assertTrue(counter.count(VarElementC, withoutTypeCountFiles) == 0);

        Assert.assertTrue(counter.count(methodElementD, withoutTypeInAllFiles) == 2);

        counter.close();

        // Drop the new table for test.
        connector.dropTable();
        connector.close();
    }
}
