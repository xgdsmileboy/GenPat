/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.MiningUtils;
import mfix.common.util.Pair;
import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.pattern.Pattern;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-03-14
 */
public class TokenStatistic {

    private Map<String, Integer> _cacheApiMap;
    private Map<String, Integer> _cacheTypeMap;
    private Map<String, Integer> _catchVarMap;
    private Set<String> _files;
    private int totalFile;

    private int _currThreadCount = 0;
    private int _maxThreadCount = Constant.MAX_FILTER_THREAD_NUM;
    private ExecutorService _threadPool;
    private List<Pair<String, Future<Keyword>>> _threadResultList = new ArrayList<>();
    private final String error_file = Utils.join(Constant.SEP, Constant.HOME, "error.log");

    private final static String COMMAND = "<command> -if <arg> [-op <arg>] [-dir <arg>]";

    private Options options() {
        Options options = new Options();

        Option option = new Option("if", "InputFile", true,
                "The file contains all the pattern paths.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("op", "outputPath", true,
                "The directory for output.");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("dir", "BaseDir", true,
                "The base directory of pattern files.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    public TokenStatistic() {
        _cacheApiMap = new HashMap<>();
        _cacheTypeMap = new HashMap<>();
        _catchVarMap = new HashMap<>();
        _files = new HashSet<>();
    }

    private void init(String... names) {
        for (String fileName : names) {
            Utils.moveFile(fileName, fileName + ".bak");
            JavaFile.writeStringToFile(fileName, "");
        }
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
        _files.add(buggyFileName);
        if (_currThreadCount >= _maxThreadCount) {
            LevelLogger.debug("Thread pool is full ....");
            Keyword result;
            for (Pair<String, Future<Keyword>> fs : _threadResultList) {
                try {
                    result = fs.getSecond().get();
                } catch (Exception e) {
                    LevelLogger.error("Do keyword statistic failed : ", e);
                    fs.getSecond().cancel(true);
                    JavaFile.writeStringToFile(error_file, fs.getFirst() + "\n", true);
                    continue;
                }
                totalFile++;
                LevelLogger.debug("Parse file : ----- < " + totalFile + " > -----");
                if (result != null) {
                    Integer count;
                    for (String s : result.getTypes()) {
                        count = _cacheTypeMap.get(s);
                        count = count == null ? 1 : count + 1;
                        _cacheTypeMap.put(s, count);
                    }
                    for (String s : result.getAPIS()) {
                        count = _cacheApiMap.get(s);
                        count = count == null ? 1 : count + 1;
                        _cacheApiMap.put(s, count);
                    }
                    for (String s : result.getNames()) {
                        count = _catchVarMap.get(s);
                        count = count == null ? 1 : count + 1;
                        _catchVarMap.put(s, count);
                    }
                }
                _currThreadCount--;
            }
            _threadResultList.clear();
            _currThreadCount = _threadResultList.size();
            LevelLogger.debug("Cleared thread pool : " + _currThreadCount);
        }
        Future<Keyword> future = _threadPool.submit(new ParseKey(patternFiles));
        _threadResultList.add(new Pair<>(buggyFileName, future));
        _currThreadCount++;
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

    private void writeFile(String outFile, Map<String, Integer> map) throws IOException {
        LevelLogger.info("........FLUSHING.......");
        if (map == null || map.isEmpty()) {
            return;
        }
        File file = new File(outFile);
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false),
                StandardCharsets.UTF_8));
        List<Map.Entry<String, Integer>> entryList = new LinkedList<>(map.entrySet());
        entryList = entryList.stream()
                .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                .collect(Collectors.toList());
        for (Map.Entry<String, Integer> entry : entryList) {
            bw.write(entry.getKey());
            bw.newLine();
            bw.write(entry.getValue().toString());
            bw.newLine();
        }
        bw.close();
    }

    private void writeBuggyFiles(String outFile) throws IOException {
        LevelLogger.info("....FLUSHING FILES.......");
        File file = new File(outFile);
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false),
                StandardCharsets.UTF_8));
        for (String s : _files) {
            bw.write(s);
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

        String outPath = Constant.HOME;
        if (cmd.hasOption("op")) {
            outPath = cmd.getOptionValue("op");
        }
        String apiFile = Utils.join(Constant.SEP, outPath, "AllTokens_api.txt");
        String typeFile = Utils.join(Constant.SEP, outPath, "AllTokens_type.txt");
        String varFile = Utils.join(Constant.SEP, outPath, "AllTokens_var.txt");
        optionMap.put("api", apiFile);
        optionMap.put("type", typeFile);
        optionMap.put("var", varFile);

        return optionMap;
    }

    public void statistic(String[] args) {
        Map<String, String> optionMap = parseOption(args);
        String apiOutFile = optionMap.get("api");
        String typeOutFile = optionMap.get("type");
        String varOutFile = optionMap.get("var");
        init(apiOutFile, typeOutFile, varOutFile);

        totalFile = 0;
        _threadPool = Executors.newFixedThreadPool(_maxThreadCount);

        statisticWithExistingRecord(optionMap.get("dir"), optionMap.get("if"));

        try {
            writeFile(apiOutFile, _cacheApiMap);
            writeFile(typeOutFile, _cacheTypeMap);
            writeFile(varOutFile, _catchVarMap);
            writeBuggyFiles(Utils.join(Constant.SEP, Constant.HOME, "BuggyFiles.txt"));
        } catch (IOException e) {
            LevelLogger.error("Dump to result to file failed!", e);
        }
        _threadPool.shutdown();

        System.out.println("Finish keyword statistics !");
    }

    public static void main(String[] args) {
        TokenStatistic tokenStatistic = new TokenStatistic();
        tokenStatistic.statistic(args);
    }

}


class ParseKey implements Callable<Keyword> {


    private Set<String> _patternFiles;

    public ParseKey(Set<String> patterns) {
        _patternFiles = patterns;
    }

    @Override
    public Keyword call() {
        if (_patternFiles == null || _patternFiles.isEmpty()) {
            return new Keyword();
        }
        Set<String> types = new HashSet<>();
        Set<String> apis = new HashSet<>();
        Set<String> vars = new HashSet<>();
        String type, api, var;
        for (String f : _patternFiles) {
            Pattern p;
            try {
                p = (Pattern) Utils.deserialize(f);
            } catch (Exception e) {
                continue;
            }
            Node node = p.getPatternNode();
            Queue<Node> nodes = new LinkedList<>();
            nodes.add(node);
            while (!nodes.isEmpty()) {
                node = nodes.poll();
                type = node.getTypeStr();
                if (type != null && !"?".equals(type)) {
                    types.add(type);
                }
                api = node.getAPIStr();
                if (api != null) {
                    apis.add(api);
                }
                var = node.getNameStr();
                if (var != null) {
                    vars.add(var);
                }
                nodes.addAll(node.getAllChildren());
            }
        }
        return new Keyword(vars, apis, types);
    }

}

class Keyword {
    private Set<String> _names;
    private Set<String> _apis;
    private Set<String> _types;

    public Keyword() {
        _names = Collections.emptySet();
        _apis = Collections.emptySet();
        _types = Collections.emptySet();
    }

    public Keyword(Set<String> names, Set<String> apis, Set<String> types) {
        _names = names;
        _apis = apis;
        _types = types;
    }

    public Set<String> getNames() {
        return _names;
    }

    public Set<String> getTypes() {
        return _types;
    }

    public Set<String> getAPIS() {
        return _apis;
    }
}