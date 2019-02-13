/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;


import mfix.common.util.Utils;
import mfix.core.stats.TokenCounter;
import mfix.core.stats.TokenGetter;

import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
//        TokenCounter counter = new TokenCounter();
//        counter.work();
        TokenGetter getter = new TokenGetter();
        TokenGetter.readFromBatch();

        /*
        try {
            HashMap<String, Integer> m = (HashMap<String, Integer>)Utils.deserialize("/home/renly/tokenSerial/data-batch1");
            System.out.println(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }
}

