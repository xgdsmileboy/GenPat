package mfix.core.stats;

import mfix.common.util.JavaFile;
import mfix.common.util.Utils;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.*;
import java.io.FileInputStream;

public class TokenCounter {
    private static mfix.core.stats.TokenCounter _instance;

    static private HashMap<String, Integer> counter;

    static private HashSet<String> currentFileToken;

    static final String SAVE_FILE = "/home/renly/tokenSerial/data";

    static final String LOAD_FILE_LIST = "/home/renly/test/FilFilesListLOC50.txt";
    // static final String LOAD_FILE_LIST = "/home/renly/test/FilFilesListLOC50FirstPart.txt";

    public static mfix.core.stats.TokenCounter getInstance() {
        if (_instance == null) {
            _instance = new mfix.core.stats.TokenCounter();
        }
        return _instance;
    }

    void runFile(String srcFile) {
        currentFileToken = new HashSet<String>();
        CompilationUnit _cunit = JavaFile.genASTFromFileWithType(srcFile, null);
        _cunit.accept(new mfix.core.stats.TokenCounter.Collector());
    }

    void runFolder(String folderPath) {
        File rootFile = new File(folderPath);
        if (!rootFile.exists()) {
            return;
        }
        List<File> files = JavaFile.ergodic(rootFile, new LinkedList<>());
        for (File file : files) {
            runFile(file.getAbsolutePath());
        }
    }

    static ArrayList<String> loadFileFolderList(String fileListFilePath) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileListFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        Integer cnt = 0;
        String filePath = null;

        ArrayList<String> fileList = new ArrayList<String>();

        while(true) {
            try {
                filePath = bufferedReader.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (filePath == null) {
                break;
            }

            fileList.add(filePath);
        }

        try {
            inputStream.close();
            bufferedReader.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return fileList;
    }

    public void addToken(String token) {
        if (!currentFileToken.contains(token)) {
            currentFileToken.add(token);
            int tmp = counter.getOrDefault(token, 0) + 1;
            counter.put(token, tmp + 1);
        }
    }

    private class Collector extends ASTVisitor {
        public boolean visit(SimpleName name) {
            // System.out.println("SName:" + name.getFullyQualifiedName());
            addToken(name.getFullyQualifiedName());
            return true;
        }

        public boolean visit(NumberLiteral name) {
            // System.out.println("Number:" + name.getToken());
            addToken(name.getToken());
            return true;
        }

        public boolean visit(StringLiteral name) {
            // System.out.println("String:" + name.getLiteralValue());
            // System.out.println("-------");
            addToken(name.getLiteralValue());
            return true;
        }
    }

    private void loadToFile(Integer batchNumber) {
        try {
            Utils.serialize(counter, SAVE_FILE + "-batch" + batchNumber.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void work() {
        ArrayList<String> fileFolderList = loadFileFolderList(LOAD_FILE_LIST);

        Integer cnt = 0;
        int batchNumber = 0;

        for (String fileFolder : fileFolderList) {
            if (cnt % 10000 == 0) {
                if (counter != null) {
                    loadToFile(batchNumber);
                }
                batchNumber += 1;
                counter = new HashMap<String, Integer>();
            }
            cnt += 1;

            System.out.println("run: " + cnt.toString() + " " + fileFolder);
            
            try {
                runFolder(fileFolder);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }

        loadToFile(batchNumber);

    }
}
