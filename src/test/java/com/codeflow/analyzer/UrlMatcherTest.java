package com.codeflow.analyzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UrlMatcher 테스트
 */
class UrlMatcherTest {

    @Test
    @DisplayName("정확한 URL 매칭 테스트")
    void testExactMatch() {
        assertTrue(UrlMatcher.matches("/user/list.do", "/user/list.do"));
        assertFalse(UrlMatcher.matches("/user/list.do", "/user/detail.do"));
        assertFalse(UrlMatcher.matches("/user/list.do", "/admin/list.do"));
    }

    @Test
    @DisplayName("와일드카드 * 매칭 테스트")
    void testSingleWildcard() {
        // /user/* 패턴 - /user/ 하위 1단계만 매칭
        assertTrue(UrlMatcher.matches("/user/list.do", "/user/*"));
        assertTrue(UrlMatcher.matches("/user/detail.do", "/user/*"));
        assertTrue(UrlMatcher.matches("/user/123", "/user/*"));

        // 2단계 이상은 매칭 안됨 (* 는 슬래시 제외)
        assertFalse(UrlMatcher.matches("/user/sub/list.do", "/user/*"));

        // *.do 패턴
        assertTrue(UrlMatcher.matches("/user/list.do", "/user/*.do"));
        assertFalse(UrlMatcher.matches("/user/list.jsp", "/user/*.do"));
    }

    @Test
    @DisplayName("와일드카드 ** 매칭 테스트")
    void testDoubleWildcard() {
        // /user/** 패턴 - /user/ 하위 모든 깊이 매칭
        assertTrue(UrlMatcher.matches("/user/list.do", "/user/**"));
        assertTrue(UrlMatcher.matches("/user/sub/list.do", "/user/**"));
        assertTrue(UrlMatcher.matches("/user/a/b/c/d", "/user/**"));
    }

    @Test
    @DisplayName("PathVariable {변수} 매칭 테스트")
    void testPathVariable() {
        // /user/{id} 패턴
        assertTrue(UrlMatcher.matches("/user/123", "/user/{id}"));
        assertTrue(UrlMatcher.matches("/user/abc", "/user/{id}"));
        assertTrue(UrlMatcher.matches("/user/user-name", "/user/{id}"));

        // 경로 구조가 다르면 매칭 안됨
        assertFalse(UrlMatcher.matches("/user/123/detail", "/user/{id}"));
        assertFalse(UrlMatcher.matches("/admin/123", "/user/{id}"));

        // 복합 PathVariable
        assertTrue(UrlMatcher.matches("/user/123/order/456", "/user/{userId}/order/{orderId}"));
    }

    @Test
    @DisplayName("부분 매칭 테스트")
    void testContainsMatch() {
        // URL에 패턴이 포함되면 매칭
        assertTrue(UrlMatcher.matches("/user/list.do", "user"));
        assertTrue(UrlMatcher.matches("/user/list.do", "list"));
        assertTrue(UrlMatcher.matches("/admin/user/detail.do", "user"));

        // 포함되지 않으면 매칭 안됨
        assertFalse(UrlMatcher.matches("/admin/list.do", "user"));
    }

    @ParameterizedTest
    @DisplayName("다양한 매칭 시나리오 테스트")
    @CsvSource({
        "/user/list.do, /user/list.do, true",
        "/user/list.do, /user/*, true",
        "/user/sub/list.do, /user/**, true",
        "/user/123, /user/{id}, true",
        "/user/list.do, list, true",
        "/user/list.do, admin, false",
        "'', /user/*, false",
        "/user/list.do, '', false"
    })
    void testVariousPatterns(String url, String pattern, boolean expected) {
        assertEquals(expected, UrlMatcher.matches(url, pattern));
    }

    @Test
    @DisplayName("PathVariable 값 추출 테스트")
    void testExtractPathVariable() {
        // 단일 변수 추출
        assertEquals("123", UrlMatcher.extractPathVariable("/user/123", "/user/{id}", "id"));
        assertEquals("abc", UrlMatcher.extractPathVariable("/user/abc", "/user/{id}", "id"));

        // 복합 변수 추출
        assertEquals("123", UrlMatcher.extractPathVariable("/user/123/order/456",
            "/user/{userId}/order/{orderId}", "userId"));
        assertEquals("456", UrlMatcher.extractPathVariable("/user/123/order/456",
            "/user/{userId}/order/{orderId}", "orderId"));

        // 매칭 안되면 null
        assertNull(UrlMatcher.extractPathVariable("/admin/123", "/user/{id}", "id"));
        assertNull(UrlMatcher.extractPathVariable("/user/123", "/user/{id}", "wrongName"));
    }

    @Test
    @DisplayName("패턴 타입 판별 테스트")
    void testPatternType() {
        assertEquals(UrlMatcher.PatternType.EXACT, UrlMatcher.getPatternType("/user/list.do"));
        assertEquals(UrlMatcher.PatternType.WILDCARD, UrlMatcher.getPatternType("/user/*"));
        assertEquals(UrlMatcher.PatternType.WILDCARD, UrlMatcher.getPatternType("/user/**"));
        assertEquals(UrlMatcher.PatternType.PATH_VARIABLE, UrlMatcher.getPatternType("/user/{id}"));
        assertEquals(UrlMatcher.PatternType.CONTAINS, UrlMatcher.getPatternType("user"));
        assertEquals(UrlMatcher.PatternType.INVALID, UrlMatcher.getPatternType(""));
        assertEquals(UrlMatcher.PatternType.INVALID, UrlMatcher.getPatternType(null));
    }

    @Test
    @DisplayName("null 및 빈 값 처리 테스트")
    void testNullAndEmpty() {
        assertFalse(UrlMatcher.matches(null, "/user/*"));
        assertFalse(UrlMatcher.matches("/user/list.do", null));
        assertFalse(UrlMatcher.matches(null, null));
        assertFalse(UrlMatcher.matches("", "/user/*"));
        assertFalse(UrlMatcher.matches("/user/list.do", ""));
        assertFalse(UrlMatcher.matches("  ", "/user/*"));
    }
}
