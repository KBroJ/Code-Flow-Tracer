package com.codeflow;

import com.codeflow.analyzer.FlowAnalyzer;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.output.ConsoleOutput;
import com.codeflow.output.ConsoleOutput.OutputStyle;
import com.codeflow.parser.JavaSourceParser;
import com.codeflow.parser.ParsedClass;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Code Flow Tracer - 레거시 코드 흐름 분석 도구
 *
 * API 엔드포인트부터 SQL까지의 호출 흐름을 자동 추적하고 문서화합니다.
 *
 * 사용 예시:
 *   java -jar code-flow-tracer.jar --path /project/src
 *   java -jar code-flow-tracer.jar --path /project/src --url "/api/user/*"
 *   java -jar code-flow-tracer.jar --path /project/src --style detailed
 */
@Command(
    name = "code-flow-tracer",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "레거시 코드 호출 흐름 분석 도구 - Controller → Service → DAO → SQL 추적"
)
public class Main implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, description = "분석할 프로젝트 경로 (필수)", required = true)
    private Path projectPath;

    @Option(names = {"-u", "--url"}, description = "URL 패턴 필터 (예: /api/user/*)")
    private String urlPattern;

    @Option(names = {"-s", "--style"}, description = "출력 스타일: compact, normal, detailed (기본: normal)", defaultValue = "normal")
    private String style;

    @Option(names = {"-o", "--output"}, description = "결과를 파일로 저장 (예: result.txt)")
    private Path outputPath;

    @Option(names = {"--no-color"}, description = "색상 출력 비활성화")
    private boolean noColor;

    @Option(names = {"--gui"}, description = "GUI 모드로 실행")
    private boolean guiMode;

    public static void main(String[] args) {
        // 스마트 인코딩 감지로 출력 스트림 설정
        PrintStream smartOut = getSmartOutputStream();

        CommandLine cmd = new CommandLine(new Main());
        cmd.setOut(new PrintWriter(smartOut, true));
        cmd.setErr(new PrintWriter(smartOut, true));
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    /**
     * 콘솔 인코딩을 자동 감지하여 최적의 PrintStream 반환 (Java 17+)
     *
     * - IDE (IntelliJ, VS Code): System.console() == null → UTF-8 사용
     * - 실제 터미널: Console.charset()으로 터미널 인코딩 감지
     */
    private static PrintStream getSmartOutputStream() {
        Console console = System.console();

        if (console == null) {
            // IDE 또는 파이프 출력 → UTF-8
            return new PrintStream(System.out, true, StandardCharsets.UTF_8);
        }

        // 실제 터미널 → 콘솔 인코딩 사용
        Charset consoleCharset = console.charset();
        return new PrintStream(System.out, true, consoleCharset);
    }

    @Override
    public Integer call() {
        // GUI 모드
        if (guiMode) {
            System.out.println("GUI 모드는 아직 구현 중입니다. (Week 3 예정)");
            return 0;
        }

        // 경로 유효성 검사
        if (!Files.exists(projectPath)) {
            System.err.println("오류: 경로가 존재하지 않습니다 - " + projectPath);
            return 1;
        }

        if (!Files.isDirectory(projectPath)) {
            System.err.println("오류: 디렉토리가 아닙니다 - " + projectPath);
            return 1;
        }

        try {
            // 분석 실행
            FlowResult result = analyzeProject();

            // 결과 출력
            outputResult(result);

            return 0;

        } catch (IOException e) {
            System.err.println("오류: 분석 중 문제가 발생했습니다 - " + e.getMessage());
            return 1;
        }
    }

    /**
     * 프로젝트 분석 실행
     */
    private FlowResult analyzeProject() throws IOException {
        // 1. 소스 코드 파싱
        JavaSourceParser parser = new JavaSourceParser();
        List<ParsedClass> parsedClasses = parser.parseProject(projectPath);

        // 2. 호출 흐름 분석
        FlowAnalyzer analyzer = new FlowAnalyzer();

        FlowResult result;
        if (urlPattern != null && !urlPattern.isEmpty()) {
            // URL 패턴 필터링
            result = analyzer.analyzeByUrl(projectPath, parsedClasses, urlPattern);
        } else {
            // 전체 분석
            result = analyzer.analyze(projectPath, parsedClasses);
        }

        return result;
    }

    /**
     * 분석 결과 출력
     */
    private void outputResult(FlowResult result) throws IOException {
        // 출력 스타일 결정
        OutputStyle outputStyle = parseOutputStyle(style);

        // 출력 대상 결정 (콘솔 또는 파일)
        if (outputPath != null) {
            // 상위 디렉토리 생성
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }

            // 파일 출력 (색상 없이)
            try (PrintStream fileOut = new PrintStream(
                    new FileOutputStream(outputPath.toFile()),
                    true,
                    StandardCharsets.UTF_8)) {

                ConsoleOutput output = new ConsoleOutput(fileOut, false, outputStyle);
                output.print(result);

                System.out.println("결과가 저장되었습니다: " + outputPath);
            }
        } else {
            // 콘솔 출력
            boolean useColors = !noColor;
            ConsoleOutput output;

            if (useColors) {
                switch (outputStyle) {
                    case COMPACT:
                        output = ConsoleOutput.compact();
                        break;
                    case DETAILED:
                        output = ConsoleOutput.detailed();
                        break;
                    default:
                        output = ConsoleOutput.colored();
                }
            } else {
                output = ConsoleOutput.plain();
                output.setStyle(outputStyle);
            }

            output.print(result);
        }
    }

    /**
     * 문자열을 OutputStyle로 변환
     */
    private OutputStyle parseOutputStyle(String styleStr) {
        if (styleStr == null) {
            return OutputStyle.NORMAL;
        }

        switch (styleStr.toLowerCase()) {
            case "compact":
                return OutputStyle.COMPACT;
            case "detailed":
                return OutputStyle.DETAILED;
            default:
                return OutputStyle.NORMAL;
        }
    }
}
