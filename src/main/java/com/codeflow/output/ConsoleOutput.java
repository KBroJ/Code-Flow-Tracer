package com.codeflow.output;

import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.ClassType;
import com.codeflow.parser.ParameterInfo;
import com.codeflow.parser.SqlInfo;

import java.io.Console;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // 다중 구현체 경고: 인터페이스명 → 구현체 목록 (print 시 설정)
    private Map<String, List<String>> multipleImplWarnings = new HashMap<>();

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
        // 다중 구현체 경고 정보 설정
        multipleImplWarnings.clear();
        if (result.hasMultipleImplWarnings()) {
            multipleImplWarnings.putAll(result.getMultipleImplWarnings());
        }

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

        // 통계 테이블 (flows 기반 - URL 필터 적용 시 필터링된 결과만 표시)
        out.println("  " + repeat("─", 30));
        out.printf("  %-15s %s%n", "전체 클래스:", color(String.valueOf(result.getFlowBasedTotalClasses()), BOLD) + "개");
        out.printf("    %-13s %s%n", "Controller:", colorByType(result.getFlowBasedControllerCount(), ClassType.CONTROLLER));
        out.printf("    %-13s %s%n", "Service:", colorByType(result.getFlowBasedServiceCount(), ClassType.SERVICE));
        out.printf("    %-13s %s%n", "DAO:", colorByType(result.getFlowBasedDaoCount(), ClassType.DAO));
        out.println("  " + repeat("─", 30));
        out.printf("  %-15s %s%n", "엔드포인트:", color(String.valueOf(result.getFlowBasedEndpointCount()), BOLD + GREEN) + "개");

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

            // URL 정보를 트리 위에 별도 라인으로 출력
            if (flow.isEndpoint() && style != OutputStyle.COMPACT) {
                printUrlInfo(flow);
            }

            // 파라미터 정보 출력 (DETAILED 스타일)
            if (flow.hasParameters() && style == OutputStyle.DETAILED) {
                printParameters(flow);
            }

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
     * URL 정보 출력 (전체 URL 표시)
     */
    private void printUrlInfo(FlowNode node) {
        StringBuilder line = new StringBuilder();

        // HTTP 메서드 + 전체 URL
        line.append(color("[" + node.getHttpMethod() + "]", httpMethodColor(node.getHttpMethod())));
        line.append(" ");
        line.append(color(node.getUrlMapping(), CYAN));

        out.println(line);
    }

    /**
     * 파라미터 정보 출력 (간결한 형식)
     *
     * 기본: Parameters: Long modelId, LocalDate startDate, ...
     * VO/Map 사용 필드가 있으면 아래에 상세 표시
     * Spring 자동 주입 파라미터(Model, HttpServletRequest 등)는 제외
     */
    private void printParameters(FlowNode node) {
        // Spring 자동 주입 파라미터 제외
        List<ParameterInfo> params = node.getParameters().stream()
            .filter(p -> !p.isSpringInjected())
            .toList();

        // 표시할 파라미터가 없으면 생략
        if (params.isEmpty()) {
            return;
        }

        // 파라미터 목록 한 줄로 표시
        StringBuilder paramLine = new StringBuilder();
        paramLine.append(color("Parameters: ", GRAY));

        for (int i = 0; i < params.size(); i++) {
            ParameterInfo param = params.get(i);
            paramLine.append(color(param.getType(), CYAN));
            paramLine.append(" ");
            paramLine.append(color(param.getName(), BOLD));

            if (i < params.size() - 1) {
                paramLine.append(color(", ", GRAY));
            }
        }
        out.println(paramLine);

        // VO/Map 타입 중 사용 필드가 있는 경우만 상세 표시
        for (ParameterInfo param : params) {
            if (param.hasUsedFields() && !param.isRequestParameter()) {
                StringBuilder fieldsLine = new StringBuilder();
                fieldsLine.append("  ");
                fieldsLine.append(color(TREE_LAST, GRAY));
                fieldsLine.append(color(param.getName(), BOLD));

                if (param.isMapType()) {
                    fieldsLine.append(color(" 사용 키: ", GRAY));
                } else {
                    fieldsLine.append(color(" 사용 필드: ", GRAY));
                }

                fieldsLine.append(color(String.join(", ", param.getUsedFields()), YELLOW));
                out.println(fieldsLine);
            }
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
            return color("(" + classUrl + " + " + methodUrl + ")", GRAY);
        }
        return "";
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
            if (node.isEndpoint()) {
                if (node.getDepth() == 0) {
                    // 루트 Controller: URL 분리 정보만 표시 (/user + /list.do)
                    String breakdown = formatUrlBreakdown(node);
                    if (!breakdown.isEmpty()) {
                        line.append("  ").append(breakdown);
                    }
                } else {
                    // 중첩된 Controller 호출: 전체 URL 표시
                    line.append("  ");
                    line.append(color("[" + node.getHttpMethod() + "]", httpMethodColor(node.getHttpMethod())));
                    line.append(" ");
                    line.append(color(node.getUrlMapping(), CYAN));
                }
            }

            // 구현 인터페이스 정보 (← InterfaceName) + 다중 구현체 경고
            if (node.hasImplementedInterface()) {
                String interfaceName = node.getPrimaryInterface();
                line.append("  ");
                line.append(color("← " + interfaceName, GRAY));

                // 다중 구현체 경고 표시: (외 OtherImpl1, OtherImpl2)
                if (multipleImplWarnings.containsKey(interfaceName)) {
                    List<String> allImpls = multipleImplWarnings.get(interfaceName);
                    String currentImpl = node.getClassName();
                    // 현재 구현체 제외한 나머지
                    String others = allImpls.stream()
                        .filter(impl -> !impl.equals(currentImpl))
                        .collect(Collectors.joining(", "));
                    if (!others.isEmpty()) {
                        line.append("  ");
                        line.append(color("(외 " + others + ")", YELLOW));
                    }
                }
            }

        }

        out.println(line);

        // SQL 정보 출력 (DAO 노드이고, SQL 정보가 있는 경우)
        if (node.hasSql() && style != OutputStyle.COMPACT) {
            String sqlPrefix = prefix + (isLast ? TREE_SPACE : TREE_VERTICAL);
            printSqlInfo(node, sqlPrefix);
        }

        // 자식 노드 출력
        String childPrefix = prefix + (isLast ? TREE_SPACE : TREE_VERTICAL);
        List<FlowNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {
            printNode(children.get(i), childPrefix, i == children.size() - 1);
        }
    }

    /**
     * SQL 정보 출력
     *
     * 기본 모드:
     *   → SQL 정보
     *     | 파일: User_SQL.xml
     *     | SQL ID: selectUserList
     *
     * 상세 모드 (DETAILED):
     *   → SQL 정보
     *     | 파일: User_SQL.xml
     *     | Namespace: userDAO
     *     | SQL ID: selectUserList
     *     | 타입: SELECT
     *     | 반환타입: UserVO
     *     | 테이블: TB_USER, TB_DEPT
     */
    private void printSqlInfo(FlowNode node, String prefix) {
        SqlInfo sqlInfo = node.getSqlInfo();

        // SQL 정보 헤더
        out.println(prefix + color("→ SQL 정보", PURPLE));

        if (sqlInfo != null) {
            // 파일명
            out.println(prefix + color("  | ", GRAY) + color("파일: ", GRAY) + color(sqlInfo.getFileName(), CYAN));

            if (style == OutputStyle.DETAILED) {
                // 상세 모드: 모든 정보 표시
                // Namespace
                if (sqlInfo.getNamespace() != null) {
                    out.println(prefix + color("  | ", GRAY) + color("Namespace: ", GRAY) + sqlInfo.getNamespace());
                }

                // SQL ID
                out.println(prefix + color("  | ", GRAY) + color("SQL ID: ", GRAY) + color(sqlInfo.getSqlId(), BOLD));

                // 타입
                if (sqlInfo.getType() != null) {
                    out.println(prefix + color("  | ", GRAY) + color("타입: ", GRAY) + color(sqlInfo.getType().name(), YELLOW));
                }

                // 반환타입
                if (sqlInfo.getResultType() != null) {
                    out.println(prefix + color("  | ", GRAY) + color("반환타입: ", GRAY) + color(sqlInfo.getResultType(), CYAN));
                }

                // 테이블
                String tables = sqlInfo.getTablesAsString();
                if (!tables.isEmpty()) {
                    out.println(prefix + color("  | ", GRAY) + color("테이블: ", GRAY) + color(tables, GREEN));
                }
            } else {
                // 기본 모드: 파일명, SQL ID만
                out.println(prefix + color("  | ", GRAY) + color("SQL ID: ", GRAY) + color(sqlInfo.getSqlId(), BOLD));
            }
        } else {
            // SqlInfo가 없으면 sqlId만 표시
            out.println(prefix + color("  | ", GRAY) + color("SQL ID: ", GRAY) + color(node.getSqlId(), BOLD));
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
    // 스마트 인코딩 감지
    // ─────────────────────────────────────────────────────

    /**
     * 콘솔 인코딩을 자동 감지하여 최적의 PrintStream 반환 (Java 17+)
     *
     * 동작 방식:
     * 1. System.console()이 null인 경우 (IDE, 파이프 출력)
     *    → UTF-8 사용 (대부분의 IDE가 UTF-8 사용)
     *
     * 2. System.console()이 있는 경우 (실제 터미널)
     *    → Console.charset() 사용 (터미널의 실제 인코딩)
     *    → Windows CMD: CP949 (한글), Linux/Mac: UTF-8
     */
    private static PrintStream getSmartOutputStream() {
        Console console = System.console();

        if (console == null) {
            // IDE (IntelliJ, VS Code) 또는 파이프 출력
            // → 대부분 UTF-8을 사용하므로 UTF-8 선택
            return new PrintStream(System.out, true, StandardCharsets.UTF_8);
        }

        // 실제 터미널에서 실행 중
        // → Console.charset()으로 터미널 인코딩 감지 (Java 17+)
        Charset consoleCharset = console.charset();
        return new PrintStream(System.out, true, consoleCharset);
    }

    /**
     * 감지된 콘솔 인코딩 정보 반환 (디버깅용)
     */
    public static String getDetectedEncoding() {
        Console console = System.console();
        if (console == null) {
            return "UTF-8 (IDE/파이프 모드)";
        }
        return console.charset().name() + " (터미널 감지)";
    }

    // ─────────────────────────────────────────────────────
    // 정적 팩토리 메서드
    // ─────────────────────────────────────────────────────

    /**
     * 색상 있는 기본 출력기 생성
     * 콘솔 인코딩 자동 감지
     */
    public static ConsoleOutput colored() {
        return new ConsoleOutput(getSmartOutputStream(), true, OutputStyle.NORMAL);
    }

    /**
     * 색상 없는 기본 출력기 생성
     * 콘솔 인코딩 자동 감지
     */
    public static ConsoleOutput plain() {
        return new ConsoleOutput(getSmartOutputStream(), false, OutputStyle.NORMAL);
    }

    /**
     * 상세 출력기 생성
     * 콘솔 인코딩 자동 감지
     */
    public static ConsoleOutput detailed() {
        return new ConsoleOutput(getSmartOutputStream(), true, OutputStyle.DETAILED);
    }

    /**
     * 간략 출력기 생성
     * 콘솔 인코딩 자동 감지
     */
    public static ConsoleOutput compact() {
        return new ConsoleOutput(getSmartOutputStream(), true, OutputStyle.COMPACT);
    }
}
