/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix;


import mfix.common.util.JavaFile;
import mfix.core.stats.element.DatabaseConnector;
import mfix.core.stats.Analyzer;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    static Analyzer analyzer;

    static void runFolder(String folderPath) {
        File rootFile = new File(folderPath);
        if (!rootFile.exists()) {
            return;
        }
        List<File> files = JavaFile.ergodic(rootFile, new LinkedList<>());
        for (File file : files) {
            analyzer.runFile(file.getAbsolutePath());
        }
    }

    static void work(String fileListFilePath) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileListFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        Integer cnt = 0;
        String filePath = null;
        while(true)
        {
            try {
                filePath = bufferedReader.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (filePath == null) {
                break;
            }

            cnt += 1;
            System.out.println(cnt.toString() + ":" + filePath);
            try {
                runFolder(filePath + "/buggy-version");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            inputStream.close();
            bufferedReader.close();
        } catch(Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        // long start = System.currentTimeMillis();

        if (args.length != 1) {
            System.out.println("Input Error!");
            return;
        }

        String fileListFilePath = args[0];
        // String fileListFilePath = "/home/renly/test/small_files_test.txt";
        // String fileListFilePath = "/Users/luyaoren/workplace/file_list.txt";

        // Create the table.
        DatabaseConnector connector = new DatabaseConnector();
        connector.open();
        connector.createTable();

        analyzer = Analyzer.getInstance();
        analyzer.open();
        work(fileListFilePath);
        analyzer.finish();

        // Delete the table.
        // connector.dropTable();
        connector.close();
        // System.out.println(1.0 * (System.currentTimeMillis() - start) / 1000);
    }
}

