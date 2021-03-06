/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.tools;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author: Jiajun
 * @date: 2019-03-12
 */
public class Timer {
    private long _start = 0;
    private long _timeout = 0;

    public Timer(int min){
        _timeout += TimeUnit.MINUTES.toMillis(min);
        System.out.println("TIMEOUT : " + _timeout + "ms");
    }

    public long whenStart() {
        return _start;
    }

    public String start(){
        _start = System.currentTimeMillis();
        return new Date(_start).toString();
    }

    public boolean timeout(){
        if((System.currentTimeMillis() - _start) > _timeout) {
            System.out.println("Timeout !");
            return true;
        }
        return false;
    }
}
