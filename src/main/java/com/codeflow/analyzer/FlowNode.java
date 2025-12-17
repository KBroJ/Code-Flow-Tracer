package com.codeflow.analyzer;

import com.codeflow.parser.ClassType;

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
    private String urlMapping;      // Controller인 경우 URL 매핑
    private String httpMethod;      // HTTP 메서드 (GET, POST 등)
    private String sqlId;           // DAO인 경우 SQL ID
    private String sqlQuery;        // 실제 SQL 쿼리
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

    public String getUrlMapping() {
        return urlMapping;
    }

    public void setUrlMapping(String urlMapping) {
        this.urlMapping = urlMapping;
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
