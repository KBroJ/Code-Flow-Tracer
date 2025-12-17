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
