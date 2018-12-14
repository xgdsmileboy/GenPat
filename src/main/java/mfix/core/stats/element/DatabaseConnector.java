package mfix.core.stats.element;

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
    private static String databaseURL = DB_URL;
    private Connection conn = null;

    public void open() {
        try {
            conn = DriverManager.getConnection(databaseURL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
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
        // "SELECT count(*) FROM elements WHERE name == XX and type == YY"
        String SQLcode = String.format("SELECT count(%s) FROM %s WHERE ", queryRow.getOrDefault("countColumn", "*"), queryRow.get("table"));

        queryRow.remove("countColumn");
        queryRow.remove("table");
        boolean first = true;
        for (Map.Entry<String, String> entry : queryRow.entrySet()) {
            if (first) {
                first = false;
            } else {
                SQLcode += " and ";
            }
            SQLcode += entry.getKey() + " = " + String.format("\'%s\'", entry.getValue());
        }

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

    public void add(Map<String, String> insertRow) {
        // INSERT INTO Elements (A, B) VALUES ('aaa', 'bbb')
        String SQLcode = String.format("INSERT INTO %s ", insertRow.get("table"));

        insertRow.remove("table");
        boolean first = true;
        for (String key : insertRow.keySet()) {
            if (first) {
                SQLcode += "(";
                first = false;
            } else {
                SQLcode += ",";
            }
            SQLcode += key;
        }
        SQLcode += ") VALUES ";

        first = true;
        for (String value : insertRow.values()) {
            if (first) {
                SQLcode += "(";
                first = false;
            } else {
                SQLcode += ",";
            }
            SQLcode += String.format("\'%s\'", value);
        }
        SQLcode += ")";

        executeSQL(SQLcode);
    }

    public void createTable() {
        executeSQL("CREATE TABLE IF NOT EXISTS VarTable(\n" +
                "\telementName varchar(255),\n" +
                "\tsourceFile TEXT,\n" +
                "    varType varchar(255)\n" +
                ");\n");
        executeSQL("CREATE TABLE IF NOT EXISTS MethodTable(\n" +
                "\telementName varchar(255),\n" +
                "\tsourceFile TEXT,\n" +
                "    retType varchar(255),\n" +
                "    objType varchar(255),\n" +
                "    argsType TEXT,\n" +
                "    argsNumber varchar(255)\n" +
                ");\n");
    }

    public void dropTable() {
        executeSQL("DROP TABLE IF EXISTS VarTable;");
        executeSQL("DROP TABLE IF EXISTS MethodTable;");
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
}
