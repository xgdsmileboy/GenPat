package mfix.core.stats;

import mfix.common.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TokenGetter {
    static private HashMap<String, Integer> counter;

    static final String SAVE_FILE = "/home/renly/tokenSerial-tmp/data";

    public void readFromBatch() {
        HashMap<String, Integer> tmpCounter;

        counter = new HashMap<String, Integer>();

        for (Integer batchNumber = 1; true; batchNumber++) {
            String path = SAVE_FILE + "-batch" + batchNumber.toString();
            if (!(new File(path)).exists()) {
                break;
            }

            try {
                tmpCounter = (HashMap<String, Integer>) Utils.deserialize(path);
                for (Map.Entry<String, Integer> entry : tmpCounter.entrySet()) {
                    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                    // counter.put(entry.getKey(), counter.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
