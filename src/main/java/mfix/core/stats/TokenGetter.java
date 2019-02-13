package mfix.core.stats;

import mfix.common.util.Utils;

import java.io.File;
import java.util.*;

public class TokenGetter {
    static private HashMap<String, Integer> counter;

    static final String SAVE_FILE = "/home/renly/tokenSerial-tmp/data";

    static public void readFromBatch() {
        HashMap<String, Integer> tmpCounter;

        counter = new HashMap<String, Integer>();

        for (Integer batchNumber = 1; batchNumber <= 3; batchNumber++) {
            String path = SAVE_FILE + "-batch" + batchNumber.toString();
            if (!(new File(path)).exists()) {
                break;
            }

            try {
                tmpCounter = (HashMap<String, Integer>) Utils.deserialize(path);
                for (Map.Entry<String, Integer> entry : tmpCounter.entrySet()) {
                    // System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                    counter.put(entry.getKey(), counter.getOrDefault(entry.getKey(), 0) + entry.getValue());

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
        List<Map.Entry<String, Integer>> list = new ArrayList<>();

        for(Map.Entry<String, Integer> entry : counter.entrySet()){
            list.add(entry);
        }

        list.sort(new Comparator<Map.Entry<String, Integer>>(){
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue()-o1.getValue();
            }
        });
        */

        /*
        int cnt = 0;
        for(Map.Entry<String, Integer> entry: list){
            System.out.println(entry);

            cnt += 1;
            if (cnt >= 50) {
                break;
            }
        }
        */
    }
}
