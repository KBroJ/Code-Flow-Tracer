# 진행 상황 (Development Log)

> 토스 러너스하이 2기 프로젝트 개발 일지

---

## Week 1

### Day 1 - 2025-12-17 (화)

#### 오늘 한 일
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

#### 배운 점
- JavaParser 라이브러리가 생각보다 강력함
- AST 기반 분석으로 정확한 메서드 호출 추출 가능
- 문서화를 먼저 해두면 방향성이 명확해짐

#### 내일 할 일
- FlowAnalyzer 구현 (클래스 간 호출 연결)
- 인터페이스 → 구현체 매핑 로직

#### 고민/질문
- 인터페이스와 구현체 매핑을 어떻게 할 것인가?
  - 방법 1: 클래스명 컨벤션 (UserService → UserServiceImpl)
  - 방법 2: @Service 어노테이션의 name 속성
  - → 두 방법 모두 지원하는 것으로 결정

---

### Day 2 - 2025-12-17 (화)

#### 오늘 한 일
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

#### 배운 점
- 재귀적 트리 구조 설계 시 순환 참조 방지가 중요
- scope(변수명)에서 클래스를 추정하는 휴리스틱이 효과적
- 인터페이스-구현체 매핑은 컨벤션 기반이 가장 실용적

#### 내일 할 일
- @RequestMapping URL 추출 개선
- URL 패턴 매칭 로직 강화
- 콘솔 출력 구현 시작

#### 고민/질문
- DAO에서 SQL ID를 정확히 추출하려면 추가 파싱 로직 필요
- IBatisParser 연동 시점을 Week 2로 유지할지 고민

---

### Day 2 추가 작업 - 2025-12-17 (화)

#### 오늘 한 일 (추가)
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

#### 배운 점
- 정적 분석은 AST 정보를 최대한 활용해야 정확도가 높아짐
- 네이밍 컨벤션 기반 추정은 fallback으로만 사용
- 문제를 발견하면 바로 기록하고 해결하는 것이 중요

---

## 주간 회고

### Week 1 회고 (예정)
- 잘한 점:
- 아쉬운 점:
- 다음 주 목표:

---

## 전체 타임라인

```
Week 1: 설계 + 기본 파서 ████████████░░░░░░░░ 60%
Week 2: iBatis + 출력   ░░░░░░░░░░░░░░░░░░░░ 0%
Week 3: GUI + 테스트    ░░░░░░░░░░░░░░░░░░░░ 0%
Week 4: 개선 + 회고     ░░░░░░░░░░░░░░░░░░░░ 0%
```

---

## 핵심 지표

| 지표 | 현재 | 목표 |
|------|------|------|
| 구현된 기능 | 3/10 | 10/10 |
| 테스트 통과율 | 100% | 100% |
| 문서 완성도 | 70% | 100% |
| 코드 커버리지 | - | 80% |
