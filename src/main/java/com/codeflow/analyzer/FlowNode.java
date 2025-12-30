package com.codeflow.analyzer;

import com.codeflow.parser.ClassType;
import com.codeflow.parser.ParameterInfo;
import com.codeflow.parser.SqlInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 호출 흐름 노드 (트리 구조)
 *
 * Controller → Service → DAO → SQL 흐름을 트리 형태로 표현합니다.
 * 각 노드는 클래스의 메서드 호출을 나타내며, 자식 노드로 다음 호출을 가집니다.
 */
public class FlowNode {

    private String className;       // 클래스명
    private String methodName;      // 메서드명
    private ClassType classType;    // 클래스 타입 (CONTROLLER, SERVICE, DAO 등)
    private String filePath;        // 소스 파일 경로 (엑셀 출력용)
    private String urlMapping;      // Controller인 경우 URL 매핑 (전체 URL)
    private String classUrlMapping; // 클래스 레벨 URL (@RequestMapping on class)
    private String methodUrlMapping;// 메서드 레벨 URL (@GetMapping, @PostMapping 등)
    private String httpMethod;      // HTTP 메서드 (GET, POST 등)
    private String sqlId;           // DAO인 경우 SQL ID
    private String sqlQuery;        // 실제 SQL 쿼리
    private SqlInfo sqlInfo;        // SQL 상세 정보 (파일명, namespace, 타입, 테이블 등)
    private List<String> implementedInterfaces = new ArrayList<>();  // 구현한 인터페이스 목록
    private List<ParameterInfo> parameters = new ArrayList<>();      // 메서드 파라미터 정보
    private List<String> callArguments = new ArrayList<>();          // 이 메서드 호출 시 전달된 인자
    private int depth;              // 트리 깊이
    private List<FlowNode> children = new ArrayList<>();  // 호출하는 메서드들

    public FlowNode() {
    }

    public FlowNode(String className, String methodName, ClassType classType) {
        this.className = className;
        this.methodName = methodName;
        this.classType = classType;
    }

    // Getters and Setters
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 파일명만 반환 (경로 제외)
     */
    public String getFileName() {
        if (filePath == null || filePath.isEmpty()) {
            return className + ".java";
        }
        int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSep >= 0) {
            return filePath.substring(lastSep + 1);
        }
        return filePath;
    }

    public String getUrlMapping() {
        return urlMapping;
    }

    public void setUrlMapping(String urlMapping) {
        this.urlMapping = urlMapping;
    }

    public String getClassUrlMapping() {
        return classUrlMapping;
    }

    public void setClassUrlMapping(String classUrlMapping) {
        this.classUrlMapping = classUrlMapping;
    }

    public String getMethodUrlMapping() {
        return methodUrlMapping;
    }

    public void setMethodUrlMapping(String methodUrlMapping) {
        this.methodUrlMapping = methodUrlMapping;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getSqlId() {
        return sqlId;
    }

    public void setSqlId(String sqlId) {
        this.sqlId = sqlId;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public SqlInfo getSqlInfo() {
        return sqlInfo;
    }

    public void setSqlInfo(SqlInfo sqlInfo) {
        this.sqlInfo = sqlInfo;
        // SqlInfo에서 sqlId도 설정
        if (sqlInfo != null) {
            this.sqlId = sqlInfo.getFullSqlId();
        }
    }

    /**
     * SQL 상세 정보가 있는지 확인
     */
    public boolean hasSqlInfo() {
        return sqlInfo != null;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public void setImplementedInterfaces(List<String> implementedInterfaces) {
        this.implementedInterfaces = implementedInterfaces;
    }

    /**
     * 구현한 인터페이스가 있는지 확인
     */
    public boolean hasImplementedInterface() {
        return implementedInterfaces != null && !implementedInterfaces.isEmpty();
    }

    /**
     * 첫 번째 구현 인터페이스 반환 (주로 Service 인터페이스)
     */
    public String getPrimaryInterface() {
        if (hasImplementedInterface()) {
            return implementedInterfaces.get(0);
        }
        return null;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterInfo> parameters) {
        this.parameters = parameters;
    }

    /**
     * 파라미터가 있는지 확인
     */
    public boolean hasParameters() {
        return parameters != null && !parameters.isEmpty();
    }

    public List<String> getCallArguments() {
        return callArguments;
    }

    public void setCallArguments(List<String> callArguments) {
        this.callArguments = callArguments;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<FlowNode> getChildren() {
        return children;
    }

    public void setChildren(List<FlowNode> children) {
        this.children = children;
    }

    public void addChild(FlowNode child) {
        child.setDepth(this.depth + 1);
        this.children.add(child);
    }

    /**
     * 엔드포인트(Controller 메서드)인지 확인
     */
    public boolean isEndpoint() {
        return urlMapping != null && !urlMapping.isEmpty();
    }

    /**
     * SQL이 있는 DAO 노드인지 확인
     */
    public boolean hasSql() {
        return sqlId != null && !sqlId.isEmpty();
    }

    /**
     * 전체 메서드 시그니처 반환
     */
    public String getFullSignature() {
        return className + "." + methodName + "()";
    }

    /**
     * 트리 형태로 출력 (디버깅용)
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, "", true);
        return sb.toString();
    }

    private void buildTreeString(StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└─ " : "├─ ");

        // 노드 정보 출력
        sb.append("[").append(classType != null ? classType.getDisplayName() : "?").append("] ");
        sb.append(className).append(".").append(methodName).append("()");

        if (isEndpoint()) {
            sb.append(" [").append(httpMethod).append(" ").append(urlMapping).append("]");
        }
        if (hasSql()) {
            sb.append(" → SQL: ").append(sqlId);
        }

        sb.append("\n");

        // 자식 노드 출력
        for (int i = 0; i < children.size(); i++) {
            String childPrefix = prefix + (isLast ? "   " : "│  ");
            children.get(i).buildTreeString(sb, childPrefix, i == children.size() - 1);
        }
    }

    @Override
    public String toString() {
        return getFullSignature();
    }
}
