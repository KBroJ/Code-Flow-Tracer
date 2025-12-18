package com.codeflow.output;

import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.ClassType;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 콘솔 출력 포맷터
 *
 * FlowResult를 다양한 형식으로 콘솔에 출력합니다.
 * - 트리 형태 호출 흐름
 * - 요약 통계
 * - 색상 지원 (ANSI)
 */
public class ConsoleOutput {

    // 트리 출력용 문자
    private static final String TREE_BRANCH = "├── ";
    private static final String TREE_LAST = "└── ";
    private static final String TREE_VERTICAL = "│   ";
    private static final String TREE_SPACE = "    ";

    // 박스 문자
    private static final String BOX_HORIZONTAL = "─";
    private static final String BOX_TOP_LEFT = "┌";
    private static final String BOX_TOP_RIGHT = "┐";
    private static final String BOX_BOTTOM_LEFT = "└";
    private static final String BOX_BOTTOM_RIGHT = "┘";
    private static final String BOX_VERTICAL = "│";

    // ANSI 색상 코드
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String GRAY = "\u001B[90m";

    private final PrintStream out;
    private final boolean useColors;
    private OutputStyle style;

    /**
     * 출력 스타일
     */
    public enum OutputStyle {
        COMPACT,    // 간략히 (클래스.메서드만)
        NORMAL,     // 기본 (타입 + 클래스.메서드)
        DETAILED    // 상세 (모든 정보)
    }

    public ConsoleOutput() {
        this(System.out, true, OutputStyle.NORMAL);
    }

    public ConsoleOutput(PrintStream out) {
        this(out, false, OutputStyle.NORMAL);
    }

    public ConsoleOutput(PrintStream out, boolean useColors, OutputStyle style) {
        this.out = out;
        this.useColors = useColors;
        this.style = style;
    }

    public void setStyle(OutputStyle style) {
        this.style = style;
    }

    /**
     * 전체 분석 결과 출력
     */
    public void print(FlowResult result) {
        printHeader(result);
        printSummary(result);
        printFlows(result);
        printFooter();
    }

    /**
     * 헤더 출력
     */
    private void printHeader(FlowResult result) {
        String title = " Code Flow Tracer - 호출 흐름 분석 결과 ";
        int boxWidth = 50;

        out.println();
        out.println(color(BOX_TOP_LEFT + repeat(BOX_HORIZONTAL, boxWidth) + BOX_TOP_RIGHT, CYAN));
        out.println(color(BOX_VERTICAL + center(title, boxWidth) + BOX_VERTICAL, CYAN));
        out.println(color(BOX_BOTTOM_LEFT + repeat(BOX_HORIZONTAL, boxWidth) + BOX_BOTTOM_RIGHT, CYAN));
        out.println();
    }

    /**
     * 요약 정보 출력
     */
    private void printSummary(FlowResult result) {
        out.println(color("[ 분석 요약 ]", BOLD + YELLOW));
        out.println();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        out.println("  " + color("프로젝트:", GRAY) + " " + result.getProjectPath());
        out.println("  " + color("분석 시간:", GRAY) + " " + result.getAnalyzedAt().format(formatter));
        out.println();

        // 통계 테이블
        out.println("  " + repeat("─", 30));
        out.printf("  %-15s %s%n", "전체 클래스:", color(String.valueOf(result.getTotalClasses()), BOLD) + "개");
        out.printf("    %-13s %s%n", "Controller:", colorByType(result.getControllerCount(), ClassType.CONTROLLER));
        out.printf("    %-13s %s%n", "Service:", colorByType(result.getServiceCount(), ClassType.SERVICE));
        out.printf("    %-13s %s%n", "DAO:", colorByType(result.getDaoCount(), ClassType.DAO));
        out.println("  " + repeat("─", 30));
        out.printf("  %-15s %s%n", "엔드포인트:", color(String.valueOf(result.getEndpointCount()), BOLD + GREEN) + "개");

        if (result.getUnmappedCallCount() > 0) {
            out.printf("  %-15s %s%n", "미매핑 호출:", color(String.valueOf(result.getUnmappedCallCount()), RED) + "개");
        }

        out.println();
    }

    /**
     * 호출 흐름 트리 출력
     */
    private void printFlows(FlowResult result) {
        out.println(color("[ 호출 흐름 ]", BOLD + YELLOW));
        out.println();

        List<FlowNode> flows = result.getFlows();

        if (flows.isEmpty()) {
            out.println("  " + color("(분석된 엔드포인트가 없습니다)", GRAY));
            out.println();
            return;
        }

        for (int i = 0; i < flows.size(); i++) {
            FlowNode flow = flows.get(i);
            printFlowNumber(i + 1, flows.size());
            printNode(flow, "", true);
            out.println();
        }
    }

    /**
     * 흐름 번호 출력
     */
    private void printFlowNumber(int current, int total) {
        out.println(color(String.format("─── %d/%d ", current, total) + repeat("─", 40), GRAY));
    }

    /**
     * 단일 노드 및 하위 트리 출력
     */
    private void printNode(FlowNode node, String prefix, boolean isLast) {
        String connector = isLast ? TREE_LAST : TREE_BRANCH;

        // 노드 라인 구성
        StringBuilder line = new StringBuilder();
        line.append(prefix).append(connector);

        // 클래스 타입 태그
        String typeTag = formatTypeTag(node.getClassType());
        line.append(typeTag).append(" ");

        // 클래스.메서드
        line.append(color(node.getClassName(), BOLD));
        line.append(color(".", GRAY));
        line.append(color(node.getMethodName() + "()", WHITE()));

        // 추가 정보 (스타일에 따라)
        if (style != OutputStyle.COMPACT) {
            // URL 매핑
            if (node.isEndpoint()) {
                line.append("  ");
                line.append(color("[" + node.getHttpMethod() + "]", httpMethodColor(node.getHttpMethod())));
                line.append(" ");
                line.append(color(node.getUrlMapping(), CYAN));
            }

            // SQL 정보
            if (node.hasSql() && style == OutputStyle.DETAILED) {
                line.append("  ");
                line.append(color("→ SQL: " + node.getSqlId(), PURPLE));
            }
        }

        out.println(line);

        // 자식 노드 출력
        String childPrefix = prefix + (isLast ? TREE_SPACE : TREE_VERTICAL);
        List<FlowNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {
            printNode(children.get(i), childPrefix, i == children.size() - 1);
        }
    }

    /**
     * 클래스 타입에 따른 태그 포맷팅
     */
    private String formatTypeTag(ClassType type) {
        if (type == null) {
            return color("[???]", GRAY);
        }

        String tag = "[" + type.getDisplayName() + "]";

        switch (type) {
            case CONTROLLER:
                return color(tag, GREEN);
            case SERVICE:
                return color(tag, BLUE);
            case DAO:
                return color(tag, PURPLE);
            default:
                return color(tag, GRAY);
        }
    }

    /**
     * HTTP 메서드별 색상
     */
    private String httpMethodColor(String method) {
        if (method == null) return GRAY;

        switch (method) {
            case "GET":
                return GREEN;
            case "POST":
                return YELLOW;
            case "PUT":
                return BLUE;
            case "DELETE":
                return RED;
            case "PATCH":
                return PURPLE;
            default:
                return GRAY;
        }
    }

    /**
     * 푸터 출력
     */
    private void printFooter() {
        out.println(color(repeat("─", 52), GRAY));
        out.println(color("  Code Flow Tracer v1.0 - 호출 흐름 분석 도구", GRAY));
        out.println();
    }

    // ─────────────────────────────────────────────────────
    // 유틸리티 메서드
    // ─────────────────────────────────────────────────────

    /**
     * 색상 적용 (useColors가 true인 경우에만)
     */
    private String color(String text, String colorCode) {
        if (useColors && colorCode != null) {
            return colorCode + text + RESET;
        }
        return text;
    }

    /**
     * 흰색 (ANSI)
     */
    private String WHITE() {
        return "\u001B[37m";
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
        if (displayWidth >= width) {
            return text;
        }
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
            || (c >= 0xFF00 && c <= 0xFFEF);  // 전각 문자
    }

    /**
     * 타입별 개수 색상
     */
    private String colorByType(int count, ClassType type) {
        String color;
        switch (type) {
            case CONTROLLER:
                color = GREEN;
                break;
            case SERVICE:
                color = BLUE;
                break;
            case DAO:
                color = PURPLE;
                break;
            default:
                color = GRAY;
        }
        return color(count + "개", color);
    }

    // ─────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────

    /**
     * UTF-8 PrintStream 싱글톤 (Java 10+)
     * Windows 환경에서도 한글이 정상 출력되도록 함
     */
    private static final PrintStream UTF8_OUT =
        new PrintStream(System.out, true, StandardCharsets.UTF_8);

    /**
     * 색상 있는 기본 출력기 생성
     */
    public static ConsoleOutput colored() {
        return new ConsoleOutput(UTF8_OUT, true, OutputStyle.NORMAL);
    }

    /**
     * 색상 없는 기본 출력기 생성 (파일 출력용)
     */
    public static ConsoleOutput plain() {
        return new ConsoleOutput(UTF8_OUT, false, OutputStyle.NORMAL);
    }

    /**
     * 상세 출력기 생성
     */
    public static ConsoleOutput detailed() {
        return new ConsoleOutput(UTF8_OUT, true, OutputStyle.DETAILED);
    }

    /**
     * 간략 출력기 생성
     */
    public static ConsoleOutput compact() {
        return new ConsoleOutput(UTF8_OUT, true, OutputStyle.COMPACT);
    }
}
