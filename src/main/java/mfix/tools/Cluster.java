/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.cluster.ClusterImpl;
import mfix.core.pattern.cluster.Group;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-02-11
 */
public class Cluster {

    private Options options() {
        Options options = new Options();

        Option option = new Option("if", "APIFile", true, "The file contains the path information of patterns.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("op", "OutPath", true, "The output path of result");
        option.setRequired(true);
        options.addOption(option);

        return options;
    }

    private Set<Pattern> readPatterns(String file) throws IOException {
        LevelLogger.debug("Start to load patterns from file : " + file);
        Set<Pattern> patterns = new HashSet<>();
        BufferedReader br;
        br = new BufferedReader(new FileReader(new File(file)));
        String line;
        Pattern p;
        while((line = br.readLine()) != null) {
            // TODO: here is the hard encode, improve later
            if (line.startsWith("/home/jiajun/GithubData")) {
                try {
                    LevelLogger.debug("Deserialize : " + line);
                    p = (Pattern) Utils.deserialize(line);
                    p.setPatternName(line);
                    patterns.add(p);
                } catch (ClassNotFoundException e) {
                    LevelLogger.error(e);
                    continue;
                } catch (Exception e) {
                    LevelLogger.error(e);
                    continue;
                }
            }
        }
        br.close();
        LevelLogger.debug("Finish deserialization!!");
        return patterns;
    }

    private void dump2File(Set<Group> groups, String outFile) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false), "UTF-8"));
        LevelLogger.debug("Start dumping result to file : " + outFile);
        for (Group g : groups) {
            for (String s : g.getRepresentPattern().getKeywords()) {
                bw.write(s);
                bw.newLine();
            }
            List<String> paths = g.getIsomorphicPatternPath();
            bw.write(g.getRepresentPatternFile() + '#' + paths.size());
            bw.newLine();
            for (String p : paths) {
                bw.write(p);
                bw.newLine();
            }
        }
        bw.close();
        LevelLogger.debug("Finish dumping result to file : " + outFile);
    }

    public void cluster(String[] args){
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp("<command> -if <arg> -op <arg>", options);
            System.exit(1);
        }
        String apiMappingFile = cmd.getOptionValue("if");
        String outFile = Utils.join(Constant.SEP, cmd.getOptionValue("op"), "clustered.txt");
        Set<Pattern> patterns = null;
        try {
            patterns = readPatterns(apiMappingFile);
        } catch (IOException e) {
            LevelLogger.error("Failed to read patterns to cluster.", e);
            return;
        }
        ClusterImpl cluster = new ClusterImpl();
        Set<Group> result = cluster.cluster(patterns);
        try {
            dump2File(result, outFile);
        } catch (IOException e) {
            LevelLogger.error(String.format("Dump result to <%s> failed!", outFile));
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : result in " + outFile + " | " + simpleDateFormat.format(new Date()));
    }

    public static void main(String[] args) {
        Cluster cluster = new Cluster();
        cluster.cluster(args);
    }

}
