/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.abs;

import mfix.common.util.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2019-02-19
 */
public class TermFrequency implements CodeAbstraction {
    private static Map<String, Integer> _tokenMap;
    private final static int TOTAL_FILE_NUM = Constant.TOTAL_BUGGY_FILE_NUMBER;

    private double _threshold;
    private double _tokenSizeInFile = 0;

    static {
        try {
            loadTokenMap(Constant.TF_IDF_TOKENS);
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

    private static void loadTokenMap(String mapFile) throws IOException {
        File file = new File(mapFile);
        if (!file.exists()) {
            throw new IOException("Token mapping file does not exist : " + file.getAbsolutePath());
        }
        _tokenMap = new Hashtable<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String token = br.readLine();
        String number = br.readLine();
        Integer num;
        while(token != null && number != null && _tokenMap.size() < 2000) {
            try {
                num = (Integer.parseInt(number) >> 1); //TODO: the shift operation should be removed
                _tokenMap.put(token, num);
            } catch (Exception e) {
            }
            token = br.readLine();
            number = br.readLine();
        }
        br.close();
    }

    @Override
    public boolean shouldAbstract(Node node) {
        String token = node.toSrcString().toString();
        double numInDoc = _tokenMap.getOrDefault(token, 1) + 1;
        double frequency = numInDoc / TOTAL_FILE_NUM;
        return frequency < _threshold;
    }

}
