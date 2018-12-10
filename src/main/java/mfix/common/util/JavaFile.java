/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/9/19
 */
public class JavaFile {

    private final static String __name__ = "@JavaFile ";

    /**
     * @param icu
     * @return
     * @see #genASTFromICU(ICompilationUnit icu, String jversion, int astLevel)
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu) {
        return genASTFromICU(icu, JavaCore.VERSION_1_7);
    }

    /**
     * @param icu
     * @param jversion
     * @return
     * @see #genASTFromICU(ICompilationUnit icu, String jversion, int astLevel)
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu, String jversion) {
        return genASTFromICU(icu, jversion, AST.JLS8);
    }

    /**
     * @param icu
     * @param astLevel
     * @return
     * @see #genASTFromICU(ICompilationUnit icu, String jversion, int astLevel)
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu, int astLevel) {
        return genASTFromICU(icu, JavaCore.VERSION_1_7, astLevel);
    }

    /**
     * create AST from {@code ICompilationUnit}
     *
     * @param icu
     *            : ICompilationUnit for creating AST
     * @param jversion
     *            : the version of JAVA, can be one of the following:
     *            <ul>
     *            <li>{@code JavaCore.VERSION_1_1}</li>
     *            <li>{@code JavaCore.VERSION_1_2}</li>
     *            <li>{@code JavaCore.VERSION_1_3}</li>
     *            <li>{@code JavaCore.VERSION_1_4}</li>
     *            <li>{@code JavaCore.VERSION_1_5}</li>
     *            <li>{@code JavaCore.VERSION_1_6}</li>
     *            <li>{@code JavaCore.VERSION_1_7}</li>
     *            <li>{@code JavaCore.VERSION_1_8}</li>
     *            </ul>
     * @param astLevel
     *            : AST level of created AST, can be one of the following:
     *            <ul>
     *            <li>{@code AST.JLS2}</li>
     *            <li>{@code AST.JLS3}</li>
     *            <li>{@code AST.JLS4}</li>
     *            <li>{@code AST.JLS8}</li>
     *            </ul>
     * @return : AST
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu, String jversion, int astLevel) {
        ASTParser astParser = ASTParser.newParser(astLevel);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(jversion, options);
        astParser.setCompilerOptions((Map<String, String>) options);
        astParser.setSource(icu);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setResolveBindings(true);
        return (CompilationUnit) astParser.createAST(null);
    }

    /**
     * @param fileName
     * @return
     * @see #genASTFromSource(String icu, String jversion, int astLevel, int type)
     */
    public static CompilationUnit genAST(String fileName) {
        return (CompilationUnit) genASTFromSource(readFileToString(fileName), JavaCore.VERSION_1_7, AST.JLS8,
                ASTParser.K_COMPILATION_UNIT);
    }

    /**
     * @param fileName
     * @param jversion
     * @return
     * @see #genASTFromSource(String icu, String jversion, int astLevel, int type)
     */
    public static CompilationUnit genAST(String fileName, String jversion) {
        return (CompilationUnit) genASTFromSource(readFileToString(fileName), jversion, AST.JLS8,
                ASTParser.K_COMPILATION_UNIT);
    }

    /**
     * @param fileName
     * @param astLevel
     * @return
     * @see #genASTFromSource(String icu, String jversion, int astLevel, int type)
     */
    public static CompilationUnit genAST(String fileName, int astLevel) {
        return (CompilationUnit) genASTFromSource(readFileToString(fileName), JavaCore.VERSION_1_7, astLevel,
                ASTParser.K_COMPILATION_UNIT);
    }

    /**
     * create AST from source code
     *
     * @param icu
     *            : source code with {@code String} format
     * @param jversion
     *            : the version of JAVA, can be one of the following:
     *            <ul>
     *            <li>{@code JavaCore.VERSION_1_1}</li>
     *            <li>{@code JavaCore.VERSION_1_2}</li>
     *            <li>{@code JavaCore.VERSION_1_3}</li>
     *            <li>{@code JavaCore.VERSION_1_4}</li>
     *            <li>{@code JavaCore.VERSION_1_5}</li>
     *            <li>{@code JavaCore.VERSION_1_6}</li>
     *            <li>{@code JavaCore.VERSION_1_7}</li>
     *            <li>{@code JavaCore.VERSION_1_8}</li>
     *            </ul>
     * @param astLevel
     *            : AST level of created AST, can be one of the following:
     *            <ul>
     *            <li>{@code AST.JLS2}</li>
     *            <li>{@code AST.JLS3}</li>
     *            <li>{@code AST.JLS4}</li>
     *            <li>{@code AST.JLS8}</li>
     *            </ul>
     * @param type
     *            : the type of AST node, can be one of the following:
     *            <ul>
     *            <li>{@code ASTParser.K_CLASS_BODY_DECLARATIONS}</li>
     *            <li>{@code ASTParser.K_COMPILATION_UNIT}</li>
     *            <li>{@code ASTParser.K_EXPRESSION}</li>
     *            <li>{@code ASTParser.K_STATEMENTS}</li>
     *            </ul>
     * @return : AST
     */
    public static ASTNode genASTFromSource(String icu, String jversion, int astLevel, int type) {
        ASTParser astParser = ASTParser.newParser(astLevel);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(jversion, options);
        astParser.setCompilerOptions((Map<String, String>) options);
        astParser.setSource(icu.toCharArray());
        astParser.setKind(type);
        astParser.setResolveBindings(true);
        return astParser.createAST(null);
    }

    /**
     * @see ASTNode genASTFromSourceWithType(String icu, int type, String filePath, String srcPath)
     * @param srcFile
     * @return
     */
    public static CompilationUnit genASTFromFileWithType(String srcFile) {
        return (CompilationUnit) genASTFromSourceWithType(readFileToString(srcFile), ASTParser.K_COMPILATION_UNIT,
                srcFile, null);
    }

    /**
     * @see ASTNode genASTFromSourceWithType(String icu, int type, String filePath, String srcPath)
     * @param srcFile
     * @return
     */
    public static CompilationUnit genASTFromFileWithType(File srcFile) {
        return (CompilationUnit) genASTFromSourceWithType(readFileToString(srcFile), ASTParser.K_COMPILATION_UNIT,
                srcFile.getAbsolutePath(), null);
    }

    /**
     * create {@code CompilationUnit} from given file {@code srcFile}
     * @param srcFile : absolute path of java source code
     * @param srcPath : base path of source code
     * @return a compilation unit
     */
    public static CompilationUnit genASTFromFileWithType(String srcFile, String srcPath) {
        return (CompilationUnit) genASTFromSourceWithType(readFileToString(srcFile), ASTParser.K_COMPILATION_UNIT, srcFile, srcPath);
    }

    /**
     * @param icu
     * @param type
     * @param filePath
     * @param srcPath
     * @return
     * @see #genASTFromSourceWithType(String icu, String jversion, int astLevel, int
     *      type, String filePath, String srcPath)
     */
    public static ASTNode genASTFromSourceWithType(String icu, int type, String filePath, String srcPath) {
        return genASTFromSourceWithType(icu, JavaCore.VERSION_1_7, AST.JLS8, type, filePath, srcPath);
    }

    /**
     * @param icu
     * @param jversion
     * @param type
     * @param filePath
     * @param srcPath
     * @return
     * @see #genASTFromSourceWithType(String icu, String jversion, int astLevel, int
     *      type, String filePath, String srcPath)
     */
    public static ASTNode genASTFromSourceWithType(String icu, String jversion, int type, String filePath, String srcPath) {
        return genASTFromSourceWithType(icu, jversion, AST.JLS8, type, filePath, srcPath);
    }

    /**
     * @param icu
     * @param astLevel
     * @param type
     * @param filePath
     * @param srcPath
     * @return
     * @see #genASTFromSourceWithType(String icu, String jversion, int astLevel, int
     *      type, String filePath, String srcPath)
     */
    public static ASTNode genASTFromSourceWithType(String icu, int astLevel, int type, String filePath, String srcPath) {
        return genASTFromSourceWithType(icu, JavaCore.VERSION_1_7, astLevel, type, filePath, srcPath);
    }

    /**
     * @param icu
     *            : source code with {@code String} format
     * @param jversion
     *            : the version of JAVA, can be one of the following:
     *            <ul>
     *            <li>{@code JavaCore.VERSION_1_1}</li>
     *            <li>{@code JavaCore.VERSION_1_2}</li>
     *            <li>{@code JavaCore.VERSION_1_3}</li>
     *            <li>{@code JavaCore.VERSION_1_4}</li>
     *            <li>{@code JavaCore.VERSION_1_5}</li>
     *            <li>{@code JavaCore.VERSION_1_6}</li>
     *            <li>{@code JavaCore.VERSION_1_7}</li>
     *            <li>{@code JavaCore.VERSION_1_8}</li>
     *            </ul>
     * @param astLevel
     *            : AST level of created AST, can be one of the following:
     *            <ul>
     *            <li>{@code AST.JLS2}</li>
     *            <li>{@code AST.JLS3}</li>
     *            <li>{@code AST.JLS4}</li>
     *            <li>{@code AST.JLS8}</li>
     *            </ul>
     * @param type
     *            : the type of AST node, can be one of the following:
     *            <ul>
     *            <li>{@code ASTParser.K_CLASS_BODY_DECLARATIONS}</li>
     *            <li>{@code ASTParser.K_COMPILATION_UNIT}</li>
     *            <li>{@code ASTParser.K_EXPRESSION}</li>
     *            <li>{@code ASTParser.K_STATEMENTS}</li>
     *            </ul>
     * @param filePath
     *            : source file absolute path
     * @param srcPath
     *            : the base of source file
     * @return AST
     */
    public static ASTNode genASTFromSourceWithType(String icu, String jversion, int astLevel, int type, String filePath, String srcPath) {
        if(icu == null || icu.isEmpty()) return null;
        ASTParser astParser = ASTParser.newParser(astLevel);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(jversion, options);
        astParser.setCompilerOptions((Map<String, String>) options);
        astParser.setSource(icu.toCharArray());
        astParser.setKind(type);
        astParser.setResolveBindings(true);
        srcPath = srcPath == null ? "" : srcPath;
        astParser.setEnvironment(getClassPath(), new String[] {srcPath}, null, true);
        astParser.setUnitName(filePath);
        astParser.setBindingsRecovery(true);
        try{
            return astParser.createAST(null);
        }catch(Exception e) {
            return null;
        }
    }

    private static String[] getClassPath() {
        String property = System.getProperty("java.class.path", ".");
        return property.split(File.pathSeparator);
    }

    /**
     * write {@code string} into file with mode as "not append"
     *
     * @param filePath
     *            : path of file
     * @param string
     *            : message
     * @return
     */
    public static boolean writeStringToFile(String filePath, String string) {
        return writeStringToFile(filePath, string, false);
    }

    /**
     * write {@code string} to file with mode as "not append"
     *
     * @param file
     *            : file of type {@code File}
     * @param string
     *            : message
     * @return
     */
    public static boolean writeStringToFile(File file, String string) {
        return writeStringToFile(file, string, false);
    }

    /**
     * @param filePath
     * @param string
     * @param append
     * @return
     * @see #writeStringToFile(File, String, boolean)
     */
    public static boolean writeStringToFile(String filePath, String string, boolean append) {
        if (filePath == null) {
            LevelLogger.error(__name__ + "#writeStringToFile Illegal file path : null.");
            return false;
        }
        File file = new File(filePath);
        return writeStringToFile(file, string, append);
    }

    /**
     * write {@code string} into file with specific mode
     *
     * @param file
     *            : file of type {@code File}
     * @param string
     *            : message
     * @param append
     *            : writing mode
     * @return
     */
    public static boolean writeStringToFile(File file, String string, boolean append) {
        if (file == null) {
            LevelLogger.error(__name__ + "#writeStringToFile Illegal arguments (File) : null.");
            return false;
        }
        if (string == null) {
            LevelLogger.error(__name__ + "#writeStringToFile Illegal arguments (string) : null.");
            return false;
        }
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                LevelLogger.error(__name__ + "#writeStringToFile Create new file failed : " + file.getAbsolutePath());
                return false;
            }
        }
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            bufferedWriter.write(string);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * read string from file
     *
     * @param filePath
     *            : file path
     * @return : string in the file
     */
    public static String readFileToString(String filePath) {
        if (filePath == null) {
            LevelLogger.error(__name__ + "#readFileToString Illegal input file path : null.");
            return new String();
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LevelLogger.error(__name__ + "#readFileToString Illegal input file path : " + filePath);
            return new String();
        }
        return readFileToString(file);
    }

    /**
     * read string from file
     *
     * @param file
     *            : file of type {@code File}
     * @return : string in the file
     */
    public static String readFileToString(File file) {
        if (file == null) {
            LevelLogger.error(__name__ + "#readFileToString Illegal input file : null.");
            return new String();
        }
        StringBuffer stringBuffer = new StringBuffer();
        InputStream in = null;
        InputStreamReader inputStreamReader = null;
        try {
            in = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(in, "UTF-8");
            char[] ch = new char[1024];
            int readCount = 0;
            while ((readCount = inputStreamReader.read(ch)) != -1) {
                stringBuffer.append(ch, 0, readCount);
            }
            inputStreamReader.close();
            in.close();

        } catch (Exception e) {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e1) {
                    return new String();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    return new String();
                }
            }
        }
        return stringBuffer.toString();
    }

    /**
     * read string from file
     *
     * @param filePath
     *            : file path
     * @return : list of string in the file
     */
    public static List<String> readFileToStringList(String filePath) {
        if (filePath == null) {
            LevelLogger.error(__name__ + "#readFileToStringList Illegal input file path : null.");
            return new ArrayList<String>();
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LevelLogger.error(__name__ + "#readFileToString Illegal input file path : " + filePath);
            return new ArrayList<String>();
        }
        return readFileToStringList(file);
    }

    /**
     * read string from file
     *
     * @param file
     *            : file of type {@code File}
     * @return : list of string in the file
     */
    public static List<String> readFileToStringList(File file) {
        List<String> results = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while((line = reader.readLine()) != null) {
                results.add(line);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            LevelLogger.error(__name__ + "#readFileToStringList file not found : " + file.getAbsolutePath());
        } catch (IOException e) {
            LevelLogger.error(__name__ + "#readFileToStringList IO exception : " + file.getAbsolutePath(), e);
        }
        return results;
    }

    /**
     * @see List<File> ergodic(File file, List<File> fileList, String srcFilePostfix)
     */
    public static List<File> ergodic(File file, List<File> fileList) {
        return ergodic(file, fileList, ".java");
    }

    /**
     * iteratively search files with the root as {@code file}
     *
     * @param file
     *            : root file of type {@code File}
     * @param fileList
     *            : list to save all the files
     * @param srcFilePostfix
     *            : postfix of file names
     * @return : a list of all files
     */
    public static List<File> ergodic(File file, List<File> fileList, String srcFilePostfix) {
        if (file == null) {
            LevelLogger.error(__name__ + "#ergodic Illegal input file : null.");
            return fileList;
        }
        File[] files = file.listFiles();
        if (files == null)
            return fileList;
        for (File f : files) {
            if (f.isDirectory()) {
                ergodic(f, fileList, srcFilePostfix);
            } else if (f.getName().endsWith(srcFilePostfix))
                fileList.add(f);
        }
        return fileList;
    }

    public static List<File> ergodic(File file, List<File> fileList, Set<String> ignoreSet, String srcFilePostfix) {
        if (file == null) {
            LevelLogger.error(__name__ + "#ergodic Illegal input file : null.");
            return fileList;
        }
        File[] files = file.listFiles();
        if (files == null)
            return fileList;
        for (File f : files) {
            if(ignoreSet.contains(f.getName())) continue;
            if (f.isDirectory()) {
                ergodic(f, fileList, ignoreSet, srcFilePostfix);
            } else if (f.getName().endsWith(srcFilePostfix))
                fileList.add(f);
        }
        return fileList;
    }

    /**
     * @see List<String> ergodic(String directory, List<String> fileList, String srcFilePostfix)
     */
    public static List<String> ergodic(String directory, List<String> fileList) {
        return ergodic(directory, fileList, ".java");
    }

    /**
     * iteratively search the file in the given {@code directory}
     *
     * @param directory
     *            : root directory
     * @param fileList
     *            : list of file
     * @return : a list of file
     */
    public static List<String> ergodic(String directory, List<String> fileList, String srcFilePostfix) {
        if (directory == null) {
            LevelLogger.error(__name__ + "#ergodic Illegal input file : null.");
            return fileList;
        }
        File file = new File(directory);
        File[] files = file.listFiles();
        if (files == null)
            return fileList;
        for (File f : files) {
            if (f.isDirectory()) {
                ergodic(f.getAbsolutePath(), fileList, srcFilePostfix);
            } else if (f.getName().endsWith(srcFilePostfix))
                fileList.add(f.getAbsolutePath());
        }
        return fileList;
    }

    /**
     * replace source code between the given range
     * @param fileName : file name to write the replaced source into
     * @param source : original source code lines
     * @param startLine : start line number to be replaced
     * @param endLine : end lin number to be replaced
     * @param replace : replace string
     * @throws IOException
     */
    public static void sourceReplace(String fileName, List<String> source, int startLine, int endLine, String replace) throws IOException{
        File file = new File(fileName);
        if(!file.exists()){
            System.out.println("File : " + fileName + " does not exist!");
            return;
        }
        boolean flag = false;
        StringBuffer stringBuffer = new StringBuffer();
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
        for(int i = 1; i < source.size(); i++){
            if(i == startLine){
                String origin = source.get(i).replace(" ", "");
                if(origin.startsWith("}else")){
                    bw.write("} else ");
                }
                bw.write("// start of generated patch" + Constant.NEW_LINE);
                bw.write(replace);
                bw.write("// end of generated patch" + Constant.NEW_LINE);
                stringBuffer.append("// start of original code" + Constant.NEW_LINE);
                stringBuffer.append("//" + source.get(i) + Constant.NEW_LINE);
                flag = true;
            } else if(startLine < i && i <= endLine){
                stringBuffer.append("//" + source.get(i) + Constant.NEW_LINE);
                continue;
            } else {
                if(flag){
                    bw.write(stringBuffer.toString());
                    bw.write("// end of original code" + Constant.NEW_LINE);
                    stringBuffer = null;
                    flag = false;
                }
                bw.write(source.get(i) + Constant.NEW_LINE);
            }
        }
        bw.close();
    }
}
