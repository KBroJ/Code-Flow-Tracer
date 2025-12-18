package com.codeflow.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Java 소스 코드 파서
 *
 * JavaParser 라이브러리를 사용하여 Java 소스 파일을 AST로 파싱합니다.
 * - Controller 클래스 탐지 (@Controller, @RestController)
 * - Service 클래스 탐지 (@Service)
 * - DAO/Repository 클래스 탐지 (@Repository)
 * - 메서드 호출 관계 추출
 */
public class JavaSourceParser {

    private final JavaParser javaParser;

    public JavaSourceParser() {
        this.javaParser = new JavaParser();
    }

    /**
     * 프로젝트 경로에서 모든 Java 파일을 찾아 파싱합니다.
     */
    public List<ParsedClass> parseProject(Path projectPath) throws IOException {
        List<ParsedClass> parsedClasses = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .forEach(path -> {
                     try {
                         ParsedClass parsed = parseFile(path);
                         if (parsed != null) {
                             parsedClasses.add(parsed);
                         }
                     } catch (IOException e) {
                         System.err.println("파싱 실패: " + path + " - " + e.getMessage());
                     }
                 });
        }

        return parsedClasses;
    }

    /**
     * 단일 Java 파일을 파싱합니다.
     */
    public ParsedClass parseFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        ParseResult<CompilationUnit> parseResult = javaParser.parse(content);

        if (!parseResult.isSuccessful()) {
            System.err.println("파싱 오류: " + filePath);
            return null;
        }

        CompilationUnit cu = parseResult.getResult().orElse(null);
        if (cu == null) {
            return null;
        }

        // 클래스 선언 찾기
        Optional<ClassOrInterfaceDeclaration> classDecl = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classDecl.isEmpty()) {
            return null;
        }

        ClassOrInterfaceDeclaration clazz = classDecl.get();
        ParsedClass parsedClass = new ParsedClass();
        parsedClass.setFilePath(filePath);
        parsedClass.setClassName(clazz.getNameAsString());
        parsedClass.setPackageName(cu.getPackageDeclaration()
                                     .map(pd -> pd.getNameAsString())
                                     .orElse(""));

        // 인터페이스 여부 확인
        parsedClass.setInterface(clazz.isInterface());

        // 구현한 인터페이스 목록 추출
        clazz.getImplementedTypes().forEach(implementedType -> {
            parsedClass.addImplementedInterface(implementedType.getNameAsString());
        });

        // 클래스 타입 판별 (Controller, Service, DAO 등)
        parsedClass.setClassType(determineClassType(clazz));

        // 클래스 레벨 @RequestMapping URL 추출
        String baseUrl = extractClassLevelUrl(clazz);
        parsedClass.setBaseUrlMapping(baseUrl);

        // 메서드 정보 추출
        for (MethodDeclaration method : clazz.getMethods()) {
            ParsedMethod parsedMethod = parseMethod(method, baseUrl);
            parsedClass.addMethod(parsedMethod);
        }

        return parsedClass;
    }

    /**
     * 클래스 타입을 어노테이션 기반으로 판별합니다.
     */
    private ClassType determineClassType(ClassOrInterfaceDeclaration clazz) {
        for (AnnotationExpr annotation : clazz.getAnnotations()) {
            String annotationName = annotation.getNameAsString();

            if (annotationName.equals("Controller") || annotationName.equals("RestController")) {
                return ClassType.CONTROLLER;
            }
            if (annotationName.equals("Service")) {
                return ClassType.SERVICE;
            }
            if (annotationName.equals("Repository") || annotationName.contains("Dao") || annotationName.contains("DAO")) {
                return ClassType.DAO;
            }
            if (annotationName.equals("Component")) {
                return ClassType.COMPONENT;
            }
        }

        // 어노테이션이 없으면 클래스명으로 추정
        String className = clazz.getNameAsString();
        if (className.endsWith("Controller")) {
            return ClassType.CONTROLLER;
        }
        if (className.endsWith("Service") || className.endsWith("ServiceImpl")) {
            return ClassType.SERVICE;
        }
        if (className.endsWith("Dao") || className.endsWith("DAO") ||
            className.endsWith("Repository") || className.endsWith("Mapper")) {
            return ClassType.DAO;
        }

        return ClassType.OTHER;
    }

    /**
     * 클래스 레벨 @RequestMapping URL을 추출합니다.
     */
    private String extractClassLevelUrl(ClassOrInterfaceDeclaration clazz) {
        for (AnnotationExpr annotation : clazz.getAnnotations()) {
            String annotationName = annotation.getNameAsString();
            if (annotationName.equals("RequestMapping")) {
                return extractUrlFromAnnotation(annotation);
            }
        }
        return "";
    }

    /**
     * 메서드 정보를 추출합니다.
     *
     * @param method 메서드 선언
     * @param baseUrl 클래스 레벨 URL (있으면 조합)
     */
    private ParsedMethod parseMethod(MethodDeclaration method, String baseUrl) {
        ParsedMethod parsedMethod = new ParsedMethod();
        parsedMethod.setMethodName(method.getNameAsString());
        parsedMethod.setReturnType(method.getTypeAsString());

        // @RequestMapping 등 URL 매핑 정보 추출
        for (AnnotationExpr annotation : method.getAnnotations()) {
            String annotationName = annotation.getNameAsString();
            if (annotationName.contains("Mapping")) {
                String methodUrl = extractUrlFromAnnotation(annotation);
                // 클래스 레벨 URL + 메서드 레벨 URL 조합
                String fullUrl = combineUrls(baseUrl, methodUrl);
                parsedMethod.setUrlMapping(fullUrl);
                parsedMethod.setHttpMethod(extractHttpMethod(annotationName));
            }
        }

        // 메서드 내 호출되는 다른 메서드들 추출
        List<MethodCallExpr> methodCalls = method.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : methodCalls) {
            String calledMethod = call.getNameAsString();
            String scope = call.getScope().map(Object::toString).orElse("");
            parsedMethod.addMethodCall(new MethodCall(scope, calledMethod));
        }

        return parsedMethod;
    }

    /**
     * 어노테이션에서 URL 경로를 추출합니다.
     *
     * 지원하는 어노테이션 형태:
     * 1. @GetMapping (값 없음) -> ""
     * 2. @GetMapping("/list.do") -> "/list.do"
     * 3. @RequestMapping(value = "/list.do") -> "/list.do"
     * 4. @RequestMapping(path = "/list.do") -> "/list.do"
     */
    private String extractUrlFromAnnotation(AnnotationExpr annotation) {
        // 1. SingleMemberAnnotationExpr: @GetMapping("/list.do")
        if (annotation instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr singleMember = (SingleMemberAnnotationExpr) annotation;
            String value = singleMember.getMemberValue().toString();
            return cleanUrlValue(value);
        }

        // 2. NormalAnnotationExpr: @RequestMapping(value = "/list.do", method = GET)
        if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
            for (MemberValuePair pair : normalAnnotation.getPairs()) {
                String name = pair.getNameAsString();
                // value 또는 path 속성에서 URL 추출
                if (name.equals("value") || name.equals("path")) {
                    String value = pair.getValue().toString();
                    return cleanUrlValue(value);
                }
            }
        }

        // 3. MarkerAnnotationExpr: @GetMapping (값 없음)
        return "";
    }

    /**
     * URL 값에서 따옴표 및 불필요한 문자를 제거합니다.
     */
    private String cleanUrlValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // 따옴표 제거 ("/list.do" -> /list.do)
        value = value.replace("\"", "");

        // 배열 형태 처리 ({"/a", "/b"} -> /a)
        if (value.startsWith("{") && value.endsWith("}")) {
            value = value.substring(1, value.length() - 1);
            // 첫 번째 값만 사용
            if (value.contains(",")) {
                value = value.split(",")[0].trim();
            }
            value = value.replace("\"", "");
        }

        return value.trim();
    }

    /**
     * 클래스 레벨 URL과 메서드 레벨 URL을 조합합니다.
     *
     * 예: "/user" + "/list.do" -> "/user/list.do"
     *     "/user/" + "/list.do" -> "/user/list.do" (중복 슬래시 제거)
     *     "" + "/list.do" -> "/list.do"
     */
    private String combineUrls(String baseUrl, String methodUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return methodUrl;
        }
        if (methodUrl == null || methodUrl.isEmpty()) {
            return baseUrl;
        }

        // 슬래시 정규화
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String method = methodUrl.startsWith("/") ? methodUrl : "/" + methodUrl;

        return base + method;
    }

    /**
     * 어노테이션명에서 HTTP 메서드를 추출합니다.
     */
    private String extractHttpMethod(String annotationName) {
        if (annotationName.equals("GetMapping")) return "GET";
        if (annotationName.equals("PostMapping")) return "POST";
        if (annotationName.equals("PutMapping")) return "PUT";
        if (annotationName.equals("DeleteMapping")) return "DELETE";
        if (annotationName.equals("PatchMapping")) return "PATCH";
        return "ALL";
    }
}
