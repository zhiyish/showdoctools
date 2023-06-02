package com.zhiyi.showdoctools.template;

import cn.hutool.core.util.StrUtil;

public class Tables {


    /**
     * 需要忽略的表
     *
     * @param tableName 表名称
     * @param ignorePre 前缀
     * @param ignoreEnd 后缀
     * @return true为忽略
     */
    public static boolean excludeWithStr(String tableName, String ignorePre, String ignoreEnd) {
        boolean result = false;
        if (StrUtil.isNotEmpty(ignorePre)) {
            if (StrUtil.startWithAnyIgnoreCase(tableName, ignorePre.split(","))) {
                result = true;
            }
        }
        if (StrUtil.isNotEmpty(ignoreEnd)) {
            if (StrUtil.endWithAnyIgnoreCase(tableName, ignoreEnd.split(","))) {
                result = true;
            }
        }

        return result;
    }

    public static String getSql4Table(String dbtype, String schema) {
        String result = "";
        if ("oracle".equalsIgnoreCase(dbtype)) {
            result = "SELECT T.TABLE_NAME as TABLE_NAME, C.COMMENTS as COMMENTS " +
                    " FROM USER_TABLES T, USER_TAB_COMMENTS C " +
                    " WHERE T.TABLE_NAME = C.TABLE_NAME " +
                    " ORDER BY T.TABLE_NAME";
        } else if ("postgresql".equalsIgnoreCase(dbtype)) {
            result = "SELECT T.table_name as TABLE_NAME, COALESCE(obj_description(relfilenode, 'pg_class'), '') AS COMMENTS  " +
                    "  FROM information_schema.TABLES AS T, pg_class C  " +
                    "  WHERE T.table_name = C.relname  " +
                    "  AND TABLE_SCHEMA =  " + StrUtil.wrap(schema, "'") +
                    "  ORDER BY T.table_name";
        } else if ("sqlserver".equalsIgnoreCase(dbtype)) {
            result = "SELECT TABLE_NAME " +
                    " FROM INFORMATION_SCHEMA.TABLES " +
                    " WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_CATALOG = " + StrUtil.wrap(schema, "'") +
                    " ORDER BY TABLE_NAME ";
        } else {
            result = "SELECT TABLE_NAME, TABLE_COMMENT " +
                    " FROM information_schema.TABLES " +
                    " WHERE TABLE_SCHEMA = " + StrUtil.wrap(schema, "'");
        }

        return result;
    }

    public static String getSql4Column(String dbtype, String schema) {
        String result = "";
        if ("oracle".equalsIgnoreCase(dbtype)) {
            result = "SELECT  " +
                    "    T.TABLE_NAME,  " +
                    "    T.COLUMN_NAME,  " +
                    "    T.DATA_DEFAULT,  " +
                    "    T.NULLABLE, " +
                    "    T.DATA_TYPE || " +
                    "    CASE " +
                    "        WHEN T.CHAR_LENGTH > 0  " +
                    "        THEN '(' || T.CHAR_LENGTH || ')' " +
                    "        WHEN T.DATA_SCALE > 0  " +
                    "        THEN '(' || T.DATA_PRECISION || ', ' || T.DATA_SCALE || ')' " +
                    "        WHEN T.DATA_PRECISION > 0  " +
                    "        THEN '(' || T.DATA_PRECISION || ')' " +
                    "        ELSE ''  " +
                    "    END AS DATA_TYPE, " +
                    "    C.COMMENTS " +
                    "FROM  " +
                    "    USER_TAB_COLUMNS  T,  " +
                    "    USER_COL_COMMENTS C " +
                    "WHERE  " +
                    "    T.TABLE_NAME = C.TABLE_NAME  " +
                    "AND T.COLUMN_NAME = C.COLUMN_NAME " +
                    "AND T.TABLE_NAME = %s " +
                    "ORDER BY  " +
                    "    T.TABLE_NAME,  " +
                    "    T.COLUMN_ID";
        } else if ("postgresql".equalsIgnoreCase(dbtype)) {
            result = "SELECT COL.table_name as TABLE_NAME," +
                    " COL.column_name as COLUMN_NAME," +
                    " COL.column_default as DATA_DEFAULT," +
                    " COL.is_nullable as NULLABLE," +
                    " COL.udt_name as DATA_TYPE, " +
                    "  COALESCE(col_description(ATT.attrelid, ATT.attnum), '') AS COMMENTS " +
                    "  FROM information_schema.COLUMNS COL, pg_attribute ATT, pg_class CLS " +
                    "  WHERE COL.TABLE_SCHEMA = " + StrUtil.wrap(schema, "'") +
                    "  AND COL.column_name = ATT.attname AND COL.table_name = CLS.relname " +
                    " AND ATT.attrelid = CLS.oid " +
                    " AND TABLE_NAME=%s " +
                    " ORDER BY COL.table_name, COL.ordinal_position";
        } else if ("sqlserver".equalsIgnoreCase(dbtype)) {
            result = "SELECT C.TABLE_NAME," +
                    " C.COLUMN_NAME," +
                    " C.COLUMN_DEFAULT AS DATA_DEFAULT," +
                    " C.IS_NULLABLE AS NULLABLE,  " +
                    "    C.DATA_TYPE +  " +
                    "     CASE  " +
                    "     WHEN C.CHARACTER_MAXIMUM_LENGTH IS NOT NULL THEN '('+ CONVERT(VARCHAR(10), C.CHARACTER_MAXIMUM_LENGTH) +')'  " +
                    "     WHEN C.NUMERIC_SCALE > 0 THEN '('+ CONVERT(VARCHAR(10), C.NUMERIC_PRECISION) +', ' + CONVERT(VARCHAR(10), C.NUMERIC_SCALE) +')' " +
                    "     WHEN C.NUMERIC_PRECISION > 0 THEN '('+ CONVERT(VARCHAR(10), C.NUMERIC_PRECISION) +')' " +
                    "     ELSE '' END  " +
                    "    AS DATA_TYPE, " +
                    "    ISNULL(EP.COLUMN_COMMENT, '') AS COMMENTS " +
                    "   FROM INFORMATION_SCHEMA.COLUMNS AS C " +
                    "   LEFT JOIN ( " +
                    "    SELECT TB.name AS TABLE_NAME, COL.name AS COLUMN_NAME, EP.value AS COLUMN_COMMENT " +
                    "    FROM sys.extended_properties AS EP, sys.tables AS TB, sys.columns AS COL " +
                    "    WHERE EP.major_id = TB.object_id AND EP.minor_id = COL.column_id AND TB.object_id = COL.object_id " +
                    "   ) AS EP ON EP.TABLE_NAME = C.TABLE_NAME AND EP.COLUMN_NAME = C.COLUMN_NAME " +
                    "   WHERE TABLE_CATALOG = " + StrUtil.wrap(schema, "'") +
                    " AND TABLE_NAME=%s " +
                    "   ORDER BY TABLE_NAME, ORDINAL_POSITION";
        } else {
            result = "SELECT TABLE_NAME, " +
                    " COLUMN_NAME, " +
                    " COLUMN_DEFAULT AS DATA_DEFAULT, " +
                    " IS_NULLABLE AS NULLABLE, " +
                    " COLUMN_TYPE AS DATA_TYPE, " +
                    " COLUMN_COMMENT AS COMMENTS " +
                    " FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = " + StrUtil.wrap(schema, "'") +
                    " AND TABLE_NAME=%s " +
                    " ORDER BY ORDINAL_POSITION ";
        }

        return result;
    }
}
