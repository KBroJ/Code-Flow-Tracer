# 진행 상황 (Development Log)

> 토스 러너스하이 2기 프로젝트 개발 일지
> 주별 상세 내용은 [dev-log](./dev-log/) 폴더 참조

---

## 목차

| Week | 기간 | 핵심 주제 | 상세 |
|------|------|----------|------|
| [Week 1](./dev-log/WEEK1.md) | 12/17~12/18 | 프로젝트 기획 및 핵심 기능 | Session 1-4 |
| [Week 2](./dev-log/WEEK2.md) | 12/18~12/22 | iBatis/MyBatis 파싱, Excel 출력 | Session 5-8 |
| [Week 3](./dev-log/WEEK3.md) | 12/22~12/26 | GUI 구현 및 배포 | Session 9-17 |
| [Week 4](./dev-log/WEEK4.md) | 12/30~12/31 | 세션 영속성, 기술 부채 청산 | Session 18-21 |
| Week 5 (현재) | 01/03~ | CRUD 분석 기능, 블로그 | Session 22-29 |

---

## 전체 타임라인

```
Week 1: 설계 + 기본 파서 ████████████████████ 100%
Week 2: iBatis + 출력   ████████████████████ 100%
Week 3: GUI + 테스트    ████████████████████ 100%
Week 4: 세션 영속성     ████████████████████ 100%
Week 5: CRUD 분석       █████████████████░░░ 85%
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
- [x] CRUD 타입별 필터링 (#22) - ✅ 완료 (PR #29)
- [x] 테이블 중심 분석 (#23) - ✅ 완료 (CLI/Excel, PR #29)
- [x] GUI 테이블 영향도 탭 (#30) - ✅ 완료 (Session 29)
- [ ] CRUD 통계 대시보드 (#24)

---

## Week 5 (현재)

### 2026-01-03 (금) - Session 22

#### Session 22: CRUD 분석 기능 요구사항 정의 및 GitHub 이슈 등록

**문제**: 각 플랫폼이 어떤 테이블에 어떤 CRUD를 수행하는지 파악 필요
**해결**: 범용적인 CRUD 분석 기능 3개 도출 및 이슈 등록

**오늘 한 일**
1. 요구사항 브레인스토밍 (Strangler Fig 패턴 등 조사)
2. 범용적 기능 3개 도출 (특화 기능 제외)
3. GitHub 이슈 등록:
   - [#22](https://github.com/KBroJ/Code-Flow-Tracer/issues/22): CRUD 타입별 필터링
   - [#23](https://github.com/KBroJ/Code-Flow-Tracer/issues/23): 테이블 중심 분석
   - [#24](https://github.com/KBroJ/Code-Flow-Tracer/issues/24): CRUD 통계 대시보드

**기술적 검토**
| 기능 | 기존 데이터 활용 | 난이도 |
|------|----------------|--------|
| CRUD 필터링 | SqlInfo.SqlType enum 존재 | 낮음 |
| 테이블 중심 분석 | SqlInfo.tables 존재 | 중간 |
| CRUD 통계 | 기존 데이터 집계 | 낮음 |

**배운 점**
- 범용성 vs 특화: 범용적 기능이 도구 가치를 높임
- Strangler Fig 패턴: 레거시 마이그레이션의 표준 접근법

---

### 2026-01-04 (토) ~ 2026-01-06 (월) - Session 23

#### Session 23: 기술 블로그 4편 및 문서 정리

**문제**: 기술 블로그 시리즈 완성 + DEV_LOG 문서 관리 어려움
**해결**: 블로그 발행 및 DEV_LOG 주별 분할

**오늘 한 일**
1. **기술 블로그 4편(#58) 발행 완료**
   - 제목: "정적 분석의 한계 - 해결할 수 없는 것들"
   - URL: https://kbroj9210.tistory.com/58
   - 핵심: 정적 vs 동적 분석 트레이드오프, 다중 구현체 경고, 순환참조 백트래킹

2. **블로그 제목 일관성 수정**
   - #57: "DAO에서 SQL까지 - XML 파싱으로 연결하기"
   - #58: "정적 분석의 한계 - 해결할 수 없는 것들"
   - 전체 시리즈 대시(-) 형식으로 통일

3. **블로그 이미지/움짤 추가**
   - #57: "But wait, there's more" GIF
   - #58: "모른다" 표정 GIF

4. **전체 블로그 시리즈 검토 (#54~#58)**
   - 제목 일관성, 이미지, 톤, 구조 확인 완료

5. **DEV_LOG 주별 분할 완료**
   - 문제: DEV_LOG.md가 31,000+ 토큰으로 읽기 어려움
   - 해결: 주별 파일로 분할 (WEEK1~4.md)
   - 형식: "문제 → 해결" 중심 구조로 재구성
   - 결과: 목차 + 현재 주(Week 5)만 DEV_LOG.md에 유지

**배운 점**
1. **블로그 시리즈 스타일 일관성**
   - 개별 글이 좋아도 시리즈로 볼 때 일관성 없으면 어색함
   - 구조(섹션 수), 톤(비유 사용), 형식(제목 스타일) 통일 필요

2. **문서화된 개발 로그의 활용**
   - DEV_LOG, ISSUES에 기록해둔 내용이 블로그 자료로 바로 활용
   - 개발 당시 고민했던 내용을 정확히 재현 가능

3. **문서 분할의 효과**
   - 주별 분할로 특정 시점 작업 내용 빠르게 탐색 가능
   - "문제 → 해결" 구조가 면접 대비에도 유용

**다음 할 일**
- [ ] CRUD 필터링 기능 구현 (#22)
- [ ] 러너스하이 마무리 회고 글 작성

---

### 2026-01-07 (화) - Session 24

#### Session 24: 기술 블로그 5편 초안 작성 (CLI 출력 구현기)

**문제**: 기술 블로그 시리즈에서 CLI 출력 관련 글이 부족
**해결**: CLI/GUI 개념부터 Picocli 사용법까지 초보자 친화적 초안 작성

**오늘 한 일**
1. **기술5 CLI 블로그 초안 작성** (`docs/blog/초안/06-분석결과를어떻게보여줄까-CLI출력구현기.md`)
   - 제목: "분석 결과를 어떻게 보여줄까? - CLI 출력 구현기"
   - 비유: "메모장 vs VS Code" (터미널용 신택스 하이라이팅)

2. **CLI/GUI 개념 설명 추가**
   - CLI vs GUI 비교표
   - Java에서 CLI 만드는 방법 3가지 (직접 파싱, Commons CLI, Picocli)
   - 왜 Picocli를 선택했는가? 6가지 기준 비교

3. **Picocli 튜토리얼 추가**
   - 개념 설명 (pico = 10^-12)
   - 의존성 추가 → 기본 구조 → 실행 예시
   - @Command, @Option 어노테이션 설명

4. **기존 블로그 스타일 분석 및 반영**
   - 티스토리 #55~#58 분석
   - 비유 추가 (기존 글에 비해 비유 부족 → 개선)
   - 3줄 요약 위아래 구분선 추가

5. **블로그 검토 및 개선**
   - 시리즈 연결: 박스 형태 → 자연스러운 문장으로 수정
   - 비유 확장: 박스 문자, ANSI 색상, Windows 인코딩 섹션에 비유 연결
   - "이 글을 쓰며 성장한 점" 섹션 추가
   - 면접 질문/참고 자료 섹션 삭제 (블로그에 불필요)

6. **티스토리 발행 완료**: [#59](https://kbroj9210.tistory.com/59)

**블로그 최종 구조 (10개 섹션)**
| 섹션 | 내용 |
|------|------|
| 1. 들어가며 | 시리즈 연결, 비유, CLI 먼저 만든 이유 |
| 2. CLI란 무엇인가? | CLI vs GUI, Java CLI 구현 방법 |
| 3. Picocli 사용법 | 개념, 의존성, 기본 구조 |
| 4. 프로젝트에 적용하기 | 직접 파싱 → Picocli 적용 |
| 5. 박스 문자 | `├── └── │` 트리 구조 (비유 연결) |
| 6. ANSI 색상 | 이스케이프 시퀀스, 레이어별 색상 (비유 연결) |
| 7. Windows 인코딩 | CP949 vs UTF-8, 4가지 해결법 (비유 연결) |
| 8. 한글 폭 계산 | UnicodeBlock으로 CJK 판별 |
| 9. 프로젝트에서 활용 | 출력 스타일, 다중 구현체 경고 |
| 10. 마치며 | 정리표, 삽질 포인트, 성장한 점 |

**배운 점**
1. **초보자 친화적 글쓰기**: 개념 설명 → 선택지 비교 → 튜토리얼 순서가 효과적
2. **비유의 힘**: "메모장 vs VS Code"가 "터미널 출력 포맷팅"보다 이해하기 쉬움
3. **러너스하이 취지**: "왜 이 기술을 선택했는가?" 질문에 답할 수 있어야 함
4. **블로그 스타일 일관성**: 기존 글(#55~#58)과 시리즈 연결, 비유 사용 방식 통일

---

### 2026-01-10 (금) ~ 2026-01-11 (토) - Session 25

#### Session 25: 기술 블로그 6편 작성 (Excel 출력 - Apache POI 활용기)

**문제**: 기술 블로그 시리즈에서 Excel 출력 관련 글이 부족
**해결**: 산출물 작성 참고용 레퍼런스 관점으로 초안 작성 및 발행

**오늘 한 일**
1. **기술6 Excel 블로그 초안 작성** (`docs/blog/초안/07-Excel출력-ApachePOI로결과정리.md`)
   - 제목: "분석 결과를 Excel로 정리하기 - Apache POI 활용기"
   - 비유: "도서관에서 매번 책 찾기 vs 정리된 색인집", "족보 vs 명단"

2. **사용자 피드백 반영 - 동기 부분 수정**
   - 기존: "PM 보고용 Excel" (잘못된 방향)
   - 수정: "산출물 작성 시 복사/붙여넣기, 필터링으로 참고할 레퍼런스"
   - 추가: DBMS 이관 프로젝트 → 양방향 동기화 → CRUD 분석 필요

3. **타겟 독자 수정**
   - 기존: "누가 이 문서를 볼 것인가?" (PM, 관리자 등)
   - 수정: "개발자가 언제 이 Excel을 쓸까?" (산출물 작성 시 참고)

4. **실제 구현과 맞게 컬럼 구조 수정**
   - 호출 흐름 시트: No, HTTP, URL, Controller 파일/메소드, Service 파일, ServiceImpl 파일/메소드, DAO 파일/메소드, SQL 파일, 비고
   - SQL 목록 시트: No, 호출 URL, SQL 파일, SQL ID, 타입, 테이블, SQL 파라미터, 쿼리

5. **Excel 출력 실행 및 스크린샷 촬영**
   - 샘플 프로젝트로 Excel 생성 (`blog-excel-demo.xlsx`)
   - 호출 흐름 시트, SQL 목록 시트 스크린샷 촬영

6. **티스토리 발행 및 검토 완료**: [#60](https://kbroj9210.tistory.com/60)
   - PC/모바일 렌더링 확인: 스크린샷, 표, 코드 블록 모두 정상

**블로그 최종 구조 (11개 섹션)**
| 섹션 | 내용 |
|------|------|
| 1. 들어가며 | 시리즈 연결, 산출물 수작업 문제, CRUD 분석 필요, 왜 Excel |
| 2. Apache POI란? | 정의, 선택 이유, 의존성 |
| 3. Apache POI 기본 구조 | Workbook → Sheet → Row → Cell, 스타일 |
| 4. 시트 구성 전략 | 3개 시트 (요약/호출흐름/SQL) |
| 5. 계층 구조 → 평면 테이블 | 필터링 편의, 평면화 로직 |
| 6. 레이어별 색상 구분 | Controller=녹색, Service=파랑, DAO=보라 |
| 7. 파라미터 표시 문제 | Controller + SQL 합집합 |
| 8. 동적 SQL 원본 출력 | XML 태그 유지 |
| 9. 자동 필터와 컬럼 너비 | setAutoFilter, autoSizeColumn |
| 10. 프로젝트에서 활용 | 실제 스크린샷 |
| 11. 마치며 | 정리표, 삽질 포인트, 다음 글 예고 |

**배운 점**
1. **실무 경험 기반 동기**: "PM 보고용"보다 "내가 산출물 쓸 때 참고"가 진정성 있음
2. **타겟 독자 명확화**: 누가 볼지보다 "언제 쓸지"가 더 명확한 기준
3. **블로그 vs 실제 구현 일치**: 초안 작성 시 실제 코드 확인 필수
4. **스크린샷의 힘**: 표보다 실제 Excel 스크린샷이 훨씬 설득력 있음

**실무 피드백 - 테이블 중심 역추적 기능 (#23)**

프로젝트 투입 후 동일한 요구사항 확인:
> "하나의 테이블에 CRUD하는 곳이 어디있는지 찾는 기능이 필요하다"

- **현재**: API URL → DAO → SQL (Top-Down)
- **요청**: 테이블 → 어떤 코드가 접근하는가? (Bottom-Up)

**사용 시나리오**:
- DBMS 이관 프로젝트에서 양방향 동기화 대상 테이블 선정
- 특정 테이블 스키마 변경 시 영향 범위 파악
- 데이터 이상 발생 시 원인 코드 추적

**구현 가능성**: ✅ 가능
- 기존 `SqlInfo.tables`와 `SqlInfo.SqlType` 활용
- 역방향 인덱싱(`Map<테이블명, List<접근정보>>`) 구축 필요
- GitHub 이슈 #23에 실무 피드백 코멘트 추가 완료

---

### 2026-01-11 (토) ~ 2026-01-12 (일) - Session 26

#### Session 26: 기술 블로그 7편 작성 및 발행 (Swing으로 모던한 GUI 만들기)

**문제**: 기술 블로그 시리즈에서 GUI 구현 관련 글이 부족
**해결**: FlatLaf, SwingWorker, JEditorPane 활용법 초안 작성 및 티스토리 발행

**오늘 한 일**
1. **기술7 GUI 블로그 초안 작성** (`docs/blog/초안/08-Swing으로모던한GUI만들기.md`)
   - 제목: "Swing으로 모던한 GUI 만들기 - CLI를 넘어서"
   - 핵심: FlatLaf 한 줄로 다크 테마, SwingWorker로 UI 블로킹 방지

2. **기술 선택 근거 정리**
   - JavaFX vs Swing vs Electron 비교표
   - Swing 선택 이유: JDK 내장, jpackage 호환, 폐쇄망 친화적

3. **핵심 구현 내용 정리**
   - FlatLaf 다크 테마 적용 (`FlatDarculaLaf.setup()`)
   - SwingWorker로 백그라운드 분석 (doInBackground, publish, process, done)
   - JEditorPane + HTML로 색상 + 텍스트 선택 지원
   - JSplitPane으로 리사이즈 가능한 패널

4. **트러블슈팅 모음 정리**
   - Issue #009: JTree 한글 깨짐 → 맑은 고딕 폰트
   - Issue #010: 텍스트 선택 불가 → JEditorPane
   - Issue #011: 창 닫아도 프로세스 안 종료 → WindowListener
   - Issue #012: HTML 박스 문자 정렬 불일치 → `<table>` + CSS
   - Issue #013: JSplitPane 패널 안 보임 → setDividerLocation()
   - Issue #014: 분석 요약 정렬 어색 → Leader dots

5. **Ctrl+휠 폰트 크기 조절 기능 설명 추가**
   - e.consume()으로 스크롤 이벤트 차단 중요성

6. **기존 블로그 스타일 일관성 검토 및 수정**
   - 문체 통일: "~습니다" → "~다" 체로 전체 수정
   - 들어가며: 이전 글 링크 추가, 비유("리모컨") 추가
   - SwingWorker: 식당 비유(홀 직원/주방 직원) 추가
   - 마치며: 삽질 포인트, 성장한 점, 다음 글 예고 추가

7. **티스토리 발행 완료**: [#61](https://kbroj9210.tistory.com/61)
   - PC/모바일 렌더링 확인
   - 표, 코드 블록 정상 표시 확인

**블로그 구조 (8개 섹션)**
| 섹션 | 내용 |
|------|------|
| 1. 들어가며 | 왜 GUI? 기술 선택 근거 |
| 2. FlatLaf | 한 줄로 모던 UI |
| 3. SwingWorker | UI 블로킹 방지 |
| 4. JEditorPane + HTML | 색상 + 텍스트 선택 |
| 5. JSplitPane | 리사이즈 가능한 패널 |
| 6. 트러블슈팅 모음 | Issues #009-#014 |
| 7. Ctrl+휠 폰트 조절 | 사용자 편의 기능 |
| 8. 마치며 | 핵심 정리, 회고 |

**배운 점**
1. **Swing의 재발견**: FlatLaf로 촌스러운 UI 문제 해결 가능
2. **EDT 스레드 모델**: SwingWorker의 각 메서드가 어느 스레드에서 실행되는지 이해
3. **문서화의 중요성**: JSplitPane visibility 이슈는 공식 문서 확인으로 해결

**다음 할 일**
- [x] CRUD 필터링 기능 구현 (#22)
- [x] 테이블 중심 분석 기능 구현 (#23) ← 실무 피드백 반영
- [ ] 러너스하이 마무리 회고 글 작성
- [x] ~~기술7 GUI 블로그 티스토리 발행~~ → #61 발행 완료

---

### 2026-01-12 (일) - Session 27

#### Session 27: CRUD 필터링 (#22) 및 테이블 중심 분석 (#23) 구현

**문제**: 실무에서 CRUD 타입별 필터링 및 테이블 역추적 기능 필요
**해결**: #22, #23 이슈 기능 구현 완료

**오늘 한 일**
1. **#22 CRUD 타입별 필터링 구현**
   - CLI: `--sql-type SELECT,INSERT,UPDATE,DELETE` 옵션 추가
   - FlowAnalyzer: `filterBySqlType()` 메서드 추가
   - GUI: SQL 타입 체크박스 (SELECT/INSERT/UPDATE/DELETE) 추가
   - 엑셀: 호출 흐름 시트에 CRUD 타입 컬럼 추가
   - 세션: CRUD 필터 상태 저장/복원

2. **#23 테이블 중심 분석 구현**
   - FlowAnalyzer: 테이블 역방향 인덱싱 (`buildTableIndex()`, `TableImpact`, `TableAccess`)
   - CLI: `--table TB_USER` (특정 테이블 접근 흐름만 표시)
   - CLI: `--list-tables` (테이블 목록 + CRUD 통계 출력)
   - 엑셀: "테이블 영향도" 시트 추가 (테이블별 접근 횟수, CRUD 통계, URL/SQL ID)

3. **FlowNode에 유틸리티 메서드 추가**
   - `copy()`: 노드 복사 (자식 제외)
   - `clearChildren()`: 자식 노드 초기화

**구현 상세**

| 기능 | 구현 위치 | 내용 |
|------|----------|------|
| `--sql-type` | Main.java:77-78 | Picocli 옵션 (콤마 split) |
| `filterBySqlType()` | FlowAnalyzer.java:122-212 | SQL 타입 필터링 로직 |
| CRUD 체크박스 | MainFrame.java:79-83, 181-189 | SELECT/INSERT/UPDATE/DELETE |
| CRUD 컬럼 | ExcelOutput.java:239, 275, 362-364 | 호출 흐름 시트 |
| `buildTableIndex()` | FlowAnalyzer.java:628-644 | 테이블 역방향 인덱싱 |
| `--table` | Main.java:80-81 | 테이블 필터링 옵션 |
| `--list-tables` | Main.java:83-84, 328-384 | 테이블 목록 출력 |
| 테이블 영향도 시트 | ExcelOutput.java:489-580 | 테이블별 접근 정보 |

**배운 점**
1. **기존 데이터 활용**: SqlInfo.SqlType, SqlInfo.tables가 이미 존재하여 새로운 파싱 없이 구현 가능
2. **역방향 인덱싱**: `Map<테이블명, List<접근정보>>` 구조로 빠른 조회 가능
3. **FlowNode 복사**: 필터링 시 원본 수정 방지를 위해 copy() 메서드 필요

**다음 할 일**
- [ ] CRUD 필터 실시간 적용 (#025)
- [ ] GUI 테이블 영향도 탭 추가 (복잡한 UI 변경으로 추후 진행)
- [ ] CRUD 통계 대시보드 (#24)
- [ ] 러너스하이 마무리 회고 글 작성

---

### 2026-01-12 (일) - Session 28

#### Session 28: CRUD 필터 실시간 적용 구현

**문제**: GUI에서 CRUD 체크박스 변경 시 재분석 필요 (Issue #025)
**해결**: 원본 데이터 저장 + UI 레이어 필터링으로 실시간 반영

**문제 분석**

현재 구현의 문제점:
```java
// startAnalysis() 내부 - 분석 시점에 필터 적용
if (sqlTypeFilter != null && !sqlTypeFilter.isEmpty()) {
    result = analyzer.filterBySqlType(result, sqlTypeFilter);
}
currentResult = result;  // ← 필터링된 결과만 저장
```
- 원본 데이터가 없어서 필터 변경 시 재분석 필요
- 엔드포인트 검색은 실시간인데 CRUD는 아님 → UX 불일치

**대안 비교**

| 방식 | 장점 | 단점 | 선택 |
|------|------|------|------|
| 원본+필터 이중 저장 | 빠름 | 메모리 2배, 동기화 복잡 | ❌ |
| 재분석 (현재) | 간단 | 느림, UX 불편 | ❌ |
| **원본 저장 + UI 필터링** | 빠름, 확장 가능 | 필터 로직 UI 위치 | ✅ |

**기술적 결정 이유**
1. 엔드포인트 검색과 동일 패턴 → 일관성
2. 분석 1회 + 필터 즉시 적용 → 반응성
3. 테이블 필터 추가 시에도 재사용 가능 → 확장성

**구현 완료**

1. `originalResult` 필드 추가 (필터 없는 원본)
2. CRUD 체크박스에 ActionListener 추가
3. `applyFiltersAndRefresh()` 메서드 추가
4. 세션 저장/복원 시 원본 데이터 사용

**변경 파일**

1. `MainFrame.java` - 실시간 필터링 UI
   - Line 98: `originalResult` 필드 추가
   - Line 632-636: CRUD 체크박스 ActionListener
   - Line 1157-1192: `applyFiltersAndRefresh()` 메서드
   - Line 1055-1074: `saveSession()` - 원본 저장
   - Line 1076-1139: `restoreSession()` - 원본 복원 + 필터 적용

2. `FlowAnalyzer.java` - 필터링 버그 수정
   - Line 197-198: Controller 예외 처리 제거
   - 버그: Controller 노드가 자식 없어도 항상 포함됨
   - 수정: 모든 노드는 필터에 맞는 자식이 있어야만 포함

**배운 점**
1. **데이터와 뷰 분리**: 원본 데이터 보존으로 필터 전환 가능
2. **일관된 패턴**: 엔드포인트 검색과 동일한 실시간 필터링 패턴
3. **UX 개선**: 재분석 없이 즉시 필터링 → 반응성 향상
4. **재귀 필터링 주의**: 트리 구조 필터링 시 루트 노드 예외 처리 버그 주의

**PR 생성 및 머지**
- PR #29 생성: "feat: CRUD 타입별 필터링 및 테이블 중심 분석 구현"
- 연결 이슈: Closes #22, Closes #23, Closes #25
- 셀프 리뷰 후 머지 완료
- 브랜치 삭제: `feature/22-23-25-crud-table-analysis`

**신규 이슈 생성**
- [#30](https://github.com/KBroJ/Code-Flow-Tracer/issues/30): GUI 테이블 영향도 탭 추가 (#23에서 분리)

**다음 할 일**
- [x] GUI 테이블 영향도 탭 추가 (#30) ← Session 29에서 구현

---

### 2026-01-12 (일) - Session 29

#### Session 29: GUI 테이블 영향도 탭 구현 (#30)

**문제**: #23 테이블 중심 분석이 CLI/Excel만 지원, GUI에서 테이블 역추적 불가
**해결**: 탭 전환 시 왼쪽 패널 변경 + 브레드크럼 드릴다운 UI 구현

**사용자 요구사항**
1. 테이블 영향도 탭 선택 시 왼쪽 패널이 "테이블 목록"으로 변경
2. 테이블 선택 시 접근 정보 표시 (CRUD/URL/XML파일/SQL ID)
3. 테이블 더블클릭 시 해당 테이블의 모든 쿼리 표시
4. 브레드크럼으로 현재 위치 표시 및 뒤로가기

**구현 완료**

1. **MainFrame.java - CardLayout 적용**
   - `leftCardPanel` + `leftCardLayout` 추가 (엔드포인트/테이블 전환)
   - `tableListPanel` 생성 (검색 + 테이블 리스트)
   - 탭 전환 리스너 (`resultTabbedPane.addChangeListener`)
   - 테이블 목록 클릭/더블클릭 이벤트 핸들러
   - `updateTableList()`, `filterTableList()` 메서드 추가

2. **TableImpactPanel.java - 완전 재구성**
   - 기존: 왼쪽(테이블 목록) + 오른쪽(접근 정보)
   - 변경: 가운데 영역만 담당 (왼쪽은 MainFrame에서 관리)
   - 브레드크럼 UI: `테이블명` → `테이블명 > 쿼리`
   - CardLayout: 접근 정보 테이블 ↔ 쿼리 상세 뷰 전환
   - 테이블명 클릭 → 접근 정보 테이블로 돌아가기

**UI 구조**

```
┌──────────────┬─────────────────────────────────────────┬──────────────┐
│ 🔍 검색      │  TB_CUSTOMER > 쿼리                      │              │
│ 15개 테이블  │  ┌─────────────────────────────────────┐│   설정 패널   │
│              │  │ /* [SELECT] selectOrder */         ││              │
│ DUAL         │  │ SELECT * FROM ORDER_TB             ││              │
│ TB_CUSTOMER  │  │ WHERE ORDER_ID = #{orderId}        ││              │
│ TB_DELIVERY  │  │ ─────────────────────────────────  ││              │
│ ...          │  │ /* [INSERT] insertOrder */         ││              │
│              │  │ INSERT INTO ORDER_TB ...           ││              │
│              │  └─────────────────────────────────────┘│              │
└──────────────┴─────────────────────────────────────────┴──────────────┘
```

**주요 변경 파일**

| 파일 | 변경 내용 |
|------|----------|
| `MainFrame.java` | CardLayout, 탭 전환, 테이블 목록 관리 |
| `TableImpactPanel.java` | 브레드크럼 + CardLayout 완전 재구성 |

**배운 점**
1. **CardLayout 활용**: 탭에 따라 왼쪽 패널 내용만 교체하는 깔끔한 패턴
2. **일관된 UX**: 호출 흐름 탭과 동일한 3단 레이아웃 유지
3. **브레드크럼 패턴**: 드릴다운 네비게이션의 표준 UI 패턴
4. **이벤트 분리**: 단일 클릭(정보 표시) vs 더블클릭(상세 뷰)

---

#### Session 29 (계속): 세션 저장/복원 기능 및 버그 수정

**사용자 피드백 및 해결**

1. **"전체" 옵션 누락 및 SQL 필터 로직 오류**
   - 문제: 테이블 목록에 "전체" 옵션이 사라짐, SQL 타입 필터 전체 해제 시 모든 결과 표시
   - 해결: `ALL_TABLES` 상수 추가, 빈 필터 → 빈 결과 반환 로직 수정

2. **세션 저장/복원 미동작**
   - 문제: 앱 종료 시 탭/테이블 선택 상태가 저장되지 않음
   - 원인: `saveSession()`이 분석 완료 후에만 호출됨
   - 해결: `windowClosing` 이벤트에서 `saveSession()` 호출 추가

3. **호출흐름 탭 엔드포인트 선택 복원 안 됨**
   - 문제: 엔드포인트 선택은 되지만 상세화면 스크롤이 안 됨
   - 원인: UI 렌더링 완료 전 `scrollToEndpoint()` 호출
   - 해결: `SwingUtilities.invokeLater()` 중첩으로 렌더링 후 스크롤

4. **테이블 영향도 특정 쿼리 복원 안 됨**
   - 문제: 더블클릭한 특정 쿼리가 아닌 전체 쿼리 목록으로 복원
   - 해결: `selectedQueryRowIndex` 필드 추가, `restoreQueryView()` 메서드 구현

5. **테이블 "전체" 선택 시 상세화면 빈 화면**
   - 문제: 분석 후 테이블 영향도 탭에서 "전체" 선택되어 있지만 상세화면 비어있음
   - 원인: `updateTableList()`가 `tableImpactPanel.updateData()` 보다 먼저 호출됨
   - 해결: 호출 순서 변경 - `updateData()` 먼저, `updateTableList()` 나중에

**구현 상세 - 세션 저장/복원**

| 필드 | 설명 | 저장 시점 |
|------|------|----------|
| `selectedTabIndex` | 선택된 탭 (0: 호출흐름, 1: 테이블영향도) | 앱 종료 시 |
| `selectedEndpoint` | 호출흐름 탭에서 선택한 엔드포인트 URL | 앱 종료 시 |
| `selectedTable` | 테이블 영향도 탭에서 선택한 테이블명 | 앱 종료 시 |
| `tableDetailViewActive` | 쿼리 상세 화면 활성화 여부 | 앱 종료 시 |
| `selectedQueryRowIndex` | 선택한 쿼리 행 인덱스 (-1: 전체) | 앱 종료 시 |

**변경 파일**

| 파일 | 변경 내용 |
|------|----------|
| `SessionData.java` | 5개 필드 추가 (selectedTabIndex, selectedEndpoint, selectedTable, tableDetailViewActive, selectedQueryRowIndex) |
| `SessionManager.java` | saveSession() 오버로드 확장 |
| `TableImpactPanel.java` | isQueryDetailViewActive(), getSelectedQueryRowIndex(), restoreQueryView() 메서드 추가 |
| `MainFrame.java` | windowClosing에서 saveSession() 호출, 호출 순서 수정 (updateData → updateTableList) |

**배운 점**
1. **Swing 렌더링 타이밍**: UI 조작은 렌더링 완료 후 실행해야 함 (invokeLater 중첩)
2. **메서드 호출 순서**: 데이터 설정 → UI 업데이트 순서가 중요
3. **세션 저장 시점**: 분석 완료 후뿐 아니라 앱 종료 시에도 저장 필요
4. **상태 추적**: 복원할 모든 UI 상태를 명시적으로 필드로 관리

---

#### Session 29 (계속): GUI 테이블 영향도 UX 개선 및 신규 기능 5종 구현

**사용자 피드백 기반 신규 기능 요청**

테스트 과정에서 5가지 추가 기능 요청:
1. 상세화면 실시간 검색
2. 분석요약 탭별 동적 변경 (CRUD 통계)
3. URL 필터 탭별 표시/숨김
4. 마우스 뒤로가기 버튼 지원
5. SQL 필터 시 상태 유지

**구현 완료 내역**

| 기능 | 구현 위치 | 내용 |
|------|----------|------|
| SQL 필터 상태 유지 | MainFrame.java:1440-1510 | `applyFiltersAndRefresh()` 수정, 테이블/쿼리 선택 상태 저장 후 복원 |
| 분석요약 동적 변경 | MainFrame.java:462-551 | CardLayout으로 클래스 통계 ↔ CRUD 통계 전환 |
| URL 필터 숨김 | MainFrame.java:602-617 | `urlFilterPanel`로 분리, 테이블 영향도 탭에서 숨김 |
| 마우스 뒤로가기 | TableImpactPanel.java:230-247 | 버튼 4 (XBUTTON1) 리스너, 쿼리 상세에서 뒤로가기 |
| 상세화면 검색 | TableImpactPanel.java:92-95, 159-166, 260-271 | `accessSearchField` + `RowFilter` 실시간 필터링 |

**기술적 결정**

1. **SQL 필터 상태 유지**
   - 문제: `applyFiltersAndRefresh()`가 `updateTableList()`를 호출하면서 "전체"로 초기화
   - 해결: 현재 탭이 테이블 영향도인 경우 상태 저장 후 복원
   - 새 메서드: `updateTableListWithoutSelection()` - 기본 선택 없이 목록만 업데이트

2. **분석요약 CardLayout**
   - 호출 흐름 탭: 클래스/Controller/Service/DAO/URL 통계
   - 테이블 영향도 탭: 테이블 수/SELECT/INSERT/UPDATE/DELETE 통계
   - CRUD 색상 구분: SELECT(청록), INSERT(파랑), UPDATE(노랑), DELETE(빨강)

3. **마우스 확장 버튼**
   - Java MouseEvent에서 확장 버튼은 정수값으로 표현
   - 버튼 4 = XBUTTON1 (뒤로가기), 버튼 5 = XBUTTON2 (앞으로가기)
   - `MouseEvent.BUTTON4` 상수가 없어서 `e.getButton() == 4`로 처리

4. **상세화면 검색**
   - `TableRowSorter`의 `RowFilter.regexFilter()` 활용
   - 대소문자 무시 (`(?i)` 플래그)
   - 모든 컬럼에서 검색 (CRUD, URL, XML 파일, SQL ID)

**추가 개선: CRUD 표시 제거**

- 문제: 분석요약에 CRUD 통계가 있으니 상세화면 오른쪽 상단의 `S:X I:X U:X D:X` 표시 중복
- 해결: `crudStatsLabel` 관련 코드 전체 제거
- 브레드크럼 패널 구조 단순화 (BorderLayout → FlowLayout)

**변경 파일**

| 파일 | 변경 내용 |
|------|----------|
| `MainFrame.java` | CardLayout(summaryCardPanel), URL 필터 패널 분리, SQL 필터 상태 유지, CRUD 통계 계산 |
| `TableImpactPanel.java` | 검색 필드 추가, 마우스 뒤로가기 리스너, crudStatsLabel 제거 |

**배운 점**
1. **CardLayout 활용**: 탭별로 다른 UI를 보여주는 표준 패턴
2. **RowFilter**: Swing JTable 실시간 필터링의 표준 방법
3. **마우스 확장 버튼**: Java에서 정수값으로 처리 (상수 없음)
4. **UI 중복 제거**: 같은 정보를 여러 곳에 표시하면 UX 혼란

**다음 할 일**
- [ ] CRUD 통계 대시보드 (#24)
- [ ] 러너스하이 마무리 회고 글 작성
- [x] 기술8 jpackage 블로그 초안 작성 ← Session 30에서 완료

---

### 2026-01-15 (수) - Session 30

#### Session 30: 기술 블로그 8편 전체 재작성 및 WiX 기술 부채 청산

**문제**: 기술8 jpackage 블로그 초안에 잘못된 내용이 있고, 설명이 부족함
**해결**: 모든 기록 검토 후 블로그 전체 재작성 + WiX 불필요 코드 제거

**오늘 한 일**

1. **jpackage 관련 모든 기록 조사**
   - WEEK3.md: Session 13(Preferences), Session 15-17(jpackage)
   - WEEK4.md: Session 18-21(Gson, 이중 저장 문제, JSON 통합)
   - ISSUES.md: #015-#019, #024 상세 내용 확인

2. **소스 코드 검증**
   - Preferences API 사용 여부 grep → **No matches found** (완전 제거됨)
   - `SessionData.java` 확인 → 모든 설정이 JSON으로 저장됨
   - `main.wxs` 확인 → 레지스트리 정리 코드가 아직 남아있음 (불필요!)

3. **WiX main.wxs 기술 부채 청산**
   - **제거**: `RegistryCleanup` 컴포넌트 전체 (RemoveRegistryKey 관련)
   - **제거**: Feature에서 `RegistryCleanup` 참조
   - **추가**: 제거 이유 주석 (Session 20에서 Preferences→JSON 전환)
   - 세션 폴더 삭제 CustomAction은 유지 (JSON 저장 경로)

4. **기술8 블로그 초안 전체 재작성** (`docs/blog/초안/09-jpackage로배포하기.md`)

**초안 재작성 핵심 변경사항**

| 섹션 | 변경 전 | 변경 후 |
|------|---------|---------|
| Issue #024 | 레지스트리 정리 설명 포함 | 레지스트리 정리 삭제 (더 이상 불필요) |
| 신규 섹션 | 없음 | **"보너스: 설정 저장 방식의 진화"** 추가 |

**신규 섹션: 설정 저장 방식의 진화**

Preferences→JSON 전환 과정을 시간순으로 설명:

| Session | 내용 |
|---------|------|
| Session 13 | Preferences API 도입 (최근 경로, URL 필터, 출력 스타일) |
| Session 18 | Gson JSON 추가 (분석 결과 저장) |
| Session 19 | 이중 저장 문제 발견! (URL 필터, 출력 스타일 중복) |
| Session 20 | JSON 단일 저장으로 통합 (Preferences 완전 제거) |

**각 선택의 WHY 설명**:
- 왜 처음에 Preferences? → Java 표준, 추가 의존성 없음
- 왜 Gson 추가? → FlowResult 복잡한 객체 직렬화 필요
- 왜 이중 저장 문제 발생? → 새 기능 추가 시 기존 로직 검토 부족
- 왜 JSON으로 통합? → 크로스 플랫폼, 디버깅 용이, 백업 간편

**WiX 변경 내역**

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| RegistryCleanup Component | 있음 (RemoveRegistryKey 4개) | 제거됨 |
| Feature ComponentRef | `<ComponentRef Id="RegistryCleanup"/>` | 제거됨 |
| 세션 폴더 정리 | CustomAction (rmdir) | 유지 |

**배운 점**
1. **코드와 문서 동기화**: 문서 작성 시 실제 코드 상태 확인 필수
2. **기술 부채 발견**: 블로그 작성이 오히려 코드 리뷰 기회가 됨
3. **WHY 설명의 중요성**: 선택 이유를 설명해야 독자가 자신의 상황에 적용 가능
4. **시간순 스토리텔링**: 기술 결정의 변천사가 독자 이해를 높임

**다음 할 일**
- [ ] 블로그 티스토리 발행 및 검토
- [ ] CRUD 통계 대시보드 (#24)
- [ ] 러너스하이 마무리 회고 글 작성

---

### 2026-01-15 (수) - Session 31

#### Session 31: 블로그 초안 일괄 작성

**문제**: 기술 블로그 시리즈 초안이 부족하고, 마무리 회고 글도 미작성 상태
**해결**: 기존 발행 글 스타일 분석 후 3편 초안 일괄 작성

**오늘 한 일**

1. **기존 발행 블로그 스타일 분석 (티스토리 #54, #55, #61)**
   - 문체: 친근한 대화체 ("~습니다" 아닌 "~다" 체)
   - 비유: 마트료시카, 리모컨, 식당 비유 등 구체적 예시
   - 구조: 문제 → 원인 → 해결책 3단계 구조
   - 시작: 3줄 요약 위아래 구분선
   - 마무리: 시리즈 링크 + 다음 글 예고

2. **블로그 글쓰기 베스트 프랙티스 웹검색**
   - 초보자 대상 글은 개념 → 선택 → 튜토리얼 순서
   - 표보다 스크린샷이 설득력 높음
   - "나는 왜 이 기술을 선택했는가?"가 핵심

3. **기술9 초안 작성 완료** (`docs/blog/초안/10-Gson으로세션영속성구현하기.md`)
   - 제목: "Gson으로 세션 영속성 구현하기"
   - 핵심: Preferences API의 한계 → JSON 저장 필요
   - 비유: "영수증(메모장) vs 영수증 관리 시스템(JSON)"
   - 섹션: Gson 개념, 기본 사용법, 프로젝트 적용, 버전 업그레이드, 성장한 점

4. **기술10 초안 작성 완료** (`docs/blog/초안/11-CRUD분석과테이블영향도.md`)
   - 제목: "CRUD 분석과 테이블 영향도 - 역추적으로 영향도 파악하기"
   - 핵심: Top-Down(API) vs Bottom-Up(테이블) 분석 방식
   - 비유: "주소 → 건물찾기 vs 건물 → 왔던길 추적"
   - 섹션: CRUD 타입별 필터링, 테이블 중심 분석, 역방향 인덱싱, 실무 사용 시나리오

5. **러너스하이 마무리 초안 작성 완료** (`docs/blog/초안/러너스하이-마무리.md`)
   - 구성: 프로젝트 개요, 기술 스택, 주요 기능, 배운 점, 회고
   - 초안 상태로 추후 다듬기 (타겟 독자, 톤 결정 필요)

6. **BLOG_SERIES_PLAN.md 업데이트**
   - 전체 12편 중 현재 8편 발행, 3편 초안 → 진행률 반영
   - 모바일 가이드라인 추가 (테이블/코드 블록 깨짐 주의)
   - 발행 예정 글 상태 표시

**생성된 파일**
- `docs/blog/초안/10-Gson으로세션영속성구현하기.md`
- `docs/blog/초안/11-CRUD분석과테이블영향도.md`
- `docs/blog/초안/러너스하이-마무리.md`

**스타일 분석 결과**

| 항목 | 분석 내용 |
|------|---------|
| 문체 | "~습니다" 대신 "~다" 체 사용, 대화체 |
| 비유 | 일상 예시 (마트료시카, 리모컨, 식당, 영수증) |
| 구조 | 문제 → 원인 → 해결책 3단계 또는 개념 → 선택 → 튜토리얼 |
| 시작 | 3줄 요약 (구분선 위아래) |
| 마무리 | 정리표, 삽질 포인트, 성장한 점, 다음 글 예고 |

**배운 점**

1. **코드를 짜는 것과 설명하는 것은 다른 능력**
   - 코드는 논리, 글은 독자 이해 중심
   - 같은 개념도 예시와 비유로 완전히 달라짐

2. **기존 글의 톤앤매너 분석 필수**
   - 개별 글이 좋아도 시리즈 일관성이 없으면 어색함
   - 문체(~다/~습니다), 비유 스타일, 섹션 수 통일 필요

3. **스토리텔링의 중요성**
   - 기술 선택의 변천사(Preferences→JSON→통합) 자체가 흥미로운 이야기
   - "왜" 질문에 답하는 과정이 독자 공감 높임

4. **초안과 발행의 거리**
   - 초안 작성 시점과 발행 시점의 문체/톤 차이 수정 필요
   - 실제 발행 글 스타일을 먼저 분석 후 초안 작성하는 것이 효율적

**다음 할 일**
- [ ] 기술9 초안 다듬기 (Gson 섹션 확장, 실제 코드 예시 추가)
- [ ] 기술10 초안 다듬기 (테이블 영향도 스크린샷 추가)
- [ ] 러너스하이 마무리 회고 최종 작성 (톤 결정, 구체적 회고 추가)
- [ ] CRUD 통계 대시보드 (#24) 검토

---

### 2026-01-15 (수) - Session 32

#### Session 32: 실무 테스트 및 성장일지 작성

**문제**: 러너스하이 종료일, 실무 프로젝트에서 CTF 테스트 시 발견된 문제점들
**해결**: 문제 분석 및 문서화, 성장일지 초안 작성

**오늘 한 일**

1. **실무 프로젝트 CTF 테스트**
   - 회사 프로젝트(BookFinder, KissEncryption 등)에서 실제 분석 수행
   - 369개 엔드포인트 분석 성공
   - 3가지 문제점 발견 및 원인 분석

2. **발견된 문제점 (Issue #031~#033)**

   | Issue | 문제 | 원인 | 상태 |
   |-------|------|------|------|
   | #031 | HTTP Method가 ALL로 표시 | @RequestMapping method 속성 미파싱 | 🟡 |
   | #032 | 내부 함수 호출 미추적 | scope 기반 필터링 (this/빈값 스킵) | 🟡 |
   | #033 | DAO/XML 매칭 불일치 | 특정 메서드명만 인식, 문자열 리터럴만 | 🟡 |

3. **문제 원인 상세 분석**

   **#031 - HTTP Method ALL 표시**
   ```java
   // JavaSourceParser.java:519-525
   private String extractHttpMethod(String annotationName) {
       if (annotationName.equals("GetMapping")) return "GET";
       if (annotationName.equals("PostMapping")) return "POST";
       return "ALL";  // @RequestMapping은 무조건 ALL
   }
   ```
   - `@RequestMapping(method=RequestMethod.POST)` 같은 경우도 ALL로 표시

   **#032 - 내부 함수 미추적**
   ```java
   // MethodCall.java:54-62
   if (scope == null || scope.isEmpty()) {
       return false;  // 내부 호출은 scope가 비어있음
   }
   ```
   - `this.getCode()`, `getToday()` 같은 호출 추적 안 됨

   **#033 - DAO/XML 매칭 문제**
   ```java
   // extractSqlId()에서 특정 메서드명만 인식
   List<String> sqlMethods = List.of("selectOne", "selectList", ...);
   if (firstArg instanceof StringLiteralExpr) { ... }  // 변수면 스킵
   ```
   - `getSqlSession().selectOne(...)` 체이닝 미지원
   - 변수로 SQL ID 전달 시 추적 불가

4. **성장일지 초안 작성** (`docs/러너스하이_성장일지_초안.md`)
   - ROI 섹션 추가: API 분석 시간 88% 단축 (25분→3분)
   - TDD 언급 제거 → "실무 적용 및 피드백 수집"으로 변경

5. **ISSUES.md 업데이트**
   - Issue #031~#033 상세 기록
   - 각 문제별 해결 방안 제시

**실무 피드백**
- 369개 엔드포인트를 분석한 것 자체는 성공
- 하지만 HTTP Method 구분이 안 되면 실제 사용성 저하
- 내부 함수 호출도 추적해야 완전한 흐름 파악 가능

**배운 점**
1. **실무 테스트의 중요성**: 샘플 코드와 실제 프로젝트는 패턴이 다름
2. **@RequestMapping의 다양성**: method 속성 사용이 많음
3. **전자정부프레임워크 패턴**: AbstractDAO, getSqlSession() 체이닝 패턴 고려 필요
4. **성장일지 ROI 정량화**: 구체적 수치가 설득력 높임

**다음 할 일**
- [ ] Issue #031 수정 (@RequestMapping method 속성 파싱)
- [ ] Issue #032 수정 (내부 함수 호출 추적 옵션)
- [ ] Issue #033 수정 (XML 기반 역매칭 방식)
- [x] 성장일지 PDF 변환 및 제출 ✅ 2026-01-18

---

### 2026-01-27 (월) - Session 33

#### Session 33: 실무 테스트 이슈 정리 및 에러 핸들링 요구사항 정의

**문제**: 실무 테스트 중 앱 멈춤 현상 발생, 에러 핸들링 부재
**해결**: 이슈 정리, GitHub 이슈 등록, 에러 핸들링 요구사항 정의

**오늘 한 일**

1. **기술 블로그 검토** (https://kbroj9210.tistory.com/)
   - #62 jpackage 배포 - ⭐⭐⭐⭐⭐ 우수
   - #63 Gson 세션 영속성 - ⭐⭐⭐⭐⭐ 우수
   - #64 CRUD 분석 - ⭐⭐⭐⭐⭐ 우수
   - 10편 시리즈 완결성 확인

2. **실무 테스트 스크린샷 분석** (30개)
   - CFT GUI 결과 화면 분석
   - AS-IS 시스템 코드 패턴 확인
   - 기존 이슈 (#031~#033) 재확인

3. **신규 이슈 정의 - #034 (앱 멈춤 + 에러 핸들링)**

   | 필요 기능 | 설명 |
   |----------|------|
   | 에러 다이얼로그 | SwingWorker.done()에서 예외 캐치 및 표시 |
   | 에러 로그 파일 | ~/.code-flow-tracer/logs/error.log |
   | 분석 타임아웃 | 기본 5분 후 자동 중단 |
   | 분석 취소 버튼 | 사용자 직접 중단 기능 |

4. **GitHub 이슈 등록**
   - [#36](https://github.com/KBroJ/Code-Flow-Tracer/issues/36) 앱 멈춤 + 에러 핸들링

5. **문서 업데이트**
   - TODO.md: 실무 테스트 이슈 섹션 추가, 버그 테이블 #31~#34 추가
   - ISSUES.md: Issue #034 상세 내용 추가

**현재 미해결 이슈 현황**

| ISSUES.md | GitHub | 설명 | 우선순위 |
|-----------|--------|------|---------|
| #034 | #36 | 앱 멈춤 + 에러 핸들링 | 🔴 최우선 |
| #031 | #33 | HTTP Method ALL 표시 | 🟠 높음 |
| #033 | #35 | DAO/XML 매칭 개선 | 🟡 중간 |
| #032 | #34 | 내부 함수 호출 추적 | 🟢 낮음 |

**앱 멈춤 현상 정보**
- 진행 표시줄이 움직이다가 멈춤 (분석 중 특정 지점에서 hang)
- 폐쇄망 환경으로 재현 소스 제공 불가
- 에러 메시지/로그 없어 원인 파악 어려움

**배운 점**
1. **에러 핸들링의 중요성**: 에러 없이 멈추면 디버깅 불가
2. **로깅 시스템 필수**: 어디서 멈추는지 추적하려면 로그 필요
3. **타임아웃/취소 기능**: 장시간 작업엔 탈출구 필요

**다음 할 일**
- [x] #034 에러 핸들링 구현 (에러 다이얼로그 + 로그 + 타임아웃 + 취소) → 진행중
- [ ] #031 수정 (@RequestMapping method 속성 파싱)
- [ ] #033 수정 (XML 기반 역매칭)
- [ ] #032 수정 (내부 함수 호출 추적)

---

### 2026-01-27 (월) - Session 33 계속

#### Session 33-2: 에러 핸들링 구현 (#36)

**문제**: 대용량 프로젝트 분석 시 앱이 멈추고, 에러 메시지/로그 없음
**해결**: 로깅 시스템 + 타임아웃 + 취소 버튼 구현

**구현 완료**

1. **로깅 시스템 (CftLogger.java 신규)**
   - 위치: `com.codeflow.util.CftLogger`
   - 로그 경로: `~/.code-flow-tracer/logs/cft.log`
   - 로테이션: 5MB × 3개 = 최대 15MB
   - 설계 결정: SLF4J 대신 java.util.logging (폐쇄망 환경, 추가 의존성 없음)

2. **분석 취소 버튼 (토글)**
   - 분석 전: `[▶ 분석 실행]` (파란색)
   - 분석 중: `[■ 분석 취소]` (빨간색)
   - SwingWorker.cancel(true) + isCancelled() 체크 포인트 3곳

3. **타임아웃 (기본 5분)**
   - javax.swing.Timer 사용 (EDT 안전)
   - 타임아웃 시 자동 취소 + 로그 경로 안내

4. **에러 다이얼로그 개선**
   - 에러 메시지 + 로그 파일 경로 함께 표시
   - 스택트레이스 로그 파일에 기록

5. **진행 상황 로깅**
   - 각 단계별 시작/완료 로그
   - 파싱된 클래스 수, SQL 수, 엔드포인트 수 기록

**기술적 결정 및 이유**

| 결정 | 선택 | 이유 |
|------|------|------|
| 로깅 라이브러리 | java.util.logging | 폐쇄망 환경, JDK 내장 |
| 타이머 | javax.swing.Timer | EDT에서 실행, UI 안전 |
| 취소 방식 | 버튼 토글 | 추가 버튼 없이 직관적 UX |
| 로그 로테이션 | 5MB × 3개 | 최대 15MB로 디스크 보호 |

**변경된 파일**
- `src/main/java/com/codeflow/util/CftLogger.java` (신규)
- `src/main/java/com/codeflow/ui/MainFrame.java` (수정)

**빌드/테스트 결과**
- 컴파일: ✅ 성공
- 테스트: ✅ 전체 통과

**남은 작업 (다음 세션에서 계속)**
- [ ] "로그 폴더 열기" 버튼 추가 (설정 버튼에)
- [ ] GUI 실행 테스트 (버튼 토글, 취소, 로그 파일 확인)
- [ ] 커밋 및 PR

---

### 2026-01-27 (월) - Session 33 계속

#### Session 33-3: 로그 설정 UI 구현 완료 (#36)

**문제**: 로그 폴더 접근 어려움, 로그 크기 사용자 설정 불가
**해결**: 설정 메뉴에 로그 폴더 열기 및 로그 크기 설정 기능 추가

**구현 완료**

1. **로그 폴더 열기 메뉴**
   - 설정 버튼(⚙) 클릭 시 "로그 폴더 열기" 메뉴 추가
   - `Desktop.getDesktop().open()` 사용
   - 폴더 없을 시 자동 생성

2. **로그 크기 설정 서브메뉴**
   - 1MB (최대 3MB)
   - 5MB (최대 15MB) - 기본
   - 10MB (최대 30MB)
   - 라디오 버튼 그룹으로 단일 선택
   - 설정 변경 시 로거 재초기화 + 세션 저장

3. **세션 영속성**
   - SessionData에 `logSizeMB` 필드 추가
   - 앱 시작 시 로그 크기 설정 로드 및 적용
   - SessionManager에서 설정 유지 로직 추가

**변경된 파일**

| 파일 | 변경 내용 |
|------|----------|
| `CftLogger.java` | setLogSizeMB(), getLogSizeMB(), getLogFolder(), reinitialize() 메서드 추가 |
| `SessionData.java` | logSizeMB 필드 및 getter/setter 추가 |
| `SessionManager.java` | saveSession()에서 logSizeMB 유지 로직 추가 |
| `MainFrame.java` | 설정 메뉴 확장 (로그 폴더 열기, 로그 크기 설정), loadSettings()에서 로그 크기 로드 |

**기술적 결정**

| 결정 | 선택 | 이유 |
|------|------|------|
| 로그 폴더 열기 | Desktop.open() | OS 기본 파일 탐색기 사용, 크로스 플랫폼 |
| 로그 크기 옵션 | 1/5/10MB 라디오 버튼 | 직관적 선택, 실수 방지 |
| 설정 저장 | SessionData 통합 | 기존 JSON 세션 파일 활용, 별도 설정 파일 불필요 |

**설정 메뉴 구조**
```
⚙ 설정 버튼 클릭 →
┌──────────────────────┐
│ 로그 폴더 열기       │
├──────────────────────┤
│ 로그 크기 설정   ▶ ──┼──┐
├──────────────────────┤  │ ○ 1MB (최대 3MB)
│ ─────────────────────│  │ ● 5MB (최대 15MB) - 기본
│ 설정/세션 초기화     │  │ ○ 10MB (최대 30MB)
└──────────────────────┘  │
                          └──────────────────┘
```

**빌드/테스트 결과**
- 컴파일: ✅ 성공
- 테스트: ✅ 전체 통과

**에러 핸들링 기능 완료 정리**

| 기능 | 상태 | 설명 |
|------|------|------|
| 에러 로그 파일 | ✅ | ~/.code-flow-tracer/logs/cft.log |
| 분석 타임아웃 | ✅ | 기본 5분, javax.swing.Timer |
| 분석 취소 버튼 | ✅ | 토글 방식 (분석 실행 ↔ 분석 취소) |
| 에러 다이얼로그 | ✅ | 에러 메시지 + 로그 경로 안내 |
| 로그 폴더 열기 | ✅ | 설정 메뉴에 추가 |
| 로그 크기 설정 | ✅ | 1/5/10MB 선택, 세션에 저장 |

---

### 2026-01-28 (화) - Session 34

#### Session 34: GUI 테스트 → 4가지 버그 발견 및 수정

**목표**: 에러 핸들링 기능 GUI 테스트 및 검증
**결과**: 테스트 중 4가지 버그 발견, 원인 분석 후 수정 완료

**테스트 코드 작성 (테스트 전)**

테스트에 앞서 신규 기능에 대한 단위 테스트를 작성했다.

| 테스트 클래스 | 테스트 수 | 내용 |
|-------------|----------|------|
| CftLoggerTest (신규) | 23개 | 싱글톤, 로그 경로, 크기 설정, 각 레벨 동작, 분석 로그 |
| SessionDataTest (+5) | 17개 | logSizeMB 기본값, getter/setter, 생성자 기본값 |
| SessionManagerTest (+5) | 15개 | logSizeMB 저장/로드, 세션 저장 시 유지, saveSettings 시 유지 |

전체 98개 테스트 통과 확인 후 GUI 테스트 진행.

---

**버그 1: 로그 크기 설정이 앱 재시작 후 유지되지 않음**

- 증상: 10MB로 변경 → 앱 종료 → 재시작 → 5MB(기본값)로 돌아감
- 원인 분석:
  - 세션 파일에는 `logSizeMB: 10`이 정상 저장됨 (파일 확인 완료)
  - 문제는 **설정 메뉴 UI**에 있었음
  - `createSettingsPopupMenu()`가 앱 시작 시 **한 번만** 호출됨
  - 이 시점에서 `logger.getLogSizeMB()`가 아직 기본값 5MB
  - `loadSettings()`에서 10MB로 변경해도, 이미 생성된 라디오 버튼은 5MB 선택 상태

```java
// 문제: 팝업 메뉴가 앱 시작 시 1번만 생성됨
JPopupMenu settingsPopup = createSettingsPopupMenu();  // 이때 5MB
settingsButton.addActionListener(e ->
    settingsPopup.show(...));  // 매번 같은 메뉴 재사용

// 해결: 클릭할 때마다 메뉴 새로 생성
settingsButton.addActionListener(e -> {
    JPopupMenu settingsPopup = createSettingsPopupMenu();  // 현재 값 반영
    settingsPopup.show(...);
});
```

- 추가 수정: `handleLogSizeChange()`에서 settings가 null일 때도 새 SessionData 생성
  - 분석 결과 없이 설정만 변경하는 경우를 처리

**배운 점**: Swing에서 동적 상태를 표시하는 UI는 캐시하면 안 됨. 팝업 메뉴처럼 매번 열릴 때 현재 상태를 반영해야 하는 UI는 매번 새로 생성하거나, 열릴 때 상태를 업데이트해야 함.

---

**버그 2: SLF4J 콘솔 로그 한글 깨짐**

- 증상: 터미널에서 SessionManager 로그가 깨져서 출력됨
  ```
  21:30:05 [AWT-EventQueue-0] INFO  SessionManager -- ?몄뀡 濡쒕뱶 ?꾨즺
  ```
  반면 CftLogger, System.out.println 로그는 정상 출력
  ```
  [2026-01-28 21:30:12] [WARNING] 분석 취소됨 (사용자 요청)
  세션 복원 완료: C:\Devel\Code-Flow-Tracer\samples
  ```

- 원인 분석:
  - Windows 콘솔 기본 인코딩: MS949 (EUC-KR 계열)
  - SLF4J/Logback: 내부적으로 UTF-8 인코딩 사용 → MS949 콘솔에서 깨짐
  - java.util.logging(CftLogger): 시스템 기본 인코딩(MS949) 사용 → 정상
  - System.out.println: 시스템 기본 인코딩 사용 → 정상

- 시도 1: `logback.xml`에 `<charset>UTF-8</charset>` 설정 → 실패 (이미 UTF-8이 문제)
- 시도 2: `logback.xml`에서 charset 제거 (시스템 기본 사용) → 실패 (Logback 내부 동작)

- 최종 해결: SessionManager에서 **SLF4J를 제거하고 CftLogger로 교체**
  ```java
  // Before: SLF4J (한글 깨짐)
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
  log.info("세션 로드 완료: {} ({} flows)", path, count);

  // After: CftLogger (정상 출력)
  import com.codeflow.util.CftLogger;
  private static final CftLogger log = CftLogger.getInstance();
  log.info("세션 로드 완료: %s (%d flows)", path, count);
  ```
  - 참고: SLF4J는 `{}`를 플레이스홀더로 사용, CftLogger는 `String.format`의 `%s` 사용

  **주의: `error(message, throwable)` 호출 시 `%s`가 아닌 `+` 연결 사용**

  SLF4J는 마지막 인자가 Throwable이면 자동으로 스택트레이스를 처리하는 특수 로직이 있다:
  ```java
  // SLF4J: {} 플레이스홀더 + 마지막 인자 Throwable 자동 처리
  log.error("설정 로드 실패: {}", e.getMessage(), e);
  ```

  CftLogger로 변환 시 단순히 `{}`를 `%s`로 바꾸면 안 된다. `error(String, Throwable)` 메서드는 포맷 문자열을 지원하지 않기 때문:
  ```java
  // CftLogger error 메서드 시그니처
  public void error(String message, Throwable throwable)  // 완성된 문자열 + 예외 객체

  // ❌ 이렇게 하면 안 됨 (error에 varargs 오버로드 없음)
  log.error("설정 로드 실패: %s", e.getMessage(), e);

  // ✅ 올바른 변환: + 연결로 문자열 완성 후 전달
  log.error("설정 로드 실패: " + e.getMessage(), e);
  ```

  **왜 `error`에만 이 문제가 있는가?**

  `info`, `warn`, `debug`는 varargs 오버로드가 있어서 `%s` 사용 가능:
  ```java
  public void info(String format, Object... args)  // varargs → String.format 지원
  ```

  하지만 `error`에서 Throwable과 varargs를 동시에 쓸 수 없다. Java에서 **varargs(`...`)는 반드시 메서드의 마지막 파라미터**여야 하기 때문:
  ```java
  // 이런 시그니처는 Java 문법상 불가능
  public void error(String format, Throwable t, Object... args)  // ❌ varargs가 마지막이 아님
  public void error(String format, Object... args, Throwable t)  // ❌ 컴파일 에러
  ```

  SLF4J는 이 제약을 우회하기 위해 **varargs 마지막 인자가 Throwable인지 런타임에 체크**하는 방식을 사용한다. java.util.logging 기반인 CftLogger는 이런 처리가 없으므로, 에러+예외 로깅 시에는 `+` 연결로 메시지를 완성해야 한다.

- 선택 이유:
  - logback.xml 설정으로는 Windows 콘솔 인코딩 문제를 근본적으로 해결 불가
  - 폐쇄망 환경에서 SLF4J 의존성을 줄이는 것이 프로젝트 방향과 일치
  - CftLogger는 이미 파일+콘솔 로깅을 지원하므로 기능 손실 없음

**배운 점**: Windows에서 Java 콘솔 출력 인코딩은 JVM과 콘솔 코드 페이지 설정에 따라 달라진다. 라이브러리마다 인코딩 처리 방식이 다르므로, 한글 환경에서는 일관된 로깅 시스템을 사용하는 것이 안전하다.

---

**버그 3: 로그 파일 삭제 시 사용 중인 파일이 삭제되지 않음**

- 증상: "로그 파일 삭제" 실행 → 일부 파일 삭제되지만 현재 사용 중인 `cft.log`는 남아있음
- 원인: Windows에서는 파일이 열려 있으면(FileHandler가 잠금) 삭제 불가
  ```
  java.util.logging.FileHandler → cft.log 파일 잠금 유지
  Files.delete(cft.log) → AccessDeniedException
  ```

- 해결: CftLogger에 `closeHandlers()` 메서드 추가
  ```java
  // 핸들러 닫기 → 파일 잠금 해제
  public void closeHandlers() {
      for (Handler handler : logger.getHandlers()) {
          handler.close();
          logger.removeHandler(handler);
      }
      initialized = false;
  }
  ```

  삭제 흐름: `closeHandlers()` → 파일 삭제 → 다음 로그 호출 시 자동 재초기화

- 자동 재초기화를 위해 `ensureInitialized()` 패턴 적용:
  ```java
  public void info(String message) {
      ensureInitialized();  // initialized가 false이면 자동으로 initialize() 호출
      logger.info(message);
  }

  private void ensureInitialized() {
      if (!initialized) {
          initialize();
      }
  }
  ```

**배운 점**: Windows 파일 잠금은 Java에서도 적용된다. 파일을 삭제하려면 해당 파일을 열고 있는 모든 핸들을 먼저 닫아야 한다. Lazy initialization 패턴을 사용하면 닫은 후 자동으로 다시 열 수 있다.

---

**추가 기능: 로그 파일 삭제 메뉴**

- 사용자 요청으로 설정 메뉴에 "로그 파일 삭제" 추가
- 확인 다이얼로그 후 삭제 실행

```
⚙ 설정 버튼 클릭 →
┌──────────────────────┐
│ 로그 폴더 열기       │
│ 로그 크기 설정   ▶   │
│ 로그 파일 삭제       │
├──────────────────────┤
│ 설정/세션 초기화     │
└──────────────────────┘
```

**변경된 파일**

| 파일 | 변경 내용 |
|------|----------|
| `CftLogger.java` | closeHandlers(), ensureInitialized() 추가 |
| `SessionManager.java` | SLF4J → CftLogger 교체, {} → %s 포맷 변환 |
| `MainFrame.java` | 팝업 메뉴 매번 생성, 로그 파일 삭제 메뉴, handleLogSizeChange null 처리 |
| `logback.xml` (신규) | 콘솔 인코딩 설정 (SLF4J 대체 후에도 다른 라이브러리용으로 유지) |
| `CftLoggerTest.java` (신규) | 23개 단위 테스트 |
| `SessionDataTest.java` | logSizeMB 테스트 5개 추가 |
| `SessionManagerTest.java` | logSizeMB 저장/로드 테스트 5개 추가 |

**빌드/테스트 결과**
- 컴파일: ✅ 성공
- 테스트: ✅ 98개 전체 통과

**남은 테스트 항목**
- [x] 한글 로그 정상 출력 확인
- [x] 로그 파일 삭제 동작 확인 → 버그 4 발견/수정 (삭제 직후 재생성)
- [x] 전체 분석 흐름 테스트 (분석 실행 → 취소 → 재실행 → 완료)
- [ ] 커밋 및 PR 생성

---

### 2026-01-28 (화) - Session 34 계속

#### 문서 현행화 검토

**작업 내용**
- ISSUES.md #034 상태: "🔴 신규" → "🟢 해결됨"
- ISSUES.md #034에 구현 상세 및 버그 3건 수정 내용 추가
- TODO.md: 버그 테이블 #34 상태 "🔴 신규" → "✅ 해결 | 2026-01-28"
- DESIGN.md: 패키지 구조 섹션에 `util/` 패키지 추가 (CftLogger.java)
- USAGE.md: GUI 기능 테이블 업데이트
  - 분석 취소/타임아웃 추가
  - 로그 관련 메뉴 추가 (로그 폴더 열기, 로그 크기 설정, 로그 파일 삭제)
  - 설정/세션 초기화 통합
- README.md:
  - GUI 주요 기능 테이블에 분석 취소/타임아웃/로그 관리 추가
  - 주요 기능 테이블에 에러 핸들링 추가
- CLAUDE.md: 패키지 구조 섹션에 `util/` 패키지 추가

**왜**: 에러 핸들링 기능 구현이 완료되었으므로 프로젝트 전체 문서를 최신 상태로 동기화하여 다음 작업이 문서 기반으로 진행될 수 있도록 함

#### 버그 4: 로그 파일 삭제 직후 재생성 수정

**문제**: 로그 파일 삭제 → "삭제되었습니다" 표시 → 로그 폴더에 `cft.log.0` + `cft.log.0.lck` 잔존

**원인 분석**:
```java
// handleClearLogs() 실행 순서
logger.closeHandlers();    // 1. FileHandler 닫기 → 잠금 해제
Files.delete(file);         // 2. 파일 삭제 (성공)
logger.info("로그 파일 삭제 후 로거 재시작");  // 3. ★ 문제!
// → ensureInitialized() → initialize() → 새 FileHandler → cft.log.0 즉시 재생성
```

- 버그 3에서 `closeHandlers()` + `ensureInitialized()` (Lazy initialization) 패턴을 도입했는데, 삭제 직후 `logger.info()`를 호출하여 바로 재초기화가 트리거됨
- `.lck` 파일: FileHandler 열릴 때 자동 생성되는 잠금 파일 (프로그램 종료 시 삭제)

**해결**: 삭제 로직 이후 불필요한 `logger.info()` 호출 제거. 로거는 닫힌 상태로 유지되고, 다음 실제 로그 이벤트(분석 시작 등) 발생 시 `ensureInitialized()`로 자동 재시작

**배운 점**: Lazy initialization 패턴의 부작용 — 리소스 정리 직후 같은 리소스를 사용하는 코드가 있으면 정리 효과가 무효화됨. 정리 로직 이후에는 해당 리소스를 다시 사용하는 코드 경로가 없는지 반드시 점검해야 함

---
