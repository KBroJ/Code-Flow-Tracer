package com.codeflow;

import com.codeflow.parser.JavaSourceParser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Code Flow Tracer - 레거시 코드 흐름 분석 도구
 *
 * API 엔드포인트부터 SQL까지의 호출 흐름을 자동 추적하고 문서화합니다.
 *
 * 사용 예시:
 *   java -jar code-flow-tracer.jar --path /project/src
 *   java -jar code-flow-tracer.jar --path /project/src --url "/api/user/*"
 *   java -jar code-flow-tracer.jar --path /project/src --format excel
 */
@Command(
    name = "code-flow-tracer",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "레거시 코드 흐름 분석 도구 - Controller → Service → DAO → SQL 추적"
)
public class Main implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, description = "분석할 프로젝트 경로", required = true)
    private Path projectPath;

    @Option(names = {"-u", "--url"}, description = "분석할 URL 패턴 (예: /api/user/*)")
    private String urlPattern;

    @Option(names = {"-c", "--class"}, description = "분석할 클래스명 (예: UserController)")
    private String className;

    @Option(names = {"-f", "--format"}, description = "출력 형식 (console, excel, markdown)", defaultValue = "console")
    private String format;

    @Option(names = {"-o", "--output"}, description = "출력 파일 경로")
    private Path outputPath;

    @Option(names = {"--gui"}, description = "GUI 모드로 실행")
    private boolean guiMode;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (guiMode) {
            // TODO: GUI 모드 실행 (Week 3)
            System.out.println("GUI 모드는 아직 구현 중입니다.");
            return 0;
        }

        System.out.println("===========================================");
        System.out.println("  Code Flow Tracer v1.0.0");
        System.out.println("  레거시 코드 흐름 분석 도구");
        System.out.println("===========================================");
        System.out.println();
        System.out.println("프로젝트 경로: " + projectPath);

        if (urlPattern != null) {
            System.out.println("URL 패턴: " + urlPattern);
        }
        if (className != null) {
            System.out.println("클래스명: " + className);
        }
        System.out.println("출력 형식: " + format);
        System.out.println();

        // TODO: 실제 분석 로직 구현
        // 1. JavaSourceParser로 소스 파싱
        // 2. FlowAnalyzer로 호출 흐름 분석
        // 3. 출력 형식에 맞게 결과 출력

        System.out.println("[INFO] 분석을 시작합니다...");
        System.out.println("[TODO] 파싱 및 분석 로직 구현 예정");

        return 0;
    }
}
