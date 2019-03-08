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
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.modify.Modification;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.PatternExtractor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author: Jiajun
 * @date: 2019-02-07
 */
public class Filter {

    private String _outFile;
    private int _maxChangeLine = 50;
    private int _maxChangeAction = 20;

    private volatile List<String> _cacheList;
    private int _currItemCount = 0;
    private int _cacheSize = 10000;

    private int _currThreadCount = 0;
    private int _maxThreadCount = 9;
    private ExecutorService _threadPool;
    private List<Future<List<String>>> _threadResultList = new ArrayList<>();

    private Options options() {
        Options options = new Options();

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(true);

        Option option = new Option("ip", "inpath", true, "input path.");
        optionGroup.addOption(option);

        option = new Option("filter", "filterFile", true, "existing record for filter");
        optionGroup.addOption(option);

        options.addOptionGroup(optionGroup);

        option = new Option("line", "maxLine", true, "max number of changed lines within one pattern.");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("change", "maxAction", true, "max number of modifications within one pattern.");
        option.setRequired(false);
        options.addOption(option);

        optionGroup = new OptionGroup();
        optionGroup.setRequired(false);
        optionGroup.addOption(new Option("op", "outpath", true, "output path."));
        optionGroup.addOption(new Option("of", "outfile", true, "output file."));
        options.addOptionGroup(optionGroup);

        return options;
    }

    public Filter() {
        _cacheList = new ArrayList<>(_cacheSize);
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

    private Set<String> readPatterns(String file) throws IOException {
        Set<String> patterns = new HashSet<>();
        BufferedReader br;
        br = new BufferedReader(new FileReader(new File(file)));
        String line;
        while((line = br.readLine()) != null) {
            // TODO: here is the hard encode, improve later
            if (line.startsWith("/home/jiajun/GithubData")) {
                patterns.add(line.substring(0, line.lastIndexOf('-'))
                        .replace("pattern-ver1-serial", "buggy-version"));
            }
        }
        br.close();
        return patterns;
    }

    private void filter(String srcFileName, String tarFileName) {
        try {
            if (_currThreadCount >= _maxThreadCount) {
                LevelLogger.info("Thread pool is full ....");
                for (Future<List<String>> fs : _threadResultList) {
                    List<String> result = fs.get();
                    writeFile(result);
                    _currThreadCount--;
                }
                _threadResultList.clear();
                _currThreadCount = _threadResultList.size();
                LevelLogger.info("Cleared thread pool : " + _currThreadCount);
            }
            Future<List<String>> future = _threadPool.submit(new ParseNode(srcFileName, tarFileName,
                    _maxChangeLine, _maxChangeAction));
            _threadResultList.add(future);
            _currThreadCount ++;

        } catch (Exception e) {
            LevelLogger.error("Parse node failed : ", e);
        }
    }

    private void filterWithExistingRecord(String file) throws IOException {
        Set<String> files = readPatterns(file);
        for (String f : files) {
            filter(f, f.replace("buggy-version", "fixed-version"));
        }
    }

    private void filter(File file) {
        if (file.isDirectory()) {
            if (file.getName().endsWith("buggy-version")) {
                File[] files = file.listFiles();
                for (File f : files) {
                    String srcFileName = f.getAbsolutePath();
                    String tarFileName = srcFileName.replace("buggy-version", "fixed-version");
                    filter(srcFileName, tarFileName);
                }
            } else {
                File[] files = file.listFiles();
                for (File f : files)  {
                    filter(f);
                }
            }
        }
    }

    public void writeFile(List<String> values) throws IOException {
        if (values != null) {
            _cacheList.addAll(values);
            _currItemCount += values.size();
            LevelLogger.info("Current cache size : " + _currItemCount);
            if (_currItemCount >= _cacheSize) {
                flush();
            }
        }
    }

    private void flush() throws IOException {
        LevelLogger.info("........FLUSHING.......");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_outFile, true), "UTF-8"));
        for (String s : _cacheList) {
            bw.write(s + Constant.NEW_LINE);
        }
        _cacheList.clear();
        _currItemCount = 0;
        bw.close();
    }

    public void filter(String[] args) {
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp("<command> -ip <arg> [-line <arg>] [-change <arg>] [-of | -op <arg>]", options);
            System.exit(1);
        }

        if (cmd.hasOption("line")) {
            _maxChangeLine = Integer.parseInt(cmd.getOptionValue("line"));
        }
        if (cmd.hasOption("change")) {
            _maxChangeAction = Integer.parseInt(cmd.getOptionValue("change"));
        }

        if (cmd.hasOption("of")) {
            _outFile = cmd.getOptionValue("of");
        } else if (cmd.hasOption("op")) {
            _outFile = Utils.join(Constant.SEP, cmd.getOptionValue("op"), "API_Mapping.txt");
        } else {
            _outFile = Utils.join(Constant.SEP, System.getProperty("user.dir"), "API_Mapping.txt");
        }

        init(_outFile);
        _threadPool = Executors.newFixedThreadPool(_maxThreadCount);

        if (cmd.hasOption("filter")) {
            String inPath = cmd.getOptionValue("filter");
            try {
                filterWithExistingRecord(inPath);
            } catch (IOException e) {
                LevelLogger.error("Failed to filter!", e);
            }
        } else {
            String inPath = cmd.getOptionValue("ip");
            filter(new File(inPath));
        }

        for (Future<List<String>> fs : _threadResultList) {
            try {
                List<String> result = fs.get();
                writeFile(result);
            } catch (Exception e) { }
        }
        try {
            flush();
        } catch (IOException e) {
            LevelLogger.error("Output data to file failed!");
        }
        _threadPool.shutdown();

        System.out.println("Finish filtering !");
    }

    public static void main(String[] args) {
        Filter filter = new Filter();
        filter.filter(args);
    }

}

class ParseNode implements Callable<List<String>> {

    private String _srcFile;
    private String _tarFile;
    private int _maxChangeLine;
    private int _maxChangeAction;

    public ParseNode(String srcFile, String tarFile, int maxChangeLine, int maxChangeAction) {
        _srcFile = srcFile;
        _tarFile = tarFile;
        _maxChangeLine = maxChangeLine;
        _maxChangeAction = maxChangeAction;
    }

    @Override
    public List<String> call() {
        LevelLogger.info("PARSE > " + _srcFile);
        List<String> result = new LinkedList<>();
        if (!(new File(_srcFile).exists()) || !(new File(_tarFile).exists())) {
            LevelLogger.info("Following file may not exist ... SKIP.\n" + _srcFile + "\n" + _tarFile);
            return result;
        }
        PatternExtractor patternExtractor = new PatternExtractor();
        Set<Pattern> patternCandidates = patternExtractor.extractPattern(_srcFile, _tarFile);
        if (patternCandidates.isEmpty()) {
            LevelLogger.info("No pattern node ... SKIP.");
            return result;
        }
        // the following is the filter process
        for (Pattern pattern : patternCandidates) {
            Set<Modification> modifications = pattern.getAllModifications();
            // filter by modifications
            if (modifications.size() > _maxChangeAction) {
                LevelLogger.info("Too many modifications : " + modifications.size() + " ... SKIP.");
                continue;
            }
            Node node = pattern.getPatternNode();
            // filter by changed lines
            if (node.getBindingNode() != null) {
                Node other = node.getBindingNode();
                TextDiff diff = new TextDiff(node, other);
                int size = diff.getMiniDiff().size();
                if (size > _maxChangeLine) {
                    LevelLogger.info("Too many changed lines : " + size + " ... SKIP.");
                    continue;
                }
            }
            MethDecl methDecl = (MethDecl) node;

            String savePatternPath = _srcFile.replace("buggy-version", "pattern-" + Constant.PATTERN_VERSION + "-serial");
            savePatternPath = savePatternPath + "-" + methDecl.getName().getName() +".pattern";
            File file = new File(savePatternPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try {
                file.createNewFile();
                Utils.serialize(pattern, savePatternPath);
            } catch (IOException e) {
                LevelLogger.warn("Serialization failed : " + savePatternPath);
                continue;
            }
            Set<String> keywords = pattern.getKeywords();

            for (String s : keywords) {
                result.add(s);
            }
            result.add(savePatternPath);
        }
        return result;
    }

}
