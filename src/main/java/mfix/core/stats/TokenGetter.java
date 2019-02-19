package mfix.core.stats;

import mfix.common.util.Utils;

import java.io.*;
import java.util.*;

public class TokenGetter {
    static private HashMap<String, Integer> counter;

    static final String SAVE_FILE = "/home/renly/tokenSerial/data";
    //static final String SAVE_FILE = "/Users/luyaoren/workplace/tokenSerial/data";

    static final String TEMP_SERIALIZE_FILE = "/home/renly/tokenSerial/map_for_all";
    //static final String TEMP_SERIALIZE_FILE = "/Users/luyaoren/workplace/tokenSerial/map_for_all";

    static final String IMPORT_FILE = "/home/renly/tokenSerial/data/all.txt";
    //static final String IMPORT_FILE = "/Users/luyaoren/workplace/tokenSerial/all.txt";

    static public void readFromBatch() {
        if ((new File(TEMP_SERIALIZE_FILE)).exists()) {
            System.out.println("Already exist!");
            try {
                counter = (HashMap<String, Integer>) Utils.deserialize(TEMP_SERIALIZE_FILE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("First load!");
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
                        int value = entry.getValue();
                        counter.put(entry.getKey(), counter.getOrDefault(entry.getKey(), 0) + value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            try {
                Utils.serialize(counter, TEMP_SERIALIZE_FILE);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    static public void importAsTextFile() throws Exception{
        if (counter == null) {
            throw new Exception("Please first load!");
        }

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

        try
        {
            FileOutputStream fos = new FileOutputStream(IMPORT_FILE);
            Writer out = new OutputStreamWriter(fos, "UTF-8");
            int cnt = 0;
            for(Map.Entry<String, Integer> entry: list) {
                out.write(entry.getKey());
                out.write(entry.getValue());
            }
            System.out.println("Finish!");
            out.close();
            fos.close();
        } catch (IOException e)
        {
            System.out.println("Write Error!");
            e.printStackTrace();
        }
    }
}
