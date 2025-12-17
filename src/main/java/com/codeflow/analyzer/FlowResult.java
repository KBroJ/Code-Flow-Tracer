package com.codeflow.analyzer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 호출 흐름 분석 결과
 *
 * 전체 프로젝트 분석 결과를 담는 컨테이너입니다.
 * - 여러 엔드포인트의 호출 흐름 트리
 * - 분석 통계 정보
 */
public class FlowResult {

    private String projectPath;                    // 분석한 프로젝트 경로
    private LocalDateTime analyzedAt;              // 분석 시간
    private List<FlowNode> flows = new ArrayList<>();  // 엔드포인트별 호출 흐름

    // 통계 정보
    private int totalClasses;       // 전체 클래스 수
    private int controllerCount;    // Controller 수
    private int serviceCount;       // Service 수
    private int daoCount;           // DAO 수
    private int endpointCount;      // 엔드포인트 수
    private int unmappedCallCount;  // 매핑되지 않은 호출 수

    public FlowResult() {
        this.analyzedAt = LocalDateTime.now();
    }

    public FlowResult(String projectPath) {
        this.projectPath = projectPath;
        this.analyzedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public List<FlowNode> getFlows() {
        return flows;
    }

    public void setFlows(List<FlowNode> flows) {
        this.flows = flows;
    }

    public void addFlow(FlowNode flow) {
        this.flows.add(flow);
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getControllerCount() {
        return controllerCount;
    }

    public void setControllerCount(int controllerCount) {
        this.controllerCount = controllerCount;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public int getDaoCount() {
        return daoCount;
    }

    public void setDaoCount(int daoCount) {
        this.daoCount = daoCount;
    }

    public int getEndpointCount() {
        return endpointCount;
    }

    public void setEndpointCount(int endpointCount) {
        this.endpointCount = endpointCount;
    }

    public int getUnmappedCallCount() {
        return unmappedCallCount;
    }

    public void setUnmappedCallCount(int unmappedCallCount) {
        this.unmappedCallCount = unmappedCallCount;
    }

    public void incrementUnmappedCallCount() {
        this.unmappedCallCount++;
    }

    /**
     * 특정 URL 패턴에 해당하는 플로우 찾기
     */
    public List<FlowNode> findFlowsByUrl(String urlPattern) {
        List<FlowNode> matched = new ArrayList<>();
        for (FlowNode flow : flows) {
            if (flow.getUrlMapping() != null && flow.getUrlMapping().contains(urlPattern)) {
                matched.add(flow);
            }
        }
        return matched;
    }

    /**
     * 특정 클래스의 플로우 찾기
     */
    public List<FlowNode> findFlowsByClass(String className) {
        List<FlowNode> matched = new ArrayList<>();
        for (FlowNode flow : flows) {
            if (containsClass(flow, className)) {
                matched.add(flow);
            }
        }
        return matched;
    }

    private boolean containsClass(FlowNode node, String className) {
        if (node.getClassName() != null && node.getClassName().contains(className)) {
            return true;
        }
        for (FlowNode child : node.getChildren()) {
            if (containsClass(child, className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 분석 요약 정보 반환
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 분석 결과 요약 ===\n");
        sb.append("프로젝트: ").append(projectPath).append("\n");
        sb.append("분석 시간: ").append(analyzedAt).append("\n");
        sb.append("─────────────────────\n");
        sb.append("전체 클래스: ").append(totalClasses).append("개\n");
        sb.append("  - Controller: ").append(controllerCount).append("개\n");
        sb.append("  - Service: ").append(serviceCount).append("개\n");
        sb.append("  - DAO: ").append(daoCount).append("개\n");
        sb.append("엔드포인트: ").append(endpointCount).append("개\n");
        if (unmappedCallCount > 0) {
            sb.append("매핑 안 된 호출: ").append(unmappedCallCount).append("개\n");
        }
        return sb.toString();
    }

    /**
     * 전체 플로우를 트리 형태로 출력
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSummary());
        sb.append("\n=== 호출 흐름 ===\n\n");

        for (FlowNode flow : flows) {
            sb.append(flow.toTreeString());
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("FlowResult[%d endpoints, %d classes]", endpointCount, totalClasses);
    }
}
