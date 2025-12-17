package com.codeflow.analyzer;

import com.codeflow.parser.ClassType;
import com.codeflow.parser.JavaSourceParser;
import com.codeflow.parser.ParsedClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowAnalyzer 테스트
 *
 * samples/ 폴더의 샘플 코드를 사용하여 호출 흐름 분석을 테스트합니다.
 */
class FlowAnalyzerTest {

    private JavaSourceParser parser;
    private FlowAnalyzer analyzer;
    private Path samplesPath;

    @BeforeEach
    void setUp() {
        parser = new JavaSourceParser();
        analyzer = new FlowAnalyzer();
        samplesPath = Paths.get("samples");
    }

    @Test
    @DisplayName("전체 프로젝트 분석 - Controller → Service → DAO 흐름 추적")
    void testAnalyzeProject() throws IOException {
        // Given: samples 폴더의 모든 Java 파일 파싱
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);
        assertFalse(parsedClasses.isEmpty(), "파싱된 클래스가 있어야 함");

        // When: 호출 흐름 분석
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // Then: 분석 결과 검증
        assertNotNull(result, "분석 결과가 null이면 안 됨");
        assertTrue(result.getTotalClasses() > 0, "분석된 클래스가 있어야 함");
        assertTrue(result.getControllerCount() > 0, "Controller가 있어야 함");
        assertTrue(result.getServiceCount() > 0, "Service가 있어야 함");
        assertTrue(result.getDaoCount() > 0, "DAO가 있어야 함");

        // 결과 출력
        System.out.println(result.toTreeString());
    }

    @Test
    @DisplayName("엔드포인트 분석 - URL 매핑이 있는 메서드 추적")
    void testEndpointAnalysis() throws IOException {
        // Given
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // When
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // Then: 엔드포인트가 있어야 함
        assertTrue(result.getEndpointCount() > 0, "엔드포인트가 있어야 함");

        // 각 플로우 노드 검증
        for (FlowNode flow : result.getFlows()) {
            assertEquals(ClassType.CONTROLLER, flow.getClassType(), "최상위 노드는 Controller여야 함");
            assertNotNull(flow.getClassName(), "클래스명이 있어야 함");
            assertNotNull(flow.getMethodName(), "메서드명이 있어야 함");
        }

        System.out.println("=== 엔드포인트 목록 ===");
        for (FlowNode flow : result.getFlows()) {
            System.out.printf("[%s %s] %s.%s()%n",
                flow.getHttpMethod(),
                flow.getUrlMapping(),
                flow.getClassName(),
                flow.getMethodName());
        }
    }

    @Test
    @DisplayName("인터페이스 → 구현체 매핑 테스트")
    void testInterfaceToImplMapping() throws IOException {
        // Given
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // When
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // Then: UserService → UserServiceImpl 매핑 확인
        boolean foundServiceImpl = false;
        for (FlowNode flow : result.getFlows()) {
            foundServiceImpl = checkForServiceImpl(flow);
            if (foundServiceImpl) break;
        }

        System.out.println("=== 서비스 구현체 매핑 확인 ===");
        for (FlowNode flow : result.getFlows()) {
            printServiceNodes(flow, 0);
        }
    }

    private boolean checkForServiceImpl(FlowNode node) {
        if (node.getClassName() != null && node.getClassName().endsWith("Impl")) {
            return true;
        }
        for (FlowNode child : node.getChildren()) {
            if (checkForServiceImpl(child)) {
                return true;
            }
        }
        return false;
    }

    private void printServiceNodes(FlowNode node, int depth) {
        if (node.getClassType() == ClassType.SERVICE) {
            String indent = "  ".repeat(depth);
            System.out.printf("%s[SERVICE] %s.%s()%n", indent, node.getClassName(), node.getMethodName());
        }
        for (FlowNode child : node.getChildren()) {
            printServiceNodes(child, depth + 1);
        }
    }

    @Test
    @DisplayName("호출 흐름 트리 구조 테스트")
    void testFlowTreeStructure() throws IOException {
        // Given
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // When
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // Then: 트리 구조 검증
        for (FlowNode flow : result.getFlows()) {
            assertEquals(0, flow.getDepth(), "루트 노드 depth는 0이어야 함");

            // 자식 노드의 depth 검증
            for (FlowNode child : flow.getChildren()) {
                assertEquals(1, child.getDepth(), "1단계 자식 depth는 1이어야 함");

                for (FlowNode grandChild : child.getChildren()) {
                    assertEquals(2, grandChild.getDepth(), "2단계 자식 depth는 2이어야 함");
                }
            }
        }

        System.out.println("=== 트리 구조 출력 ===");
        System.out.println(result.toTreeString());
    }

    @Test
    @DisplayName("URL 패턴으로 필터링 분석")
    void testAnalyzeByUrl() throws IOException {
        // Given
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // When: /list 패턴으로 필터링
        FlowResult result = analyzer.analyzeByUrl(samplesPath, parsedClasses, "list");

        // Then: 필터링된 결과 확인
        System.out.println("=== URL 패턴 'list' 필터링 결과 ===");
        System.out.println("매칭된 엔드포인트: " + result.getFlows().size() + "개");
        for (FlowNode flow : result.getFlows()) {
            System.out.println(flow.toTreeString());
        }
    }

    @Test
    @DisplayName("FlowNode 트리 출력 테스트")
    void testFlowNodeTreeString() {
        // Given: 수동으로 FlowNode 트리 생성
        FlowNode controller = new FlowNode("UserController", "selectUserList", ClassType.CONTROLLER);
        controller.setUrlMapping("/user/list.do");
        controller.setHttpMethod("GET");

        FlowNode service = new FlowNode("UserServiceImpl", "selectUserList", ClassType.SERVICE);
        controller.addChild(service);

        FlowNode dao = new FlowNode("UserDAO", "selectUserList", ClassType.DAO);
        dao.setSqlId("userDAO.selectUserList");
        service.addChild(dao);

        // When
        String treeString = controller.toTreeString();

        // Then
        assertNotNull(treeString);
        assertTrue(treeString.contains("UserController"));
        assertTrue(treeString.contains("UserServiceImpl"));
        assertTrue(treeString.contains("UserDAO"));
        assertTrue(treeString.contains("GET"));
        assertTrue(treeString.contains("/user/list.do"));

        System.out.println("=== FlowNode 트리 출력 ===");
        System.out.println(treeString);
    }

    @Test
    @DisplayName("implements 기반 인터페이스 매핑 테스트")
    void testImplementsBasedMapping() throws IOException {
        // Given
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // 파싱된 클래스에서 implements 정보 확인
        System.out.println("=== 파싱된 클래스의 implements 정보 ===");
        for (ParsedClass clazz : parsedClasses) {
            if (!clazz.getImplementedInterfaces().isEmpty()) {
                System.out.printf("%s implements %s%n",
                    clazz.getClassName(),
                    clazz.getImplementedInterfaces());
            }
        }

        // UserServiceImpl이 UserService를 구현하는지 확인
        boolean foundImplements = false;
        for (ParsedClass clazz : parsedClasses) {
            if (clazz.getClassName().equals("UserServiceImpl")) {
                assertTrue(clazz.getImplementedInterfaces().contains("UserService"),
                    "UserServiceImpl은 UserService를 구현해야 함");
                foundImplements = true;
            }
        }
        assertTrue(foundImplements, "UserServiceImpl 클래스가 있어야 함");

        // When
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // Then: 분석 결과에서 UserServiceImpl이 정상적으로 매핑되었는지 확인
        boolean foundServiceImplInFlow = false;
        for (FlowNode flow : result.getFlows()) {
            foundServiceImplInFlow = checkForClassName(flow, "UserServiceImpl");
            if (foundServiceImplInFlow) break;
        }
        assertTrue(foundServiceImplInFlow, "분석 결과에 UserServiceImpl이 있어야 함");
    }

    private boolean checkForClassName(FlowNode node, String className) {
        if (className.equals(node.getClassName())) {
            return true;
        }
        for (FlowNode child : node.getChildren()) {
            if (checkForClassName(child, className)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("FlowResult 요약 정보 테스트")
    void testFlowResultSummary() throws IOException {
        // Given
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // When
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // Then
        String summary = result.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("분석 결과 요약"));
        assertTrue(summary.contains("Controller"));
        assertTrue(summary.contains("Service"));
        assertTrue(summary.contains("DAO"));

        System.out.println(summary);
    }
}
