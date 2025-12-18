package com.codeflow.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * 파싱된 메서드 정보
 */
public class ParsedMethod {

    private String methodName;
    private String returnType;
    private String urlMapping;       // 전체 URL (클래스 + 메서드 조합)
    private String methodUrlOnly;    // 메서드 레벨 URL만 (@GetMapping, @PostMapping 등)
    private String httpMethod;
    private List<MethodCall> methodCalls = new ArrayList<>();
    private List<ParameterInfo> parameters = new ArrayList<>();  // 메서드 파라미터 정보

    // Getters and Setters
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getUrlMapping() {
        return urlMapping;
    }

    public void setUrlMapping(String urlMapping) {
        this.urlMapping = urlMapping;
    }

    public String getMethodUrlOnly() {
        return methodUrlOnly;
    }

    public void setMethodUrlOnly(String methodUrlOnly) {
        this.methodUrlOnly = methodUrlOnly;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<MethodCall> getMethodCalls() {
        return methodCalls;
    }

    public void setMethodCalls(List<MethodCall> methodCalls) {
        this.methodCalls = methodCalls;
    }

    public void addMethodCall(MethodCall methodCall) {
        this.methodCalls.add(methodCall);
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterInfo> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(ParameterInfo param) {
        this.parameters.add(param);
    }

    /**
     * 파라미터가 있는지 확인
     */
    public boolean hasParameters() {
        return parameters != null && !parameters.isEmpty();
    }

    /**
     * URL 매핑이 있는 엔드포인트 메서드인지 확인
     */
    public boolean isEndpoint() {
        return urlMapping != null && !urlMapping.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append("()");

        if (isEndpoint()) {
            sb.append(" [").append(httpMethod).append(" ").append(urlMapping).append("]");
        }

        if (!methodCalls.isEmpty()) {
            sb.append(" -> calls: ").append(methodCalls.size());
        }

        return sb.toString();
    }
}
