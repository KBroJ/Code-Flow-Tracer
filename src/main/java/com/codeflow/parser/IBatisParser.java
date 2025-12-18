package com.codeflow.parser;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * iBatis/MyBatis XML 파서
 *
 * SQL 매퍼 XML 파일을 파싱하여 SQL 정보를 추출합니다.
 *
 * 지원 형식:
 * - iBatis: sqlMap (namespace 속성)
 * - MyBatis: mapper (namespace 속성)
 */
public class IBatisParser {

    // SQL 태그 목록
    private static final List<String> SQL_TAGS = List.of("select", "insert", "update", "delete");

    // 결과 타입 속성명 (iBatis/MyBatis 차이)
    private static final List<String> RESULT_TYPE_ATTRS = List.of(
        "resultClass",      // iBatis
        "resultType",       // MyBatis
        "resultMap"         // 둘 다 사용
    );

    /**
     * 프로젝트 내 모든 SQL 매퍼 XML 파싱
     *
     * @param projectPath 프로젝트 루트 경로
     * @return SQL ID → SqlInfo 매핑
     */
    public Map<String, SqlInfo> parseProject(Path projectPath) throws IOException {
        Map<String, SqlInfo> sqlMap = new HashMap<>();

        // XML 파일 찾기
        List<Path> xmlFiles = findXmlFiles(projectPath);

        for (Path xmlFile : xmlFiles) {
            try {
                Map<String, SqlInfo> fileSqlMap = parseFile(xmlFile);
                sqlMap.putAll(fileSqlMap);
            } catch (Exception e) {
                // 파싱 실패한 파일은 건너뛰기 (SQL 매퍼가 아닌 XML일 수 있음)
                // 디버그용 로그
                // System.err.println("XML 파싱 스킵: " + xmlFile + " - " + e.getMessage());
            }
        }

        return sqlMap;
    }

    /**
     * 단일 XML 파일 파싱
     *
     * @param xmlFile XML 파일 경로
     * @return SQL ID → SqlInfo 매핑
     */
    public Map<String, SqlInfo> parseFile(Path xmlFile) throws Exception {
        Map<String, SqlInfo> sqlMap = new HashMap<>();

        // DTD 검증 비활성화 (외부 네트워크 연결 방지)
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document document = builder.build(xmlFile.toFile());
        Element root = document.getRootElement();

        // 루트 요소 확인 (sqlMap 또는 mapper)
        String rootName = root.getName().toLowerCase();
        if (!rootName.equals("sqlmap") && !rootName.equals("mapper")) {
            // SQL 매퍼 XML이 아님
            return sqlMap;
        }

        // namespace 추출
        String namespace = root.getAttributeValue("namespace");
        String fileName = xmlFile.getFileName().toString();

        // SQL 태그 파싱
        for (String tagName : SQL_TAGS) {
            List<Element> elements = root.getChildren(tagName);
            for (Element element : elements) {
                SqlInfo sqlInfo = parseElement(element, fileName, namespace, tagName);
                if (sqlInfo != null) {
                    sqlMap.put(sqlInfo.getFullSqlId(), sqlInfo);
                }
            }
        }

        return sqlMap;
    }

    /**
     * SQL 요소 파싱
     */
    private SqlInfo parseElement(Element element, String fileName, String namespace, String tagName) {
        String id = element.getAttributeValue("id");
        if (id == null || id.isEmpty()) {
            return null;
        }

        SqlInfo sqlInfo = new SqlInfo(fileName, namespace, id);
        sqlInfo.setType(SqlInfo.SqlType.fromTagName(tagName));

        // 결과 타입 추출
        for (String attrName : RESULT_TYPE_ATTRS) {
            String resultType = element.getAttributeValue(attrName);
            if (resultType != null && !resultType.isEmpty()) {
                // 패키지명 제거하고 클래스명만 저장
                sqlInfo.setResultType(simplifyTypeName(resultType));
                break;
            }
        }

        // 쿼리 추출 (테이블 자동 추출됨)
        String query = extractQuery(element);
        sqlInfo.setQuery(query);

        return sqlInfo;
    }

    /**
     * 요소에서 쿼리 텍스트 추출
     * (CDATA, 하위 요소 포함)
     */
    private String extractQuery(Element element) {
        StringBuilder query = new StringBuilder();
        extractQueryRecursive(element, query);
        return normalizeQuery(query.toString());
    }

    /**
     * 재귀적으로 쿼리 추출 (if, choose, foreach 등 동적 SQL 처리)
     */
    private void extractQueryRecursive(Element element, StringBuilder query) {
        // 텍스트 내용 추가
        String text = element.getTextTrim();
        if (text != null && !text.isEmpty()) {
            query.append(text).append(" ");
        }

        // 하위 요소 처리
        for (Element child : element.getChildren()) {
            extractQueryRecursive(child, query);
        }
    }

    /**
     * 쿼리 정규화 (불필요한 공백 제거)
     */
    private String normalizeQuery(String query) {
        if (query == null) return "";
        return query.replaceAll("\\s+", " ").trim();
    }

    /**
     * 타입명 간소화 (패키지명 제거)
     */
    private String simplifyTypeName(String typeName) {
        if (typeName == null) return null;
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot >= 0) {
            return typeName.substring(lastDot + 1);
        }
        return typeName;
    }

    /**
     * 프로젝트 내 XML 파일 찾기
     */
    private List<Path> findXmlFiles(Path projectPath) throws IOException {
        if (!Files.exists(projectPath)) {
            return new ArrayList<>();
        }

        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                .filter(this::isSqlMapperXml)
                .collect(Collectors.toList());
        }
    }

    /**
     * SQL 매퍼 XML인지 간단히 확인
     * (파일명 또는 경로에 SQL, mapper, sqlmap 포함)
     */
    private boolean isSqlMapperXml(Path path) {
        String pathStr = path.toString().toLowerCase();
        String fileName = path.getFileName().toString().toLowerCase();

        // 일반적인 SQL 매퍼 패턴
        if (fileName.contains("sql") || fileName.contains("mapper")) {
            return true;
        }

        // 경로에 mapper, sqlmap 폴더 포함
        if (pathStr.contains("mapper") || pathStr.contains("sqlmap")) {
            return true;
        }

        // _SQL.xml 패턴 (전자정부프레임워크)
        if (fileName.endsWith("_sql.xml")) {
            return true;
        }

        // 그 외 XML은 내용 확인 필요 (일단 포함)
        return true;
    }

    /**
     * SQL ID로 SqlInfo 조회 (편의 메서드)
     */
    public static SqlInfo findBySqlId(Map<String, SqlInfo> sqlMap, String sqlId) {
        if (sqlMap == null || sqlId == null) return null;

        // 정확한 매칭
        if (sqlMap.containsKey(sqlId)) {
            return sqlMap.get(sqlId);
        }

        // namespace 없이 매칭 시도
        for (Map.Entry<String, SqlInfo> entry : sqlMap.entrySet()) {
            if (entry.getKey().endsWith("." + sqlId)) {
                return entry.getValue();
            }
            if (entry.getValue().getSqlId().equals(sqlId)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
