# Week 1: 프로젝트 기획 및 핵심 기능 구현

> 2025-12-17 (화) ~ 2025-12-18 (수)
> Session 1-4

---

## Session 1: 프로젝트 기획 및 셋업

**문제**: 레거시 Java 코드의 호출 흐름 파악이 어렵다
**해결**: Code Flow Tracer 도구 기획 및 기본 구조 설계

### 오늘 한 일
1. 토스 러너스하이 1기 후기 분석 → Pain Point 도출
2. 기술 스택 결정 (Java 17, Gradle, JavaParser, JDOM2, Apache POI, Swing)
3. Gradle 프로젝트 셋업 및 패키지 구조 설계
4. JavaSourceParser 기본 파싱 로직 구현
5. 테스트 코드 작성 및 문서화 구조 설정

### 핵심 결정
- **기술 선택 이유**: 각 기술의 "왜"를 문서화
- **인터페이스-구현체 매핑**: 두 방법(Impl 접미사, @Service name) 모두 지원

### 배운 점
- JavaParser가 생각보다 강력함 (AST 기반 정확한 메서드 호출 추출)
- 문서화를 먼저 해두면 방향성이 명확해짐

---

## Session 2: FlowAnalyzer 핵심 구현

**문제**: 파싱한 클래스에서 호출 흐름을 어떻게 추적할 것인가?
**해결**: 재귀적 트리 구조로 호출 흐름 분석

### 오늘 한 일
1. FlowNode, FlowResult, FlowAnalyzer 핵심 클래스 구현
2. 인터페이스 → 구현체 매핑 (Impl 접미사 + scope 변환)
3. Controller 엔드포인트에서 시작하여 재귀적 호출 추적
4. 순환 참조 방지 로직 + 최대 깊이 제한 (10단계)
5. 7개 테스트 케이스 작성 및 통과

### 핵심 결정
- **재귀적 트리 구조**: 호출 흐름이 자연스럽게 트리 형태
- **순환 참조 방지**: visitedMethods Set으로 중복 방문 체크

### 배운 점
- 재귀적 트리 설계 시 순환 참조 방지가 중요
- scope(변수명)에서 클래스를 추정하는 휴리스틱이 효과적
- 인터페이스-구현체 매핑은 컨벤션 기반이 가장 실용적

---

## Session 3: 인터페이스-구현체 매핑 개선

**문제**: Impl 접미사만으로는 모든 구현체를 찾을 수 없다
**해결**: implements 키워드 기반 매핑 추가

### 오늘 한 일
1. ParsedClass 확장 (isInterface, implementedInterfaces 필드)
2. JavaSourceParser 개선 (implements 정보 추출)
3. Impl 접미사는 fallback으로 유지
4. testImplementsBasedMapping() 테스트 추가
5. ISSUES.md #003 문제 기록

### 핵심 결정
- **AST 정보 최대 활용**: 네이밍 컨벤션보다 정확
- **Impl 접미사는 fallback**: 두 방식 병행

### 배운 점
- 정적 분석은 AST 정보를 최대한 활용해야 정확도가 높아짐
- 문제를 발견하면 바로 기록하고 해결하는 것이 중요

---

## Session 4: URL 추출 및 CLI 통합

**문제**: @RequestMapping URL을 정확히 추출해야 한다
**해결**: 어노테이션 타입별 처리 + URL 패턴 매칭 유틸리티

### 4-1. URL 추출 및 패턴 매칭

**오늘 한 일**
1. 어노테이션 타입별 처리 (Single/Normal/Marker)
2. 클래스 레벨 + 메서드 레벨 URL 조합
3. UrlMatcher.java 구현 (와일드카드, PathVariable 지원)
4. 13개 UrlMatcher 테스트 케이스

**핵심 결정**
- **UrlMatcher 분리**: 단일 책임 원칙 + 재사용성 + 테스트 용이성

### 4-2. ConsoleOutput 구현

**문제**: 분석 결과를 보기 좋게 출력해야 한다
**해결**: 트리 형태 + ANSI 색상 + 3가지 출력 스타일

**오늘 한 일**
1. ConsoleOutput 클래스 구현 (박스 문자, ANSI 색상)
2. 3가지 출력 스타일 (COMPACT, NORMAL, DETAILED)
3. Windows 한글 깨짐 해결 (UTF-8 PrintStream)
4. 한글 폭 계산 로직 추가

**핵심 결정**
- **UTF-8 싱글톤**: 매번 생성 대신 static final로 재사용
- **한글 폭 계산**: Character.UnicodeBlock으로 문자 종류 판별

### 4-3. Picocli CLI 통합

**문제**: CLI 옵션 파싱 및 실행 파이프라인 구성
**해결**: Picocli 어노테이션 기반 CLI + 파이프라인 분리

**오늘 한 일**
1. Main.java Picocli 통합 (--path, --url, --style, --output 등)
2. 통합 테스트 6개 작성
3. Windows 콘솔 한글 인코딩 문제 해결 (chcp 65001)

**핵심 결정**
- **파이프라인 분리**: Parser → Analyzer → Output (각 단계 독립)
- **chcp 배치 파일 래퍼**: Java에서 chcp 실행해도 부모 콘솔에 적용 안 됨

### 4-4. 파라미터 표시 및 순환참조 수정

**문제**: API 파라미터 정보 필요 + 순환참조 오탐
**해결**: 파라미터 추출 + 호출 스택 방식 순환참조 체크

**오늘 한 일**
1. ParameterInfo.java 생성, @RequestParam/@PathVariable 값 추출
2. VO 타입 getter 분석, Map.get() 키 추출
3. Spring 자동 주입 파라미터 필터링 (Model, HttpServletRequest 등)
4. 순환참조 오탐 수정: 전역 Set → 호출 스택 방식 (탐색 후 remove())

**핵심 결정**
- **호출 스택 방식**: 같은 메서드를 다른 경로에서 호출 가능하게

### 배운 점 (Session 4 전체)
- JavaParser AST에서 어노테이션 값 추출하는 방법
- Windows 콘솔 코드 페이지는 같은 프로세스에서 변경해야 적용됨
- 순환참조 체크는 "전체 방문"이 아닌 "현재 경로"로 해야 정확

---

## Week 1 완료!

- 2일간 4개 Session 완료
- 다음: Week 2 iBatis/MyBatis 파싱
