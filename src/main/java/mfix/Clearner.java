/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import org.apache.commons.io.FileUtils;

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
public class Clearner {

    public static void delete(Map<Pair<String, Integer>, Set<String>> method2PatternFiles, String mName,
                              int argNumber) {
        Set<String> patternFileList = method2PatternFiles.getOrDefault(new Pair<>(mName,
                argNumber), new HashSet<>());
        for (String patternFile : patternFileList) {
            int ind = patternFile.indexOf("pattern-ver4-serial");
            String filePath = Constant.DATASET_PATH + patternFile.substring(0, ind - 1);

            String patternSerializePath = Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION);
            File file = new File(patternSerializePath);
            if (!file.exists()) {
                continue;
            }
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                LevelLogger.error("Delete directory failed : " + patternSerializePath);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Please provide the method name, arg number and output file path!");
            System.exit(0);
        }
        String mName = args[0];
        int argNumber = Integer.parseInt(args[1]);

        delete(Utils.loadAPI(Constant.API_MAPPING_FILE, Constant.PATTERN_NUMBER, new HashSet<>()), mName, argNumber);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : " + mName + " | " + argNumber + " > " + simpleDateFormat.format(new Date()));
    }

}
