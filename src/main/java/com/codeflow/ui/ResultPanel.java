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

    // 레이어별 색상 (다크 테마용 - VS Code 터미널 참고)
    private static final String COLOR_CONTROLLER = "#4EC9B0";  // 청록 (밝음)
    private static final String COLOR_SERVICE = "#569CD6";     // 파랑 (밝음)
    private static final String COLOR_DAO = "#C586C0";         // 보라 (밝음)
    private static final String COLOR_SQL = "#CE9178";         // 주황 (밝음)
    private static final String COLOR_WARNING = "#F44747";     // 빨강 (밝음)
    private static final String COLOR_INTERFACE = "#9CDCFE";   // 연한 파랑
    private static final String COLOR_SUMMARY = "#CCCCCC";     // 밝은 회색
    private static final String COLOR_WARNING_HEADER = "#DCDCAA"; // 연한 노랑
    private static final String COLOR_DEFAULT = "#D4D4D4";     // 기본 텍스트

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

    // 트리 출력용 문자 (CLI와 동일)
    private static final String TREE_BRANCH = "├── ";
    private static final String TREE_LAST = "└── ";
    private static final String TREE_VERTICAL = "│   ";
    private static final String TREE_SPACE = "    ";

    /**
     * 분석 결과 표시 (CLI 콘솔 스타일)
     */
    public void displayResult(FlowResult result, String style) {
        this.currentStyle = style != null ? style : "normal";
        this.multipleImplWarnings = result.getMultipleImplWarnings();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: 'D2Coding', 'Consolas', 'Malgun Gothic', monospace; font-size: 13px; ");
        html.append("margin: 10px; background-color: #1E1E1E; color: #D4D4D4; line-height: 1.4; }");
        html.append("pre { font-family: inherit; margin: 0; white-space: pre-wrap; }");
        html.append("</style></head><body><pre>");

        // 헤더 박스
        appendHeader(html);

        // 요약 정보
        appendSummary(html, result);

        // 다중 구현체 경고
        if (result.hasMultipleImplWarnings()) {
            appendWarnings(html, result.getMultipleImplWarnings());
        }

        // 호출 흐름 섹션
        appendFlowsSection(html, result);

        // 푸터
        appendFooter(html);

        html.append("</pre></body></html>");

        resultPane.setText(html.toString());
        resultPane.setCaretPosition(0);
    }

    /**
     * 헤더 박스 추가 (HTML table로 정렬 보장)
     */
    private void appendHeader(StringBuilder html) {
        // pre 태그 임시 종료 후 table 사용, 다시 pre 시작
        html.append("</pre>");
        html.append("<table style='border-collapse: collapse; color: #4EC9B0; font-family: inherit; margin: 10px 0;'>");
        html.append("<tr><td style='border: 1px solid #4EC9B0; padding: 8px 40px; text-align: center;'>");
        html.append("Code Flow Tracer - 호출 흐름 분석 결과");
        html.append("</td></tr>");
        html.append("</table>");
        html.append("<pre>");
    }

    /**
     * 푸터 추가
     */
    private void appendFooter(StringBuilder html) {
        html.append(colorize(repeat("─", 52), COLOR_INTERFACE)).append("\n");
        html.append(colorize("  Code Flow Tracer v1.0 - 호출 흐름 분석 도구", COLOR_INTERFACE)).append("\n");
    }

    /**
     * 호출 흐름 섹션
     */
    private void appendFlowsSection(StringBuilder html, FlowResult result) {
        html.append(colorize("[ 호출 흐름 ]", COLOR_WARNING_HEADER)).append("\n\n");

        List<FlowNode> flows = result.getFlows();
        if (flows.isEmpty()) {
            html.append(colorize("  (분석된 엔드포인트가 없습니다)", COLOR_INTERFACE)).append("\n\n");
            return;
        }

        for (int i = 0; i < flows.size(); i++) {
            FlowNode flow = flows.get(i);

            // 흐름 번호
            String flowNum = String.format("─── %d/%d ", i + 1, flows.size()) + repeat("─", 40);
            html.append(colorize(flowNum, COLOR_INTERFACE)).append("\n");

            // URL 정보
            if (flow.isEndpoint() && !"compact".equals(currentStyle)) {
                html.append(colorize("[" + flow.getHttpMethod() + "]", getHttpMethodColor(flow.getHttpMethod())));
                html.append(" ");
                html.append(colorize(flow.getUrlMapping(), "#4EC9B0")).append("\n");
            }

            // 트리 출력
            appendFlowNode(html, flow, "", true);
            html.append("\n");
        }
    }

    /**
     * 요약 정보 추가 (CLI 스타일)
     */
    private void appendSummary(StringBuilder html, FlowResult result) {
        html.append(colorize("[ 분석 요약 ]", COLOR_WARNING_HEADER)).append("\n\n");

        // 프로젝트 경로
        html.append("  ").append(colorize("프로젝트:", COLOR_INTERFACE)).append(" ");
        html.append(escapeHtml(result.getProjectPath().toString())).append("\n");

        // 분석 시간
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        html.append("  ").append(colorize("분석 시간:", COLOR_INTERFACE)).append(" ");
        html.append(result.getAnalyzedAt().format(formatter)).append("\n\n");

        // 통계 테이블
        html.append("  ").append(repeat("─", 30)).append("\n");
        html.append(String.format("  %-15s ", "전체 클래스:"));
        html.append(colorize(result.getTotalClasses() + "개", COLOR_SUMMARY)).append("\n");

        html.append(String.format("    %-13s ", "Controller:"));
        html.append(colorize(result.getControllerCount() + "개", COLOR_CONTROLLER)).append("\n");

        html.append(String.format("    %-13s ", "Service:"));
        html.append(colorize(result.getServiceCount() + "개", COLOR_SERVICE)).append("\n");

        html.append(String.format("    %-13s ", "DAO:"));
        html.append(colorize(result.getDaoCount() + "개", COLOR_DAO)).append("\n");

        html.append("  ").append(repeat("─", 30)).append("\n");
        html.append(String.format("  %-15s ", "엔드포인트:"));
        html.append(colorize(result.getEndpointCount() + "개", COLOR_CONTROLLER)).append("\n\n");
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
     * FlowNode를 HTML로 추가 (CLI 스타일 트리)
     */
    private void appendFlowNode(StringBuilder html, FlowNode node, String prefix, boolean isLast) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;

        ClassType classType = node.getClassType();
        String color = getColorForClassType(classType);

        // 타입 태그
        String typeTag = "";
        if (!"compact".equals(currentStyle) && classType != null) {
            typeTag = colorize("[" + classType.getDisplayName() + "]", color) + " ";
        }

        // 클래스.메서드
        String methodCall = escapeHtml(node.getClassName()) + "." + escapeHtml(node.getMethodName()) + "()";

        html.append(prefix).append(connector);
        html.append(typeTag);
        html.append(colorize(methodCall, color));

        // URL 분리 정보 (Controller인 경우): (/order + /detail.do)
        if (node.isEndpoint() && !"compact".equals(currentStyle)) {
            String urlBreakdown = formatUrlBreakdown(node);
            if (!urlBreakdown.isEmpty()) {
                html.append("  ").append(colorize(urlBreakdown, COLOR_INTERFACE));
            }
        }

        // 인터페이스 정보
        if (node.hasImplementedInterface() && !"compact".equals(currentStyle)) {
            String primaryInterface = node.getPrimaryInterface();
            html.append(colorize("  ← " + escapeHtml(primaryInterface), COLOR_INTERFACE));

            // 다중 구현체 경고
            if (multipleImplWarnings.containsKey(primaryInterface)) {
                List<String> allImpls = multipleImplWarnings.get(primaryInterface);
                List<String> otherImpls = new java.util.ArrayList<>();
                for (String impl : allImpls) {
                    if (!impl.equals(node.getClassName())) {
                        otherImpls.add(impl);
                    }
                }
                if (!otherImpls.isEmpty()) {
                    html.append(colorize("  (외 " + escapeHtml(String.join(", ", otherImpls)) + ")", COLOR_WARNING));
                }
            }
        }

        html.append("\n");

        // SQL 정보 출력 (DAO인 경우)
        if (node.hasSql() && !"compact".equals(currentStyle)) {
            String sqlPrefix = prefix + (isLast ? TREE_SPACE : TREE_VERTICAL);
            appendSqlInfo(html, node, sqlPrefix);
        }

        // 자식 노드 재귀 처리
        String childPrefix = prefix + (isLast ? TREE_SPACE : TREE_VERTICAL);
        List<FlowNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            appendFlowNode(html, children.get(i), childPrefix, i == children.size() - 1);
        }
    }

    /**
     * SQL 정보 출력
     */
    private void appendSqlInfo(StringBuilder html, FlowNode node, String prefix) {
        html.append(prefix).append(colorize("→ SQL 정보", COLOR_DAO)).append("\n");

        SqlInfo sqlInfo = node.getSqlInfo();
        if (sqlInfo != null) {
            html.append(prefix).append(colorize("  | ", COLOR_INTERFACE));
            html.append(colorize("파일: ", COLOR_INTERFACE));
            html.append(colorize(escapeHtml(sqlInfo.getFileName()), "#4EC9B0")).append("\n");

            html.append(prefix).append(colorize("  | ", COLOR_INTERFACE));
            html.append(colorize("SQL ID: ", COLOR_INTERFACE));
            html.append(colorize(escapeHtml(sqlInfo.getSqlId()), COLOR_SUMMARY)).append("\n");

            if ("detailed".equals(currentStyle)) {
                if (sqlInfo.getType() != null) {
                    html.append(prefix).append(colorize("  | ", COLOR_INTERFACE));
                    html.append(colorize("타입: ", COLOR_INTERFACE));
                    html.append(colorize(sqlInfo.getType().name(), COLOR_WARNING_HEADER)).append("\n");
                }
                String tables = sqlInfo.getTablesAsString();
                if (!tables.isEmpty()) {
                    html.append(prefix).append(colorize("  | ", COLOR_INTERFACE));
                    html.append(colorize("테이블: ", COLOR_INTERFACE));
                    html.append(colorize(escapeHtml(tables), COLOR_CONTROLLER)).append("\n");
                }
            }
        } else {
            html.append(prefix).append(colorize("  | ", COLOR_INTERFACE));
            html.append(colorize("SQL ID: ", COLOR_INTERFACE));
            html.append(colorize(escapeHtml(node.getSqlId()), COLOR_SUMMARY)).append("\n");
        }
    }

    // ─────────────────────────────────────────────────────
    // 유틸리티 메서드
    // ─────────────────────────────────────────────────────

    /**
     * HTML 색상 적용
     */
    private String colorize(String text, String color) {
        return "<span style='color:" + color + "'>" + text + "</span>";
    }

    /**
     * 문자열 반복
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 문자열 가운데 정렬 (한글 폭 고려)
     */
    private String center(String text, int width) {
        int displayWidth = getDisplayWidth(text);
        if (displayWidth >= width) return text;
        int padding = (width - displayWidth) / 2;
        return repeat(" ", padding) + text + repeat(" ", width - displayWidth - padding);
    }

    /**
     * 문자열의 표시 폭 계산 (한글/CJK 문자는 2칸)
     */
    private int getDisplayWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            if (isWideChar(c)) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }

    /**
     * 넓은 문자 여부 (한글, 한자, 일본어 등)
     */
    private boolean isWideChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || (c >= 0xFF00 && c <= 0xFFEF);
    }

    /**
     * HTTP 메서드별 색상
     */
    private String getHttpMethodColor(String method) {
        if (method == null) return COLOR_INTERFACE;
        switch (method) {
            case "GET": return COLOR_CONTROLLER;  // 청록
            case "POST": return COLOR_WARNING_HEADER;  // 노랑
            case "PUT": return COLOR_SERVICE;  // 파랑
            case "DELETE": return COLOR_WARNING;  // 빨강
            default: return COLOR_INTERFACE;
        }
    }

    /**
     * URL 분리 정보 포맷 (클래스 URL + 메서드 URL)
     * 예: (/user + /list.do)
     */
    private String formatUrlBreakdown(FlowNode node) {
        String classUrl = node.getClassUrlMapping();
        String methodUrl = node.getMethodUrlMapping();

        if (classUrl != null && !classUrl.isEmpty() && methodUrl != null && !methodUrl.isEmpty()) {
            return "(" + classUrl + " + " + methodUrl + ")";
        }
        return "";
    }

    /**
     * ClassType에 맞는 색상 반환
     */
    private String getColorForClassType(ClassType classType) {
        if (classType == null) return COLOR_DEFAULT;

        switch (classType) {
            case CONTROLLER:
                return COLOR_CONTROLLER;
            case SERVICE:
                return COLOR_SERVICE;
            case DAO:
                return COLOR_DAO;
            default:
                return COLOR_DEFAULT;
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
