/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.java;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Subject {

    protected String _base;
    protected String _name;
    protected String _ssrc;
    protected String _tsrc;
    protected String _sbin;
    protected String _tbin;

    // for compile
    protected SOURCE_LEVEL _src_level;
    protected List<String> _classpath;

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

    public Subject(String base, String name, String ssrc, String tsrc, String sbin, String tbin, SOURCE_LEVEL sourceLevel, List<String> classpath) {
        _base = base;
        _name = name;
        _ssrc = ssrc;
        _tsrc = tsrc;
        _sbin = sbin;
        _tbin = tbin;
        _src_level = sourceLevel;
        _classpath = classpath;
    }

    public String getHome() {
        return _base;
    }

    public String getName() {
        return _name;
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

    public String getSourceLevelStr() {
        return _src_level.toString();
    }

    public void setClasspath(List<String> classpath) {
        _classpath = classpath;
    }

    public List<String> getClasspath() {
        return _classpath;
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
