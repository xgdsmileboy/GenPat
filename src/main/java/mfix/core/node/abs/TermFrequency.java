/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.MType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-02-19
 */
public class TermFrequency implements CodeAbstraction {
    private static Map<String, Integer> _tokenMap;
    private static Map<String, Integer> _apiMap;
    private static Map<String, Integer> _typeMap;
    private final static int TOTAL_FILE_NUM = Constant.TOTAL_BUGGY_FILE_NUMBER;

    private double _threshold;

    static {
        try {
            _tokenMap = loadTokenMap(Constant.TF_IDF_TOKENS);
//            _apiMap = loadTokenMap(Constant.API_TOKENS);
//            _typeMap = loadTokenMap(Constant.TYPE_TOKENS);
        } catch (IOException e) {
            LevelLogger.fatal("Load token mapping ");
        }
    }

    public TermFrequency(double threshold) {
        _threshold = threshold;
    }

    @Override
    public TermFrequency lazyInit() {
        return this;
    }

    private static Map<String, Integer> loadTokenMap(String mapFile) throws IOException {
        File file = new File(mapFile);
        if (!file.exists()) {
            throw new IOException("Token mapping file does not exist : " + file.getAbsolutePath());
        }
        Map<String, Integer> map = new Hashtable<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8));
        String token = br.readLine();
        String number = br.readLine();
        Integer num;
        while (token != null && number != null) {
            try {
                num = Integer.parseInt(number);
                map.put(token, num);
            } catch (Exception e) {
            }
            token = br.readLine();
            number = br.readLine();
        }
        br.close();
        return map;
    }

    private boolean abstraction(String token, Map<String, Integer> map) {
        double numInDoc = _tokenMap.getOrDefault(token, 1);
        double frequency = numInDoc / TOTAL_FILE_NUM;
        return frequency < _threshold;
    }

    @Override
    public boolean shouldAbstract(String string) {
        return abstraction(string, _tokenMap);
    }

    @Override
    public boolean shouldAbstract(Node node) {
        String token;
        if (node.getNodeType() == Node.TYPE.TYPE) {
            token = NodeUtils.distillBasicType((MType) node);
        } else {
            token = node.toSrcString().toString();
        }
        return abstraction(token, _tokenMap);
//        double numInDoc = _tokenMap.getOrDefault(token, 1) + 1;
//        double frequency = numInDoc / TOTAL_FILE_NUM;
//        return frequency < _threshold;
    }

    @Override
    public boolean shouldAbstract(Node node, Category category) {
        if (node != null) {
            String token;
            if (node.getNodeType() == Node.TYPE.TYPE) {
                token = NodeUtils.distillBasicType((MType) node);
            } else {
                token = node.toSrcString().toString();
            }
            switch (category) {
                case API:
                    return abstraction(token, _apiMap);
                case TYPE:
                    return abstraction(token, _typeMap);
            }
        }
        return true;
    }
}
