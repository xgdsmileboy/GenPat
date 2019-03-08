/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.modify.Modification;
import mfix.core.pattern.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

    private Options options() {
        Options options = new Options();

        Option option = new Option("m", "APIName", true, "the name of API to focus on.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("n", "argNumber", true, "number of argument number.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("o", "outpath", true, "output path.");
        option.setRequired(true);
        options.addOption(option);

        return options;
    }

    public static void print(Map<Pair<String, Integer>, Set<String>> method2PatternFiles, String mName, int argNumber
            , String outFile) {
        Set<String> patternFileList = method2PatternFiles.getOrDefault(new Pair<>(mName,
                argNumber), new HashSet<>());
        outFile = outFile + "/" + mName + "_" + argNumber + ".txt";
        StringBuffer stringBuffer = new StringBuffer(">>>>>>> " + mName + " | " + argNumber + "<<<<<<<<");
        stringBuffer.append("\n-------------------\n");
        String versionStr = "pattern-" + Constant.PATTERN_VERSION + "-serial";
        int indLen = versionStr.length();
        for (String patternFile : patternFileList) {
            int ind = patternFile.indexOf(versionStr);
            String filePath = Constant.DATASET_PATH +  patternFile.substring(0, ind - 1);
            String fileAndMethod = patternFile.substring(ind + indLen + 1);

            String patternSerializePath = Utils.join(Constant.SEP, filePath, Constant.PATTERN_VERSION,
                    fileAndMethod);
            File file = new File(patternSerializePath);
            if (!file.exists()) {
                stringBuffer = new StringBuffer();
                continue;
            }
            Pattern fixPattern;
            try {
                fixPattern = (Pattern) Utils.deserialize(patternSerializePath);
            } catch (IOException | ClassNotFoundException e) {
                LevelLogger.error("Deserialize pattern failed!", e);
                stringBuffer = new StringBuffer();
                continue;
            }

            Set<Modification> modifications = fixPattern.getAllModifications();
            if (modifications.size() > 0) {
                stringBuffer.append("FILE: " + patternSerializePath + "\n");
                stringBuffer.append("\n>>>>>>\n");
                for (Modification m : modifications) {
                    stringBuffer.append(m.toString() + "\n");
                }
                stringBuffer.append("-------------------\n");
                JavaFile.writeStringToFile(outFile, stringBuffer.toString(), true);
            }
            stringBuffer = new StringBuffer();
        }
    }

    public void print(String[] args) {
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp("<command> -m <arg> -n <arg> -o <arg>", options);
            System.exit(1);
        }
        String mName = cmd.getOptionValue("m");
        int argNumber = Integer.parseInt(cmd.getOptionValue("n"));
        String outPath = cmd.getOptionValue("o");
        print(Utils.loadAPI(Constant.API_MAPPING_FILE, Constant.PATTERN_NUMBER, new HashSet<>()), mName, argNumber,
                outPath);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : " + mName + " | " + argNumber + " > " + simpleDateFormat.format(new Date()));
    }

    public static void main(String[] args) {
        PatternPrinter patternPrinter = new PatternPrinter();
        patternPrinter.print(args);
    }

}
