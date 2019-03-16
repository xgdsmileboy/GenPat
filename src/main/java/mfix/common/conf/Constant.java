/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.conf;

import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Constant {

    /*  basic path configurations */
    public final static String HOME = System.getProperty("user.dir");
    public final static char SEP = File.separatorChar;
    public final static String RES_DIR = HOME + SEP + "resources";
    public final static String NEW_LINE = "\n";

    /* display tags for source code diff */
    public final static String PATCH_KEEP_LEADING = " ";
    public final static String PATCH_DEL_LEADING = "-";
    public final static String PATCH_ADD_LEADING = "+";

    /* pattern version tags */
    public static String PATTERN_VERSION;

    /* files used for pattern abstraction */
    public final static String TF_IDF_TOKENS = Utils.join(SEP, RES_DIR, "db", "AllTokens.txt");
    public final static String DB_CACHE_FILE = Utils.join(SEP, RES_DIR, "db", "MethodTableElements.txt");
    public final static String DB_CACHE_FILE_WITH_TYPE = Utils.join(SEP, RES_DIR, "db", "MethodTableElementsWithType.txt");
    public final static String BANNED_API_FILE = Utils.join(SEP, RES_DIR, "conf", "whiteList.txt");

    /* values for pattern abstraction */
    public final static int TOTAL_BUGGY_FILE_NUMBER = 1217392; // used to compute TF-IDF
    public final static int API_FREQUENCY = 100;
    public final static double TF_IDF_FREQUENCY = 0.5;
    public final static double TOKEN_FREQENCY = 0.01;

    /* runtime configurations */
    public final static String DEFAULT_SUBJECT_XML = Utils.join(SEP, RES_DIR, "conf", "project.xml");
    public final static String RUNTIME_CONF_FILE = Utils.join(SEP, RES_DIR, "conf", "configure.properties");

    /* max number of concurrent threads for pattern cluster process */
    public static int MAX_CLUSTER_THREAD_NUM;

    /* max number of concurrent threads for pattern filter process */
    public static int MAX_FILTER_THREAD_NUM;

    /* max number of concurrent threads for keyword statistics */
    public static int MAX_KEYWORD_STATISTIC_THREAD_NUM;

    /* max number of candidate patches for each subject */
    public static int MAX_PATCH_NUMBER;

    /* max number of repair time for single subject (in minutes) */
    public static int MAX_REPAIR_TIME;

    /* max number of locations to repair */
    public static int MAX_REPAIR_LOCATION;

    /* max number of patterns used to repair one location */
    public static int TOP_K_PATTERN_EACH_LOCATION;

    /* max number of changed lines for pattern filtering */
    public static int FILTER_MAX_CHANGE_LINE;

    /* max number of modifications for pattern filtering */
    public static int FILTER_MAX_CHANGE_ACTION;

    /* default directory to same patch log info */
    public final static String PATCH_PATH = Utils.join(SEP, HOME, "patch");

    /* default home directory of pattern files */
    public final static String DEFAULT_PATTERN_HOME = "/home/jiajun/GithubData";

    /*
     * markers
     */
    public final static boolean INGORE_OPERATOR_FOR_DELETE_MATCH = false;


    /*
     * Defects4j configure information
     */
    private final static String D4J_INFO_DIR = Utils.join(SEP, RES_DIR, "d4j-info");
    public final static String D4J_LIB_DIR = Utils.join(SEP, D4J_INFO_DIR, "d4jlibs");
    public final static String D4J_FAULT_LOC = Utils.join(SEP, D4J_INFO_DIR, "location", "groundtruth");
    public final static String D4J_SRC_INFO = Utils.join(SEP, D4J_INFO_DIR, "src_path");

    static {
        Properties prop = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(RUNTIME_CONF_FILE));
            prop.load(in);

            // System commands
            Constant.PATTERN_VERSION = "ver" + prop.getProperty("PATTERN.VERSION", "1");
            String number = prop.getProperty("PATTERN.NUMBER", "All");
            Constant.TOP_K_PATTERN_EACH_LOCATION = "All".equals(number) ? Integer.MAX_VALUE : Integer.parseInt(number);
            Constant.MAX_REPAIR_TIME = Integer.parseInt(prop.getProperty("REPAIR.TIME", "120"));
            Constant.MAX_PATCH_NUMBER = Integer.parseInt(prop.getProperty("REPAIR.PATCH", "100"));
            Constant.MAX_REPAIR_LOCATION = Integer.parseInt(prop.getProperty("REPAIR.LOCATION", "100"));
            Constant.MAX_FILTER_THREAD_NUM = Integer.parseInt(prop.getProperty("FILTER.THREAD", "10"));
            Constant.MAX_CLUSTER_THREAD_NUM = Integer.parseInt(prop.getProperty("CLUSTER.THREAD", "10"));
            Constant.MAX_KEYWORD_STATISTIC_THREAD_NUM = Integer.parseInt(prop.getProperty("STATISTIC.THREAD", "10"));
            Constant.FILTER_MAX_CHANGE_LINE = Integer.parseInt(prop.getProperty("FILTER.LINE", "10"));
            Constant.FILTER_MAX_CHANGE_ACTION = Integer.parseInt(prop.getProperty("FILTER.ACTION", "5"));
        } catch (IOException e) {
            LevelLogger.error("#Constant get properties failed!" + e.getMessage());
        }
    }

}
