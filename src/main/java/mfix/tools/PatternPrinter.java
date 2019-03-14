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
import mfix.common.util.Utils;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.modify.Modification;
import mfix.core.pattern.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-02-01
 */
public class PatternPrinter {

    private final static String COMMAND = "<command> -if <arg> [-of <arg>]";
    private Options options() {
        Options options = new Options();

        Option option = new Option("if", "PatternFile", true, "Path of pattern file.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("of", "ResultFile", true, "Result file for output.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    public static void print(String patternFile, String outFile) {
        Pattern fixPattern;
        try {
            fixPattern = (Pattern) Utils.deserialize(patternFile);
        } catch (IOException | ClassNotFoundException e) {
            LevelLogger.error("Deserialize pattern failed!", e);
            return;
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("FILE | ").append(fixPattern.getPatternName())
                .append(Constant.NEW_LINE)
                .append("SRC >> ").append(fixPattern.getFileName())
                .append(Constant.NEW_LINE)
                .append("TAR >> ").append(fixPattern.getPatternNode().getBindingNode().getFileName())
                .append(Constant.NEW_LINE)
                .append("-----------------------")
                .append(Constant.NEW_LINE);
        StringBuffer b = new StringBuffer();
        for (String ip : fixPattern.getImports()) {
            b.append(ip).append(Constant.NEW_LINE);
        }
        b.append(fixPattern.getPatternNode().getBindingNode().toSrcString());
        TextDiff diff = new TextDiff(fixPattern.getPatternNode().toSrcString().toString(), b.toString());
        Set<Modification> modifications = fixPattern.getAllModifications();
        stringBuffer.append("DIFF : [").append(diff.getMiniDiff().size()).append("]>>>")
                .append(Constant.NEW_LINE)
                .append(diff.toString())
                .append(Constant.NEW_LINE).append(Constant.NEW_LINE)
                .append("Modification : [")
                .append(modifications.size()).append("]>>>")
                .append(Constant.NEW_LINE);

        if (modifications.size() > 0) {
            for (Modification m : modifications) {
                stringBuffer.append(m.toString()).append(Constant.NEW_LINE);
            }
        }

        Set<String> keys = fixPattern.getKeywords();
        stringBuffer.append(Constant.NEW_LINE).append(Constant.NEW_LINE)
                .append("SRC KEYS : [")
                .append(keys.size()).append("]>>>")
                .append(Constant.NEW_LINE);
        for (String s : keys) {
            stringBuffer.append(s).append(',');
        }

        keys = fixPattern.getTargetKeywords();
        stringBuffer.append(Constant.NEW_LINE).append(Constant.NEW_LINE)
                .append("TAR KEYS : [")
                .append(keys.size()).append("]>>>")
                .append(Constant.NEW_LINE);
        for (String s : keys) {
            stringBuffer.append(s).append(',');
        }

        stringBuffer.append(Constant.NEW_LINE).append(Constant.NEW_LINE)
                .append("FORMAL FORM : ").append(Constant.NEW_LINE)
                .append(fixPattern.formalForm());

        System.out.println(stringBuffer);
        if (outFile != null) {
            JavaFile.writeStringToFile(outFile, stringBuffer.toString());
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
            formatter.printHelp(COMMAND, options);
            System.exit(1);
        }
        String patternFile = cmd.getOptionValue("if");
        String outFile = null;
        if (cmd.hasOption("of")) {
            outFile = cmd.getOptionValue("of");
        }
        print(patternFile, outFile);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : " + patternFile + " > " + simpleDateFormat.format(new Date()));
    }

    public static void main(String[] args) {
        PatternPrinter patternPrinter = new PatternPrinter();
        patternPrinter.print(args);
    }

}
