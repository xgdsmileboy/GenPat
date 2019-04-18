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
    private String _ins;
    private String _del;
    private String _upd;

    public Search(String fileName, String ins, String del, String upd) {
        _fileName = fileName;
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
            Set<Modification> inserts = modifications.stream()
                    .filter(m -> m instanceof Insertion || m instanceof Wrap)
                    .collect(Collectors.toSet());
            boolean contain = false;
            Insertion insertion;
            for (Modification m : inserts) {
                insertion = (Insertion) m;
                if (insertion.getInsertedNode().toString().contains(_ins)) {
                    contain = true;
                    break;
                }
            }
            if (!contain) return null;
        }
        if (_del != null) {
            Set<Modification> deletes = modifications.stream()
                    .filter(m -> m instanceof Deletion)
                    .collect(Collectors.toSet());
            boolean contain = false;
            Deletion deletion;
            for (Modification m : deletes) {
                deletion = (Deletion) m;
                if (deletion.getDelNode().toString().contains(_del)) {
                    contain = true;
                    break;
                }
            }
            if (!contain) return null;

        }
        if (_upd != null) {
            Set<Modification> updates = modifications.stream()
                    .filter(m -> m instanceof Update)
                    .collect(Collectors.toSet());
            boolean contain = false;
            Update update;
            for (Modification m : updates) {
                update = (Update) m;
                if (update.getSrcNode() != null && update.getSrcNode().toString().contains(_upd)) {
                    contain = true;
                    break;
                }
                if (update.getTarNode() != null && update.getTarNode().toString().contains(_upd)) {
                    contain = true;
                    break;
                }
            }
            if (!contain) return null;
        }
        return _fileName;
    }

    private static void search(String fileName, String ins, String del, String upd) {
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
                .append("\n---------------\n");
        JavaFile.writeStringToFile(result, buffer.toString(), false);
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<String>> threads = new LinkedList<>();
        int count = 0;
        for (String file : files) {
            if (count >= threadPoolSize) {
                clear(threads);
                threads.clear();
                count = 0;
            }
            threads.add(pool.submit(new Search(file, ins, del, upd)));
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
                LevelLogger.error("Get result exception.");
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
            formatter.printHelp("<command> -if <arg> [-ins <arg>] [-del <arg>] [-upd <arg>]", options);
            System.exit(1);
        }
        if (cmd.hasOption("thread")) {
            threadPoolSize = Integer.parseInt(cmd.getOptionValue("thread"));
        }

        search(cmd.getOptionValue("if"), cmd.getOptionValue("ins"),
                cmd.getOptionValue("del"), cmd.getOptionValue("upd"));
    }


    public static void main(String[] args) throws Exception{
//        search(args);
        Pattern p = (Pattern) Utils.deserialize("p.pattern");
        Set<Modification> modifications = p.getAllModifications();
        String _upd = "instance";
        if (_upd != null) {
            Set<Modification> updates = modifications.stream()
                    .filter(m -> m instanceof Update)
                    .collect(Collectors.toSet());
            boolean contain = false;
            Update update;
            for (Modification m : updates) {
                update = (Update) m;
                if (update.getSrcNode() != null && update.getSrcNode().toString().contains(_upd)) {
                    contain = true;
                    break;
                }
                if (update.getTarNode() != null && update.getTarNode().toString().contains(_upd)) {
                    contain = true;
                    break;
                }
            }
            System.out.println(contain);
        }
    }

}
