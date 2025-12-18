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
