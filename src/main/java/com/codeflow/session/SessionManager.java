package com.codeflow.session;

import com.codeflow.analyzer.FlowResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 세션 관리자
 *
 * 분석 결과를 JSON 파일로 저장하고 불러오는 기능을 제공합니다.
 * 앱 종료 후 재시작해도 마지막 분석 결과를 유지할 수 있습니다.
 */
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    // 세션 파일 경로: ~/.code-flow-tracer/session.json
    private static final Path SESSION_DIR = Paths.get(
            System.getProperty("user.home"), ".code-flow-tracer");
    private static final Path SESSION_FILE = SESSION_DIR.resolve("session.json");

    private final Gson gson;

    public SessionManager() {
        this.gson = createGson();
    }

    /**
     * Gson 인스턴스 생성 (LocalDateTime 지원 포함)
     */
    private Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * 세션 저장
     *
     * @param data 저장할 세션 데이터
     * @return 저장 성공 여부
     */
    public boolean saveSession(SessionData data) {
        if (data == null) {
            log.warn("세션 데이터가 null입니다.");
            return false;
        }

        try {
            // 디렉토리 생성
            if (!Files.exists(SESSION_DIR)) {
                Files.createDirectories(SESSION_DIR);
                log.info("세션 디렉토리 생성: {}", SESSION_DIR);
            }

            // JSON으로 직렬화 및 저장
            String json = gson.toJson(data);
            Files.writeString(SESSION_FILE, json, StandardCharsets.UTF_8);

            log.info("세션 저장 완료: {} ({} flows)",
                    SESSION_FILE,
                    data.getFlowResult() != null ? data.getFlowResult().getFlows().size() : 0);
            return true;

        } catch (IOException e) {
            log.error("세션 저장 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 세션 불러오기 (분석 결과 포함)
     *
     * @return 저장된 세션 데이터, 없거나 오류 시 null
     */
    public SessionData loadSession() {
        if (!Files.exists(SESSION_FILE)) {
            log.debug("세션 파일 없음: {}", SESSION_FILE);
            return null;
        }

        try {
            String json = Files.readString(SESSION_FILE, StandardCharsets.UTF_8);
            SessionData data = gson.fromJson(json, SessionData.class);

            if (data != null && data.isValid()) {
                log.info("세션 로드 완료: {} ({} flows)",
                        data.getProjectPath(),
                        data.getFlowResult().getFlows().size());
                return data;
            } else {
                log.warn("유효하지 않은 세션 데이터");
                return null;
            }

        } catch (Exception e) {
            log.error("세션 로드 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 설정만 불러오기 (분석 결과 없어도 됨)
     *
     * @return 저장된 세션 데이터 (설정만), 없거나 오류 시 null
     */
    public SessionData loadSettings() {
        if (!Files.exists(SESSION_FILE)) {
            log.debug("세션 파일 없음: {}", SESSION_FILE);
            return null;
        }

        try {
            String json = Files.readString(SESSION_FILE, StandardCharsets.UTF_8);
            SessionData data = gson.fromJson(json, SessionData.class);

            if (data != null) {
                log.info("설정 로드 완료");
                return data;
            } else {
                return null;
            }

        } catch (Exception e) {
            log.error("설정 로드 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 설정만 저장 (분석 결과는 유지)
     */
    public boolean saveSettings(java.util.List<String> recentPaths, String urlFilter,
                                String outputStyle, String endpointFilter, java.util.List<String> sqlTypeFilter) {
        // 기존 세션 로드 (분석 결과 유지를 위해)
        SessionData data = loadSettings();
        if (data == null) {
            data = new SessionData();
        }

        data.setRecentPaths(recentPaths);
        data.setUrlFilter(urlFilter);
        data.setOutputStyle(outputStyle);
        data.setEndpointFilter(endpointFilter);
        data.setSqlTypeFilter(sqlTypeFilter);

        return saveSession(data);
    }

    /**
     * 세션 삭제
     *
     * @return 삭제 성공 여부
     */
    public boolean clearSession() {
        try {
            if (Files.exists(SESSION_FILE)) {
                Files.delete(SESSION_FILE);
                log.info("세션 삭제 완료: {}", SESSION_FILE);
            }
            return true;
        } catch (IOException e) {
            log.error("세션 삭제 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 세션 파일 존재 여부 확인
     */
    public boolean hasSession() {
        return Files.exists(SESSION_FILE);
    }

    /**
     * 세션 파일 경로 반환
     */
    public Path getSessionFilePath() {
        return SESSION_FILE;
    }

    /**
     * 빠른 세션 저장 (FlowResult로부터)
     * 기존 설정(recentPaths, endpointFilter)은 유지
     */
    public boolean saveSession(String projectPath, FlowResult result, String urlFilter, String outputStyle) {
        // 기존 설정 로드 (recentPaths, endpointFilter 유지를 위해)
        SessionData existing = loadSettings();

        SessionData data = new SessionData(projectPath, result);
        data.setUrlFilter(urlFilter);
        data.setOutputStyle(outputStyle);

        // 기존 설정 유지
        if (existing != null) {
            if (existing.getRecentPaths() != null) {
                data.setRecentPaths(existing.getRecentPaths());
            }
            if (existing.getEndpointFilter() != null) {
                data.setEndpointFilter(existing.getEndpointFilter());
            }
        }

        return saveSession(data);
    }

    /**
     * LocalDateTime TypeAdapter for Gson
     */
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(FORMATTER.format(value));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            String value = in.nextString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(value, FORMATTER);
        }
    }
}
