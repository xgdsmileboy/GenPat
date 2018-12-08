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

        Analyzer nodeAnalyzer = Analyzer.getInstance();

        MethodDeclCollector methodDeclCollector = new MethodDeclCollector();
        methodDeclCollector.init();
        srcUnit.accept(methodDeclCollector);
        List<MethodDeclaration> srcMethods = methodDeclCollector.getAllMethDecl();

        NodeParser nodePaser = NodeParser.getInstance();

        for(MethodDeclaration method: srcMethods) {
            nodePaser.setCompilationUnit(srcFile, srcUnit);
            nodeAnalyzer.analyze(nodePaser.process(method));
        }

        Assert.assertTrue(nodeAnalyzer.getElementFrequency(new Element("currentThread")) == 10);
        Assert.assertTrue(nodeAnalyzer.getElementFrequency(new Element("path")) == 5);

        TypedElement element1 = new TypedElement("path", "org.apache.tools.ant.Path");
        TypedElement element2 = new TypedElement("path", "org.apache.tools.ant.StringBuffer");
        Assert.assertTrue(nodeAnalyzer.getTypedElementFrequency(element1) == 1);
        Assert.assertTrue(nodeAnalyzer.getTypedElementFrequency(element2) == 4);

        Assert.assertTrue(nodeAnalyzer.getElementFrequency(element1) == 5);
    }
}
