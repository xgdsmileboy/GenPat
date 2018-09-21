/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

import mfix.common.java.D4jSubject;
import mfix.common.java.JCompiler;
import mfix.common.java.Subject;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class JCompilerTest {

    private final String base = Constant.RES_DIR + Constant.SEP + "forTest";

    @Test
    public void test_compile_lang() {
        Subject subject = new D4jSubject(base, "lang", 1);
        JCompiler compiler = JCompiler.getInstance();
        Assert.assertTrue(compiler.compile(subject));
    }

    @Test
    public void test_compile_math() {
        Subject subject = new D4jSubject(base, "math", 3);
        JCompiler compiler = JCompiler.getInstance();
        Assert.assertTrue(compiler.compile(subject));
    }

    @Test
    public void test_compile_chart() {
        Subject subject = new D4jSubject(base, "chart", 1);
        JCompiler compiler = JCompiler.getInstance();
        Assert.assertTrue(compiler.compile(subject));
    }

    @Test
    public void test_compile_time() {
        Subject subject = new D4jSubject(base, "time", 1);
        JCompiler compiler = JCompiler.getInstance();
        Assert.assertTrue(compiler.compile(subject));
    }

    @Test
    public void test_compile_closure() {
        // TODO : need to compile the whole project first from command line,
        // since it needs to compile the .proto files first.
        Subject subject = new D4jSubject(base, "closure", 1);
        JCompiler compiler = JCompiler.getInstance();
        Assert.assertTrue(compiler.compile(subject));
    }

    @Test
    public void test_compile_file() {
        String relJavaFile = "/lang/lang_1_buggy/src/main/java/org/apache/commons/lang3/ArrayUtils.java";
        String content = JavaFile
                .readFileToString(base + relJavaFile);
        Subject subject = new D4jSubject(base, "lang", 1);
        JCompiler compiler = JCompiler.getInstance();
        Assert.assertTrue(compiler.compile(subject, "org/apache/commons/lang3/ArrayUtils.java", content));
    }


}
