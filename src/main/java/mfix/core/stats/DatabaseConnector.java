package mfix.core.stats;

import java.sql.*;

import java.util.Map;

/**
 * @author: Luyao Ren
 * @date: 2018/12/11
 */
public class DatabaseConnector {
    static final String DB_URL = "jdbc:mysql://localhost:3306/MINEFIX";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "thisispassword";
    static final String DB_TABLE_NAME = "Elements";
    static final String DB_TEST_TABLE_NAME = "TestElements";
    private static String _tableName = DB_TABLE_NAME;
    private Connection conn = null;

    public void open() {
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createTable() {
        String SQLcodeforInit =
                "CREATE TABLE " + _tableName + " \n" +
                "(\n" +
                "\t typeName varchar(255),\n" +
                "\t elementName TEXT,\n" +
                "\t sourceFile TEXT\n" +
                ");\n";

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute(SQLcodeforInit);
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

    public void dropTable() {
        String SQLcodeforClose = "DROP TABLE " + _tableName;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute(SQLcodeforClose);
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

    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    Integer query(Map<String, String> queryRow) {
        // "SELECT count(*) FROM elements WHERE name == XX and type == YY"
        String SQLcode = String.format("SELECT count(%s) FROM %s WHERE ", queryRow.getOrDefault("countElement", "*"), _tableName);

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

    void add(Map<String, String> insertRow) {
        // INSERT INTO Elements (A, B) VALUES ('aaa', 'bbb')
        String SQLcode = String.format("INSERT INTO %s ", _tableName);

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

        // DEBUG
        // System.out.println(SQLcode);
    }

    public void setAsTestMode() {
        _tableName = DB_TEST_TABLE_NAME;
    }
}
