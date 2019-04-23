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
import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.node.modify.Deletion;
import mfix.core.node.modify.Insertion;
import mfix.core.node.modify.Modification;
import mfix.core.node.modify.Update;
import mfix.core.node.modify.Wrap;
import mfix.core.pattern.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author: Jiajun
 * @date: 2019-04-17
 */
public class Search implements Callable<String> {

    private final static String result = Constant.HOME + Constant.SEP + "path.txt";
    private static int  threadPoolSize = 10;

    private String _fileName;
    private boolean _regex;
    private String _ins;
    private String _del;
    private String _upd;

    public Search(String fileName, boolean regex, String ins, String del, String upd) {
        _fileName = fileName;
        _regex = regex;
        _ins = ins;
        _del = del;
        _upd = upd;
    }

    @Override
    public String call() throws Exception {
        LevelLogger.info(_fileName);
        Pattern p = (Pattern) Utils.deserialize(_fileName);
        Set<Modification> modifications = p.getAllModifications();
        if (_ins != null) {
            Set<Node> inserts = modifications.stream()
                    .filter(m -> m instanceof Insertion)
                    .map(m -> ((Insertion) m).getInsertedNode())
                    .collect(Collectors.toSet());
            boolean contain = _regex ? containRegex(inserts, _ins) : containStr(inserts, _ins);
            if (!contain) return null;
        }
        if (_del != null) {
            Set<Node> deletes = modifications.stream()
                    .filter(m -> m instanceof Deletion)
                    .map(m -> ((Deletion) m).getDelNode())
                    .collect(Collectors.toSet());
            boolean contain = _regex ? containRegex(deletes, _del) : containStr(deletes, _del);
            if (!contain) return null;

        }
        if (_upd != null) {
            Set<Modification> updates = modifications.stream()
                    .filter(m -> m instanceof Update || m instanceof Wrap)
                    .collect(Collectors.toSet());
            Update update;
            Set<Node> nodes = new HashSet<>();
            for (Modification m : updates) {
                update = (Update) m;
                nodes.add(update.getSrcNode());
                nodes.add(update.getTarNode());
            }
            boolean contain = _regex ? containRegex(nodes, _upd) : containStr(nodes, _upd);
            if (!contain) return null;
        }
        return _fileName;
    }

    private boolean containRegex(Set<Node> nodes, String string) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(string);
        for (Node node : nodes) {
            if (node == null) continue;
            if (pattern.matcher(node.toSrcString().toString()).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean containStr(Set<Node> nodes, String string) {
        for (Node node : nodes) {
            if (node == null) continue;
            if (node.toSrcString().toString().contains(string)) {
                return true;
            }
        }
        return false;
    }

    private static void search(String fileName, boolean regex, String ins, String del, String upd) {
        if (ins == null && del == null && upd == null) {
            LevelLogger.error("Please given searching criteria.");
            return;
        }
        List<String> files = JavaFile.readFileToStringList(fileName);
        if (files == null || files.isEmpty()) {
            LevelLogger.error("No pattern records found!");
            return ;
        }
        StringBuffer buffer = new StringBuffer("INS : ")
                .append(ins == null ? "" : ins)
                .append("\nDEL : ")
                .append(del == null ? "" : del)
                .append("\nUPD : ")
                .append(upd == null ? "" : upd)
                .append("\nRegex : ").append(regex)
                .append("\n---------------\n");
        JavaFile.writeStringToFile(result, buffer.toString(), false);
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<String>> threads = new LinkedList<>();
        int count = 0;
        for (String file : files) {
            if (file.startsWith(Constant.DEFAULT_PATTERN_HOME)) {
                if (count >= threadPoolSize) {
                    clear(threads);
                    threads.clear();
                    count = 0;
                }
                threads.add(pool.submit(new Search(file, regex, ins, del, upd)));
            }
        }
        clear(threads);
        pool.shutdown();
    }

    private static void clear(List<Future<String>> threads) {
        StringBuffer buffer = new StringBuffer();
        for (Future<String> future : threads) {
            try {
                String rslt = future.get(2, TimeUnit.MINUTES);
                if (rslt != null) {
                    buffer.append(rslt).append("\n");
                }
            } catch (TimeoutException e) {
                LevelLogger.error("Get result timeout.");
                future.cancel(true);
            } catch (Exception e) {
                LevelLogger.error("Get result exception.", e);
                future.cancel(true);
            }
        }
        threads.clear();
        if (buffer.length() > 0) {
            JavaFile.writeStringToFile(result, buffer.toString(), true);
        }
    }

    private static Options options() {
        Options options = new Options();

        Option option = new Option("if", "RecordFile", true,
                "The file that contains the paths of patterns to search.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("e", "E", false,
                "Use regular expression for code search");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("ins", "insert", true,
                "String in the inserted node");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("del", "delete", true,
                "String in the deleted node");
        option.setRequired(false);
        options.addOption(option);


        option = new Option("upd", "update", true,
                "String in the old or new node");
        option.setRequired(false);
        options.addOption(option);

        option = new Option("thread", "thread", true,
                "Max number of concurrent threads.");
        option.setRequired(false);
        options.addOption(option);

        return options;
    }

    public static void search(String[] args) {
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp("<command> -if <arg> [-e] [-ins <arg>] [-del <arg>] [-upd <arg>]", options);
            System.exit(1);
        }
        if (cmd.hasOption("thread")) {
            threadPoolSize = Integer.parseInt(cmd.getOptionValue("thread"));
        }

        search(cmd.getOptionValue("if"), cmd.hasOption("e"), cmd.getOptionValue("ins"),
                cmd.getOptionValue("del"), cmd.getOptionValue("upd"));
    }


    public static void main(String[] args) {
        search(args);
    }

}
