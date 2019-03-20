/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.conf;

import mfix.common.conf.Constant;
import mfix.common.java.Subject;
import mfix.common.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2019-03-20
 */
public class Configure {

    private final static Map<String, Pair<Integer, Set<Integer>>> projInfo;

    static {
        projInfo = getProjectInfoFromJSon();
    }

    public static boolean shouldPurify(Subject subject) {
        Pair<Integer, Set<Integer>> pair = projInfo.get(subject.getName());
        if (pair == null) return false;
        return !pair.getSecond().contains(subject.getId());
    }

    private static Map<String, Pair<Integer, Set<Integer>>> getProjectInfoFromJSon() {
        Map<String, Pair<Integer, Set<Integer>>> projectInfo = new HashMap<>();
        try {
            // read the json file
            FileReader reader = new FileReader(Constant.D4J_PROJ_JSON_FILE);
            JSONParser jsonParser = new JSONParser();
            JSONArray jsonArray = (JSONArray) jsonParser.parse(reader);

            for(int i = 0; i < jsonArray.size(); i++){
                JSONObject project = (JSONObject)jsonArray.get(i);
                String name = (String) project.get("name");
                JSONObject info = (JSONObject) project.get("info");
                Long number = (Long)info.get("number");
                String idString = (String) info.get("single");

                Set<Integer> bugId = new HashSet<>();
                String[] ids = idString.split(",");
                for(int j = 0; j < ids.length; j++){
                    int dash = ids[j].indexOf("-");
                    if(dash == -1){
                        bugId.add(Integer.parseInt(ids[j]));
                    } else {
                        int start = Integer.parseInt(ids[j].substring(0, dash));
                        int end = Integer.parseInt(ids[j].substring(dash + 1));
                        for(; start <= end; start ++){
                            bugId.add(start);
                        }
                    }
                }
                projectInfo.put(name, new Pair<Integer, Set<Integer>>(number.intValue(), bugId));
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return projectInfo;
    }
}
