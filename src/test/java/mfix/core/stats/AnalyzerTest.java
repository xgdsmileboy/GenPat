package mfix.core.stats;

import mfix.common.util.Constant;
import mfix.core.TestCase;
import mfix.core.stats.element.*;

import org.junit.Assert;
import org.junit.Test;

public class AnalyzerTest extends TestCase {
    @Test
    public void test_parse() {
        String srcFile = testbase + Constant.SEP + "src_Project.java";

        // Create a new table for test.
        DatabaseConnector connector = new DatabaseConnector();
        connector.setAsTestMode();
        connector.open();
        connector.dropTable(); // Drop the table if exist.
        connector.createTable();

        Analyzer analyzer = Analyzer.getInstance();
        analyzer.open();
        analyzer.runFile(srcFile);
        analyzer.finish();

        ElementCounter counter = new ElementCounter();
        counter.open();

        ElementQueryType withTypeInAllFiles = new ElementQueryType(true, ElementQueryType.CountType.ALL);
        ElementQueryType withoutTypeInAllFiles = new ElementQueryType(false, ElementQueryType.CountType.ALL);
        ElementQueryType withoutTypeCountFiles = new ElementQueryType(false, ElementQueryType.CountType.COUNT_FILES);

        // Variable haven't support now.
        /*
        Element VarElementA = new VarElement("path", "org.apache.tools.ant.Path", srcFile);
        Element VarElementB = new VarElement("path", "java.lang.StringBuffer", srcFile);
        Element VarElementC = new VarElement("noThisVar", "java.lang.StringBuffer", srcFile);

        Assert.assertTrue(counter.count(VarElementA, withTypeInAllFiles) == 1);
        Assert.assertTrue(counter.count(VarElementA, withoutTypeInAllFiles) == 6);
        Assert.assertTrue(counter.count(VarElementA, withoutTypeCountFiles) == 1);

        Assert.assertTrue(counter.count(VarElementB, withTypeInAllFiles) == 5);

        Assert.assertTrue(counter.count(VarElementC, withTypeInAllFiles) == 0);
        Assert.assertTrue(counter.count(VarElementC, withoutTypeInAllFiles) == 0);
        Assert.assertTrue(counter.count(VarElementC, withoutTypeCountFiles) == 0);
        */

        MethodElement methodElementD = new MethodElement("close", srcFile);
        Assert.assertTrue(counter.count(methodElementD, withoutTypeInAllFiles) == 2);

        MethodElement methodElementE = new MethodElement("setProperty", srcFile);
        methodElementE.setObjType("PropertyHelper");
        methodElementE.setArgsNumber(4);
        Assert.assertTrue(counter.count(methodElementE, withoutTypeInAllFiles) == 2);
        Assert.assertTrue(counter.count(methodElementE, withTypeInAllFiles) == 1);

        methodElementE.setArgsNumber(3);
        Assert.assertTrue(counter.count(methodElementE, withTypeInAllFiles) == 0);
        
        counter.close();

        // Drop the new table for test.
        connector.dropTable();
        connector.close();
    }
}
