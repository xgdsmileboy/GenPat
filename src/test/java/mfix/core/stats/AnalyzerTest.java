package mfix.core.stats;

import mfix.common.util.Constant;
import mfix.core.TestCase;
import mfix.core.stats.element.*;

import org.junit.Assert;
import org.junit.Test;

public class AnalyzerTest extends TestCase {
    @Test
    public void test() throws Exception {
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
        ElementQueryType withTypeInOneFile = new ElementQueryType(true, false, ElementQueryType.CountType.IN_FILE);

        ElementQueryType withoutTypeInAllFilesOutputPercent = new ElementQueryType(false, true, ElementQueryType.CountType.ALL);
        ElementQueryType withoutTypeCountFilesOutputPercent = new ElementQueryType(false, true, ElementQueryType.CountType.COUNT_FILES);
        ElementQueryType withoutTypeInOneFilesOutputPercent = new ElementQueryType(false, true, ElementQueryType.CountType.IN_FILE);
        ElementQueryType withTypeInOneFilesOutputPercent = new ElementQueryType(true, true, ElementQueryType.CountType.IN_FILE);
        ElementQueryType withTypeInAllFilesOutputPercent = new ElementQueryType(true, true, ElementQueryType.CountType.ALL);

        Element VarElementA = new VarElement("path", "org.apache.tools.ant.Path", null);
        Element VarElementB = new VarElement("path", "java.lang.StringBuffer", null);
        Element VarElementC = new VarElement("noThisVar", "java.lang.StringBuffer", null);

        Assert.assertTrue(counter.count(VarElementA, withTypeInAllFiles) == 2);

        VarElementA.setSourceFile(srcFile);
        Assert.assertTrue(counter.count(VarElementA, withTypeInOneFile) == 2);
        VarElementA.setSourceFile(srcFile2);
        Assert.assertTrue(counter.count(VarElementA, withTypeInOneFile) == 0);

        Assert.assertTrue(counter.count(VarElementA, withoutTypeInAllFiles) == 7);
        Assert.assertTrue(counter.count(VarElementA, withoutTypeCountFiles) == 1);

        Assert.assertTrue(Math.abs(counter.count(VarElementA, withTypeInAllFilesOutputPercent) - 0.001932) <= 1e-5);
        Assert.assertTrue(Math.abs(counter.count(VarElementA, withoutTypeInAllFilesOutputPercent) - 0.006763) <= 1e-5);

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
        Element VarElementI = new VarElement("result", "noSuchType", srcFile2);

        Assert.assertTrue(Math.abs(counter.count(VarElementG, withoutTypeInAllFilesOutputPercent) - 0.0029) <= 1e-3);

        Assert.assertTrue(counter.count(VarElementG, withoutTypeCountFilesOutputPercent) == 0.5);
        Assert.assertTrue(Math.abs(counter.count(VarElementG, withoutTypeInOneFilesOutputPercent) - 0.136) <= 1e-3);
        Assert.assertTrue(counter.count(VarElementI, withTypeInOneFilesOutputPercent) == 0);

        counter.close();

        // Drop the new table for test.
        connector.dropTable();
        connector.close();
    }


    @Test
    public void test_with_cache() throws Exception {
        String srcFile = testbase + Constant.SEP + "src_Project.java";
        String srcFile2 = testbase + Constant.SEP + "src_Intersect.java";
        String cacheResourceFile = testbase + Constant.SEP + "CacheResourceForMethodTableElements.txt";

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
        counter.loadCache(cacheResourceFile);

        ElementQueryType withoutTypeCountFiles = new ElementQueryType(false, false, ElementQueryType.CountType.COUNT_FILES);
        ElementQueryType withoutTypeCountFilesOutputPercent = new ElementQueryType(false, true, ElementQueryType.CountType.COUNT_FILES);

        MethodElement methodElementA = new MethodElement("getPropertyHelper", null);
        methodElementA.setArgsNumber(1);

        Assert.assertTrue(counter.count(methodElementA, withoutTypeCountFiles) == 1);
        Assert.assertTrue(Math.abs(counter.count(methodElementA, withoutTypeCountFilesOutputPercent) - 1.0 / 2) <= 1e-3);

        methodElementA.setArgsNumber(0);
        Assert.assertTrue(counter.count(methodElementA, withoutTypeCountFiles) == 0);
        Assert.assertTrue(Math.abs(counter.count(methodElementA, withoutTypeCountFilesOutputPercent) - 0.0) <= 1e-3);

        MethodElement methodElementB = new MethodElement("size", null);
        methodElementA.setArgsNumber(0);
        Assert.assertTrue(counter.count(methodElementA, withoutTypeCountFiles) == 2);
        Assert.assertTrue(Math.abs(counter.count(methodElementA, withoutTypeCountFilesOutputPercent) - 1.0 / 1) <= 1e-3);

        counter.close();

        // Drop the new table for test.
        connector.dropTable();
        connector.close();
    }
}
