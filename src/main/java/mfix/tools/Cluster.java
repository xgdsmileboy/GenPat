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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-02-11
 */
public class Cluster {

    private static class Keys{
        private Set<String> _keys;
        public Keys(Set<String> keys) {
            _keys = keys;
        }

        @Override
        public int hashCode() {
            return _keys.size();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Keys) {
                Keys key = (Keys) obj;
                return Utils.safeCollectionEqual(_keys, key._keys);
            }
            return false;
        }
    }

    private Options options() {
        Options options = new Options();

        Option option = new Option("if", "APIFile", true, "The file contains the path information of patterns.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("op", "OutPath", true, "The output path of result");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    private Map<Keys, Set<String>> preClusterByKeys(String file, Map<Keys, Set<String>> map) throws IOException {
        LevelLogger.debug("Start to perform pre pattern clustering : " + file);
        BufferedReader br;
        br = new BufferedReader(new FileReader(new File(file)));
        String line;
        Keys keys;
        Set<String> set = new HashSet<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith("/home/jiajun/GithubData")) {
                LevelLogger.debug(line);
                keys = new Keys(set);
                Set<String> paths = map.get(keys);
                if (paths == null) {
                    paths = new HashSet<>();
                    map.put(keys, paths);
                }
                paths.add(line);
                set = new HashSet<>();
            } else {
                set.add(line);
            }
        }
        return map;
    }

    private Set<Pattern> readPatterns(Set<String> files) {
        LevelLogger.debug("Start to load patterns from files!");
        Set<Pattern> patterns = new HashSet<>();
        Pattern p;
        for (String f : files) {
            try {
                LevelLogger.debug("Deserialize : " + f);
                p = (Pattern) Utils.deserialize(f);
                p.setPatternName(f);
                patterns.add(p);
            } catch (Exception e) {
                LevelLogger.error(e);
                continue;
            }
        }

        LevelLogger.debug("Finish deserialization!!");
        return patterns;
    }

    private void dump2File(Set<Group> groups, String outFile, boolean append) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, append), "UTF-8"));
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

    private void clusterPatterns(String outFile, String... apiMappingFiles) {
        Map<Keys, Set<String>> key2Paths = new HashMap<>();
        try {
            for (String apiMappingFile : apiMappingFiles) {
                key2Paths = preClusterByKeys(apiMappingFile, key2Paths);
            }
        } catch (Exception e) {
            LevelLogger.error("Failed to cluster by keys.", e);
            return;
        }

        Set<Pattern> patterns;
        boolean append = false;
        int loop = key2Paths.size();
        ClusterImpl cluster = new ClusterImpl();
        LevelLogger.info("====================== Total clusters : [ " + loop + " ] ===================");
        for (Map.Entry<Keys, Set<String>> entry : key2Paths.entrySet()) {
            LevelLogger.debug(">>>>>>>>>> LOOP LEFT [ " + (loop --) + " ] <<<<<<<<<<<");
            patterns = readPatterns(entry.getValue());
            Set<Group> result = cluster.reset().cluster(patterns);
            try {
                dump2File(result, outFile, append);
                append = true;
            } catch (IOException e) {
                LevelLogger.error(String.format("Dump result to <%s> failed!", outFile));
            }
        }
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
        String outFile;
        if (cmd.hasOption("op")) {
            outFile = Utils.join(Constant.SEP, cmd.getOptionValue("op"), "clustered.txt");
        } else {
            outFile = Utils.join(Constant.SEP, Constant.HOME, "cluster.txt");
        }
        clusterPatterns(outFile, apiMappingFile.split(","));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : result in " + outFile + " | " + simpleDateFormat.format(new Date()));
    }

    public static void main(String[] args) {
        Cluster cluster = new Cluster();
        cluster.cluster(args);
    }

}
