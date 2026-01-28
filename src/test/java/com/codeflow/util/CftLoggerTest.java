package com.codeflow.util;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CftLogger 테스트
 *
 * 로깅 시스템의 초기화, 로그 크기 설정, 파일 생성 등을 테스트합니다.
 */
class CftLoggerTest {

    private CftLogger logger;

    @BeforeEach
    void setUp() {
        // 싱글톤 인스턴스 가져오기
        logger = CftLogger.getInstance();
    }

    @Test
    @DisplayName("싱글톤 인스턴스 - 동일한 인스턴스 반환")
    void testSingletonInstance() {
        // When
        CftLogger instance1 = CftLogger.getInstance();
        CftLogger instance2 = CftLogger.getInstance();

        // Then
        assertSame(instance1, instance2, "싱글톤이므로 동일한 인스턴스여야 함");
    }

    @Test
    @DisplayName("로그 경로 반환 - null이 아님")
    void testGetLogPath() {
        // When
        Path logPath = logger.getLogPath();

        // Then
        assertNotNull(logPath, "로그 경로가 null이면 안 됨");
        assertTrue(logPath.toString().contains(".code-flow-tracer"), "앱 데이터 폴더 포함");
        assertTrue(logPath.toString().contains("logs"), "logs 폴더 포함");
        assertTrue(logPath.toString().endsWith("cft.log"), "파일명 확인");
    }

    @Test
    @DisplayName("로그 폴더 반환 - null이 아님")
    void testGetLogFolder() {
        // When
        Path logFolder = logger.getLogFolder();

        // Then
        assertNotNull(logFolder, "로그 폴더가 null이면 안 됨");
        assertTrue(logFolder.toString().contains(".code-flow-tracer"), "앱 데이터 폴더 포함");
        assertTrue(logFolder.toString().endsWith("logs"), "logs로 끝남");
    }

    @Test
    @DisplayName("기본 로그 크기 - 5MB")
    void testDefaultLogSize() {
        // When
        int defaultSize = CftLogger.DEFAULT_LOG_SIZE_MB;

        // Then
        assertEquals(5, defaultSize, "기본 로그 크기는 5MB");
    }

    @Test
    @DisplayName("로그 크기 상수 값 확인")
    void testLogSizeConstants() {
        // Then
        assertEquals(1, CftLogger.LOG_SIZE_1MB, "1MB 상수");
        assertEquals(5, CftLogger.LOG_SIZE_5MB, "5MB 상수");
        assertEquals(10, CftLogger.LOG_SIZE_10MB, "10MB 상수");
    }

    @Test
    @DisplayName("로그 크기 설정 - 유효한 값")
    void testSetLogSizeMB_ValidValue() {
        // Given
        int originalSize = logger.getLogSizeMB();

        try {
            // When
            logger.setLogSizeMB(10);

            // Then
            assertEquals(10, logger.getLogSizeMB(), "설정한 값으로 변경되어야 함");
        } finally {
            // 원래 값으로 복원
            logger.setLogSizeMB(originalSize);
        }
    }

    @Test
    @DisplayName("로그 크기 설정 - 최소값 제한 (1MB)")
    void testSetLogSizeMB_MinValue() {
        // Given
        int originalSize = logger.getLogSizeMB();

        try {
            // When: 0 이하 값 설정 시도
            logger.setLogSizeMB(0);

            // Then: 최소 1MB로 설정됨
            assertEquals(1, logger.getLogSizeMB(), "최소 1MB로 제한되어야 함");
        } finally {
            logger.setLogSizeMB(originalSize);
        }
    }

    @Test
    @DisplayName("로그 크기 설정 - 최대값 제한 (50MB)")
    void testSetLogSizeMB_MaxValue() {
        // Given
        int originalSize = logger.getLogSizeMB();

        try {
            // When: 50 초과 값 설정 시도
            logger.setLogSizeMB(100);

            // Then: 최대 50MB로 설정됨
            assertEquals(50, logger.getLogSizeMB(), "최대 50MB로 제한되어야 함");
        } finally {
            logger.setLogSizeMB(originalSize);
        }
    }

    @Test
    @DisplayName("로그 크기 설정 - 동일한 값이면 재초기화 안 함")
    void testSetLogSizeMB_SameValue() {
        // Given
        int currentSize = logger.getLogSizeMB();

        // When: 동일한 값 설정 (재초기화 없어야 함)
        logger.setLogSizeMB(currentSize);

        // Then: 값 유지 (내부적으로 reinitialize 호출 안 함)
        assertEquals(currentSize, logger.getLogSizeMB());
    }

    @Test
    @DisplayName("INFO 로그 기록 - 예외 없음")
    void testInfoLog() {
        // When & Then: 예외 없이 실행되어야 함
        assertDoesNotThrow(() -> logger.info("테스트 INFO 메시지"));
    }

    @Test
    @DisplayName("INFO 로그 포맷 - 예외 없음")
    void testInfoLogFormat() {
        // When & Then
        assertDoesNotThrow(() -> logger.info("테스트 %s 메시지 %d", "포맷", 123));
    }

    @Test
    @DisplayName("WARN 로그 기록 - 예외 없음")
    void testWarnLog() {
        // When & Then
        assertDoesNotThrow(() -> logger.warn("테스트 WARNING 메시지"));
    }

    @Test
    @DisplayName("WARN 로그 포맷 - 예외 없음")
    void testWarnLogFormat() {
        // When & Then
        assertDoesNotThrow(() -> logger.warn("테스트 %s 경고", "포맷"));
    }

    @Test
    @DisplayName("ERROR 로그 기록 - 예외 없음")
    void testErrorLog() {
        // When & Then
        assertDoesNotThrow(() -> logger.error("테스트 ERROR 메시지"));
    }

    @Test
    @DisplayName("ERROR 로그 예외 포함 - 예외 없음")
    void testErrorLogWithThrowable() {
        // Given
        Exception testException = new RuntimeException("테스트 예외");

        // When & Then
        assertDoesNotThrow(() -> logger.error("테스트 에러", testException));
    }

    @Test
    @DisplayName("DEBUG 로그 기록 - 예외 없음")
    void testDebugLog() {
        // When & Then
        assertDoesNotThrow(() -> logger.debug("테스트 DEBUG 메시지"));
    }

    @Test
    @DisplayName("DEBUG 로그 포맷 - 예외 없음")
    void testDebugLogFormat() {
        // When & Then
        assertDoesNotThrow(() -> logger.debug("테스트 %s 디버그 %d", "포맷", 456));
    }

    @Test
    @DisplayName("분석 시작 로그 - 예외 없음")
    void testLogAnalysisStart() {
        // Given
        Path testPath = Path.of("C:/test/project");

        // When & Then
        assertDoesNotThrow(() -> logger.logAnalysisStart(testPath));
    }

    @Test
    @DisplayName("분석 완료 로그 - 예외 없음")
    void testLogAnalysisComplete() {
        // When & Then
        assertDoesNotThrow(() -> logger.logAnalysisComplete(100, 5000L));
    }

    @Test
    @DisplayName("분석 에러 로그 - 예외 없음")
    void testLogAnalysisError() {
        // Given
        Exception testException = new RuntimeException("분석 실패");

        // When & Then
        assertDoesNotThrow(() -> logger.logAnalysisError(testException));
    }

    @Test
    @DisplayName("분석 취소 로그 - 예외 없음")
    void testLogAnalysisCancelled() {
        // When & Then
        assertDoesNotThrow(() -> logger.logAnalysisCancelled());
    }

    @Test
    @DisplayName("분석 타임아웃 로그 - 예외 없음")
    void testLogAnalysisTimeout() {
        // When & Then
        assertDoesNotThrow(() -> logger.logAnalysisTimeout(5));
    }

    @Test
    @DisplayName("로그 파일 생성 확인")
    void testLogFileCreation() throws IOException {
        // Given: 로그 기록
        logger.info("로그 파일 생성 테스트");

        // When
        Path logFolder = logger.getLogFolder();

        // Then: 로그 폴더가 존재해야 함
        assertTrue(Files.exists(logFolder), "로그 폴더가 생성되어야 함");
    }
}
