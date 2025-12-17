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

    // SQL ID → SQL 쿼리 매핑 (IBatisParser에서 제공, 현재는 빈 상태)
    private Map<String, String> sqlMappings = new HashMap<>();

    // 분석 중 순환 참조 방지를 위한 방문 기록
    private final Set<String> visitedMethods = new HashSet<>();

    // 매핑되지 않은 호출 카운트
    private int unmappedCallCount = 0;

    public FlowAnalyzer() {
    }

    /**
     * SQL 매핑 설정 (IBatisParser 연동용)
     */
    public void setSqlMappings(Map<String, String> sqlMappings) {
        this.sqlMappings = sqlMappings;
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
            if (flow.getUrlMapping() != null && flow.getUrlMapping().contains(urlPattern)) {
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
     * 전략:
     * 1. "Impl"로 끝나는 클래스 → 인터페이스 추정 (UserServiceImpl → UserService)
     * 2. 같은 이름의 인터페이스가 있으면 매핑
     */
    private void buildInterfaceMapping(List<ParsedClass> parsedClasses) {
        interfaceToImpl.clear();

        // 모든 클래스명 수집
        Set<String> allClassNames = new HashSet<>();
        for (ParsedClass clazz : parsedClasses) {
            allClassNames.add(clazz.getClassName());
        }

        // Impl로 끝나는 클래스에서 인터페이스 매핑
        for (ParsedClass clazz : parsedClasses) {
            String className = clazz.getClassName();
            if (className.endsWith("Impl")) {
                String interfaceName = className.substring(0, className.length() - 4);
                // 인터페이스가 실제로 존재하는 경우에만 매핑
                if (allClassNames.contains(interfaceName)) {
                    interfaceToImpl.put(interfaceName, className);
                } else {
                    // 인터페이스가 없어도 Impl 클래스는 매핑 (인터페이스가 외부에 있을 수 있음)
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
     */
    private FlowNode buildFlowTree(ParsedClass clazz, ParsedMethod method, int depth) {
        // 순환 참조 방지
        String signature = clazz.getClassName() + "." + method.getMethodName();
        if (visitedMethods.contains(signature)) {
            FlowNode cycleNode = new FlowNode(clazz.getClassName(), method.getMethodName() + " [순환참조]", clazz.getClassType());
            cycleNode.setDepth(depth);
            return cycleNode;
        }
        visitedMethods.add(signature);

        // 현재 노드 생성
        FlowNode node = new FlowNode(clazz.getClassName(), method.getMethodName(), clazz.getClassType());
        node.setDepth(depth);
        node.setUrlMapping(method.getUrlMapping());
        node.setHttpMethod(method.getHttpMethod());

        // DAO인 경우 SQL ID 추출 시도
        if (clazz.getClassType() == ClassType.DAO) {
            extractSqlInfo(node, method);
        }

        // 최대 깊이 제한 (무한 루프 방지)
        if (depth > 10) {
            return node;
        }

        // 메서드 호출 분석
        for (MethodCall call : method.getMethodCalls()) {
            FlowNode childNode = traceMethodCall(call, depth + 1);
            if (childNode != null) {
                node.addChild(childNode);
            }
        }

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
            return unresolvedNode;
        }

        // 재귀적으로 하위 호출 분석
        return buildFlowTree(targetClass, targetMethod, depth);
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
     */
    private void extractSqlInfo(FlowNode node, ParsedMethod method) {
        // DAO 메서드에서 SQL ID 추출 시도
        // 일반적인 패턴: dao.select("namespace.sqlId", params)
        for (MethodCall call : method.getMethodCalls()) {
            String methodName = call.getMethodName();
            // selectOne, selectList, insert, update, delete 등 iBatis/MyBatis 메서드 패턴
            if (methodName.startsWith("select") || methodName.equals("insert") ||
                methodName.equals("update") || methodName.equals("delete") ||
                methodName.equals("queryForList") || methodName.equals("queryForObject")) {
                // SQL ID는 보통 첫 번째 파라미터로 전달됨
                // 현재는 메서드명만으로 추정
                String sqlId = node.getClassName().toLowerCase() + "." + node.getMethodName();
                node.setSqlId(sqlId);

                // SQL 매핑이 있으면 쿼리 설정
                if (sqlMappings.containsKey(sqlId)) {
                    node.setSqlQuery(sqlMappings.get(sqlId));
                }
                break;
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
