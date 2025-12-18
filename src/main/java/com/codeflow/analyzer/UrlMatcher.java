package com.codeflow.analyzer;

import java.util.regex.Pattern;

/**
 * URL 패턴 매칭 유틸리티
 *
 * 다양한 URL 패턴 형태를 지원합니다:
 * - 정확한 매칭: "/user/list.do"
 * - 와일드카드: "/user/*" (user 하위 모든 경로)
 * - PathVariable: "/user/{id}" ({id}는 어떤 값이든 매칭)
 * - 부분 매칭: "user" (URL에 user가 포함되면 매칭)
 */
public class UrlMatcher {

    /**
     * URL이 패턴과 매칭되는지 확인합니다.
     *
     * @param url 실제 URL (예: "/user/list.do")
     * @param pattern 매칭 패턴 (예: "/user/*")
     * @return 매칭 여부
     */
    public static boolean matches(String url, String pattern) {
        if (url == null || pattern == null) {
            return false;
        }

        url = url.trim();
        pattern = pattern.trim();

        if (url.isEmpty() || pattern.isEmpty()) {
            return false;
        }

        // 1. 정확한 매칭
        if (url.equals(pattern)) {
            return true;
        }

        // 2. 와일드카드 패턴 (* 또는 **)
        if (pattern.contains("*")) {
            return matchWildcard(url, pattern);
        }

        // 3. PathVariable 패턴 ({변수명})
        if (pattern.contains("{") && pattern.contains("}")) {
            return matchPathVariable(url, pattern);
        }

        // 4. 부분 매칭 (패턴이 URL에 포함되는지)
        return url.contains(pattern);
    }

    /**
     * 와일드카드 패턴 매칭
     *
     * - "/user/*" : /user/ 하위 1단계
     * - "/user/**" : /user/ 하위 모든 깊이
     * - "*.do" : .do로 끝나는 모든 URL
     */
    private static boolean matchWildcard(String url, String pattern) {
        // ** (모든 경로) 처리
        if (pattern.equals("**")) {
            return true;
        }

        // 패턴을 정규식으로 변환
        String regex = pattern
            .replace(".", "\\.")      // . -> \.
            .replace("**", "§§§")     // ** 임시 치환
            .replace("*", "[^/]*")    // * -> 슬래시 제외 모든 문자
            .replace("§§§", ".*");    // ** -> 모든 문자

        // 앵커 추가 (전체 매칭)
        regex = "^" + regex + "$";

        try {
            return Pattern.matches(regex, url);
        } catch (Exception e) {
            // 정규식 오류 시 단순 포함 검사로 폴백
            return url.contains(pattern.replace("*", ""));
        }
    }

    /**
     * PathVariable 패턴 매칭
     *
     * "/user/{id}" 는 "/user/123", "/user/abc" 등과 매칭
     */
    private static boolean matchPathVariable(String url, String pattern) {
        // {변수명}을 [^/]+ 로 변환 (슬래시 제외 1개 이상 문자)
        String regex = pattern
            .replace(".", "\\.")
            .replaceAll("\\{[^}]+\\}", "[^/]+");

        regex = "^" + regex + "$";

        try {
            return Pattern.matches(regex, url);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * URL에서 PathVariable 값을 추출합니다.
     *
     * @param url 실제 URL (예: "/user/123")
     * @param pattern 패턴 (예: "/user/{id}")
     * @param variableName 추출할 변수명 (예: "id")
     * @return 추출된 값 (예: "123") 또는 null
     */
    public static String extractPathVariable(String url, String pattern, String variableName) {
        if (!matches(url, pattern)) {
            return null;
        }

        String[] urlParts = url.split("/");
        String[] patternParts = pattern.split("/");

        if (urlParts.length != patternParts.length) {
            return null;
        }

        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            if (patternPart.equals("{" + variableName + "}")) {
                return urlParts[i];
            }
        }

        return null;
    }

    /**
     * 패턴 타입을 판별합니다.
     */
    public static PatternType getPatternType(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return PatternType.INVALID;
        }

        if (pattern.contains("*")) {
            return PatternType.WILDCARD;
        }

        if (pattern.contains("{") && pattern.contains("}")) {
            return PatternType.PATH_VARIABLE;
        }

        if (pattern.startsWith("/")) {
            return PatternType.EXACT;
        }

        return PatternType.CONTAINS;
    }

    /**
     * 패턴 타입 열거형
     */
    public enum PatternType {
        EXACT,          // 정확한 매칭 ("/user/list.do")
        WILDCARD,       // 와일드카드 ("/user/*")
        PATH_VARIABLE,  // PathVariable ("/user/{id}")
        CONTAINS,       // 부분 매칭 ("user")
        INVALID         // 잘못된 패턴
    }
}
