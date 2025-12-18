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

---

## 미해결/진행중 문제

(현재 없음)

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
