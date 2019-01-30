package mfix.core.stats.element;

import mfix.common.util.LevelLogger;

import java.sql.*;
import java.util.Map;

/**
 * @author: Luyao Ren
 * @date: 2018/12/11
 */
public class DatabaseConnector {
    static final String DB_URL = "jdbc:mysql://localhost:3306/MINEFIX";
    static final String DB_URL_FOR_TEST = "jdbc:mysql://localhost:3306/MINEFIX_test";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "thisispassword";

    static final String DB_SELECT_FOR_COUNT_CODE = "SELECT count(%s) FROM (%s) WHERE (%s)";
    static final String DB_INSERT_CODE = "INSERT INTO %s (%s) VALUES (%s)";
    static final String DB_TRY_DROP_TABLE = "DROP TABLE IF EXISTS %s";
    static final String DB_TRY_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %s(%s)";
    static final String VAR_TABLE_DEFINE = "elementName varchar(255),\n sourceFile TEXT,\n varType varchar(255)";
    static final String METHOD_TABLE_DEFINE = "elementName varchar(255),\n sourceFile TEXT,\n retType varchar(255),\n objType varchar(255),\n argsType TEXT,\n argsNumber varchar(255)\n";
    static final String DEFAULT_COUNT_COLUMN = "*";
    static final String NO_CONDITION_IN_WHRER = "TRUE";


    private static String databaseURL = DB_URL;
    private Connection conn = null;

    public void open() {
        try {
            conn = DriverManager.getConnection(databaseURL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            LevelLogger.warn("Open database failed!");
        }
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Integer query(Map<String, String> queryRow) {
        // "SELECT count(*) FROM elements (WHERE name == XX and type == YY)"
        String tableName = queryRow.get(Element.KEYWORD_FOR_TABLE);
        String countColumn = queryRow.getOrDefault(Element.KEYWORD_FOR_COUNT_COLUMN, DEFAULT_COUNT_COLUMN);

        StringBuffer conditionsConcat = new StringBuffer();
        boolean first = true;
        for (Map.Entry<String, String> entry : queryRow.entrySet()) {
            String key = entry.getKey();
            if (key.equals(Element.KEYWORD_FOR_TABLE) || key.equals(Element.KEYWORD_FOR_COUNT_COLUMN)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                conditionsConcat.append(" and ");
            }
            conditionsConcat.append(key);
            conditionsConcat.append("=");
            conditionsConcat.append(String.format("\'%s\'", entry.getValue()));
        }

        if (first) {
            conditionsConcat.append(NO_CONDITION_IN_WHRER);
        }

        return executeSQLwithSingleNumberReturn(
                String.format(DB_SELECT_FOR_COUNT_CODE, countColumn, tableName, conditionsConcat.toString()));
    }

    public void add(Map<String, String> insertRow) {
        // INSERT INTO Elements (A, B) VALUES ('aaa', 'bbb')
        String tableName = insertRow.get(Element.KEYWORD_FOR_TABLE);

        StringBuffer keysConcat = new StringBuffer();
        StringBuffer valuesConcat = new StringBuffer();
        boolean first = true;

        for (Map.Entry<String, String> entry : insertRow.entrySet()) {
            String key = entry.getKey();
            if (key.equals(Element.KEYWORD_FOR_TABLE)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                keysConcat.append(",");
                valuesConcat.append(",");
            }
            keysConcat.append(key);
            valuesConcat.append(String.format("\'%s\'", entry.getValue()));
        }

        executeSQL(String.format(DB_INSERT_CODE, tableName, keysConcat, valuesConcat));
    }

    public void createTable() {
        executeSQL(String.format(DB_TRY_CREATE_TABLE, Element.VAR_TABLE_NAME, VAR_TABLE_DEFINE));
        executeSQL(String.format(DB_TRY_CREATE_TABLE, Element.METHOD_TABLE_NAME, METHOD_TABLE_DEFINE));
    }

    public void dropTable() {
        executeSQL(String.format(DB_TRY_DROP_TABLE, Element.VAR_TABLE_NAME));
        executeSQL(String.format(DB_TRY_DROP_TABLE, Element.METHOD_TABLE_NAME));
    }

    public void setAsTestMode() {
        databaseURL = DB_URL_FOR_TEST;
    }

    private void executeSQL(String SQLcode) {
        // DEBUG
        // System.out.println(SQLcode);

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute(SQLcode);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    private Integer executeSQLwithSingleNumberReturn(String SQLcode) {
        // DEBUG
        // System.out.println(SQLcode);

        Integer countNum = -1;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQLcode);
            if (rs.next()) {
                countNum = rs.getInt(1);
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return countNum;
    }
}
