package com.zhiyi.showdoctools.cmd;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.zhiyi.showdoctools.template.Tables;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class Database implements CommandLineRunner {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${showdoc.ignore-pre:}")
    private String ignorePre;
    @Value("${showdoc.ignore-end:}")
    private String ignoreEnd;

    @Value("${showdoc.server-url:https://www.showdoc.com.cn}")
    private String serverUrl;

    @Value("${showdoc.apikey:https://www.showdoc.com.cn}")
    private String apikey;
    @Value("${showdoc.apitoken:https://www.showdoc.com.cn}")
    private String apitoken;
    @Value("${showdoc.catname:/}")
    private String catname;
    @Value("${spring.datasource.hikari.schema:}")
    private String schema;

    @Override
    public void run(String... args) throws Exception {
        if (StrUtil.isEmpty(schema)) {
            throw new Exception("数据库schema不能为空！");
        }
        String url = StrUtil.removeSuffix(serverUrl, "/");

        Map<String, Object> param = new HashMap<>();
        param.put("from", "shell");
        param.put("api_key", apikey);
        param.put("api_token", apitoken);
        param.put("cat_name", catname);
        param.put("page_title", schema);
        param.put("page_content", getTables());

        String ret = HttpUtil.post(url, param);
        System.out.println(ret);
        System.exit(0);
    }


    private String getTables() {
        StringBuilder allTables = new StringBuilder();
        String sqlTables = "SELECT T.TABLE_NAME as TABLE_NAME, C.COMMENTS as COMMENTS FROM USER_TABLES T, USER_TAB_COMMENTS C WHERE T.TABLE_NAME = C.TABLE_NAME ORDER BY T.TABLE_NAME";
        List<Map<String, Object>> tableList = jdbcTemplate.queryForList(sqlTables);
        allTables.append(StrUtil.LF).append("[TOC]").append(StrUtil.LF).append(StrUtil.LF).append("** 文档更新时间：" + DateUtil.now() + " **");
        for (Map<String, Object> tableMap : tableList) {
            String mdTableName = "## %s";

            String tableName = MapUtil.getStr(tableMap, "TABLE_NAME");
            String tableComment = MapUtil.getStr(tableMap, "COMMENTS", "本表无说明");

            if (!Tables.excludeWithStr(tableName, ignorePre, ignoreEnd)) {
                String tableInfo = String.format(mdTableName, "`" + tableName + "`" + " (" + tableComment + ")");
                log.debug("table data={}", tableInfo);
                allTables.append(StrUtil.LF).append(tableInfo);
                String sqlColumns = "SELECT  " +
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
                List<Map<String, Object>> columns = jdbcTemplate.queryForList(String.format(sqlColumns, StrUtil.wrap(tableName, "'")));
                allTables.append(StrUtil.LF).append("|字段|类型|允许空|默认值|注释|").append(StrUtil.LF).append("|:----|:----|:----|----|----|");
                for (Map<String, Object> column : columns) {
                    String mdColumns = "|%s|%s|%s|%s|%s|";

                    String COLUMN_NAME = MapUtil.getStr(column, "COLUMN_NAME");
                    String DATA_TYPE = MapUtil.getStr(column, "DATA_TYPE", "");
                    String NULLABLE = MapUtil.getStr(column, "NULLABLE", "");
                    String DATA_DEFAULT = MapUtil.getStr(column, "DATA_DEFAULT", "");
                    String COMMENTS = MapUtil.getStr(column, "COMMENTS", "字段无备注");
                    String colNewComment = StrUtil.removeAllLineBreaks(COMMENTS);
                    String colNewDATA_DEFAULT = StrUtil.removeAllLineBreaks(DATA_DEFAULT);
                    if ((COMMENTS.contains(StrUtil.LF)) || (COMMENTS.contains(StrUtil.CRLF)) || COMMENTS.contains(" ")) {
                        jdbcTemplate.execute("COMMENT ON column " + tableName + "." + COLUMN_NAME + " IS " + " '" + StrUtil.removeAny(colNewComment, " ") + "'");
                    }
                    if ((DATA_DEFAULT.contains(StrUtil.LF)) || (DATA_DEFAULT.contains(StrUtil.CRLF)) || DATA_DEFAULT.contains(" ")) {
                        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY (" + COLUMN_NAME + " DEFAULT " + StrUtil.removeAny(colNewDATA_DEFAULT, " ") + ")");
                    }
                    String columnInfo = String.format(mdColumns, COLUMN_NAME, DATA_TYPE, NULLABLE, colNewDATA_DEFAULT, colNewComment);
                    log.debug("column data={}", columnInfo);
                    allTables.append(StrUtil.LF).append(columnInfo);
                }
            }
        }
        return allTables.toString();
    }

}
