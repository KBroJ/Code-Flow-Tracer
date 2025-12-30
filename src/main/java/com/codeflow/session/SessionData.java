package com.codeflow.session;

import com.codeflow.analyzer.FlowResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 세션 데이터
 *
 * 앱 종료 후에도 유지되어야 하는 분석 결과 및 설정을 담는 클래스입니다.
 * JSON으로 직렬화되어 파일에 저장됩니다.
 */
public class SessionData {

    private String projectPath;           // 분석한 프로젝트 경로
    private LocalDateTime analyzedAt;     // 분석 시간
    private FlowResult flowResult;        // 분석 결과
    private String urlFilter;             // URL 필터 (오른쪽 패널)
    private String outputStyle;           // 출력 스타일 (compact, normal, detailed)
    private List<String> recentPaths;     // 최근 프로젝트 경로 목록
    private String endpointFilter;        // 엔드포인트 검색 필터 (왼쪽 패널)

    public SessionData() {
    }

    public SessionData(String projectPath, FlowResult flowResult) {
        this.projectPath = projectPath;
        this.analyzedAt = LocalDateTime.now();
        this.flowResult = flowResult;
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

    public FlowResult getFlowResult() {
        return flowResult;
    }

    public void setFlowResult(FlowResult flowResult) {
        this.flowResult = flowResult;
    }

    public String getUrlFilter() {
        return urlFilter;
    }

    public void setUrlFilter(String urlFilter) {
        this.urlFilter = urlFilter;
    }

    public String getOutputStyle() {
        return outputStyle;
    }

    public void setOutputStyle(String outputStyle) {
        this.outputStyle = outputStyle;
    }

    public List<String> getRecentPaths() {
        return recentPaths;
    }

    public void setRecentPaths(List<String> recentPaths) {
        this.recentPaths = recentPaths;
    }

    public String getEndpointFilter() {
        return endpointFilter;
    }

    public void setEndpointFilter(String endpointFilter) {
        this.endpointFilter = endpointFilter;
    }

    /**
     * 최근 경로 추가 (중복 제거, 최대 10개 유지)
     */
    public void addRecentPath(String path) {
        if (recentPaths == null) {
            recentPaths = new ArrayList<>();
        }
        // 이미 있으면 제거 후 맨 앞에 추가
        recentPaths.remove(path);
        recentPaths.add(0, path);
        // 최대 10개 유지
        while (recentPaths.size() > 10) {
            recentPaths.remove(recentPaths.size() - 1);
        }
    }

    /**
     * 유효한 세션 데이터인지 확인 (분석 결과 포함)
     */
    public boolean isValid() {
        return projectPath != null && !projectPath.isEmpty()
                && flowResult != null
                && flowResult.getFlows() != null;
    }

    /**
     * 설정 데이터가 있는지 확인 (분석 결과 없어도 됨)
     */
    public boolean hasSettings() {
        return (recentPaths != null && !recentPaths.isEmpty())
                || (urlFilter != null && !urlFilter.isEmpty())
                || (outputStyle != null && !outputStyle.isEmpty());
    }

    @Override
    public String toString() {
        return String.format("SessionData[%s, %d flows, %s]",
                projectPath,
                flowResult != null ? flowResult.getFlows().size() : 0,
                analyzedAt);
    }
}
