/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import mfix.common.util.Utils;

import java.io.File;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Constant {

    public final static String THIS_HOME = System.getProperty("user.dir");
    public final static char SEP = File.separatorChar;
    public final static String RES_DIR = THIS_HOME + SEP + "resources";
    public final static String NEW_LINE = "\n";
    public final static String PLACE_HOLDER = "$p$";

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

}
