package com.codeflow.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaSourceParser 테스트
 */
class JavaSourceParserTest {

    private JavaSourceParser parser;
    private Path samplesPath;

    @BeforeEach
    void setUp() {
        parser = new JavaSourceParser();
        // 프로젝트 루트의 samples 폴더
        samplesPath = Paths.get("samples");
    }

    @Test
    @DisplayName("Controller 클래스 파싱 테스트")
    void testParseController() throws Exception {
        Path controllerPath = samplesPath.resolve("UserController.java");

        ParsedClass parsed = parser.parseFile(controllerPath);

        assertNotNull(parsed, "파싱 결과가 null이면 안됨");
        assertEquals("UserController", parsed.getClassName());
        assertEquals(ClassType.CONTROLLER, parsed.getClassType());

        // 메서드 확인
        assertFalse(parsed.getMethods().isEmpty(), "메서드가 파싱되어야 함");

        System.out.println("=== Controller 파싱 결과 ===");
        System.out.println(parsed);
        for (ParsedMethod method : parsed.getMethods()) {
            System.out.println("  - " + method);

            // 메서드 호출 출력
            for (MethodCall call : method.getMethodCalls()) {
                if (call.isServiceOrDaoCall()) {
                    System.out.println("      -> " + call);
                }
            }
        }
    }

    @Test
    @DisplayName("Service 구현체 클래스 파싱 테스트")
    void testParseServiceImpl() throws Exception {
        Path servicePath = samplesPath.resolve("UserServiceImpl.java");

        ParsedClass parsed = parser.parseFile(servicePath);

        assertNotNull(parsed, "파싱 결과가 null이면 안됨");
        assertEquals("UserServiceImpl", parsed.getClassName());
        assertEquals(ClassType.SERVICE, parsed.getClassType());

        System.out.println("\n=== Service 파싱 결과 ===");
        System.out.println(parsed);
        for (ParsedMethod method : parsed.getMethods()) {
            System.out.println("  - " + method);

            for (MethodCall call : method.getMethodCalls()) {
                if (call.isServiceOrDaoCall()) {
                    System.out.println("      -> " + call);
                }
            }
        }
    }

    @Test
    @DisplayName("DAO 클래스 파싱 테스트")
    void testParseDAO() throws Exception {
        Path daoPath = samplesPath.resolve("UserDAO.java");

        ParsedClass parsed = parser.parseFile(daoPath);

        assertNotNull(parsed, "파싱 결과가 null이면 안됨");
        assertEquals("UserDAO", parsed.getClassName());
        assertEquals(ClassType.DAO, parsed.getClassType());

        System.out.println("\n=== DAO 파싱 결과 ===");
        System.out.println(parsed);
        for (ParsedMethod method : parsed.getMethods()) {
            System.out.println("  - " + method);
        }
    }

    @Test
    @DisplayName("samples 폴더 전체 파싱 테스트")
    void testParseProject() throws Exception {
        var parsedClasses = parser.parseProject(samplesPath);

        assertFalse(parsedClasses.isEmpty(), "파싱된 클래스가 있어야 함");

        System.out.println("\n=== 전체 프로젝트 파싱 결과 ===");
        System.out.println("총 " + parsedClasses.size() + "개 클래스 파싱됨\n");

        for (ParsedClass clazz : parsedClasses) {
            System.out.println(clazz);
        }
    }

    @Test
    @DisplayName("클래스 레벨 + 메서드 레벨 URL 조합 테스트")
    void testUrlCombination() throws Exception {
        Path controllerPath = samplesPath.resolve("UserController.java");

        ParsedClass parsed = parser.parseFile(controllerPath);

        assertNotNull(parsed);

        // 클래스 레벨 @RequestMapping("/user") 확인
        assertEquals("/user", parsed.getBaseUrlMapping(),
            "클래스 레벨 URL이 '/user'여야 함");

        // 메서드 레벨 URL 조합 확인
        System.out.println("\n=== URL 조합 테스트 ===");
        System.out.println("Base URL: " + parsed.getBaseUrlMapping());

        boolean foundListEndpoint = false;
        boolean foundDetailEndpoint = false;

        for (ParsedMethod method : parsed.getMethods()) {
            if (method.isEndpoint()) {
                System.out.println("  " + method.getMethodName() + " -> " +
                    method.getHttpMethod() + " " + method.getUrlMapping());

                // /user/list.do 확인
                if (method.getMethodName().equals("selectUserList")) {
                    assertEquals("/user/list.do", method.getUrlMapping(),
                        "URL이 클래스+메서드 조합이어야 함");
                    assertEquals("GET", method.getHttpMethod());
                    foundListEndpoint = true;
                }

                // /user/detail.do 확인
                if (method.getMethodName().equals("selectUser")) {
                    assertEquals("/user/detail.do", method.getUrlMapping(),
                        "URL이 클래스+메서드 조합이어야 함");
                    foundDetailEndpoint = true;
                }
            }
        }

        assertTrue(foundListEndpoint, "selectUserList 엔드포인트를 찾아야 함");
        assertTrue(foundDetailEndpoint, "selectUser 엔드포인트를 찾아야 함");
    }

    @Test
    @DisplayName("HTTP 메서드 추출 테스트")
    void testHttpMethodExtraction() throws Exception {
        Path controllerPath = samplesPath.resolve("UserController.java");

        ParsedClass parsed = parser.parseFile(controllerPath);

        assertNotNull(parsed);

        System.out.println("\n=== HTTP 메서드 추출 테스트 ===");

        int getCount = 0;
        int postCount = 0;

        for (ParsedMethod method : parsed.getMethods()) {
            if (method.isEndpoint()) {
                String httpMethod = method.getHttpMethod();
                System.out.println("  " + method.getMethodName() + " -> " + httpMethod);

                if ("GET".equals(httpMethod)) getCount++;
                if ("POST".equals(httpMethod)) postCount++;
            }
        }

        // UserController: GET 2개 (list.do, detail.do), POST 3개 (insert, update, delete)
        assertEquals(2, getCount, "GET 메서드 2개여야 함");
        assertEquals(3, postCount, "POST 메서드 3개여야 함");
    }
}
