# 사용법 (Usage Guide)

> 최종 수정일: 2025-12-17

## 1. 설치

### 1.1 요구사항
- Java 17 이상
- (개발 시) Gradle 8.5 이상

### 1.2 빌드
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

> ⚠️ GUI는 Week 3에 구현 예정

### 3.1 실행
```bash
java -jar code-flow-tracer.jar --gui
```

또는 JAR 파일 더블클릭

### 3.2 화면 구성
```
┌─ Code Flow Tracer ─────────────────────────┐
│  프로젝트 경로: [                ] [찾기]  │
│  분석 대상:  ● URL 패턴  [/api/user/*    ] │
│  출력 형식:  [v] 콘솔  [v] 엑셀  [ ] MD    │
│  [분석 시작]                               │
│  ┌─ 결과 ────────────────────────────────┐ │
│  │ UserController.getList()              │ │
│  │   └→ UserService.findAll()            │ │
│  │        └→ UserDAO.selectUserList      │ │
│  └───────────────────────────────────────┘ │
│  [엑셀 저장]  [클립보드 복사]              │
└────────────────────────────────────────────┘
```

---

## 4. 출력 형식

### 4.1 콘솔 출력
```
=== 호출 흐름 분석 결과 ===

[GET /user/list.do]
UserController.selectUserList()
  └→ userService.selectUserList()
      └→ UserServiceImpl.selectUserList()
          └→ userDAO.selectUserList()
              └→ SQL: SELECT USER_ID, USER_NAME FROM TB_USER WHERE USE_YN = 'Y'

[GET /user/detail.do]
UserController.selectUser()
  └→ userService.selectUser()
      └→ UserServiceImpl.selectUser()
          └→ userDAO.selectUser()
              └→ SQL: SELECT * FROM TB_USER WHERE USER_ID = ?
```

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
