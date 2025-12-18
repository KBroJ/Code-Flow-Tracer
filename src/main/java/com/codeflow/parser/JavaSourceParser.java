package com.codeflow.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

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
                parsedMethod.setMethodUrlOnly(methodUrl);  // 원본 메서드 URL 저장
                parsedMethod.setHttpMethod(extractHttpMethod(annotationName));
            }
        }

        // 메서드 파라미터 추출 및 사용 분석
        extractParameters(method, parsedMethod);

        // 메서드 내 호출되는 다른 메서드들 추출
        List<MethodCallExpr> methodCalls = method.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : methodCalls) {
            String calledMethod = call.getNameAsString();
            String scope = call.getScope().map(Object::toString).orElse("");
            parsedMethod.addMethodCall(new MethodCall(scope, calledMethod));

            // DAO 메서드에서 SQL ID 추출 (iBatis/MyBatis 패턴)
            String sqlId = extractSqlId(call);
            if (sqlId != null) {
                parsedMethod.addSqlId(sqlId);
            }
        }

        return parsedMethod;
    }

    /**
     * DAO 메서드 호출에서 SQL ID를 추출합니다.
     *
     * 지원하는 패턴:
     * - list("userDAO.selectUserList", param)
     * - selectList("userDAO.selectUserList", param)
     * - select("userDAO.selectUser", userId)
     * - selectOne("userDAO.selectUser", userId)
     * - insert("userDAO.insertUser", userVO)
     * - update("userDAO.updateUser", userVO)
     * - delete("userDAO.deleteUser", userId)
     * - getSqlMapClientTemplate().queryForList("sqlId", param)
     */
    private String extractSqlId(MethodCallExpr call) {
        String methodName = call.getNameAsString();

        // iBatis/MyBatis DAO 메서드 패턴
        List<String> sqlMethods = List.of(
            "list", "selectList", "queryForList",
            "select", "selectOne", "queryForObject",
            "insert",
            "update",
            "delete"
        );

        if (!sqlMethods.contains(methodName)) {
            return null;
        }

        // 첫 번째 인자가 SQL ID
        if (call.getArguments().isEmpty()) {
            return null;
        }

        Expression firstArg = call.getArgument(0);
        if (firstArg instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) firstArg).getValue();
        }

        return null;
    }

    /**
     * 메서드 파라미터를 추출하고 사용된 필드/키를 분석합니다.
     */
    private void extractParameters(MethodDeclaration method, ParsedMethod parsedMethod) {
        List<MethodCallExpr> allCalls = method.findAll(MethodCallExpr.class);

        for (Parameter param : method.getParameters()) {
            ParameterInfo paramInfo = new ParameterInfo(
                param.getNameAsString(),
                param.getTypeAsString()
            );

            // @RequestParam 어노테이션에서 실제 파라미터 이름 추출
            extractRequestParamAnnotation(param, paramInfo);

            // VO/Map 타입인 경우 메서드 바디에서 사용된 필드/키 분석
            analyzeParameterUsage(param.getNameAsString(), paramInfo, allCalls);

            parsedMethod.addParameter(paramInfo);
        }
    }

    /**
     * @RequestParam 어노테이션에서 value 속성을 추출합니다.
     *
     * 예: @RequestParam("userId") String userId -> usedFields에 "userId" 추가
     *     @RequestParam(value = "pageNo", defaultValue = "1") int page
     */
    private void extractRequestParamAnnotation(Parameter param, ParameterInfo paramInfo) {
        for (AnnotationExpr annotation : param.getAnnotations()) {
            String annotationName = annotation.getNameAsString();

            if (annotationName.equals("RequestParam")) {
                paramInfo.setHasRequestParam(true);
                String paramName = extractAnnotationValue(annotation);
                if (paramName != null && !paramName.isEmpty()) {
                    paramInfo.addUsedField(paramName);
                }
            } else if (annotationName.equals("PathVariable")) {
                paramInfo.setHasPathVariable(true);
                String paramName = extractAnnotationValue(annotation);
                if (paramName != null && !paramName.isEmpty()) {
                    paramInfo.addUsedField(paramName);
                }
            }
        }
    }

    /**
     * 어노테이션에서 value 추출
     */
    private String extractAnnotationValue(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr) {
            SingleMemberAnnotationExpr single = (SingleMemberAnnotationExpr) annotation;
            return cleanUrlValue(single.getMemberValue().toString());
        } else if (annotation instanceof NormalAnnotationExpr) {
            NormalAnnotationExpr normal = (NormalAnnotationExpr) annotation;
            for (MemberValuePair pair : normal.getPairs()) {
                if (pair.getNameAsString().equals("value") || pair.getNameAsString().equals("name")) {
                    return cleanUrlValue(pair.getValue().toString());
                }
            }
        }
        return null;
    }

    /**
     * 파라미터가 메서드 바디에서 어떻게 사용되는지 분석합니다.
     *
     * VO/DTO 타입: userVO.getName() -> "name" 추출
     * Map 타입: params.get("userId") -> "userId" 추출
     */
    private void analyzeParameterUsage(String paramName, ParameterInfo paramInfo, List<MethodCallExpr> allCalls) {
        for (MethodCallExpr call : allCalls) {
            String scope = call.getScope().map(Object::toString).orElse("");

            // 해당 파라미터에 대한 호출인지 확인
            if (!scope.equals(paramName)) {
                continue;
            }

            String methodName = call.getNameAsString();

            if (paramInfo.isMapType()) {
                // Map.get("key") 패턴 분석
                if (methodName.equals("get") && !call.getArguments().isEmpty()) {
                    Expression arg = call.getArgument(0);
                    if (arg instanceof StringLiteralExpr) {
                        String key = ((StringLiteralExpr) arg).getValue();
                        paramInfo.addUsedField(key);
                    }
                }
            } else if (paramInfo.isVoType()) {
                // VO.getXxx() 패턴 분석
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    // getName -> name 으로 변환
                    String fieldName = methodName.substring(3, 4).toLowerCase()
                                     + methodName.substring(4);
                    paramInfo.addUsedField(fieldName);
                }
            }
        }
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
