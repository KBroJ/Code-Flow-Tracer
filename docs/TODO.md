# 할 일 목록 (TODO)

> 최종 수정일: 2025-12-19

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

---

## Week 3: GUI + 테스트 ⏳

### Swing GUI
- [ ] Swing GUI 기본 프레임
- [ ] 프로젝트 경로 선택 (JFileChooser)
- [ ] 분석 옵션 UI
- [ ] 결과 표시 패널 (JTree 또는 JTextArea)

### GUI 연동
- [ ] GUI ↔ Core 연동
- [ ] 진행 상태 표시 (ProgressBar)
- [ ] 결과 저장 기능

### 실제 테스트
- [ ] egovframe-simple-homepage-template 클론
- [ ] 실제 전자정부 프로젝트로 통합 테스트
- [ ] GitHub 저장소 공개

---

## Week 4: 개선 + 회고 ⏳

### 안정화
- [ ] 버그 수정 및 안정화
- [ ] 엣지 케이스 처리
- [ ] 성능 최적화 (대용량 프로젝트)

### 문서화
- [ ] README 최종 정리
- [ ] 사용 가이드 영상/GIF 제작
- [ ] 전체 과정 회고 작성

### 제출
- [ ] 러너스하이 경력기술서 작성
- [ ] 최종 점검 및 제출

---

## Backlog (향후 과제)

### 기능 확장
- [ ] MarkdownOutput 구현
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
- [ ] GitHub Releases
- [ ] Maven Central 배포
- [ ] 도커 이미지

### 설치파일/패키징
- [ ] jpackage로 Windows 설치파일 생성 (.exe, .msi)
- [ ] jlink로 경량 JRE 번들 생성 (폐쇄망 배포용, ~40MB)
- [ ] 배치 스크립트 포함 배포 패키지 (Portable JDK + JAR)

---

## 버그/이슈

| ID | 설명 | 상태 | 해결일 |
|----|------|------|--------|
| #1 | 미사용 import로 인한 컴파일 에러 | ✅ 해결 | 2025-12-17 |
| #7 | 순환참조 오탐 (같은 메서드 다른 경로 호출 시 [순환참조] 표시) | ✅ 해결 | 2025-12-18 |

---

## 메모

- MVP 범위를 넘어서는 기능은 Backlog로 이동
- 매일 TODO 업데이트 및 DEV_LOG.md에 기록
- 문제 발생 시 ISSUES.md에 기록
