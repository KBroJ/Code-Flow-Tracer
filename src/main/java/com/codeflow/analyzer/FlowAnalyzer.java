package com.codeflow.analyzer;

import com.codeflow.parser.*;

import java.nio.file.Path;
import java.util.*;

/**
 * 호출 흐름 분석 엔진
 *
 * Controller → Service → DAO 호출 흐름을 분석합니다.
 * - 파싱된 클래스들을 인덱싱
 * - 인터페이스 → 구현체 매핑
 * - 메서드 호출을 따라가며 트리 구조 생성
 */
public class FlowAnalyzer {

    // 클래스명 → ParsedClass 매핑 (빠른 조회용)
    private final Map<String, ParsedClass> classIndex = new HashMap<>();

    // 인터페이스명 → 구현체 클래스명 매핑
    private final Map<String, String> interfaceToImpl = new HashMap<>();

    // scope(변수명) → 클래스명 매핑 (변수명으로 클래스 추정)
    private final Map<String, String> scopeToClassName = new HashMap<>();

    // SQL ID → SqlInfo 매핑 (IBatisParser에서 제공)
    private Map<String, SqlInfo> sqlInfoMap = new HashMap<>();

    // 분석 중 순환 참조 방지를 위한 방문 기록
    private final Set<String> visitedMethods = new HashSet<>();

    // 매핑되지 않은 호출 카운트
    private int unmappedCallCount = 0;

    public FlowAnalyzer() {
    }

    /**
     * SQL 정보 매핑 설정 (IBatisParser 연동용)
     */
    public void setSqlInfoMap(Map<String, SqlInfo> sqlInfoMap) {
        this.sqlInfoMap = sqlInfoMap;
    }

    /**
     * 프로젝트 분석 실행
     *
     * @param projectPath 프로젝트 경로
     * @param parsedClasses 파싱된 클래스 목록
     * @return 분석 결과
     */
    public FlowResult analyze(Path projectPath, List<ParsedClass> parsedClasses) {
        FlowResult result = new FlowResult(projectPath.toString());

        // 1. 클래스 인덱싱
        indexClasses(parsedClasses);

        // 2. 인터페이스 → 구현체 매핑 생성
        buildInterfaceMapping(parsedClasses);

        // 3. 통계 정보 수집
        collectStatistics(result, parsedClasses);

        // 4. Controller 엔드포인트에서 시작하여 호출 흐름 분석
        for (ParsedClass clazz : parsedClasses) {
            if (clazz.getClassType() == ClassType.CONTROLLER) {
                analyzeController(result, clazz);
            }
        }

        result.setUnmappedCallCount(unmappedCallCount);
        return result;
    }

    /**
     * 특정 URL 패턴에 해당하는 흐름만 분석
     *
     * 지원하는 패턴:
     * - 정확한 매칭: "/user/list.do"
     * - 와일드카드: "/user/*" 또는 "/user/**"
     * - PathVariable: "/user/{id}"
     * - 부분 매칭: "user" (URL에 포함되면 매칭)
     */
    public FlowResult analyzeByUrl(Path projectPath, List<ParsedClass> parsedClasses, String urlPattern) {
        FlowResult fullResult = analyze(projectPath, parsedClasses);

        // URL 패턴 필터링
        FlowResult filtered = new FlowResult(projectPath.toString());
        filtered.setTotalClasses(fullResult.getTotalClasses());
        filtered.setControllerCount(fullResult.getControllerCount());
        filtered.setServiceCount(fullResult.getServiceCount());
        filtered.setDaoCount(fullResult.getDaoCount());

        for (FlowNode flow : fullResult.getFlows()) {
            String url = flow.getUrlMapping();
            if (url != null && UrlMatcher.matches(url, urlPattern)) {
                filtered.addFlow(flow);
            }
        }

        filtered.setEndpointCount(filtered.getFlows().size());
        return filtered;
    }

    /**
     * 클래스 인덱싱 - 빠른 조회를 위해 Map 생성
     */
    private void indexClasses(List<ParsedClass> parsedClasses) {
        classIndex.clear();
        scopeToClassName.clear();

        for (ParsedClass clazz : parsedClasses) {
            String className = clazz.getClassName();
            classIndex.put(className, clazz);

            // scope 매핑 생성 (userService → UserService, userDAO → UserDAO)
            String scopeName = toLowerCamelCase(className);
            scopeToClassName.put(scopeName, className);

            // 일반적인 변수명 패턴도 추가
            // UserServiceImpl → userService (Impl 제거)
            if (className.endsWith("Impl")) {
                String baseName = className.substring(0, className.length() - 4);
                scopeToClassName.put(toLowerCamelCase(baseName), className);
            }
        }
    }

    /**
     * 인터페이스 → 구현체 매핑 생성
     *
     * 전략 (우선순위):
     * 1. implements 키워드 기반 매핑 (가장 정확)
     *    - class UserServiceV2 implements UserService → UserService → UserServiceV2
     * 2. "Impl" 접미사 기반 매핑 (fallback)
     *    - UserServiceImpl → UserService → UserServiceImpl
     */
    private void buildInterfaceMapping(List<ParsedClass> parsedClasses) {
        interfaceToImpl.clear();

        // 1단계: implements 기반 매핑 (가장 정확)
        for (ParsedClass clazz : parsedClasses) {
            // 인터페이스는 구현체가 아니므로 스킵
            if (clazz.isInterface()) {
                continue;
            }

            // 이 클래스가 구현한 인터페이스들에 대해 매핑
            for (String interfaceName : clazz.getImplementedInterfaces()) {
                // 이미 매핑이 있으면 스킵 (첫 번째 구현체 우선)
                if (!interfaceToImpl.containsKey(interfaceName)) {
                    interfaceToImpl.put(interfaceName, clazz.getClassName());
                }
            }
        }

        // 2단계: Impl 접미사 기반 매핑 (fallback, implements 매핑이 없는 경우에만)
        for (ParsedClass clazz : parsedClasses) {
            String className = clazz.getClassName();
            if (className.endsWith("Impl")) {
                String interfaceName = className.substring(0, className.length() - 4);
                // implements 매핑이 없는 경우에만 추가
                if (!interfaceToImpl.containsKey(interfaceName)) {
                    interfaceToImpl.put(interfaceName, className);
                }
            }
        }
    }

    /**
     * 통계 정보 수집
     */
    private void collectStatistics(FlowResult result, List<ParsedClass> parsedClasses) {
        int controllers = 0, services = 0, daos = 0, endpoints = 0;

        for (ParsedClass clazz : parsedClasses) {
            switch (clazz.getClassType()) {
                case CONTROLLER:
                    controllers++;
                    for (ParsedMethod method : clazz.getMethods()) {
                        if (method.isEndpoint()) {
                            endpoints++;
                        }
                    }
                    break;
                case SERVICE:
                    services++;
                    break;
                case DAO:
                    daos++;
                    break;
            }
        }

        result.setTotalClasses(parsedClasses.size());
        result.setControllerCount(controllers);
        result.setServiceCount(services);
        result.setDaoCount(daos);
        result.setEndpointCount(endpoints);
    }

    /**
     * Controller 클래스 분석
     */
    private void analyzeController(FlowResult result, ParsedClass controller) {
        for (ParsedMethod method : controller.getMethods()) {
            // 엔드포인트 메서드만 분석 (URL 매핑이 있는 메서드)
            if (method.isEndpoint()) {
                visitedMethods.clear();  // 각 엔드포인트마다 방문 기록 초기화
                FlowNode flowNode = buildFlowTree(controller, method, 0);
                result.addFlow(flowNode);
            }
        }
    }

    /**
     * 호출 흐름 트리 생성 (재귀)
     *
     * visitedMethods는 "호출 스택" 역할을 함:
     * - 현재 경로(A→B→C)에서 A를 다시 호출하면 순환 참조
     * - 다른 경로(A→B, A→C)에서 같은 메서드를 호출하는 건 OK
     */
    private FlowNode buildFlowTree(ParsedClass clazz, ParsedMethod method, int depth) {
        String signature = clazz.getClassName() + "." + method.getMethodName();

        // 현재 호출 스택에 이미 있으면 = 진짜 순환 참조 (A→B→A)
        if (visitedMethods.contains(signature)) {
            FlowNode cycleNode = new FlowNode(clazz.getClassName(), method.getMethodName(), clazz.getClassType());
            cycleNode.setDepth(depth);
            return cycleNode;  // 라벨 없이 그냥 반환 (무한 루프만 방지)
        }

        // 현재 호출 스택에 추가
        visitedMethods.add(signature);

        // 현재 노드 생성
        FlowNode node = new FlowNode(clazz.getClassName(), method.getMethodName(), clazz.getClassType());
        node.setDepth(depth);
        node.setFilePath(clazz.getFilePath() != null ? clazz.getFilePath().toString() : null);
        node.setUrlMapping(method.getUrlMapping());
        node.setClassUrlMapping(clazz.getBaseUrlMapping());      // 클래스 레벨 URL
        node.setMethodUrlMapping(method.getMethodUrlOnly());     // 메서드 레벨 URL
        node.setHttpMethod(method.getHttpMethod());
        node.setImplementedInterfaces(clazz.getImplementedInterfaces());  // 구현 인터페이스
        node.setParameters(method.getParameters());                        // 메서드 파라미터

        // DAO인 경우 SQL ID 추출 시도
        if (clazz.getClassType() == ClassType.DAO) {
            extractSqlInfo(node, method);
        }

        // 최대 깊이 제한 (무한 루프 방지)
        if (depth > 10) {
            visitedMethods.remove(signature);  // 스택에서 제거
            return node;
        }

        // 메서드 호출 분석
        for (MethodCall call : method.getMethodCalls()) {
            FlowNode childNode = traceMethodCall(call, depth + 1);
            if (childNode != null) {
                node.addChild(childNode);
            }
        }

        // 현재 경로 탐색 완료 → 스택에서 제거 (다른 경로에서 다시 호출 가능)
        visitedMethods.remove(signature);

        return node;
    }

    /**
     * 메서드 호출 추적
     */
    private FlowNode traceMethodCall(MethodCall call, int depth) {
        // Service/DAO 호출이 아니면 스킵 (유틸리티, 로깅 등 제외)
        if (!call.isServiceOrDaoCall()) {
            return null;
        }

        String scope = call.getScope();
        String methodName = call.getMethodName();

        // scope에서 클래스명 추정
        String className = resolveClassName(scope);
        if (className == null) {
            unmappedCallCount++;
            return null;
        }

        // 클래스 조회
        ParsedClass targetClass = classIndex.get(className);
        if (targetClass == null) {
            unmappedCallCount++;
            return null;
        }

        // 메서드 조회
        ParsedMethod targetMethod = findMethod(targetClass, methodName);
        if (targetMethod == null) {
            // 메서드가 없으면 노드만 생성 (호출은 있지만 구현이 없는 경우)
            FlowNode unresolvedNode = new FlowNode(className, methodName, targetClass.getClassType());
            unresolvedNode.setDepth(depth);
            // 호출 인자 설정
            if (call.hasArguments()) {
                unresolvedNode.setCallArguments(call.getArguments());
            }
            return unresolvedNode;
        }

        // 재귀적으로 하위 호출 분석
        FlowNode node = buildFlowTree(targetClass, targetMethod, depth);
        // 호출 인자 설정
        if (call.hasArguments()) {
            node.setCallArguments(call.getArguments());
        }
        return node;
    }

    /**
     * scope(변수명)에서 클래스명 추정
     */
    private String resolveClassName(String scope) {
        if (scope == null || scope.isEmpty()) {
            return null;
        }

        // 1. 직접 매핑 확인
        if (scopeToClassName.containsKey(scope)) {
            String className = scopeToClassName.get(scope);
            // 인터페이스면 구현체로 변환
            return resolveToImplementation(className);
        }

        // 2. 첫 글자 대문자로 변환하여 확인
        String pascalCase = toUpperCamelCase(scope);
        if (classIndex.containsKey(pascalCase)) {
            return resolveToImplementation(pascalCase);
        }

        // 3. Impl 접미사 붙여서 확인
        if (classIndex.containsKey(pascalCase + "Impl")) {
            return pascalCase + "Impl";
        }

        return null;
    }

    /**
     * 인터페이스명을 구현체명으로 변환
     */
    private String resolveToImplementation(String className) {
        // 인터페이스 → 구현체 매핑 확인
        if (interfaceToImpl.containsKey(className)) {
            String implName = interfaceToImpl.get(className);
            // 구현체가 실제로 인덱스에 있는지 확인
            if (classIndex.containsKey(implName)) {
                return implName;
            }
        }
        return className;
    }

    /**
     * 클래스에서 메서드 찾기
     */
    private ParsedMethod findMethod(ParsedClass clazz, String methodName) {
        for (ParsedMethod method : clazz.getMethods()) {
            if (method.getMethodName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * DAO 메서드에서 SQL 정보 추출
     *
     * ParsedMethod.sqlIds에서 SQL ID를 가져와서 SqlInfo와 매핑합니다.
     */
    private void extractSqlInfo(FlowNode node, ParsedMethod method) {
        // JavaSourceParser에서 추출한 SQL ID 사용
        if (method.hasSqlIds()) {
            String sqlId = method.getSqlIds().get(0);  // 첫 번째 SQL ID 사용
            node.setSqlId(sqlId);

            // SqlInfo 매핑이 있으면 설정
            SqlInfo sqlInfo = IBatisParser.findBySqlId(sqlInfoMap, sqlId);
            if (sqlInfo != null) {
                node.setSqlInfo(sqlInfo);
                node.setSqlQuery(sqlInfo.getQuery());
            }
        }
    }

    /**
     * PascalCase → camelCase 변환
     */
    private String toLowerCamelCase(String pascalCase) {
        if (pascalCase == null || pascalCase.isEmpty()) {
            return pascalCase;
        }
        return Character.toLowerCase(pascalCase.charAt(0)) + pascalCase.substring(1);
    }

    /**
     * camelCase → PascalCase 변환
     */
    private String toUpperCamelCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
    }
}
