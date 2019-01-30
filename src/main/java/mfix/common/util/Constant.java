/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

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

    public final static String HOME = System.getProperty("user.dir");
    public final static char SEP = File.separatorChar;
    public final static String RES_DIR = HOME + SEP + "resources";

    public final static String NEW_LINE = "\n";
    public final static String PATCH_KEEP_LEADING = " ";
    public final static String PATCH_DEL_LEADING = "-";
    public final static String PATCH_ADD_LEADING = "+";

    public final static String DB_CACHE_FILE = Utils.join(SEP, RES_DIR, "db", "MethodTableElements.txt");

    public static String API_MAPPING_FILE;
    public static String PATTERN_VERSION;
    public static int PATTERN_NUMBER;
    public static String RESULT_PATH;

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
            String filePath = Utils.join(SEP, RES_DIR, "conf", "configure.properties");
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            prop.load(in);

            // System commands
            Constant.API_MAPPING_FILE = prop.getProperty("PATH.API_MAPPING_FILE");
            Constant.PATTERN_VERSION = "ver" + prop.getProperty("PATTERN.VERSION", "0");
            String number = prop.getProperty("PATTERN.NUMBER", "All");
            Constant.PATTERN_NUMBER = "All".equals(number) ? Integer.MAX_VALUE : Integer.parseInt(number);
            Constant.RESULT_PATH = prop.getProperty("PATH.RESULT", HOME);

        } catch (IOException e) {
            LevelLogger.error("#Constant get properties failed!" + e.getMessage());
        }
    }

}
