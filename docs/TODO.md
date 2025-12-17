# 할 일 목록 (TODO)

> 최종 수정일: 2025-12-17

## 진행 상태 범례
- ✅ 완료
- 🔄 진행 중
- ⏳ 대기
- ❌ 취소/보류

---

## Week 1: 설계 + 기본 파서 개발

### Day 1 (2025-12-17) ✅
- [x] 프로젝트 기획 및 방향 설정
- [x] Gradle 프로젝트 생성
- [x] build.gradle 의존성 설정
- [x] 기본 패키지 구조 생성
- [x] JavaSourceParser 기본 구현
- [x] 테스트용 샘플 코드 작성
- [x] 테스트 코드 작성 및 통과
- [x] 문서화 구조 설정

### Day 2 (2025-12-17) ✅
- [x] FlowAnalyzer 설계 및 구현
- [x] 클래스 간 호출 관계 연결 로직
- [x] 인터페이스 → 구현체 매핑

### Day 3 ⏳
- [ ] @RequestMapping URL 추출 개선
- [ ] URL 패턴 매칭 로직
- [ ] 분석 결과 데이터 모델 (FlowNode, FlowResult)

### Day 4-5 ⏳
- [ ] 콘솔 출력 구현 (ConsoleOutput)
- [ ] 트리 형태 출력 포맷팅
- [ ] CLI 통합 테스트

---

## Week 2: iBatis 파싱 + 출력

### Day 6-7 ⏳
- [ ] IBatisParser 구현
- [ ] iBatis XML 파싱 (sqlMap 형식)
- [ ] SQL ID → 쿼리 매핑

### Day 8-9 ⏳
- [ ] DAO 메서드 → SQL ID 연결
- [ ] MyBatis XML 지원 (mapper 형식)
- [ ] 전자정부프레임워크 샘플로 테스트

### Day 10 ⏳
- [ ] ExcelOutput 구현 (Apache POI)
- [ ] 엑셀 템플릿 설계
- [ ] 시트 구성 (요약, 상세)

---

## Week 3: GUI + 테스트

### Day 11-13 ⏳
- [ ] Swing GUI 기본 프레임
- [ ] 프로젝트 경로 선택 (JFileChooser)
- [ ] 분석 옵션 UI
- [ ] 결과 표시 패널 (JTree 또는 JTextArea)

### Day 14 ⏳
- [ ] GUI ↔ Core 연동
- [ ] 진행 상태 표시 (ProgressBar)
- [ ] 결과 저장 기능

### Day 15 ⏳
- [ ] egovframe-simple-homepage-template 클론
- [ ] 실제 전자정부 프로젝트로 통합 테스트
- [ ] GitHub 저장소 공개

---

## Week 4: 개선 + 회고

### Day 16-18 ⏳
- [ ] 버그 수정 및 안정화
- [ ] 엣지 케이스 처리
- [ ] 성능 최적화 (대용량 프로젝트)

### Day 19-20 ⏳
- [ ] README 최종 정리
- [ ] 사용 가이드 영상/GIF 제작
- [ ] 전체 과정 회고 작성

### Day 21 ⏳
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

### 품질 개선
- [ ] 로깅 개선 (Logback 설정)
- [ ] 예외 처리 강화
- [ ] 테스트 커버리지 80% 이상

### 배포
- [ ] GitHub Releases
- [ ] Maven Central 배포
- [ ] 도커 이미지

---

## 버그/이슈

| ID | 설명 | 상태 | 해결일 |
|----|------|------|--------|
| #1 | 미사용 import로 인한 컴파일 에러 | ✅ 해결 | 2025-12-17 |

---

## 메모

- MVP 범위를 넘어서는 기능은 Backlog로 이동
- 매일 TODO 업데이트 및 DEV_LOG.md에 기록
- 문제 발생 시 ISSUES.md에 기록
