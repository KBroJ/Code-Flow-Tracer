package com.codeflow.analyzer;

import com.codeflow.parser.ClassType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // 다중 구현체 경고: 인터페이스명 → 모든 구현체 목록
    private Map<String, List<String>> multipleImplWarnings = new HashMap<>();

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

    public Map<String, List<String>> getMultipleImplWarnings() {
        return multipleImplWarnings;
    }

    public void setMultipleImplWarnings(Map<String, List<String>> multipleImplWarnings) {
        this.multipleImplWarnings = multipleImplWarnings;
    }

    /**
     * 다중 구현체 경고가 있는지 확인
     */
    public boolean hasMultipleImplWarnings() {
        return multipleImplWarnings != null && !multipleImplWarnings.isEmpty();
    }

    // ===== Flows 기반 통계 (URL 필터 적용 시 사용) =====

    /**
     * flows에 포함된 고유 클래스 수 반환 (필터링된 결과 기준)
     */
    public int getFlowBasedTotalClasses() {
        Set<String> uniqueClasses = new HashSet<>();
        for (FlowNode flow : flows) {
            collectAllClasses(flow, uniqueClasses);
        }
        return uniqueClasses.size();
    }

    /**
     * flows에 포함된 Controller 수 반환
     */
    public int getFlowBasedControllerCount() {
        return countUniqueClassesByType(ClassType.CONTROLLER);
    }

    /**
     * flows에 포함된 Service 수 반환
     */
    public int getFlowBasedServiceCount() {
        return countUniqueClassesByType(ClassType.SERVICE);
    }

    /**
     * flows에 포함된 DAO 수 반환
     */
    public int getFlowBasedDaoCount() {
        return countUniqueClassesByType(ClassType.DAO);
    }

    /**
     * flows 개수 (엔드포인트 수)
     */
    public int getFlowBasedEndpointCount() {
        return flows.size();
    }

    /**
     * 특정 타입의 고유 클래스 수 계산
     */
    private int countUniqueClassesByType(ClassType type) {
        Set<String> uniqueClasses = new HashSet<>();
        for (FlowNode flow : flows) {
            collectClassesByType(flow, type, uniqueClasses);
        }
        return uniqueClasses.size();
    }

    /**
     * 트리에서 특정 타입의 클래스명 수집 (재귀)
     */
    private void collectClassesByType(FlowNode node, ClassType type, Set<String> classes) {
        if (node.getClassType() == type && node.getClassName() != null) {
            classes.add(node.getClassName());
        }
        for (FlowNode child : node.getChildren()) {
            collectClassesByType(child, type, classes);
        }
    }

    /**
     * 트리에서 모든 클래스명 수집 (재귀)
     */
    private void collectAllClasses(FlowNode node, Set<String> classes) {
        if (node.getClassName() != null) {
            classes.add(node.getClassName());
        }
        for (FlowNode child : node.getChildren()) {
            collectAllClasses(child, classes);
        }
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
