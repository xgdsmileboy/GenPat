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
import java.util.LinkedList;
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

        Option option = new Option("if", "PatternFile", true,
                "The file contains the path information of patterns.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("dir", "BaseDir", true,
                "The home directory of pattern files.");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("op", "ResultPath", true,
                "The output path of result");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    private Map<Keys, Set<String>> preClusterByKeys(String baseDir, String file, Map<Keys, Set<String>> map) throws IOException {
        LevelLogger.debug("Start to perform pre pattern clustering : " + file);
        BufferedReader br;
        br = new BufferedReader(new FileReader(new File(file)));
        String line;
        Keys keys;
        Set<String> set = new HashSet<>();
        while ((line = br.readLine()) != null) {
            if (line.startsWith(baseDir)) {
                int index = line.indexOf(">");
                if (index > 0) {
                    line = line.substring(0, index);
                }
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
        int size = files.size();
        for (String f : files) {
            try {
                LevelLogger.debug("< " + (size --) + "> Deserialize : " + f);
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
        LevelLogger.info("Start dumping result to file : " + outFile);
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
        LevelLogger.info("Finish dumping result to file : " + outFile);
    }

    private List<Set<String>> split(Set<String> strings) {
        List<Set<String>> list = new LinkedList<>();
        Set<String> set = new HashSet<>();
        for (String s : strings) {
            set.add(s);
            if (set.size() >= Constant.MAX_PATTERN_NUM_EACH_CLUSTER) {
                list.add(set);
                set = new HashSet<>();
            }
        }
        if (!set.isEmpty()) {
            list.add(set);
        }
        LevelLogger.info("Split to < " + list.size() + " > batch.");
        return list;
    }

    private void clusterPatterns(String baseDir, String outFile, String... recordFiles) {
        Map<Keys, Set<String>> key2Paths = new HashMap<>();
        try {
            for (String record : recordFiles) {
                key2Paths = preClusterByKeys(baseDir, record, key2Paths);
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
        List<Set<String>> list;
        for (Map.Entry<Keys, Set<String>> entry : key2Paths.entrySet()) {
            LevelLogger.info(">>>>>>>>>> LOOP LEFT [ " + (loop --) + " ] <<<<<<<<<<<");
            if (Constant.SPLIT_CLUSTER) {
                list = split(entry.getValue());
            } else {
                list = new LinkedList<>();
                list.add(entry.getValue());
            }
            for (Set<String> set : list) {
                patterns = readPatterns(set);
                Set<Group> result = cluster.reset().cluster(patterns);
                try {
                    dump2File(result, outFile, append);
                    append = true;
                } catch (IOException e) {
                    LevelLogger.error(String.format("Dump result to <%s> failed!", outFile));
                }
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
            formatter.printHelp("<command> -if <arg> [-dir <arg>] [-op <arg>]", options);
            System.exit(1);
        }
        String recordFiles = cmd.getOptionValue("if");
        String outFile;
        if (cmd.hasOption("op")) {
            outFile = Utils.join(Constant.SEP, cmd.getOptionValue("op"), "clustered.txt");
        } else {
            outFile = Utils.join(Constant.SEP, Constant.HOME, "clustered.txt");
        }
        String baseDir = Constant.DEFAULT_PATTERN_HOME;
        if (cmd.hasOption("dir")) {
            baseDir = cmd.getOptionValue("dir");
        }
        clusterPatterns(baseDir, outFile, recordFiles.split(","));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        System.out.println("Finish : result in " + outFile + " | " + simpleDateFormat.format(new Date()));
    }

    public static void main(String[] args) {
        Cluster cluster = new Cluster();
        cluster.cluster(args);
    }

}
