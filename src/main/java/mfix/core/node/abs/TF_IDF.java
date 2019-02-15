/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs;

import mfix.common.util.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-02-14
 */
public class TF_IDF implements CodeAbstraction {

    private static Map<String, Integer> _tokenMap;
    private final static int TOTAL_FILE_NUM = Constant.TOTAL_BUGGY_FILE_NUMBER;

    private double _threshold;
    private String _fileName;
    private Map<String, Integer> _tokenMapInFile;
    private double _tokenSizeInFile = 0;

    public TF_IDF(String fileName, double threshold) {
        if (_tokenMap == null) {
            try {
                loadTokenMap(Constant.TF_IDF_TOKENS);
            } catch (IOException e) {
                LevelLogger.fatal("Load token mapping ");
            }
        }
        _fileName = fileName;
        _threshold = threshold;
    }

    @Override
    public void lazyInit() {
        if (_tokenMapInFile == null) {
            _tokenMapInFile = new Hashtable<>();
            TokenCollector tokenCollector = new TokenCollector(_fileName);
            _tokenSizeInFile = tokenCollector.collect().tokenSize();
        }
    }

    private synchronized void loadTokenMap(String mapFile) throws IOException {
        File file = new File(mapFile);
        if (!file.exists()) {
            throw new IOException("Token mapping file does not exist : " + file.getAbsolutePath());
        }
        _tokenMap = new Hashtable<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String token = br.readLine();
        String number = br.readLine();
        Integer num;
        while(token != null && number != null) {
            try {
                num = (Integer.parseInt(number) >> 1); //TODO: the shift operation should be removed
                _tokenMap.put(token, num);
            } catch (Exception e) { }
            token = br.readLine();
            number = br.readLine();
        }
        br.close();
    }

    @Override
    public boolean shouldAbstract(Node node) {
        String token = node.toString();
        double numInFile = _tokenMapInFile.getOrDefault(token, 1);
        double numInDoc = _tokenMap.getOrDefault(token, 1) + 1;
        double tf = numInFile / _tokenSizeInFile;
        double idf = Math.log(TOTAL_FILE_NUM / numInDoc);
        // The smaller the value of TF_IDF, the more prevalent of the token
        return tf * idf < _threshold;
    }

    private class TokenCollector extends ASTVisitor {

        private String _file;
        private int _size = 0;

        public TokenCollector(String file) {
            _file = file;
        }

        public TokenCollector collect() {
            CompilationUnit unit = JavaFile.genASTFromFileWithType(_file);
            unit.accept(this);
            return this;
        }

        public int tokenSize() {
            return _size;
        }

        private void addToken(String token) {
            Integer num = _tokenMapInFile.get(token);
            if (num == null) {
                num = 0;
            }
            num ++;
            _size ++;
            _tokenMapInFile.put(token, num);
        }

        public boolean visit(SimpleName name) {
            addToken(name.getFullyQualifiedName());
            return true;
        }

        public boolean visit(NumberLiteral name) {
            addToken(name.getToken());
            return true;
        }

        public boolean visit(StringLiteral name) {
            addToken(name.getLiteralValue());
            return true;
        }

        public boolean visit(CharacterLiteral literal) {
            addToken(literal.getEscapedValue());
            return true;
        }

        public boolean visit(TypeLiteral literal) {
            addToken(literal.toString());
            return true;
        }

    }
}
