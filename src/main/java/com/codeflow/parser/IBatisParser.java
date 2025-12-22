package com.codeflow.parser;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
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
     * XML 원본 형태 그대로 (줄바꿈, 들여쓰기, 동적 태그 포함)
     */
    private String extractQuery(Element element) {
        StringBuilder query = new StringBuilder();
        extractQueryRecursive(element, query);
        return trimPreservingStructure(query.toString());
    }

    /**
     * 재귀적으로 쿼리 추출 (XML 원본 형태 유지)
     * 줄바꿈, 들여쓰기, 동적 태그를 그대로 보존합니다.
     */
    private void extractQueryRecursive(Element element, StringBuilder query) {
        for (Content content : element.getContent()) {
            if (content instanceof Text) {
                // 텍스트 노드 (줄바꿈, 공백 포함하여 그대로)
                String text = ((Text) content).getText();
                if (text != null) {
                    query.append(text);
                }
            } else if (content instanceof Element) {
                Element childElement = (Element) content;

                // 여는 태그 출력 <tagName attr="value">
                query.append("<").append(childElement.getName());
                for (Attribute attr : childElement.getAttributes()) {
                    query.append(" ").append(attr.getName())
                         .append("=\"").append(attr.getValue()).append("\"");
                }
                query.append(">");

                // 자식 요소 재귀 처리
                extractQueryRecursive(childElement, query);

                // 닫는 태그 출력 </tagName>
                query.append("</").append(childElement.getName()).append(">");
            }
            // CDATA도 Text로 처리됨
        }
    }

    /**
     * 앞뒤 빈 줄 제거, 공통 들여쓰기 제거, 내부 구조(줄바꿈)는 유지
     */
    private String trimPreservingStructure(String query) {
        if (query == null || query.isEmpty()) return "";

        String[] lines = query.split("\n", -1);  // -1: 빈 문자열도 유지

        // 공통 들여쓰기 찾기 (비어있지 않은 줄만)
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            int indent = countLeadingSpaces(line);
            minIndent = Math.min(minIndent, indent);
        }

        if (minIndent == Integer.MAX_VALUE) {
            return "";  // 빈 줄만 있음
        }

        // 첫 번째/마지막 비어있지 않은 줄 찾기
        int firstNonEmpty = -1;
        int lastNonEmpty = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                if (firstNonEmpty == -1) firstNonEmpty = i;
                lastNonEmpty = i;
            }
        }

        // 공통 들여쓰기 제거 + 앞뒤 빈 줄 제거
        StringBuilder sb = new StringBuilder();
        for (int i = firstNonEmpty; i <= lastNonEmpty; i++) {
            String line = lines[i];

            // 빈 줄은 그대로 유지
            if (line.trim().isEmpty()) {
                sb.append("\n");
                continue;
            }

            // 공통 들여쓰기 제거
            if (minIndent > 0 && line.length() > minIndent) {
                sb.append(removeLeadingSpaces(line, minIndent));
            } else {
                sb.append(line);
            }

            if (i < lastNonEmpty) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 줄 앞 공백 개수 세기
     */
    private int countLeadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 4;
            else break;
        }
        return count;
    }

    /**
     * 줄 앞에서 지정된 수의 공백 제거
     */
    private String removeLeadingSpaces(String line, int spacesToRemove) {
        int removed = 0;
        int idx = 0;
        while (removed < spacesToRemove && idx < line.length()) {
            char c = line.charAt(idx);
            if (c == ' ') {
                removed++;
                idx++;
            } else if (c == '\t') {
                removed += 4;
                idx++;
            } else {
                break;
            }
        }
        return line.substring(idx);
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
