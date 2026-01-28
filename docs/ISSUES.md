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

### Issue #023: URL 필터 적용 시 분석 요약 통계 미반영

**발생일**: 2025-12-31
**상태**: 🟢 해결됨

#### 문제 상황
URL 필터를 적용해도 분석 요약의 Controller/Service/DAO 개수가 변하지 않음
- 예: `/user/*` 필터 적용 시 엔드포인트는 5개로 줄어들지만, Controller는 여전히 2개로 표시
- 기대: 필터된 결과에 포함된 클래스만 카운트

#### 원인 분석
- `result.getControllerCount()` 등 기존 메서드는 **전체 파싱된 클래스** 기준
- `FlowResult`가 가진 `controllers`, `services`, `daos` 필드는 파싱 시점에 채워짐
- URL 필터는 `flows`만 필터링하고, 위 필드들은 그대로 유지

```java
// 문제 코드 (MainFrame.java)
lblControllerCount.setText(result.getControllerCount() + "개");  // 전체 기준
```

#### 최종 해결
**Flow 기반 통계 메서드 추가** (`FlowResult.java`):

```java
// 실제 호출 흐름에 포함된 클래스만 카운트
public int getFlowBasedControllerCount() {
    return countUniqueClassesByType(ClassType.CONTROLLER);
}

private int countUniqueClassesByType(ClassType type) {
    Set<String> uniqueClasses = new HashSet<>();
    for (FlowNode flow : flows) {
        collectClassesByType(flow, type, uniqueClasses);  // 재귀 탐색
    }
    return uniqueClasses.size();
}
```

**GUI/Console 출력 수정**:
- `ConsoleOutput.java:131-138`: `getFlowBasedXxxCount()` 사용
- `MainFrame.java:807-812`: `getFlowBasedXxxCount()` 사용

#### 결과
| 항목 | 필터 없음 | `/user/*` 필터 |
|------|----------|---------------|
| 엔드포인트 | 11개 | 5개 |
| Controller | 2개 | 1개 ✅ |
| Service | 2개 | 1개 ✅ |
| DAO | 7개 | 2개 ✅ |

#### 배운 점
1. **통계 기준 명확화**: 전체 파싱 vs 필터된 결과 구분 필요
2. **재귀 탐색**: 트리 구조에서 특정 타입 노드 수집 시 재귀 활용
3. **Set으로 중복 제거**: 같은 클래스가 여러 흐름에서 호출될 수 있음

---

### Issue #024: 설치 삭제 시 세션 데이터 유지됨

**발생일**: 2025-12-31
**상태**: 🟢 해결됨

#### 문제 상황
프로그램 삭제 후 재설치해도 이전 세션 기록(분석 결과, 설정)이 그대로 남아있음
- 세션 파일 위치: `~/.code-flow-tracer/session.json`
- 설치 삭제 후에도 파일이 삭제되지 않음

#### 시도한 해결책 (실패)

1. **`util:RemoveFolderEx` 사용**
   ```xml
   <util:RemoveFolderEx Id="RemoveSessionFolderEx" On="uninstall" Property="CFTCLEANUPDIR" />
   ```
   - 결과: WiX 컴파일 오류 (exit code 10, 62)
   - 원인: `Property` 설정, `RegistrySearch` 등 추가 설정 필요

2. **와일드카드 `RemoveFile` + `RemoveFolder`**
   ```xml
   <Directory Id="ProfileFolder">
     <Directory Id="CFTSessionDir" Name=".code-flow-tracer">
       <Component Id="SessionCleanup" ...>
         <RemoveFile Id="RemoveAllSessionFiles" Name="*" On="uninstall" />
         <RemoveFolder Id="RemoveSessionFolder" On="uninstall" />
       </Component>
     </Directory>
   </Directory>
   ```
   - 결과: **동작 안 함!**
   - 레지스트리에 `SessionCleanup=1`은 등록되었지만 파일 삭제 안 됨

#### 원인 분석 (핵심!)

**WiX Directory 구조 문제**:
- `ProfileFolder`가 `TARGETDIR` **내부에 중첩**되어 있었음
- WiX는 `ProfileFolder`를 `%USERPROFILE%`이 아닌 **설치 경로의 하위 디렉토리**로 해석

```xml
<!-- 문제의 구조 -->
<Directory Id="TARGETDIR" Name="SourceDir">
  ...
  <Directory Id="ProfileFolder">  <!-- ← TARGETDIR 안에 있음! -->
    <Directory Id="CFTSessionDir" Name=".code-flow-tracer">
```

**결과**: `C:\Program Files\CFT\.code-flow-tracer`를 삭제하려고 시도 (존재하지 않음)
**실제 위치**: `C:\Users\Winbit\.code-flow-tracer`

#### 최종 해결: CustomAction으로 직접 삭제

WiX Directory 구조 문제를 우회하여 `cmd.exe /c rmdir`로 직접 삭제:

```xml
<!-- installer-resources/main.wxs -->

<!-- Session folder cleanup via cmd.exe -->
<CustomAction Id="RemoveSessionFolder"
              Directory="TARGETDIR"
              ExeCommand="cmd.exe /c &quot;if exist %USERPROFILE%\.code-flow-tracer rmdir /s /q %USERPROFILE%\.code-flow-tracer&quot;"
              Execute="deferred"
              Return="ignore" />

<!-- InstallExecuteSequence에 추가 -->
<InstallExecuteSequence>
  ...
  <Custom Action="RemoveSessionFolder" After="RemoveFiles">REMOVE="ALL"</Custom>
</InstallExecuteSequence>
```

**동작 방식**:
1. `REMOVE="ALL"` 조건: 언인스톨 시에만 실행
2. `After="RemoveFiles"`: 기본 파일 삭제 후 실행
3. `%USERPROFILE%` 환경변수로 정확한 경로 지정
4. `rmdir /s /q`: 폴더와 모든 내용 강제 삭제
5. `Return="ignore"`: 폴더가 없어도 에러 무시

#### 배운 점
1. **WiX Directory 중첩 주의**: `ProfileFolder` 같은 특수 디렉토리는 `TARGETDIR` 외부에서 독립적으로 참조해야 함
2. **레지스트리 등록 ≠ 동작 확인**: 레지스트리에 값이 등록되어도 실제 동작은 별도 검증 필요
3. **CustomAction이 더 확실**: 복잡한 WiX 설정보다 `cmd.exe` 직접 실행이 더 간단하고 확실
4. **환경변수 활용**: `%USERPROFILE%`로 사용자별 경로 문제 해결

---

## 미해결/진행중 문제

### Issue #025: GUI CRUD 필터 실시간 적용 불가

**발생일**: 2026-01-12
**상태**: 🟢 해결됨

#### 문제 상황
- GUI에서 CRUD 타입 체크박스(SELECT, INSERT, UPDATE, DELETE) 선택 후 실시간 필터링이 안 됨
- 현재는 체크박스 변경 → "분석 시작" 버튼 클릭 → 전체 재분석 필요
- 반면, 좌측 엔드포인트 검색창은 입력 즉시 실시간 필터링 됨

```
현재 동작:
1. 분석 실행 → 결과 표시
2. CRUD 체크박스 변경
3. 다시 "분석 시작" 클릭 필요 ← 문제!
4. 전체 재분석 (느림)

원하는 동작:
1. 분석 실행 → 결과 표시
2. CRUD 체크박스 변경 → 즉시 필터링 ← 목표
```

#### 원인 분석

**현재 구현 방식 (`MainFrame.java`)**:
```java
// startAnalysis() 내부 - 분석 시점에 필터 적용
if (sqlTypeFilter != null && !sqlTypeFilter.isEmpty()) {
    result = analyzer.filterBySqlType(result, sqlTypeFilter);  // 분석 단계에서 필터링
}
currentResult = result;  // 필터링된 결과만 저장
```

**문제점**:
- `currentResult`에 **필터링된** 결과만 저장됨
- 원본 데이터가 없어서 필터 변경 시 재계산 불가능
- 체크박스 변경 시 전체 재분석 필요 (비효율적)

#### 대안 비교

| 방식 | 장점 | 단점 | 선택 |
|------|------|------|------|
| **A. 원본+필터링 이중 저장** | 빠른 필터 전환 | 메모리 2배, 동기화 복잡 | ❌ |
| **B. 재분석 (현재 방식)** | 구현 간단 | 느림, UX 불편 | ❌ |
| **C. 원본 저장 + UI 레이어 필터링** | 빠름, 메모리 효율적, 확장 가능 | 필터 로직 UI에 위치 | ✅ |

**선택: C. 원본 저장 + UI 레이어 필터링**

이유:
1. 엔드포인트 검색창과 동일한 패턴 (일관성)
2. 분석은 1회만, 필터는 즉시 적용
3. 향후 테이블 필터 추가 시에도 동일 패턴 재사용 가능

#### 구현 계획

1. **원본 결과 별도 저장**
   ```java
   private FlowResult originalResult;  // 필터 없는 원본
   private FlowResult currentResult;   // 필터 적용된 현재 표시용
   ```

2. **체크박스에 실시간 리스너 추가**
   ```java
   cbSelect.addActionListener(e -> applyFiltersAndRefresh());
   cbInsert.addActionListener(e -> applyFiltersAndRefresh());
   // ...
   ```

3. **필터 적용 메서드 추가**
   ```java
   private void applyFiltersAndRefresh() {
       if (originalResult == null) return;

       FlowResult filtered = originalResult;
       if (!isAllSqlTypesSelected()) {
           FlowAnalyzer analyzer = new FlowAnalyzer();
           filtered = analyzer.filterBySqlType(originalResult, getSelectedSqlTypes());
       }
       currentResult = filtered;

       updateSummaryPanel(filtered);
       updateEndpointList(filtered);
       resultPanel.displayResult(filtered, getSelectedStyle());
   }
   ```

#### 최종 해결

**변경된 파일**: `MainFrame.java`

1. **원본 결과 필드 추가**
   ```java
   private FlowResult originalResult;  // 필터 없는 원본 결과
   private FlowResult currentResult;   // 현재 표시용 (필터 적용된)
   ```

2. **체크박스에 ActionListener 추가**
   ```java
   cbSelect.addActionListener(e -> applyFiltersAndRefresh());
   cbInsert.addActionListener(e -> applyFiltersAndRefresh());
   cbUpdate.addActionListener(e -> applyFiltersAndRefresh());
   cbDelete.addActionListener(e -> applyFiltersAndRefresh());
   ```

3. **실시간 필터 적용 메서드 추가**
   ```java
   private void applyFiltersAndRefresh() {
       if (originalResult == null) return;

       FlowResult filtered = originalResult;
       if (!isAllSqlTypesSelected()) {
           FlowAnalyzer analyzer = new FlowAnalyzer();
           filtered = analyzer.filterBySqlType(originalResult, getSelectedSqlTypes());
       }
       currentResult = filtered;

       updateSummaryPanel(filtered);
       updateEndpointList(filtered);
       resultPanel.displayResult(filtered, getSelectedStyle());
   }
   ```

4. **세션 저장/복원 시 원본 데이터 사용**
   - `saveSession()`: `originalResult` 저장
   - `restoreSession()`: `originalResult`로 복원 후 CRUD 필터 적용

5. **필터링 로직 버그 수정** (`FlowAnalyzer.java`)
   - 문제: Controller 노드가 자식 없어도 항상 포함됨
   - 원인: `filterFlowBySqlType()`에서 Controller 예외 처리
   ```java
   // 버그 코드
   if (!filtered.getChildren().isEmpty() || node.getClassType() == ClassType.CONTROLLER) {
       return filtered;  // Controller는 자식 없어도 반환
   }

   // 수정 코드
   if (!filtered.getChildren().isEmpty()) {
       return filtered;  // 모든 노드는 자식 있어야 반환
   }
   ```

#### 배운 점
- **데이터와 뷰 분리**: 원본 데이터를 보존해야 필터 전환이 가능
- **일관된 패턴**: 엔드포인트 검색과 동일한 실시간 필터링 패턴 적용
- **UI 반응성**: 재분석 없이 즉시 필터링 → UX 개선
- **재귀 필터링 주의**: 트리 구조 필터링 시 루트 노드 예외 처리는 버그 원인이 될 수 있음

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

### Issue #019: Gradle clean 시 빌드 디렉토리 파일 잠금

**발생일**: 2025-12-26
**상태**: ✅ 해결 (우회)

#### 문제 상황
`./gradlew clean shadowJar` 실행 시 빌드 디렉토리 삭제 실패:

```
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':clean'.
> java.io.IOException: Unable to delete directory 'C:\Devel\Code-Flow-Tracer\build'
    Failed to delete some children:
    - C:\Devel\Code-Flow-Tracer\build\installer\CFT-1.0.0.exe
    - C:\Devel\Code-Flow-Tracer\build\jpackage-temp\images\win-exe.image\CFT-1.0.0.exe
```

#### 원인 분석
- 이전 jpackage 실행으로 생성된 `.exe` 파일이 잠김
- 가능한 잠금 원인:
  - Windows 탐색기에서 해당 폴더 열어둠
  - 백신 프로그램이 exe 파일 스캔 중
  - 이전 jpackage 프로세스가 완전히 종료되지 않음
- Windows 파일 시스템 특성: 열려있는 파일은 삭제 불가

#### 시도한 해결책
1. `clean` 없이 `shadowJar`만 실행 → ✅ shadowJar 성공
2. jpackage 실행 시 기존 installer 폴더에 덮어쓰기 → ❌ AccessDeniedException
3. jpackage 출력 경로를 `build/release`로 변경 → ✅ 성공

#### 최종 해결
jpackage를 직접 호출하면서 출력 경로를 변경:

```powershell
# 기존 (실패)
--dest build/installer

# 변경 (성공)
--dest build/release
```

#### 영구적 해결 방법 (선택)
1. **PC 재시작**: 모든 파일 잠금 해제
2. **탐색기 종료**: 해당 폴더를 보고 있는 창 닫기
3. **프로세스 확인**: 작업 관리자에서 관련 프로세스 종료
4. **잠금 확인 도구**: Process Explorer, Handle 등으로 잠금 프로세스 찾기

#### 배운 점
- Windows에서 exe 파일은 탐색기, 백신 등에 의해 쉽게 잠김
- Gradle clean이 실패해도 incremental build는 가능
- 빌드 출력 경로를 변경하는 것도 유효한 우회 방법
- CI/CD 환경에서는 매번 클린 빌드 환경을 사용하므로 이 문제 없음

---

### Issue #020: 설정 이중 저장 (Registry + JSON)

**발생일**: 2025-12-31
**해결일**: 2025-12-31
**상태**: 🟢 해결됨

#### 문제 상황
설정이 두 곳에 중복 저장되고 있음:

| 저장소 | 저장 항목 | 도입 시점 |
|--------|----------|----------|
| Registry (Preferences API) | 최근 경로, URL 필터, 출력 스타일 | Session 13 |
| JSON 파일 | 분석 결과, URL 필터, 출력 스타일 | Session 18 |

- URL 필터, 출력 스타일이 **두 곳에 중복 저장**
- 두 저장소 간 동기화 문제 가능성

#### 원인 분석

**Session 13 (2025-12-24)**: GUI 설정 저장 기능 구현
- 저장 대상: 최근 경로, URL 필터, 출력 스타일 (단순 문자열)
- 선택: Java Preferences API (표준 API, 추가 의존성 없음)
- 당시 합리적 선택이었음

**Session 18 (2025-12-30)**: 세션 영속성 구현
- 저장 대상: FlowResult (복잡한 객체 트리 구조)
- 선택: Gson JSON 파일 (객체 직렬화 필수)
- **문제**: URL 필터, 출력 스타일도 SessionData에 포함하여 저장
- 기존 Preferences 저장 로직을 제거하지 않음 → 이중 저장 발생

**왜 당시 발견하지 못했나?**
- 세션 영속성 구현에 집중
- 기존 설정 저장 로직과의 연관성 검토 부족
- 동작에는 문제 없음 (둘 다 로드되어 마지막 값 사용)

#### 저장 방식 비교

| 항목 | Registry (Preferences) | JSON 파일 |
|------|----------------------|-----------|
| 플랫폼 | Windows: Registry, 기타: 파일 | 모든 OS 동일 |
| 복잡한 객체 | ❌ 문자열/숫자만 | ✅ 객체 직렬화 가능 |
| 백업/이동 | ❌ regedit 필요 | ✅ 파일 복사 |
| 디버깅 | ❌ 레지스트리 편집기 | ✅ 텍스트 에디터 |
| 삭제 시 정리 | WiX RemoveRegistryKey | WiX RemoveFile |
| 표준 API | ✅ Java 표준 | ❌ Gson 의존 |

#### 해결 방향

**권장: JSON 단일 저장으로 통합 (v1.2)**

```json
// ~/.code-flow-tracer/session.json
{
  "projectPath": "/path/to/project",
  "recentPaths": ["/path1", "/path2"],  // Registry에서 이동
  "urlFilter": "/api/*",
  "outputStyle": "normal",
  "analyzedAt": "2025-12-31T14:30:00",
  "flowResult": { ... }
}
```

**통합 시 장점**:
- 설정 관리 일원화
- 크로스 플랫폼 완전 지원
- 디버깅/백업 용이
- 설치 삭제 시 정리 간단
- **GUI 메뉴 단순화** (삭제 메뉴 통합)

**GUI 메뉴 현재 상태**:
현재 설정 메뉴에 삭제 버튼이 2개로 분리되어 있음:
- "설정 초기화" → Registry만 삭제 (`prefs.clear()`)
- "세션 삭제" → JSON 파일만 삭제 (`sessionManager.clearSession()`)

**GUI 메뉴 통합 계획** (v1.2):
JSON 단일 저장으로 통합 시, 삭제 메뉴도 하나로 통합:
- 기존: "설정 초기화" + "세션 삭제" (2개)
- 변경: "설정/세션 초기화" (1개) → JSON 파일 삭제

**마이그레이션 전략**:
1. 앱 시작 시 Registry 설정 존재 여부 확인
2. 있으면 JSON으로 마이그레이션 후 Registry 삭제
3. 없으면 JSON에서만 로드
4. WiX Registry 정리 로직은 1~2 버전간 유지 (이전 버전 사용자 대응)
5. GUI 삭제 메뉴 통합 (2개 → 1개)

#### 최종 해결 (2025-12-31)

**JSON 단일 저장으로 통합 완료**:

1. **SessionData.java**: `recentPaths` 필드 추가
2. **SessionManager.java**: `loadSettings()`, `saveSettings()` 메서드 추가
3. **MainFrame.java**:
   - `Preferences` API 완전 제거
   - 모든 설정을 `SessionManager`로 저장/로드
   - 삭제 메뉴 통합: "설정 초기화" + "세션 삭제" → "설정/세션 초기화"

**결과**:
- 설정 저장 위치: `~/.code-flow-tracer/session.json` (단일)
- Registry 의존성 제거 (크로스 플랫폼 완전 지원)
- GUI 메뉴 단순화 (삭제 버튼 1개)

#### 배운 점

1. **기능 추가 시 기존 구현과의 연관성 검토 필수**
   - 새 저장 방식 도입 시 기존 저장 로직 확인
   - 중복/충돌 가능성 사전 검토

2. **저장 방식 결정 시 향후 확장 고려**
   - 단순 값만 저장할 것 같아도 나중에 복잡한 객체가 추가될 수 있음
   - 처음부터 JSON/파일 방식이 확장성이 좋음

3. **기술 부채는 조기에 문서화**
   - 발견 즉시 기록하여 망각 방지
   - 리팩토링 계획 수립 용이

---

### Issue #021: GUI UX 일관성 문제

**발생일**: 2025-12-31
**상태**: 🟢 해결됨

#### 문제 상황

테스트 중 발견된 3가지 UX 문제:

**1. 분석 결과 영역 커서 여전히 표시됨**
- 증상: 초기 상태(분석 전)에서 결과 영역 클릭 시 커서(|) 표시
- 이전: 흰색 커서
- 현재: 검은색 커서 (배경색 수정 후)
- 원인: 초기 상태는 HTML 콘텐츠가 없어서 기본 배경색(흰색/밝은색) 사용

**2. 왼쪽 필터(엔드포인트 검색) 저장 타이밍 문제**
- 증상: 왼쪽 필터 입력 후 "분석 실행"을 눌러야만 저장됨
- 문제점:
  - 사용자가 필터만 입력하고 앱 종료 시 저장 안 됨
  - 오른쪽 URL 필터와 동작 방식 불일치
- 고민:
  - 왼쪽 필터는 "결과 내 검색"용 (실시간 필터링)
  - 오른쪽 URL 필터는 "분석 대상 필터"용 (분석 시 적용)
  - 성격이 다르므로 저장하지 않는 것도 방법

**3. 설정/세션 초기화 시 불완전한 초기화**
- 증상: 초기화 클릭 시
  - ✅ 프로젝트 경로: 삭제됨
  - ✅ URL 필터: 삭제됨
  - ❌ 분석 결과: 화면에 그대로 남음
- 문제점: 일관성 없음 - 완전 초기화 기대하지만 화면은 그대로

#### 조사 및 해결 방안

**1. 커서(|) 문제 - 조사 결과**

조사한 방법들:

| 방법 | 설명 | 장점 | 단점 |
|------|------|------|------|
| `setCaret(null)` | 커서 객체 제거 | 가장 간단 | 텍스트 선택 불가 |
| `setFocusable(false)` | 포커스 비활성화 | 간단 | Ctrl+휠 줌 기능 동작 안 함 |
| `setCaretColor(배경색)` | 커서 색상을 배경과 동일하게 | 간단 | 초기 상태에서 배경색 다르면 보임 |
| 초기 HTML 설정 | 빈 상태에도 다크 배경 HTML | 간단 | 근본 해결 아님 |
| **Custom Caret** | `DefaultCaret` 상속하여 `paint()` 오버라이드 | **완벽한 해결** | 클래스 추가 필요 |

**선택: Custom Caret (InvisibleCaret)**

[Oracle 공식 문서](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/text/DefaultCaret.html) 참고:
- Swing에서 **Caret**은 커서(|) 그리기 담당
- **Highlighter**는 선택 영역 표시 담당 (별도 컴포넌트)
- → Caret의 `paint()` 메서드만 비우면 **커서는 숨기고 텍스트 선택은 유지** 가능

```java
public class InvisibleCaret extends DefaultCaret {
    @Override
    public void paint(Graphics g) {
        // 아무것도 그리지 않음 - 커서 완전히 숨김
    }

    @Override
    protected synchronized void damage(Rectangle r) {
        // 그리지 않으므로 damage도 무시
    }
}
```

장점:
- ✅ 커서 완전히 숨김 (초기 상태, 클릭 후 모두)
- ✅ 텍스트 드래그 선택 가능
- ✅ Ctrl+휠 줌 기능 유지
- ✅ 배경색과 무관하게 동작

---

**2. 왼쪽 필터 저장 문제 - 결정**

| 필터 | 용도 | 성격 | 저장 여부 |
|------|------|------|----------|
| 오른쪽 URL 필터 | 분석 대상 선택 | **설정** | ✅ 저장 |
| 왼쪽 검색 필터 | 결과 내 검색 | **UI 상태** | ❌ 저장 안 함 |

**선택: 저장하지 않음**
- 왼쪽 필터는 Ctrl+F 같은 "찾기" 기능과 동일한 성격
- 앱 재시작 시 검색어가 남아있으면 오히려 혼란
- 결론: `endpointFilter` 관련 저장/복원 코드 제거

---

**3. 초기화 불완전 문제 - 결정**

**선택: 화면도 함께 초기화**
- `handleClearAll()`에서 `resultPanel.clear()` 호출 추가
- 사용자 기대: "초기화" = 완전히 처음 상태로 돌아감
- 일관성 있는 UX 제공

---

**4. 처음 실행과 초기화 후 화면 불일치 (추가 발견)**

테스트 중 발견한 추가 문제:
- **처음 실행**: 왼쪽 패널 숨김, 분석 요약 숨김 (2단 구조)
- **초기화 후**: 왼쪽 패널 보임, 분석 요약 보임 (3단 구조)
- 사용자 관점: 레이아웃이 달라서 혼란

**선택: 처음부터 3단 구조로 통일**
- 사용자 선호: 3단 구조가 더 직관적
- 데이터만 비어있고 레이아웃은 동일하게

---

#### 최종 해결

**1. InvisibleCaret 클래스 추가** (`ResultPanel.java`)

```java
/**
 * 투명 커서 (Caret)
 * 커서(|)를 완전히 숨기면서 텍스트 선택 기능은 유지합니다.
 */
private static class InvisibleCaret extends DefaultCaret {
    @Override
    public void paint(Graphics g) {
        // 아무것도 그리지 않음 - 커서 완전히 숨김
    }

    @Override
    protected synchronized void damage(Rectangle r) {
        // 그리지 않으므로 damage도 무시
    }
}

// 사용
resultPane.setCaret(new InvisibleCaret());
```

**2. 왼쪽 필터 저장 제거** (`MainFrame.java`)

```java
// saveSettings()에서 endpointFilter를 null로 전달
sessionManager.saveSettings(paths, urlFilter, outputStyle, null);

// loadSettings()에서 endpointFilter 복원 코드 제거
```

**3. 완전한 초기화** (`MainFrame.java:handleClearAll()`)

```java
if (sessionManager.clearSession()) {
    projectPathComboBox.removeAllItems();
    urlFilterField.setText("");
    endpointSearchField.setText("");       // 왼쪽 검색 필터
    rbNormal.setSelected(true);
    endpointListModel.clear();             // 왼쪽 엔드포인트 목록
    resultPanel.clear();                   // 분석 결과 화면
    currentResult = null;                  // 분석 결과 객체
    // 분석 요약도 초기화
    lblTotalClasses.setText("0개");
    lblControllerCount.setText("0개");
    lblServiceCount.setText("0개");
    lblDaoCount.setText("0개");
    lblEndpointCount.setText("0개");
    statusLabel.setText("설정 및 세션이 초기화되었습니다.");
}
```

**4. 초기 레이아웃 통일** (`MainFrame.java`)

```java
// 처음부터 왼쪽 패널 표시
mainSplitPane.setDividerLocation(ENDPOINT_PANEL_WIDTH);

// 처음부터 분석 요약 표시 (초기값 0개)
summaryPanel.setVisible(true);
```

#### 결과

| 항목 | 이전 | 이후 |
|------|------|------|
| 커서 표시 | 검은색 커서 보임 | 완전히 숨김 |
| 텍스트 선택 | 가능 | 가능 (유지) |
| 왼쪽 필터 저장 | 분석 시에만 저장 | 저장 안 함 |
| 초기화 범위 | 입력 필드만 | 전체 화면 |
| 초기 레이아웃 | 2단 구조 | 3단 구조 |
| 초기화 후 레이아웃 | 3단 구조 | 3단 구조 (동일) |

#### 배운 점

1. **Swing Caret vs Highlighter**: 커서와 텍스트 선택은 별도 컴포넌트
2. **UI 상태 vs 설정**: 일시적 검색 상태는 저장하지 않는 것이 자연스러움
3. **레이아웃 일관성**: 사용자는 같은 앱에서 레이아웃 변화를 혼란스러워함
4. **완전한 초기화**: 사용자 기대에 맞게 모든 상태를 리셋해야 함

---

### Issue #022: 미사용 코드 정리 (Dead Code Cleanup)

**발생일**: 2025-12-31
**상태**: 🟢 해결됨

#### 문제 상황

PR 머지 전 전체 소스 검토 중 발견된 미사용 코드:

**검토 방법**: 4개 에이전트 병렬 실행으로 전체 19개 소스 파일 분석
- parser 패키지 검토
- analyzer 패키지 검토
- output/session/ui 패키지 검토
- TDD 커버리지 분석

**발견된 문제**:

| 파일 | 미사용 코드 | 유형 |
|------|------------|------|
| MainFrame.java | `createColoredLabel()` | 미사용 private 메서드 |
| FlowNode.java | `addCallArgument()`, `getCallArgumentsAsString()` | 미사용 public 메서드 |
| FlowResult.java | `findFlowsByUrl()`, `findFlowsByClass()`, `containsClass()` | 미사용 public/private 메서드 |
| MethodCall.java | `getArgumentsAsString()` | 미사용 public 메서드 |
| ClassType.java | `description` 필드, `getDescription()` | 미사용 필드/메서드 |

#### 해결 과정

**1차 시도: 모든 미사용 코드 제거**

아래 메서드들도 함께 제거 시도:
- `ParameterInfo.isSpringInjected()`
- `SqlInfo.getSqlParametersAsString()`

**빌드 실패**:
```
ConsoleOutput.java:212: error: cannot find symbol
    .filter(p -> !p.isSpringInjected())

ExcelOutput.java:423: error: cannot find symbol
    String sqlParams = sqlInfo.getSqlParametersAsString();
```

**교훈**: grep 검색으로 "정의"만 찾으면 실제 사용처를 놓칠 수 있음.
람다/스트림 내부 호출은 별도 확인 필요.

**2차 시도: 실제 사용 확인 후 선별 제거**

사용 중인 메서드 복구:
- `ParameterInfo.isSpringInjected()` - ConsoleOutput에서 파라미터 필터링에 사용
- `SqlInfo.getSqlParametersAsString()` - ExcelOutput에서 SQL 파라미터 표시에 사용

#### 최종 결과

**제거된 코드** (6개 메서드, 1개 필드):
```java
// MainFrame.java
- private JLabel createColoredLabel(String text, Color color)

// FlowNode.java
- public void addCallArgument(String argument)
- public String getCallArgumentsAsString()

// FlowResult.java
- public List<FlowNode> findFlowsByUrl(String urlPattern)
- public List<FlowNode> findFlowsByClass(String className)
- private boolean containsClass(FlowNode node, String className)

// MethodCall.java
- public String getArgumentsAsString()

// ClassType.java
- private final String description 필드
- public String getDescription()
```

**유지된 코드** (실제 사용 중):
- `ParameterInfo.isSpringInjected()` - ConsoleOutput:212
- `SqlInfo.getSqlParametersAsString()` - ExcelOutput:423

#### 향후 작업 (별도 PR)

**중복 코드 리팩토링** 필요:
- `ResultPanel.center()` ↔ `ConsoleOutput.center()` 중복
- `ResultPanel.getDisplayWidth()` ↔ `ConsoleOutput.getDisplayWidth()` 중복
- → 공통 유틸 클래스(`StringUtils` 또는 `DisplayUtils`)로 분리 권장

#### 배운 점

1. **코드 검토 자동화**: 병렬 에이전트로 대규모 검토 효율화 가능
2. **정적 분석의 한계**: 람다/스트림 내부 호출은 grep으로 놓치기 쉬움
3. **빌드 테스트 필수**: 제거 전 반드시 빌드/테스트로 검증
4. **점진적 정리**: 한 번에 모두 제거하지 말고 단계별로 검증

---

### Issue #025: GUI CRUD 필터 실시간 적용 불가

**발생일**: 2026-01-12
**상태**: ✅ 해결

#### 문제 상황

GUI에서 CRUD 체크박스(SELECT/INSERT/UPDATE/DELETE)를 변경해도 즉시 반영되지 않음.
- 엔드포인트 검색 필터: 타이핑할 때마다 실시간 필터링 ✅
- CRUD 타입 필터: 체크박스 변경 후 "분석 시작" 버튼을 다시 눌러야 반영 ❌

**UX 불일치**: 같은 화면에서 필터 동작 방식이 다름

#### 원인 분석

```java
// MainFrame.java - startAnalysis() 내부
if (sqlTypeFilter != null && !sqlTypeFilter.isEmpty()) {
    result = analyzer.filterBySqlType(result, sqlTypeFilter);
}
currentResult = result;  // ← 필터링된 결과만 저장
```

**문제점**:
- 분석 시점에 CRUD 필터를 적용하여 `currentResult`에 저장
- 원본 데이터(`originalResult`)가 없어서 필터 변경 시 재분석 필요
- 프로젝트 규모가 크면 재분석에 수 초~수십 초 소요

#### 고민했던 대안

| 방식 | 장점 | 단점 | 선택 |
|------|------|------|------|
| 원본+필터 이중 저장 | 빠름 | 메모리 2배, 동기화 복잡 | ❌ |
| 재분석 (현재) | 구현 간단 | 느림, UX 불편 | ❌ |
| **원본 저장 + UI 필터링** | 빠름, 확장 가능 | 필터 로직이 UI에 위치 | ✅ |

#### 기술적 결정 이유

**Option C (원본 저장 + UI 필터링) 선택 근거**:
1. **일관성**: 엔드포인트 검색과 동일한 실시간 필터링 패턴
2. **반응성**: 분석 1회 + 필터 즉시 적용 → 체감 속도 향상
3. **확장성**: 테이블 필터 추가 시에도 동일 패턴 재사용 가능

#### 최종 해결

**1. 원본/현재 결과 분리**
```java
private FlowResult originalResult;  // 필터 없는 원본 결과
private FlowResult currentResult;   // 현재 표시용 (필터 적용된)
```

**2. CRUD 체크박스에 ActionListener 추가**
```java
cbSelect.addActionListener(e -> applyFiltersAndRefresh());
cbInsert.addActionListener(e -> applyFiltersAndRefresh());
cbUpdate.addActionListener(e -> applyFiltersAndRefresh());
cbDelete.addActionListener(e -> applyFiltersAndRefresh());
```

**3. 실시간 필터링 메서드**
```java
private void applyFiltersAndRefresh() {
    if (originalResult == null) return;

    FlowResult filtered = originalResult;
    if (!isAllSqlTypesSelected()) {
        List<String> sqlTypes = getSelectedSqlTypes();
        if (!sqlTypes.isEmpty()) {
            FlowAnalyzer analyzer = new FlowAnalyzer();
            filtered = analyzer.filterBySqlType(originalResult, sqlTypes);
        }
    }
    currentResult = filtered;

    // UI 갱신
    updateSummaryPanel(filtered);
    updateEndpointList(filtered);
    resultPanel.displayResult(filtered, getSelectedStyle());
}
```

**4. 세션 저장/복원 수정**
- `saveSession()`: `originalResult` 저장 (필터 없는 원본)
- `restoreSession()`: `originalResult` 복원 후 현재 CRUD 필터 적용

#### 발견된 버그 (추가 수정)

**CRUD 필터링 시 DELETE 해제해도 DELETE 항목이 표시됨**

**원인**:
```java
// FlowAnalyzer.filterFlowBySqlType() - 기존 코드
if (!filtered.getChildren().isEmpty() || node.getClassType() == ClassType.CONTROLLER) {
    return filtered;  // ← Controller는 항상 포함 (버그)
}
```

**수정**:
```java
// 모든 노드는 필터에 맞는 자식이 있어야만 포함
if (!filtered.getChildren().isEmpty()) {
    return filtered;
}
return null;
```

#### 배운 점

1. **데이터와 뷰 분리**: 원본 데이터 보존으로 필터 전환이 자유로워짐
2. **일관된 UX 패턴**: 같은 화면의 필터는 동일한 동작 방식이어야 함
3. **재귀 필터링 주의**: 트리 구조 필터링 시 루트 노드 예외 처리 버그 주의
4. **테스트 중요성**: 구현 후 실제 동작 테스트로 숨은 버그 발견

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

### Issue #026: 테이블 영향도 세션 저장/복원 미동작

**발생일**: 2026-01-12
**상태**: ✅ 해결

#### 문제 상황
- 테이블 영향도 탭에서 테이블 선택 후 앱 종료 → 재시작 시 선택 상태 복원 안 됨
- 호출흐름 탭에서 선택한 엔드포인트도 마찬가지

#### 원인 분석
- `saveSession()`이 분석 완료 후에만 호출됨
- 탭 전환, 테이블 선택, 앱 종료 시에는 저장되지 않음

#### 해결
`windowClosing` 이벤트에서 `saveSession()` 호출 추가:
```java
addWindowListener(new java.awt.event.WindowAdapter() {
    @Override
    public void windowClosing(java.awt.event.WindowEvent e) {
        saveSession();  // 종료 전 세션 저장
        System.exit(0);
    }
});
```

#### 배운 점
- 세션 저장은 분석 완료 시점뿐 아니라 앱 종료 시점에도 필요
- UI 상태 변경은 즉시 저장보다 종료 시 일괄 저장이 효율적

---

### Issue #027: 호출흐름 스크롤 복원 타이밍 문제

**발생일**: 2026-01-12
**상태**: ✅ 해결

#### 문제 상황
- 세션 복원 시 엔드포인트 선택은 되지만 상세화면이 해당 위치로 스크롤되지 않음
- 목록에서 엔드포인트가 선택되어 있지만 오른쪽 결과 패널은 최상단 표시

#### 원인 분석
- `scrollToEndpoint()`가 UI 렌더링 완료 전에 호출됨
- `resultPane.modelToView()`가 아직 계산되지 않은 상태에서 호출

#### 해결
`SwingUtilities.invokeLater()` 중첩으로 렌더링 완료 후 스크롤:
```java
// UI 업데이트 후 스크롤 (렌더링 완료 대기)
SwingUtilities.invokeLater(() -> {
    // ... 다른 UI 업데이트 ...

    // 스크롤은 한 번 더 invokeLater로 감싸서 렌더링 후 실행
    SwingUtilities.invokeLater(() -> {
        resultPanel.scrollToEndpoint(savedSelectedEndpoint);
    });
});
```

#### 배운 점
- Swing에서 UI 조작은 렌더링 완료 후 실행해야 정확히 동작
- `invokeLater` 중첩으로 렌더링 큐 이후 실행 보장 가능

---

### Issue #028: 테이블 "전체" 선택 시 상세화면 빈 화면

**발생일**: 2026-01-12
**상태**: ✅ 해결

#### 문제 상황
- 분석 완료 후 테이블 영향도 탭에서 "전체"가 선택되어 있지만 상세화면이 비어있음
- 다른 테이블을 선택하면 정상 표시됨

#### 원인 분석
메서드 호출 순서 문제:
```java
// 기존 (잘못된 순서)
updateTableList(currentResult);           // displayTableAccesses() 호출
tableImpactPanel.updateData(currentResult);  // tableIndex 설정
```
- `updateTableList()`에서 `displayTableAccesses(ALL_TABLES)` 호출 시
- `tableImpactPanel.tableIndex`가 아직 null이어서 아무것도 표시 안 됨

#### 해결
호출 순서 변경:
```java
// 수정 (올바른 순서)
tableImpactPanel.updateData(currentResult);  // tableIndex 먼저 설정
updateTableList(currentResult);              // 그 후 displayTableAccesses() 호출
```

3곳 모두 수정:
1. `done()` - 분석 완료 후
2. `restoreSession()` - 세션 복원 시
3. `applyFiltersAndRefresh()` - 필터 변경 시

#### 배운 점
- 데이터 설정 → UI 업데이트 순서가 중요
- 여러 곳에서 동일 로직 사용 시 모든 곳 일관되게 수정 필요

---

### Issue #029: SQL 필터 변경 시 테이블 선택 상태 초기화

**발생일**: 2026-01-12
**상태**: ✅ 해결

#### 문제 상황
- 테이블 영향도 탭에서 테이블을 선택하고 상세화면(쿼리 목록)을 보고 있는 중
- SQL 필터(SELECT/INSERT/UPDATE/DELETE)를 변경하면 테이블 목록이 "전체"로 리셋됨
- 쿼리 상세 보기 화면도 초기화되어 사용자가 다시 찾아가야 함

#### 원인 분석
`applyFiltersAndRefresh()` 메서드가 필터 변경 시:
1. 테이블 목록을 새로 생성하면서 기존 선택 상태를 저장하지 않음
2. 결과적으로 항상 첫 번째 항목("전체")이 선택됨
3. 쿼리 상세 보기 상태도 유지되지 않음

```java
// 기존 코드 - 상태 저장 없음
updateTableList(currentResult);  // 항상 "전체" 선택으로 리셋
```

#### 해결
필터 적용 전 현재 상태를 저장하고, 필터 적용 후 복원:

```java
// 수정 - 상태 저장 및 복원
// 1. 현재 상태 저장
final int currentTab = resultTabbedPane.getSelectedIndex();
final String savedTableSelection = tableList.getSelectedValue();
final boolean savedQueryDetailActive = tableImpactPanel.isQueryDetailViewActive();
final int savedQueryRowIndex = tableImpactPanel.getSelectedQueryRowIndex();

// 2. 필터 적용 및 데이터 갱신
tableImpactPanel.updateData(currentResult);
updateTableListWithoutSelection(currentResult);  // 선택 없이 목록만 갱신

// 3. 상태 복원
if (savedTableSelection != null) {
    tableList.setSelectedValue(savedTableSelection, true);
}
if (savedQueryDetailActive && savedQueryRowIndex >= 0) {
    tableImpactPanel.restoreQueryDetailView(savedQueryRowIndex);
}
```

주요 변경:
1. `updateTableListWithoutSelection()` 메서드 추가 - 테이블 목록 갱신 시 선택을 자동으로 하지 않음
2. 쿼리 상세 보기 상태 저장/복원을 위한 helper 메서드 활용
3. `SwingUtilities.invokeLater()`로 복원 타이밍 보장

#### 배운 점
- 필터 변경은 "데이터 갱신"이지만 사용자 관점에서는 "현재 위치 유지"가 자연스러움
- 상태 저장 → 갱신 → 상태 복원 패턴은 UI 작업에서 자주 사용되는 패턴
- 테이블 영향도 탭의 여러 뷰 상태(목록/상세/쿼리상세)를 모두 고려해야 함

---

### Issue #030: Git Stash 충돌 - 병렬 작업으로 인한 세션 번호 중복

**발생일**: 2026-01-12
**상태**: ✅ 해결

#### 문제 상황
다른 환경에서 작업 후 `git pull`을 시도했으나, 로컬에 stash된 변경사항과 원격의 변경사항이 **같은 파일(DEV_LOG.md)의 같은 위치**를 수정하여 충돌 발생.

```bash
$ git stash pop
Auto-merging docs/DEV_LOG.md
CONFLICT (content): Merge conflict in docs/DEV_LOG.md
```

**충돌 원인**: 두 곳에서 동시에 작업 진행
| 작업 위치 | 작업 내용 | Session 번호 |
|-----------|----------|-------------|
| 이전 세션 (Stash) | 기술7 GUI 블로그 작성 | Session 26 |
| 다른 환경 (Remote) | CRUD 구현, 실시간 필터, 테이블 영향도 | Session 26, 27, 28 |

**결과**: 서로 다른 작업이 같은 Session 26 번호를 사용

#### 충돌 지점 3곳

**1. Week 5 세션 범위 (Line 16)**
```markdown
<<<<<<< Updated upstream
| Week 5 (현재) | 01/03~ | CRUD 분석 기능, 블로그 | Session 22-28 |
=======
| Week 5 (현재) | 01/03~ | CRUD 분석 기능, 블로그 | Session 22-26 |
>>>>>>> Stashed changes
```

**2. 진행률 (Line 27)**
```markdown
<<<<<<< Updated upstream
Week 5: CRUD 분석       █████████████████░░░ 85%
=======
Week 5: CRUD 분석       ██████░░░░░░░░░░░░░░ 30%
>>>>>>> Stashed changes
```

**3. Session 25 "다음 할 일" 이후 (Line 347~)**
- Stash: Session 26으로 블로그 작성 기록 추가
- Remote: Session 26으로 CRUD 구현 기록 추가
- 두 작업이 같은 Session 번호를 사용하여 내용 충돌

#### 해결 과정

**Step 1: 충돌 마커 확인**
```bash
$ grep -n "<<<<<<\|======\|>>>>>>" docs/DEV_LOG.md
16:<<<<<<< Updated upstream
18:=======
20:>>>>>>> Stashed changes
...
```

**Step 2: 세션 번호 재정렬 전략 수립**
- 블로그 작업(01-11~12)이 CRUD 작업(01-12)보다 먼저 시작됨
- 시간순으로 정렬: 블로그 → CRUD 구현 → 실시간 필터 → 테이블 영향도

| 기존 | 변경 후 | 내용 |
|------|---------|------|
| Stash Session 26 | **Session 26** | 기술7 GUI 블로그 |
| Remote Session 26 | **Session 27** | CRUD 필터링 구현 |
| Remote Session 27 | **Session 28** | CRUD 필터 실시간 적용 |
| Remote Session 28 | **Session 29** | GUI 테이블 영향도 탭 |

**Step 3: 수동 병합 수행**
```bash
# 1. 헤더 수정: Session 22-29로 통합
| Week 5 (현재) | 01/03~ | CRUD 분석 기능, 블로그 | Session 22-29 |

# 2. 진행률: 최신값(85%) 유지
Week 5: CRUD 분석       █████████████████░░░ 85%

# 3. 세션 번호 재정렬
Session 26 → 블로그 (stash 내용 유지)
Session 26 → Session 27 (remote)
Session 27 → Session 28 (remote)
Session 28 → Session 29 (remote)
```

**Step 4: 충돌 해결 후 커밋**
```bash
$ git add docs/DEV_LOG.md
$ git commit -m "docs: Session 26 블로그 작성 기록 추가 및 세션 번호 재정렬"
$ git stash drop  # 사용한 stash 정리
$ git push
```

#### 최종 결과
```
Session 26 (01/11~12) → 기술7 GUI 블로그 작성 및 발행
Session 27 (01/12)    → CRUD 필터링 (#22) 및 테이블 중심 분석 (#23)
Session 28 (01/12)    → CRUD 필터 실시간 적용 (#25)
Session 29 (01/12)    → GUI 테이블 영향도 탭 (#30) + UX 개선
```

#### 배운 점

1. **Stash 사용 시 주의점**
   - `git stash`는 임시 저장소지만, 오래 방치하면 원격과 충돌 가능성 증가
   - 가능하면 stash보다 브랜치를 사용하는 것이 충돌 관리에 유리

2. **병렬 작업 시 세션 번호 관리**
   - 여러 환경에서 작업 시 세션 번호가 중복될 수 있음
   - 날짜/시간 기반으로 세션 순서를 재정렬하는 기준 필요

3. **충돌 해결 전략**
   - `<<<<<<< Updated upstream`: 원격(pull 받은 내용)
   - `>>>>>>> Stashed changes`: 로컬(stash 내용)
   - 둘 다 유효한 내용이면 **병합**, 하나만 필요하면 **선택**

4. **문서 충돌의 특성**
   - 코드 충돌과 달리 문서 충돌은 "둘 다 맞는 내용"인 경우가 많음
   - 시간순/논리순으로 재정렬하여 병합하는 것이 일반적

5. **예방책**
   - 작업 시작 전 `git pull` 습관화
   - 장시간 작업 시 중간중간 push하여 원격과 동기화
   - DEV_LOG.md처럼 자주 수정되는 파일은 작업 단위를 작게 유지

---

## 실무 테스트 발견 문제 (Session 31)

### Issue #031: HTTP Method가 ALL로 표시됨

**발생일**: 2026-01-15
**상태**: 🟡 진행중

#### 문제 상황
실무 프로젝트에서 CTF 테스트 결과, 모든 엔드포인트가 `[ALL]`로 표시됨:
```
[ALL] /kiss/getKissEncryption.do
[ALL] /bookFinder/system/start
[ALL] /bookFinder/main
```

실제 코드에는 GET/POST 구분이 있는데도 ALL로 표시되어 혼란 발생.

#### 원인 분석
`JavaSourceParser.extractHttpMethod()` 함수가 **어노테이션 이름**만으로 HTTP 메서드 결정:

```java
// JavaSourceParser.java:519-525
private String extractHttpMethod(String annotationName) {
    if (annotationName.equals("GetMapping")) return "GET";
    if (annotationName.equals("PostMapping")) return "POST";
    // ...
    return "ALL";  // ← @RequestMapping은 여기로 빠짐
}
```

**문제 케이스**:
1. `@RequestMapping("/url")` → method 속성 없음 → ALL
2. `@RequestMapping(value="/url", method=RequestMethod.POST)` → method 속성 파싱 안 함 → ALL

#### 해결 방안

**방안 1: @RequestMapping의 method 속성 파싱** ✅ 권장
```java
// NormalAnnotationExpr에서 method 속성 추출
if (annotationName.equals("RequestMapping")) {
    for (MemberValuePair pair : annotation.getPairs()) {
        if (pair.getNameAsString().equals("method")) {
            // RequestMethod.GET, RequestMethod.POST 등 파싱
            return extractMethodFromValue(pair.getValue());
        }
    }
    return "ALL";  // method 속성 없으면 실제로 ALL
}
```

**방안 2: 표시 방식 변경**
- "ALL" 대신 "ANY" 또는 "@RequestMapping"으로 표시
- 사용자에게 "이건 모든 HTTP 메서드 허용"임을 명확히 전달

#### 선택된 해결책: 방안 1 (method 속성 파싱)

**구현 계획**:
1. `JavaSourceParser.parseMethod()`에서 `@RequestMapping` 처리 시 method 속성 확인
2. `NormalAnnotationExpr`의 `MemberValuePair`에서 "method" 키 찾기
3. `RequestMethod.GET` → "GET" 변환 로직 추가
4. 복수 메서드 `{GET, POST}` → "GET/POST" 처리

---

### Issue #032: 같은 클래스 내부 함수 호출 미추적

**발생일**: 2026-01-15
**상태**: 🟡 진행중

#### 문제 상황
Controller 내에서 private 메서드 호출이 추적되지 않음:

```java
@Controller
public class KissEncryption {
    @RequestMapping("/kiss/getKissEncryption.do")
    public JSONObject getKissEncryption(...) {
        String code = this.getCode();   // ← 추적 안 됨
        String today = getToday();      // ← 추적 안 됨
        // ...
    }

    private String getCode() { ... }    // 이 메서드로 따라가지 않음
    private String getToday() { ... }
}
```

#### 원인 분석
`MethodCall.isServiceOrDaoCall()` 함수가 scope 기반으로 필터링:

```java
// MethodCall.java:54-62
public boolean isServiceOrDaoCall() {
    if (scope == null || scope.isEmpty()) {
        return false;  // ← 내부 호출은 scope가 비어있음!
    }
    String lowerScope = scope.toLowerCase();
    return lowerScope.contains("service") ||
           lowerScope.contains("dao") || ...
}
```

**필터링 조건**:
- `userService.findById()` → scope="userService" → ✅ 추적
- `getCode()` → scope="" → ❌ 스킵
- `this.getCode()` → scope="this" → ❌ 스킵 (service/dao 아님)

#### 해결 방안

**방안 1: 현재 클래스 컨텍스트 전달** ✅ 권장
```java
// buildFlowTree()에서 현재 클래스를 traceMethodCall()에 전달
private FlowNode traceMethodCall(MethodCall call, int depth, ParsedClass currentClass) {
    // scope가 비어있거나 "this"인 경우 → 현재 클래스에서 메서드 찾기
    if (call.getScope().isEmpty() || call.getScope().equals("this")) {
        ParsedMethod localMethod = findMethod(currentClass, call.getMethodName());
        if (localMethod != null) {
            return buildFlowTree(currentClass, localMethod, depth);
        }
    }
    // 기존 로직...
}
```

**방안 2: 옵션으로 제공**
- `--trace-internal-calls` 옵션 추가
- 기본값 OFF (노이즈 방지), 필요 시 ON

**트레이드오프 고려**:
- 장점: 더 완전한 호출 흐름 파악
- 단점: 노이즈 증가 (getter/setter, 유틸 메서드 등 모두 표시)
- 해결: private 메서드만 추적 또는 depth 제한

#### 선택된 해결책: 방안 1 (현재 클래스 컨텍스트 전달)

**구현 계획**:
1. `FlowAnalyzer.buildFlowTree()`에서 `traceMethodCall()` 호출 시 `currentClass` 전달
2. `traceMethodCall()` 시그니처 변경: `(MethodCall, int, ParsedClass)`
3. scope가 빈 문자열 또는 "this"인 경우 현재 클래스에서 메서드 검색
4. 노이즈 방지: `get*`, `set*`, `is*` 패턴 제외 옵션

---

### Issue #033: DAO → XML SQL ID 매칭 불일치

**발생일**: 2026-01-15
**상태**: 🟡 진행중

#### 문제 상황
일부 DAO 메서드에서 SQL ID가 추출되고, 일부는 안 됨:

```java
// ✅ 추적됨
selectOne("bookFinder.getBookFinderInfo", param);

// ❌ 추적 안 됨
getSqlSession().selectOne("bookFinder.xxx", param);
String sqlId = "bookFinder.xxx";
selectOne(sqlId, param);
```

#### 원인 분석
`JavaSourceParser.extractSqlId()` 함수의 제한적인 패턴 매칭:

```java
// JavaSourceParser.java:232-258
private String extractSqlId(MethodCallExpr call) {
    // 1. 특정 메서드명만 인식
    List<String> sqlMethods = List.of(
        "list", "selectList", "queryForList",
        "select", "selectOne", "queryForObject",
        "insert", "update", "delete"
    );
    if (!sqlMethods.contains(methodName)) {
        return null;  // ← getSqlSession() 같은 체이닝은 스킵
    }

    // 2. 첫 번째 인자가 문자열 리터럴인 경우만
    Expression firstArg = call.getArgument(0);
    if (firstArg instanceof StringLiteralExpr) {  // ← 변수면 스킵
        return ((StringLiteralExpr) firstArg).getValue();
    }
    return null;
}
```

**한계점**:
1. 메서드 체이닝 미지원: `getSqlSession().selectOne(...)`
2. 변수 참조 미지원: `selectOne(sqlId, param)`
3. 하드코딩된 메서드명 목록

#### 해결 방안

**방안 1: XML 기반 역매칭** ✅ 권장 (패턴 의존 제거)
```java
// 1단계: XML 파일에서 모든 SQL ID 수집
Set<String> allSqlIds = xmlParser.getAllSqlIds();
// 예: {"bookFinder.getBookFinderInfo", "use.selectOriginalDBStatus", ...}

// 2단계: DAO 메서드의 모든 문자열 리터럴 추출
for (StringLiteralExpr literal : method.findAll(StringLiteralExpr.class)) {
    String value = literal.getValue();
    // 3단계: SQL ID 목록에 존재하면 매칭
    if (allSqlIds.contains(value)) {
        return value;
    }
}
```

**장점**:
- 메서드명에 의존하지 않음
- 변수 문제 일부 해결 (인라인된 문자열은 찾을 수 있음)
- XML에 정의된 실제 SQL만 매칭 (오탐 감소)

**방안 2: 메서드 체이닝 지원 추가**
```java
// getSqlSession().selectOne(...) 패턴 인식
if (call.getScope().map(Object::toString).orElse("").contains("getSqlSession")) {
    // 내부 호출에서 SQL ID 추출
}
```

**방안 3: 상수/필드 추적 (고급)**
```java
// 클래스 레벨 상수 수집
private static final String SQL_ID = "bookFinder.xxx";

// 메서드 내 지역 변수 추적 (제한적)
String sqlId = "bookFinder.xxx";  // 이건 추적 어려움
```

#### 선택된 해결책: 방안 1 (XML 기반 역매칭)

**구현 계획**:
1. `XmlParser`에 `getAllSqlIds()` 메서드 추가 - 모든 namespace.id 수집
2. `FlowAnalyzer.analyze()` 시작 시 SQL ID Set 미리 수집
3. `JavaSourceParser.extractSqlId()` 개선 - 메서드 내 모든 문자열 리터럴 검사
4. 문자열이 SQL ID Set에 존재하면 매칭 (메서드명 무관)

**장점**:
- 메서드 체이닝 `getSqlSession().selectOne(...)` 자동 지원
- 하드코딩된 메서드명 목록 제거
- XML에 존재하는 SQL만 매칭 (오탐 감소)

---

### Issue #034: 대용량 프로젝트 분석 시 앱 멈춤 + 에러 핸들링 부재

**발생일**: 2026-01-27
**해결일**: 2026-01-28
**상태**: 🟢 해결됨

#### 문제 상황
실무 AS-IS 시스템 분석 시 "분석 실행" 버튼 클릭 후 앱이 멈춰버림 (Hang):
- GUI가 응답 없음 상태로 전환
- 진행 상황 표시 없음
- 에러 메시지 없이 무한 대기
- 강제 종료 외 방법 없음

#### 근본 원인 분석

| 문제점 | 설명 |
|--------|------|
| 타임아웃 없음 | 분석이 무한히 진행될 수 있음 |
| 에러 메시지 부재 | SwingWorker 내부 예외가 숨겨짐 |
| 에러 로깅 없음 | 디버깅을 위한 로그 파일 없음 |
| 취소 불가 | 사용자가 분석을 중단할 수 없음 |

#### 최종 해결 (Session 33-34)

**1. 로깅 시스템 구현 (`CftLogger.java` 신규)**

```java
// 설계 결정: SLF4J 대신 java.util.logging 선택
// 이유: 폐쇄망 환경에서 추가 JAR 배포 불필요 (JDK 내장)
public class CftLogger {
    private static CftLogger instance;  // 싱글톤
    private final Logger logger;

    // 로그 경로: ~/.code-flow-tracer/logs/cft.log
    // 로테이션: 설정 크기(1/5/10MB) × 3개 파일
    private void initialize() {
        FileHandler fileHandler = new FileHandler(
            logFilePath.toString(), logSizeBytes, MAX_LOG_COUNT, true);
        fileHandler.setFormatter(new CftLogFormatter());
        // ...
    }
}
```

- 로그 레벨: INFO(진행 상황), WARNING(경고), SEVERE(에러), FINE(디버그)
- 커스텀 포맷: `[2026-01-28 14:30:45] [INFO] 메시지`
- 분석 시작/완료/실패/취소/타임아웃 전용 로그 메서드 제공

**2. 분석 취소 버튼 (토글 방식)**

```java
// 분석 전: [▶ 분석 실행] (파란색)
// 분석 중: [■ 분석 취소] (빨간색)
analyzeButton.addActionListener(e -> {
    if (isAnalyzing) {
        analysisWorker.cancel(true);  // 취소
    } else {
        startAnalysis();  // 시작
    }
});
```

- SwingWorker.cancel(true) 사용
- isCancelled() 체크 포인트 3곳 (파싱/분석/출력 단계)

**3. 분석 타임아웃 (기본 5분)**

```java
// javax.swing.Timer 사용 (EDT 안전)
analysisTimer = new Timer(TIMEOUT_MINUTES * 60 * 1000, e -> {
    if (analysisWorker != null && !analysisWorker.isDone()) {
        analysisWorker.cancel(true);
        log.logAnalysisTimeout(TIMEOUT_MINUTES);
        showErrorWithLogPath("분석 시간이 초과되었습니다.");
    }
});
```

- 선택 이유: `javax.swing.Timer`는 EDT에서 콜백 → UI 조작 안전
- `java.util.Timer`나 `ScheduledExecutor`는 별도 스레드 → SwingUtilities.invokeLater 필요

**4. 에러 다이얼로그 + 로그 경로 안내**

```java
@Override
protected void done() {
    try {
        FlowResult result = get();
        // 성공 처리...
    } catch (CancellationException e) {
        log.logAnalysisCancelled();
    } catch (Exception e) {
        log.logAnalysisError(e.getCause());
        showErrorWithLogPath("분석 중 오류: " + e.getCause().getMessage());
    }
}

private void showErrorWithLogPath(String message) {
    JOptionPane.showMessageDialog(this,
        message + "\n\n로그 파일: " + log.getLogPath(),
        "오류", JOptionPane.ERROR_MESSAGE);
}
```

**5. 로그 설정 UI (설정 메뉴)**

```
⚙ 설정 버튼 →
┌──────────────────────┐
│ 로그 폴더 열기       │  ← Desktop.open()
├──────────────────────┤
│ 로그 크기 설정   ▶ ──┼── ○ 1MB (최대 3MB)
│                      │   ● 5MB (최대 15MB) - 기본
│                      │   ○ 10MB (최대 30MB)
├──────────────────────┤
│ 로그 파일 삭제       │  ← closeHandlers() 후 삭제
├──────────────────────┤
│ 설정/세션 초기화     │
└──────────────────────┘
```

- 로그 크기 설정은 세션(session.json)에 저장되어 재시작 후 유지
- SessionData에 `logSizeMB` 필드 추가

#### GUI 테스트 중 발견된 버그 3건

**버그 1: 로그 크기 설정이 앱 재시작 후 유지되지 않음**

- 증상: 10MB로 변경 → 종료 → 재시작 → 5MB로 표시
- 원인: `createSettingsPopupMenu()`가 앱 시작 시 한 번만 호출되어 라디오 버튼이 초기값으로 캐시됨
- 해결: 팝업 메뉴를 클릭할 때마다 새로 생성

```java
// Before (캐시된 메뉴 재사용)
JPopupMenu settingsPopup = createSettingsPopupMenu();
settingsButton.addActionListener(e -> settingsPopup.show(...));

// After (매번 새로 생성)
settingsButton.addActionListener(e -> {
    JPopupMenu settingsPopup = createSettingsPopupMenu();
    settingsPopup.show(...);
});
```

- 교훈: Swing에서 동적 상태를 반영하는 UI(라디오 버튼 등)는 캐시하면 안 됨

**버그 2: SLF4J 콘솔 로그 한글 깨짐**

- 증상: SessionManager의 SLF4J 로그만 깨짐, CftLogger/System.out.println은 정상
- 원인: SLF4J/Logback은 UTF-8로 출력 → Windows 콘솔(MS949)에서 깨짐
- 시도: logback.xml charset 설정 → 실패 (Logback 내부 동작)
- 해결: SessionManager에서 SLF4J를 제거하고 CftLogger로 교체

```java
// Before: SLF4J (한글 깨짐)
import org.slf4j.Logger;
log.info("세션 로드 완료: {} ({} flows)", path, count);  // {} 플레이스홀더

// After: CftLogger (정상 출력)
import com.codeflow.util.CftLogger;
log.info("세션 로드 완료: %s (%d flows)", path, count);  // %s 포맷
```

**주의: `error(message, throwable)` 변환 시 `%s`가 아닌 `+` 연결 필요**

SLF4J에서 CftLogger로 변환할 때, `info`/`warn`은 `{}` → `%s`로 단순 치환하면 되지만, `error`는 다르다:

```java
// SLF4J: {} + 마지막 인자 Throwable 자동 처리 (SLF4J 내부 런타임 체크)
log.error("설정 로드 실패: {}", e.getMessage(), e);

// CftLogger: error(String, Throwable) → 포맷 문자열 미지원
// ❌ 틀린 변환
log.error("설정 로드 실패: %s", e.getMessage(), e);  // 이런 시그니처 없음

// ✅ 올바른 변환
log.error("설정 로드 실패: " + e.getMessage(), e);   // + 연결로 문자열 완성
```

**근본 원인: Java varargs 제약**

`info`에는 varargs 오버로드가 있지만, `error`에서는 Throwable과 varargs를 함께 쓸 수 없다:
```java
// CftLogger 메서드 시그니처
public void info(String format, Object... args)      // ✅ varargs 사용 가능
public void error(String message, Throwable throwable) // Throwable 전용

// 이런 시그니처는 Java에서 불가능 (varargs는 반드시 마지막 파라미터)
public void error(String format, Throwable t, Object... args)  // ❌ 컴파일 에러
public void error(String format, Object... args, Throwable t)  // ❌ 컴파일 에러
```

SLF4J는 이 제약을 우회하기 위해 `Object... args`의 마지막 인자가 Throwable인지 **런타임에 체크**하여 스택트레이스를 자동 추출한다. java.util.logging 기반인 CftLogger에는 이런 처리가 없으므로, 에러+예외 로깅 시에는 `+` 연결로 메시지를 먼저 완성해야 한다.

- 교훈:
  1. Windows에서 Java 콘솔 인코딩은 라이브러리마다 처리 방식이 다름. 한글 환경에서는 일관된 로깅 시스템 사용이 안전.
  2. SLF4J → 다른 로거 변환 시 `{}` → `%s` 단순 치환이 아니라, 메서드 시그니처(특히 Throwable 인자)를 확인해야 한다.
  3. Java varargs는 반드시 마지막 파라미터여야 하므로, Throwable과 가변 인자를 동시에 받는 메서드는 설계 불가.

**버그 3: 로그 파일 삭제 시 사용 중인 파일 삭제 불가**

- 증상: "로그 파일 삭제" → 일부만 삭제, 현재 사용 중인 `cft.log`는 남음
- 원인: Windows에서 FileHandler가 파일을 잠금 → Files.delete() 시 AccessDeniedException
- 해결: CftLogger에 `closeHandlers()` 메서드 추가하여 잠금 해제 후 삭제

```java
// CftLogger.java
public void closeHandlers() {
    for (Handler handler : logger.getHandlers()) {
        handler.close();           // 파일 잠금 해제
        logger.removeHandler(handler);
    }
    initialized = false;           // 다음 로그 호출 시 자동 재초기화
}

// ensureInitialized() 패턴으로 자동 복구
public void info(String message) {
    ensureInitialized();  // initialized == false이면 initialize() 호출
    logger.info(message);
}
```

- 삭제 흐름: `closeHandlers()` → 파일 삭제 → 다음 로그 호출 시 자동 재초기화
- 교훈: Windows 파일 잠금은 Java에서도 적용됨. Lazy initialization 패턴으로 복구 가능.

**버그 4: 로그 파일 삭제 직후 `cft.log.0` 즉시 재생성**

- 증상: "로그 파일 삭제" 클릭 → "삭제되었습니다" 표시 → 로그 폴더에 `cft.log.0` + `cft.log.0.lck` 남아있음. 프로그램 종료 후에도 `cft.log.0`이 남음
- 원인: 삭제 직후 `logger.info("로그 파일 삭제 후 로거 재시작")` 호출이 `ensureInitialized()` 트리거 → `initialize()` → 새 FileHandler 생성 → `cft.log.0` + `.lck` 즉시 재생성
- `.lck` 파일: `java.util.logging.FileHandler`가 파일을 열 때 자동 생성하는 잠금 파일. 프로그램 종료 시 FileHandler가 닫히면서 `.lck`만 삭제됨

```java
// Before (MainFrame.java - handleClearLogs)
logger.closeHandlers();                      // 1. 핸들러 닫기
// ... Files.delete(file) ...               // 2. 파일 삭제 (성공)
logger.info("로그 파일 삭제 후 로거 재시작");   // 3. ★ ensureInitialized() → 새 파일 즉시 생성!

// After
logger.closeHandlers();                      // 1. 핸들러 닫기
// ... Files.delete(file) ...               // 2. 파일 삭제 (성공)
// logger.info() 호출 제거 → 다음 실제 로그 이벤트까지 파일 없음
```

- 해결: 삭제 로직 이후 불필요한 `logger.info()` 호출 제거. 로거는 닫힌 상태로 유지되며, 다음 실제 로그 이벤트(분석 시작 등) 발생 시 `ensureInitialized()`로 자동 재시작
- 교훈: Lazy initialization 패턴은 리소스 정리 직후 같은 리소스를 사용하는 코드가 있으면 즉시 재생성됨. 정리 로직 이후에는 해당 리소스를 다시 사용하는 코드가 없는지 반드시 확인해야 함

#### 변경된 파일

| 파일 | 변경 내용 |
|------|----------|
| `CftLogger.java` (신규) | 로깅 시스템 전체, closeHandlers(), ensureInitialized(), logSizeMB 설정 |
| `SessionData.java` | logSizeMB 필드 추가 (기본 5MB) |
| `SessionManager.java` | SLF4J → CftLogger 교체, logSizeMB 유지 로직, {} → %s 포맷 변환 |
| `MainFrame.java` | 취소 버튼, 타임아웃, 에러 다이얼로그, 설정 메뉴(로그 폴더/크기/삭제), 팝업 메뉴 매번 생성 |
| `logback.xml` (신규) | SLF4J를 사용하는 다른 라이브러리용 기본 설정 |
| `CftLoggerTest.java` (신규) | 23개 단위 테스트 |
| `SessionDataTest.java` | logSizeMB 테스트 5개 추가 |
| `SessionManagerTest.java` | logSizeMB 저장/로드 테스트 5개 추가 |

#### 기술적 결정 요약

| 결정 | 선택 | 이유 |
|------|------|------|
| 로깅 라이브러리 | java.util.logging | 폐쇄망 환경, JDK 내장, 추가 의존성 없음 |
| 타이머 | javax.swing.Timer | EDT에서 실행, UI 조작 안전 |
| 취소 방식 | 버튼 토글 | 추가 버튼 없이 직관적 UX |
| 로그 로테이션 | 설정 크기 × 3개 | 디스크 보호, 사용자 선택 가능 |
| 설정 저장 | SessionData 통합 | 기존 JSON 세션 파일 활용, 별도 파일 불필요 |
| 한글 깨짐 | SLF4J 제거 | logback 설정으로 해결 불가, CftLogger 통합이 근본 해결 |
| 파일 잠금 | closeHandlers() + ensureInitialized() | 삭제 전 잠금 해제, 이후 자동 복구 |
| 삭제 후 재생성 방지 | 삭제 직후 logger 호출 제거 | Lazy init이 즉시 재생성하는 것 방지 |

#### 배운 점

1. **에러 핸들링은 기능**: "에러가 없다"가 아니라 "에러를 보여주지 않는다"가 진짜 문제
2. **Windows 파일 잠금**: FileHandler가 열고 있는 파일은 삭제 불가, 반드시 close 필요
3. **인코딩 일관성**: 같은 앱에서 여러 로깅 시스템을 쓰면 인코딩 불일치 발생
4. **Swing UI 캐시 주의**: 동적 상태를 반영하는 컴포넌트(라디오 버튼 등)는 매번 생성
5. **Lazy initialization**: 리소스 해제 후 자동 복구에 유용한 패턴
6. **Lazy initialization 부작용**: 정리 직후 같은 리소스를 사용하면 즉시 재생성됨 — 정리 로직 이후 코드 경로 점검 필수

---

## 참고 자료

- [JavaParser 공식 문서](https://javaparser.org/)
- [Gradle 문제 해결](https://docs.gradle.org/current/userguide/troubleshooting.html)
- [Stack Overflow - JavaParser 태그](https://stackoverflow.com/questions/tagged/javaparser)
