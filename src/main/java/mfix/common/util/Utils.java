/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import mfix.core.parse.diff.Diff;

import java.util.Arrays;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 9/21/18
 */
public class Utils {
    public static String join(char delimiter, String... element) {
        return join(delimiter, Arrays.asList(element));
    }

    public static String join(char delimiter, List<String> elements) {
        StringBuffer buffer = new StringBuffer();
        if (elements.size() > 0) {
            buffer.append(elements.get(0));
        }
        for (int i = 1; i < elements.size(); i++) {
            buffer.append(delimiter);
            buffer.append(elements.get(i));
        }
        return buffer.toString();
    }

    public static void log(String logFile, String path, int startLine, int endLine, String content,
                           boolean append) {
        log(logFile, path, startLine, endLine, 0, 0, content, null, append);
    }

    public static <T> void log(String logFile, String path, int startLine, int endLine, double normSim,
                           double cosineSim, String content, Diff<T> diff,
                           boolean append) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("path : " + path + Constant.NEW_LINE);
        buffer.append("range : <" + startLine + "," + endLine + ">" + Constant.NEW_LINE);
        buffer.append("NORM : " + normSim + Constant.NEW_LINE);
        buffer.append("COSIN : " + cosineSim + Constant.NEW_LINE);
        buffer.append("-----------------------------" + Constant.NEW_LINE);
        buffer.append(content);
        if(diff != null) {
            buffer.append("-----------------------------" + Constant.NEW_LINE);
            buffer.append(diff.miniDiff() + Constant.NEW_LINE);
        }
        JavaFile.writeStringToFile(logFile, buffer.toString(), append);
    }
}
