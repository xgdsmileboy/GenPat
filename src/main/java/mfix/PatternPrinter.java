/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.node.modify.Modification;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-02-01
 */
public class PatternPrinter {

    public static void print(Map<Pair<String, Integer>, Set<String>> method2PatternFiles, String mName, int argNumber
            , String outFile) {
        Set<String> patternFileList = method2PatternFiles.getOrDefault(new Pair<>(mName,
                argNumber), new HashSet<>());
        outFile = outFile + "/" + mName + "_" + argNumber + ".txt";
        StringBuffer stringBuffer = new StringBuffer(">>>>>>> " + mName + " | " + argNumber + "<<<<<<<<");
        stringBuffer.append("-------------------");
        for (String patternFile : patternFileList) {
            int ind = patternFile.indexOf("pattern-ver4-serial");
            int indLen = "pattern-ver4-serial".length();
            String filePath = Constant.DATASET_PATH +  patternFile.substring(0, ind - 1);
            String fileAndMethod = patternFile.substring(ind + indLen + 1);

            String patternSerializePath = Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION,
                    fileAndMethod);
            File file = new File(patternSerializePath);
            if (!file.exists()) {
                stringBuffer = new StringBuffer();
                continue;
            }
            Node fixPattern;
            try {
                fixPattern = (Node) Utils.deserialize(patternSerializePath);
            } catch (IOException | ClassNotFoundException e) {
                LevelLogger.error("Deserialize pattern failed!", e);
                stringBuffer = new StringBuffer();
                continue;
            }

            Set<Modification> modifications = fixPattern.getAllModifications(new HashSet<>());
            if (modifications.size() > 0) {
                stringBuffer.append("FILE: " + patternSerializePath + "\n");
                stringBuffer.append(">>>>>>");
                for (Modification m : modifications) {
                    stringBuffer.append(m.toString());
                }
                stringBuffer.append("-------------------");
                JavaFile.writeStringToFile(outFile, stringBuffer.toString(), true);
            }
            stringBuffer = new StringBuffer();
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Please provide the method name, arg number and output file path!");
            System.exit(0);
        }
        String mName = args[0];
        int argNumber = Integer.parseInt(args[1]);
        String outPath = args[2];

        print(Utils.loadAPI(Constant.API_MAPPING_FILE, Constant.PATTERN_NUMBER, new HashSet<>()), mName, argNumber,
                outPath);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : " + mName + " | " + argNumber + " > " + simpleDateFormat.format(new Date()));
    }

}
