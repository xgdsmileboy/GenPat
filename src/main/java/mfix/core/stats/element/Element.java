package mfix.core.stats.element;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Luyao Ren
 * @date: 2018/12/09
 */
public class Element {
    protected String _elementName;
    protected String _sourceFile;

    static final String KEYWORD_FOR_TABLE = "table";
    static final String VAR_TABLE_NAME = "VarTable";
    static final String METHOD_TABLE_NAME = "MethodTable";

    static final String DBKEY_ELEMENT_NAME = "elementName";
    static final String DBKEY_COLUMN_SOURCE_FILE = "sourceFile";

    static final String DBKEY_VAR_TYPE = "varType";
    static final String DBKEY_RET_TYPE = "retType";
    static final String DBKEY_OBJ_TYPE = "objType";
    static final String DBKEY_ARGS_TYPE = "argsType";
    static final String DBKEY_ARGS_NUMBER = "argsNumber";

    static final String KEYWORD_FOR_COUNT_COLUMN = "countColumn";
    static final String COUNT_OF_DISTINCT_SOURCEFILE = "distinct sourceFile";

    public Element(String name, String sourceFile) {
        _elementName = name;
        _sourceFile = sourceFile;
    }

    public Map<String, String> toInsertRow() {
        Map<String, String> KeyToValue = new HashMap<String, String>();

        KeyToValue.put(DBKEY_ELEMENT_NAME, _elementName);
        KeyToValue.put(DBKEY_COLUMN_SOURCE_FILE, _sourceFile);

        if (this instanceof VarElement) {
            VarElement var = (VarElement) this;
            KeyToValue.put(KEYWORD_FOR_TABLE, VAR_TABLE_NAME);
            if (var._varType != null) {
                KeyToValue.put(DBKEY_VAR_TYPE, var._varType);
            }
        } else if (this instanceof MethodElement) {
            MethodElement method = (MethodElement) this;
            KeyToValue.put(KEYWORD_FOR_TABLE, METHOD_TABLE_NAME);
            if (method._retType != null) {
                KeyToValue.put(DBKEY_RET_TYPE, method._retType);
            }
            if (method._objType != null) {
                KeyToValue.put(DBKEY_OBJ_TYPE, method._objType);
            }
            if (method._argsType != null) {
                KeyToValue.put(DBKEY_ARGS_TYPE, method._argsType);
            }
            if (method._argsNumber != null) {
                KeyToValue.put(DBKEY_ARGS_NUMBER, method._argsNumber.toString());
            }
        }

        return KeyToValue;
    }

    public Map<String, String> toQueryRow(ElementQueryType query) {
        Map<String, String> KeyToValue = toInsertRow();

        if (!query.getWithType()) {
            if (this instanceof VarElement) {
                KeyToValue.remove(DBKEY_VAR_TYPE);
            } else if (this instanceof MethodElement) {
                KeyToValue.remove(DBKEY_RET_TYPE);
                KeyToValue.remove(DBKEY_OBJ_TYPE);
                KeyToValue.remove(DBKEY_ARGS_TYPE);
                KeyToValue.remove(DBKEY_ARGS_NUMBER);
            }
        }

        if (query.getCountType() != ElementQueryType.CountType.IN_FILE) {
            KeyToValue.remove(DBKEY_COLUMN_SOURCE_FILE);
        }

        if (query.getCountType() == ElementQueryType.CountType.COUNT_FILES) {
            KeyToValue.put(KEYWORD_FOR_COUNT_COLUMN, COUNT_OF_DISTINCT_SOURCEFILE);
        }

        return KeyToValue;
    }
}
