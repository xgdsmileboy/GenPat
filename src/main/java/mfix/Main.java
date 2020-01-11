/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;

import mfix.tools.*;

public class Main {

    static String pSrc = "package com.company;\n" +
            "public class Main {\n" +
            "    public static void test1(int x) {\n" +
            "        int t;\n" +
            "        t = x;\n" +
            "        return t;\n" +
            "    }\n" +
            "    public static void test2(int y) {\n" +
            "        int t;\n" +
            "        t = y;\n" +
            "        return t;\n" +
            "    }\n" +
            "    public static void main(String[] args) {\n" +
            "    }\n" +
            "}\n";
    static String pTar = "package com.company;\n" +
            "public class Main {\n" +
            "    public static void test1(int x) {\n" +
            "        int t;\n" +
            "        t = x + 1;\n" +
            "        return t;\n" +
            "    }\n" +
            "    public static void test2(int y) {\n" +
            "        int t;\n" +
            "        t = y;\n" +
            "        return t;\n" +
            "    }\n" +
            "    public static void main(String[] args) {\n" +
            "    }\n" +
            "}\n";
    static String pSrcMethod = "public static void test1(int x) {\n" +
            "        int t;\n" +
            "        t = x;\n" +
            "        return t;\n" +
            "    }";
    static String pTarMethod = "public static void test1(int x) {\n" +
            "        int t;\n" +
            "        t = x + 1;\n" +
            "        return t;\n" +
            "    }";
    static String buggyMethod = "public static void test2(int y) {\n" +
            "        int t;\n" +
            "        t = y;\n" +
            "        return t;\n" +
            "    }";
    
    public static void main(String[] args) {

        Transformer t = new Transformer();
        t.loadPatternSrc(pSrcMethod, pSrc, "C:\\Users\\37583\\IdeaProjects\\untitled2\\src\\com\\company\\a.java");
        t.loadPatternTar(pTarMethod, pTar, "C:\\Users\\37583\\IdeaProjects\\untitled2\\src\\com\\company\\b.java");
        t.extractPattern();
        String ret = t.apply(buggyMethod, "C:\\Users\\37583\\IdeaProjects\\untitled2\\src\\com\\company\\c.java");
        System.out.println(ret);

        if (args.length == 0) {
            System.err.println("Please given the arguments");
            System.err.println("\tclean : delete serialized pattern files.");
            System.err.println("\tprint : print detail information of given pattern.");
            System.err.println("\trepair : repair bugs.");
            System.err.println("\tfilter : serialize and filter patterns and output pattern records.");
            System.err.println("\tcluster : cluster serialized patterns.");
            System.err.println("\tstatistic : keyword statistics.");
            System.err.println("\tsearch : search desired patterns.");
            System.exit(1);
        }

        switch (args[0]) {
            case "clean":
                Cleaner cleaner = new Cleaner();
                cleaner.clean(args);
                break;
            case "print":
                PatternPrinter patternPrinter = new PatternPrinter();
                patternPrinter.print(args);
                break;
            case "filter":
                Filter filter = new Filter();
                filter.filter(args);
                break;
            case "cluster":
                Cluster cluster = new Cluster();
                cluster.cluster(args);
                break;
            case "statistic":
                TokenStatistic tokenStatistic = new TokenStatistic();
                tokenStatistic.statistic(args);
                break;
            case "search":
                Search.search(args);
                break;
            default:
                Repair.repairAPI(args);
        }
    }
}

