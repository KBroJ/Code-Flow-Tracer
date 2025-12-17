# 전체 설계 (Architecture)

> 최종 수정일: 2025-12-17

## 1. 프로젝트 개요

### 1.1 목적
레거시 Java 코드의 **API 엔드포인트 → Controller → Service → DAO → SQL** 호출 흐름을 자동으로 추적하고 문서화하는 도구

### 1.2 배경
- 4년간 공공 SI/SM 프로젝트 경험에서 느낀 Pain Point
- 레거시 코드 파악에 시간이 많이 소요됨
- 분기가 많은 코드에서 흐름 추적이 어려움
- 인수인계 시 문서화된 자료 부족

### 1.3 목표
```
[요청 URL: /api/user/list]
    ↓
[UserController.getList()]
    ↓
[UserService.findAll()]
    ↓
[UserDAO.selectUserList]
    ↓
[SQL: SELECT * FROM TB_USER...]
```

이 흐름을 **자동으로 분석**하고 **엑셀/마크다운으로 출력**

---

## 2. 시스템 아키텍처

### 2.1 전체 구조

```
┌─────────────────────────────────────────────────────────┐
│                      사용자 인터페이스                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │    CLI      │  │  Swing GUI  │  │   (확장)    │      │
│  └──────┬──────┘  └──────┬──────┘  └─────────────┘      │
└─────────┼────────────────┼──────────────────────────────┘
          │                │
          ▼                ▼
┌─────────────────────────────────────────────────────────┐
│                      Core Engine                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │                  FlowAnalyzer                    │    │
│  │  - 호출 흐름 추적                                │    │
│  │  - 클래스 간 연결                                │    │
│  └─────────────────────────────────────────────────┘    │
│           │                        │                     │
│           ▼                        ▼                     │
│  ┌─────────────────┐      ┌─────────────────┐           │
│  │ JavaSourceParser│      │  IBatisParser   │           │
│  │ - Java AST 분석 │      │ - XML SQL 파싱  │           │
│  └─────────────────┘      └─────────────────┘           │
└─────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────┐
│                      Output                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │   Console   │  │    Excel    │  │  Markdown   │      │
│  └─────────────┘  └─────────────┘  └─────────────┘      │
└─────────────────────────────────────────────────────────┘
```

### 2.2 패키지 구조

```
com.codeflow/
├── Main.java                 # 엔트리포인트, CLI 처리
│
├── parser/                   # 소스 코드 파싱
│   ├── JavaSourceParser.java # Java 소스 파싱 (JavaParser 사용)
│   ├── IBatisParser.java     # iBatis/MyBatis XML 파싱
│   ├── ParsedClass.java      # 파싱된 클래스 정보
│   ├── ParsedMethod.java     # 파싱된 메서드 정보
│   ├── MethodCall.java       # 메서드 호출 정보
│   └── ClassType.java        # 클래스 타입 (Controller, Service, DAO)
│
├── analyzer/                 # 호출 흐름 분석
│   ├── FlowAnalyzer.java     # 메인 분석 엔진
│   ├── FlowNode.java         # 흐름 노드 (트리 구조)
│   └── FlowResult.java       # 분석 결과
│
├── output/                   # 결과 출력
│   ├── OutputFormatter.java  # 출력 인터페이스
│   ├── ConsoleOutput.java    # 콘솔 출력
│   ├── ExcelOutput.java      # 엑셀 출력 (Apache POI)
│   └── MarkdownOutput.java   # 마크다운 출력
│
└── ui/                       # GUI
    ├── MainFrame.java        # 메인 윈도우
    └── ResultPanel.java      # 결과 표시 패널
```

---

## 3. 핵심 컴포넌트 설계

### 3.1 JavaSourceParser

**역할**: Java 소스 코드를 AST로 파싱하여 클래스, 메서드, 호출 관계 추출

**왜 JavaParser 라이브러리를 선택했는가?**
- 정규식 기반 파싱은 복잡한 코드에서 오류 발생
- AST 기반 분석으로 정확한 메서드 호출 관계 추출 가능
- Java 1.0 ~ 21까지 모든 버전 지원
- 활발한 커뮤니티, 풍부한 문서

```java
// 사용 예시
JavaSourceParser parser = new JavaSourceParser();
ParsedClass clazz = parser.parseFile(Path.of("UserController.java"));

// 결과
// clazz.getClassName() → "UserController"
// clazz.getClassType() → ClassType.CONTROLLER
// clazz.getMethods() → [selectUserList(), insertUser(), ...]
```

### 3.2 IBatisParser

**역할**: iBatis/MyBatis XML 파일에서 SQL ID와 실제 쿼리 매핑

**왜 JDOM2를 선택했는가?**
- 직관적인 API
- DOM 방식으로 전체 구조 파악 용이
- 네임스페이스 처리 지원

```java
// 사용 예시
IBatisParser parser = new IBatisParser();
Map<String, String> sqlMap = parser.parseFile(Path.of("User_SQL.xml"));

// 결과
// sqlMap.get("userDAO.selectUserList") → "SELECT USER_ID, USER_NAME FROM TB_USER..."
```

### 3.3 FlowAnalyzer

**역할**: 파싱된 클래스들을 연결하여 호출 흐름 트리 생성

**분석 알고리즘**:
1. 모든 Java 파일 파싱
2. Controller 클래스에서 시작
3. 메서드 호출을 따라가며 Service → DAO 연결
4. DAO 메서드에서 SQL ID 추출
5. iBatis XML에서 실제 SQL 매핑
6. 트리 구조로 결과 반환

```java
// 사용 예시
FlowAnalyzer analyzer = new FlowAnalyzer(parsedClasses, sqlMappings);
FlowResult result = analyzer.analyze("/api/user/list");

// 결과: 트리 구조
// UserController.selectUserList()
//   └→ userService.selectUserList()
//       └→ UserServiceImpl.selectUserList()
//           └→ userDAO.selectUserList()
//               └→ SQL: SELECT * FROM TB_USER...
```

### 3.4 Output Formatters

**역할**: 분석 결과를 다양한 형식으로 출력

| Formatter | 용도 | 라이브러리 |
|-----------|------|-----------|
| ConsoleOutput | 터미널 출력, 디버깅 | - |
| ExcelOutput | 인수인계 문서 | Apache POI |
| MarkdownOutput | README, Wiki | - |

---

## 4. 데이터 모델

### 4.1 ParsedClass
```java
public class ParsedClass {
    Path filePath;           // 파일 경로
    String packageName;      // 패키지명
    String className;        // 클래스명
    ClassType classType;     // CONTROLLER, SERVICE, DAO, OTHER
    List<ParsedMethod> methods;
}
```

### 4.2 ParsedMethod
```java
public class ParsedMethod {
    String methodName;       // 메서드명
    String returnType;       // 반환 타입
    String urlMapping;       // @RequestMapping URL
    String httpMethod;       // GET, POST, PUT, DELETE
    List<MethodCall> methodCalls;  // 내부에서 호출하는 메서드들
}
```

### 4.3 FlowNode (트리 구조)
```java
public class FlowNode {
    String className;
    String methodName;
    ClassType classType;
    String sqlId;            // DAO인 경우 SQL ID
    String sqlQuery;         // 실제 SQL
    List<FlowNode> children; // 호출하는 메서드들
}
```

---

## 5. 기술 선택 근거

### 5.1 왜 Java 17인가?
- Record, Pattern Matching 등 모던 문법 학습
- 장기 지원(LTS) 버전
- 분석 대상은 모든 Java 버전 지원

### 5.2 왜 Swing인가? (Web UI 대신)
| 기준 | Web UI | Swing |
|------|--------|-------|
| 폐쇄망 사용 | △ 서버 필요 | ◎ JAR만 있으면 됨 |
| 배포 | 복잡 | JAR 하나 |
| 의존성 | 브라우저 필요 | Java만 있으면 됨 |

**결론**: 폐쇄망 SI 환경에서 JAR 하나로 실행 가능한 Swing 선택

### 5.3 왜 Picocli인가?
- 어노테이션 기반 CLI 정의
- 자동 help 생성
- 타입 변환 자동 처리

---

## 6. 확장 계획

### Phase 1 (MVP, 4주)
- [x] 기본 파싱 (Controller, Service, DAO)
- [ ] 호출 흐름 연결
- [ ] iBatis XML 파싱
- [ ] 콘솔/엑셀 출력
- [ ] Swing GUI

### Phase 2 (향후)
- [ ] MyBatis 어노테이션 지원
- [ ] Spring Data JPA 지원
- [ ] 헥사고날 아키텍처 지원
- [ ] 시각화 (다이어그램 생성)

---

## 7. 제약사항

### 7.1 지원 범위
- 직접 메서드 호출만 추적
- 정적 분석 기반 (런타임 동작 X)

### 7.2 미지원 (MVP 범위)
- 이벤트 기반 호출 (@EventListener)
- AOP 프록시 동작
- 리플렉션 기반 호출
- 동적 프록시
