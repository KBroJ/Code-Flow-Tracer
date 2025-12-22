package com.codeflow;

import com.codeflow.analyzer.FlowAnalyzer;
import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.output.ConsoleOutput;
import com.codeflow.parser.JavaSourceParser;
import com.codeflow.parser.ParsedClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트
 *
 * 전체 파이프라인 테스트: JavaSourceParser → FlowAnalyzer → ConsoleOutput
 */
@DisplayName("통합 테스트")
class IntegrationTest {

    private Path samplesPath;
    private JavaSourceParser parser;
    private FlowAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        samplesPath = Paths.get("samples");
        parser = new JavaSourceParser();
        analyzer = new FlowAnalyzer();
    }

    @Test
    @DisplayName("전체 파이프라인: 파싱 → 분석 → 출력")
    void testFullPipeline() throws IOException {
        // 1. 파싱
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);
        assertFalse(parsedClasses.isEmpty(), "파싱된 클래스가 있어야 함");

        // 2. 분석
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);
        assertNotNull(result, "분석 결과가 있어야 함");
        assertTrue(result.getEndpointCount() > 0, "엔드포인트가 있어야 함");

        // 3. 출력 (문자열로 캡처)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
        ConsoleOutput output = new ConsoleOutput(ps, false, ConsoleOutput.OutputStyle.NORMAL);
        output.print(result);

        String outputStr = baos.toString(StandardCharsets.UTF_8);
        assertFalse(outputStr.isEmpty(), "출력이 있어야 함");
        assertTrue(outputStr.contains("호출 흐름"), "출력에 '호출 흐름'이 포함되어야 함");
        assertTrue(outputStr.contains("UserController"), "출력에 'UserController'가 포함되어야 함");
    }

    @Test
    @DisplayName("URL 필터링 테스트")
    void testUrlFiltering() throws IOException {
        // 파싱
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // 전체 분석
        FlowResult fullResult = analyzer.analyze(samplesPath, parsedClasses);
        int fullCount = fullResult.getEndpointCount();

        // URL 필터링 분석
        FlowResult filteredResult = analyzer.analyzeByUrl(samplesPath, parsedClasses, "/user/list*");
        int filteredCount = filteredResult.getEndpointCount();

        assertTrue(fullCount > filteredCount, "필터링 후 엔드포인트 수가 줄어야 함");
        assertEquals(1, filteredCount, "/user/list* 패턴은 1개 매칭되어야 함");
    }

    @Test
    @DisplayName("호출 흐름 구조 검증")
    void testFlowStructure() throws IOException {
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // selectUserList 엔드포인트 찾기
        FlowNode listFlow = result.getFlows().stream()
                .filter(f -> f.getMethodName().equals("selectUserList"))
                .findFirst()
                .orElse(null);

        assertNotNull(listFlow, "selectUserList 흐름이 있어야 함");
        assertEquals("UserController", listFlow.getClassName());
        assertEquals("/user/list.do", listFlow.getUrlMapping());
        assertEquals("GET", listFlow.getHttpMethod());

        // 하위 호출 확인 (Controller → Service → DAO)
        assertFalse(listFlow.getChildren().isEmpty(), "하위 호출이 있어야 함");

        FlowNode serviceCall = listFlow.getChildren().get(0);
        assertEquals("UserServiceImpl", serviceCall.getClassName());

        assertFalse(serviceCall.getChildren().isEmpty(), "Service에서 DAO 호출이 있어야 함");
        FlowNode daoCall = serviceCall.getChildren().get(0);
        assertEquals("UserDAO", daoCall.getClassName());
    }

    @Test
    @DisplayName("출력 스타일 테스트 - COMPACT")
    void testCompactStyle() throws IOException {
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
        ConsoleOutput output = new ConsoleOutput(ps, false, ConsoleOutput.OutputStyle.COMPACT);
        output.print(result);

        String outputStr = baos.toString(StandardCharsets.UTF_8);

        // COMPACT 스타일에서는 URL이 표시되지 않음
        assertTrue(outputStr.contains("UserController"), "클래스명은 표시되어야 함");
        // COMPACT에서도 타입 태그는 표시됨
        assertTrue(outputStr.contains("[Controller]"), "타입 태그는 표시되어야 함");
    }

    @Test
    @DisplayName("출력 스타일 테스트 - DETAILED")
    void testDetailedStyle() throws IOException {
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
        ConsoleOutput output = new ConsoleOutput(ps, false, ConsoleOutput.OutputStyle.DETAILED);
        output.print(result);

        String outputStr = baos.toString(StandardCharsets.UTF_8);

        // URL은 "클래스URL + 메서드URL" 형태로 표시됨
        assertTrue(outputStr.contains("/user") && outputStr.contains("/list.do"), "URL이 표시되어야 함");
        assertTrue(outputStr.contains("[GET]"), "HTTP 메서드가 표시되어야 함");
    }

    @Test
    @DisplayName("통계 정보 검증")
    void testStatistics() throws IOException {
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        assertEquals(13, result.getTotalClasses(), "전체 클래스 13개");
        assertEquals(2, result.getControllerCount(), "Controller 2개 (User + Order)");
        assertEquals(4, result.getServiceCount(), "Service 4개 (2 인터페이스 + 2 Impl)");
        assertEquals(7, result.getDaoCount(), "DAO 7개");
        assertEquals(11, result.getEndpointCount(), "엔드포인트 11개 (User 5 + Order 6)");
    }
}
