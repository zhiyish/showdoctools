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
    @Value("${showdoc.allinone:true}")
    private boolean allinone;
    @Value("${showdoc.dbtype:mysql}")
    private String dbtype;
    @Value("${spring.datasource.hikari.schema:}")
    private String schema;

    @Override
    public void run(String... args) throws Exception {
        if (StrUtil.isEmpty(schema)) {
            throw new Exception("数据库schema不能为空！");
        }
        String ret = "";
        if (allinone) {
            ret = getAllInOne();
        } else {
            ret = getMultiPage();
        }
        System.out.println(ret);
    }

    /**
     * 获得 单个 markdown文件，标题为数据库schema名称
     *
     * @return String
     */
    private String getAllInOne() {
        String result = "";

        StringBuilder allTables = new StringBuilder();
        String sqlTables = Tables.getSql4Table(dbtype, schema);
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
                String sqlColumns = Tables.getSql4Column(dbtype, schema);
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
                   /*

                   if ((COMMENTS.contains(StrUtil.LF)) || (COMMENTS.contains(StrUtil.CRLF)) || COMMENTS.contains(" ")) {
                        // 备注：清理多余空格与换行符
                        jdbcTemplate.execute("COMMENT ON column " + tableName + "." + COLUMN_NAME + " IS " + " '" + StrUtil.removeAny(colNewComment, " ") + "'");
                    }
                    if ((DATA_DEFAULT.contains(StrUtil.LF)) || (DATA_DEFAULT.contains(StrUtil.CRLF)) || DATA_DEFAULT.contains(" ")) {
                        // 默认值：清理多余空格与换行符
                        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY (" + COLUMN_NAME + " DEFAULT " + StrUtil.removeAny(colNewDATA_DEFAULT, " ") + ")");
                    }
                    */
                    String columnInfo = String.format(mdColumns, COLUMN_NAME, DATA_TYPE, NULLABLE, colNewDATA_DEFAULT, colNewComment);
                    log.debug("column data={}", columnInfo);
                    allTables.append(StrUtil.LF).append(columnInfo);
                }
            }
        }

        Map<String, Object> param = new HashMap<>();
        param.put("api_key", apikey);
        param.put("api_token", apitoken);
        param.put("cat_name", catname);
        param.put("page_title", schema);
        param.put("page_content", allTables.toString());

        result = HttpUtil.post(serverUrl, param);
        return result;
    }

    /**
     * 获得 多个个 markdown文件，标题为表名称
     *
     * @return String
     */
    private String getMultiPage() {
        String result = "共%d 张表，完成 %d ";
        int i = 0;
        String sqlTables = Tables.getSql4Table(dbtype, schema);
        List<Map<String, Object>> tableList = jdbcTemplate.queryForList(sqlTables);
        for (Map<String, Object> tableMap : tableList) {
            StringBuilder allTables = new StringBuilder();
            allTables.append(StrUtil.LF).append(StrUtil.LF).append("** 文档更新时间：" + DateUtil.now() + " **");
            String mdTableName = "## %s";
            String tableName = MapUtil.getStr(tableMap, "TABLE_NAME");
            String tableComment = MapUtil.getStr(tableMap, "COMMENTS", "本表无说明");
            if (!Tables.excludeWithStr(tableName, ignorePre, ignoreEnd)) {
                String tableInfo = String.format(mdTableName, "`" + tableName + "`" + " (" + tableComment + ")");
                log.debug("table data={}", tableInfo);
                allTables.append(StrUtil.LF).append(tableInfo);
                String sqlColumns = Tables.getSql4Column(dbtype, schema);
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
                   /*
                   if ((COMMENTS.contains(StrUtil.LF)) || (COMMENTS.contains(StrUtil.CRLF)) || COMMENTS.contains(" ")) {
                        // 备注：清理多余空格与换行符
                        jdbcTemplate.execute("COMMENT ON column " + tableName + "." + COLUMN_NAME + " IS " + " '" + StrUtil.removeAny(colNewComment, " ") + "'");
                    }
                    if ((DATA_DEFAULT.contains(StrUtil.LF)) || (DATA_DEFAULT.contains(StrUtil.CRLF)) || DATA_DEFAULT.contains(" ")) {
                        // 默认值：清理多余空格与换行符
                        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY (" + COLUMN_NAME + " DEFAULT " + StrUtil.removeAny(colNewDATA_DEFAULT, " ") + ")");
                    }
                    */
                    String columnInfo = String.format(mdColumns, COLUMN_NAME, DATA_TYPE, NULLABLE, colNewDATA_DEFAULT, colNewComment);
                    log.debug("column data={}", columnInfo);
                    allTables.append(StrUtil.LF).append(columnInfo);
                }
                try {
                    Map<String, Object> param = new HashMap<>();
                    param.put("api_key", apikey);
                    param.put("api_token", apitoken);
                    param.put("cat_name", catname);
                    param.put("page_title", tableName);
                    param.put("page_content", allTables.toString());
                    param.put("s_number", (i + 1));
                    result = HttpUtil.post(serverUrl, param);
                    i = i + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return String.format(result, tableList.size(), i);
    }


}
