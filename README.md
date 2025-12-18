# Code Flow Tracer

> 레거시 Java 코드의 호출 흐름을 자동으로 분석하고 문서화하는 도구

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/)
[![Build](https://img.shields.io/badge/Build-Gradle-02303A)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## 왜 만들었나요?

SI 프로젝트에서 **레거시 코드를 파악하는 것은 매우 시간이 많이 걸리는 작업**입니다.

- API 하나의 흐름을 따라가려면 여러 파일을 오가며 수동으로 추적해야 합니다
- 분기가 많은 코드에서는 흐름이 헷갈립니다
- 인수인계 시 문서화된 자료가 부족합니다

**Code Flow Tracer**는 이 과정을 자동화합니다:

```
[요청 URL: /api/user/list]
    ↓
[UserController.getList()]
    ↓
[UserService.findAll()]
    ↓
[UserDAO.selectUserList]
    ↓
[SQL: SELECT * FROM TB_USER WHERE USE_YN = 'Y']
```

---

## 주요 기능

| 기능 | 설명 | 상태 |
|------|------|------|
| **호출 흐름 추적** | Controller → Service → DAO → SQL | ✅ 완료 |
| **iBatis/MyBatis 파싱** | XML SQL 매퍼에서 쿼리 추출 | ⏳ 예정 |
| **콘솔 출력** | 트리 형태 + ANSI 색상 지원 | ✅ 완료 |
| **엑셀/마크다운 출력** | 문서화용 출력 형식 | ⏳ 예정 |
| **Desktop GUI** | Swing 기반, JAR 더블클릭 실행 | ⏳ 예정 |
| **전자정부프레임워크** | 레거시 SI 환경 특화 지원 | ✅ 지원 |

---

## 빠른 시작

### 요구사항
- Java 17 이상

### 빌드 & 실행
```bash
# 빌드
./gradlew build

# 단일 실행 JAR 생성
./gradlew shadowJar

# 실행
java -jar build/libs/code-flow-tracer.jar --path /your/project/src
```

### CLI 옵션
```bash
# 전체 프로젝트 분석
java -jar code-flow-tracer.jar -p /path/to/project

# 특정 URL만 분석
java -jar code-flow-tracer.jar -p /path/to/project -u "/api/user/*"

# 엑셀로 출력
java -jar code-flow-tracer.jar -p /path/to/project -f excel -o result.xlsx

# GUI 모드
java -jar code-flow-tracer.jar --gui
```

---

## 기술 스택

| 구분 | 기술 | 용도 |
|------|------|------|
| Language | Java 17+ | 모던 Java 문법 활용 |
| Build | Gradle 8.5 | 의존성 관리, Shadow JAR |
| 파싱 | JavaParser | Java 소스 AST 분석 |
| XML | JDOM2 | iBatis/MyBatis XML 파싱 |
| Excel | Apache POI | 엑셀 출력 |
| CLI | Picocli | 명령줄 인터페이스 |
| GUI | Swing | Desktop GUI (폐쇄망 대응) |

### 왜 이 기술들을 선택했나요?

자세한 내용은 [DESIGN.md](docs/DESIGN.md) 참고

---

## 프로젝트 구조

```
code-flow-tracer/
├── CLAUDE.md                 # 프로젝트 규칙 (AI 어시스턴트용)
├── README.md                 # 이 문서
├── build.gradle
├── docs/                     # 문서
│   ├── PROJECT_PLAN.md      # 프로젝트 기획 (러너스하이)
│   ├── DESIGN.md            # 전체 설계
│   ├── USAGE.md             # 사용법
│   ├── TODO.md              # 할 일 목록
│   ├── DEV_LOG.md           # 개발 일지
│   └── ISSUES.md            # 문제 해결 기록
├── scripts/                  # 실행 스크립트
│   ├── run.bat              # GUI 모드 실행
│   └── analyze.bat          # CLI 모드 실행
├── src/
│   ├── main/java/com/codeflow/
│   │   ├── Main.java        # CLI 엔트리포인트
│   │   ├── parser/          # 소스 코드 파싱 ✅
│   │   ├── analyzer/        # 흐름 분석 ✅
│   │   ├── output/          # 출력 (콘솔 ✅, 엑셀 ⏳)
│   │   └── ui/              # Swing GUI (⏳ 예정)
│   └── test/java/
└── samples/                  # 테스트용 샘플 코드
```

---

## 문서

| 문서 | 설명 |
|------|------|
| [PROJECT_PLAN.md](docs/PROJECT_PLAN.md) | 프로젝트 기획, 러너스하이 계획 |
| [DESIGN.md](docs/DESIGN.md) | 전체 설계, 기술 선택 근거 |
| [USAGE.md](docs/USAGE.md) | 상세 사용법 |
| [TODO.md](docs/TODO.md) | 할 일 목록 |
| [DEV_LOG.md](docs/DEV_LOG.md) | 개발 일지 |
| [ISSUES.md](docs/ISSUES.md) | 문제 해결 기록 |

---

## 개발 배경

이 프로젝트는 **토스 러너스하이 2기** 참여를 위해 개발되었습니다.

4년간의 공공 SI/SM 경험에서 느낀 Pain Point:
- 레거시 코드 흐름 파악의 어려움
- 분기가 많으면 헷갈림
- API 호출부터 관련 소스 찾기가 번거로움
- 인수인계 문서화의 어려움

이 도구는 **다음 프로젝트에 투입될 때 바로 사용할 수 있도록** 만들었습니다.

---

## 기존 도구와의 차이점

### IntelliJ Call Hierarchy와 비교

"IntelliJ에 이미 Call Hierarchy 기능이 있지 않나요?" → 맞습니다. 하지만 **목적이 다릅니다.**

| 구분 | IntelliJ Call Hierarchy | Code Flow Tracer |
|------|------------------------|------------------|
| **탐색 방식** | 메서드 하나씩 수동 클릭 | URL 패턴으로 전체 흐름 자동 |
| **인터페이스→구현체** | Ctrl+Alt+B로 수동 이동 | 자동 매핑 |
| **URL→Controller** | 직접 연결 안 됨 | `/api/user/*` → Controller 자동 |
| **DAO→SQL** | XML 파일 수동 검색 | iBatis/MyBatis 자동 연결 |
| **결과 출력** | 화면에서만 확인 | 엑셀, 마크다운 문서화 |
| **일괄 분석** | ❌ 하나씩만 | ✅ 프로젝트 전체 |
| **폐쇄망** | IDE 설치 필요 | JAR 하나로 실행 |

**핵심 차이:**
- IntelliJ = **개발 중 탐색 도구** (한 메서드씩 수동)
- Code Flow Tracer = **분석/문서화 자동화** (전체 API 일괄)

### 유사 도구 비교

| 도구 | 특징 | Code Flow Tracer와 차이 |
|------|------|------------------------|
| [java-callgraph](https://github.com/gousiosg/java-callgraph) | 정적 콜그래프 생성 | 범용 콜그래프, 웹 계층 흐름 없음 |
| [Sourcetrail](https://www.sourcetrail.com/) | 코드 시각화 (2021 중단) | 범용 시각화, SQL 연동 없음 |
| [JArchitect](https://www.jarchitect.com/) | 코드 품질 분석 (상용) | 메트릭 중심, 흐름 추적 아님 |
| [SonarQube](https://www.sonarqube.org/) | 코드 품질/보안 | 품질 분석, 호출 흐름 없음 |
| [Understand](https://scitools.com/) | 정적 분석 (상용) | 범용 분석, 웹 계층 특화 아님 |
| [Eclipse CallGraph](https://marketplace.eclipse.org/content/callgraph-viewer) | Eclipse 플러그인 | Eclipse 전용, 문서화 없음 |

**Code Flow Tracer만의 특징:**
- ✅ **웹 애플리케이션 특화**: URL → Controller → Service → DAO → SQL 전체 흐름
- ✅ **iBatis/MyBatis 연동**: DAO에서 SQL 쿼리까지 추적
- ✅ **문서화 출력**: 엑셀, 마크다운으로 인수인계 자료 생성
- ✅ **전자정부프레임워크 최적화**: 국내 SI 환경 특화
- ✅ **폐쇄망 지원**: 단일 JAR, 외부 의존성 없음

---

## 지원 환경

### 분석 가능한 프로젝트
- 전자정부프레임워크
- Spring MVC / Spring Boot
- 일반 Java 프로젝트

### 분석 대상 Java 버전
Java 1.4 ~ 21 모두 지원 (도구 실행은 Java 17+ 필요)

### 지원 SQL 매퍼
- iBatis (sqlMap XML)
- MyBatis (mapper XML)

