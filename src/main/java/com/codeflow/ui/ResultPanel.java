package com.codeflow.ui;

import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.ClassType;
import com.codeflow.parser.SqlInfo;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 분석 결과 표시 패널 (HTML 기반)
 *
 * FlowResult를 HTML 형태로 시각화합니다.
 * 드래그 선택 및 복사가 자유롭게 가능합니다.
 */
public class ResultPanel extends JPanel {

    private JEditorPane resultPane;

    // 레이어별 색상 (HTML 색상 코드)
    private static final String COLOR_CONTROLLER = "#009600";  // 녹색
    private static final String COLOR_SERVICE = "#0064C8";     // 파랑
    private static final String COLOR_DAO = "#800080";         // 보라
    private static final String COLOR_SQL = "#C86400";         // 주황
    private static final String COLOR_WARNING = "#C80000";     // 빨강
    private static final String COLOR_INTERFACE = "#808080";   // 회색
    private static final String COLOR_SUMMARY = "#404040";     // 진한 회색
    private static final String COLOR_WARNING_HEADER = "#C89600"; // 노랑

    // 출력 스타일
    private String currentStyle = "normal";

    // 다중 구현체 경고 정보
    private Map<String, List<String>> multipleImplWarnings = new java.util.HashMap<>();

    public ResultPanel() {
        setLayout(new BorderLayout());
        initializePane();
    }

    /**
     * 에디터 패널 초기화
     */
    private void initializePane() {
        resultPane = new JEditorPane();
        resultPane.setContentType("text/html");
        resultPane.setEditable(false);
        resultPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        resultPane.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));

        add(new JScrollPane(resultPane), BorderLayout.CENTER);
    }

    /**
     * 분석 결과 표시
     */
    public void displayResult(FlowResult result, String style) {
        this.currentStyle = style != null ? style : "normal";
        this.multipleImplWarnings = result.getMultipleImplWarnings();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: 'Malgun Gothic', sans-serif; font-size: 13px; margin: 10px; }");
        html.append("pre { font-family: 'Malgun Gothic', monospace; margin: 0; white-space: pre-wrap; }");
        html.append("</style></head><body><pre>");

        // 요약 정보
        appendSummary(html, result);

        // 다중 구현체 경고
        if (result.hasMultipleImplWarnings()) {
            appendWarnings(html, result.getMultipleImplWarnings());
        }

        // 각 엔드포인트별 호출 흐름
        for (FlowNode flow : result.getFlows()) {
            html.append("\n");
            appendFlowNode(html, flow, 0);
        }

        html.append("</pre></body></html>");

        resultPane.setText(html.toString());
        resultPane.setCaretPosition(0);
    }

    /**
     * 요약 정보 추가
     */
    private void appendSummary(StringBuilder html, FlowResult result) {
        String summary = String.format("분석 요약: %d개 엔드포인트, 전체 %d개 클래스 (Controller: %d, Service: %d, DAO: %d)",
                result.getEndpointCount(),
                result.getTotalClasses(),
                result.getControllerCount(),
                result.getServiceCount(),
                result.getDaoCount());

        html.append("<b style='color:").append(COLOR_SUMMARY).append("'>")
            .append(escapeHtml(summary))
            .append("</b>\n");
    }

    /**
     * 다중 구현체 경고 추가
     */
    private void appendWarnings(StringBuilder html, Map<String, List<String>> warnings) {
        html.append("\n<b style='color:").append(COLOR_WARNING_HEADER).append("'>");
        html.append(String.format("[경고] %d개 인터페이스에 다중 구현체 존재", warnings.size()));
        html.append("</b>\n");

        for (Map.Entry<String, List<String>> entry : warnings.entrySet()) {
            html.append("<span style='color:").append(COLOR_WARNING_HEADER).append("'>  ");
            html.append(escapeHtml(entry.getKey())).append(": ");
            html.append(escapeHtml(String.join(", ", entry.getValue())));
            html.append("</span>\n");
        }
    }

    /**
     * FlowNode를 HTML로 추가 (재귀)
     */
    private void appendFlowNode(StringBuilder html, FlowNode flowNode, int depth) {
        // 들여쓰기
        String indent = getIndent(depth);

        // 트리 구조 문자
        String prefix = depth == 0 ? "" : "└── ";

        ClassType classType = flowNode.getClassType();
        String color = getColorForClassType(classType);

        // 기본 정보: [타입] ClassName.methodName()
        StringBuilder line = new StringBuilder();
        line.append(indent).append(prefix);

        if (!"compact".equals(currentStyle)) {
            line.append("[").append(classType != null ? classType.getDisplayName() : "?").append("] ");
        }
        line.append(flowNode.getClassName()).append(".").append(flowNode.getMethodName()).append("()");

        // HTTP 정보
        if (flowNode.isEndpoint()) {
            line.append("  [").append(flowNode.getHttpMethod()).append(" ").append(flowNode.getUrlMapping()).append("]");
        }

        html.append("<span style='color:").append(color).append("'>")
            .append(escapeHtml(line.toString()))
            .append("</span>");

        // 인터페이스 정보 (회색)
        if (flowNode.hasImplementedInterface() && !"compact".equals(currentStyle)) {
            String primaryInterface = flowNode.getPrimaryInterface();
            html.append("<span style='color:").append(COLOR_INTERFACE).append("'>")
                .append("  ← ").append(escapeHtml(primaryInterface))
                .append("</span>");

            // 다중 구현체 경고 (빨간색 + 굵게)
            if (multipleImplWarnings.containsKey(primaryInterface)) {
                List<String> allImpls = multipleImplWarnings.get(primaryInterface);
                List<String> otherImpls = new java.util.ArrayList<>();
                for (String impl : allImpls) {
                    if (!impl.equals(flowNode.getClassName())) {
                        otherImpls.add(impl);
                    }
                }
                if (!otherImpls.isEmpty()) {
                    html.append("<b style='color:").append(COLOR_WARNING).append("'>")
                        .append("  (외 ").append(escapeHtml(String.join(", ", otherImpls))).append(")")
                        .append("</b>");
                }
            }
        }

        // SQL 정보 (주황색)
        if ("detailed".equals(currentStyle) && flowNode.hasSqlInfo()) {
            SqlInfo sql = flowNode.getSqlInfo();
            StringBuilder sqlText = new StringBuilder();
            sqlText.append("  → ").append(sql.getFileName()).append(":").append(sql.getSqlId());
            if (!sql.getTables().isEmpty()) {
                sqlText.append(" (").append(String.join(", ", sql.getTables())).append(")");
            }
            html.append("<span style='color:").append(COLOR_SQL).append("'>")
                .append(escapeHtml(sqlText.toString()))
                .append("</span>");
        } else if (flowNode.hasSql()) {
            html.append("<span style='color:").append(COLOR_SQL).append("'>")
                .append("  → SQL: ").append(escapeHtml(flowNode.getSqlId()))
                .append("</span>");
        }

        html.append("\n");

        // 자식 노드 재귀 처리
        for (FlowNode child : flowNode.getChildren()) {
            appendFlowNode(html, child, depth + 1);
        }
    }

    /**
     * 들여쓰기 문자열 생성
     */
    private String getIndent(int depth) {
        if (depth == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    /**
     * ClassType에 맞는 색상 반환
     */
    private String getColorForClassType(ClassType classType) {
        if (classType == null) return "#000000";

        switch (classType) {
            case CONTROLLER:
                return COLOR_CONTROLLER;
            case SERVICE:
                return COLOR_SERVICE;
            case DAO:
                return COLOR_DAO;
            default:
                return "#000000";
        }
    }

    /**
     * HTML 특수문자 이스케이프
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * 결과 지우기
     */
    public void clear() {
        resultPane.setText("");
    }

    /**
     * 결과 텍스트 반환 (복사용)
     */
    public String getResultAsText() {
        try {
            // HTML에서 텍스트만 추출
            javax.swing.text.Document doc = resultPane.getDocument();
            return doc.getText(0, doc.getLength());
        } catch (Exception e) {
            return "";
        }
    }
}
