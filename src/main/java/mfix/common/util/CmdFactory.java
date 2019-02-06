/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import mfix.common.java.Subject;

/**
 * @author: Jiajun
 * @date: 2019-02-05
 */
public class CmdFactory {

    public static String[] createComipleCommand(Subject subject) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd " + subject.getHome());
        stringBuffer.append(" && " + subject.getCompileCommand());
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

}
