package com.codeflow.session;

import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.ClassType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionData 테스트
 *
 * 세션 데이터 클래스의 유효성 검증 및 기본 동작을 테스트합니다.
 */
class SessionDataTest {

    @Test
    @DisplayName("기본 생성자 - 모든 필드 null")
    void testDefaultConstructor() {
        // When
        SessionData data = new SessionData();

        // Then
        assertNull(data.getProjectPath());
        assertNull(data.getAnalyzedAt());
        assertNull(data.getFlowResult());
        assertNull(data.getUrlFilter());
        assertNull(data.getOutputStyle());
    }

    @Test
    @DisplayName("파라미터 생성자 - 필수 필드 설정")
    void testParameterizedConstructor() {
        // Given
        String projectPath = "C:/test/project";
        FlowResult flowResult = createTestFlowResult();

        // When
        SessionData data = new SessionData(projectPath, flowResult);

        // Then
        assertEquals(projectPath, data.getProjectPath());
        assertNotNull(data.getAnalyzedAt(), "분석 시간이 자동 설정되어야 함");
        assertEquals(flowResult, data.getFlowResult());
        assertNull(data.getUrlFilter(), "선택 필드는 null");
        assertNull(data.getOutputStyle(), "선택 필드는 null");
    }

    @Test
    @DisplayName("isValid - 유효한 데이터")
    void testIsValid_ValidData() {
        // Given
        SessionData data = new SessionData("C:/test", createTestFlowResult());

        // When & Then
        assertTrue(data.isValid(), "유효한 데이터는 true 반환");
    }

    @Test
    @DisplayName("isValid - projectPath가 null이면 무효")
    void testIsValid_NullProjectPath() {
        // Given
        SessionData data = new SessionData();
        data.setFlowResult(createTestFlowResult());

        // When & Then
        assertFalse(data.isValid(), "projectPath가 null이면 무효");
    }

    @Test
    @DisplayName("isValid - projectPath가 빈 문자열이면 무효")
    void testIsValid_EmptyProjectPath() {
        // Given
        SessionData data = new SessionData("", createTestFlowResult());

        // When & Then
        assertFalse(data.isValid(), "projectPath가 빈 문자열이면 무효");
    }

    @Test
    @DisplayName("isValid - flowResult가 null이면 무효")
    void testIsValid_NullFlowResult() {
        // Given
        SessionData data = new SessionData();
        data.setProjectPath("C:/test");

        // When & Then
        assertFalse(data.isValid(), "flowResult가 null이면 무효");
    }

    @Test
    @DisplayName("isValid - flows 목록이 null이면 무효")
    void testIsValid_NullFlows() {
        // Given: FlowResult의 flows를 null로 설정
        FlowResult flowResult = new FlowResult("C:/test");
        flowResult.setFlows(null);  // 역직렬화 시 null이 될 수 있는 상황 시뮬레이션
        SessionData data = new SessionData("C:/test", flowResult);

        // When & Then
        // flows가 null이면 isValid()에서 false 반환해야 함
        assertFalse(data.isValid(), "flows가 null이면 무효");
    }

    @Test
    @DisplayName("Getter/Setter - 모든 필드")
    void testGettersAndSetters() {
        // Given
        SessionData data = new SessionData();
        String projectPath = "C:/test/project";
        LocalDateTime analyzedAt = LocalDateTime.now();
        FlowResult flowResult = createTestFlowResult();
        String urlFilter = "/api/*";
        String outputStyle = "detailed";

        // When
        data.setProjectPath(projectPath);
        data.setAnalyzedAt(analyzedAt);
        data.setFlowResult(flowResult);
        data.setUrlFilter(urlFilter);
        data.setOutputStyle(outputStyle);

        // Then
        assertEquals(projectPath, data.getProjectPath());
        assertEquals(analyzedAt, data.getAnalyzedAt());
        assertEquals(flowResult, data.getFlowResult());
        assertEquals(urlFilter, data.getUrlFilter());
        assertEquals(outputStyle, data.getOutputStyle());
    }

    @Test
    @DisplayName("toString - 포맷 확인")
    void testToString() {
        // Given
        SessionData data = new SessionData("C:/test/project", createTestFlowResult());

        // When
        String result = data.toString();

        // Then
        assertTrue(result.contains("SessionData"), "클래스명 포함");
        assertTrue(result.contains("C:/test/project"), "프로젝트 경로 포함");
        assertTrue(result.contains("1 flows"), "Flow 개수 포함");
    }

    @Test
    @DisplayName("toString - flowResult가 null일 때")
    void testToString_NullFlowResult() {
        // Given
        SessionData data = new SessionData();
        data.setProjectPath("C:/test");

        // When
        String result = data.toString();

        // Then
        assertTrue(result.contains("0 flows"), "null일 때 0 flows");
    }

    @Test
    @DisplayName("isValid - 빈 flows 목록도 유효")
    void testIsValid_EmptyFlows() {
        // Given: 빈 flows 목록을 가진 FlowResult
        FlowResult flowResult = new FlowResult("C:/test");
        // FlowResult 생성 시 빈 ArrayList로 초기화됨
        SessionData data = new SessionData("C:/test", flowResult);

        // When & Then
        assertTrue(data.isValid(), "빈 flows 목록도 유효 (분석 결과가 없는 경우)");
    }

    @Test
    @DisplayName("분석 시간 자동 설정 - 생성 시점 기준")
    void testAnalyzedAt_AutoSet() {
        // Given
        LocalDateTime before = LocalDateTime.now();

        // When
        SessionData data = new SessionData("C:/test", createTestFlowResult());

        // Then
        LocalDateTime after = LocalDateTime.now();
        assertNotNull(data.getAnalyzedAt());
        assertFalse(data.getAnalyzedAt().isBefore(before), "생성 시간 이후여야 함");
        assertFalse(data.getAnalyzedAt().isAfter(after), "현재 시간 이전이어야 함");
    }

    // ==================== logSizeMB 테스트 ====================

    @Test
    @DisplayName("logSizeMB 기본값 - 5MB")
    void testLogSizeMB_DefaultValue() {
        // Given
        SessionData data = new SessionData();

        // When & Then
        assertEquals(5, data.getLogSizeMB(), "기본 로그 크기는 5MB");
    }

    @Test
    @DisplayName("logSizeMB Getter/Setter")
    void testLogSizeMB_GetterSetter() {
        // Given
        SessionData data = new SessionData();

        // When
        data.setLogSizeMB(10);

        // Then
        assertEquals(10, data.getLogSizeMB(), "설정한 값으로 변경되어야 함");
    }

    @Test
    @DisplayName("logSizeMB 1MB 설정")
    void testLogSizeMB_1MB() {
        // Given
        SessionData data = new SessionData();

        // When
        data.setLogSizeMB(1);

        // Then
        assertEquals(1, data.getLogSizeMB());
    }

    @Test
    @DisplayName("logSizeMB 10MB 설정")
    void testLogSizeMB_10MB() {
        // Given
        SessionData data = new SessionData();

        // When
        data.setLogSizeMB(10);

        // Then
        assertEquals(10, data.getLogSizeMB());
    }

    @Test
    @DisplayName("logSizeMB - 파라미터 생성자에서 기본값 유지")
    void testLogSizeMB_ParameterizedConstructor() {
        // Given & When
        SessionData data = new SessionData("C:/test", createTestFlowResult());

        // Then
        assertEquals(5, data.getLogSizeMB(), "파라미터 생성자에서도 기본값 5MB");
    }

    /**
     * 테스트용 FlowResult 생성
     */
    private FlowResult createTestFlowResult() {
        FlowNode node = new FlowNode("TestController", "testMethod", ClassType.CONTROLLER);
        FlowResult result = new FlowResult("C:/test");
        result.addFlow(node);
        return result;
    }
}
