/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;

import mfix.tools.Cleaner;
import mfix.tools.Filter;
import mfix.tools.PatternPrinter;
import mfix.tools.Repair;

public class Main {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Please given the arguments");
            System.err.println("\tclean : delete serialized pattern files.");
            System.err.println("\tprint : print serialized patterns.");
            System.err.println("\trepair : repair bugs.");
            System.err.println("\tfilter : serialize and filter patterns and output API mapping info.");
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
            default:
                Repair repair = new Repair();
                repair.repair(args);
        }
    }
}

