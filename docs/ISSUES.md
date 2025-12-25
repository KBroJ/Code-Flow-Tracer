# 문제 및 해결 과정 (Troubleshooting)

> 개발 중 마주친 문제와 해결 과정을 기록합니다.
> 러너스하이의 핵심: "왜 문제가 발생했는가? 어떻게 해결할 것인가?"

---

## 문제 기록 템플릿

```markdown
### [#번호] 문제 제목

**발생일**: YYYY-MM-DD
**상태**: 🔴 미해결 / 🟡 진행중 / 🟢 해결됨

#### 문제 상황
- 어떤 상황에서 발생했는지
- 에러 메시지 또는 증상

#### 원인 분석
- 왜 이 문제가 발생했는지
- 디버깅 과정

#### 시도한 해결책
1. 첫 번째 시도 - 결과
2. 두 번째 시도 - 결과

#### 최종 해결
- 어떻게 해결했는지
- 코드 변경 내용

#### 배운 점
- 이 문제를 통해 배운 것
- 앞으로 주의할 점
```

---

## 해결된 문제

### [#001] 미사용 import로 인한 컴파일 에러

**발생일**: 2025-12-17
**상태**: 🟢 해결됨

#### 문제 상황
테스트 실행 시 컴파일 에러 발생

```
> Task :compileJava FAILED
C:\Devel\think\code-flow-tracer\src\main\java\com\codeflow\Main.java:3: error: package com.codeflow.analyzer does not exist
import com.codeflow.analyzer.FlowAnalyzer;
                            ^
C:\Devel\think\code-flow-tracer\src\main\java\com\codeflow\Main.java:4: error: package com.codeflow.output does not exist
import com.codeflow.output.ConsoleOutput;
                          ^
2 errors
```

#### 원인 분석
- Main.java에서 아직 구현하지 않은 클래스를 import
- FlowAnalyzer, ConsoleOutput은 TODO로 남겨둔 상태
- 코드 스켈레톤 작성 시 미리 import를 추가해둔 것이 원인

#### 시도한 해결책
1. 빈 클래스 생성 - 불필요한 코드 증가로 보류
2. import 제거 - ✅ 채택

#### 최종 해결
Main.java에서 미사용 import 제거

```java
// 제거된 코드
import com.codeflow.analyzer.FlowAnalyzer;
import com.codeflow.output.ConsoleOutput;
```

#### 배운 점
- 아직 구현하지 않은 클래스는 import하지 않기
- TODO 주석으로 남겨두고, 실제 구현할 때 import 추가
- IDE의 "Optimize Imports" 기능 활용

### [#002] IntelliJ "Project JDK is not defined" 에러

**발생일**: 2025-12-17
**상태**: 🟢 해결됨

#### 문제 상황
IntelliJ에서 프로젝트를 열었을 때 "Project JDK is not defined" 에러 발생
- 모든 Java 파일에서 빨간 에러 표시
- 코드 자동완성, 문법 검사 불가

#### 원인 분석
- `.idea/misc.xml`에 ProjectRootManager 설정 누락
- Gradle toolchain 미설정으로 IntelliJ가 JDK를 자동 인식하지 못함
- 시스템에 Java 21 설치되어 있지만 프로젝트와 연결되지 않음

#### 시도한 해결책
1. `.idea/misc.xml`에 JDK 설정 추가 - ✅ 효과 있음
2. `build.gradle`에 toolchain 설정 추가 - ✅ 근본적 해결

#### 최종 해결

**1. build.gradle에 toolchain 추가**
```groovy
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // IntelliJ에서 자동으로 JDK를 찾도록 toolchain 설정
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

**2. Gradle 프로젝트 동기화**
- IntelliJ에서 Gradle 새로고침 (🔄) 클릭
- 또는 `File` → `Sync Project with Gradle Files`

#### 배운 점
- Gradle toolchain 설정으로 IDE가 자동으로 JDK를 찾게 할 수 있음
- 프로젝트 타겟 버전(17)과 실행 환경(21)은 다를 수 있음
- 호환성을 위해 최소 요구 버전으로 빌드하는 것이 좋음

### [#003] 인터페이스-구현체 매핑이 Impl 접미사에만 의존

**발생일**: 2025-12-17
**상태**: 🟢 해결됨

#### 문제 상황
기존 인터페이스-구현체 매핑 로직이 클래스명 `Impl` 접미사에만 의존
```java
// 지원됨
UserServiceImpl → UserService ✅

// 지원 안 됨
DefaultUserService → UserService ❌
UserServiceV2 → UserService ❌
UserServiceAdapter → UserService ❌
```

#### 원인 분석
- 네이밍 컨벤션에만 의존하는 단순한 로직
- 실제 `implements` 관계를 분석하지 않음
- 다양한 네이밍 패턴을 가진 레거시 코드에서 매핑 실패

#### 시도한 해결책
1. `implements` 키워드 기반 매핑 추가 - ✅ 채택
2. `Impl` 접미사는 fallback으로 유지 - ✅ 채택

#### 최종 해결

**1. ParsedClass에 필드 추가**
```java
private boolean isInterface;
private List<String> implementedInterfaces = new ArrayList<>();
```

**2. JavaSourceParser에서 implements 정보 추출**
```java
// 인터페이스 여부 확인
parsedClass.setInterface(clazz.isInterface());

// 구현한 인터페이스 목록 추출
clazz.getImplementedTypes().forEach(implementedType -> {
    parsedClass.addImplementedInterface(implementedType.getNameAsString());
});
```

**3. FlowAnalyzer 매핑 로직 개선**
```java
// 1단계: implements 기반 매핑 (가장 정확)
for (String interfaceName : clazz.getImplementedInterfaces()) {
    interfaceToImpl.put(interfaceName, clazz.getClassName());
}

// 2단계: Impl 접미사 기반 매핑 (fallback)
if (className.endsWith("Impl") && !interfaceToImpl.containsKey(interfaceName)) {
    interfaceToImpl.put(interfaceName, className);
}
```

#### 배운 점
- 정적 분석에서는 AST 정보를 최대한 활용해야 함
- 네이밍 컨벤션 기반 추정은 fallback으로만 사용
- JavaParser의 `getImplementedTypes()`로 정확한 관계 추출 가능
- 테스트 케이스로 개선 사항 검증 필수

### [#004] Windows 환경에서 콘솔 한글 출력 깨짐

**발생일**: 2025-12-18
**상태**: 🟢 해결됨

#### 문제 상황
IntelliJ에서 ConsoleOutputDemo 실행 시 한글이 깨져서 출력
```
// 예상 출력
전체 클래스:         4개

// 실제 출력
��ü Ŭ����:         4��
```

#### 원인 분석
- `System.out`은 JVM 기본 인코딩 사용
- Windows 기본 인코딩: CP949 (한글) 또는 CP1252 (영문)
- Java 코드는 UTF-8로 작성, 출력은 CP949로 해석 → 깨짐
- Linux/Mac은 기본 UTF-8이라 문제 없음

#### 시도한 해결책
1. IntelliJ 설정 변경 (Console Encoding → UTF-8) - 개발환경에서만 해결
2. JVM 옵션 `-Dfile.encoding=UTF-8` - 사용자가 매번 추가해야 함
3. 코드에서 UTF-8 PrintStream 강제 생성 - ✅ 채택 (근본적 해결)

#### 최종 해결 (1차 - 동작하지만 비효율적)

**ConsoleOutput.java에 UTF-8 PrintStream 생성 메서드 추가**
```java
private static PrintStream createUtf8PrintStream() {
    try {
        return new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
        // UTF-8은 항상 지원되므로 발생하지 않음
        return System.out;
    }
}
```

#### 개선된 해결 (2차 - Java 10+ 최적화)

1차 해결의 문제점:
- 매번 새 PrintStream 객체 생성 (메모리 낭비)
- checked exception 처리가 장황함
- `String` 인코딩명 사용 (`.name()` 호출 필요)

**Java 10+ API 활용한 싱글톤 패턴**
```java
// Before (8줄)
private static PrintStream createUtf8PrintStream() {
    try {
        return new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
        return System.out;
    }
}

// After (2줄)
private static final PrintStream UTF8_OUT =
    new PrintStream(System.out, true, StandardCharsets.UTF_8);
```

**개선 효과:**
| 항목 | Before | After |
|------|--------|-------|
| 코드량 | 8줄 | 2줄 |
| 예외 처리 | try-catch 필요 | 불필요 |
| 객체 생성 | 매번 새로 | 싱글톤 재사용 |
| API | `.name()` 호출 | `Charset` 직접 전달 |

**왜 가능한가?**
- Java 10+에서 `PrintStream(OutputStream, boolean, Charset)` 생성자 추가
- `Charset`을 직접 받으므로 `UnsupportedEncodingException` 발생 안 함
- 프로젝트 타겟이 Java 17이므로 사용 가능

#### 배운 점
- `System.out`은 플랫폼 기본 인코딩에 의존 → 이식성 문제
- CLI 도구 개발 시 명시적 인코딩 설정 필수
- **Java 버전별 API 개선사항 확인 필요** (Java 10+ PrintStream 개선)
- 싱글톤 패턴으로 불필요한 객체 생성 방지
- checked exception이 필요 없는 API가 있다면 그것을 사용

### [#005] 콘솔 박스 출력 시 한글 정렬 어긋남

**발생일**: 2025-12-18
**상태**: 🟢 해결됨 (부분적)

#### 문제 상황
콘솔 박스 출력 시 한글이 포함되면 오른쪽 테두리가 어긋남
```
// 예상 출력
┌──────────────────────────────────────────────────┐
│      Code Flow Tracer - 호출 흐름 분석 결과      │
└──────────────────────────────────────────────────┘

// 실제 출력 (IntelliJ)
┌──────────────────────────────────────────────────┐
│      Code Flow Tracer - 호출 흐름 분석 결과    │
└──────────────────────────────────────────────────┘
```

#### 원인 분석
- Java `String.length()`는 문자 개수만 반환
- 한글은 터미널에서 2칸 폭으로 표시됨 (영문은 1칸)
- 가운데 정렬 시 실제 표시 폭이 아닌 문자 수로 계산 → 어긋남

#### 최종 해결

**한글 폭 계산 메서드 추가**
```java
private int getDisplayWidth(String text) {
    int width = 0;
    for (char c : text.toCharArray()) {
        if (isWideChar(c)) {
            width += 2;  // 한글, CJK 문자는 2칸
        } else {
            width += 1;
        }
    }
    return width;
}

private boolean isWideChar(char c) {
    Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
    return block == Character.UnicodeBlock.HANGUL_SYLLABLES
        || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
        // ... 기타 CJK 블록
        || (c >= 0xFF00 && c <= 0xFFEF);  // 전각 문자
}
```

#### 남은 이슈
- **코드는 정확하게 계산함**
- IntelliJ 콘솔 폰트에 따라 여전히 어긋날 수 있음
- 실제 CLI 환경(Windows Terminal, CMD)에서는 정상 출력 예상
- IntelliJ 콘솔에서 정확히 보려면 고정폭 한글 폰트 필요 (D2Coding, NanumGothicCoding)

#### 배운 점
- 터미널 출력 시 문자 폭(display width) 고려 필요
- `Character.UnicodeBlock`으로 문자 종류 판별 가능
- 같은 코드도 터미널/폰트에 따라 다르게 보일 수 있음
- 최종 배포 환경에서 테스트하는 것이 중요

---

### Issue #006: Picocli --help 한글 깨짐

**발생일**: 2025-12-18
**상태**: ✅ 해결

#### 문제 상황
```bash
PS C:\> java -jar code-flow-tracer.jar --help

# 출력 (깨짐)
?덇굅??肄붾뱶 ?먮쫫 遺꾩꽍 ?꾧뎄 - Controller ??Service ??DAO ??SQL 異붿쟻
      --gui                  GUI 紐⑤뱶濡??ㅽ뻾
```

- IntelliJ 터미널, Windows PowerShell, CMD 모두 동일하게 발생
- `-Dfile.encoding=UTF-8` 설정해도 해결 안 됨
- `chcp 65001`도 효과 없음

#### 원인 분석

**Picocli의 기본 동작**:
1. Picocli는 `System.out`을 직접 사용
2. Windows 콘솔 기본 인코딩은 CP949 (한글 Windows) 또는 CP1252
3. Java는 UTF-8로 한글 바이트를 출력
4. 콘솔은 CP949로 해석 → 깨짐

**우리가 만든 ConsoleOutput은 왜 괜찮았나?**
- `new PrintStream(System.out, true, StandardCharsets.UTF_8)` 사용
- Picocli의 `--help`는 이 스트림을 사용하지 않음

#### 해결 방법

**Picocli 출력 스트림 명시적 설정**:
```java
public static void main(String[] args) {
    // UTF-8 출력 스트림
    PrintStream UTF8_OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    CommandLine cmd = new CommandLine(new Main());
    // Picocli도 UTF-8 스트림 사용하도록 설정
    cmd.setOut(new PrintWriter(UTF8_OUT, true));
    cmd.setErr(new PrintWriter(UTF8_ERR, true));

    int exitCode = cmd.execute(args);
    System.exit(exitCode);
}
```

**추가 조치 - @Option description 영어화**:
```java
// Before (한글 - 깨질 수 있음)
@Option(names = {"-p", "--path"}, description = "분석할 프로젝트 경로 (필수)")

// After (영어 - 안전)
@Option(names = {"-p", "--path"}, description = "Project path to analyze (required)")
```

**분석 결과 출력 (한글) 해결 - 배치 파일에 chcp 추가**:
```batch
REM scripts/analyze.bat
@echo off
REM UTF-8 콘솔 출력 설정 (한글 깨짐 방지)
chcp 65001 > nul 2>&1

java -jar build\libs\code-flow-tracer.jar %*
```

**왜 Java 코드에서 chcp 실행이 안 되나?**
- `ProcessBuilder`로 `chcp 65001` 실행 시 **자식 프로세스**의 코드 페이지만 변경됨
- 부모 콘솔(Java가 실행 중인)은 영향 없음
- 배치 파일에서 실행하면 **같은 콘솔**에서 코드 페이지 변경 → 동작함

#### 최종 해결

| 실행 방법 | 한글 출력 |
|----------|----------|
| `scripts\analyze.bat -p samples` | ✅ 정상 |
| `chcp 65001` 후 `java -jar ...` | ✅ 정상 |
| `java -jar ...` 직접 실행 | ❌ 깨짐 |
| `--output result.txt` 파일 저장 | ✅ 정상 |

#### 배운 점
- 라이브러리가 `System.out`을 직접 사용하면 인코딩 문제 발생 가능
- 라이브러리 초기화 시 출력 스트림을 명시적으로 설정해야 함
- CLI 도움말은 영어로 작성하면 인코딩 문제 회피 가능
- Windows 콘솔 코드 페이지는 **같은 프로세스**에서 변경해야 적용됨
- 배치 파일 래퍼가 Windows 환경에서 인코딩 문제 해결에 효과적

### Issue #007: 순환참조 오탐 (같은 메서드 다른 경로 호출 시 잘못된 표시)

**발생일**: 2025-12-18
**상태**: ✅ 해결

#### 문제 상황
```
─── 1/7 ────────────────────────────────────────
[GET] /api/webtoons
└── [Controller] ContentApiController.getMainWebtoons()
    ├── [Service] WebtoonService.getFeaturedContent()
    │   └── [DAO/Repository] ContentRepository.findTop5ByOrderByViewCountDesc()
    ├── [Service] WebtoonService.getPopularContent()
    │   └── [DAO/Repository] ContentRepository.findTop5ByOrderByViewCountDesc [순환참조]()  ← 잘못됨!
    └── [Service] WebtoonService.getTodayContent()
        └── [DAO/Repository] ContentRepository.findBySerializationDay()
```

- 같은 Repository 메서드를 다른 Service에서 호출하면 `[순환참조]`로 표시됨
- 이것은 진짜 순환참조(A→B→A)가 아님
- 단순히 같은 메서드를 두 번 호출한 것

#### 원인 분석

**기존 로직**:
```java
// FlowAnalyzer.java
private Set<String> visitedMethods = new HashSet<>();  // 전체 분석에서 공유

private FlowNode buildFlowTree(...) {
    String signature = clazz.getClassName() + "." + method.getMethodName();
    if (visitedMethods.contains(signature)) {
        // 이미 방문한 메서드 → [순환참조]로 표시
        return new FlowNode(..., methodName + " [순환참조]", ...);
    }
    visitedMethods.add(signature);
    // ...
}
```

**문제점**:
- `visitedMethods`가 전체 분석에서 공유됨
- 경로 A에서 `findTop5`를 방문 → Set에 추가
- 경로 B에서 `findTop5` 호출 시 이미 Set에 있음 → 순환참조로 오탐

#### 해결 방법

**호출 스택 방식으로 변경**:
```java
private FlowNode buildFlowTree(...) {
    String signature = clazz.getClassName() + "." + method.getMethodName();

    // 현재 호출 스택에 이미 있으면 = 진짜 순환 (A→B→A)
    if (visitedMethods.contains(signature)) {
        return new FlowNode(...);  // 라벨 없이 반환 (무한 루프만 방지)
    }

    visitedMethods.add(signature);  // 스택에 추가

    // ... 자식 노드 탐색 ...

    visitedMethods.remove(signature);  // 탐색 완료 → 스택에서 제거

    return node;
}
```

**핵심 변경**:
- 탐색 완료 후 `visitedMethods.remove(signature)` 추가
- `visitedMethods`가 "전체 방문 기록"이 아닌 "현재 호출 스택" 역할
- 다른 경로에서 같은 메서드 호출 가능

#### 결과

**수정 후**:
```
├── [Service] WebtoonService.getFeaturedContent()
│   └── [DAO/Repository] ContentRepository.findTop5ByOrderByViewCountDesc()
├── [Service] WebtoonService.getPopularContent()
│   └── [DAO/Repository] ContentRepository.findTop5ByOrderByViewCountDesc()  ← 정상 표시!
```

#### 배운 점
- 순환참조 체크는 "전체 방문"이 아닌 "현재 경로(호출 스택)"로 해야 정확
- 트리 탐색에서 백트래킹 시 상태 복원(remove) 필요
- 라벨(`[순환참조]`)을 붙이기 전에 실제로 순환인지 확인 필요

### Issue #008: 엑셀 파라미터 컬럼에 Controller 파라미터만 표시되는 문제

**발생일**: 2025-12-19
**상태**: ✅ 해결

#### 문제 상황
```
/user/detail.do
├── [행1] Controller → Service.selectUser() → DAO.selectUser() → SQL: #userId#
└── [행2] Controller → Service.selectDeptName() → DAO.selectDept() → SQL: #deptId#

현재 결과: 행1, 행2 모두 "userId" 표시
기대 결과: 행1은 "userId", 행2는 "userId, deptId"
```

- 모든 행에 Controller 파라미터(userId)만 표시됨
- SQL에서 실제 사용하는 파라미터(#deptId#)가 누락됨

#### 원인 분석
```java
// ExcelOutput.java:177-179
for (FlowNode flow : result.getFlows()) {
    // ❌ 문제: flow는 Controller 노드 → Controller 파라미터만 가져옴
    String paramStr = formatParameters(flow.getParameters());

    // ❌ 모든 행에 동일한 paramStr 적용
    for (FlatFlowRow flatRow : flatRows) {
        createCell(row, 3, paramStr, rowStyle);
    }
}
```

- `flow`는 루트 노드(Controller)
- `flow.getParameters()`로 Controller 파라미터만 추출
- 각 행(경로)별 SQL 파라미터를 고려하지 않음

#### 고민했던 해결 방안

| 방안 | 설명 | 장단점 |
|------|------|--------|
| SQL 파라미터만 | SQL에서 #param# 추출 | 분기 파라미터(gubun) 누락 |
| Controller만 | 기존 방식 유지 | SQL별 파라미터 차이 표현 불가 |
| **합집합** | Controller + SQL 파라미터 | ✅ 채택 - 실용적 범위 |
| 컬럼 분리 | API/SQL 파라미터 별도 컬럼 | 복잡, 컬럼 증가 |

**분기 파라미터 자동 추출 검토**:
```
Controller.getUser(userId, gubun)
├── if(gubun==1) → DAO1.select1() → #userId#
└── if(gubun==2) → DAO2.select2() → #deptId#
```
- gubun은 SQL에서 사용 안 됨, 분기 결정에만 사용
- 추출하려면 if/switch 조건문 AST 분석 필요 → 큰 작업
- **향후 과제로 결정**

#### 최종 해결

**1. SqlInfo에 SQL 파라미터 추출 기능 추가**
```java
// SqlInfo.java
private static final Pattern IBATIS_PARAM_PATTERN = Pattern.compile("#([a-zA-Z_][a-zA-Z0-9_]*)#");
private static final Pattern MYBATIS_PARAM_PATTERN = Pattern.compile("#\\{([a-zA-Z_][a-zA-Z0-9_.]*)\\}");

private void extractParametersFromQuery(String query) {
    Set<String> params = new HashSet<>();

    // iBatis: #paramName#
    Matcher ibatisMatcher = IBATIS_PARAM_PATTERN.matcher(query);
    while (ibatisMatcher.find()) {
        params.add(ibatisMatcher.group(1));
    }

    // MyBatis: #{paramName} 또는 #{obj.property}
    Matcher mybatisMatcher = MYBATIS_PARAM_PATTERN.matcher(query);
    while (mybatisMatcher.find()) {
        String param = mybatisMatcher.group(1);
        if (param.contains(".")) {
            param = param.substring(param.lastIndexOf('.') + 1);
        }
        params.add(param);
    }

    sqlParameters.addAll(params);
}
```

**2. ExcelOutput에서 Controller + SQL 파라미터 합집합**
```java
// 각 행별로 파라미터 합집합 계산
for (FlatFlowRow flatRow : flatRows) {
    String paramStr = mergeParameters(controllerParams, flatRow.sqlParams);
    createCell(row, 3, paramStr, rowStyle);
}

private String mergeParameters(Set<String> controllerParams, List<String> sqlParams) {
    Set<String> merged = new LinkedHashSet<>();
    merged.addAll(controllerParams);  // Controller 파라미터 먼저
    merged.addAll(sqlParams);         // SQL 파라미터 추가 (중복 제거)
    return merged.isEmpty() ? "-" : String.join(", ", merged);
}
```

#### 결과
```
/user/detail.do
├── [행1] → DAO.selectUser() → 파라미터: userId ✅
└── [행2] → DAO.selectDept() → 파라미터: userId, deptId ✅
```

#### 배운 점
- 정적 분석의 한계: 분기 조건 파라미터, 죽은 코드 자동 판별 불가
- 실용적 범위 설정의 중요성 - 완벽보다 실용적인 해결책
- 정규식으로 SQL에서 파라미터 추출하는 패턴 학습
- 사용자 관점에서 "어떤 정보가 필요한가" 고민 필요

### Issue #009: Swing GUI에서 한글 깨짐

**발생일**: 2025-12-24
**상태**: ✅ 해결

#### 문제 상황
Swing GUI에서 분석 결과 표시 시 한글이 네모(□)로 깨져서 표시됨
```
분석 요약: 11개 엔드포인트... → □□ □□: 11□ □□□□□...
```

- JTree에서 한글 텍스트가 모두 깨짐
- 복사해서 메모장에 붙여넣으면 정상 출력

#### 원인 분석
- `ResultPanel.java`에서 `Consolas` 폰트 사용
- `Consolas`는 영문 고정폭 폰트로 **한글 글리프가 없음**
- Java Swing은 폰트에 없는 문자를 □(tofu)로 표시
- 클립보드 복사는 문자 데이터만 복사하므로 폰트와 무관하게 정상

```java
// 문제 코드
resultTree.setFont(new Font("Consolas", Font.PLAIN, 13));
```

#### 해결 방법

**한글 지원 폰트로 변경**:
```java
// 수정 후
resultTree.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
```

**대안 폰트 옵션**:
| 폰트명 | 특징 |
|--------|------|
| Malgun Gothic (맑은 고딕) | Windows 기본, 한글 지원 |
| D2Coding | 개발용, 한글 고정폭 |
| NanumGothicCoding | 한글 고정폭 |
| Dialog | Java 기본, 다국어 지원 |

#### 배운 점
- Swing 폰트 선택 시 다국어(한글) 지원 여부 확인 필요
- 영문 전용 폰트(Consolas, Monaco, Menlo 등)는 한글 표시 불가
- 폰트 fallback은 OS 설정에 따라 다르게 동작
- 국제화(i18n) 고려 시 시스템 기본 폰트나 다국어 폰트 사용 권장

---

### Issue #010: GUI 텍스트 드래그 선택 불가

**발생일**: 2025-12-24
**상태**: ✅ 해결

#### 문제 상황
- GUI 결과 패널에서 텍스트 드래그 선택이 불가능
- 사용자가 결과 일부를 복사하려면 우클릭 메뉴나 전체 복사만 가능
- 일반 텍스트 에디터처럼 자유로운 드래그 선택 요청

#### 원인 분석
- 기존 구현: `JTree` + `DefaultTreeCellRenderer`
- `JTree`는 노드 단위 선택만 지원, 텍스트 부분 선택 불가
- 트리 구조 시각화에는 좋지만 텍스트 복사 UX가 불편

**시도한 방법들**:
1. `JTextPane` + `StyledDocument` → 드래그 여전히 안 됨
2. `DefaultCaret.setSelectionVisible(true)` → 효과 없음
3. `setDragEnabled(true)` → 효과 없음

#### 최종 해결

**`JEditorPane` + HTML 방식으로 완전히 변경**:
```java
// ResultPanel.java
private JEditorPane resultPane;

private void initializePane() {
    resultPane = new JEditorPane();
    resultPane.setContentType("text/html");  // HTML 렌더링
    resultPane.setEditable(false);
    // ...
}

public void displayResult(FlowResult result, String style) {
    StringBuilder html = new StringBuilder();
    html.append("<html><body><pre>");
    // HTML 태그로 색상 적용
    html.append("<span style='color:#009600'>[Controller] ...</span>");
    // ...
    resultPane.setText(html.toString());
}
```

**장점**:
- 텍스트 드래그 선택 완벽 지원
- HTML 스타일로 색상 유지 (Controller: 녹색, Service: 파랑 등)
- Ctrl+C 복사 기본 지원

#### 배운 점
- `JTree`는 구조 탐색용, 텍스트 선택 UX에는 부적합
- `JEditorPane` + HTML이 색상 + 텍스트 선택 조합에 최적
- Swing 컴포넌트 선택 시 사용 목적(탐색 vs 복사)을 먼저 고려

---

### Issue #011: GUI 창 닫아도 프로세스 종료 안 됨

**발생일**: 2025-12-24
**상태**: ✅ 해결

#### 문제 상황
- GUI 창을 X 버튼으로 닫아도 Java 프로세스가 계속 남아있음
- 작업 관리자에서 확인하면 java.exe 프로세스 존재
- 여러 번 실행하면 프로세스가 누적됨

#### 원인 분석
- `setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)` 설정되어 있음
- 하지만 `SwingWorker` 백그라운드 스레드가 실행 중이면 JVM이 종료되지 않을 수 있음
- `EXIT_ON_CLOSE`는 모든 non-daemon 스레드가 종료되어야 JVM 종료

#### 최종 해결

**`WindowListener`로 명시적 `System.exit()` 호출**:
```java
// MainFrame.java - initializeFrame()
addWindowListener(new java.awt.event.WindowAdapter() {
    @Override
    public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);  // 강제 종료
    }
});
```

- 창이 닫힐 때 `System.exit(0)` 명시적 호출
- 백그라운드 스레드 상태와 관계없이 즉시 종료

#### 배운 점
- `EXIT_ON_CLOSE`만으로는 모든 상황에서 프로세스 종료가 보장되지 않음
- `SwingWorker`나 다른 백그라운드 스레드가 있으면 명시적 종료 필요
- 사용자 입장에서 "창 닫기 = 프로세스 종료"가 직관적

---

### Issue #012: HTML 렌더링에서 박스 문자 정렬 불일치

**발생일**: 2025-12-24
**상태**: ✅ 해결

#### 문제 상황
GUI 결과 패널에서 CLI 스타일 박스 헤더가 정렬되지 않음
```
┌──────────────────────────────────────────────────┐
│     Code Flow Tracer - 호출 흐름 분석 결과       │  ← 예상
└──────────────────────────────────────────────────┘

실제 출력:
┌──────────────────────────────────────────────────┐
│     Code Flow Tracer - 호출 흐름 분석 결과
                                                   │  ← 오른쪽 │가 다음 줄로 밀림
└──────────────────────────────────────────────────┘
```

#### 원인 분석
**터미널 vs HTML 렌더링 차이:**

| 환경 | 문자 폭 처리 |
|------|-------------|
| 터미널 (CLI) | 모든 문자가 고정 폭 (한글=2칸, ASCII=1칸) 정확히 보장 |
| HTML (JEditorPane) | "monospace" 폰트여도 `│`, `─`, 한글, 공백의 실제 픽셀 폭이 미세하게 다름 |

- `center()` 함수에서 한글 폭(2칸)을 계산해도 HTML 렌더링에서는 정확히 반영 안 됨
- 박스 문자(`│`, `─`)가 일반 문자와 다른 폭으로 렌더링될 수 있음
- CSS `font-family: monospace`가 완전한 고정폭을 보장하지 않음

#### 시도한 해결책
1. **한글 폭 계산 추가** (`getDisplayWidth`, `isWideChar`) - 효과 없음
   - 계산은 맞지만 HTML 렌더링이 이를 따르지 않음

2. **단순 형태로 변경** (측면 `│` 제거, `═` 라인만 사용)
   - 동작하지만 박스 느낌이 사라짐

3. **HTML `<table>` 사용** ✅ 최종 해결
   - CSS border로 박스 생성
   - 브라우저/HTML 렌더러가 테이블 정렬을 보장

#### 최종 해결
HTML `<table>` 태그로 헤더 박스 구현:

```java
// ResultPanel.java - appendHeader()
private void appendHeader(StringBuilder html) {
    html.append("</pre>");  // pre 태그 임시 종료
    html.append("<table style='border-collapse: collapse; color: #4EC9B0; ...'>");
    html.append("<tr><td style='border: 1px solid #4EC9B0; padding: 8px 40px;'>");
    html.append("Code Flow Tracer - 호출 흐름 분석 결과");
    html.append("</td></tr>");
    html.append("</table>");
    html.append("<pre>");  // 다시 pre 시작
}
```

#### 배운 점
- 터미널 고정폭과 HTML monospace 폰트는 동작 방식이 다름
- 박스 문자 정렬이 필요하면 HTML에서는 `<table>` 또는 CSS Grid 사용이 확실함
- CLI 출력을 그대로 GUI로 옮기는 것은 한계가 있음 → 각 환경에 맞는 방식 선택 필요

### Issue #013: JSplitPane 내부 컴포넌트 가시성 제어 문제

**발생일**: 2025-12-25
**상태**: ✅ 해결

#### 문제 상황
- 분석 완료 후 좌측 URL 목록 패널이 나타나지 않음
- `setVisible(false)` / `setVisible(true)` 호출해도 효과 없음

#### 원인 분석
- `JSplitPane` 내부의 컴포넌트에서 `setVisible(false)`를 호출하면:
  - 컴포넌트가 보이지 않게 되지만 **공간은 그대로 차지**
  - 또는 `JSplitPane`이 레이아웃을 재조정하지 않음
- `JSplitPane`은 visibility가 아닌 **divider 위치**로 패널 크기를 제어하도록 설계됨

#### 시도한 해결책
1. `setVisible(false)` → 효과 없음
2. `setSize(0, height)` → 부분적 효과
3. `setDividerLocation(0)` → ✅ 정상 동작

#### 최종 해결
```java
// 숨기기 (분석 전)
mainSplitPane.setDividerLocation(0);

// 표시하기 (분석 후)
mainSplitPane.setDividerLocation(ENDPOINT_PANEL_WIDTH);  // 예: 250
```

- `setDividerLocation(0)`: 좌측 패널 폭이 0이 되어 사실상 숨김
- `setDividerLocation(width)`: 좌측 패널이 지정 폭으로 표시됨

#### 배운 점
- `JSplitPane`은 visibility 대신 divider 위치로 패널 표시/숨김 제어
- Swing 레이아웃 매니저는 컴포넌트별로 동작 방식이 다름
- 컴포넌트 문서에서 권장하는 방식을 확인하는 것이 중요

---

### Issue #014: 분석 요약 레이아웃 정렬 문제

**발생일**: 2025-12-25
**상태**: ✅ 해결

#### 문제 상황
GUI 분석 요약 섹션에서:
1. 라벨과 개수 값 사이 간격이 너무 넓음 (GridLayout 사용 시)
2. 가운데 정렬하면 타이틀/구분선과 정렬 불일치
3. 오른쪽 빈 공간이 어색함

```
[ 분석 요약 ]
  클래스:                                         4개    ← 간격 너무 넓음
  Controller:                                     1개
```

#### 원인 분석
- 다른 섹션(프로젝트 경로, 옵션)은 가로 전체 폭 사용
- 분석 요약만 좁은 내용 → 오른쪽 여백 발생
- 라벨-값 사이를 어떻게 채울 것인가?

#### 시도한 해결책

1. **GridLayout** → 간격이 너무 넓어짐 ❌
2. **가운데 정렬** → 타이틀/구분선과 불일치 ❌
3. **Leader Dots (점선 리더)** → ✅ 채택

#### 최종 해결
커스텀 `JPanel`로 점선 리더 구현:

```java
private JPanel createSummaryRow(JLabel label, JLabel valueLabel) {
    JPanel row = new JPanel(new BorderLayout(4, 0));
    row.add(label, BorderLayout.WEST);

    // 점선 리더 (가운데 채우기)
    JPanel dotsPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(150, 150, 150));  // 다크 테마에 맞는 밝기
            int y = getHeight() / 2;
            for (int x = 4; x < getWidth() - 4; x += 6) {
                g.fillOval(x, y, 2, 2);  // 2px 원형 점
            }
        }
    };
    dotsPanel.setOpaque(false);
    row.add(dotsPanel, BorderLayout.CENTER);

    row.add(valueLabel, BorderLayout.EAST);
    return row;
}
```

**결과:**
```
[ 분석 요약 ]
  클래스: .......................... 4개
  Controller: ...................... 1개
```

#### 배운 점
- `paintComponent()` 오버라이드로 간단한 커스텀 UI 요소 구현 가능
- Leader dots는 Word/Excel 목차에서 익숙한 패턴
- 다크 테마에서는 점선 색상도 배경과 대비되게 조절 필요 (150,150,150 사용)

---

## 미해결/진행중 문제

(현재 없음)

---

## 해결된 문제 (Session 15)

### Issue #015: jpackage 빌드 시 WiX Toolset 필요

**발생일**: 2025-12-25
**상태**: ✅ 해결

#### 문제 상황
jpackage로 Windows 설치 파일(.exe) 생성 시도 시 오류 발생:

```
Can not find WiX tools (light.exe, candle.exe)
Download WiX 3.0 or later from https://wixtoolset.org
Error: Invalid or unsupported type: [exe]
```

#### 원인 분석
- jpackage는 Windows에서 `.exe`, `.msi` 설치 파일 생성 시 **WiX Toolset** 필요
- WiX (Windows Installer XML): Microsoft의 오픈소스 설치 패키지 도구
- JDK에 WiX가 포함되어 있지 않아 별도 설치 필요

#### 해결 방법

**WiX Toolset 설치**:
1. https://wixtoolset.org/releases/ 접속
2. WiX 3.x 또는 WiX 4.x 다운로드 및 설치
3. 시스템 PATH에 WiX bin 폴더 추가 (설치 시 자동 추가됨)
4. `gradlew jpackage` 재실행

**확인 방법**:
```bash
# WiX 설치 확인
where candle.exe
where light.exe
```

#### 대안 (WiX 없이 진행)
`app-image` 타입으로 포터블 버전 생성 가능:

```groovy
// build.gradle에서 --type 'exe' 대신
'--type', 'app-image'
```

결과: 설치 파일 대신 실행 가능한 폴더 생성

#### 배운 점
- jpackage는 OS별로 추가 도구가 필요할 수 있음
- Windows: WiX Toolset (exe, msi)
- macOS: Xcode command line tools (pkg, dmg)
- 폐쇄망 배포 시 빌드 환경 사전 준비 필요

---

### Issue #016: WiX 6.0과 JDK 21 호환성 문제

**발생일**: 2025-12-25
**상태**: ✅ 해결

#### 문제 상황
WiX Toolset 6.0 설치 후에도 jpackage에서 WiX 도구를 찾지 못함:

```
Can not find WiX tools (light.exe, candle.exe)
```

#### 원인 분석
- **WiX 버전 아키텍처 변경**: WiX 4.0부터 도구 구조가 완전히 바뀜
  - WiX 3.x: `candle.exe` + `light.exe` (분리된 도구)
  - WiX 4/5/6: `wix.exe` (통합 도구)
- **JDK 호환성 매트릭스**:
  | JDK 버전 | WiX 3 | WiX 4/5/6 |
  |----------|:-----:|:---------:|
  | JDK 23 이하 | ✅ | ❌ |
  | JDK 24+ | ✅ | ✅ |
- JDK 24부터 WiX 4+ 지원 추가 (JDK-8319457)

#### 시도한 해결책
1. WiX 6.0 설치 → 실패 (JDK 21에서 미지원)
2. PATH 확인 → WiX 6.0에는 candle.exe/light.exe 없음

#### 최종 해결
**WiX 3.14 추가 설치** (WiX 6.0과 공존 가능):

```powershell
winget install WiXToolset.WiXToolset
```

설치 경로: `C:\Program Files (x86)\WiX Toolset v3.14\bin\`

#### 배운 점
- 도구 버전 업그레이드가 항상 좋은 것은 아님 (호환성 확인 필요)
- JDK LTS (17, 21)를 사용할 경우 WiX 3.x 사용 권장
- 대부분의 개발자/튜토리얼이 WiX 3.x 기준 (업계 표준)

---

### Issue #017: jpackage description 한글 인코딩 오류

**발생일**: 2025-12-25
**상태**: ✅ 해결 (우회)

#### 문제 상황
WiX 3.14 설치 후에도 jpackage 빌드 실패 (exit code 311):

```
light.exe ... exited with 311 code
```

#### 원인 분석
- `--description` 파라미터에 한글 포함:
  ```
  --description "Code Flow Tracer - Java 호출 흐름 분석 도구"
  ```
- 인코딩 변환 과정에서 깨짐:
  ```
  Gradle (UTF-8) → PowerShell (CP949) → jpackage → WiX (windows-1252)
  ```
- WiX 기본 로컬라이제이션 파일이 `windows-1252` 인코딩 사용
- 한글(비 ASCII 문자)은 이 인코딩에서 지원되지 않음

#### 최종 해결 (우회)
description을 **영문으로 변경**:

```groovy
// build.gradle
appDescription = 'Code Flow Tracer - Java Call Flow Analyzer'
```

#### 한글 사용이 필요한 경우 (대안)

**방법 1: 커스텀 로컬라이제이션 파일**

1. `installer-resources/` 폴더 생성
2. WiX 로컬라이제이션 파일 작성 (ko-KR.wxl):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<WixLocalization Culture="ko-KR" Codepage="949"
                 xmlns="http://wixtoolset.org/schemas/v4/wxl">
    <String Id="ApplicationDescription">Java 호출 흐름 분석 도구</String>
</WixLocalization>
```
3. jpackage 옵션 추가:
```bash
jpackage ... --resource-dir ./installer-resources
```

**방법 2: JDK 24+ 업그레이드**
- JDK-8290519에서 codepage 지정 기능 개선 논의 중
- 향후 버전에서 더 쉬워질 가능성

**참고 링크**:
- [JDK-8290519: jpackage codepage 지정](https://bugs.openjdk.org/browse/JDK-8290519)
- [JDK-8223325: WiX sources 개선](https://bugs.openjdk.org/browse/JDK-8223325)

#### 한글 사용 범위 정리

| 항목 | 한글 사용 | 비고 |
|------|:--------:|------|
| 앱 내부 (GUI, 메시지) | ✅ | 문제없음 |
| 설치 파일 description | ⚠️ | 커스텀 설정 필요 |
| 앱 이름 | ⚠️ | 영문 권장 |
| 설치 경로 | ⚠️ | OS별 차이 |

#### 배운 점
- Windows 환경에서 인코딩은 여러 레이어에서 문제 발생 가능
- jpackage → WiX 체인에서 기본 인코딩은 windows-1252 (한글 미지원)
- 설치 파일 메타데이터와 앱 내부 콘텐츠는 별개로 처리됨
- 빠른 배포가 필요하면 영문 사용, 한글 필수면 커스텀 설정 추가

---

### Issue #018: jpackage 생성 exe 실행 시 아무 반응 없음

**발생일**: 2025-12-25
**상태**: ✅ 해결

#### 문제 상황
jpackage로 생성한 CFT-1.0.0.exe를 설치 후 실행 시 아무 반응 없음:
- 바탕화면 바로가기 클릭 → 반응 없음
- 설치 폴더의 CFT.exe 클릭 → 반응 없음
- 프로세스가 순간적으로 시작되었다가 즉시 종료

#### 원인 분석

**Main.java 코드 확인**:
```java
@Command(name = "cft", ...)
public class Main implements Callable<Integer> {
    @Option(names = {"--gui", "-g"}, description = "GUI 모드로 실행")
    private boolean guiMode;

    @Parameters(index = "0", description = "분석할 프로젝트 경로", arity = "0..1")
    private String projectPath;

    @Override
    public Integer call() {
        if (guiMode) {
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
            return 0;
        }
        // CLI 모드: projectPath 필수
        if (projectPath == null) {
            spec.commandLine().usage(System.out);
            return 1;  // 에러 종료
        }
        // ...
    }
}
```

- jpackage 기본 실행 시 **인자 없이** 실행됨
- `guiMode = false` (기본값)
- `projectPath = null` (인자 없음)
- CLI 모드로 진입 → 경로 없어서 즉시 종료

**CFT.cfg 파일 확인**:
```ini
[Application]
app.classpath=$APPDIR\code-flow-tracer.jar
app.mainclass=com.codeflow.Main

[JavaOptions]
java-options=-Djpackage.app-version=1.0.0
java-options=-Dfile.encoding=UTF-8
```
→ `--gui` 인자가 설정되지 않음

#### 최종 해결
build.gradle의 jpackage 태스크에 `--arguments` 옵션 추가:

```groovy
task jpackage(type: Exec, dependsOn: shadowJar) {
    commandLine jpackagePath,
        // ... 기존 옵션들 ...
        '--java-options', '-Dfile.encoding=UTF-8',
        '--arguments', '--gui'  // ← 추가
}
```

**효과**:
- CFT.cfg에 `app.mainjar.argument.1=--gui` 자동 추가
- exe 실행 시 GUI 모드로 바로 시작

#### 대안 설계 고려
향후 개선 시 Main.java 자체를 수정하는 방법도 있음:
```java
// 인자 없이 실행하면 기본적으로 GUI 모드
if (projectPath == null && !guiMode) {
    guiMode = true;  // 기본값을 GUI로
}
```
→ 현재는 CLI 도구로서의 일관성을 위해 유지

#### 배운 점
- jpackage로 GUI 앱 배포 시 기본 실행 인자 설정 필수
- CLI/GUI 겸용 앱은 기본 동작 모드를 명확히 정의해야 함
- `--arguments` 옵션으로 런처 기본 인자 설정 가능
- CFT.cfg 파일을 확인하면 실제 전달되는 인자 확인 가능

---

## 자주 발생하는 문제

### Gradle 빌드 관련

#### 의존성 다운로드 실패
```bash
# Gradle 캐시 삭제 후 재시도
./gradlew clean build --refresh-dependencies
```

#### Gradle Wrapper 없음
```bash
# IntelliJ에서 프로젝트 열면 자동 생성
# 또는 gradle wrapper 명령어 실행
```

### JavaParser 관련

#### 파싱 실패
- 원인: 문법 오류가 있는 Java 파일
- 해결: try-catch로 감싸고 로그 출력, 계속 진행

```java
try {
    ParsedClass parsed = parser.parseFile(path);
} catch (Exception e) {
    System.err.println("파싱 실패: " + path + " - " + e.getMessage());
    // 계속 진행
}
```

### 인코딩 관련

#### 한글 깨짐
```bash
# JVM 옵션으로 UTF-8 지정
java -Dfile.encoding=UTF-8 -jar code-flow-tracer.jar
```

#### Gradle 빌드 시 인코딩
```groovy
// build.gradle에 추가
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}
```

---

## 참고 자료

- [JavaParser 공식 문서](https://javaparser.org/)
- [Gradle 문제 해결](https://docs.gradle.org/current/userguide/troubleshooting.html)
- [Stack Overflow - JavaParser 태그](https://stackoverflow.com/questions/tagged/javaparser)
