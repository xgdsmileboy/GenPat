package mfix.core.stats;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.core.TestCase;
import mfix.core.parse.NodeParser;
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

        Element elementA = new Element("path", "org.apache.tools.ant.Path");

        QueryElement queryElementA1 = new QueryElement(elementA, true, QueryElement.QueryType.ALL);
        Assert.assertTrue(counter.count(queryElementA1) == 1);

        QueryElement queryElementA2 = new QueryElement(elementA, false, QueryElement.QueryType.ALL);
        Assert.assertTrue(counter.count(queryElementA2) == 5);

        Element elementB = new Element("path", "java.lang.StringBuffer");
        QueryElement queryElementB1 = new QueryElement(elementB, true, QueryElement.QueryType.ALL);
        Assert.assertTrue(counter.count(queryElementB1) == 4);

        Element elementC = new Element("noThisMethod", "java.lang.StringBuffer");
        QueryElement queryElementC1 = new QueryElement(elementC, true, QueryElement.QueryType.ALL);
        QueryElement queryElementC2 = new QueryElement(elementC, false, QueryElement.QueryType.ALL);

        Assert.assertTrue(counter.count(queryElementC1) == 0);
        Assert.assertTrue(counter.count(queryElementC2) == 0);

        counter.close();

        // Drop the new table for test.
        connector.dropTable();
        connector.close();
    }
}
