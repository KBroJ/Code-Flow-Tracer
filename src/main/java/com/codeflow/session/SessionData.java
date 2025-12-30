package com.codeflow.session;

import com.codeflow.analyzer.FlowResult;

import java.time.LocalDateTime;

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
    private String urlFilter;             // URL 필터
    private String outputStyle;           // 출력 스타일 (compact, normal, detailed)

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

    /**
     * 유효한 세션 데이터인지 확인
     */
    public boolean isValid() {
        return projectPath != null && !projectPath.isEmpty()
                && flowResult != null
                && flowResult.getFlows() != null;
    }

    @Override
    public String toString() {
        return String.format("SessionData[%s, %d flows, %s]",
                projectPath,
                flowResult != null ? flowResult.getFlows().size() : 0,
                analyzedAt);
    }
}
