/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.node.modify;

import mfix.core.node.ast.MatchLevel;
import mfix.core.node.diff.TextDiff;

/**
 * @author: Jiajun
 * @date: 2019-04-30
 */
public class Adaptee {

    public enum CHANGE{
        INSERT,
        WRAP,
        UPDATE,
        DELETE,
    }

    private int _upd;
    private int _ins;
    private int _del;
    private int _mod;
    private int _all;
    private CHANGE _change = CHANGE.UPDATE;
    private String _adaptedCode;
    private MatchLevel _level;
    private String _patternName;
    private TextDiff _diff;

    public Adaptee(int modnum) {
        this(modnum, 0, 0, 0);
    }

    public Adaptee(int modnum, int upd, int ins, int del) {
        _upd = upd;
        _ins = ins;
        _del = del;
        _mod = modnum;
        _all = upd + ins + del;
    }

    public void setAdaptedCode(String code) {
        _adaptedCode = code;
    }

    public String getAdaptedCode() {
        return _adaptedCode;
    }

    public void setDiff(TextDiff diff) {
        _diff = diff;
    }

    public TextDiff getDiff() {
        return _diff;
    }

    public void setMatchLevel(MatchLevel level) {
        _level = level;
    }

    public MatchLevel getMatchLevel() {
        return _level;
    }

    public void setPatternName(String pattern) {
        _patternName = pattern;
    }

    public String getPatternName() {
        return _patternName;
    }

    public void setChange(CHANGE change) {
        _change = change;
    }

    public void inc() {
        add(1);
    }

    public void add(int size) {
        add(size, _change);
    }

    public void add(int size, CHANGE change) {
        switch (change) {
            case UPDATE:
            case WRAP:
                _upd += size;
                break;
            case DELETE:
                _del += size;
                break;
            case INSERT:
                _ins += size;
                break;
        }
        _all += size;
    }

    public int getModificationNumber() {
        return _mod;
    }

    public int getChangeNumber() {
        return _all;
    }

    public int getAll() {
        return _all;
    }

    public int negIns() {
        return - _ins;
    }

    public int negUpd() {
        return - _upd;
    }

    public int geDel() {
        return - _del;
    }

    public int getIns() {
        return _ins;
    }

    public int getUpd() {
        return _upd;
    }

    public int getDel() {
        return _del;
    }

    @Override
    public String toString() {
        return "<" + _mod + ", " + _all + ", " + _upd + ", " + _ins + ", " + _del + ">";
    }
}
