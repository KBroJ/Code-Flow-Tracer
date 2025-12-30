package com.codeflow.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * 메서드 호출 정보
 *
 * 예: userService.findById(id) 인 경우
 * - scope: "userService"
 * - methodName: "findById"
 * - arguments: ["id"]
 */
public class MethodCall {

    private final String scope;      // 호출 대상 (변수명, 클래스명 등)
    private final String methodName; // 호출하는 메서드명
    private final List<String> arguments;  // 호출 인자 목록

    public MethodCall(String scope, String methodName) {
        this.scope = scope;
        this.methodName = methodName;
        this.arguments = new ArrayList<>();
    }

    public MethodCall(String scope, String methodName, List<String> arguments) {
        this.scope = scope;
        this.methodName = methodName;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }

    public String getScope() {
        return scope;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getArguments() {
        return arguments;
    }

    /**
     * 인자가 있는지 확인
     */
    public boolean hasArguments() {
        return arguments != null && !arguments.isEmpty();
    }

    /**
     * Service, DAO 호출로 추정되는지 확인
     */
    public boolean isServiceOrDaoCall() {
        if (scope == null || scope.isEmpty()) {
            return false;
        }
        String lowerScope = scope.toLowerCase();
        return lowerScope.contains("service") ||
               lowerScope.contains("dao") ||
               lowerScope.contains("repository") ||
               lowerScope.contains("mapper");
    }

    @Override
    public String toString() {
        if (scope == null || scope.isEmpty()) {
            return methodName + "()";
        }
        return scope + "." + methodName + "()";
    }
}
