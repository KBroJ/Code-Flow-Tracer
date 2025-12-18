package com.codeflow.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 매퍼 정보
 *
 * iBatis/MyBatis XML에서 추출한 SQL 정보를 저장합니다.
 */
public class SqlInfo {

    /**
     * SQL 타입
     */
    public enum SqlType {
        SELECT, INSERT, UPDATE, DELETE, UNKNOWN;

        public static SqlType fromTagName(String tagName) {
            if (tagName == null) return UNKNOWN;
            switch (tagName.toLowerCase()) {
                case "select": return SELECT;
                case "insert": return INSERT;
                case "update": return UPDATE;
                case "delete": return DELETE;
                default: return UNKNOWN;
            }
        }
    }

    // iBatis: #paramName#, MyBatis: #{paramName}
    private static final Pattern IBATIS_PARAM_PATTERN = Pattern.compile("#([a-zA-Z_][a-zA-Z0-9_]*)#");
    private static final Pattern MYBATIS_PARAM_PATTERN = Pattern.compile("#\\{([a-zA-Z_][a-zA-Z0-9_.]*)\\}");

    private String fileName;           // User_SQL.xml
    private String namespace;          // userDAO
    private String sqlId;              // selectUserList
    private SqlType type;              // SELECT, INSERT, UPDATE, DELETE
    private String resultType;         // UserVO, HashMap, int 등
    private List<String> tables;       // [TB_USER, TB_DEPT]
    private String query;              // 전체 쿼리 (엑셀 출력용)
    private List<String> sqlParameters;  // SQL에서 사용하는 파라미터 목록

    public SqlInfo() {
        this.tables = new ArrayList<>();
        this.sqlParameters = new ArrayList<>();
        this.type = SqlType.UNKNOWN;
    }

    public SqlInfo(String fileName, String namespace, String sqlId) {
        this();
        this.fileName = fileName;
        this.namespace = namespace;
        this.sqlId = sqlId;
    }

    /**
     * 전체 SQL ID 반환 (namespace.sqlId)
     */
    public String getFullSqlId() {
        if (namespace != null && !namespace.isEmpty()) {
            return namespace + "." + sqlId;
        }
        return sqlId;
    }

    // Getters and Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSqlId() {
        return sqlId;
    }

    public void setSqlId(String sqlId) {
        this.sqlId = sqlId;
    }

    public SqlType getType() {
        return type;
    }

    public void setType(SqlType type) {
        this.type = type;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public void addTable(String table) {
        if (table != null && !table.isEmpty() && !tables.contains(table)) {
            tables.add(table);
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        if (query != null) {
            // 쿼리에서 테이블 추출
            if (tables.isEmpty()) {
                extractTablesFromQuery(query);
            }
            // 쿼리에서 파라미터 추출
            if (sqlParameters.isEmpty()) {
                extractParametersFromQuery(query);
            }
        }
    }

    /**
     * SQL 쿼리에서 파라미터 추출 (iBatis: #param#, MyBatis: #{param})
     */
    private void extractParametersFromQuery(String query) {
        if (query == null || query.isEmpty()) return;

        Set<String> params = new HashSet<>();

        // iBatis 형식: #paramName#
        Matcher ibatisMatcher = IBATIS_PARAM_PATTERN.matcher(query);
        while (ibatisMatcher.find()) {
            params.add(ibatisMatcher.group(1));
        }

        // MyBatis 형식: #{paramName} 또는 #{obj.property}
        Matcher mybatisMatcher = MYBATIS_PARAM_PATTERN.matcher(query);
        while (mybatisMatcher.find()) {
            String param = mybatisMatcher.group(1);
            // obj.property 형식이면 property만 추출
            if (param.contains(".")) {
                param = param.substring(param.lastIndexOf('.') + 1);
            }
            params.add(param);
        }

        sqlParameters.addAll(params);
    }

    /**
     * SQL 쿼리에서 테이블명 추출
     */
    private void extractTablesFromQuery(String query) {
        if (query == null || query.isEmpty()) return;

        String upperQuery = query.toUpperCase();
        String[] tokens = upperQuery.split("\\s+");

        for (int i = 0; i < tokens.length - 1; i++) {
            String token = tokens[i];
            String nextToken = tokens[i + 1];

            // FROM, JOIN, INTO, UPDATE 뒤에 오는 테이블명 추출
            if (token.equals("FROM") || token.equals("JOIN") ||
                token.equals("INTO") || token.equals("UPDATE")) {

                // 테이블명 정리 (괄호, 별칭 제거)
                String tableName = cleanTableName(nextToken);
                if (isValidTableName(tableName)) {
                    addTable(tableName);
                }
            }

            // LEFT/RIGHT/INNER/OUTER JOIN 처리
            if ((token.equals("LEFT") || token.equals("RIGHT") ||
                 token.equals("INNER") || token.equals("OUTER")) &&
                nextToken.equals("JOIN") && i + 2 < tokens.length) {

                String tableName = cleanTableName(tokens[i + 2]);
                if (isValidTableName(tableName)) {
                    addTable(tableName);
                }
            }
        }
    }

    /**
     * 테이블명 정리 (괄호, 쉼표 제거)
     */
    private String cleanTableName(String name) {
        if (name == null) return "";
        return name.replaceAll("[(),]", "").trim();
    }

    /**
     * 유효한 테이블명인지 확인
     */
    private boolean isValidTableName(String name) {
        if (name == null || name.isEmpty()) return false;
        // SQL 키워드 제외
        String[] keywords = {"SELECT", "WHERE", "AND", "OR", "ON", "SET", "VALUES", "(", ")", ","};
        for (String keyword : keywords) {
            if (name.equals(keyword)) return false;
        }
        // 특수문자로 시작하면 제외
        if (name.startsWith("(") || name.startsWith("#") || name.startsWith("$")) {
            return false;
        }
        return true;
    }

    /**
     * 테이블 목록을 문자열로 반환
     */
    public String getTablesAsString() {
        if (tables == null || tables.isEmpty()) {
            return "";
        }
        return String.join(", ", tables);
    }

    /**
     * SQL 파라미터 목록 반환
     */
    public List<String> getSqlParameters() {
        return sqlParameters;
    }

    /**
     * SQL 파라미터가 있는지 확인
     */
    public boolean hasSqlParameters() {
        return sqlParameters != null && !sqlParameters.isEmpty();
    }

    /**
     * SQL 파라미터 목록을 문자열로 반환
     */
    public String getSqlParametersAsString() {
        if (sqlParameters == null || sqlParameters.isEmpty()) {
            return "";
        }
        return String.join(", ", sqlParameters);
    }

    @Override
    public String toString() {
        return String.format("SqlInfo{file=%s, namespace=%s, id=%s, type=%s, result=%s, tables=%s}",
            fileName, namespace, sqlId, type, resultType, tables);
    }
}
