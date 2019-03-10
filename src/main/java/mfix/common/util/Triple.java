/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.common.util;

/**
 * @author: Jiajun
 * @date: 2019-03-10
 */
public class Triple<T1, T2, T>  {

    private T1 first;
    private T2 second;
    private T tag;

    public Triple() {
    }

    public Triple(T1 fst, T2 snd, T tag){
        this.first = fst;
        this.second = snd;
        this.tag = tag;
    }

    public T1 getFirst(){
        return this.first;
    }

    public T2 getSecond(){
        return this.second;
    }

    public T getTag() {
        return this.tag;
    }

    public void setFirst(T1 fst){
        this.first = fst;
    }

    public void setSecond(T2 snd){
        this.second = snd;
    }

    public void setTag(T tag) {
        this.tag = tag;
    }

}
