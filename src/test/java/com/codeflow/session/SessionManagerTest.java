package com.codeflow.session;

import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.ClassType;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionManager 테스트
 *
 * 세션 저장/로드/삭제 기능을 테스트합니다.
 */
class SessionManagerTest {

    private SessionManager sessionManager;
    private Path testSessionDir;
    private Path testSessionFile;

    @BeforeEach
    void setUp() throws IOException {
        sessionManager = new SessionManager();

        // 테스트용 임시 세션 파일 경로
        testSessionDir = Paths.get(System.getProperty("user.home"), ".code-flow-tracer-test");
        testSessionFile = testSessionDir.resolve("session.json");

        // 기존 테스트 파일 정리
        cleanupTestFiles();
    }

    @AfterEach
    void tearDown() throws IOException {
        cleanupTestFiles();
    }

    private void cleanupTestFiles() throws IOException {
        if (Files.exists(testSessionFile)) {
            Files.delete(testSessionFile);
        }
        if (Files.exists(testSessionDir)) {
            Files.delete(testSessionDir);
        }
    }

    @Test
    @DisplayName("세션 저장 - 유효한 데이터 저장 성공")
    void testSaveSession_ValidData() {
        // Given: 유효한 세션 데이터
        SessionData data = createTestSessionData();

        // When: 세션 저장
        boolean result = sessionManager.saveSession(data);

        // Then: 저장 성공 및 파일 존재 확인
        assertTrue(result, "세션 저장이 성공해야 함");
        assertTrue(sessionManager.hasSession(), "세션 파일이 존재해야 함");
    }

    @Test
    @DisplayName("세션 저장 - null 데이터 저장 실패")
    void testSaveSession_NullData() {
        // When: null 데이터 저장 시도
        boolean result = sessionManager.saveSession((SessionData) null);

        // Then: 저장 실패
        assertFalse(result, "null 데이터는 저장 실패해야 함");
    }

    @Test
    @DisplayName("세션 로드 - 저장된 세션 복원")
    void testLoadSession_Success() {
        // Given: 세션 저장
        SessionData originalData = createTestSessionData();
        sessionManager.saveSession(originalData);

        // When: 세션 로드
        SessionData loadedData = sessionManager.loadSession();

        // Then: 데이터 복원 확인
        assertNotNull(loadedData, "로드된 데이터가 null이면 안 됨");
        assertEquals(originalData.getProjectPath(), loadedData.getProjectPath(), "프로젝트 경로 일치");
        assertEquals(originalData.getUrlFilter(), loadedData.getUrlFilter(), "URL 필터 일치");
        assertEquals(originalData.getOutputStyle(), loadedData.getOutputStyle(), "출력 스타일 일치");
        assertNotNull(loadedData.getFlowResult(), "FlowResult가 복원되어야 함");
        assertEquals(
                originalData.getFlowResult().getFlows().size(),
                loadedData.getFlowResult().getFlows().size(),
                "Flow 개수 일치"
        );
    }

    @Test
    @DisplayName("세션 로드 - 파일 없을 때 null 반환")
    void testLoadSession_NoFile() {
        // Given: 세션 파일 없음 (setUp에서 정리됨)
        sessionManager.clearSession();

        // When: 세션 로드 시도
        SessionData loadedData = sessionManager.loadSession();

        // Then: null 반환
        assertNull(loadedData, "세션 파일이 없으면 null 반환");
    }

    @Test
    @DisplayName("세션 삭제 - 파일 삭제 성공")
    void testClearSession() {
        // Given: 세션 저장
        sessionManager.saveSession(createTestSessionData());
        assertTrue(sessionManager.hasSession(), "세션 파일이 존재해야 함");

        // When: 세션 삭제
        boolean result = sessionManager.clearSession();

        // Then: 삭제 성공
        assertTrue(result, "세션 삭제가 성공해야 함");
        assertFalse(sessionManager.hasSession(), "세션 파일이 삭제되어야 함");
    }

    @Test
    @DisplayName("세션 삭제 - 파일 없어도 성공")
    void testClearSession_NoFile() {
        // Given: 세션 파일 없음
        sessionManager.clearSession();
        assertFalse(sessionManager.hasSession());

        // When: 다시 삭제 시도
        boolean result = sessionManager.clearSession();

        // Then: 성공 (예외 없음)
        assertTrue(result, "파일 없어도 삭제 성공해야 함");
    }

    @Test
    @DisplayName("세션 저장 - 오버로드 메서드 테스트")
    void testSaveSession_Overload() {
        // Given: 개별 파라미터
        String projectPath = "C:/test/project";
        FlowResult flowResult = createTestFlowResult();
        String urlFilter = "/api/*";
        String outputStyle = "detailed";

        // When: 오버로드된 메서드로 저장
        boolean result = sessionManager.saveSession(projectPath, flowResult, urlFilter, outputStyle);

        // Then: 저장 성공 및 데이터 확인
        assertTrue(result, "저장 성공");

        SessionData loaded = sessionManager.loadSession();
        assertNotNull(loaded);
        assertEquals(projectPath, loaded.getProjectPath());
        assertEquals(urlFilter, loaded.getUrlFilter());
        assertEquals(outputStyle, loaded.getOutputStyle());
    }

    @Test
    @DisplayName("세션 파일 경로 반환")
    void testGetSessionFilePath() {
        // When
        Path path = sessionManager.getSessionFilePath();

        // Then
        assertNotNull(path, "경로가 null이면 안 됨");
        assertTrue(path.toString().contains(".code-flow-tracer"), "세션 폴더 포함");
        assertTrue(path.toString().endsWith("session.json"), "파일명 확인");
    }

    @Test
    @DisplayName("LocalDateTime 직렬화/역직렬화")
    void testLocalDateTimeSerialization() {
        // Given: 특정 시간의 세션 데이터
        SessionData data = createTestSessionData();
        LocalDateTime originalTime = data.getAnalyzedAt();

        // When: 저장 후 로드
        sessionManager.saveSession(data);
        SessionData loadedData = sessionManager.loadSession();

        // Then: 시간 정보 복원 확인
        assertNotNull(loadedData.getAnalyzedAt(), "시간 정보가 복원되어야 함");
        assertEquals(
                originalTime.withNano(0),
                loadedData.getAnalyzedAt().withNano(0),
                "시간 일치 (나노초 제외)"
        );
    }

    @Test
    @DisplayName("FlowNode 트리 구조 직렬화/역직렬화")
    void testFlowNodeTreeSerialization() {
        // Given: 중첩된 FlowNode 구조
        FlowNode root = new FlowNode("UserController", "getUser", ClassType.CONTROLLER);
        FlowNode child1 = new FlowNode("UserService", "findUser", ClassType.SERVICE);
        FlowNode child2 = new FlowNode("UserDAO", "selectUser", ClassType.DAO);
        root.addChild(child1);
        child1.addChild(child2);

        FlowResult flowResult = new FlowResult("C:/test");
        flowResult.addFlow(root);
        SessionData data = new SessionData("C:/test", flowResult);

        // When: 저장 후 로드
        sessionManager.saveSession(data);
        SessionData loaded = sessionManager.loadSession();

        // Then: 트리 구조 복원 확인
        assertNotNull(loaded);
        assertEquals(1, loaded.getFlowResult().getFlows().size());

        FlowNode loadedRoot = loaded.getFlowResult().getFlows().get(0);
        assertEquals("UserController", loadedRoot.getClassName());
        assertEquals(1, loadedRoot.getChildren().size());

        FlowNode loadedChild1 = loadedRoot.getChildren().get(0);
        assertEquals("UserService", loadedChild1.getClassName());
        assertEquals(1, loadedChild1.getChildren().size());

        FlowNode loadedChild2 = loadedChild1.getChildren().get(0);
        assertEquals("UserDAO", loadedChild2.getClassName());
    }

    // ==================== logSizeMB 테스트 ====================

    @Test
    @DisplayName("logSizeMB 저장/로드 - 기본값 5MB")
    void testLogSizeMB_DefaultValue() {
        // Given: 로그 크기 설정하지 않은 세션 데이터
        SessionData data = createTestSessionData();

        // When: 저장 후 로드
        sessionManager.saveSession(data);
        SessionData loaded = sessionManager.loadSession();

        // Then: 기본값 5MB
        assertNotNull(loaded);
        assertEquals(5, loaded.getLogSizeMB(), "기본 로그 크기 5MB");
    }

    @Test
    @DisplayName("logSizeMB 저장/로드 - 커스텀 값")
    void testLogSizeMB_CustomValue() {
        // Given: 로그 크기 10MB로 설정
        SessionData data = createTestSessionData();
        data.setLogSizeMB(10);

        // When: 저장 후 로드
        sessionManager.saveSession(data);
        SessionData loaded = sessionManager.loadSession();

        // Then: 설정한 값 유지
        assertNotNull(loaded);
        assertEquals(10, loaded.getLogSizeMB(), "설정한 로그 크기 10MB");
    }

    @Test
    @DisplayName("logSizeMB 저장/로드 - 1MB 설정")
    void testLogSizeMB_1MB() {
        // Given
        SessionData data = createTestSessionData();
        data.setLogSizeMB(1);

        // When
        sessionManager.saveSession(data);
        SessionData loaded = sessionManager.loadSession();

        // Then
        assertEquals(1, loaded.getLogSizeMB());
    }

    @Test
    @DisplayName("logSizeMB - 기존 세션에서 설정 유지")
    void testLogSizeMB_PreservedOnSessionSave() {
        // Given: 로그 크기 10MB로 설정 후 저장
        SessionData data = createTestSessionData();
        data.setLogSizeMB(10);
        sessionManager.saveSession(data);

        // When: 새 세션 저장 (오버로드 메서드 사용)
        sessionManager.saveSession("C:/new/project", createTestFlowResult(), "/api/*", "normal");

        // Then: 로그 크기 설정이 유지되어야 함
        SessionData loaded = sessionManager.loadSession();
        assertNotNull(loaded);
        assertEquals(10, loaded.getLogSizeMB(), "기존 로그 크기 설정이 유지되어야 함");
    }

    @Test
    @DisplayName("logSizeMB - saveSettings에서 유지")
    void testLogSizeMB_PreservedOnSaveSettings() {
        // Given: 로그 크기 10MB로 설정 후 저장
        SessionData data = createTestSessionData();
        data.setLogSizeMB(10);
        sessionManager.saveSession(data);

        // When: 설정만 저장
        List<String> recentPaths = new ArrayList<>();
        recentPaths.add("C:/new/path");
        sessionManager.saveSettings(recentPaths, "/api/*", "detailed", null, null);

        // Then: 로그 크기 설정이 유지되어야 함
        SessionData loaded = sessionManager.loadSettings();
        assertNotNull(loaded);
        assertEquals(10, loaded.getLogSizeMB(), "saveSettings 후에도 로그 크기 유지");
    }

    /**
     * 테스트용 SessionData 생성
     */
    private SessionData createTestSessionData() {
        FlowResult flowResult = createTestFlowResult();
        SessionData data = new SessionData("C:/test/project", flowResult);
        data.setUrlFilter("/api/user/*");
        data.setOutputStyle("normal");
        return data;
    }

    /**
     * 테스트용 FlowResult 생성
     */
    private FlowResult createTestFlowResult() {
        FlowNode node = new FlowNode("TestController", "testMethod", ClassType.CONTROLLER);
        node.setUrlMapping("/api/test");
        node.setHttpMethod("GET");

        FlowResult result = new FlowResult("C:/test");
        result.addFlow(node);
        return result;
    }
}
