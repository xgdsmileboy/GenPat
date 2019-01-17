/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;

import mfix.common.util.JavaFile;
import mfix.core.node.PatternExtractor;
import mfix.core.node.ast.Node;
import mfix.core.node.modify.Modification;
import mfix.core.pattern.Pattern;
import mfix.core.pattern.relation.Relation;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class Main {

    static void savePatternToFile(String outputFile, Set<Node> obj){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile));
            oos.writeObject(obj);

            System.out.println("save for" + outputFile);

            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Set<Node> loadPatternFromFile(String inputFile) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFile));
            Set<Node> person=(Set<Node>)ois.readObject();
            return person;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void printPattern(Set<Node> pattern) {
        System.out.println("-------start---------");
        for (Node node : pattern) {
            System.out.println(node.getAllModifications(new HashSet<Modification>()));
        }

        System.out.println("--------------------");
        System.out.println("-------end----------");
    }


    static void timeoutMethod(int timeout, String srcFile, String tarFile, String saveFile) {
        FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return extract(srcFile, tarFile, saveFile);
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();


        executorService.execute(futureTask);

        try {
            boolean result = futureTask.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            System.out.println("Timeout or other error for" + srcFile);
            futureTask.cancel(true);
        }

        executorService.shutdownNow();
    }

    static boolean extract(String srcFile, String tarFile, String saveFile) {
        Set<Node> patterns = PatternExtractor.extractPattern(srcFile, tarFile);


        // System.out.println("-----before save-------");
        // printPattern(patterns);

        savePatternToFile(saveFile, patterns);

        // Set<Node> newp = loadPatternFromFile(saveFile);
        // System.out.println("-----after save-------");
        // printPattern(newp);

        return true;
    }

    static void runFolder(String folderPath) throws Exception {
        File buggyFolder = new File(folderPath + "/buggy-version");
        File fixedFolder = new File(folderPath + "/fixed-version");

        if ((!buggyFolder.exists()) || (!fixedFolder.exists())) {
            throw new Exception("Files not exist!");
        }

        List<File> buggyFiles = JavaFile.ergodic(buggyFolder, new LinkedList<>());
        List<File> fixedFiles = JavaFile.ergodic(fixedFolder, new LinkedList<>());
        HashMap<String, File> buggyFilesMap = new HashMap<String, File>();

        for (File file : buggyFiles) {
            buggyFilesMap.put(file.getName(), file);
        }

        for (File fixedFile : fixedFiles) {
            File buggyFile = buggyFilesMap.getOrDefault(fixedFile.getName(), null);

            File saveFolder = new File(folderPath + "/pattern-v1-serial");
            if (!saveFolder.exists()) {
                saveFolder.mkdirs();
            }

            String saveFile = saveFolder.getAbsolutePath() + "/" + fixedFile.getName() + ".pattern";

            if (buggyFile != null) {

                try {
                    timeoutMethod(60 * 5, buggyFile.getAbsolutePath(), fixedFile.getAbsolutePath(), saveFile);
                } catch (Exception e) {
                    // TODO: handle exception
                }

                // extract(buggyFile.getAbsolutePath(), fixedFile.getAbsolutePath(), saveFile);
            }
        }

        //System.out.println(buggyFiles.toString());
        //System.out.println(fixedFiles.toString());
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
                runFolder(filePath);
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

        /*
        if (args.length != 1) {
            System.out.println("Input Error!");
            return;
        }
        String fileListFilePath = args[0];
        */

        // String fileListFilePath = "/home/renly/test/small_files_test.txt";
        String fileListFilePath = "/Users/luyaoren/workplace/file_list.txt";

        work(fileListFilePath);
    }
}

