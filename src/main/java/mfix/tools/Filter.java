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
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.diff.TextDiff;
import mfix.core.node.modify.Modification;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
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

    private List<String> _cacheList;
    private int _currItemCount = 0;
    private int _cacheSize = 10000;

    private int _currThreadCount = 0;
    private int _maxThreadCount = 9;
    private ExecutorService _threadPool;
    private List<Future<Boolean>> _threadResultList = new ArrayList<>();

    private Options options() {
        Options options = new Options();

        Option option = new Option("ip", "inpath", true, "input path.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("line", "maxLine", true, "max number of changed lines within one pattern.");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("change", "maxAction", true, "max number of modifications within one pattern.");
        option.setRequired(false);
        options.addOption(option);

        OptionGroup optionGroup = new OptionGroup();
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

    private void filter(File file) {
        if (file.isDirectory()) {
            if (file.getName().endsWith("buggy-version")) {
                File[] files = file.listFiles();
                for (File f : files) {
                    String srcFileName = f.getAbsolutePath();
                    String tarFileName = srcFileName.replace("buggy-version", "fixed-version");
                    try {
                        if (_currThreadCount >= _maxThreadCount) {
                            LevelLogger.info("Thread pool is full ....");
                            for (Future<Boolean> fs : _threadResultList) {
                                Boolean result = fs.get();
                                _currThreadCount--;
                            }
                            _threadResultList.clear();
                            _currThreadCount = _threadResultList.size();
                            LevelLogger.info("Cleared thread pool : " + _currThreadCount);
                        }
                        Future<Boolean> future = _threadPool.submit(new ParseNode(srcFileName, tarFileName,
                                _maxChangeLine, _maxChangeAction, this));
                        _threadResultList.add(future);

                    } catch (Exception e) {
                        LevelLogger.error("Parse node failed : " + e.getMessage());
                    }
                }
            } else {
                File[] files = file.listFiles();
                for (File f : files)  {
                    filter(f);
                }
            }
        }
    }

    public synchronized void writeFile(Set<String> values) throws IOException {
        if (values != null) {
            _cacheList.addAll(values);
            _currItemCount += values.size();
            LevelLogger.info("Current cache size : " + _currItemCount);
            if (_currItemCount >= _cacheSize) {
                flush();
            }
        }
    }

    private synchronized void flush() throws IOException {
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

        final String inPath = cmd.getOptionValue("ip");
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

        filter(new File(inPath));

        try {
            flush();
        } catch (IOException e) {
            LevelLogger.error("Output data to file failed!");
        }
        _threadPool.shutdown();

        System.out.println("Finish filtering !");
    }

    public static void main(String[] args) throws Exception {
        Filter filter = new Filter();
        filter.filter(args);
    }

}

class ParseNode implements Callable<Boolean> {

    private String _srcFile;
    private String _tarFile;
    private int _maxChangeLine;
    private int _maxChangeAction;
    private Filter _filter;

    public ParseNode(String srcFile, String tarFile, int maxChangeLine, int maxChangeAction, Filter filter) {
        _srcFile = srcFile;
        _tarFile = tarFile;
        _maxChangeLine = maxChangeLine;
        _maxChangeAction = maxChangeAction;
        _filter = filter;
    }

    @Override
    public Boolean call() {
        LevelLogger.info("PARSE > " + _srcFile);
        if (!(new File(_srcFile).exists()) || !(new File(_tarFile).exists())) {
            LevelLogger.info("Following file may not exist ... SKIP.\n" + _srcFile + "\n" + _tarFile);
            return false;
        }
        PatternExtractor patternExtractor = new PatternExtractor();
        Set<Node> patternCandidates = patternExtractor.extractPattern(_srcFile, _tarFile);
        if (patternCandidates.isEmpty()) {
            LevelLogger.info("No pattern node ... SKIP.");
            return false;
        }
        Set<String> result = new HashSet<>();
        // the following is the filter process
        for (Node node : patternCandidates) {
            Set<Modification> modifications = node.getAllModifications(new HashSet<>());
            // filter by modifications
            if (modifications.size() > _maxChangeAction) {
                LevelLogger.info("Too many modifications : " + modifications.size() + " ... SKIP.");
                continue;
            }
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
                Utils.serialize(node, savePatternPath);
            } catch (IOException e) {
                LevelLogger.warn("Serialization failed : " + savePatternPath);
                continue;
            }
            Set<MethodInv> methods = node.getUniversalAPIs(new HashSet<>(), true);
            if (!methods.isEmpty()) {
                for (MethodInv methodInv : methods) {
                    String rType = methodInv.getTypeString();
                    String name = methodInv.getName().getName();
                    Expr expr = methodInv.getExpression();
                    String oType = expr == null ? "?" : expr.getTypeString();
                    List<Expr> exprList = methodInv.getArguments().getExpr();
                    int argNum = exprList.size();
                    // the first "?" is the placeholder for argument
                    StringBuffer argType = new StringBuffer("?");
                    for (Expr e : exprList) {
                        argType.append("," + e.getTypeString());
                    }

                    result.add(String.format("%s\t%d\t%s\t%s\t%s\t%s", name, argNum, rType, oType, argType, savePatternPath));
                }
            } else {
                LevelLogger.info("No API info .... SKIP.");
            }
        }
        try {
            _filter.writeFile(result);
        } catch (IOException e) {
            LevelLogger.error("Write to file failed in parse thread!");
        }
        return true;
    }

}
