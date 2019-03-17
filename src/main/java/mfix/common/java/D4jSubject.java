/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.java;

import mfix.common.cmd.CmdFactory;
import mfix.common.cmd.ExecuteCommand;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class D4jSubject extends Subject {

    public final static String NAME = "D4jSubject";
    private int _id;
    private List<String> _failedTestCases;

    public D4jSubject(String base, String name, int id) {
        super(Utils.join(Constant.SEP, base, name, name + "_" + id + "_buggy"), name);
        _type = NAME;
        _id = id;
        setClasspath(obtainClasspath(name));
        setSourceLevel(name.equals("chart") ? SOURCE_LEVEL.L_1_4 : SOURCE_LEVEL.L_1_7);
        setCompileFile(false);
        setCompileProject(true);
        setTestProject(true);
        setCompileCommand(Constant.CMD_DEFECTS4J + " compile");
        setTestCommand(Constant.CMD_DEFECTS4J + " test");
        setPath(name, id);
        setJDKHome(Constant.JAVA_7_HOME);
        setCompileSuccessMessage("BUILD SUCCESSFUL");
        setTestSuccessMessage("Failing tests: 0");
    }

    public int getId() {
        return _id;
    }

    public void configFailedTestCases() {
        String file = Utils.join(Constant.SEP, Constant.D4J_FAILING_TEST, getName(), _id + ".txt");
        _failedTestCases = JavaFile.readFileToStringList(file);
    }

    private void setPath(String projName, int id) {
        String file = Utils.join(Constant.SEP, Constant.D4J_SRC_INFO, projName, id + ".txt");
        List<String> paths = JavaFile.readFileToStringList(file);
        if(paths == null || paths.size() < 4) {
            LevelLogger.error(String.format("D4jSubject#setPath : path info error : <{0}>", file));
            return;
        }
        _ssrc = paths.get(0);
        _sbin = paths.get(1);
        _tsrc = paths.get(2);
        _tbin = paths.get(3);
    }

    @Override
    public String getLogFilePath() {
        return _name + '/' + _id;
    }

    private List<String> obtainClasspath(final String projName) {
        List<String> classpath = new LinkedList<String>();
        switch (projName) {
            case "math":
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                break;
            case "chart":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/iText-2.1.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/servlet.jar");
                break;
            case "lang":
                classpath.add(Constant.D4J_LIB_DIR + "/cglib-nodep-2.2.2.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/commons-io-2.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/easymock-3.1.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/objenesis-1.2.jar");
                break;
            case "closure":
                classpath.add(Constant.D4J_LIB_DIR + "/caja-r4314.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/jarjar.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/ant.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/ant-launcher.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/args4j.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/jsr305.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/guava.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/json.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/protobuf-java.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/rhino.jar");
                break;
            case "time":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/joda-convert-1.2.jar");
                break;
            case "mockito":
                break;
            default:
                System.err.println("UNKNOWN project name : " + projName);
        }
        return classpath;
    }

    @Override
    public boolean test() {
        if (_failedTestCases != null) {
            String home = getHome();
            String jdk = getJDKHome();
            for (String test : _failedTestCases) {
                LevelLogger.debug("TESTING : " + test);
                if (!checkSuccess(ExecuteCommand.execute(
                        CmdFactory.createCommand(home, Constant.CMD_DEFECTS4J + " test -t " + test),
                        jdk), _key_test_suc)) {
                    LevelLogger.debug("FAILED : " + test);
                    return false;
                } else {
                    LevelLogger.debug("PASS : " + test);
                }
            }
        }
        return super.test();
    }

}
