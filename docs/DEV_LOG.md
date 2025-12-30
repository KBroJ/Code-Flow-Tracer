# 진행 상황 (Development Log)

> 토스 러너스하이 2기 프로젝트 개발 일지

---

## Week 1

### 2025-12-17 (화)

#### Session 1: 프로젝트 기획 및 셋업

**오늘 한 일**
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

**배운 점**
- JavaParser 라이브러리가 생각보다 강력함
- AST 기반 분석으로 정확한 메서드 호출 추출 가능
- 문서화를 먼저 해두면 방향성이 명확해짐

**고민/질문**
- 인터페이스와 구현체 매핑을 어떻게 할 것인가?
  - 방법 1: 클래스명 컨벤션 (UserService → UserServiceImpl)
  - 방법 2: @Service 어노테이션의 name 속성
  - → 두 방법 모두 지원하는 것으로 결정

---

#### Session 2: FlowAnalyzer 핵심 구현

**오늘 한 일**
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

**배운 점**
- 재귀적 트리 구조 설계 시 순환 참조 방지가 중요
- scope(변수명)에서 클래스를 추정하는 휴리스틱이 효과적
- 인터페이스-구현체 매핑은 컨벤션 기반이 가장 실용적

**고민/질문**
- DAO에서 SQL ID를 정확히 추출하려면 추가 파싱 로직 필요
- IBatisParser 연동 시점을 Week 2로 유지할지 고민

---

#### Session 3: 인터페이스-구현체 매핑 개선

**오늘 한 일**
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

**배운 점**
- 정적 분석은 AST 정보를 최대한 활용해야 정확도가 높아짐
- 네이밍 컨벤션 기반 추정은 fallback으로만 사용
- 문제를 발견하면 바로 기록하고 해결하는 것이 중요

---

### 2025-12-18 (수)

#### Session 1: URL 추출 및 패턴 매칭

**오늘 한 일**

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

**왜 이렇게 구현했는가?**

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

**배운 점**

- JavaParser AST에서 어노테이션 값을 추출하는 방법
- 정규식을 활용한 URL 패턴 매칭 전략
- 단위 테스트의 중요성 - 13개 테스트로 엣지 케이스 검증

---

#### Session 2: ConsoleOutput 구현

**오늘 한 일**

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

6. **Windows 환경 한글 깨짐 문제 해결**
   - 문제: IntelliJ 콘솔에서 한글이 `��ü Ŭ����:` 처럼 깨짐
   - 원인: `System.out`이 JVM 기본 인코딩(CP949) 사용
   - 해결: UTF-8 PrintStream 생성하여 사용

7. **Java 10+ API 활용한 코드 개선**
   - Before: try-catch로 `UnsupportedEncodingException` 처리 (8줄)
   - After: `Charset` 직접 전달 + 싱글톤 패턴 (2줄)
   ```java
   // Java 10+ API - 예외 처리 불필요
   private static final PrintStream UTF8_OUT =
       new PrintStream(System.out, true, StandardCharsets.UTF_8);
   ```

8. **한글 폭 계산 로직 추가**
   - 문제: 박스 출력 시 한글 정렬 어긋남
   - 원인: `String.length()`는 문자 수만 반환, 한글은 2칸 폭
   - 해결: `getDisplayWidth()`, `isWideChar()` 메서드 추가

9. **배포 스크립트 기본 구조 생성**
   - `scripts/run.bat`: GUI 모드 실행
   - `scripts/analyze.bat`: CLI 모드 실행
   - 향후 jlink 경량 JRE 번들 배포 준비

**왜 이렇게 구현했는가?**

1. **출력 전담 클래스를 분리한 이유 (SRP)**
   - 기존: FlowNode/FlowResult에 `toTreeString()` 메서드 존재
   - 문제: 데이터 클래스가 출력 책임까지 가짐
   - 해결: 단일 책임 원칙에 따라 출력 로직 분리

2. **ANSI 색상을 사용한 이유**
   - 가독성 향상: 타입별로 시각적 구분
   - 터미널 친화적: 대부분의 현대 터미널 지원
   - 옵션 제공: 파일 출력 시에는 색상 제거 가능

3. **싱글톤 패턴 사용 이유**
   - `new PrintStream()` 매번 호출 → 객체 생성 비용
   - `static final`로 한 번만 생성, 재사용
   - 같은 기능인데 메모리/CPU 절약

**배운 점**

- Java에서 ANSI escape code 사용법
- 트리 구조 출력을 위한 재귀 알고리즘
- PrintStream을 사용한 출력 추상화의 유용성
- `System.out`은 플랫폼 인코딩에 의존 → 이식성 문제
- `Character.UnicodeBlock`으로 문자 종류 판별 가능

---

#### Session 3: Picocli CLI 통합

**오늘 한 일**

1. **Picocli CLI 통합 완료**
   - Main.java에 실제 분석 로직 연결
   - 파이프라인: JavaSourceParser → FlowAnalyzer → ConsoleOutput

2. **CLI 옵션 구현**
   - `-p, --path`: 분석 대상 프로젝트 경로 (필수)
   - `-u, --url`: URL 패턴 필터 (예: `/api/user/*`)
   - `-s, --style`: 출력 스타일 (compact, normal, detailed)
   - `-o, --output`: 결과 파일 저장 경로
   - `--no-color`: 색상 출력 비활성화
   - `--gui`: GUI 모드 (Week 3 예정)

3. **통합 테스트 작성**
   - `IntegrationTest.java`: 6개 테스트 케이스
   - 전체 파이프라인, URL 필터링, 출력 스타일, 통계 검증

4. **README 기존 도구 비교 섹션 추가**
   - IntelliJ Call Hierarchy와 차이점
   - 유사 도구 비교 (java-callgraph, Sourcetrail 등)

5. **Windows 콘솔 한글 인코딩 문제 해결**
   - 문제: `--help` 및 분석 결과 한글 깨짐
   - 원인: Picocli가 `System.out` 직접 사용 (CP949)
   - 해결:
     - `--help`: `cmd.setOut(UTF8 PrintWriter)` + 영어 설명 전환
     - 분석 결과: `scripts/analyze.bat`에 `chcp 65001` 추가
   - 문서화: ISSUES.md #006, IMPLEMENTATION_GUIDE.md 섹션 10

**왜 이렇게 구현했는가?**

1. **Picocli 선택 이유**
   - 어노테이션 기반 → 간결한 코드
   - `--help` 자동 생성
   - 타입 변환, 필수값 검증 자동 처리

2. **파이프라인 분리 이유**
   - 파싱(Parser) → 분석(Analyzer) → 출력(Output) 분리
   - 각 단계가 독립적 → 테스트 용이
   - 출력 형식 추가 시 Output만 변경하면 됨

**배운 점**

- Picocli로 CLI 구현하면 코드량이 대폭 줄어듦
- `Callable<Integer>` 사용 시 종료 코드 반환 가능
- 통합 테스트는 개별 단위 테스트보다 실제 동작 검증에 효과적
- Windows 콘솔 코드 페이지는 **같은 프로세스**에서 변경해야 적용됨
- Java에서 `ProcessBuilder`로 `chcp` 실행해도 부모 콘솔에 적용 안 됨
- 배치 파일 래퍼가 Windows 환경 호환성 확보에 효과적

---

#### Session 4: 파라미터 표시 및 순환참조 수정

**오늘 한 일**

1. **파라미터 표시 기능 구현**
   - `ParameterInfo.java`: 파라미터 정보 클래스 생성
   - `ParsedMethod`, `FlowNode`: parameters 필드 추가
   - `JavaSourceParser`: 파라미터 추출 로직 구현
     - `@RequestParam`, `@PathVariable` 어노테이션 값 추출
     - VO 타입: `userVO.getUserId()` → `userId` 필드 추출
     - Map 타입: `params.get("key")` → `key` 추출
   - `ConsoleOutput`: 간결한 파라미터 표시
     - 기본: `Parameters: Long modelId, LocalDate startDate`
     - VO/Map 사용 필드: `└── userVO 사용 필드: userId, name`

2. **Spring 자동 주입 파라미터 필터링**
   - `Model`, `ModelMap`, `HttpServletRequest` 등 제외
   - `Pageable`, `MultipartFile`은 클라이언트 요청이므로 표시

3. **순환참조 오탐 수정**
   - 문제: 같은 메서드를 다른 경로에서 호출하면 `[순환참조]` 표시됨
   - 원인: `visitedMethods`가 전체 분석에서 공유됨
   - 해결: 호출 스택 방식으로 변경 (탐색 완료 후 `remove()`)
   - 결과: 진짜 순환(A→B→A)만 방지, 다른 경로는 정상 표시

**왜 이렇게 구현했는가?**

1. **파라미터 추출 방식**
   - `@RequestParam`: 어노테이션 값이 곧 요청 파라미터명
   - VO getter 분석: 실제 사용하는 필드만 추출 (문서화에 유용)
   - Map.get() 분석: 문자열 리터럴 키만 추출 가능 (상수/동적 키는 한계)

2. **간결한 표시 형식 선택**
   - 파라미터가 많을 때 줄이 너무 길어지는 문제 해결
   - Spring 주입 파라미터는 API 문서에 불필요 → 필터링
   - VO/Map 사용 필드만 상세 표시 (실제 가치 있는 정보)

3. **호출 스택 방식 순환참조 체크**
   - 전역 Set → 호출 스택 Set으로 변경
   - 탐색 완료 후 `remove()`로 스택에서 제거
   - 다른 경로에서 같은 메서드 호출 가능

**배운 점**

- JavaParser `findAll(MethodCallExpr.class)`로 메서드 바디 내 모든 호출 추출 가능
- `StringLiteralExpr`로 문자열 리터럴 값 직접 접근
- 순환참조 체크는 "전체 방문"이 아닌 "현재 경로"로 해야 정확
- Spring MVC 파라미터 중 실제 요청 데이터와 프레임워크 주입을 구분해야 함

---

#### Week 1 완료!

- 2일간 7개 Session 완료
- 다음: Week 2 iBatis/MyBatis 파싱

---

## Week 2

### 2025-12-18 (수)

#### Session 5: IBatisParser 구현 및 DAO-SQL 연결

**오늘 한 일**

1. **SqlInfo 데이터 클래스 생성**
   - 위치: `com.codeflow.parser.SqlInfo`
   - SQL 정보 저장: 파일명, namespace, SQL ID, 타입, 반환타입, 테이블 목록, 쿼리
   - `SqlType` enum: SELECT, INSERT, UPDATE, DELETE, UNKNOWN
   - `getFullSqlId()`: namespace.sqlId 형식 반환

2. **IBatisParser 구현**
   - 위치: `com.codeflow.parser.IBatisParser`
   - JDOM2 기반 XML 파싱
   - iBatis(`sqlMap`) 및 MyBatis(`mapper`) 형식 지원
   - DTD 검증 비활성화 (폐쇄망/오프라인 환경 대응)

3. **SQL 쿼리에서 테이블명 자동 추출**
   - `extractTables()` 메서드 구현
   - FROM, JOIN, INTO, UPDATE 키워드 다음 테이블명 추출
   - 정규식: `(?i)(?:FROM|JOIN|INTO|UPDATE)\\s+([A-Za-z_][A-Za-z0-9_]*)`

4. **DAO 메서드에서 SQL ID 추출**
   - `JavaSourceParser.extractSqlId()` 메서드 추가
   - 지원 패턴: `list()`, `selectList()`, `queryForList()`, `select()`, `selectOne()`, `queryForObject()`, `insert()`, `update()`, `delete()`
   - 문자열 리터럴 첫 번째 인자에서 SQL ID 추출

5. **FlowAnalyzer와 IBatisParser 연동**
   - `sqlInfoMap` 필드 추가 (SQL ID → SqlInfo 매핑)
   - `extractSqlInfo()`: DAO 메서드의 SQL ID로 SqlInfo 조회
   - `FlowNode.sqlInfo` 필드 추가

6. **ConsoleOutput SQL 정보 표시**
   - NORMAL 모드: 파일명 + SQL ID
   - DETAILED 모드: 파일명, namespace, SQL ID, 타입, 반환타입, 테이블 목록

**왜 이렇게 구현했는가?**

1. **DTD 검증 비활성화 이유**
   - 문제: JDOM2가 기본적으로 외부 DTD 로드 시도
   - 증상: 폐쇄망에서 네트워크 타임아웃 발생
   - 해결: `SAXBuilder`에 3가지 feature 비활성화
   ```java
   builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
   builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
   builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
   ```

2. **SQL 정보 표시 모드 분리 이유**
   - 실제 SQL 쿼리는 수백 줄 → 콘솔 출력에 부적합
   - 기본(NORMAL): 파일명 + SQL ID만 표시 (간결)
   - 상세(DETAILED): 테이블, 타입 등 추가 표시 (분석용)
   - 실제 쿼리는 Excel 출력에서만 포함 예정

3. **테이블 자동 추출 이유**
   - 산출물 작성 시 "어떤 테이블을 사용하는지"가 중요 정보
   - 동적 테이블명(`#tableName#`)은 추출 불가 → 한계점
   - 단순 패턴이라도 대부분의 쿼리에서 유용

**배운 점**

- JDOM2에서 외부 엔티티 로드 비활성화하는 방법
- iBatis(`sqlMap`)와 MyBatis(`mapper`) XML 구조 차이
- 정규식으로 SQL에서 테이블명 추출하는 패턴
- 정적 분석의 한계: 동적 SQL, 변수 기반 SQL ID는 추출 불가

---

#### Session 6: ExcelOutput 구현

**오늘 한 일**

1. **ExcelOutput 클래스 구현**
   - 위치: `com.codeflow.output.ExcelOutput`
   - Apache POI (XSSF) 기반 엑셀 출력
   - 3개 시트 구성: 요약, API 목록, 호출 흐름

2. **시트별 구성**
   - **요약 시트**: 프로젝트 경로, 분석 시간, 클래스/엔드포인트 통계
   - **API 목록 시트**: URL, HTTP Method, 파일명, 클래스, 메서드, 파라미터
   - **호출 흐름 시트**: 전체 호출 흐름 (Controller → Service → DAO → SQL)

3. **레이어별 색상 구분**
   - Controller: 녹색 배경
   - Service: 파랑 배경
   - DAO: 보라 배경
   - 시각적으로 레이어 구분 용이

4. **파일 경로 컬럼 추가**
   - FlowNode에 `filePath` 필드 추가
   - FlowAnalyzer에서 ParsedClass.filePath 연결
   - 산출물 작성 시 파일 경로 활용 가능

5. **Main.java 연동**
   - `-o output.xlsx` 옵션으로 엑셀 출력
   - 확장자 기반 자동 판단 (`.xlsx` → 엑셀, 그 외 → 텍스트)

**왜 이렇게 구현했는가?**

1. **3개 시트로 분리한 이유**
   - 요약: 전체 현황 파악 (경영진/PM 보고용)
   - API 목록: 엔드포인트 기준 목록 (API 문서 작성용)
   - 호출 흐름: 상세 분석 결과 (개발자 분석용)

2. **레이어별 색상을 적용한 이유**
   - 엑셀에서 많은 행을 볼 때 레이어 구분이 어려움
   - 색상으로 시각적 구분 → 가독성 향상
   - 산출물 제출 시 깔끔한 형태

3. **자동 필터 추가 이유**
   - 엔드포인트가 많을 때 특정 URL, 레이어 필터링 필요
   - 엑셀 자동 필터로 빠른 검색 가능

**배운 점**

- Apache POI로 엑셀 생성하는 방법 (XSSFWorkbook, Sheet, Row, Cell)
- CellStyle로 셀 스타일링 (배경색, 테두리, 폰트)
- 셀 병합 (CellRangeAddress)
- 자동 필터 설정 (setAutoFilter)

---

---

### 2025-12-19 (목)

#### Session 7: ExcelOutput 개선 및 파라미터 표시 고도화

**오늘 한 일**

1. **ExcelOutput 구조 개선**
   - 기존 계층 구조(└ 들여쓰기) 제거 → 평면 테이블 형식으로 변경
   - 레이어별 컬럼 분리: No, HTTP, URL, 파라미터, Controller, Service, DAO, SQL 파일, SQL ID, 테이블, 쿼리
   - 모든 행에 URL, 파라미터 채우기 (엑셀 필터링 용이)
   - 시트2 (API 목록) 제거 - 자동 추출 불가능한 필드가 많아 불필요

2. **파라미터 표시 로직 개선 (핵심)**
   - 문제: 모든 행에 Controller 파라미터만 표시됨
   - 예: `/user/detail.do` → `UserDAO.selectUser()` (SQL: #userId#) → 파라미터: userId ✅
   - 예: `/user/detail.do` → `DeptDAO.selectDept()` (SQL: #deptId#) → 파라미터: userId, deptId ✅
   - 해결: Controller 파라미터 + SQL 파라미터 합집합으로 표시

3. **SqlInfo SQL 파라미터 추출 기능 추가**
   - iBatis 형식: `#paramName#` 추출
   - MyBatis 형식: `#{paramName}` 또는 `#{obj.property}` 추출
   - 정규식 패턴 매칭으로 쿼리에서 파라미터 자동 추출

4. **기본 저장 경로/파일명 및 중복 처리 구현**
   - 기본 경로: `./output/`
   - 기본 파일명: `code-flow-result.xlsx`
   - 중복 시 자동 번호 추가: `code-flow-result (1).xlsx`, `(2).xlsx` ...
   - 새 CLI 옵션: `--excel`, `-d, --output-dir`

**고민했던 점 - 파라미터 표시 범위**

```
문제 상황:
Controller.getUser(userId, gubun)
├── if(gubun==1) → Service.method1() → DAO1.select1() → SQL: #userId#
└── if(gubun==2) → Service.method2() → DAO1.select2() → SQL: #centerCd#, #deptId#
```

- **SQL 파라미터만?**: 분기 조건 파라미터(gubun) 누락
- **경로상 모든 파라미터?**: 과다 정보, 관련없는 파라미터 포함
- **분기 파라미터 자동 추출?**: 조건문(if/switch) 분석 필요 → 구현 복잡

**결론**: Controller 파라미터 + SQL 파라미터 합집합으로 결정
- 실용적 범위에서 정보 제공
- 분기 파라미터는 향후 과제 (AST 조건문 분석 필요)
- 정적 분석 한계 인정 (죽은 코드, 런타임 분기 판별 불가)

**왜 이렇게 구현했는가?**

1. **평면 테이블 형식 선택 이유**
   - 공공 SI 산출물 양식과 유사
   - 엑셀 필터로 특정 URL, Service, DAO 검색 용이
   - 계층 구조는 콘솔 출력에서 이미 제공

2. **SQL 파라미터 추출 방식**
   - iBatis/MyBatis 모두 지원 (`#param#`, `#{param}`)
   - 정규식으로 간단히 추출 가능
   - `#{obj.property}` 형식은 property만 추출

3. **합집합 방식 선택 이유**
   - API 호출 시 필요한 파라미터 (Controller)
   - SQL 실행 시 필요한 파라미터 (SQL)
   - 둘 다 중요한 정보 → 합쳐서 표시

**배운 점**

- 정적 분석의 한계: 런타임 분기, 죽은 코드 판별 불가
- 실용적 범위 설정의 중요성 - 모든 것을 추출하려 하기보다 핵심 정보에 집중
- 정규식으로 SQL 파라미터 추출하는 패턴
- 사용자 관점에서 "어떤 정보가 필요한가" 고민 필요

---

#### Week 2 완료!

- IBatisParser + ExcelOutput 구현 완료
- ExcelOutput 고도화 (평면 테이블, 파라미터 개선)
- 다음: Week 3 Swing GUI

---

### 2025-12-22 (일)

#### Session 8: ExcelOutput SQL 쿼리 표시 개선

**오늘 한 일**

1. **SQL 목록 시트 컬럼 순서 변경**
   - 변경 전: `No, 호출 URL, SQL 파일, SQL ID, 타입, SQL 파라미터, 테이블, 쿼리`
   - 변경 후: `No, 호출 URL, SQL 파일, SQL ID, 타입, 테이블, SQL 파라미터, 쿼리`
   - 테이블과 SQL 파라미터의 순서가 더 논리적

2. **동적 SQL 태그 원본 형태 출력**
   - 기존: 텍스트만 추출 (동적 태그 정보 누락)
   - 시도: `/* isNotEmpty(categoryId) */` 주석 형식 → 사용자 피드백으로 변경
   - 최종: XML 원본 형태 그대로 출력
   ```xml
   <dynamic prepend="WHERE">
       <isNotEmpty property="categoryId">
           AND CATEGORY_ID = #categoryId#
       </isNotEmpty>
   </dynamic>
   ```

3. **쿼리 표시 개선**
   - 길이 제한 제거 (기존 1000자 → 전체 표시)
   - 자동 줄바꿈 비활성화 (`wrapText: false`)
   - 셀 클릭 시 수식 입력줄에서 전체 쿼리 확인 가능

4. **공통 들여쓰기 제거 로직 개선**
   - 문제: XML 내 8칸 들여쓰기가 그대로 유지됨
   - 해결: `trimPreservingStructure()` 로직 개선
     - 모든 줄에서 공통 들여쓰기 찾기
     - 첫 번째 비어있지 않은 줄부터 마지막까지만 추출
     - 줄바꿈, 상대 들여쓰기는 유지
   - 결과: SELECT부터 시작, 내부 들여쓰기 4칸씩 유지

**왜 이렇게 구현했는가?**

1. **XML 원본 형태 출력 선택 이유**
   - 사용자 요청: "xml에 써져있는 그대로 보이게끔"
   - 동적 SQL 태그와 중첩 구조를 명확히 파악 가능
   - 개발자에게 익숙한 XML 형식

2. **자동 줄바꿈 비활성화 이유**
   - Excel에서 줄바꿈 있는 셀은 높이가 커짐
   - 많은 행을 한눈에 보기 어려움
   - 상세 내용은 셀 클릭으로 수식 입력줄에서 확인

3. **공통 들여쓰기 제거 이유**
   - XML 파일 내 들여쓰기(8칸)가 그대로 출력되면 불필요한 공백
   - 쿼리 첫 줄(SELECT)이 들여쓰기 없이 시작되어야 깔끔

**배운 점**

- Excel 클립보드 동작: 줄바꿈 + 따옴표 있으면 `""` 이스케이프
- JDOM2 `getContent()`: 혼합 콘텐츠(텍스트 + 자식요소) 순서대로 순회
- 정적 분석 출력 형식은 사용자 피드백으로 개선 반복 필요

---

#### Session 9: 블로그 글 작성 및 테스트 샘플 확장

**오늘 한 일**

1. **러너스하이 블로그 1편 작성**
   - 주제: "토스 러너스하이 2기, 시작하며"
   - 내용: 지원 동기, 주제 선정 과정, 기술 스택 선택 이유
   - 결과물 스크린샷 (콘솔 출력, 엑셀 출력) 포함
   - 티스토리 게시: https://kbroj9210.tistory.com/54
   - 블로그 초안/이미지 가이드는 `docs/blog/`에 저장 (git 미추적)

2. **테스트 샘플 코드 확장**
   - 주문(Order) 도메인 추가: Controller, Service, DAO, SQL XML
   - 연관 도메인 추가: Product, Stock, Payment, Delivery
   - 복잡한 호출 흐름 테스트 가능 (다중 DAO, 분기 등)

3. **`.gitignore` 업데이트**
   - `docs/blog/` 폴더 제외 (블로그 초안은 개인 작업물)

**배운 점**

- 결과물 스크린샷이 블로그 신뢰도를 높여줌
- 구체적인 프로젝트명(형사사법기록관)이 추상적 표현보다 설득력 있음
- 복잡한 샘플 코드가 도구 검증에 효과적

---

#### Session 10: README 데모 섹션 추가

**오늘 한 일**

1. **README.md "실행 결과" 섹션 추가**
   - 콘솔 출력 스크린샷 (`assets/console-output.png`)
   - 엑셀 출력 스크린샷 (`assets/excel-output.png`)
   - 출력 스타일 옵션 설명 (접이식 details)
   - CLI 옵션 수정: `-f excel` → `--excel` (실제 CLI와 일치)

2. **assets/ 폴더 생성**
   - 데모 이미지 저장 위치
   - 이미지 캡처 가이드 README 포함

3. **FlowAnalyzer.java 경고 수정**
   - 문제: `switch (clazz.getClassType())`에서 COMPONENT, OTHER case 누락
   - 해결: 누락된 case 추가 (통계에서 제외 처리)
   - IDE 경고 해결

**왜 이렇게 구현했는가?**

1. **README에 데모 이미지 추가 이유**
   - GitHub 방문자가 도구의 실제 결과물을 즉시 확인 가능
   - 텍스트 설명보다 스크린샷이 직관적
   - 오픈소스 프로젝트 신뢰도 향상

2. **switch문 완전성 확보**
   - enum의 모든 case를 명시적으로 처리
   - 향후 enum 값 추가 시 컴파일러가 경고
   - 코드 품질 및 유지보수성 향상

**배운 점**

- README 첫인상이 프로젝트 평가에 중요
- IDE 경고는 무시하지 말고 즉시 해결
- switch문에서 default 대신 모든 case 명시가 더 안전

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
Week 2: iBatis + 출력   ████████████████████ 100%
Week 3: GUI + 테스트    ████████████████████ 100%
Week 4: 개선 + 회고     ██████░░░░░░░░░░░░░░ 30%
```

---

## 핵심 지표

| 지표 | 현재 | 목표 |
|------|------|------|
| 구현된 기능 | 18/18 | 18/18 |
| 테스트 통과율 | 100% | 100% |
| 문서 완성도 | 100% | 100% |
| 코드 커버리지 | - | 80% |

### 완료된 기능
- ✅ 호출 흐름 추적 (Controller → Service → DAO)
- ✅ iBatis/MyBatis XML 파싱
- ✅ 콘솔 출력 (트리 형태 + ANSI 색상)
- ✅ 엑셀 출력 (호출 흐름 + SQL 목록)
- ✅ URL 패턴 필터링
- ✅ 파라미터 표시
- ✅ 인터페이스 → 구현체 매핑
- ✅ SQL 쿼리 원본 형태 출력 (동적 SQL 태그 포함)
- ✅ CLI 통합 (Picocli)
- ✅ Windows 한글 인코딩 지원
- ✅ README 데모 섹션 (스크린샷)
- ✅ 다중 구현체 경고 (콘솔 + 엑셀)
- ✅ Swing GUI (프로젝트 선택, 분석 실행, 결과 트리 표시)
- ✅ GUI 개선 (다크 테마, 콘솔 스타일, 설정 저장)
- ✅ GUI UX 개선 (리사이즈, 레이아웃, 폰트 조절)
- ✅ jpackage Windows 설치파일 (.exe)
- ✅ 애플리케이션 아이콘
- ✅ GitHub Release v1.0.0

### 예정된 기능

---

### 2025-12-23 (월)

#### Session 11: 다중 구현체 경고 기능 구현

**오늘 한 일**

1. **다중 구현체 경고 기능 구현**
   - 문제: 정적 분석 시 인터페이스에 여러 구현체가 있을 경우, 첫 번째 발견된 구현체만 사용
   - 해결: 다중 구현체가 있음을 사용자에게 경고하는 기능 추가

2. **FlowAnalyzer 개선**
   - `multipleImplWarnings` 필드 추가 (인터페이스 → 구현체 목록 Map)
   - `buildInterfaceMapping()` 수정: 모든 구현체 수집 후 2개 이상이면 경고 목록에 추가
   - 경고 정보를 `FlowResult`에 전달

3. **콘솔 출력 경고 표시 (ConsoleOutput)**
   - Service 노드 옆에 인라인으로 경고 표시
   - 형식: `(외 UserServiceV2, UserServiceV3)` (현재 사용 중인 구현체 제외)
   - 노란색으로 강조 표시

4. **엑셀 출력 경고 표시 (ExcelOutput)**
   - 연한 살구색(#FFF0E0)으로 경고 행 강조
   - "비고" 칼럼 추가: `외 UserServiceV2, UserServiceV3`
   - 요약 시트에 `[다중 구현체 경고]` 섹션 추가
     - 경고 설명: "정적 분석의 한계로 첫 번째 구현체 기준으로 분석"
     - 인터페이스별 구현체 목록 표시

5. **테스트 샘플 확장**
   - `UserServiceV2.java`, `UserServiceV3.java` 추가
   - 3개 구현체 시나리오 테스트

**왜 이렇게 구현했는가?**

1. **정적 분석의 근본적 한계 인정**
   - 런타임에 어떤 구현체가 실제로 주입되는지 알 수 없음
   - Spring 설정, 프로파일, 조건부 빈 등 요소가 많음
   - 해결보다 경고하여 사용자가 확인하도록 유도

2. **인라인 경고 표시 선택 이유**
   - 처음에는 상단에 요약 경고 표시 → 직관적이지 않음
   - 해당 Service 노드 옆에 표시 → 어느 서비스가 문제인지 명확

3. **비고 칼럼 + 요약 시트 조합**
   - 비고: 간결하게 어떤 구현체가 더 있는지 표시
   - 요약 시트: 처음 보는 사용자를 위한 상세 설명

**배운 점**

- 정적 분석 도구는 한계를 명확히 알리는 것이 중요
- 경고 위치가 UX에 큰 영향을 미침 (상단 vs 인라인)
- Apache POI에서 XSSFColor로 사용자 정의 색상 설정하는 방법

---

### 2025-12-24 (화)

#### Session 12: Swing GUI 구현

**오늘 한 일**

1. **MainFrame.java 구현**
   - 위치: `com.codeflow.ui.MainFrame`
   - 메인 윈도우 프레임 (1200x800)
   - 시스템 룩앤필 적용

2. **프로젝트 경로 선택 UI**
   - JFileChooser로 폴더 선택
   - 직접 경로 입력도 가능

3. **분석 옵션 UI**
   - URL 필터 입력 필드 (예: `/api/user/*`)
   - 출력 스타일 선택 (normal, compact, detailed)

4. **결과 표시 패널 (ResultPanel.java)**
   - 위치: `com.codeflow.ui.ResultPanel`
   - JTree로 호출 흐름 시각화
   - 레이어별 색상 구분 (Controller: 녹색, Service: 파랑, DAO: 보라)
   - 커스텀 TreeCellRenderer 구현

5. **분석 실행 (SwingWorker)**
   - 백그라운드 스레드에서 분석 실행
   - ProgressBar로 진행 상태 표시
   - UI 블로킹 방지

6. **엑셀 저장 기능**
   - JFileChooser로 저장 위치 선택
   - 분석 결과를 엑셀로 내보내기

7. **Main.java 연동**
   - `--gui` 옵션으로 GUI 모드 실행
   - GUI 모드에서는 `--path` 옵션 불필요

**왜 이렇게 구현했는가?**

1. **SwingWorker 사용 이유**
   - 분석 작업이 오래 걸릴 수 있음
   - 메인 UI 스레드 블로킹 방지
   - 진행 상태 표시 가능

2. **JTree 선택 이유**
   - 호출 흐름이 트리 구조
   - 노드 펼치기/접기 지원
   - 커스텀 렌더러로 색상 적용 용이

3. **레이어별 색상 적용**
   - ConsoleOutput과 동일한 색상 스킴
   - 시각적으로 레이어 구분 용이

**배운 점**

- Swing에서 백그라운드 작업은 SwingWorker 사용
- DefaultTreeCellRenderer를 상속하여 커스텀 렌더링
- JTree 행 높이, 폰트 설정 방법

**트러블슈팅: GUI 한글 깨짐 (Issue #009)**
- 문제: JTree에서 한글이 □로 표시됨
- 원인: `Consolas` 폰트는 한글 글리프 없음
- 해결: `Malgun Gothic` (맑은 고딕)으로 폰트 변경
- 교훈: 다국어 UI에서는 한글 지원 폰트 사용 필수

**트러블슈팅: GUI 텍스트 드래그 선택 불가 (Issue #010)**
- 문제: JTree에서 텍스트 부분 선택 불가, 노드 단위로만 선택됨
- 시도: JTextPane, DefaultCaret 설정 등 → 실패
- 해결: `JEditorPane` + HTML 방식으로 완전히 변경
- 결과: 색상 유지 + 자유로운 텍스트 드래그 선택 가능

**트러블슈팅: GUI 창 닫아도 프로세스 종료 안 됨 (Issue #011)**
- 문제: X 버튼으로 창 닫아도 Java 프로세스가 남아있음
- 원인: SwingWorker 백그라운드 스레드가 JVM 종료 방해
- 해결: `WindowListener`로 `System.exit(0)` 명시적 호출
- 교훈: EXIT_ON_CLOSE만으로는 불충분, 명시적 종료 필요

---

### 2025-12-24 (화) - Session 13

#### Session 13: GUI 개선 (다크 테마 + 콘솔 스타일)

**오늘 한 일**

1. **FlatLaf 다크 테마 적용**
   - `build.gradle`에 FlatLaf 3.2.5 의존성 추가
   - `MainFrame.launch()`에서 `FlatDarculaLaf.setup()` 호출
   - IntelliJ Darcula 스타일의 모던 UI 적용

2. **다크 테마용 색상 수정**
   - 밝은 배경용 색상 → 다크 배경용 색상으로 변경
   - VS Code 터미널 색상 팔레트 참고
   - Controller: #4EC9B0 (청록), Service: #569CD6 (파랑), DAO: #C586C0 (보라)

3. **콘솔 스타일 결과 패널**
   - CLI ConsoleOutput과 동일한 형식으로 출력
   - 헤더 박스, 요약 섹션, 흐름 번호, 트리 구조 적용
   - URL 분리 정보 추가: `(/order + /detail.do)`

4. **설정 저장 기능 구현**
   - Java Preferences API 사용 (OS별 적절한 위치에 저장)
   - JTextField → 편집 가능한 JComboBox로 변경 (드롭다운 선택)
   - 최근 프로젝트 경로 최대 10개 저장
   - URL 필터, 출력 스타일 설정 기억
   - 분석 성공 시 자동으로 설정 저장

**트러블슈팅: HTML 렌더링에서 박스 문자 정렬 불일치 (Issue #012)**
- 문제: CLI 스타일 박스 헤더 `│...│`가 정렬 안 됨
- 원인: HTML monospace 폰트가 박스 문자/한글 폭을 일관되게 렌더링 안 함
- 시도: 한글 폭 계산 (`getDisplayWidth`) → 효과 없음
- 해결: HTML `<table>` + CSS border로 박스 생성
- 교훈: CLI 출력을 그대로 GUI로 옮기는 것은 한계 있음, 환경에 맞는 방식 선택 필요

**왜 이렇게 구현했는가?**

1. **FlatLaf 선택 이유**
   - 한 줄 코드로 모던 UI 적용 가능
   - IntelliJ와 유사한 Darcula 테마 제공
   - 개발자 도구에 적합한 다크 테마

2. **HTML table 사용 이유**
   - 박스 문자 정렬 문제 해결
   - CSS가 테이블 셀 정렬을 보장
   - pre 태그와 혼용 가능

3. **Preferences API 선택 이유**
   - Java 표준 API (추가 의존성 없음)
   - OS별 적절한 위치에 자동 저장 (Windows: Registry, Mac/Linux: 홈)
   - 파일 직접 관리 불필요

**배운 점**
- 터미널 고정폭과 HTML monospace는 동작 방식이 다름
- 다크 테마에서는 색상 대비(contrast) 고려 필수
- CLI 출력을 GUI로 변환 시 환경 특성 고려 필요
- Preferences API로 간단하게 설정 저장 가능 (파일 관리 불필요)

---

### 2025-12-25 (수) - Session 14

#### Session 14: GUI UX 개선 (리사이즈, 레이아웃, 폰트 조절)

**오늘 한 일**

1. **JSplitPane 구현 (URL 목록 패널 리사이즈)**
   - 기존: 고정 폭 좌측 패널
   - 개선: `JSplitPane`으로 드래그하여 크기 조절 가능
   - `mainSplitPane.setDividerLocation(0)`으로 초기 숨김
   - 분석 완료 후 `setDividerLocation(ENDPOINT_PANEL_WIDTH)`로 표시

2. **분석 요약 레이아웃 개선 (Leader Dots)**
   - 문제: 라벨과 개수 사이 간격이 어색함
   - 시도 1: GridLayout → 간격 너무 넓음
   - 시도 2: 가운데 정렬 → 타이틀/구분선과 불일치
   - 최종: Leader dots (점선 리더)로 빈 공간 채우기
   - `paintComponent()` 오버라이드로 커스텀 점 그리기
   - 색상: `(150, 150, 150)` - 다크 테마에서 적절한 밝기

3. **Ctrl+휠 폰트 크기 조절 (ResultPanel)**
   - 결과 패널에서 `MouseWheelListener` 추가
   - Ctrl+휠 위로: 폰트 확대 (+1)
   - Ctrl+휠 아래로: 폰트 축소 (-1)
   - 폰트 크기 범위: 9px ~ 24px (기본 13px)
   - `cachedResult`로 결과 캐싱, 크기 변경 시 즉시 재렌더링

**트러블슈팅: JSplitPane 가시성 문제 (Issue #013)**
- 문제: 분석 후 좌측 패널이 나타나지 않음
- 원인: `setVisible(false/true)` 사용 → JSplitPane 내부 컴포넌트에 효과 없음
- 해결: `setDividerLocation(0)`으로 숨김, `setDividerLocation(width)`로 표시
- 교훈: JSplitPane은 visibility 대신 divider 위치로 제어

**트러블슈팅: 분석 요약 레이아웃 정렬 (Issue #014)**
- 문제: 라벨-개수 간격이 넓거나, 가운데 정렬 시 타이틀과 불일치
- 원인: 다른 섹션은 가로 전체 사용, 분석 요약만 좁은 폭
- 해결: Leader dots (점선 리더)로 빈 공간 시각적 연결
- 교훈: 레이아웃 일관성을 위해 시각적 요소로 공간 채우기

**왜 이렇게 구현했는가?**

1. **JSplitPane 선택 이유**
   - 표준 Swing 컴포넌트로 드래그 리사이즈 기본 지원
   - 사용자가 원하는 크기로 좌우 패널 조절 가능
   - `setContinuousLayout(true)`로 실시간 미리보기

2. **Leader Dots 선택 이유**
   - Word/Excel의 목차 스타일과 유사 (익숙함)
   - 라벨과 값 사이 시각적 연결
   - 빈 공간을 자연스럽게 채움

3. **폰트 크기 조절 UX**
   - 브라우저/IDE와 동일한 조작 방식 (Ctrl+휠)
   - 접근성 향상 (시력에 따라 조절)
   - 즉시 반영으로 빠른 피드백

**배운 점**
- JSplitPane은 setVisible이 아닌 dividerLocation으로 제어
- 커스텀 페인팅으로 간단한 UI 요소(점선) 직접 구현 가능
- Ctrl+휠 이벤트 처리 시 `e.consume()`으로 스크롤 이벤트 차단 필요

---

### 2025-12-25 (수) - Session 15

#### Session 15: jpackage 설치 파일 생성

**오늘 한 일**
1. **버전 관리 체계 수립**
   - `VERSION.md` 생성 (버전 히스토리 관리)
   - Semantic Versioning 규칙 정립
   - v1.0.0 첫 정식 릴리즈 결정

2. **jpackage 설정 구현**
   - build.gradle에 jpackage task 추가
   - 앱 이름: CFT (Code Flow Tracer)
   - 설치 옵션: 경로 선택, 시작 메뉴, 바탕화면 바로가기

3. **WiX Toolset 호환성 문제 해결**
   - Issue #015: jpackage에 WiX Toolset 필요
   - Issue #016: WiX 6.0과 JDK 21 호환성 문제
   - WiX 3.14 설치로 해결

4. **한글 인코딩 문제 해결**
   - jpackage description에 한글 사용 시 WiX 빌드 실패
   - 영문 description으로 변경하여 해결

5. **설치 파일 생성 성공**
   - `CFT-1.0.0.exe` (77.3 MB) 생성
   - JRE 17 번들 포함
   - 폐쇄망에서 Java 없이 실행 가능

6. **exe 실행 문제 해결**
   - Issue #018: exe 더블클릭 시 아무 반응 없음
   - 원인: `--gui` 인자 없이 CLI 모드로 실행 → 즉시 종료
   - 해결: jpackage `--arguments '--gui'` 옵션 추가

7. **설치 UI 커스터마이징**
   - `installer-resources/ShortcutPromptDlg.wxs` 생성
   - 바로가기 체크박스 간격 축소 (Y: 140→180 에서 140→165로 변경)
   - build.gradle에 `--resource-dir` 옵션 추가

8. **현대적 폴더 브라우저 검토**
   - WiX 기본 `BrowseDlg`가 구식 Windows XP 스타일
   - Vista+ 스타일 IFileDialog 적용하려면 C# CustomAction 필요
   - STA 스레드 처리, 커스텀 DLL 빌드 등 복잡도 높음
   - → Backlog로 이동 (MVP 범위 초과)

9. **설치 삭제 시 설정 자동 정리**
   - Java Preferences API가 레지스트리에 설정 저장
   - 저장 위치: `HKCU\Software\JavaSoft\Prefs\com\codeflow\ui`
   - 커스텀 `main.wxs`에 `RemoveRegistryKey` 추가
   - 설치 삭제 시 레지스트리 자동 정리 구현

**기술 선택 이유**
1. **jpackage 선택 이유**
   - JDK 14+에서 기본 제공 (추가 도구 불필요)
   - 네이티브 설치 파일 생성 (.exe, .msi)
   - JRE 번들링으로 Java 설치 불필요

2. **WiX 3.14 선택 이유**
   - JDK 21 (LTS) 환경에서는 WiX 4/5/6 미지원
   - JDK 24+에서만 WiX 4+ 지원
   - 대부분의 개발자가 WiX 3.x 사용 (안정성)

**트러블슈팅 요약**
| 문제 | 원인 | 해결 |
|------|------|------|
| WiX tools not found | WiX 6.0은 candle.exe/light.exe 없음 | WiX 3.14 설치 |
| exit code 311 | description에 한글 포함 | 영문으로 변경 |
| exe 실행 무반응 | --gui 인자 없이 CLI 모드 실행 | --arguments '--gui' 추가 |
| 바로가기 체크박스 간격 | 기본 Y좌표 140→180 (40px) | 140→165 (25px)로 수정 |

**배운 점**
- jpackage는 OS별로 추가 도구 필요 (Windows: WiX)
- WiX 버전과 JDK 버전 간 호환성 매트릭스 존재
- 설치 파일 메타데이터는 ASCII/영문 권장 (인코딩 문제 방지)
- 한글 사용 시 커스텀 로컬라이제이션 파일 필요 (Codepage 949 설정)
- CLI/GUI 겸용 앱 배포 시 기본 실행 인자 설정 필수
- jpackage `--resource-dir`로 WiX 템플릿 커스터마이징 가능
- 현대적 폴더 브라우저(IFileDialog) 적용은 C# CustomAction 필요 (복잡)
- Java Preferences API는 Windows에서 레지스트리에 저장됨 (HKCU)
- Program Files는 쓰기 권한이 없어 설정 파일을 설치 폴더에 저장 불가
- WiX RemoveRegistryKey로 설치 삭제 시 레지스트리 자동 정리 가능

**참고**: 각 이슈 상세 내용은 ISSUES.md (#015~#018) 참조

---

#### Session 16: 문서 일관성 수정 및 아이콘 추가

**오늘 한 일**

1. **문서 전체 검토 및 일관성 수정**
   - 모든 문서 파일 검토 (README, DESIGN, USAGE, TODO 등)
   - Markdown 출력 기능 제거 (폐쇄망 환경에서 불필요)
   - 날짜/상태 최신화 (최종 수정일, Week 3 완료 등)
   - USAGE.md CLI 옵션 테이블 수정 (실제 Main.java와 일치하도록)
   - FAQ jpackage 항목 업데이트 ("향후 지원 예정" → "제공 중")

2. **애플리케이션 아이콘 추가**
   - DALL-E로 아이콘 생성 (노드 흐름 디자인 선택)
   - 3개 컨셉 중 "노드 연결 흐름" 선택 (Controller→Service→DAO 시각화)
   - GUI 윈도우 아이콘 적용 (`MainFrame.java` - `setIconImage()`)
   - jpackage 설치파일 아이콘 적용 (`build.gradle` - `--icon` 옵션)
   - 아이콘 파일: `icon.png` (GUI용), `icon.ico` (설치파일용)

**왜 이렇게 구현했는가?**

1. **Markdown 출력 제거 이유**
   - 원래 목적: GitHub Wiki, Confluence 등에서 공유
   - 실제 환경: 폐쇄망 SI → 외부 플랫폼 접근 불가
   - 엑셀 출력이 인수인계 문서로 충분
   - 불필요한 기능은 유지보수 부담

2. **노드 흐름 아이콘 선택 이유**
   - "Code Flow Tracer"의 핵심 기능을 시각적으로 표현
   - 색상이 GUI 레이어 색상과 일치 (청록=Controller, 파랑=Service, 보라=DAO)
   - 한눈에 "흐름 추적 도구"임을 인식 가능

3. **PNG + ICO 이중 관리 이유**
   - Java Swing: PNG 직접 로드 가능 (`ImageIcon`)
   - Windows jpackage: ICO 형식만 지원
   - 하나의 원본 PNG에서 ICO 변환

**배운 점**
- 문서 일관성 관리는 기능 추가만큼 중요
- 아이콘 하나로 애플리케이션 완성도가 크게 향상
- DALL-E로 개발자 도구 아이콘도 충분히 생성 가능
- jpackage `--icon` 옵션으로 exe 파일, 바로가기 모두 아이콘 적용

---

### 2025-12-26 (목) - Session 17

#### Session 17: GitHub Release v1.0.0 발행

**오늘 한 일**

1. **PR #13 머지 완료**
   - 애플리케이션 아이콘 추가 및 문서 일관성 수정
   - 셀프리뷰 코멘트 작성 후 머지
   - 브랜치 삭제 완료

2. **v1.0.0 릴리즈 빌드**
   - shadowJar 빌드 (code-flow-tracer.jar, ~26MB)
   - jpackage로 Windows 설치파일 생성 (CFT-1.0.0.exe, ~78MB)
   - 빌드 디렉토리 파일 잠금 문제 발생 → 출력 경로 변경으로 해결

3. **GitHub Release 생성**
   - 태그: v1.0.0
   - 제목: v1.0.0
   - 릴리즈 노트 작성 (주요 기능, 다운로드, 사용법, 시스템 요구사항)
   - 첨부 파일: CFT-1.0.0.exe, code-flow-tracer.jar

4. **릴리즈 노트 수정**
   - `@Controller, @Service, @Repository` → GitHub 멘션으로 인식되는 문제
   - @ 기호 제거하여 수정

**트러블슈팅: Gradle clean 시 파일 잠금 (Issue #019)**
- 문제: `./gradlew clean shadowJar` 실행 시 빌드 디렉토리 삭제 실패
- 원인: 이전 jpackage 실행으로 생성된 exe 파일이 다른 프로세스에 의해 잠김
- 해결: clean 없이 shadowJar 실행, jpackage 출력 경로를 `build/release`로 변경

**배운 점**
- Windows에서 exe 파일은 탐색기, 백신 등에 의해 잠길 수 있음
- 빌드 실패 시 출력 경로를 변경하는 것도 유효한 우회 방법
- GitHub 릴리즈 노트에서 `@` 기호는 멘션으로 해석됨 (주의 필요)
- GitHub 릴리즈의 Source code (zip/tar.gz)는 자동 생성되며 삭제 불가

---

### 2025-12-30 (월) - Session 18

#### Session 18: 세션 영속성 구현 (#15)

**오늘 한 일**

1. **GitHub 이슈 생성**
   - #15: 세션 영속성 - 분석 결과 저장/복원
   - #16: 작업 관리 탭 - 내장 칸반 보드
   - 사용자 피드백 기반 Feature Request 형식으로 작성

2. **v1.1 기능 계획 문서화**
   - TODO.md에 v1.1 기능 계획 추가
   - DESIGN.md에 세션 영속성 및 작업 관리 탭 설계 추가
   - PR #17 생성 및 머지

3. **세션 영속성 기능 구현**
   - `SessionData.java`: 세션 데이터 클래스 생성
   - `SessionManager.java`: 세션 저장/로드/삭제 기능 구현
   - `MainFrame.java`: 세션 복원/저장 연동

4. **기술 스택 결정**
   - JSON 직렬화: **Gson** 선택
   - 저장 위치: `~/.code-flow-tracer/session.json`

**왜 Gson을 선택했는가?**

JSON 직렬화 라이브러리 비교:

| 라이브러리 | 장점 | 단점 |
|-----------|------|------|
| Gson | 가볍고 간단, 추가 의존성 없음, 직관적 API | 성능은 Jackson보다 낮음 |
| Jackson | 고성능, 풍부한 기능 | 의존성 복잡, 설정 많음 |
| org.json | JDK 표준 아님 | 객체 매핑 불편 |

**Gson 선택 이유:**
1. **단순성**: 한 줄로 객체 ↔ JSON 변환 (`gson.toJson()`, `gson.fromJson()`)
2. **가벼움**: JAR 크기 약 250KB, 추가 의존성 없음
3. **목적 적합**: 세션 데이터 저장은 성능보다 편의성이 중요
4. **레거시 호환**: Java 8+에서 안정적으로 동작

**왜 ~/.code-flow-tracer/ 경로를 선택했는가?**

저장 위치 후보:

| 위치 | 장점 | 단점 |
|------|------|------|
| 설치 폴더 | 앱과 함께 관리 | Program Files 쓰기 권한 없음 |
| AppData | Windows 표준 | OS별 경로 다름 |
| 홈 디렉토리 | 크로스 플랫폼, 명확함 | 숨김 폴더로 관리 필요 |

**홈 디렉토리 선택 이유:**
1. **크로스 플랫폼**: Windows/Mac/Linux 모두 `user.home` 사용 가능
2. **권한 문제 없음**: 사용자 홈 디렉토리는 쓰기 권한 보장
3. **명확한 위치**: `~/.code-flow-tracer/`로 앱 전용 폴더
4. **기존 패턴**: 많은 CLI 도구가 이 방식 사용 (`.git`, `.npm`, `.gradle`)

**구현 방식**

```java
// SessionManager.java
public class SessionManager {
    private static final Path SESSION_DIR = Paths.get(
        System.getProperty("user.home"), ".code-flow-tracer");
    private static final Path SESSION_FILE = SESSION_DIR.resolve("session.json");

    // Gson + LocalDateTime 어댑터
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
        .create();

    public boolean saveSession(SessionData data) { ... }
    public SessionData loadSession() { ... }
    public boolean clearSession() { ... }
}
```

**동작 흐름**
```
앱 시작
  ↓
restoreSession()
  ↓
세션 파일 존재? ──Yes→ JSON 역직렬화 → UI 복원 → "이전 세션 복원됨" 표시
       │
       No→ 빈 화면으로 시작

분석 실행
  ↓
분석 완료
  ↓
saveSession() → JSON 직렬화 → session.json 저장
```

**LocalDateTime 직렬화 문제 해결**
- 문제: Gson은 `LocalDateTime`을 기본 지원하지 않음
- 해결: 커스텀 `TypeAdapter` 구현
- 형식: ISO-8601 (`2025-12-30T14:30:00`)

```java
private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        out.value(value != null ? FORMATTER.format(value) : null);
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        return LocalDateTime.parse(in.nextString(), FORMATTER);
    }
}
```

**배운 점**

1. **Gson TypeAdapter 사용법**
   - Java 8 Date/Time API는 별도 어댑터 필요
   - `registerTypeAdapter()`로 커스텀 직렬화 등록

2. **세션 복원 시 유효성 검증 중요**
   - 프로젝트 경로가 삭제되었을 수 있음 → `Files.exists()` 체크
   - JSON 파싱 실패 가능 → try-catch로 안전하게 처리

3. **UI 스레드 안전성**
   - 세션 복원 후 UI 업데이트는 `SwingUtilities.invokeLater()` 사용
   - EDT(Event Dispatch Thread)에서만 UI 수정

4. **설정 메뉴 확장**
   - "세션 삭제" 옵션 추가로 사용자가 수동으로 초기화 가능
   - 기존 "설정 초기화"와 분리 (설정 vs 분석 결과)

**고민했던 점**

1. **세션 저장 시점**
   - 선택지: 앱 종료 시 vs 분석 완료 시
   - 결정: **분석 완료 시** 저장
   - 이유: 앱이 비정상 종료되어도 마지막 분석 결과 보존

2. **여러 프로젝트 세션 관리**
   - 선택지: 프로젝트별 세션 vs 단일 세션
   - 결정: **단일 세션** (마지막 분석 결과만)
   - 이유: MVP 범위, 복잡도 증가 방지, 실제 사용 패턴 고려

**참고 자료**
- Gson User Guide: https://github.com/google/gson/blob/main/UserGuide.md
- Java Preferences API: https://docs.oracle.com/javase/8/docs/technotes/guides/preferences/

---

### 2025-12-31 (화) - Session 19

#### Session 19: 세션 영속성 TDD 테스트 및 설정 저장 방식 분석

**오늘 한 일**

1. **PR #18 세션 영속성 코드 리뷰**
   - SessionData.java, SessionManager.java 구조 분석
   - Gson JSON 직렬화 방식 확인
   - 저장 위치: `~/.code-flow-tracer/session.json`

2. **TDD 단위 테스트 작성**
   - `SessionManagerTest.java` 생성 (11개 테스트 케이스)
     - 세션 저장/로드/삭제 기능 테스트
     - FlowNode 트리 구조 직렬화/역직렬화 테스트
     - LocalDateTime 직렬화 테스트
   - `SessionDataTest.java` 생성 (12개 테스트 케이스)
     - 유효성 검증 (isValid) 테스트
     - Getter/Setter 테스트
     - toString 형식 테스트
   - 컴파일 오류 수정:
     - `FlowResult` 생성자 API 수정 (Path 대신 String 사용)
     - `node.setUrl()` → `node.setUrlMapping()` 변경
     - 누락된 `Paths` import 추가
   - 전체 테스트 통과 확인

3. **WiX 설치 파일 세션 폴더 정리 추가**
   - `installer-resources/main.wxs` 수정
   - 설치 삭제 시 `~/.code-flow-tracer/` 폴더 자동 정리 추가
   - `RemoveFile` (session.json) + `RemoveFolder` 설정
   - `SessionCleanup` 컴포넌트 Feature에 추가

4. **설정 저장 방식 분석 및 이중 저장 문제 발견**
   - **현재 상태**: 설정이 2곳에 저장됨
     - Registry (Preferences API): 최근 경로, URL 필터, 출력 스타일
     - JSON 파일: 세션 데이터 + URL 필터, 출력 스타일 (중복!)
   - 설정 저장 방식 비교 분석 (아래 표 참고)
   - 권장: JSON 단일 저장으로 통합

**설정 저장 방식 비교**

| 항목 | Registry (Preferences API) | JSON 파일 |
|------|---------------------------|-----------|
| 플랫폼 | Windows 전용 | 크로스 플랫폼 |
| 저장 위치 | `HKCU\Software\JavaSoft\Prefs\...` | `~/.code-flow-tracer/session.json` |
| 복잡한 객체 | ❌ 문자열/숫자만 | ✅ 객체 직렬화 가능 |
| 백업/이동 | ❌ regedit 필요 | ✅ 파일 복사로 가능 |
| 디버깅 | ❌ 레지스트리 편집기 필요 | ✅ 텍스트 에디터로 확인 |
| 삭제 시 정리 | WiX RemoveRegistryKey 필요 | WiX RemoveFile로 간단 |

**왜 이중 저장이 발생했는가?**

1. **Session 13 (2025-12-24)**: GUI 설정 저장 기능 구현
   - 당시 저장 대상: 최근 경로, URL 필터, 출력 스타일
   - 선택: Java Preferences API (표준 API, 간단한 값 저장에 적합)
   - 저장 위치: Windows Registry

2. **Session 18 (2025-12-30)**: 세션 영속성 구현
   - 당시 저장 대상: FlowResult (복잡한 객체 구조)
   - 선택: Gson JSON 파일 (객체 직렬화 필요)
   - 추가 저장: URL 필터, 출력 스타일도 함께 저장 (중복 발생)

**권장 해결 방향: JSON 단일 저장으로 통합**

```
~/.code-flow-tracer/session.json
{
  "projectPath": "/path/to/project",
  "recentPaths": [...],           // Registry에서 이동
  "urlFilter": "/api/*",          // 이미 있음
  "outputStyle": "normal",        // 이미 있음
  "analyzedAt": "2025-12-31T...",
  "flowResult": { ... }
}
```

**통합 시 장점**:
- 설정 관리 일원화 (한 곳에서 관리)
- 크로스 플랫폼 지원 (Mac/Linux에서도 동일 동작)
- 디버깅 용이 (JSON 파일 직접 확인 가능)
- 설치 삭제 시 정리 간단 (파일 삭제만으로 완료)

**통합 시 주의점**:
- 기존 Registry 설정 마이그레이션 로직 필요 (1회성)
- WiX main.wxs의 Registry 정리 로직 유지 (마이그레이션 전 사용자 대응)

**배운 점**

1. **설계 단계에서 저장 방식 통일 중요**
   - 기능 추가 시 기존 저장 방식과의 일관성 검토 필요
   - 복잡한 객체 저장이 예상되면 처음부터 JSON 선택

2. **TDD 테스트의 가치**
   - 테스트 작성 중 API 사용법 오류 발견 (FlowResult 생성자)
   - 리팩토링 시 안전망 역할

3. **기술 부채 인식**
   - 빠른 구현을 위해 발생한 이중 저장
   - 문서화하여 향후 리팩토링 계획 수립

**다음 할 일**
- ~~JSON 단일 저장으로 통합 리팩토링 (v1.2에서 진행 권장)~~ ✅ Session 20에서 완료
- ~~기존 Registry 설정 마이그레이션 로직 구현~~ ✅ Session 20에서 완료

---

### 2025-12-31 (화) - Session 20

#### Session 20: 기술 부채 청산 - Registry → JSON 단일 저장 통합

> 🎯 **핵심 주제**: 기술 부채를 발견하고, 분석하고, 즉시 해결한 사례

**배경**

Session 19에서 발견한 "설정 이중 저장" 문제를 바로 해결하기로 결정.
원래 v1.2에서 하려던 계획이었지만, "어렵지 않다면 지금 하자"는 판단.

**문제 상황 (Before)**

```
┌─────────────────────────────────────────────────────────────┐
│                    설정 저장 구조 (Before)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │  Registry (Windows) │    │  JSON 파일                   │ │
│  │  HKCU\Software\     │    │  ~/.code-flow-tracer/        │ │
│  │  JavaSoft\Prefs\... │    │  session.json                │ │
│  ├─────────────────────┤    ├─────────────────────────────┤ │
│  │ • 최근 경로          │    │ • 분석 결과 (FlowResult)     │ │
│  │ • URL 필터  ◄────────┼────┼─► URL 필터 (중복!)          │ │
│  │ • 출력 스타일 ◄──────┼────┼─► 출력 스타일 (중복!)       │ │
│  └─────────────────────┘    └─────────────────────────────┘ │
│                                                             │
│  GUI 삭제 메뉴: "설정 초기화" / "세션 삭제" (2개 분리)        │
└─────────────────────────────────────────────────────────────┘

문제점:
- URL 필터, 출력 스타일이 두 곳에 중복 저장
- Registry는 Windows 전용, 다른 OS에서 동작 방식이 다름
- 삭제 메뉴가 2개로 분리되어 사용자 혼란
- 디버깅 어려움 (regedit 필요)
```

**해결 과정**

1. **난이도 판단** (5분)
   - SessionData에 필드 1개 추가
   - SessionManager에 메서드 2개 추가
   - MainFrame에서 Preferences 코드 제거
   - 예상 소요: 30분 → "지금 하자"

2. **구현** (25분)

   ```java
   // 1. SessionData.java - recentPaths 추가
   private List<String> recentPaths;

   public void addRecentPath(String path) {
       if (recentPaths == null) recentPaths = new ArrayList<>();
       recentPaths.remove(path);  // 중복 제거
       recentPaths.add(0, path);  // 맨 앞에 추가
       while (recentPaths.size() > 10) {
           recentPaths.remove(recentPaths.size() - 1);  // 최대 10개 유지
       }
   }

   // 2. SessionManager.java - 설정 전용 메서드
   public SessionData loadSettings() { ... }  // 분석 결과 없어도 로드
   public boolean saveSettings(List<String> paths, String filter, String style) { ... }

   // 3. MainFrame.java - Preferences 완전 제거
   // import java.util.prefs.Preferences; → 삭제
   // private final Preferences prefs = ... → 삭제
   // prefs.get(), prefs.put() → sessionManager.loadSettings(), saveSettings()
   ```

3. **GUI 메뉴 통합**
   - 기존: "설정 초기화" + "세션 삭제"
   - 변경: "설정/세션 초기화" (1개로 통합)

**결과 (After)**

```
┌─────────────────────────────────────────────────────────────┐
│                    설정 저장 구조 (After)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  JSON 파일: ~/.code-flow-tracer/session.json            │ │
│  ├─────────────────────────────────────────────────────────┤ │
│  │  {                                                      │ │
│  │    "projectPath": "/path/to/project",                   │ │
│  │    "recentPaths": ["/path1", "/path2"],  ← 통합됨       │ │
│  │    "urlFilter": "/api/*",                               │ │
│  │    "outputStyle": "normal",                             │ │
│  │    "analyzedAt": "2025-12-31T...",                      │ │
│  │    "flowResult": { ... }                                │ │
│  │  }                                                      │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  GUI 삭제 메뉴: "설정/세션 초기화" (1개로 통합)               │
└─────────────────────────────────────────────────────────────┘

장점:
✅ 단일 저장소 - 관리 용이
✅ 크로스 플랫폼 - Windows/Mac/Linux 동일 동작
✅ 디버깅 용이 - 텍스트 에디터로 확인 가능
✅ 백업 간편 - 파일 복사만으로 완료
✅ GUI 단순화 - 삭제 버튼 1개
```

**코드 변경량**

| 파일 | 추가 | 삭제 | 설명 |
|------|------|------|------|
| SessionData.java | +23 | +0 | recentPaths, hasSettings() |
| SessionManager.java | +35 | +0 | loadSettings(), saveSettings() |
| MainFrame.java | +30 | -40 | Preferences 제거, 메뉴 통합 |
| **합계** | **+88** | **-40** | 순증가 48줄 |

**배운 점 (러너스하이 어필 포인트)**

1. **"나중에" vs "지금"의 판단**
   - 기술 부채를 v1.2로 미루려 했으나, 난이도 평가 결과 30분 작업
   - "어렵지 않으면 지금 하자" → 즉시 해결
   - 미루면 컨텍스트 손실, 다시 파악하는 데 더 오래 걸림

2. **기술 부채 발견 → 분석 → 해결의 사이클**
   ```
   발견 (Session 19) → 문서화 → 난이도 평가 → 즉시 해결 (Session 20)
   │                    │          │              │
   └── 30분 ──────────────────────────────────────┘
   ```

3. **의사결정 문서화의 가치**
   - 문제 상황을 ISSUES.md에 기록하면서 해결책이 명확해짐
   - 저장 방식 비교표 작성 → 자연스럽게 JSON이 정답임을 확인
   - 블로그 포스트로 바로 활용 가능한 자료 축적

4. **리팩토링과 기능 추가의 균형**
   - 새 기능(Session Persistence) 개발 중 발견한 기술 부채
   - 기능 완성 후 바로 리팩토링하여 깔끔한 상태 유지
   - "기능 완성 → 문제 발견 → 즉시 개선" 사이클

**기술 블로그 주제 아이디어**

1. **"Java 앱에서 설정 저장: Registry vs JSON 파일"**
   - Preferences API의 한계
   - Gson을 활용한 JSON 저장의 장점
   - 크로스 플랫폼 고려사항

2. **"기술 부채를 대하는 자세: 미루기 vs 즉시 해결"**
   - 난이도 평가의 중요성
   - 컨텍스트 손실 비용
   - 문서화가 의사결정을 돕는 방법

3. **"레거시 코드에서 발견한 설계 문제 해결기"**
   - 이중 저장 문제 발견 과정
   - 단일 저장소로 통합하는 리팩토링
   - Before/After 비교

**테스트 완료**
- [x] 빌드 성공 (compileJava)
- [x] 전체 테스트 통과 (./gradlew test)
- [x] 문서 업데이트 (ISSUES.md #020 해결, TODO.md, DESIGN.md)

**커밋**
```
refactor: JSON 단일 저장으로 통합 (Registry 제거)

- SessionData: recentPaths 필드 추가
- SessionManager: loadSettings(), saveSettings() 메서드 추가
- MainFrame: Preferences API 완전 제거, SessionManager로 통합
- GUI: 삭제 메뉴 통합 (설정 초기화 + 세션 삭제 → 설정/세션 초기화)
```
