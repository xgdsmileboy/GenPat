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
import mfix.common.util.MiningUtils;
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.pattern.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author: Jiajun
 * @date: 2019-03-14
 */
public class TokenStatistic {

    private volatile Map<String, Integer> _cacheMap;

    private int _currThreadCount = 0;
    private int _maxThreadCount = Constant.MAX_FILTER_THREAD_NUM;
    private ExecutorService _threadPool;
    private List<Future<Set<String>>> _threadResultList = new ArrayList<>();

    private final static String COMMAND = "<command> -if <arg> [-of <arg>] [-dir <arg>]";
    private Options options() {
        Options options = new Options();

        Option option = new Option("if", "InputFile", true,
                "The file contains all the pattern paths.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("of", "outputFile", true,
                "The file for output.");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("dir", "BaseDir", true,
                "The base directory of pattern files.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    public TokenStatistic() {
        _cacheMap = new HashMap<>();
    }

    private void init(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileUtils.moveFile(file, new File(fileName + ".bak"));
            } catch (IOException e) {
                LevelLogger.error("Backup previous out file failed!" + fileName);
            }
        }
        JavaFile.writeStringToFile(fileName, "");
    }

    private Map<String, Set<String>> readPatternRecordsFromFile(String baseDir, String file) throws IOException {
        Map<String, Set<String>> patterns = new HashMap<>();
        BufferedReader br;
        br = new BufferedReader(new FileReader(new File(file)));
        String line;
        Set<String> set;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(baseDir)) {
                String fileName = line.substring(0, line.lastIndexOf('-'))
                        .replace(MiningUtils.patternSubDirName(), MiningUtils.buggyFileSubDirName());
                set = patterns.get(fileName);
                if (set == null) {
                    set = new HashSet<>();
                    patterns.put(fileName, set);
                }
                set.add(line);
            }
        }
        br.close();
        return patterns;
    }

    private void statistic(String buggyFileName, Set<String> patternFiles) {
        LevelLogger.info("Statistic : " + buggyFileName);
        try {
            if (_currThreadCount >= _maxThreadCount) {
                LevelLogger.debug("Thread pool is full ....");
                for (Future<Set<String>> fs : _threadResultList) {
                    Set<String> result = fs.get();
                    if (result != null) {
                        Integer count;
                        for (String s : result) {
                            count = _cacheMap.get(s);
                            count = count == null ? 1 : count + 1;
                            _cacheMap.put(s, count);
                        }
                    }
                    _currThreadCount--;
                }
                _threadResultList.clear();
                _currThreadCount = _threadResultList.size();
                LevelLogger.debug("Cleared thread pool : " + _currThreadCount);
            }
            Future<Set<String>> future = _threadPool.submit(new ParseKey(patternFiles));
            _threadResultList.add(future);
            _currThreadCount++;

        } catch (Exception e) {
            LevelLogger.error("Do keyword statistic failed : ", e);
        }
    }

    private void statisticWithExistingRecord(String baseDir, String file) {
        Map<String, Set<String>> bfile2Patterns;
        try {
            bfile2Patterns = readPatternRecordsFromFile(baseDir, file);
        } catch (IOException e) {
            LevelLogger.error("Failed to read existing pattern paths", e);
            return;
        }

        for (Map.Entry<String, Set<String>> entry : bfile2Patterns.entrySet()) {
            statistic(entry.getKey(), entry.getValue());
        }
    }

    private void writeFile(String outFile) throws IOException {
        LevelLogger.info("........FLUSHING.......");
        File file = new File(outFile);
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false), "UTF-8"));
        for (Map.Entry<String, Integer> entry : _cacheMap.entrySet()) {
            bw.write(entry.getKey());
            bw.newLine();
            bw.write(entry.getKey());
            bw.newLine();
        }
        bw.close();
    }

    private Map<String, String> parseOption(String[] args) {
        Map<String, String> optionMap = new HashMap<>();
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

        optionMap.put("if", cmd.getOptionValue("if"));

        String baseDir = Constant.DEFAULT_PATTERN_HOME;
        if (cmd.hasOption("dir")) {
            baseDir = cmd.getOptionValue("dir");
        }
        optionMap.put("dir", baseDir);

        String outFile;
        if (cmd.hasOption("of")) {
            outFile = cmd.getOptionValue("of");
        } else {
            outFile = Utils.join(Constant.SEP, Constant.HOME, "PatternRecord.txt");
        }
        optionMap.put("of", outFile);

        return optionMap;
    }

    public void statistic(String[] args) {
        Map<String, String> optionMap = parseOption(args);
        String outFile = optionMap.get("of");
        init(outFile);

        _threadPool = Executors.newFixedThreadPool(_maxThreadCount);

        statisticWithExistingRecord(optionMap.get("dir"), optionMap.get("if"));

        if (!_cacheMap.isEmpty()) {
            try {
                writeFile(outFile);
            } catch (IOException e) {
                LevelLogger.error("Dump to result to file failed!", e);
            }
        } else {
            LevelLogger.error("No keywords found!");
        }
        _threadPool.shutdown();

        System.out.println("Finish keyword statistics !");
    }

    public static void main(String[] args) {
        Filter filter = new Filter();
        filter.filter(args);
    }

}

class ParseKey implements Callable<Set<String>> {


    private static final Set<String> EmptySet = new HashSet<>(0);
    private Set<String> _patternFiles;

    public ParseKey(Set<String> patterns) {
        _patternFiles = patterns;
    }

    private Serializable deserialize(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(fileName);
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
        return (Serializable) objectInputStream.readObject();
    }

    @Override
    public Set<String> call() {
        if (_patternFiles == null || _patternFiles.isEmpty()) {
            return EmptySet;
        }
        Set<String> strings = new HashSet<>();
        for (String f : _patternFiles) {
            Pattern p;
            try {
                p = (Pattern) deserialize(f);
            } catch (Exception e) {
                continue;
            }
            Node node = p.getPatternNode();
            Queue<Node> nodes = new LinkedList<>();
            nodes.add(node);
            while(!nodes.isEmpty()) {
                node = nodes.poll();
                if (NodeUtils.isSimpleExpr(node)) {
                    switch (node.getNodeType()) {
                        case BLITERAL:
                        case NULL:
                        case THIS:
                            break;
                        case SLITERAL:
                            StringBuffer s = node.toSrcString();
                            if (s.length() < 10) {
                                strings.add(s.toString());
                            }
                            break;
                        default:
                            strings.add(node.toSrcString().toString());
                    }
                }
                nodes.addAll(node.getAllChildren());
            }
        }
        return strings;
    }

}