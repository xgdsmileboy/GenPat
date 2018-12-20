package mfix.core.stats;

import mfix.common.util.Constant;
import mfix.core.TestCase;
import mfix.core.stats.element.*;

import org.junit.Assert;
import org.junit.Test;

public class AnalyzerTest extends TestCase {
    @Test
    public void test_parse() throws ElementException {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String srcFile2 = testbase + Constant.SEP + "src_Intersect.java";

        // Create a new table for test.
        DatabaseConnector connector = new DatabaseConnector();
        connector.setAsTestMode();
        connector.open();
        connector.dropTable(); // Drop the table if exist.
        connector.createTable();

        Analyzer analyzer = Analyzer.getInstance();
        analyzer.open();
        analyzer.runFile(srcFile);
        analyzer.runFile(srcFile2);
        analyzer.finish();

        ElementCounter counter = new ElementCounter();
        counter.open();

        ElementQueryType withTypeInAllFiles = new ElementQueryType(true, false, ElementQueryType.CountType.ALL);
        ElementQueryType withoutTypeInAllFiles = new ElementQueryType(false, false, ElementQueryType.CountType.ALL);
        ElementQueryType withoutTypeCountFiles = new ElementQueryType(false, false, ElementQueryType.CountType.COUNT_FILES);
        ElementQueryType withTypeInAllFilesOutputPercent = new ElementQueryType(false, true, ElementQueryType.CountType.COUNT_FILES);
        ElementQueryType withoutTypeCountFilesOutputPercent = new ElementQueryType(false, true, ElementQueryType.CountType.COUNT_FILES);

        Element VarElementA = new VarElement("path", "org.apache.tools.ant.Path", srcFile);
        Element VarElementB = new VarElement("path", "java.lang.StringBuffer", srcFile);
        Element VarElementC = new VarElement("noThisVar", "java.lang.StringBuffer", srcFile);

        Assert.assertTrue(counter.count(VarElementA, withTypeInAllFiles) == 2);
        Assert.assertTrue(counter.count(VarElementA, withoutTypeInAllFiles) == 7);
        Assert.assertTrue(counter.count(VarElementA, withoutTypeCountFiles) == 1);

        Assert.assertTrue(counter.count(VarElementB, withTypeInAllFiles) == 5);

        Assert.assertTrue(counter.count(VarElementC, withTypeInAllFiles) == 0);
        Assert.assertTrue(counter.count(VarElementC, withoutTypeInAllFiles) == 0);
        Assert.assertTrue(counter.count(VarElementC, withoutTypeCountFiles) == 0);

        MethodElement methodElementD = new MethodElement("close", srcFile);
        methodElementD.setArgsNumber(0);
        Assert.assertTrue(counter.count(methodElementD, withoutTypeInAllFiles) == 2);

        MethodElement methodElementE = new MethodElement("setProperty", srcFile);
        methodElementE.setArgsNumber(4);
        Assert.assertTrue(counter.count(methodElementE, withoutTypeInAllFiles) == 2);

        methodElementE.setArgsNumber(3);
        Assert.assertTrue(counter.count(methodElementE, withoutTypeInAllFiles) == 0);

        MethodElement methodElementF = new MethodElement("load", srcFile);
        methodElementF.setArgsNumber(1);
        methodElementF.setArgsType("java.io.InputStream,");
        methodElementF.setObjType("java.util.Properties");
        methodElementF.setRetType("void");
        Assert.assertTrue(counter.count(methodElementF, withTypeInAllFiles) == 2);

        methodElementF.setRetType("int");
        Assert.assertTrue(counter.count(methodElementF, withTypeInAllFiles) == 0);

        Element VarElementG = new VarElement("result", "java.util.List<org.apache.tools.ant.types.resources.Resource>", srcFile2);
        Assert.assertTrue(counter.count(VarElementG, withTypeInAllFilesOutputPercent) == 0.5);
        Assert.assertTrue(counter.count(VarElementG, withoutTypeCountFilesOutputPercent) == 0.5);

        counter.close();

        // Drop the new table for test.
        connector.dropTable();
        connector.close();
    }
}
