/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.cmd;

import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.java.Subject;

/**
 * @author: Jiajun
 * @date: 2019-02-05
 */
public class CmdFactory {

    public static String[] createSbflCmd(D4jSubject subject, int timeout){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd ").append(Constant.LOCATOR_HOME)
                .append(" && ")
                .append(Constant.CMD_TIMEOUT).append(" ")
                .append(timeout)
                .append(" ")
                .append(Constant.COMMAND_LOCATOR)
                .append(subject.getName())
                .append(" ")
                .append(subject.getId())
                .append(" ")
                .append(subject.getHome());
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

    public static String[] createCommand(String dir, String command) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd " + dir);
        stringBuffer.append(" && " + command);
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

    public static String[] createTestCommand(int timeout, Subject subject) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd ").append(subject.getHome())
                .append(" && ").append(Constant.CMD_TIMEOUT)
                .append(" ").append(timeout)
                .append(" ").append(subject.getTestCommand());
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

    public static String[] createTestCommand(Subject subject) {
        return createTestCommand(Constant.TEST_SUBJECT_TIMEOUT, subject);
    }

    public static String[] createComipleCommand(Subject subject) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd " + subject.getHome());
        stringBuffer.append(" && " + subject.getCompileCommand());
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

}
