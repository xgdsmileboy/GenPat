/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-02-01
 */
public class Cleaner {

    private Options options() {
        Options options = new Options();

        Option option = new Option("f", "RecordFile", true,
                "The file that contains the paths of patterns to delete.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("dir", "BaseDir", true,
                "The home directory for pattern files.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }


    private Set<String> getAllPatternPaths(String fileName, String baseDir) throws IOException {
        LevelLogger.debug("Read pattern file paths form : " + fileName);
        BufferedReader br;
        br = new BufferedReader(new FileReader(new File(fileName)));
        String line;
        Set<String> set = new HashSet<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith(baseDir)) {
                LevelLogger.debug(line);
                set.add(line);
            }
        }
        return set;
    }

    private void delete(Set<String> paths) {
        for (String patternFile : paths) {
            File parent = new File(patternFile).getParentFile();
            Utils.deleteDirs(parent);
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
            formatter.printHelp("<command> -f <arg> [-dir <arg>]", options);
            System.exit(1);
        }
        String fileName = cmd.getOptionValue("f");
        String baseDir = Constant.DEFAULT_PATTERN_HOME;
        if (cmd.hasOption("dir")) {
            baseDir = cmd.getOptionValue("dir");
        }
        try {
            delete(getAllPatternPaths(fileName, baseDir));
        } catch (IOException e) {
            LevelLogger.error("Delete patterns failed!", e);
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : Delete pattern files in < " + fileName + " > " + simpleDateFormat.format(new Date()));
    }


    public static void main(String[] args) {
        Cleaner cleaner = new Cleaner();
        cleaner.clean(args);
    }

}
