/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.java;

import mfix.common.util.Utils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class JCompiler {

    private final char separator = File.pathSeparatorChar;
    private static JCompiler instance = null;

    public static JCompiler getInstance() {
        if (instance == null) {
            instance = new JCompiler();
        }
        return instance;
    }

    private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private MDiagnosticListener mDiagnosticListener = new MDiagnosticListener();

    private JCompiler() {
    }

    public static class MDiagnosticListener implements DiagnosticListener<JavaFileObject> {
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                System.out.println("Line Number->" + diagnostic.getLineNumber());
                System.out.println("code       ->" + diagnostic.getCode());
                System.out.println("Message    ->" + diagnostic.getMessage(Locale.ENGLISH));
                System.out.println("Source     ->" + diagnostic.getSource());
                System.out.println("");
            }
        }
    }

    /**
     * java File Object represents an in-memory java source file <br>
     * so there is no need to put the source file on hard disk
     **/
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private String contents = null;

        public InMemoryJavaFileObject(String className, String contents) throws Exception {
            super(URI.create("string:///" + className), Kind.SOURCE);
            this.contents = contents;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return contents;
        }
    }

    public static List<JavaFileObject> getAllJavaFileObjects(String path) {
        List<String> files = JavaFile.ergodic(path, new LinkedList<String>());
        List<JavaFileObject> javaFileObjects = new LinkedList<>();
        int length = path.length();
        try {
            for (String file : files) {
                String content = JavaFile.readFileToString(file);
                String className = file.substring(length);
                SimpleJavaFileObject simpleJavaFileObject = new InMemoryJavaFileObject(className, content);
                javaFileObjects.add(simpleJavaFileObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return javaFileObjects;
    }

    /**
     * compile your files by JavaCompiler
     */
    private boolean compile(Iterable<? extends JavaFileObject> files, Iterable<String> options) {
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(mDiagnosticListener, Locale.ENGLISH,
                null);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, mDiagnosticListener, options, null,
                files);
        return task.call();
    }

    private String buildClasspath(Subject subject) {
        List<String> classpath = subject.getClasspath();
        StringBuffer libs = new StringBuffer();
        if (classpath != null && !classpath.isEmpty()) {
            libs.append(Utils.join(separator, classpath));
            libs.append(classpath.get(0));
            for (int i = 1; i < classpath.size(); i++) {
                libs.append(separator + classpath.get(i));
            }
            libs.append(separator + subject.getHome() + subject.getSbin());
            libs.append(separator + System.getenv("classpath"));
        } else {
            libs.append(System.getenv("classpath"));
        }
        return libs.toString();
    }

    public boolean compile(Subject subject) {
        subject.checkAndInitBuildDir();
        String classpath = buildClasspath(subject);
        Iterable<? extends JavaFileObject> files = getAllJavaFileObjects(subject.getHome() + subject.getSsrc());
        Iterable<String> options = Arrays.asList("-d", subject.getHome() + subject.getSbin(), "-classpath", classpath
                , "-source", subject.getSourceLevelStr(), "-target", subject.getSourceLevelStr());
        if (!compile(files, options)) {
            return false;
        }
        files = getAllJavaFileObjects(subject.getHome() + subject.getTsrc());
        options = Arrays.asList("-d", subject.getHome() + subject.getTbin(), "-classpath", classpath, "-source",
                subject.getSourceLevelStr(), "-target", subject.getSourceLevelStr());
        if (!compile(files, options)) {
            return false;
        }
        return true;
    }

    public boolean compile(Subject subject, String className, String code2compile) {
        subject.checkAndInitBuildDir();
        String classpath = buildClasspath(subject);
        SimpleJavaFileObject simpleJavaFileObject = null;
        try {
            simpleJavaFileObject = new InMemoryJavaFileObject(className, code2compile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Iterable<? extends JavaFileObject> files = new ArrayList<>(Arrays.asList(simpleJavaFileObject));
        Iterable<String> options = Arrays.asList("-d", subject.getHome() + subject.getSbin(), "-classpath", classpath
                , "-source", subject.getSourceLevelStr(), "-target", subject.getSourceLevelStr());
        if (!compile(files, options)) {
            return false;
        }
        return true;
    }
}
