/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.vector;

/**
 * @author: Jiajun
 * @date: 2019-02-11
 */
public class Vector {

    private long _vec;
    private final int size = VIndex.LENGTH;

    public Vector() {
        _vec = 0L;
    }

    public void set(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Illegal argument :" + index);
        }
        _vec |= (1L << index);
    }

    public void clear(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Illegal argument :" + index);
        }
        _vec &= ~(1L << index);
    }

    public void reset() {
        _vec = 0L;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Illegal argument :" + index);
        }
        return (int) ((_vec >> index) & 1L);
    }

    public void or(Vector vector) {
        _vec |= vector._vec;
    }

    public void and(Vector vector) {
        _vec &= vector._vec;
    }

    public void xor(Vector vector) {
        _vec ^= vector._vec;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Vector) {
            Vector vector = (Vector) obj;
            return (_vec ^ vector._vec) == 0;
        }
        return false;
    }

    @Override
    public String toString() {
        String string = Long.toBinaryString(_vec);
        int complement = size - string.length();
        if (complement == 0) return string;
        StringBuffer buffer = new StringBuffer();
        while(complement-- >= 0) {
            buffer.append('0');
        }
        return buffer.append(string).toString();
    }
}
