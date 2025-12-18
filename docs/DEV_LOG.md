# 진행 상황 (Development Log)

> 토스 러너스하이 2기 프로젝트 개발 일지

---

## Week 1

### Day 1 - 2025-12-17 (화)

#### 오늘 한 일
1. **프로젝트 기획 완료**
   - 토스 러너스하이 1기 후기 분석
   - Pain Point 도출: 레거시 코드 흐름 파악의 어려움
   - 프로젝트 방향 결정: Code Flow Tracer

2. **기술 스택 결정**
   - Java 17, Gradle, JavaParser, JDOM2, Apache POI, Swing
   - 각 기술 선택의 "왜"를 문서화

3. **프로젝트 셋업**
   - Gradle 프로젝트 생성
   - 의존성 설정 (JavaParser, POI, Picocli 등)
   - 패키지 구조 설계

4. **기본 파싱 로직 구현**
   - JavaSourceParser: Java 소스 파일 AST 파싱
   - Controller/Service/DAO 타입 판별
   - 메서드 호출 관계 추출

5. **테스트 코드 작성**
   - 전자정부프레임워크 스타일 샘플 코드 작성
   - JUnit 5 테스트 통과

6. **문서화 구조 설정**
   - docs/ 폴더 생성
   - CLAUDE.md, DESIGN.md, USAGE.md, TODO.md, DEV_LOG.md, ISSUES.md

#### 배운 점
- JavaParser 라이브러리가 생각보다 강력함
- AST 기반 분석으로 정확한 메서드 호출 추출 가능
- 문서화를 먼저 해두면 방향성이 명확해짐

#### 내일 할 일
- FlowAnalyzer 구현 (클래스 간 호출 연결)
- 인터페이스 → 구현체 매핑 로직

#### 고민/질문
- 인터페이스와 구현체 매핑을 어떻게 할 것인가?
  - 방법 1: 클래스명 컨벤션 (UserService → UserServiceImpl)
  - 방법 2: @Service 어노테이션의 name 속성
  - → 두 방법 모두 지원하는 것으로 결정

---

### Day 2 - 2025-12-17 (화)

#### 오늘 한 일
1. **FlowAnalyzer 핵심 구현 완료**
   - `FlowNode.java`: 호출 흐름 트리 노드
   - `FlowResult.java`: 분석 결과 컨테이너
   - `FlowAnalyzer.java`: 호출 흐름 분석 엔진

2. **인터페이스 → 구현체 매핑 구현**
   - `Impl` 접미사 기반 자동 매핑 (UserService → UserServiceImpl)
   - scope(변수명) → 클래스명 변환 (userService → UserServiceImpl)

3. **분석 알고리즘 구현**
   - Controller 엔드포인트에서 시작하여 재귀적 호출 추적
   - 순환 참조 방지 로직
   - 최대 깊이 제한 (10단계)

4. **테스트 코드 작성 및 통과**
   - `FlowAnalyzerTest.java`: 7개 테스트 케이스
   - 전체 프로젝트 분석, 엔드포인트 분석, URL 필터링 등
   - BUILD SUCCESSFUL (모든 테스트 통과)

#### 배운 점
- 재귀적 트리 구조 설계 시 순환 참조 방지가 중요
- scope(변수명)에서 클래스를 추정하는 휴리스틱이 효과적
- 인터페이스-구현체 매핑은 컨벤션 기반이 가장 실용적

#### 내일 할 일
- @RequestMapping URL 추출 개선
- URL 패턴 매칭 로직 강화
- 콘솔 출력 구현 시작

#### 고민/질문
- DAO에서 SQL ID를 정확히 추출하려면 추가 파싱 로직 필요
- IBatisParser 연동 시점을 Week 2로 유지할지 고민

---

### Day 4 - 2025-12-18 (수)

#### 오늘 한 일

1. **ConsoleOutput 클래스 구현**
   - 위치: `com.codeflow.output.ConsoleOutput`
   - FlowResult를 콘솔에 보기 좋게 출력하는 전담 클래스

2. **트리 형태 출력 포맷팅**
   - 박스 문자 사용 (├── └── │)
   - 다중 레벨 호출 추적 시각화
   - Controller → Service → DAO 흐름 명확히 표현

3. **ANSI 색상 지원**
   - 클래스 타입별 색상: Controller(녹색), Service(파랑), DAO(보라)
   - HTTP 메서드별 색상: GET(녹색), POST(노랑), DELETE(빨강)
   - `useColors` 옵션으로 색상 on/off 가능

4. **3가지 출력 스타일 구현**
   - `COMPACT`: 클래스.메서드만 출력
   - `NORMAL`: 타입 태그 + URL 매핑 포함
   - `DETAILED`: SQL ID 등 모든 정보 포함

5. **정적 팩토리 메서드 제공**
   - `ConsoleOutput.colored()`: 색상 있는 기본 출력
   - `ConsoleOutput.plain()`: 색상 없는 출력 (파일 저장용)
   - `ConsoleOutput.detailed()`: 상세 출력
   - `ConsoleOutput.compact()`: 간략 출력

#### 왜 이렇게 구현했는가?

1. **출력 전담 클래스를 분리한 이유 (SRP)**
   - 기존: FlowNode/FlowResult에 `toTreeString()` 메서드 존재
   - 문제: 데이터 클래스가 출력 책임까지 가짐
   - 해결: 단일 책임 원칙에 따라 출력 로직 분리

2. **ANSI 색상을 사용한 이유**
   - 가독성 향상: 타입별로 시각적 구분
   - 터미널 친화적: 대부분의 현대 터미널 지원
   - 옵션 제공: 파일 출력 시에는 색상 제거 가능

3. **3가지 스타일을 제공한 이유**
   - 사용 목적에 따라 다른 상세도 필요
   - COMPACT: 빠른 개요 파악
   - DETAILED: 디버깅, SQL 추적

#### 배운 점

- Java에서 ANSI escape code 사용법
- 트리 구조 출력을 위한 재귀 알고리즘
- PrintStream을 사용한 출력 추상화의 유용성

---

### Day 4 추가 작업 - 2025-12-18 (수)

#### 오늘 한 일 (추가)

1. **Windows 환경 한글 깨짐 문제 해결**
   - 문제: IntelliJ 콘솔에서 한글이 `��ü Ŭ����:` 처럼 깨짐
   - 원인: `System.out`이 JVM 기본 인코딩(CP949) 사용
   - 해결: UTF-8 PrintStream 생성하여 사용

2. **Java 10+ API 활용한 코드 개선**
   - Before: try-catch로 `UnsupportedEncodingException` 처리 (8줄)
   - After: `Charset` 직접 전달 + 싱글톤 패턴 (2줄)
   ```java
   // Java 10+ API - 예외 처리 불필요
   private static final PrintStream UTF8_OUT =
       new PrintStream(System.out, true, StandardCharsets.UTF_8);
   ```

3. **한글 폭 계산 로직 추가**
   - 문제: 박스 출력 시 한글 정렬 어긋남
   - 원인: `String.length()`는 문자 수만 반환, 한글은 2칸 폭
   - 해결: `getDisplayWidth()`, `isWideChar()` 메서드 추가

4. **배포 스크립트 기본 구조 생성**
   - `scripts/run.bat`: GUI 모드 실행
   - `scripts/analyze.bat`: CLI 모드 실행
   - 향후 jlink 경량 JRE 번들 배포 준비

5. **문서 업데이트**
   - ISSUES.md: #004 한글 깨짐, #005 박스 정렬 문제 기록
   - USAGE.md: SI 현장/폐쇄망 환경 설치 가이드 추가
   - TODO.md: 패키징 로드맵 추가 (jpackage, jlink)

#### 왜 이렇게 구현했는가?

1. **싱글톤 패턴 사용 이유**
   - `new PrintStream()` 매번 호출 → 객체 생성 비용
   - `static final`로 한 번만 생성, 재사용
   - 같은 기능인데 메모리/CPU 절약

2. **Java 10+ API 선택 이유**
   - 프로젝트 타겟이 Java 17 → Java 10 API 사용 가능
   - `PrintStream(OutputStream, boolean, Charset)` 생성자는 예외 안 던짐
   - checked exception 처리 불필요 → 코드 간결화

3. **배포 스크립트 구조 설계**
   - 번들 JDK 있으면 사용, 없으면 시스템 Java 사용
   - SI 현장의 기존 Java 환경과 충돌 방지

#### 배운 점

- `System.out`은 플랫폼 인코딩에 의존 → 이식성 문제
- Java 버전별 API 개선사항 확인의 중요성
- 싱글톤 패턴으로 불필요한 객체 생성 방지
- `Character.UnicodeBlock`으로 문자 종류 판별 가능

#### 내일 할 일

- Picocli CLI 연동
- 명령어 옵션 (--style, --output 등)

---

### Day 3 - 2025-12-18 (수)

#### 오늘 한 일

1. **@RequestMapping URL 추출 개선**
   - 문제: 기존 `extractUrlFromAnnotation()`이 URL 값을 추출하지 않고 어노테이션 전체 문자열 반환
   - 해결: JavaParser의 어노테이션 타입별 처리 구현
     - `SingleMemberAnnotationExpr`: `@GetMapping("/list.do")`
     - `NormalAnnotationExpr`: `@RequestMapping(value = "/list.do")`
     - `MarkerAnnotationExpr`: `@GetMapping` (값 없음)

2. **클래스 레벨 + 메서드 레벨 URL 조합**
   - 문제: `@RequestMapping("/user")` 클래스 레벨 URL이 무시됨
   - 해결: `ParsedClass.baseUrlMapping` 필드 추가, `combineUrls()` 메서드로 조합
   - 결과: `/user` + `/list.do` = `/user/list.do`

3. **URL 패턴 매칭 유틸리티 구현 (UrlMatcher.java)**
   - 정확한 매칭: `/user/list.do`
   - 와일드카드: `/user/*`, `/user/**`
   - PathVariable: `/user/{id}`
   - 부분 매칭: `user` (URL에 포함되면 매칭)

4. **테스트 코드 보강**
   - `testUrlCombination()`: 클래스+메서드 URL 조합 검증
   - `testHttpMethodExtraction()`: HTTP 메서드 추출 검증
   - `UrlMatcherTest`: 13개 테스트 케이스

#### 왜 이렇게 구현했는가?

1. **어노테이션 타입별 처리가 필요한 이유**
   - JavaParser는 어노테이션을 3가지 타입으로 구분
   - 각 타입마다 값 접근 방식이 다름
   - 레거시 코드는 다양한 스타일이 혼재 → 모두 지원 필요

2. **UrlMatcher를 별도 클래스로 분리한 이유**
   - 단일 책임 원칙 (SRP): FlowAnalyzer는 흐름 분석에 집중
   - 재사용성: 향후 다른 곳에서도 URL 매칭 필요할 수 있음
   - 테스트 용이성: 독립적으로 URL 매칭 로직 테스트 가능

3. **와일드카드 패턴을 정규식으로 변환한 이유**
   - `*` → `[^/]*` (슬래시 제외)로 1단계만 매칭
   - `**` → `.*`로 모든 경로 매칭
   - 단순 문자열 비교보다 유연한 패턴 지원

#### 배운 점

- JavaParser AST에서 어노테이션 값을 추출하는 방법
- 정규식을 활용한 URL 패턴 매칭 전략
- 단위 테스트의 중요성 - 13개 테스트로 엣지 케이스 검증

#### 내일 할 일

- ConsoleOutput 구현 (트리 형태 출력)
- CLI 통합 (Picocli 연동)

---

### Day 2 추가 작업 - 2025-12-17 (화)

#### 오늘 한 일 (추가)
1. **인터페이스-구현체 매핑 개선**
   - 기존: `Impl` 접미사에만 의존 → 한계 발견
   - 개선: `implements` 키워드 기반 매핑 추가
   - `Impl` 접미사는 fallback으로 유지

2. **ParsedClass 확장**
   - `isInterface` 필드 추가
   - `implementedInterfaces` 목록 추가

3. **JavaSourceParser 개선**
   - `clazz.isInterface()` 정보 추출
   - `clazz.getImplementedTypes()` 정보 추출

4. **테스트 코드 보강**
   - `testImplementsBasedMapping()` 테스트 추가
   - implements 정보 추출 검증

5. **문서 업데이트**
   - ISSUES.md: #003 문제 기록
   - IMPLEMENTATION_GUIDE.md: 매핑 전략 업데이트

#### 배운 점
- 정적 분석은 AST 정보를 최대한 활용해야 정확도가 높아짐
- 네이밍 컨벤션 기반 추정은 fallback으로만 사용
- 문제를 발견하면 바로 기록하고 해결하는 것이 중요

---

## 주간 회고

### Week 1 회고 (예정)
- 잘한 점:
- 아쉬운 점:
- 다음 주 목표:

---

## 전체 타임라인

```
Week 1: 설계 + 기본 파서 ████████████████████ 100%
Week 2: iBatis + 출력   ░░░░░░░░░░░░░░░░░░░░ 0%
Week 3: GUI + 테스트    ░░░░░░░░░░░░░░░░░░░░ 0%
Week 4: 개선 + 회고     ░░░░░░░░░░░░░░░░░░░░ 0%
```

---

## 핵심 지표

| 지표 | 현재 | 목표 |
|------|------|------|
| 구현된 기능 | 5/10 | 10/10 |
| 테스트 통과율 | 100% | 100% |
| 문서 완성도 | 80% | 100% |
| 코드 커버리지 | - | 80% |
