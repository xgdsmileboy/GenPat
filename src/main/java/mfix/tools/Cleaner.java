/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
public class Cleaner {

    private Options options() {
        Options options = new Options();

        Option option = new Option("m", "APIName", true, "the name of API to focus on.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("n", "ArgNumber", true, "number of argument number.");
        option.setRequired(true);
        options.addOption(option);

        return options;
    }

    private static void delete(Map<Pair<String, Integer>, Set<String>> method2PatternFiles, String mName,
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

    public void clean(String[] args) {
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
        String mName = cmd.getOptionValue("m");
        int argNumber = Integer.parseInt(cmd.getOptionValue("n"));
        delete(Utils.loadAPI(Constant.API_MAPPING_FILE, Constant.PATTERN_NUMBER, new HashSet<>()), mName, argNumber);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : " + mName + " | " + argNumber + " > " + simpleDateFormat.format(new Date()));
    }


    public static void main(String[] args) {
        Cleaner cleaner = new Cleaner();
        cleaner.clean(args);
    }

}
