/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.java;

import mfix.common.cmd.ExecuteCommand;
import mfix.common.conf.Configure;
import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Subject implements IExecute {

    protected String _type;
    protected String _base;
    protected String _name;
    protected String _ssrc;
    protected String _tsrc;
    protected String _sbin;
    protected String _tbin;
    protected int _id = 1;

    protected boolean _compile_file = false;
    protected boolean _compileProject = false;
    protected boolean _test_subject = false;
    // for compile
    protected SOURCE_LEVEL _src_level;
    protected List<String> _classpath;
    // for compile the complete subject
    protected String _compile_command;
    protected String _test_command;
    protected String _key_compile_suc;
    protected String _key_test_suc;
    protected String _jdk_home;

    protected Subject(String base, String name) {
        this(base, name, null, null, null, null);
    }
    /**
     * subject
     *
     * @param base : the base directory of subject, e.g., "/home/ubuntu/code/chart"
     * @param name : name of subject, e.g., "chart".
     * @param ssrc : relative path for source folder, e.g., "/source"
     * @param tsrc : relative path for test folder, e.g., "/tests"
     * @param sbin : relative path for source byte code, e.g., "/classes"
     * @param tbin : relative path for test byte code, e.g., "/test-classes"
     */
    public Subject(String base, String name, String ssrc, String tsrc, String sbin, String tbin) {
        this(base, name, ssrc, tsrc, sbin, tbin, SOURCE_LEVEL.L_1_6, new LinkedList<>());
    }

    private Subject(String base, String name, String ssrc, String tsrc, String sbin, String tbin,
                  SOURCE_LEVEL sourceLevel, List<String> classpath) {
        _base = base;
        _name = name;
        _ssrc = ssrc;
        _tsrc = tsrc;
        _sbin = sbin;
        _tbin = tbin;
        _src_level = sourceLevel;
        _classpath = classpath;
    }

    public String getType() {
        return _type;
    }

    public String getHome() {
        return _base;
    }

    public String getName() {
        return _name;
    }

    public int getId() {
        return _id;
    }

    public String getSsrc() {
        return _ssrc;
    }

    public String getTsrc() {
        return _tsrc;
    }

    public String getSbin() {
        return _sbin;
    }

    public String getTbin() {
        return _tbin;
    }

    public void setCompileFile(boolean compileFile) {
        _compile_file = compileFile;
    }

    public boolean compileFile() {
        return _compile_file;
    }

    public void setCompileProject(boolean compileProject) {
        _compileProject = compileProject;
    }

    public boolean compileProject() {
        return _compileProject;
    }

    public void setTestProject(boolean testProject) {
        _test_subject = testProject;
    }

    public boolean testProject() {
        return _test_subject;
    }

    public void setSourceLevel(String sourceLeve) {
        setSourceLevel(SOURCE_LEVEL.toSourceLevel(sourceLeve));
    }

    public void setSourceLevel(SOURCE_LEVEL sourceLevel) {
        _src_level = sourceLevel;
    }

    public String getSourceLevelStr() {
        return _src_level.toString();
    }

    public void setClasspath(List<String> classpath) {
        _classpath = classpath;
    }

    public List<String> getClasspath() {
        return _classpath;
    }

    public void setCompileCommand(String command) {
        _compile_command = command;
    }

    public String getCompileCommand() {
        return _compile_command;
    }

    public void setTestCommand(String command) {
        _test_command = command;
    }

    public String getTestCommand() {
        return _test_command;
    }

    public void setCompileSuccessMessage(String string) {
        _key_compile_suc = string;
    }

    public void setTestSuccessMessage(String string) {
        _key_test_suc = string;
    }

    public void setJDKHome(String jdkHome) {
        _jdk_home = jdkHome;
    }

    public  String getJDKHome() {
        return _jdk_home;
    }

    public String getPatchFile() {
        return Utils.join(Constant.SEP, Constant.PATCH_PATH, _name, _id + ".txt");
    }

    public String getLogFile() {
        return Utils.join(Constant.SEP, Constant.REPAIR_LOG_PATH, _name, _id + ".txt");
    }

    public void backup() throws IOException {
        String srcDir = getHome() + getSsrc();
        backup(srcDir, srcDir + "_bak");
        String testDir = getHome() + getTsrc();
        backup(testDir, testDir + "_bak");
    }

    private void backup(String file, String tar) throws IOException {
        File tarFile = new File(tar);
        if (tarFile.exists()) {
            FileUtils.copyDirectory(tarFile, new File(file));
        } else {
            FileUtils.copyDirectory(new File(file), tarFile);
        }
    }

    public void restore() throws IOException {
        String srcDir = getHome() + getSsrc();
        restore(srcDir, srcDir + "_bak");
        String testDir = getHome() + getTsrc();
        restore(testDir, testDir + "_bak");
    }

    public void restore(String file) throws IOException {
        restore(file, file + "_bak");
    }

    private void restore(String file, String tar) throws IOException {
        File tarFile = new File(tar);
        if (tarFile.exists()) {
            FileUtils.copyDirectory(tarFile, new File(file));
        } else {
            LevelLogger.error("Restore source file failed!");
        }
    }

    public void backupPurifiedTest() throws IOException {
        String testDir = getHome() + getTsrc();
        FileUtils.copyDirectory(new File(testDir), new File(testDir + "_purify"));
    }

    public void restorePurifiedTest() throws IOException {
        String testDir = getHome() + getTsrc();
        FileUtils.copyDirectory(new File(testDir + "_purify"), new File(testDir));
    }

    protected boolean checkSuccess(List<String> compileMessage, String key) {
        for (String string : compileMessage) {
            if (string.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkAndInitBuildDir() {
        File file = new File(getHome() + getSbin());
        if (!file.exists()) {
            file.mkdirs();
        }
        file = new File(getHome() + getTbin());
        if (!file.exists()) {
            file.mkdirs();
        }
        return true;
    }

    public boolean purify() {
        return Configure.shouldPurify(this);
    }

    @Override
    public boolean compile() {
        return checkSuccess(ExecuteCommand.executeCompiling(this), _key_compile_suc);
    }

    @Override
    public boolean test() {
        return checkSuccess(ExecuteCommand.executeTest(this), _key_test_suc);
    }

    @Override
    public boolean test(String testcase) {
        return true;
    }

    @Override
    public boolean test(String clazz, String method) {
        return true;
    }

    @Override
    public String toString() {
        return "[_name=" + _name + ", _ssrc=" + _ssrc + ", _tsrc=" + _tsrc + ", _sbin=" + _sbin
                + ", _tbin=" + _tbin + "]";
    }

    public enum SOURCE_LEVEL {
        L_1_4("1.4"),
        L_1_5("1.5"),
        L_1_6("1.6"),
        L_1_7("1.7"),
        L_1_8("1.8");

        private String value;

        public static SOURCE_LEVEL toSourceLevel(String string) {
            if (string == null) return L_1_7;
            switch (string) {
                case "1.4": return L_1_4;
                case "1.5": return L_1_5;
                case "1.6": return L_1_5;
                case "1.7": return L_1_5;
                case "1.8": return L_1_8;
            }
            return L_1_7;
        }

        SOURCE_LEVEL(String val) {
            value = val;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
