# 할 일 목록 (TODO)

> 최종 수정일: 2025-12-30

## 진행 상태 범례
- ✅ 완료
- 🔄 진행 중
- ⏳ 대기
- ❌ 취소/보류

---

## Week 1: 설계 + 기본 파서 개발 ✅

### 2025-12-17 (화) ✅
**Session 1: 프로젝트 기획 및 셋업**
- [x] 프로젝트 기획 및 방향 설정
- [x] Gradle 프로젝트 생성
- [x] build.gradle 의존성 설정
- [x] 기본 패키지 구조 생성
- [x] JavaSourceParser 기본 구현
- [x] 테스트용 샘플 코드 작성
- [x] 테스트 코드 작성 및 통과
- [x] 문서화 구조 설정

**Session 2: FlowAnalyzer 핵심 구현**
- [x] FlowAnalyzer 설계 및 구현
- [x] 클래스 간 호출 관계 연결 로직
- [x] 인터페이스 → 구현체 매핑

**Session 3: 인터페이스-구현체 매핑 개선**
- [x] implements 키워드 기반 매핑 추가
- [x] ParsedClass 확장 (isInterface, implementedInterfaces)

### 2025-12-18 (수) ✅
**Session 1: URL 추출 및 패턴 매칭**
- [x] @RequestMapping URL 추출 개선 (클래스+메서드 레벨 조합)
- [x] URL 패턴 매칭 로직 (UrlMatcher 구현)

**Session 2: ConsoleOutput 구현**
- [x] 콘솔 출력 구현 (ConsoleOutput)
- [x] 트리 형태 출력 포맷팅
- [x] ANSI 색상 지원
- [x] 3가지 출력 스타일 (COMPACT, NORMAL, DETAILED)
- [x] Windows 한글 깨짐 문제 해결
- [x] 배포 스크립트 기본 구조 생성

**Session 3: Picocli CLI 통합**
- [x] CLI 통합 (Picocli 연동)
- [x] 통합 테스트

**Session 4: 파라미터 표시 및 순환참조 수정**
- [x] 파라미터 표시 기능 구현
  - [x] ParameterInfo 클래스 생성
  - [x] @RequestParam, @PathVariable 추출
  - [x] VO getter, Map.get() 분석으로 사용 필드 추출
  - [x] 간결한 표시 형식 (한 줄 + VO/Map 상세)
  - [x] Spring 자동 주입 파라미터 필터링 (Model, HttpServletRequest 등)
- [x] 순환참조 오탐 수정 (호출 스택 방식으로 변경)

---

## Week 2: iBatis 파싱 + 출력 ✅

### 2025-12-18 (수) - Session 5 ✅
**IBatisParser 구현**
- [x] SqlInfo 클래스 생성 (파일명, namespace, SQL ID, 타입, 반환타입, 테이블)
- [x] IBatisParser 구현 (JDOM2 기반)
- [x] iBatis XML 파싱 (sqlMap 형식)
- [x] DTD 검증 비활성화 (외부 네트워크 연결 방지)
- [x] SQL 쿼리에서 테이블명 자동 추출

**DAO-SQL 연결**
- [x] JavaSourceParser에서 SQL ID 추출 로직 추가
- [x] DAO 메서드 → SQL ID 연결
- [x] FlowAnalyzer와 IBatisParser 연동
- [x] ConsoleOutput SQL 정보 표시 (기본/상세 모드)
- [ ] MyBatis XML 지원 (mapper 형식) - Backlog로 이동

### ExcelOutput 구현 ✅
- [x] ExcelOutput 구현 (Apache POI)
- [x] 엑셀 템플릿 설계
- [x] 시트 구성 (요약, API 목록, 호출 흐름)
- [x] 파일 경로 컬럼 추가 (산출물용)
- [x] 레이어별 색상 구분 (Controller: 녹색, Service: 파랑, DAO: 보라)

### 2025-12-19 (목) - Session 7 ✅
**ExcelOutput 개선 및 파라미터 고도화**
- [x] 평면 테이블 형식으로 변경 (레이어별 컬럼 분리)
- [x] 시트2 (API 목록) 제거 - 불필요
- [x] 호출 단위 줄무늬 색상 (흰색/연회색)
- [x] SQL 목록 시트 추가
- [x] 파라미터 표시 개선 (Controller + SQL 파라미터 합집합)
- [x] SqlInfo에 SQL 파라미터 추출 기능 추가 (#param#, #{param})
- [x] 기본 저장 경로/파일명 (output/code-flow-result.xlsx)
- [x] 중복 파일명 자동 처리 ((1), (2), ...)
- [x] --excel, -d 옵션 추가

### 2025-12-22 (일) - Session 8 ✅
**ExcelOutput SQL 쿼리 표시 개선**
- [x] SQL 목록 시트 컬럼 순서 변경 (테이블 → SQL 파라미터 → 쿼리)
- [x] 동적 SQL 태그 원본 형태 출력 (XML 그대로)
- [x] 쿼리 길이 제한 제거 (전체 표시)
- [x] 자동 줄바꿈 비활성화 (wrapText: false)
- [x] 공통 들여쓰기 제거 로직 개선

### 2025-12-23 (월) - Session 11 ✅
**다중 구현체 경고 기능**
- [x] FlowAnalyzer에 다중 구현체 감지 로직 추가
- [x] FlowResult에 경고 정보 전달
- [x] 콘솔 출력: Service 노드 옆에 인라인 경고 표시 (외 V2, V3)
- [x] 엑셀 출력: 연한 살구색 강조 + 비고 칼럼 추가
- [x] 엑셀 요약 시트: 다중 구현체 경고 설명 섹션 추가
- [x] 테스트 샘플 추가 (UserServiceV2, UserServiceV3)

---

## Week 3: GUI + 테스트 ✅

### 2025-12-24 (화) - Session 12 ✅
**Swing GUI 구현**
- [x] Swing GUI 기본 프레임 (MainFrame.java)
- [x] 프로젝트 경로 선택 (JFileChooser)
- [x] 분석 옵션 UI (URL 필터, 출력 스타일)
- [x] 결과 표시 패널 (ResultPanel.java - JEditorPane + HTML)
- [x] GUI ↔ Core 연동 (SwingWorker)
- [x] 진행 상태 표시 (ProgressBar)
- [x] 엑셀 저장 기능

**GUI 개선**
- [x] 텍스트 드래그 선택 가능 (JTree → JEditorPane + HTML 변경)
- [x] 색상 구분 유지 (Controller: 녹색, Service: 파랑, DAO: 보라, SQL: 주황)
- [x] 다중 구현체 경고 표시 (빨강 + 굵게)
- [x] 창 닫을 때 프로세스 종료 문제 해결 (WindowListener 추가)
- [x] run.bat 개선 (javaw 사용 → 콘솔 창 없이 GUI만 실행)

### 2025-12-24 (화) - Session 13 ✅
**GUI 개선**
- [x] UI 디자인 개선 (FlatLaf Darcula 다크 테마 적용)
- [x] 다크 테마용 색상 수정 (VS Code 터미널 참고)
- [x] 출력 내용 콘솔 스타일로 변경 (CLI ConsoleOutput 형식 재현)
- [x] 헤더 박스 정렬 문제 해결 (HTML table 사용)
- [x] 프로젝트 경로 기억 기능 (Java Preferences API)
- [x] 최근 경로 드롭다운 (JComboBox, 최대 10개)
- [x] URL 필터/출력 스타일 설정 저장

### 2025-12-25 (수) - Session 14 ✅
**GUI UX 개선**
- [x] JSplitPane으로 좌측 패널 드래그 리사이즈 가능
- [x] 분석 요약 레이아웃 개선 (Leader Dots 점선 리더)
- [x] 결과 패널 Ctrl+휠 폰트 크기 조절 (9px~24px)

### 2025-12-25 (수) - Session 15 ✅
**jpackage 설치 파일 생성**
- [x] VERSION.md 생성 및 버전 관리 체계 수립
- [x] jpackage task 구현 (build.gradle)
- [x] WiX Toolset 호환성 문제 해결 (WiX 3.14)
- [x] exe 실행 시 --gui 인자 자동 전달
- [x] 설치 UI 커스터마이징 (바로가기 체크박스 간격)
- [x] 설치 삭제 시 레지스트리 자동 정리

### 2025-12-25 (수) - Session 16 ✅
**문서 일관성 수정 및 아이콘 추가**
- [x] 전체 문서 검토 및 일관성 수정
- [x] Markdown 출력 기능 관련 내용 제거
- [x] USAGE.md CLI 옵션 테이블 수정
- [x] 애플리케이션 아이콘 추가 (DALL-E 생성)
- [x] GUI 윈도우 아이콘 적용 (MainFrame.java)
- [x] jpackage 설치파일 아이콘 적용 (build.gradle)

### 2025-12-26 (목) - Session 17 ✅
**GitHub Release v1.0.0 발행**
- [x] PR #13 머지 (아이콘 추가, 문서 수정)
- [x] shadowJar 빌드
- [x] jpackage Windows 설치파일 생성
- [x] GitHub Release v1.0.0 생성
- [x] 릴리즈 노트 작성 및 파일 업로드

### 실제 테스트
- [ ] egovframe-simple-homepage-template 클론
- [ ] 실제 전자정부 프로젝트로 통합 테스트
- [ ] GitHub 저장소 공개

---

## Week 4: 기능 확장 + 회고 🔄

### 2025-12-30 (월) - v1.1 기능 확장 🔄
**세션 영속성 ([#15](https://github.com/KBroJ/Code-Flow-Tracer/issues/15))**
- [ ] FlowResult JSON 직렬화/역직렬화
- [ ] 앱 시작 시 마지막 분석 결과 복원
- [ ] 세션 파일 저장 (~/.code-flow-tracer/session.json)

**작업 관리 탭 - Jira 스타일 칸반 ([#16](https://github.com/KBroJ/Code-Flow-Tracer/issues/16))**
- [ ] 작업 관리 탭 UI 추가
- [ ] 칸반 보드 레이아웃 (할 일 / 진행 중 / 완료)
- [ ] 이슈 카드 컴포넌트 (제목, 우선순위, 날짜)
- [ ] 이슈 상세 패널 (제목, 설명, 시작일/마감일, 상태, 우선순위)
- [ ] 이슈 CRUD (추가/수정/삭제)
- [ ] 이슈 번호 자동 부여 (CFT-001, CFT-002...)
- [ ] 상태 변경 (드롭다운)
- [ ] 체크 시 취소선
- [ ] 이슈 데이터 저장/불러오기 (JSON)

### 향후 (v1.2+)
**분기 분석**
- [ ] if/switch 문 조건 추출
- [ ] 분기별 호출 흐름 분리
- [ ] 분기별 파라미터 사용 추적

### 안정화
- [ ] 버그 수정 및 안정화
- [ ] 엣지 케이스 처리
- [ ] 성능 최적화 (대용량 프로젝트)

### 문서화
- [x] README 실행 결과 데모 섹션 추가 (스크린샷)
- [ ] README 최종 정리
- [ ] 사용 가이드 영상/GIF 제작
- [ ] 전체 과정 회고 작성

### 제출
- [ ] 러너스하이 경력기술서 작성
- [ ] 최종 점검 및 제출

---

## Backlog (향후 과제)

### 기능 확장
- [ ] MyBatis 어노테이션 지원 (@Select 등)
- [ ] Spring Data JPA 지원
- [ ] 헥사고날 아키텍처 지원
- [ ] 시각화 (PlantUML, Mermaid 다이어그램)
- [ ] 분기 조건 파라미터 추출 (if/switch 조건식에서 gubun 등 추출)

### 쿼리 튜닝 어드바이저 (Query Tuning Advisor)
> MVP 완료 후 추가 개발 고려. 프로젝트 투입 시 쿼리 튜닝 업무 지원 목적.

**구현 방식**: JSqlParser + 규칙 기반 정적 분석 (LLM 불필요)

- [ ] SQL 안티패턴 감지
  - [ ] `SELECT *` 사용 경고
  - [ ] `WHERE` 절 함수 사용 (인덱스 미사용) 감지
  - [ ] `LIKE '%...'` 패턴 경고
  - [ ] `NOT IN (서브쿼리)` → `NOT EXISTS` 권장
  - [ ] 다수 `OR` 조건 → `IN` 또는 `UNION` 권장
- [ ] 서브쿼리 분석
  - [ ] 스칼라 서브쿼리 → JOIN 변환 권장
  - [ ] 상관 서브쿼리 감지 및 경고
- [ ] 쿼리 복잡도 리포트
  - [ ] 서브쿼리 개수, JOIN 수, 테이블 수 통계
- [ ] (선택) EXPLAIN 결과 분석 - DB 연결 필요

### 품질 개선
- [ ] 로깅 개선 (Logback 설정)
- [ ] 예외 처리 강화
- [ ] 테스트 커버리지 80% 이상

### 배포
- [x] GitHub Releases ✅ 2025-12-26

### 설치파일/패키징
- [x] jpackage로 Windows 설치파일 생성 (.exe) ✅ 2025-12-25
- [x] 설치 UI 바로가기 체크박스 간격 수정 ✅ 2025-12-25
- [x] 설치 삭제 시 설정(레지스트리) 자동 정리 ✅ 2025-12-25
- [x] 애플리케이션 아이콘 추가 (exe, GUI 윈도우) ✅ 2025-12-25
- [ ] 현대적 폴더 브라우저 UI (IFileDialog) - C# CustomAction 필요, 복잡도 높음

---

## 버그/이슈

| ID | 설명 | 상태 | 해결일 |
|----|------|------|--------|
| #1 | 미사용 import로 인한 컴파일 에러 | ✅ 해결 | 2025-12-17 |
| #7 | 순환참조 오탐 (같은 메서드 다른 경로 호출 시 [순환참조] 표시) | ✅ 해결 | 2025-12-18 |
| #9 | GUI 한글 깨짐 (Consolas 폰트 → Malgun Gothic) | ✅ 해결 | 2025-12-24 |
| #10 | GUI 텍스트 드래그 선택 불가 (JTree → JEditorPane + HTML) | ✅ 해결 | 2025-12-24 |
| #11 | GUI 창 닫아도 프로세스 종료 안 됨 (WindowListener 추가) | ✅ 해결 | 2025-12-24 |
| #12 | HTML 박스 문자 정렬 불일치 (HTML table로 해결) | ✅ 해결 | 2025-12-24 |
| #13 | JSplitPane 내부 컴포넌트 가시성 제어 (setDividerLocation 사용) | ✅ 해결 | 2025-12-25 |
| #14 | 분석 요약 레이아웃 정렬 (Leader Dots로 해결) | ✅ 해결 | 2025-12-25 |
| #15 | jpackage WiX Toolset 필요 (WiX 3.14 설치로 해결) | ✅ 해결 | 2025-12-25 |
| #16 | WiX 6.0과 JDK 21 호환성 문제 (WiX 3.14 사용) | ✅ 해결 | 2025-12-25 |
| #17 | jpackage description 한글 인코딩 오류 (영문으로 변경) | ✅ 해결 | 2025-12-25 |
| #18 | jpackage exe 실행 시 아무 반응 없음 (--arguments --gui 추가) | ✅ 해결 | 2025-12-25 |
| #19 | Gradle clean 시 빌드 디렉토리 파일 잠금 (출력 경로 변경으로 우회) | ✅ 해결 | 2025-12-26 |

---

## 메모

- MVP 범위를 넘어서는 기능은 Backlog로 이동
- 매일 TODO 업데이트 및 DEV_LOG.md에 기록
- 문제 발생 시 ISSUES.md에 기록
