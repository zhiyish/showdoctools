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
}
