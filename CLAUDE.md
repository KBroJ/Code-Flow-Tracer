# CLAUDE.md - 프로젝트 규칙 및 컨텍스트

> 이 파일은 Claude Code가 프로젝트 작업 시 참고하는 규칙입니다.

## 프로젝트 개요

- **프로젝트명**: Code Flow Tracer
- **목적**: 레거시 Java 코드의 호출 흐름 자동 분석 및 문서화
- **배경**: 토스 러너스하이 2기 참여 프로젝트

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 17+ |
| Build | Gradle | 8.5 |
| 파싱 | JavaParser | 3.25.5 |
| XML | JDOM2 | 2.0.6.1 |
| Excel | Apache POI | 5.2.5 |
| JSON | Gson | 2.10.1 |
| CLI | Picocli | 4.7.5 |
| GUI | Swing | JDK 내장 |
| Test | JUnit 5 | 5.10.1 |

## 코딩 컨벤션

### 패키지 구조
```
com.codeflow/
├── Main.java           # 엔트리포인트
├── parser/             # 소스 코드 파싱 (JavaParser)
├── analyzer/           # 호출 흐름 분석
├── output/             # 출력 (Console, Excel)
├── session/            # 세션 영속성 (Gson)
└── ui/                 # Swing GUI
```

### 네이밍 규칙
- 클래스: PascalCase (예: `JavaSourceParser`)
- 메서드/변수: camelCase (예: `parseFile`, `parsedClasses`)
- 상수: UPPER_SNAKE_CASE (예: `DEFAULT_TIMEOUT`)
- 패키지: lowercase (예: `com.codeflow.parser`)

### 코드 스타일
- 들여쓰기: 4 spaces
- 최대 줄 길이: 120자
- Javadoc: public 메서드에 필수
- 한글 주석 허용 (사용자가 한국어 사용)

## 개발 규칙

### 테스트
- 새 기능 추가 시 테스트 코드 필수
- `samples/` 폴더의 샘플 코드로 테스트
- 테스트 실행: `./gradlew test`

> Git 관련 규칙(커밋, 브랜치, PR)은 하단의 **"프로젝트 Git 워크플로우 규칙"** 참조

## 문서화 규칙

### docs 폴더 구조
```
docs/
├── PROJECT_PLAN.md        # 프로젝트 기획 (러너스하이)
├── DESIGN.md              # 전체 설계
├── IMPLEMENTATION_GUIDE.md # 구현 상세 가이드 (코드 흐름, 설계 결정)
├── USAGE.md               # 사용법
├── TODO.md                # 할 일 목록
├── DEV_LOG.md             # 개발 일지 (목차 + 현재 주)
├── ISSUES.md              # 문제 및 해결 과정
└── dev-log/               # 주별 개발 일지 (WEEK1~4.md)
```

### 문서 작성 원칙
1. **왜(Why)** 먼저: 기술 선택, 설계 결정의 이유를 반드시 기록
2. **문제 → 해결**: 트러블슈팅은 문제 상황, 시도, 해결, 배운 점 순서로
3. **날짜 기록**: 진행 상황에는 날짜 필수
4. **코드 예시**: 가능하면 코드 예시 포함

## 러너스하이 관련

### 핵심 질문 (항상 기억)
- 왜 해당 기술을 적용해야 하는가?
- 왜 이 방법을 선택했는가?
- 왜 문제가 발생했는가?
- 어떻게 해결할 것인가?
- 어떻게 성장할 것인가?

### 기록 원칙
- 매일 개발 일지 작성 (DEV_LOG.md)
- 문제 해결 과정 상세 기록 (ISSUES.md)
- 기술 선택의 근거 문서화 (DESIGN.md)

## 작업별 문서화 규칙 (자동 적용)

> **중요**: 아래 규칙은 Claude가 작업 시 자동으로 적용합니다.

### 코드 변경 시
- DEV_LOG.md에 오늘 날짜로 "오늘 한 일" 기록 추가
- 관련 TODO.md 항목 상태 업데이트

### 새 기능 구현 시
- DESIGN.md에 설계 내용 추가 (구조 변경이 있는 경우)
- USAGE.md에 사용법 추가 (사용자 인터페이스 변경 시)
- DEV_LOG.md에 구현 내용 및 기술 선택 이유 기록

### 버그/문제 해결 시
- ISSUES.md에 문제 → 원인 → 해결 → 배운 점 기록
- DEV_LOG.md "배운 점" 섹션에 요약 추가

### 하루 작업 종료 시
- DEV_LOG.md "내일 할 일" 작성
- TODO.md 진행 상태 업데이트 (완료 항목 체크)
- 고민/질문 사항 기록

### 문서 현행화 체크리스트
작업 완료 후 아래 항목 확인:
- [ ] README.md의 기능 상태 표시가 최신인가?
- [ ] TODO.md의 체크박스가 실제 진행 상황과 일치하는가?
- [ ] DEV_LOG.md에 오늘 날짜 기록이 있는가?

## 자주 사용하는 명령어

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 단일 JAR 생성
./gradlew shadowJar

# 실행
java -jar build/libs/code-flow-tracer.jar --help
```

## 주의사항

1. **분석 대상 vs 도구 환경**
   - 도구는 Java 17+로 개발
   - 분석 대상은 Java 1.4~21 모두 지원

2. **폐쇄망 고려**
   - 외부 네트워크 의존성 최소화
   - 단일 JAR로 배포 가능하게

3. **레거시 환경 지원**
   - 전자정부프레임워크 구조 우선 지원
   - iBatis/MyBatis XML 파싱 지원

---

## Git 워크플로우 규칙

### Repository
- **GitHub Repository:** `KBroJ/Code-Flow-Tracer`
- **Main Branch:** `main`

### Branching Strategy
- `main`: 안정 버전
- `feature/[이슈번호]-[설명]`: 기능 개발 (예: `feature/1-add-flow-analyzer`)
- `fix/[설명]`: 버그 수정
- `chore/[설명]`: 빌드, 설정 변경

### Commit Message Convention
**Conventional Commits** 명세를 따름 (설명은 한글 사용)

```
<type>: <한글 설명>

[본문 - 선택, 한글 가능]

타입:
- feat: 새 기능
- fix: 버그 수정
- docs: 문서 수정
- refactor: 리팩토링
- test: 테스트 추가/수정
- chore: 빌드, 설정 변경
```

예시: `feat: FlowAnalyzer 호출 흐름 분석 기능 추가`

### Pull Request (PR) Process
- **PR 작성 필수**: 모든 기능은 PR을 통해 `main` 브랜치로 머지
- **셀프 리뷰 허용**: 혼자 개발하는 프로젝트이므로 셀프 리뷰 후 머지 가능
- **PR 템플릿 준수**: `.github/PULL_REQUEST_TEMPLATE.md`를 사용하여 변경 사항 요약 작성
- **PR 제목/본문**: 한글 사용 가능 (예: `feat: FlowAnalyzer 호출 흐름 분석 기능 추가`)
- **라벨 필수**: PR 작성 시 적절한 라벨 추가 (예: `feature`, `bug`, `docs`, `refactor`)
- GitHub 이슈 연결: `Closes #[이슈번호]` (선택)

### PR 변경 내용 작성 형식

**접이식(Collapsible) + 커밋 SHA 링크** 형식 사용:

```markdown
<details>
<summary>변경 내용 설명 (<code>파일명.java</code>)</summary>

- 세부 변경 1 ([#L시작-L끝](https://github.com/KBroJ/Code-Flow-Tracer/blob/{커밋SHA}/경로/파일.java#L시작-L끝))
- 세부 변경 2 ([#L라인](링크))

</details>
```

**규칙:**
- 설명이 먼저, 파일명은 괄호 안에: `변경 내용 설명 (파일명.java)`
- 신규 파일은 앞에 `(신규)` 표시: `(신규) 새 기능 설명 (NewFile.java)`
- 링크는 **커밋 SHA 기준** 사용 (브랜치 삭제 후에도 링크 유지)
- 세부 변경 항목에 라인 번호 링크 포함

**예시:**
```markdown
<details>
<summary>(신규) URL 패턴 매칭 유틸리티 (<code>UrlMatcher.java</code>)</summary>

- 와일드카드 패턴 매칭 ([#L57-L79](https://github.com/KBroJ/Code-Flow-Tracer/blob/8725f41/src/.../UrlMatcher.java#L57-L79))

</details>
```

### AI 도구 사용
- 커밋 메시지에 자동 생성 문구 제거 (예: "🤖 Generated with Claude Code" 제거)
