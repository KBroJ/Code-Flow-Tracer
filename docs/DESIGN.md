# 전체 설계 (Architecture)

> 최종 수정일: 2025-12-30

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

이 흐름을 **자동으로 분석**하고 **엑셀로 출력**

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
│  ┌─────────────┐  ┌─────────────┐                       │
│  │   Console   │  │    Excel    │                       │
│  └─────────────┘  └─────────────┘                       │
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
│   ├── ClassType.java        # 클래스 타입 (Controller, Service, DAO)
│   ├── SqlInfo.java          # SQL 상세 정보 (파일명, namespace, 타입, 테이블, 파라미터)
│   └── ParameterInfo.java    # 파라미터 정보 (@RequestParam, VO 필드 등)
│
├── analyzer/                 # 호출 흐름 분석
│   ├── FlowAnalyzer.java     # 메인 분석 엔진
│   ├── FlowNode.java         # 흐름 노드 (트리 구조)
│   └── FlowResult.java       # 분석 결과
│
├── output/                   # 결과 출력
│   ├── OutputFormatter.java  # 출력 인터페이스
│   ├── ConsoleOutput.java    # 콘솔 출력
│   └── ExcelOutput.java      # 엑셀 출력 (Apache POI)
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

**역할**: iBatis/MyBatis XML 파일에서 SQL 정보 추출 및 매핑

**왜 JDOM2를 선택했는가?**
- 직관적인 API
- DOM 방식으로 전체 구조 파악 용이
- 네임스페이스 처리 지원

**지원 형식**:
- iBatis: `<sqlMap namespace="...">` 루트 요소
- MyBatis: `<mapper namespace="...">` 루트 요소

**추출 정보**:
- 파일명, namespace, SQL ID
- SQL 타입 (SELECT, INSERT, UPDATE, DELETE)
- 반환타입 (resultClass, resultType, resultMap)
- 사용 테이블 (FROM, JOIN, INTO, UPDATE 키워드에서 추출)
- 전체 쿼리 (Excel 출력용)

**DTD 검증 비활성화**:
```java
// 폐쇄망 환경 대응 - 외부 DTD 로드 시도 방지
SAXBuilder builder = new SAXBuilder();
builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
```

```java
// 사용 예시
IBatisParser parser = new IBatisParser();
Map<String, SqlInfo> sqlMap = parser.parseProject(projectPath);

// 결과
SqlInfo info = sqlMap.get("userDAO.selectUserList");
// info.getFileName() → "User_SQL.xml"
// info.getType() → SqlType.SELECT
// info.getTables() → ["TB_USER"]
// info.getQuery() → "SELECT USER_ID, USER_NAME FROM TB_USER..."
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
    SqlInfo sqlInfo;         // SQL 상세 정보 (파일명, namespace, 타입, 테이블 등)
    List<FlowNode> children; // 호출하는 메서드들
}
```

### 4.4 SqlInfo (SQL 상세 정보)
```java
public class SqlInfo {
    String fileName;         // XML 파일명 (User_SQL.xml)
    String namespace;        // sqlMap/mapper namespace
    String sqlId;            // SQL ID (selectUserList)
    SqlType type;            // SELECT, INSERT, UPDATE, DELETE
    String resultType;       // 반환 타입 (UserVO, HashMap)
    List<String> tables;     // 사용 테이블 목록 [TB_USER, TB_DEPT]
    String query;            // 전체 SQL 쿼리 (Excel 출력용)
    List<String> sqlParameters; // SQL 파라미터 목록 [userId, deptId]
}
```

**SQL 파라미터 자동 추출**:
- iBatis 형식: `#paramName#` → `paramName`
- MyBatis 형식: `#{paramName}` → `paramName`
- MyBatis 객체 형식: `#{obj.property}` → `property`

```java
// 정규식 패턴
private static final Pattern IBATIS_PARAM_PATTERN = Pattern.compile("#([a-zA-Z_][a-zA-Z0-9_]*)#");
private static final Pattern MYBATIS_PARAM_PATTERN = Pattern.compile("#\\{([a-zA-Z_][a-zA-Z0-9_.]*)\\}");
```

---

## 4.5 ExcelOutput 설계

### 시트 구성
| 시트 | 용도 | 내용 |
|------|------|------|
| 요약 | 전체 현황 | 프로젝트 경로, 분석 시간, 클래스/엔드포인트 통계 |
| 호출 흐름 | 상세 분석 | 평면 테이블 형식 (레이어별 컬럼 분리) |
| SQL 목록 | SQL 목록 | 모든 SQL ID, 타입, 테이블, 쿼리 |

### 호출 흐름 시트 컬럼
```
No | HTTP | URL | 파라미터 | Controller | Service | DAO | SQL 파일 | SQL ID | 테이블 | 쿼리
```

### 파라미터 표시 전략
- **Controller 파라미터**: `@RequestParam`, `@PathVariable`, VO 사용 필드
- **SQL 파라미터**: `#param#`, `#{param}` 추출
- **합집합**: Controller + SQL 파라미터를 병합하여 표시

```
예시: /user/detail.do → DeptDAO.selectDept()
- Controller 파라미터: userId
- SQL 파라미터: deptId
- 표시: userId, deptId
```

### CLI 옵션
```bash
# 기본 경로로 엑셀 생성 (output/code-flow-result.xlsx)
java -jar code-flow-tracer.jar -p samples --excel

# 사용자 지정 출력 파일
java -jar code-flow-tracer.jar -p samples -o result.xlsx

# 출력 디렉토리 변경
java -jar code-flow-tracer.jar -p samples --excel -d exports
```

### 중복 파일명 처리
```
code-flow-result.xlsx (이미 존재)
→ code-flow-result (1).xlsx
→ code-flow-result (2).xlsx
...
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
- [x] 호출 흐름 연결
- [x] 콘솔 출력 (트리 형태, ANSI 색상)
- [x] iBatis XML 파싱 (SqlInfo, IBatisParser, DAO-SQL 연결)
- [x] 엑셀 출력 (요약, API 목록, 호출 흐름 시트)
- [x] Swing GUI (FlatLaf 다크 테마)

### Phase 2 (v1.1 - 기능 확장)
- [ ] 세션 영속성 (분석 결과 저장/복원) - [#15](https://github.com/KBroJ/Code-Flow-Tracer/issues/15)
- [ ] 작업 관리 탭 (Jira 스타일 칸반 보드) - [#16](https://github.com/KBroJ/Code-Flow-Tracer/issues/16)

### Phase 3 (향후)
- [ ] 분기 조건 파라미터 추출 (if/switch 분석)
- [ ] MyBatis 어노테이션 지원
- [ ] Spring Data JPA 지원
- [ ] 헥사고날 아키텍처 지원
- [ ] 시각화 (다이어그램 생성)

---

## 7. 제약사항 및 한계점

### 7.1 지원 범위
- 직접 메서드 호출만 추적
- 정적 분석 기반 (런타임 동작 X)

### 7.2 미지원 (MVP 범위)
- 이벤트 기반 호출 (@EventListener)
- AOP 프록시 동작
- 리플렉션 기반 호출
- 동적 프록시

### 7.3 정적 분석의 한계점

> 이 도구는 **정적 분석** 기반이므로 아래 케이스에서 한계가 있습니다.

#### 분기 조건 파라미터 추출 불가

```java
// Service 메서드
public void process(String gubun, String userId, String deptId) {
    if ("1".equals(gubun)) {
        userDAO.selectUser(userId);    // SQL: #userId#
    } else if ("2".equals(gubun)) {
        deptDAO.selectDept(deptId);    // SQL: #deptId#
    }
}
```

- **현재**: `gubun`이 분기 조건으로 사용된다는 것을 감지하지 못함
- **추출되는 파라미터**: userId, deptId (SQL 파라미터만)
- **누락**: gubun (분기 조건 파라미터)
- **이유**: if/switch 조건식 분석 구현 복잡도 (다양한 패턴 존재)

```java
// 다양한 분기 패턴 예시 - 모두 감지하기 어려움
if ("1".equals(gubun)) { ... }           // 단순 비교
if (gubun != null && gubun.equals(type)) // 복합 조건
switch (gubun) { case "1": ... }         // switch
dao = gubun.equals("1") ? dao1 : dao2;   // 삼항 연산자
if (StringUtils.equals(gubun, "1")) { }  // 유틸리티 메서드
```

#### 죽은 코드 판별 불가

```java
public void process(String type) {
    if (type.equals("A")) {
        daoA.select();  // 실제로 호출됨
    }
    if (false) {
        daoB.select();  // 죽은 코드 - 절대 실행 안됨
    }
}
```

- **현재**: daoA, daoB 모두 호출 흐름에 포함
- **이유**: 정적 분석으로는 `if (false)` 같은 명백한 경우만 판별 가능, 런타임 조건은 판별 불가

#### 동적 SQL ID 추출 불가

```java
// 정적 SQL ID - 추출 가능
dao.select("userDAO.selectUser", params);

// 동적 SQL ID - 추출 불가
String sqlId = "userDAO." + methodName;
dao.select(sqlId, params);

// 상수 기반 - 추출 불가 (상수 추적 미구현)
dao.select(SQL_ID_CONSTANT, params);
```

#### 동적 테이블명 추출 불가

```xml
<!-- 동적 테이블명 - 추출 불가 -->
SELECT * FROM $tableName$ WHERE ...
SELECT * FROM ${schemaName}.TB_USER WHERE ...
```

### 7.4 현재 파라미터 추출 전략

**결정**: Controller 파라미터 + SQL 파라미터 합집합

| 항목 | 추출 여부 | 방식 |
|------|----------|------|
| @RequestParam | ✅ | 어노테이션 값 추출 |
| @PathVariable | ✅ | 어노테이션 값 추출 |
| VO 사용 필드 | ✅ | getter 호출 분석 (userVO.getUserId() → userId) |
| Map.get() 키 | ✅ | 문자열 리터럴만 (params.get("key") → key) |
| SQL #param# | ✅ | 정규식 패턴 매칭 |
| SQL #{param} | ✅ | 정규식 패턴 매칭 |
| 분기 조건 파라미터 | ❌ | 미구현 (향후 과제) |

**왜 합집합인가?**
- API 호출 시 필요한 파라미터 (Controller) + SQL 실행 시 필요한 파라미터 (SQL)
- 두 정보 모두 산출물 작성에 유용
- 분기 파라미터 누락은 인정하되, 실용적 범위에서 최대한 추출

---

## 8. 다중 구현체 경고 기능

### 8.1 배경
정적 분석에서 인터페이스에 여러 구현체가 있을 경우, 실제 런타임에 어떤 구현체가 주입되는지 알 수 없습니다.
- Spring 설정 XML/Java Config
- 프로파일(@Profile)
- 조건부 빈(@Conditional)
- 우선순위(@Primary, @Order)

이 도구는 **첫 번째 발견된 구현체**를 사용하므로, 사용자에게 다른 구현체가 존재함을 경고합니다.

### 8.2 구현 방식

```java
// FlowAnalyzer.java
private final Map<String, List<String>> multipleImplWarnings = new HashMap<>();

private void buildInterfaceMapping(List<ParsedClass> parsedClasses) {
    // 1. 모든 구현체 수집
    Map<String, List<String>> interfaceToAllImpls = new HashMap<>();
    for (ParsedClass clazz : parsedClasses) {
        for (String interfaceName : clazz.getImplementedInterfaces()) {
            interfaceToAllImpls
                .computeIfAbsent(interfaceName, k -> new ArrayList<>())
                .add(clazz.getClassName());
        }
    }

    // 2. 첫 번째 구현체를 매핑에 사용
    // 3. 2개 이상이면 경고 목록에 추가
    for (Map.Entry<String, List<String>> entry : interfaceToAllImpls.entrySet()) {
        if (entry.getValue().size() > 1) {
            multipleImplWarnings.put(entry.getKey(), entry.getValue());
        }
    }
}
```

### 8.3 출력 형식

#### 콘솔 출력
```
└─ [Service] UserServiceImpl.selectUserList()  ←UserService  (외 UserServiceV2, UserServiceV3)
```
- 현재 사용 중인 구현체(UserServiceImpl) 제외
- 노란색으로 강조

#### 엑셀 출력
| 구분 | 내용 |
|------|------|
| 강조 색상 | 연한 살구색 (#FFF0E0) |
| 비고 칼럼 | `외 UserServiceV2, UserServiceV3` |
| 요약 시트 | 경고 설명 + 인터페이스별 구현체 목록 |

### 8.4 요약 시트 경고 섹션
```
[다중 구현체 경고]
※ 아래 인터페이스는 여러 구현체가 존재합니다.
※ 정적 분석의 한계로 첫 번째 구현체 기준으로 분석되었습니다.
※ 실제 런타임에 다른 구현체가 사용될 수 있으니 확인이 필요합니다.

인터페이스         구현체
UserService       UserServiceImpl, UserServiceV2, UserServiceV3
```

### 8.5 설계 결정 이유

1. **해결보다 경고 선택**
   - 어떤 구현체가 실제로 사용되는지 판별하려면 Spring 설정 파싱 필요
   - 복잡도 대비 가치가 낮음 → 경고로 사용자가 확인하도록 유도

2. **인라인 표시 선택**
   - 처음 시도: 상단에 요약 경고 → 어떤 Service인지 파악 어려움
   - 최종: 해당 Service 노드 옆에 표시 → 직관적

3. **비고 칼럼 + 요약 시트 조합**
   - 비고: 간결하게 다른 구현체 목록
   - 요약: 처음 보는 사용자를 위한 상세 설명

---

## 9. 설정 저장 및 배포

### 9.1 사용자 설정 저장

**저장 방식**: Java Preferences API

```java
// MainFrame.java
private final Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);

// 저장
prefs.put("recentPaths", String.join("|", paths));
prefs.put("urlFilter", filterText);
prefs.put("outputStyle", "normal");

// 로드
String pathsStr = prefs.get("recentPaths", "");
```

**저장 위치** (OS별):
| OS | 저장 위치 |
|----|----------|
| Windows | `HKCU\Software\JavaSoft\Prefs\com\codeflow\ui` |
| Linux | `~/.java/.userPrefs/com/codeflow/ui/` |
| macOS | `~/Library/Preferences/com.codeflow.ui.plist` |

**저장 항목**:
- `recentPaths`: 최근 프로젝트 경로 (최대 10개, `|`로 구분)
- `urlFilter`: URL 필터 패턴
- `outputStyle`: 출력 스타일 (compact/normal/detailed)

**설계 결정 이유**:
- Java 표준 API로 크로스 플랫폼 지원
- 별도 설정 파일 관리 불필요
- 설치 폴더(Program Files)에는 쓰기 권한이 없어 외부 저장 필요

### 9.2 설치 파일 (jpackage)

**빌드 방식**: JDK 내장 jpackage + WiX Toolset 3.14

```bash
./gradlew jpackage
# 출력: build/installer/CFT-1.0.0.exe (약 77MB)
```

**포함 내용**:
- 애플리케이션 JAR (code-flow-tracer.jar)
- 번들 JRE (Java 17 런타임)
- 네이티브 런처 (CFT.exe)

**커스터마이징 파일** (`installer-resources/`):
| 파일 | 용도 |
|------|------|
| `main.wxs` | 메인 WiX 프로젝트 (레지스트리 정리 추가) |
| `ShortcutPromptDlg.wxs` | 바로가기 선택 다이얼로그 (간격 수정) |

### 9.3 설치 삭제 시 정리

**자동 정리 항목** (WiX RemoveRegistryKey):
```
HKCU\Software\JavaSoft\Prefs\com\codeflow\ui  ← 설정값
HKCU\Software\JavaSoft\Prefs\com\codeflow     ← 상위 폴더
HKCU\Software\CFT                              ← 설치 마커
```

**수동 삭제 방법** (설치 파일 없이 JAR로 사용한 경우):
1. `Win + R` → `regedit` 실행
2. `HKEY_CURRENT_USER\Software\JavaSoft\Prefs\com\codeflow` 로 이동
3. `codeflow` 폴더 삭제

또는 PowerShell:
```powershell
Remove-Item -Path "HKCU:\Software\JavaSoft\Prefs\com\codeflow" -Recurse
```

---

## 10. 세션 영속성 (v1.1) - [#15](https://github.com/KBroJ/Code-Flow-Tracer/issues/15)

### 10.1 개요

앱 종료 후 재시작해도 마지막 분석 결과가 유지되어 바로 이어서 작업 가능

### 10.2 저장 항목

| 항목 | 저장 위치 | 형식 |
|------|----------|------|
| 프로젝트 경로 | Preferences | String |
| 분석 결과 (FlowResult) | 파일 | JSON |
| URL 필터 | Preferences | String |
| 출력 스타일 | Preferences | String |

### 10.3 세션 파일 경로

```
~/.code-flow-tracer/session.json
```

### 10.4 JSON 구조

```json
{
  "projectPath": "/path/to/project",
  "analyzedAt": "2025-12-30T14:30:00",
  "results": [
    {
      "urlPattern": "/api/user/list",
      "httpMethod": "GET",
      "parameters": ["userId", "pageNo"],
      "rootNode": {
        "className": "UserController",
        "methodName": "selectUserList",
        "classType": "CONTROLLER",
        "children": [...]
      }
    }
  ]
}
```

### 10.5 동작 흐름

```
앱 시작
  ↓
세션 파일 존재?
  ├── Yes → JSON 역직렬화 → 결과 패널에 표시
  └── No  → 빈 화면

분석 실행
  ↓
FlowResult 생성
  ↓
JSON 직렬화 → session.json 저장
```

### 10.6 구현 클래스

```java
// 신규 클래스
public class SessionManager {
    private static final Path SESSION_FILE =
        Paths.get(System.getProperty("user.home"), ".code-flow-tracer", "session.json");

    public void saveSession(SessionData data);
    public SessionData loadSession();
    public void clearSession();
}

public class SessionData {
    String projectPath;
    LocalDateTime analyzedAt;
    List<FlowResult> results;
}
```

### 10.7 라이브러리

- **Gson** (com.google.code.gson:gson:2.10.1)
  - 선택 이유: 가볍고, 추가 의존성 없음, 직관적인 API

---

## 11. 작업 관리 탭 (v1.1) - [#16](https://github.com/KBroJ/Code-Flow-Tracer/issues/16)

### 11.1 개요

폐쇄망 SI 환경에서 Notion, Jira 등을 사용할 수 없는 상황을 위한 내장 작업 관리 도구

### 11.2 UI 레이아웃

```
┌─────────────────────────────────────────────────────────────────┐
│  [분석 결과]  [작업 관리]                                          │
├─────────────────────────────────────────────────────────────────┤
│  [+ 이슈 추가]                                                    │
├───────────────────┬───────────────────┬─────────────────────────┤
│     📋 할 일 (3)   │   🔄 진행 중 (1)   │     ✅ 완료 (5)          │
├───────────────────┼───────────────────┼─────────────────────────┤
│ ┌───────────────┐ │ ┌───────────────┐ │ ┌─────────────────────┐ │
│ │ CFT-004       │ │ │ CFT-003       │ │ │ ~~CFT-001~~         │ │
│ │ 세션 영속성    │ │ │ 블로그 작성    │ │ │ ~~MVP 개발~~        │ │
│ │ 🔴 높음       │ │ │ 🟡 보통       │ │ │ 2025-12-25 완료     │ │
│ │ ~12/31       │ │ │ ~12/30       │ │ └─────────────────────┘ │
│ └───────────────┘ │ └───────────────┘ │ ┌─────────────────────┐ │
│ ┌───────────────┐ │                   │ │ ~~CFT-002~~         │ │
│ │ CFT-005       │ │                   │ │ ~~GUI 구현~~         │ │
│ │ 칸반 보드 구현  │ │                   │ │ 2025-12-26 완료     │ │
│ │ 🟡 보통       │ │                   │ └─────────────────────┘ │
│ │ ~01/02       │ │                   │                         │
│ └───────────────┘ │                   │                         │
└───────────────────┴───────────────────┴─────────────────────────┘
                              │
         이슈 카드 클릭 시 상세 패널 표시
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  이슈 상세                                         [저장] [삭제] │
├─────────────────────────────────────────────────────────────────┤
│  번호: CFT-003                                                   │
│  제목: [블로그 작성                                        ]     │
│  설명: [호출흐름 재귀탐색 블로그 포스트 작성              ]     │
│        [                                                  ]     │
│  상태: [진행 중 ▼]  (드롭다운으로 변경)                          │
│  우선순위: [🟡 보통 ▼]                                           │
│  시작일: [2025-12-28]  마감일: [2025-12-30]                      │
└─────────────────────────────────────────────────────────────────┘
```

### 11.3 데이터 모델

```java
public class Issue {
    String issueId;          // CFT-001, CFT-002...
    String title;            // 제목
    String description;      // 설명 (여러 줄)
    IssueStatus status;      // TODO, IN_PROGRESS, DONE
    IssuePriority priority;  // HIGH, MEDIUM, LOW
    LocalDate startDate;     // 시작일
    LocalDate dueDate;       // 마감일
    LocalDateTime createdAt; // 생성일시
    LocalDateTime updatedAt; // 수정일시
}

public enum IssueStatus {
    TODO("할 일"),
    IN_PROGRESS("진행 중"),
    DONE("완료");
}

public enum IssuePriority {
    HIGH("🔴 높음"),
    MEDIUM("🟡 보통"),
    LOW("🟢 낮음");
}
```

### 11.4 저장 구조

```
~/.code-flow-tracer/issues.json
```

```json
{
  "nextIssueNumber": 6,
  "issues": [
    {
      "issueId": "CFT-001",
      "title": "MVP 개발",
      "description": "기본 파싱 및 분석 기능 구현",
      "status": "DONE",
      "priority": "HIGH",
      "startDate": "2025-12-16",
      "dueDate": "2025-12-25",
      "createdAt": "2025-12-16T09:00:00",
      "updatedAt": "2025-12-25T18:30:00"
    }
  ]
}
```

### 11.5 기능 목록

| 기능 | 설명 |
|------|------|
| 이슈 추가 | + 버튼으로 새 이슈 생성, 자동 번호 부여 (CFT-XXX) |
| 이슈 수정 | 카드 클릭 → 상세 패널에서 편집 |
| 이슈 삭제 | 상세 패널에서 삭제 버튼 |
| 상태 변경 | 드롭다운으로 상태 변경 (칸반 컬럼 이동) |
| 취소선 표시 | 완료 상태 이슈는 제목에 취소선 |
| 우선순위 표시 | 카드에 아이콘으로 표시 |
| 마감일 표시 | 카드 하단에 간략히 표시 |
| 데이터 영속성 | JSON 파일로 저장/불러오기 |

### 11.6 구현 클래스

```
com.codeflow.ui/
├── MainFrame.java          # 탭 추가
├── TaskPanel.java          # 작업 관리 메인 패널 (신규)
├── KanbanColumn.java       # 칸반 컬럼 (신규)
├── IssueCard.java          # 이슈 카드 컴포넌트 (신규)
├── IssueDetailPanel.java   # 이슈 상세/편집 패널 (신규)
└── IssueManager.java       # 이슈 CRUD + 저장/로드 (신규)
```

### 11.7 설계 결정

**왜 드래그앤드롭이 아닌 드롭다운인가?**
- Swing에서 드래그앤드롭 구현 복잡도 높음
- 드롭다운도 1클릭으로 상태 변경 가능
- 구현 시간 단축 (2-3일 → 1-2일)

**왜 별도 탭인가?**
- 분석 결과와 작업 관리는 독립적 기능
- 탭으로 분리하여 UI 복잡도 감소
- 향후 확장 용이 (다른 프로젝트의 작업도 관리 가능)
