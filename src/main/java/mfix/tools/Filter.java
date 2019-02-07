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
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.MethodInv;
import mfix.core.node.modify.Modification;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

    private BufferedWriter bw;
    private int MAX_CHANGE_LINE;

    private List<String> cacheList;
    private int currItemCount = 0;
    private int cacheSize = 10000;

    private int currThreadCount = 0;
    private int maxThreadCount = 9;
    private ExecutorService threadPool;
    private List<Future<Set<String>>> threadResultList = new ArrayList<>();

    private Options options() {
        Options options = new Options();

        Option option = new Option("ip", "inpath", true, "input path.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("line", "maxLine", true, "max changed line number");
        option.setRequired(true);
        options.addOption(option);

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.setRequired(false);
        optionGroup.addOption(new Option("op", "outpath", true, "output path."));
        optionGroup.addOption(new Option("of", "outfile", true, "output file."));
        options.addOptionGroup(optionGroup);

        return options;
    }

    public Filter() {
        cacheList = new ArrayList<>(cacheSize);
    }

    private void initWriter(String filePath) throws IOException {
        initWriter(new File(Utils.join(Constant.SEP, filePath, "API_Mapping.txt")));
    }

    private void initWriter(File file) throws IOException {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                LevelLogger.error("Create file failed : " + file.getAbsolutePath());
                System.exit(1);
            }
        }
        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"));
    }

    private void filter(File file) {
        if (file.isDirectory()) {
            if (file.getName().endsWith("buggy-version")) {
                File[] files = file.listFiles();
                for (File f : files) {
                    String srcFileName = f.getAbsolutePath();
                    String tarFileName = srcFileName.replace("buggy-version", "fixed-version");
                    try {
                        if (currThreadCount >= maxThreadCount) {
                            for (Future<Set<String>> fs : threadResultList) {
                                Set<String> result = fs.get();
                                currThreadCount--;
                                if (result != null) {
                                    writeFile(result);
                                }
                            }
                            threadResultList.clear();
                            currThreadCount = threadResultList.size();
                        }
                        Future<Set<String>> future = threadPool.submit(new ParseNode(srcFileName, tarFileName));
                        threadResultList.add(future);

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

    private synchronized void writeFile(Set<String> values) throws IOException {
        if (values != null) {
            cacheList.addAll(values);
            currItemCount += values.size();
            if (currItemCount >= cacheSize) {
                flush();
            }
        }
    }

    private synchronized void flush() throws IOException {
        for (String s : cacheList) {
            bw.write(s + Constant.NEW_LINE);
        }
        cacheList.clear();
        currItemCount = 0;
    }

    private void close() throws IOException{
        if (bw != null) {
            bw.close();
        }
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
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        final String inpath = cmd.getOptionValue("ip");
        MAX_CHANGE_LINE = Integer.parseInt(cmd.getOptionValue("line"));

        try {
            if (cmd.hasOption("of")) {
                initWriter(new File(cmd.getOptionValue("of")));
            } else if (cmd.hasOption("op")) {
                initWriter(cmd.getOptionValue("op"));
            } else {
                initWriter(System.getProperty("user.dir"));
            }
        } catch (IOException e) {
            LevelLogger.error("Init output file failed!");
            System.exit(1);
        }

        threadPool = Executors.newFixedThreadPool(maxThreadCount);

        filter(new File(inpath));

        try {
            flush();
            close();
        } catch (IOException e) {
            LevelLogger.error("Output data to file failed!");
        }
        threadPool.shutdown();

        System.out.println("Finish filtering !");
    }

    public static void main(String[] args) throws Exception {
        Filter filter = new Filter();
        filter.filter(args);
    }

}

class ParseNode implements Callable<Set<String>> {

    private String _srcFile;
    private String _tarFile;

    public ParseNode(String srcFile, String tarFile) {
        _srcFile = srcFile;
        _tarFile = tarFile;
    }

    @Override
    public Set<String> call() {
        if (!(new File(_srcFile).exists()) || !(new File(_tarFile).exists())) {
            return null;
        }
        Set<Node> patternCandidates = PatternExtractor.extractPattern(_srcFile, _tarFile);
        if (patternCandidates.isEmpty()) {
            return null;
        }
        Set<String> result = new HashSet<>();
        // the following is the filter process
        for (Node node : patternCandidates) {
            Set<Modification> modifications = node.getAllModifications(new HashSet<>());
            if (modifications.size() > 20) {
                continue;
            }
            MethDecl methDecl = (MethDecl) node;

            String savePatternPath = _srcFile.replace("buggy-version", "pattern-" + Constant.PATTERN_VERSION + "-serial");
            savePatternPath = savePatternPath + "-" + methDecl.getName().getName() +".pattern";
            try {
                Utils.serialize(node, savePatternPath);
            } catch (IOException e) {
                continue;
            }
            Set<MethodInv> methods = node.getUniversalAPIs(new HashSet<>(), true);
            if (methods.isEmpty()) {
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
            }
        }
        return result;
    }

}
