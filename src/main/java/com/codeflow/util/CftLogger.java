package com.codeflow.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Code Flow Tracer 로깅 시스템
 *
 * java.util.logging 기반으로 구현 (추가 의존성 없음, 폐쇄망 환경 고려)
 *
 * 설계 결정:
 * - SLF4J + Logback 대신 JDK 내장 로거 선택: 폐쇄망에서 추가 JAR 배포 불필요
 * - 싱글톤 패턴: 앱 전체에서 동일한 로거 인스턴스 사용
 * - 로그 위치: ~/.code-flow-tracer/logs/ (SessionManager와 동일한 앱 데이터 폴더)
 *
 * 로그 레벨:
 * - INFO: 정상 진행 상황 (파싱 시작/완료, 분석 시작/완료)
 * - WARNING: 경고 (파일 스킵, 파싱 실패 등)
 * - SEVERE: 에러 (분석 실패, 예외 발생)
 * - FINE: 디버그 (상세 진행 상황)
 */
public class CftLogger {

    private static final String LOGGER_NAME = "CodeFlowTracer";
    private static final String LOG_FOLDER = "logs";
    private static final String LOG_FILE = "cft.log";
    private static final int MAX_LOG_COUNT = 3;  // 최대 3개 파일 로테이션

    // 로그 크기 옵션 (MB 단위)
    public static final int LOG_SIZE_1MB = 1;
    public static final int LOG_SIZE_5MB = 5;
    public static final int LOG_SIZE_10MB = 10;
    public static final int DEFAULT_LOG_SIZE_MB = LOG_SIZE_5MB;

    private static CftLogger instance;
    private final Logger logger;
    private final Path logFilePath;
    private boolean initialized = false;
    private int currentLogSizeMB = DEFAULT_LOG_SIZE_MB;  // 현재 로그 크기 설정 (MB)

    /**
     * 싱글톤 인스턴스 반환
     */
    public static synchronized CftLogger getInstance() {
        if (instance == null) {
            instance = new CftLogger();
        }
        return instance;
    }

    private CftLogger() {
        logger = Logger.getLogger(LOGGER_NAME);
        logFilePath = getLogFilePath();
        initialize();
    }

    /**
     * 로그 파일 경로 결정
     * SessionManager와 동일한 위치: ~/.code-flow-tracer/logs/cft.log
     */
    private Path getLogFilePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".code-flow-tracer", LOG_FOLDER, LOG_FILE);
    }

    /**
     * 로거 초기화
     * - 로그 폴더 생성
     * - FileHandler 설정 (로테이션 포함)
     * - 포맷터 설정
     */
    private void initialize() {
        if (initialized) return;

        try {
            // 로그 폴더 생성
            Path logFolder = logFilePath.getParent();
            if (!Files.exists(logFolder)) {
                Files.createDirectories(logFolder);
            }

            // 기존 핸들러 제거 (중복 방지)
            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }

            // FileHandler 설정 (로테이션: 설정된 크기 × 3개)
            int logSizeBytes = currentLogSizeMB * 1024 * 1024;
            FileHandler fileHandler = new FileHandler(
                logFilePath.toString(),
                logSizeBytes,
                MAX_LOG_COUNT,
                true  // append
            );
            fileHandler.setFormatter(new CftLogFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);

            // 콘솔 핸들러 (개발 시 유용)
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new CftLogFormatter());
            consoleHandler.setLevel(Level.WARNING);  // 콘솔엔 WARNING 이상만
            logger.addHandler(consoleHandler);

            // 부모 로거로 전파 방지
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);

            initialized = true;
            info("로거 초기화 완료: " + logFilePath);

        } catch (IOException e) {
            // 로거 초기화 실패 시 콘솔에 출력
            System.err.println("로거 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 로그 파일 경로 반환 (UI에서 "로그 폴더 열기" 등에 사용)
     */
    public Path getLogPath() {
        return logFilePath;
    }

    /**
     * 로그 폴더 경로 반환
     */
    public Path getLogFolder() {
        return logFilePath.getParent();
    }

    /**
     * 현재 로그 크기 설정값 반환 (MB)
     */
    public int getLogSizeMB() {
        return currentLogSizeMB;
    }

    /**
     * 로그 크기 설정 변경 및 로거 재초기화
     *
     * @param sizeMB 로그 파일 크기 (MB 단위, 1/5/10 권장)
     */
    public void setLogSizeMB(int sizeMB) {
        if (sizeMB < 1) sizeMB = 1;
        if (sizeMB > 50) sizeMB = 50;  // 최대 50MB 제한

        if (this.currentLogSizeMB != sizeMB) {
            this.currentLogSizeMB = sizeMB;
            reinitialize();
            info("로그 크기 설정 변경: %dMB (최대 %dMB)", sizeMB, sizeMB * MAX_LOG_COUNT);
        }
    }

    /**
     * 로거 재초기화 (설정 변경 시 호출)
     */
    private void reinitialize() {
        closeHandlers();
        initialized = false;
        initialize();
    }

    /**
     * 모든 핸들러 닫기 (로그 파일 삭제 전 호출 필요)
     * Windows에서는 FileHandler가 파일을 잠그고 있어 닫아야 삭제 가능
     */
    public void closeHandlers() {
        for (Handler handler : logger.getHandlers()) {
            handler.close();
            logger.removeHandler(handler);
        }
        initialized = false;
    }

    // ==================== 로깅 메서드 ====================

    /**
     * INFO 레벨 로그 (정상 진행 상황)
     */
    public void info(String message) {
        ensureInitialized();
        logger.info(message);
    }

    /**
     * INFO 레벨 로그 (포맷 지원)
     */
    public void info(String format, Object... args) {
        ensureInitialized();
        logger.info(String.format(format, args));
    }

    /**
     * WARNING 레벨 로그 (경고)
     */
    public void warn(String message) {
        ensureInitialized();
        logger.warning(message);
    }

    /**
     * WARNING 레벨 로그 (포맷 지원)
     */
    public void warn(String format, Object... args) {
        ensureInitialized();
        logger.warning(String.format(format, args));
    }

    /**
     * SEVERE 레벨 로그 (에러)
     */
    public void error(String message) {
        ensureInitialized();
        logger.severe(message);
    }

    /**
     * SEVERE 레벨 로그 (예외 포함)
     * 스택트레이스를 로그 파일에 기록
     */
    public void error(String message, Throwable throwable) {
        ensureInitialized();
        logger.severe(message + "\n" + getStackTraceString(throwable));
    }

    /**
     * FINE 레벨 로그 (디버그)
     */
    public void debug(String message) {
        ensureInitialized();
        logger.fine(message);
    }

    /**
     * FINE 레벨 로그 (포맷 지원)
     */
    public void debug(String format, Object... args) {
        ensureInitialized();
        logger.fine(String.format(format, args));
    }

    /**
     * 초기화 상태 확인 및 자동 재초기화
     */
    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * 분석 시작 로그 (표준 포맷)
     */
    public void logAnalysisStart(Path projectPath) {
        info("========================================");
        info("분석 시작: %s", projectPath);
        info("시작 시간: %s", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        info("========================================");
    }

    /**
     * 분석 완료 로그 (표준 포맷)
     */
    public void logAnalysisComplete(int endpointCount, long elapsedMillis) {
        info("========================================");
        info("분석 완료: %d개 엔드포인트", endpointCount);
        info("소요 시간: %.2f초", elapsedMillis / 1000.0);
        info("========================================");
    }

    /**
     * 분석 실패 로그 (표준 포맷)
     */
    public void logAnalysisError(Throwable throwable) {
        error("========================================");
        error("분석 실패", throwable);
        error("========================================");
    }

    /**
     * 분석 취소 로그
     */
    public void logAnalysisCancelled() {
        warn("========================================");
        warn("분석 취소됨 (사용자 요청)");
        warn("========================================");
    }

    /**
     * 분석 타임아웃 로그
     */
    public void logAnalysisTimeout(int timeoutMinutes) {
        error("========================================");
        error(String.format("분석 타임아웃: %d분 초과", timeoutMinutes));
        error("========================================");
    }

    // ==================== 유틸리티 ====================

    /**
     * 예외 스택트레이스를 문자열로 변환
     */
    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 커스텀 로그 포맷터
     * 형식: [2026-01-27 14:30:45] [INFO] 메시지
     */
    private static class CftLogFormatter extends Formatter {
        private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(LocalDateTime.now().format(DATE_FORMAT));
            sb.append("] [");
            sb.append(record.getLevel().getName());
            sb.append("] ");
            sb.append(formatMessage(record));
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }
}
