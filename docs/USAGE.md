# 사용법 (Usage Guide)

> 최종 수정일: 2025-12-25

## 1. 설치

### 1.1 요구사항

**실행 환경:**
- Java 17 이상 (도구 실행용)
- (개발 시) Gradle 8.5 이상

**분석 대상:**
- Java 1.4 ~ 21 소스 코드 (파싱만 하므로 버전 무관)

> ⚠️ **주의**: 분석 대상 프로젝트의 Java 버전과 도구 실행 환경은 별개입니다.
> - 서버가 Java 1.4여도 **개발 PC에 Java 17+만 있으면** 사용 가능
> - 소스 코드는 "텍스트로 파싱"하는 것이지 "실행"하는 게 아님

### 1.2 SI 현장/폐쇄망 환경 설치

#### 방법 1: Portable JDK 사용 (설치 권한 없을 때)
```bash
# 1. Adoptium에서 JDK 17 ZIP 다운로드
#    https://adoptium.net/temurin/releases/
#    → Windows x64 → ZIP 선택

# 2. USB로 현장 반입 후 압축 해제
C:\tools\jdk-17\

# 3. 설치 없이 직접 실행
C:\tools\jdk-17\bin\java -jar code-flow-tracer.jar -p ./project
```

#### 방법 2: 기존 Java와 공존
```bash
# 프로젝트용 Java (기존)
set JAVA_HOME=C:\java\jdk1.8

# 도구 실행 시만 Java 17 사용
C:\tools\jdk-17\bin\java -jar code-flow-tracer.jar -p ./project
```

#### 방법 3: 배치 파일 작성
```batch
@echo off
REM run-tracer.bat
set TOOL_JAVA=C:\tools\jdk-17\bin\java
%TOOL_JAVA% -jar code-flow-tracer.jar %*
```

### 1.3 빌드
```bash
# 프로젝트 클론
git clone https://github.com/your-repo/code-flow-tracer.git
cd code-flow-tracer

# 빌드
./gradlew build

# 단일 실행 JAR 생성
./gradlew shadowJar
```

빌드 결과물: `build/libs/code-flow-tracer.jar`

---

## 2. CLI 사용법

### 2.1 기본 사용
```bash
java -jar code-flow-tracer.jar --path <프로젝트경로>
```

### 2.2 옵션

| 옵션 | 축약 | 설명 | 기본값 |
|------|------|------|--------|
| `--path` | `-p` | 분석할 프로젝트 경로 (필수) | - |
| `--url` | `-u` | 분석할 URL 패턴 | 전체 |
| `--class` | `-c` | 분석할 클래스명 | 전체 |
| `--format` | `-f` | 출력 형식 (console, excel, markdown) | console |
| `--output` | `-o` | 출력 파일 경로 | - |
| `--gui` | - | GUI 모드로 실행 | false |
| `--help` | `-h` | 도움말 출력 | - |
| `--version` | `-V` | 버전 출력 | - |

### 2.3 사용 예시

```bash
# 전체 프로젝트 분석 (콘솔 출력)
java -jar code-flow-tracer.jar -p /path/to/project

# 특정 URL 패턴만 분석
java -jar code-flow-tracer.jar -p /path/to/project -u "/api/user/*"

# 특정 Controller만 분석
java -jar code-flow-tracer.jar -p /path/to/project -c UserController

# 엑셀로 출력
java -jar code-flow-tracer.jar -p /path/to/project -f excel -o result.xlsx

# 마크다운으로 출력
java -jar code-flow-tracer.jar -p /path/to/project -f markdown -o result.md

# GUI 모드
java -jar code-flow-tracer.jar --gui
```

---

## 3. GUI 사용법

> ✅ **구현 완료** (MainFrame.java, ResultPanel.java)
>
> **v1.1 업데이트**: FlatLaf 다크 테마 적용, 콘솔 스타일 결과 패널, 설정 저장 기능

### 3.1 실행

**방법 1: 배치 파일 (권장)**
```bash
# scripts/run.bat 더블클릭
# 콘솔 창 없이 GUI만 실행됨
```

**방법 2: 명령줄**
```bash
# 콘솔 창 없이 실행
javaw -jar code-flow-tracer.jar --gui

# 콘솔 창과 함께 실행 (로그 확인용)
java -jar code-flow-tracer.jar --gui
```

### 3.2 화면 구성

**다크 테마 (FlatLaf Darcula)**
```
┌─ Code Flow Tracer ────────────────────────────────────────┐
│  ┌─ 프로젝트 경로 ─────────────────────────────────────┐  │
│  │ [C:\project\src        ▼]  [찾아보기...]            │  │
│  └─────────────────────────────────────────────────────┘  │
│  ┌─ 분석 옵션 ───────────────────────────────────────┐    │
│  │ URL 필터: [          ]  출력 스타일: [normal▼]    │    │
│  └───────────────────────────────────────────────────┘    │
│                              [엑셀 저장] [분석 시작] [⚙]  │
│  ┌─ 분석 결과 (콘솔 스타일) ───────────────────────────┐  │
│  │ ┌──────────────────────────────────────────────┐   │  │
│  │ │  Code Flow Tracer - 호출 흐름 분석 결과      │   │  │
│  │ └──────────────────────────────────────────────┘   │  │
│  │                                                     │  │
│  │ └── [Controller] UserController.selectUserList()   │  │
│  │     └── [Service] UserServiceImpl.selectUserList() │  │
│  │         └── [DAO] UserDAO.selectUserList()         │  │
│  │             ┌─ SQL ────────────────────────────┐   │  │
│  │             │ SELECT * FROM TB_USER            │   │  │
│  │             └──────────────────────────────────┘   │  │
│  └─────────────────────────────────────────────────────┘  │
│  [====================] 분석 완료: 11개 엔드포인트        │
└───────────────────────────────────────────────────────────┘
```

- **프로젝트 경로**: 드롭다운으로 최근 경로 선택 가능 (최대 10개 기억)
- **⚙ 버튼**: 설정 메뉴 (설정 초기화 등)

### 3.3 기능

| 기능 | 설명 |
|------|------|
| **프로젝트 선택** | 드롭다운에서 최근 경로 선택 또는 직접 입력 |
| **URL 필터** | 특정 URL 패턴만 분석 (예: `/api/user/*`) |
| **출력 스타일** | compact, normal, detailed 선택 |
| **분석 시작** | 백그라운드에서 분석 실행, 진행률 표시 |
| **엑셀 저장** | 분석 결과를 엑셀 파일로 저장 |
| **텍스트 복사** | 결과 영역에서 드래그 선택 후 Ctrl+C |
| **폰트 크기 조절** | 결과 영역에서 Ctrl+휠로 확대/축소 (9px~24px) |
| **패널 리사이즈** | URL 목록 패널 경계 드래그하여 크기 조절 |
| **설정 저장** | 최근 경로, URL 필터, 출력 스타일 자동 저장 |
| **설정 초기화** | ⚙ 버튼 → 저장된 설정 삭제 (레지스트리 정리) |

### 3.4 색상 구분 (다크 테마)

| 레이어 | 색상 | Hex 코드 |
|--------|------|----------|
| Controller | 청록 | #4EC9B0 |
| Service | 파랑 | #569CD6 |
| DAO | 보라 | #C586C0 |
| SQL | 주황 | #CE9178 |
| 인터페이스 정보 | 연파랑 | #9CDCFE |
| 다중 구현체 경고 | 빨강 (굵게) | #F44747 |

> VS Code 터미널 색상 팔레트 참고

### 3.5 다중 구현체 경고

인터페이스에 여러 구현체가 있을 경우 경고 표시:
```
[Service] UserServiceImpl.selectUser()  ← UserService  (외 UserServiceV2, UserServiceV3)
```
- `← UserService`: 구현 대상 인터페이스 (회색)
- `(외 ...)`: 다른 구현체 존재 경고 (빨강)

---

## 4. 출력 형식

### 4.1 콘솔 출력

> ✅ **구현 완료** (ConsoleOutput.java)

```
┌──────────────────────────────────────────────────┐
│     Code Flow Tracer - 호출 흐름 분석 결과       │
└──────────────────────────────────────────────────┘

[ 분석 요약 ]
  전체 클래스: 4개
    Controller: 1개
    Service:    1개
    DAO:        1개

[ 호출 흐름 ]
─── 1/2 ────────────────────────────────────
└── [Controller] UserController.selectUserList()  [GET] /user/list.do
    └── [Service] UserServiceImpl.selectUserList()
        └── [DAO] UserDAO.selectUserList()

─── 2/2 ────────────────────────────────────
└── [Controller] UserController.selectUser()  [GET] /user/detail.do
    └── [Service] UserServiceImpl.selectUser()
        └── [DAO] UserDAO.selectUser()
```

**출력 스타일 옵션:**
- `COMPACT`: 클래스.메서드만 출력
- `NORMAL`: 타입 태그 + URL 매핑 포함 (기본값)
- `DETAILED`: SQL ID 등 모든 정보 포함

**색상 지원:**
- Controller: 녹색
- Service: 파랑
- DAO: 보라
- HTTP 메서드: GET(녹색), POST(노랑), DELETE(빨강)

### 4.2 엑셀 출력
| URL | HTTP | Controller | Service | DAO | SQL ID | SQL |
|-----|------|------------|---------|-----|--------|-----|
| /user/list.do | GET | UserController.selectUserList | UserServiceImpl.selectUserList | UserDAO.selectUserList | userDAO.selectUserList | SELECT... |

### 4.3 마크다운 출력
```markdown
# API 호출 흐름

## GET /user/list.do

| Layer | Class | Method |
|-------|-------|--------|
| Controller | UserController | selectUserList() |
| Service | UserServiceImpl | selectUserList() |
| DAO | UserDAO | selectUserList() |

**SQL:**
\`\`\`sql
SELECT USER_ID, USER_NAME FROM TB_USER WHERE USE_YN = 'Y'
\`\`\`
```

---

## 5. 지원 환경

### 5.1 분석 가능한 프로젝트
- 전자정부프레임워크
- Spring MVC
- Spring Boot
- 일반 Java 프로젝트

### 5.2 지원 Java 버전
- 분석 대상: Java 1.4 ~ 21
- 도구 실행: Java 17+

### 5.3 지원 SQL 매퍼
- iBatis (sqlMap XML)
- MyBatis (mapper XML)

---

## 6. 문제 해결

### 6.1 파싱 오류
```
파싱 오류: /path/to/SomeClass.java
```
- 문법 오류가 있는 Java 파일
- 지원하지 않는 Java 버전 문법
- → 해당 파일 제외하고 계속 분석

### 6.2 메모리 부족
```bash
# JVM 힙 메모리 증가
java -Xmx2g -jar code-flow-tracer.jar -p /path/to/project
```

### 6.3 인코딩 문제
```bash
# UTF-8 강제 지정
java -Dfile.encoding=UTF-8 -jar code-flow-tracer.jar -p /path/to/project
```

---

## 7. FAQ

**Q: 분석 대상 프로젝트가 컴파일되지 않아도 되나요?**
A: 네, 소스 코드만 있으면 분석 가능합니다. 정적 분석 기반입니다.

**Q: 폐쇄망에서 사용 가능한가요?**
A: 네, JAR 파일 하나만 있으면 됩니다. 외부 네트워크 불필요.

**Q: MyBatis 어노테이션 방식도 지원하나요?**
A: 현재 MVP에서는 XML만 지원합니다. 향후 확장 예정.

**Q: Java 설치 없이 EXE 파일로 실행할 수 없나요?**
A: 향후 지원 예정입니다. 아래 옵션들을 검토 중:

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **jpackage** | Java 14+ 기본 제공, OS별 설치파일 생성 | 공식 도구, 안정적 | JRE 포함으로 용량 큼 (~150MB) |
| **jlink** | 필요한 모듈만 포함한 경량 JRE 생성 | 용량 최소화 (~40MB) | 모듈 의존성 분석 필요 |
| **Portable 배포** | JDK ZIP + JAR + 배치파일 | 설정 가장 간단 | 수동 압축 해제 필요 |

현재는 **Portable JDK + JAR** 조합을 권장합니다. 정식 릴리즈 시 jpackage로 설치파일 제공 예정.
