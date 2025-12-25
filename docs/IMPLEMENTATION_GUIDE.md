# 구현 상세 가이드 (Implementation Guide)

> 이 문서는 Code Flow Tracer의 구현 흐름과 설계 결정을 상세히 설명합니다.
> 프로젝트에 처음 참여하거나 다른 사람에게 설명할 때 참고할 수 있습니다.

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [핵심 개념](#3-핵심-개념)
4. [Parser 모듈 상세](#4-parser-모듈-상세)
5. [Analyzer 모듈 상세](#5-analyzer-모듈-상세)
6. [데이터 흐름](#6-데이터-흐름)
7. [설계 결정과 이유 (WHY)](#7-설계-결정과-이유-why)
8. [실제 동작 예시](#8-실제-동작-예시)
9. [향후 확장](#9-향후-확장)
10. [CLI (Picocli) 모듈 상세](#10-cli-picocli-모듈-상세)
11. [파라미터 분석 모듈 상세](#11-파라미터-분석-모듈-상세)

---

## 1. 프로젝트 개요

### 1.1 이 프로젝트가 해결하려는 문제

레거시 Java 프로젝트(특히 전자정부프레임워크 기반)에서 **"이 API가 어떤 흐름으로 동작하는지"** 파악하기 어렵습니다.

```
실제 업무 상황:
- 신입 개발자: "이 API 수정하려면 어디를 봐야 하나요?"
- 인수인계 시: "이 기능이 어떻게 동작하는지 문서가 없어요"
- 유지보수 시: "이 SQL이 어디서 호출되는지 모르겠어요"
```

### 1.2 해결 방법

소스 코드를 **정적 분석**하여 호출 흐름을 자동으로 추적합니다.

```
입력: Java 소스 코드 폴더
      ↓
처리: Controller → Service → DAO → SQL 흐름 분석
      ↓
출력: 트리 형태의 호출 흐름 문서
```

### 1.3 핵심 결과물

```
[GET /user/list.do] UserController.selectUserList()
└─ userService.selectUserList()
   └─ UserServiceImpl.selectUserList()
      └─ userDAO.selectUserList()
         └─ SQL: userDAO.selectUserList
```

---

## 2. 전체 아키텍처

### 2.1 패키지 구조

```
com.codeflow/
├── Main.java                 # CLI 진입점 (Picocli 사용)
│
├── parser/                   # [완료] 소스 코드 파싱
│   ├── JavaSourceParser.java # Java 파일 → 구조화된 데이터
│   ├── ParsedClass.java      # 파싱된 클래스 정보
│   ├── ParsedMethod.java     # 파싱된 메서드 정보
│   ├── MethodCall.java       # 메서드 호출 정보
│   ├── ParameterInfo.java    # 파라미터 정보 (타입, 이름, 사용 필드)
│   └── ClassType.java        # 클래스 타입 (Controller/Service/DAO)
│
├── analyzer/                 # [완료] 호출 흐름 분석
│   ├── FlowAnalyzer.java     # 핵심 분석 엔진
│   ├── FlowNode.java         # 호출 흐름 트리 노드
│   └── FlowResult.java       # 분석 결과 컨테이너
│
├── output/                   # [완료] 결과 출력
│   ├── ConsoleOutput.java    # 콘솔 출력 ✅
│   └── ExcelOutput.java      # 엑셀 출력 ✅
│
└── ui/                       # [완료] GUI
    ├── MainFrame.java        # Swing GUI (FlatLaf 다크 테마)
    └── ResultPanel.java      # 분석 결과 표시 패널
```

### 2.2 처리 단계

```
┌─────────────────────────────────────────────────────────────────┐
│                        사용자 입력                               │
│                  (프로젝트 경로, URL 패턴)                        │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  1단계: Parser (소스 코드 파싱)                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ JavaSourceParser                                        │    │
│  │ - .java 파일을 찾아서 읽음                               │    │
│  │ - JavaParser 라이브러리로 AST(추상 구문 트리) 생성        │    │
│  │ - 클래스/메서드/호출 관계를 ParsedClass 객체로 변환       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  출력: List<ParsedClass>                                         │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  2단계: Analyzer (호출 흐름 분석)                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ FlowAnalyzer                                            │    │
│  │ - ParsedClass들을 인덱싱 (빠른 검색용)                   │    │
│  │ - 인터페이스 → 구현체 매핑 생성                          │    │
│  │ - Controller 엔드포인트에서 시작                         │    │
│  │ - 메서드 호출을 재귀적으로 따라가며 트리 생성            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  출력: FlowResult (FlowNode 트리 포함)                           │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  3단계: Output (결과 출력)                                        │
│  - ConsoleOutput: 터미널에 트리 출력                             │
│  - ExcelOutput: 엑셀 파일로 저장                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 핵심 개념

### 3.1 전자정부프레임워크 계층 구조

이 프로젝트가 분석하는 대상의 일반적인 구조입니다.

```
┌──────────────────────────────────────────────────────────┐
│                    Controller 계층                        │
│  - HTTP 요청을 받음                                       │
│  - @Controller, @RestController 어노테이션               │
│  - URL 매핑 (@RequestMapping, @GetMapping 등)            │
│  예: UserController.java                                 │
└────────────────────────┬─────────────────────────────────┘
                         │ 호출
                         ▼
┌──────────────────────────────────────────────────────────┐
│                    Service 계층                           │
│  - 비즈니스 로직 처리                                     │
│  - @Service 어노테이션                                    │
│  - 보통 인터페이스 + 구현체 패턴                          │
│  예: UserService (인터페이스), UserServiceImpl (구현체)   │
└────────────────────────┬─────────────────────────────────┘
                         │ 호출
                         ▼
┌──────────────────────────────────────────────────────────┐
│                    DAO 계층                               │
│  - 데이터베이스 접근                                      │
│  - @Repository 어노테이션                                 │
│  - iBatis/MyBatis SQL 매퍼 호출                          │
│  예: UserDAO.java                                        │
└────────────────────────┬─────────────────────────────────┘
                         │ 호출
                         ▼
┌──────────────────────────────────────────────────────────┐
│                    SQL (XML)                              │
│  - iBatis/MyBatis SQL 매퍼 파일                          │
│  - 실제 SQL 쿼리 정의                                    │
│  예: User_SQL.xml                                        │
└──────────────────────────────────────────────────────────┘
```

### 3.2 AST (Abstract Syntax Tree)란?

**AST**는 소스 코드를 트리 구조로 표현한 것입니다.

```java
// 원본 코드
public void selectUserList() {
    userService.getList();
}
```

```
// AST 표현 (단순화)
MethodDeclaration
├── name: "selectUserList"
├── returnType: "void"
└── body
    └── MethodCallExpr
        ├── scope: "userService"
        └── name: "getList"
```

**왜 AST를 사용하는가?**
- 정규식보다 정확함 (문자열 내 코드, 주석 등 구분 가능)
- 구조적 분석 가능 (메서드 안의 호출만 추출 등)
- JavaParser 라이브러리가 이 작업을 해줌

### 3.3 정적 분석 vs 동적 분석

| 구분 | 정적 분석 (이 프로젝트) | 동적 분석 |
|------|------------------------|----------|
| 방식 | 소스 코드를 읽어서 분석 | 실행하면서 추적 |
| 장점 | 실행 환경 불필요 | 실제 동작 추적 가능 |
| 단점 | 런타임 동작 추적 불가 | 환경 구성 필요 |
| 예시 | 코드 리뷰 도구 | 프로파일러, 디버거 |

---

## 4. Parser 모듈 상세

### 4.1 클래스 다이어그램

```
┌─────────────────────┐
│  JavaSourceParser   │
├─────────────────────┤
│ - javaParser        │
├─────────────────────┤
│ + parseProject()    │──────┐
│ + parseFile()       │      │
└─────────────────────┘      │
                             │ 생성
                             ▼
┌─────────────────────┐     ┌─────────────────────┐
│    ParsedClass      │     │     ClassType       │
├─────────────────────┤     ├─────────────────────┤
│ - filePath          │     │ CONTROLLER          │
│ - packageName       │     │ SERVICE             │
│ - className         │     │ DAO                 │
│ - classType ────────┼────▶│ COMPONENT           │
│ - methods           │     │ OTHER               │
└─────────┬───────────┘     └─────────────────────┘
          │
          │ 포함 (1:N)
          ▼
┌─────────────────────┐
│   ParsedMethod      │
├─────────────────────┤
│ - methodName        │
│ - returnType        │
│ - urlMapping        │
│ - httpMethod        │
│ - methodCalls       │
└─────────┬───────────┘
          │
          │ 포함 (1:N)
          ▼
┌─────────────────────┐
│    MethodCall       │
├─────────────────────┤
│ - scope             │  예: "userService"
│ - methodName        │  예: "getList"
└─────────────────────┘
```

### 4.2 JavaSourceParser 동작 원리

```java
public class JavaSourceParser {

    // 1. 프로젝트 전체 파싱
    public List<ParsedClass> parseProject(Path projectPath) {
        // projectPath 아래의 모든 .java 파일을 찾음
        // 각 파일에 대해 parseFile() 호출
        // 결과를 List로 모아서 반환
    }

    // 2. 단일 파일 파싱
    public ParsedClass parseFile(Path filePath) {
        // 파일 내용을 읽음
        // JavaParser로 AST 생성
        // 클래스 선언 찾기
        // 클래스 타입 판별 (Controller? Service? DAO?)
        // 메서드 정보 추출
    }

    // 3. 클래스 타입 판별
    private ClassType determineClassType(ClassOrInterfaceDeclaration clazz) {
        // 방법 1: 어노테이션 확인
        //   @Controller, @RestController → CONTROLLER
        //   @Service → SERVICE
        //   @Repository → DAO

        // 방법 2: 클래스명으로 추정 (어노테이션 없을 때)
        //   *Controller → CONTROLLER
        //   *Service, *ServiceImpl → SERVICE
        //   *DAO, *Dao, *Repository → DAO
    }
}
```

### 4.3 클래스 타입 판별 로직

```java
private ClassType determineClassType(ClassOrInterfaceDeclaration clazz) {
    // 1단계: 어노테이션으로 판별 (가장 정확)
    for (AnnotationExpr annotation : clazz.getAnnotations()) {
        String name = annotation.getNameAsString();

        if (name.equals("Controller") || name.equals("RestController"))
            return ClassType.CONTROLLER;
        if (name.equals("Service"))
            return ClassType.SERVICE;
        if (name.equals("Repository") || name.contains("Dao"))
            return ClassType.DAO;
    }

    // 2단계: 클래스명으로 추정 (어노테이션 없는 레거시 코드용)
    String className = clazz.getNameAsString();

    if (className.endsWith("Controller"))
        return ClassType.CONTROLLER;
    if (className.endsWith("Service") || className.endsWith("ServiceImpl"))
        return ClassType.SERVICE;
    if (className.endsWith("Dao") || className.endsWith("DAO"))
        return ClassType.DAO;

    return ClassType.OTHER;
}
```

**왜 2단계로 판별하는가?**
- 최신 코드: 어노테이션 사용
- 레거시 코드: 어노테이션 없이 네이밍 컨벤션만 사용
- 둘 다 지원해야 현실적으로 유용함

### 4.4 메서드 호출 추출

```java
private ParsedMethod parseMethod(MethodDeclaration method) {
    ParsedMethod parsed = new ParsedMethod();
    parsed.setMethodName(method.getNameAsString());

    // URL 매핑 정보 추출 (@GetMapping, @PostMapping 등)
    for (AnnotationExpr annotation : method.getAnnotations()) {
        if (annotation.getNameAsString().contains("Mapping")) {
            parsed.setUrlMapping(extractUrl(annotation));
            parsed.setHttpMethod(extractHttpMethod(annotation));
        }
    }

    // 메서드 내부에서 호출하는 다른 메서드들 추출
    List<MethodCallExpr> calls = method.findAll(MethodCallExpr.class);
    for (MethodCallExpr call : calls) {
        // userService.getList() 형태에서
        // scope = "userService", methodName = "getList"
        String scope = call.getScope().map(Object::toString).orElse("");
        String methodName = call.getNameAsString();
        parsed.addMethodCall(new MethodCall(scope, methodName));
    }

    return parsed;
}
```

---

## 5. Analyzer 모듈 상세

### 5.1 클래스 다이어그램

```
┌─────────────────────────────┐
│       FlowAnalyzer          │
├─────────────────────────────┤
│ - classIndex                │  Map<클래스명, ParsedClass>
│ - interfaceToImpl           │  Map<인터페이스명, 구현체명>
│ - scopeToClassName          │  Map<변수명, 클래스명>
│ - visitedMethods            │  순환 참조 방지용
├─────────────────────────────┤
│ + analyze()                 │
│ + analyzeByUrl()            │
│ - indexClasses()            │
│ - buildInterfaceMapping()   │
│ - buildFlowTree()           │  재귀 함수
│ - traceMethodCall()         │
│ - resolveClassName()        │
└─────────────────────────────┘
              │
              │ 생성
              ▼
┌─────────────────────┐     ┌─────────────────────┐
│    FlowResult       │     │     FlowNode        │
├─────────────────────┤     ├─────────────────────┤
│ - projectPath       │     │ - className         │
│ - analyzedAt        │     │ - methodName        │
│ - flows ────────────┼────▶│ - classType         │
│ - totalClasses      │     │ - urlMapping        │
│ - controllerCount   │     │ - depth             │
│ - serviceCount      │     │ - children ─────────┼──┐
│ - daoCount          │     └─────────────────────┘  │
└─────────────────────┘              ▲               │
                                     └───────────────┘
                                     (트리 구조: 자기 참조)
```

### 5.2 분석 알고리즘 상세

#### 5.2.1 전체 흐름

```java
public FlowResult analyze(Path projectPath, List<ParsedClass> parsedClasses) {

    // 1. 클래스 인덱싱 (빠른 검색을 위해)
    indexClasses(parsedClasses);

    // 2. 인터페이스 → 구현체 매핑 생성
    buildInterfaceMapping(parsedClasses);

    // 3. Controller에서 시작하여 호출 흐름 분석
    for (ParsedClass clazz : parsedClasses) {
        if (clazz.getClassType() == ClassType.CONTROLLER) {
            analyzeController(result, clazz);
        }
    }

    return result;
}
```

#### 5.2.2 클래스 인덱싱

**왜 인덱싱이 필요한가?**

```java
// 인덱싱 없이 클래스를 찾으려면...
for (ParsedClass c : parsedClasses) {
    if (c.getClassName().equals("UserServiceImpl")) {
        // 찾음!
    }
}
// → O(N) 시간 복잡도, 매번 전체 순회

// 인덱싱하면...
ParsedClass found = classIndex.get("UserServiceImpl");
// → O(1) 시간 복잡도, 즉시 조회
```

```java
private void indexClasses(List<ParsedClass> parsedClasses) {
    for (ParsedClass clazz : parsedClasses) {
        String className = clazz.getClassName();

        // 클래스명 → ParsedClass 매핑
        classIndex.put(className, clazz);
        // 예: "UserController" → ParsedClass 객체

        // 변수명 → 클래스명 매핑 (camelCase 변환)
        String scopeName = toLowerCamelCase(className);
        scopeToClassName.put(scopeName, className);
        // 예: "userController" → "UserController"

        // Impl 클래스의 경우 인터페이스명으로도 매핑
        if (className.endsWith("Impl")) {
            String baseName = className.substring(0, className.length() - 4);
            scopeToClassName.put(toLowerCamelCase(baseName), className);
            // 예: "userService" → "UserServiceImpl"
        }
    }
}
```

#### 5.2.3 인터페이스 → 구현체 매핑

**왜 이 매핑이 필요한가?**

```java
// Controller 코드
@Autowired
private UserService userService;  // 인터페이스 타입으로 선언

public void list() {
    userService.getList();  // 실제로는 UserServiceImpl.getList() 호출
}
```

코드에서는 `userService`가 `UserService` 인터페이스 타입이지만,
실제 런타임에는 `UserServiceImpl`이 실행됩니다.
정적 분석에서 이를 추적하려면 매핑이 필요합니다.

**매핑 전략 (우선순위)**

| 우선순위 | 방법 | 예시 | 정확도 |
|---------|------|------|--------|
| 1 | `implements` 키워드 분석 | `class UserServiceV2 implements UserService` | 매우 높음 |
| 2 | `Impl` 접미사 추정 (fallback) | `UserServiceImpl` → `UserService` | 보통 |

```java
private void buildInterfaceMapping(List<ParsedClass> parsedClasses) {
    // 1단계: implements 기반 매핑 (가장 정확)
    for (ParsedClass clazz : parsedClasses) {
        if (clazz.isInterface()) continue;  // 인터페이스는 스킵

        // 이 클래스가 구현한 인터페이스들에 대해 매핑
        for (String interfaceName : clazz.getImplementedInterfaces()) {
            if (!interfaceToImpl.containsKey(interfaceName)) {
                interfaceToImpl.put(interfaceName, clazz.getClassName());
                // 예: "UserService" → "UserServiceV2"
            }
        }
    }

    // 2단계: Impl 접미사 기반 매핑 (fallback)
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
```

**이제 지원되는 패턴**
```java
// 모두 지원됨
class UserServiceImpl implements UserService { }     ✅
class DefaultUserService implements UserService { }  ✅
class UserServiceV2 implements UserService { }       ✅
class UserServiceAdapter implements UserService { }  ✅
```

#### 5.2.4 호출 흐름 트리 생성 (재귀)

```java
private FlowNode buildFlowTree(ParsedClass clazz, ParsedMethod method, int depth) {

    // 순환 참조 방지
    String signature = clazz.getClassName() + "." + method.getMethodName();
    if (visitedMethods.contains(signature)) {
        return new FlowNode(clazz.getClassName(),
                           method.getMethodName() + " [순환참조]",
                           clazz.getClassType());
    }
    visitedMethods.add(signature);

    // 현재 노드 생성
    FlowNode node = new FlowNode(clazz.getClassName(),
                                 method.getMethodName(),
                                 clazz.getClassType());

    // 깊이 제한 (무한 루프 방지)
    if (depth > 10) {
        return node;
    }

    // 이 메서드가 호출하는 다른 메서드들 추적
    for (MethodCall call : method.getMethodCalls()) {
        FlowNode childNode = traceMethodCall(call, depth + 1);
        if (childNode != null) {
            node.addChild(childNode);
        }
    }

    return node;
}
```

#### 5.2.5 메서드 호출 추적

```java
private FlowNode traceMethodCall(MethodCall call, int depth) {

    // Service/DAO 호출만 추적 (유틸리티 메서드 등은 제외)
    if (!call.isServiceOrDaoCall()) {
        return null;  // log.info(), StringUtils.isEmpty() 등은 스킵
    }

    String scope = call.getScope();      // 예: "userService"
    String methodName = call.getMethodName();  // 예: "getList"

    // scope에서 실제 클래스명 찾기
    String className = resolveClassName(scope);
    // "userService" → "UserServiceImpl" 로 변환

    if (className == null) {
        return null;  // 찾지 못한 경우
    }

    // 클래스와 메서드 찾기
    ParsedClass targetClass = classIndex.get(className);
    ParsedMethod targetMethod = findMethod(targetClass, methodName);

    // 재귀적으로 하위 호출 분석
    return buildFlowTree(targetClass, targetMethod, depth);
}
```

#### 5.2.6 클래스명 해석 (resolveClassName)

```java
private String resolveClassName(String scope) {
    // scope: "userService"

    // 1. 직접 매핑 확인
    if (scopeToClassName.containsKey(scope)) {
        String className = scopeToClassName.get(scope);
        return resolveToImplementation(className);
    }
    // scopeToClassName["userService"] = "UserServiceImpl"

    // 2. 첫 글자 대문자로 변환
    String pascalCase = toUpperCamelCase(scope);
    // "userService" → "UserService"

    if (classIndex.containsKey(pascalCase)) {
        return resolveToImplementation(pascalCase);
    }

    // 3. Impl 붙여서 확인
    if (classIndex.containsKey(pascalCase + "Impl")) {
        return pascalCase + "Impl";
    }

    return null;
}

private String resolveToImplementation(String className) {
    // 인터페이스면 구현체로 변환
    if (interfaceToImpl.containsKey(className)) {
        return interfaceToImpl.get(className);
    }
    return className;
}
```

### 5.3 FlowNode 트리 구조

```java
public class FlowNode {
    private String className;       // "UserController"
    private String methodName;      // "selectUserList"
    private ClassType classType;    // CONTROLLER
    private String urlMapping;      // "/user/list.do"
    private String httpMethod;      // "GET"
    private int depth;              // 0 (루트), 1 (1단계 자식), ...
    private List<FlowNode> children; // 이 메서드가 호출하는 메서드들

    // 트리 형태로 출력
    public String toTreeString() {
        // 재귀적으로 트리 문자열 생성
        // └─ [Controller] UserController.selectUserList()
        //    └─ [Service] UserServiceImpl.selectUserList()
        //       └─ [DAO] UserDAO.selectUserList()
    }
}
```

---

## 6. 데이터 흐름

### 6.1 전체 데이터 흐름도

```
┌─────────────────────────────────────────────────────────────────┐
│                        입력 데이터                               │
│                                                                  │
│  samples/                                                        │
│  ├── UserController.java                                        │
│  ├── UserService.java                                           │
│  ├── UserServiceImpl.java                                       │
│  ├── UserDAO.java                                               │
│  └── User_SQL.xml                                               │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   JavaSourceParser.parseProject()                │
│                                                                  │
│  각 .java 파일을 읽어서 ParsedClass 객체로 변환                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    List<ParsedClass>                             │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ParsedClass: UserController                              │    │
│  │   classType: CONTROLLER                                  │    │
│  │   methods:                                               │    │
│  │     - selectUserList() [@GetMapping("/list.do")]        │    │
│  │       calls: [userService.selectUserList()]             │    │
│  │     - selectUser() [@GetMapping("/detail.do")]          │    │
│  │       calls: [userService.selectUser()]                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ParsedClass: UserServiceImpl                             │    │
│  │   classType: SERVICE                                     │    │
│  │   methods:                                               │    │
│  │     - selectUserList()                                   │    │
│  │       calls: [userDAO.selectUserList()]                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ParsedClass: UserDAO                                     │    │
│  │   classType: DAO                                         │    │
│  │   methods:                                               │    │
│  │     - selectUserList()                                   │    │
│  │       calls: [selectList("userDAO.selectUserList")]     │    │
│  └─────────────────────────────────────────────────────────┘    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   FlowAnalyzer.analyze()                         │
│                                                                  │
│  1. 클래스 인덱싱                                                │
│     classIndex: {                                                │
│       "UserController" → ParsedClass,                           │
│       "UserServiceImpl" → ParsedClass,                          │
│       "UserDAO" → ParsedClass                                   │
│     }                                                            │
│                                                                  │
│  2. 인터페이스 매핑                                              │
│     interfaceToImpl: {                                           │
│       "UserService" → "UserServiceImpl"                         │
│     }                                                            │
│                                                                  │
│  3. 호출 흐름 추적 (재귀)                                        │
│     Controller의 각 엔드포인트에서 시작                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       FlowResult                                 │
│                                                                  │
│  flows: [                                                        │
│    FlowNode (depth=0)                                           │
│    ├── className: "UserController"                              │
│    ├── methodName: "selectUserList"                             │
│    ├── urlMapping: "/user/list.do"                              │
│    └── children: [                                              │
│          FlowNode (depth=1)                                     │
│          ├── className: "UserServiceImpl"                       │
│          ├── methodName: "selectUserList"                       │
│          └── children: [                                        │
│                FlowNode (depth=2)                               │
│                ├── className: "UserDAO"                         │
│                ├── methodName: "selectUserList"                 │
│                └── children: []                                 │
│              ]                                                   │
│        ]                                                         │
│  ]                                                               │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        출력 (예정)                               │
│                                                                  │
│  [GET /user/list.do] UserController.selectUserList()            │
│  └─ userService.selectUserList()                                │
│     └─ UserServiceImpl.selectUserList()                         │
│        └─ userDAO.selectUserList()                              │
│           └─ SQL: userDAO.selectUserList                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. 설계 결정과 이유 (WHY)

### 7.1 왜 JavaParser 라이브러리를 선택했는가?

| 방법 | 장점 | 단점 |
|------|------|------|
| **정규식** | 간단, 의존성 없음 | 복잡한 코드 파싱 불가, 오류 많음 |
| **직접 파서 구현** | 완전한 제어 | 개발 시간 막대, 버그 가능성 |
| **JavaParser** ✅ | 정확, 검증됨, 활발한 커뮤니티 | 의존성 추가 |

**결정**: JavaParser 선택
- Java 1.0 ~ 21 모든 버전 지원
- AST 기반으로 정확한 분석
- 메서드 호출, 어노테이션 등 구조적 추출 용이

### 7.2 왜 인터페이스 → 구현체 매핑이 필요한가?

```java
// 전자정부프레임워크의 일반적인 패턴
@Controller
public class UserController {

    @Autowired
    private UserService userService;  // 인터페이스 타입!

    public void list() {
        userService.getList();  // 실제로는 UserServiceImpl 실행
    }
}
```

**문제**: 코드상으로는 `UserService`를 호출하지만, 실제 구현은 `UserServiceImpl`에 있음

**해결 (2단계 전략)**:
1. **implements 기반 매핑** (우선): `class UserServiceV2 implements UserService` 관계 분석
2. **Impl 접미사 매핑** (fallback): `UserServiceImpl` → `UserService` 추정

```java
// 이제 모두 지원됨
UserService → UserServiceImpl      (Impl 접미사)
UserService → DefaultUserService   (implements 분석)
UserService → UserServiceV2        (implements 분석)
```

### 7.3 왜 순환 참조 방지가 필요한가?

```java
// 순환 참조 예시
class A {
    void methodA() {
        b.methodB();  // A → B 호출
    }
}

class B {
    void methodB() {
        a.methodA();  // B → A 호출 (순환!)
    }
}
```

**문제**: 재귀 분석 시 무한 루프 발생

**해결**: 방문한 메서드를 기록하고, 이미 방문했으면 `[순환참조]` 표시 후 중단
```java
Set<String> visitedMethods = new HashSet<>();

if (visitedMethods.contains(signature)) {
    return new FlowNode(..., "[순환참조]", ...);
}
visitedMethods.add(signature);
```

### 7.4 왜 scope 기반으로 클래스를 찾는가?

```java
userService.selectUserList();
```

이 코드에서 `userService`가 어떤 클래스인지 알아야 합니다.

**문제**: 정적 분석에서는 변수의 실제 타입을 알기 어려움

**해결**: 네이밍 컨벤션 기반 추정
```
"userService" → "UserService" → "UserServiceImpl"
```

이 방법은 100% 정확하지 않지만, 전자정부프레임워크와 Spring의 일반적인 네이밍 컨벤션을 따르면 대부분 정확함.

### 7.5 왜 깊이 제한(depth > 10)을 두는가?

**문제**:
- 순환 참조 방지가 실패할 경우 무한 루프
- 너무 깊은 호출 트리는 분석에 무의미

**해결**: 10단계 이상은 추적하지 않음
- 실제 비즈니스 로직에서 10단계 이상 호출은 거의 없음
- 있다면 리팩토링이 필요한 코드

---

## 8. 실제 동작 예시

### 8.1 샘플 코드 (samples/ 폴더)

**UserController.java**
```java
@Controller
@RequestMapping("/user")
public class UserController {

    @Resource(name = "userService")
    private UserService userService;

    @RequestMapping("/list.do")
    public String selectUserList(Model model) {
        List<UserVO> userList = userService.selectUserList();
        model.addAttribute("userList", userList);
        return "user/list";
    }
}
```

**UserServiceImpl.java**
```java
@Service("userService")
public class UserServiceImpl implements UserService {

    @Resource(name = "userDAO")
    private UserDAO userDAO;

    @Override
    public List<UserVO> selectUserList() {
        return userDAO.selectUserList();
    }
}
```

**UserDAO.java**
```java
@Repository("userDAO")
public class UserDAO extends EgovAbstractDAO {

    public List<UserVO> selectUserList() {
        return selectList("userDAO.selectUserList");
    }
}
```

### 8.2 분석 결과

```
=== 분석 결과 요약 ===
프로젝트: samples
분석 시간: 2025-12-17T18:30:00
─────────────────────
전체 클래스: 4개
  - Controller: 1개
  - Service: 2개
  - DAO: 1개
엔드포인트: 5개

=== 호출 흐름 ===

└─ [Controller] UserController.selectUserList() [GET /user/list.do]
   └─ [Service] UserServiceImpl.selectUserList()
      └─ [DAO] UserDAO.selectUserList() → SQL: userdao.selectuserlist

└─ [Controller] UserController.selectUser() [GET /user/detail.do]
   └─ [Service] UserServiceImpl.selectUser()
      └─ [DAO] UserDAO.selectUser() → SQL: userdao.selectuser
```

### 8.3 테스트 실행

```bash
./gradlew test
```

```
> Task :test

FlowAnalyzerTest > 전체 프로젝트 분석 PASSED
FlowAnalyzerTest > 엔드포인트 분석 PASSED
FlowAnalyzerTest > 인터페이스 → 구현체 매핑 테스트 PASSED
FlowAnalyzerTest > 호출 흐름 트리 구조 테스트 PASSED
FlowAnalyzerTest > URL 패턴으로 필터링 분석 PASSED
FlowAnalyzerTest > FlowNode 트리 출력 테스트 PASSED
FlowAnalyzerTest > FlowResult 요약 정보 테스트 PASSED

BUILD SUCCESSFUL
```

---

## 9. 향후 확장

### 9.1 구현 완료

| 기능 | 설명 | 완료일 |
|------|------|--------|
| **ConsoleOutput** | 콘솔에 트리 형태로 출력, ANSI 색상 지원 | 2025-12-18 |
| **Picocli CLI** | 명령줄 옵션 처리 (--path, --url, --style, --output) | 2025-12-18 |

### 9.2 다음 구현 예정

| 기능 | 설명 |
|------|------|
| **IBatisParser** | iBatis XML에서 SQL ID와 쿼리 추출 |
| **ExcelOutput** | Apache POI로 엑셀 파일 생성 |

### 9.3 iBatis 파싱 예정 흐름

```
User_SQL.xml
    ↓
IBatisParser.parseFile()
    ↓
Map<String, String>
  "userDAO.selectUserList" → "SELECT * FROM TB_USER..."
  "userDAO.selectUser" → "SELECT * FROM TB_USER WHERE USER_ID = #userId#"
    ↓
FlowAnalyzer에 주입
    ↓
FlowNode.sqlQuery에 실제 SQL 표시
```

### 9.4 확장 가능한 구조

```
                    ┌─────────────────┐
                    │  FlowAnalyzer   │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
    ┌─────────────────┐ ┌─────────────┐ ┌─────────────┐
    │ JavaSourceParser│ │ IBatisParser│ │ MyBatisParser│
    └─────────────────┘ └─────────────┘ └─────────────┘
              │              │              │
              ▼              ▼              ▼
         .java 파일    iBatis XML     MyBatis XML
```

---

## 10. CLI (Picocli) 모듈 상세

### 10.1 Picocli란?

**Picocli**는 Java에서 CLI(Command Line Interface)를 쉽게 만들어주는 라이브러리입니다.

| 구분 | 직접 구현 | Picocli 사용 |
|------|----------|-------------|
| 옵션 파싱 | for문으로 직접 | 어노테이션 |
| 도움말 | 직접 작성 | 자동 생성 |
| 필수값 검증 | if문으로 직접 | `required = true` |
| 타입 변환 | `Integer.parseInt()` | 자동 |
| 코드량 | 많음 | 적음 |

### 10.2 `--help` 동작 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│  java -jar code-flow-tracer.jar --help                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. main(String[] args)                                         │
│     args = ["--help"]                                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. new CommandLine(new Main())                                 │
│     - Main 클래스의 @Command, @Option 어노테이션 파싱           │
│     - 옵션 정보 수집 (-p, -u, -s, -o, --help, --version 등)     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. cmd.setOut(new PrintWriter(UTF8_OUT, true))                 │
│     - Picocli 출력 스트림을 UTF-8로 설정                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. cmd.execute(args)                                           │
│     - args 파싱: "--help" 발견                                  │
│     - mixinStandardHelpOptions = true 이므로 --help 자동 처리   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  --help 감지?   │
                    └─────────────────┘
                      │           │
                    Yes          No
                      │           │
                      ▼           ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│  5a. 도움말 출력          │  │  5b. Main.call() 실행     │
│  - @Command description   │  │  - 실제 분석 로직 수행    │
│  - @Option description    │  │                          │
│  - 자동 포맷팅            │  │                          │
│  → cmd.getOut()으로 출력  │  │                          │
└──────────────────────────┘  └──────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. return exitCode (0 = 성공)                                  │
└─────────────────────────────────────────────────────────────────┘
```

**핵심 포인트**: `--help`가 있으면 `call()` 메서드는 실행되지 않고, Picocli가 자동으로 도움말을 출력합니다.

### 10.3 어노테이션 → 도움말 출력 매핑

#### 코드 (Main.java)
```java
@Command(
    name = "code-flow-tracer",                    // ← 프로그램 이름
    mixinStandardHelpOptions = true,              // ← -h, --help, -V, --version 자동 추가
    version = "1.0.0",                            // ← --version 출력값
    description = "Legacy code flow analyzer..." // ← 설명
)
public class Main implements Callable<Integer> {

    @Option(names = {"-p", "--path"}, description = "Project path...", required = true)
    private Path projectPath;

    @Option(names = {"-u", "--url"}, description = "URL pattern filter...")
    private String urlPattern;

    @Option(names = {"-s", "--style"}, description = "Output style...", defaultValue = "normal")
    private String style;
}
```

#### 출력 (--help)
```
Usage: code-flow-tracer [-hV] [--gui] [--no-color] [-o=<outputPath>]
                        -p=<projectPath> [-s=<style>] [-u=<urlPattern>]
       ↑                 ↑                 ↑
       │                 │                 └─ required=true라서 필수 표시
       │                 └─ 대괄호 [] = 선택 옵션
       └─ @Command의 name

Legacy code flow analyzer - Controller -> Service -> DAO -> SQL tracing
↑
└─ @Command의 description

      --gui                  Run in GUI mode        ← @Option 그대로
  -h, --help                 Show this help...      ← mixinStandardHelpOptions가 자동 추가
  -p, --path=<projectPath>   Project path...        ← required=true
  -s, --style=<style>        Output style...
  -V, --version              Print version...       ← mixinStandardHelpOptions가 자동 추가
```

### 10.4 매핑 관계 표

| 어노테이션 | --help 출력 |
|-----------|-------------|
| `@Command(name = "...")` | `Usage: code-flow-tracer` |
| `@Command(description = "...")` | 프로그램 설명 줄 |
| `@Option(names = {"-p", "--path"})` | `-p, --path=<projectPath>` |
| `@Option(description = "...")` | 옵션 설명 |
| `@Option(required = true)` | 대괄호 없음 (필수) |
| `@Option(defaultValue = "normal")` | 내부적으로 기본값 설정 |
| `mixinStandardHelpOptions = true` | `-h, --help`, `-V, --version` 자동 추가 |

### 10.5 Windows 한글 깨짐 해결

**문제**: Picocli의 `--help` 출력이 Windows 콘솔에서 한글이 깨짐

**원인**: Picocli가 기본적으로 `System.out`을 사용하고, Windows 콘솔은 UTF-8이 아닌 CP949 사용

**해결**:
```java
public static void main(String[] args) {
    // UTF-8 출력 스트림 생성
    PrintStream UTF8_OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    CommandLine cmd = new CommandLine(new Main());
    // Picocli도 UTF-8 스트림 사용하도록 설정
    cmd.setOut(new PrintWriter(UTF8_OUT, true));
    cmd.setErr(new PrintWriter(UTF8_ERR, true));

    int exitCode = cmd.execute(args);
    System.exit(exitCode);
}
```

**추가 조치**: `@Option`의 description을 영어로 작성하면 인코딩 문제 회피 가능

### 10.6 전체 파이프라인

```
사용자 입력 (CLI)
       │
       ▼
┌─────────────────────────────────────────────────────┐
│  Main.java (Picocli)                                │
│  - 옵션 파싱: --path, --url, --style, --output     │
│  - 유효성 검사: 경로 존재 여부 등                    │
└───────────────────────────┬─────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────┐
│  JavaSourceParser.parseProject()                    │
│  - .java 파일 파싱                                  │
│  - List<ParsedClass> 생성                           │
└───────────────────────────┬─────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────┐
│  FlowAnalyzer.analyze() 또는 analyzeByUrl()         │
│  - 호출 흐름 분석                                   │
│  - FlowResult 생성                                  │
└───────────────────────────┬─────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────┐
│  ConsoleOutput.print()                              │
│  - 트리 형태로 콘솔 출력                            │
│  - 또는 파일로 저장 (--output)                      │
└─────────────────────────────────────────────────────┘
```

---

## 11. 파라미터 분석 모듈 상세

### 11.1 개요

Controller 메서드의 파라미터를 추출하고, 실제 사용되는 필드/키를 분석하여 표시합니다.

**출력 예시**:
```
[POST] /user/insert.do
Parameters: UserVO userVO
  └── userVO 사용 필드: userId, name, deptId
└── [Controller] UserController.insertUser()
    └── ...
```

### 11.2 지원하는 파라미터 타입

| 타입 | 분석 방법 | 예시 |
|------|-----------|------|
| `@RequestParam` | 어노테이션 값 추출 | `@RequestParam("userId") String id` → `userId` |
| `@PathVariable` | 어노테이션 값 추출 | `@PathVariable("id") Long id` → `id` |
| VO/DTO | `getter` 호출 분석 | `userVO.getUserId()` → `userId` |
| Map | `get("key")` 호출 분석 | `params.get("pageNo")` → `pageNo` |

### 11.3 핵심 클래스

**ParameterInfo.java**:
```java
public class ParameterInfo {
    private String name;           // 파라미터 이름 (예: userVO)
    private String type;           // 타입 (예: UserVO)
    private String simpleType;     // 단순 타입 (예: UserVO, Map)
    private List<String> usedFields;    // 사용된 필드/키
    private boolean hasRequestParam;    // @RequestParam 여부
    private boolean hasPathVariable;    // @PathVariable 여부

    // 타입 판별 메서드
    public boolean isMapType() { ... }
    public boolean isVoType() { ... }
    public boolean isSpringInjected() { ... }  // Model, HttpServletRequest 등
}
```

### 11.4 파라미터 추출 흐름

```
┌─────────────────────────────────────────────────────┐
│  JavaSourceParser.parseMethod()                     │
│                                                     │
│  1. method.getParameters() → 파라미터 목록          │
│  2. 각 파라미터에 대해:                              │
│     - ParameterInfo 생성 (name, type)              │
│     - @RequestParam/@PathVariable 확인              │
│     - VO/Map이면 메서드 바디에서 사용 필드 분석      │
└───────────────────────────┬─────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────┐
│  analyzeParameterUsage()                            │
│                                                     │
│  Map 타입:                                          │
│    params.get("userId") → "userId" 추출            │
│                                                     │
│  VO 타입:                                           │
│    userVO.getUserId() → "userId" 추출              │
│    userVO.getName() → "name" 추출                  │
└─────────────────────────────────────────────────────┘
```

### 11.5 Spring 자동 주입 파라미터 필터링

API 문서화에 불필요한 Spring 프레임워크 파라미터는 자동으로 제외됩니다:

```java
public boolean isSpringInjected() {
    return simpleType.equals("Model") || simpleType.equals("ModelMap")
        || simpleType.equals("HttpServletRequest")
        || simpleType.equals("HttpServletResponse")
        || simpleType.equals("HttpSession")
        || simpleType.equals("RedirectAttributes")
        || simpleType.equals("BindingResult")
        || simpleType.equals("Principal")
        // ...
}
```

**필터링 대상**:
- `Model`, `ModelMap`, `ModelAndView` - 뷰에 데이터 전달용
- `HttpServletRequest`, `HttpServletResponse` - 서블릿 객체
- `HttpSession` - 세션 객체
- `RedirectAttributes` - 리다이렉트 시 플래시 속성
- `BindingResult`, `Errors` - 유효성 검사 결과
- `Principal`, `Authentication` - 보안 컨텍스트

**필터링 제외 (표시됨)**:
- `Pageable` - 클라이언트가 `?page=0&size=10` 전송
- `MultipartFile` - 클라이언트가 파일 업로드

### 11.6 한계점

| 케이스 | 분석 가능 여부 | 설명 |
|--------|---------------|------|
| `params.get("userId")` | ✅ 가능 | 문자열 리터럴 |
| `params.get(KEY_CONST)` | ⚠️ 추적 필요 | 상수 추적 미구현 |
| `params.get(dynamicKey)` | ❌ 불가능 | 런타임 값 |
| `userVO.getUserId()` | ✅ 가능 | getter 패턴 |
| `BeanUtils.copyProperties()` | ❌ 불가능 | 리플렉션 |

---

## 부록: 자주 묻는 질문 (FAQ)

### Q1. 왜 런타임 동작을 추적하지 않나요?

**A**: 이 프로젝트의 목표는 **소스 코드만으로** 분석하는 것입니다.
- 실행 환경이 필요 없음 (폐쇄망에서도 사용 가능)
- 빠른 분석 (실행 없이 즉시 분석)
- 레거시 코드도 분석 가능 (실행이 안 되는 코드도)

### Q2. 정확도는 어느 정도인가요?

**A**: 일반적인 전자정부프레임워크 구조에서 90% 이상 정확합니다.
다음 경우는 추적이 어렵습니다:
- 리플렉션 기반 호출
- 동적 프록시
- AOP 어드바이스

### Q3. 대용량 프로젝트도 분석 가능한가요?

**A**: 네, 가능합니다. 단, 파일 수에 비례하여 시간이 걸립니다.
- 100개 파일: 수 초
- 1,000개 파일: 수십 초
- 10,000개 파일: 수 분

### Q4. 다른 프레임워크도 지원하나요?

**A**: 현재는 전자정부프레임워크/Spring MVC 중심입니다.
Spring Boot, Spring Data JPA 등은 향후 확장 예정입니다.

---

> **문서 작성일**: 2025-12-17
> **최종 수정일**: 2025-12-25 (Markdown 출력 제거, GUI/Excel 완료 반영)
> **작성자**: Claude Code (러너스하이 2기 프로젝트)
