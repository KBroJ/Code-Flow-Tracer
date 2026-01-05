# Week 4: 세션 영속성 및 기술 부채 청산

> 2025-12-30 (월) ~ 2025-12-31 (화)
> Session 18-21

---

## Session 18: 세션 영속성 구현 (#15)

**문제**: 앱을 닫으면 분석 결과가 사라진다
**해결**: Gson JSON 직렬화로 세션 저장/복원

### 오늘 한 일
1. GitHub 이슈 생성 (#15 세션 영속성, #16 작업 관리 탭)
2. v1.1 기능 계획 문서화 (TODO.md, DESIGN.md)
3. SessionData.java, SessionManager.java 구현
4. MainFrame.java 세션 복원/저장 연동

### 핵심 결정 - Gson 선택 이유
| 라이브러리 | 장점 | 단점 |
|-----------|------|------|
| **Gson** | 가볍고 간단, 한 줄로 변환 | 성능은 Jackson보다 낮음 |
| Jackson | 고성능, 풍부한 기능 | 의존성 복잡, 설정 많음 |

→ 세션 데이터 저장은 성능보다 편의성이 중요 → **Gson 선택**

### 핵심 결정 - 저장 경로
| 위치 | 장점 | 단점 |
|------|------|------|
| 설치 폴더 | 앱과 함께 관리 | Program Files 쓰기 권한 없음 |
| **홈 디렉토리** | 크로스 플랫폼, 권한 보장 | 숨김 폴더 관리 필요 |

→ `~/.code-flow-tracer/session.json` 선택

### LocalDateTime 직렬화 문제 해결
```java
private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        out.value(FORMATTER.format(value));  // ISO-8601
    }
}
```

### 배운 점
- Gson TypeAdapter 사용법 (Java 8 Date/Time API 별도 어댑터 필요)
- 세션 복원 시 유효성 검증 중요 (프로젝트 경로 삭제 가능성)
- 세션 저장 시점: 앱 종료 시 vs **분석 완료 시** (비정상 종료 대비)

---

## Session 19: 세션 영속성 TDD 테스트 및 설정 저장 방식 분석

**문제**: 테스트 없이 배포 + 설정이 Registry와 JSON에 이중 저장됨
**해결**: TDD 테스트 작성 + 저장 방식 분석

### 오늘 한 일
1. SessionManagerTest.java (11개 테스트)
2. SessionDataTest.java (12개 테스트)
3. WiX 설치 삭제 시 세션 폴더 정리 추가
4. 설정 저장 방식 분석 및 이중 저장 문제 발견

### 이중 저장 문제 발견!
```
┌─────────────────────────────────────────────────────────────┐
│                    설정 저장 구조 (Before)                    │
├─────────────────────────────────────────────────────────────┤
│  Registry (Preferences API)    │    JSON 파일               │
│  • 최근 경로                    │    • 분석 결과              │
│  • URL 필터  ◄─────────────────┼──► URL 필터 (중복!)         │
│  • 출력 스타일 ◄───────────────┼──► 출력 스타일 (중복!)      │
└─────────────────────────────────────────────────────────────┘
```

### 배운 점
- TDD 테스트 작성 중 API 사용법 오류 발견 (FlowResult 생성자)
- 설계 단계에서 저장 방식 통일 중요
- 기술 부채 인식 → 문서화하여 향후 리팩토링 계획

---

## Session 20: 기술 부채 청산 - Registry → JSON 단일 저장 통합

**문제**: 설정이 Registry와 JSON에 이중 저장 (기술 부채)
**해결**: JSON 단일 저장으로 통합 (즉시 해결)

### 의사결정 과정
1. 난이도 판단 (5분): 30분 작업 → "지금 하자"
2. 구현 (25분): 코드 변경
3. GUI 메뉴 통합: "설정 초기화" + "세션 삭제" → "설정/세션 초기화"

### 결과 (After)
```
┌─────────────────────────────────────────────────────────────┐
│  JSON 파일: ~/.code-flow-tracer/session.json                │
│  {                                                          │
│    "projectPath": "/path/to/project",                       │
│    "recentPaths": ["/path1", "/path2"],  ← 통합됨           │
│    "urlFilter": "/api/*",                                   │
│    "outputStyle": "normal",                                 │
│    "flowResult": { ... }                                    │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
장점: 단일 저장소, 크로스 플랫폼, 디버깅 용이, 백업 간편
```

### 코드 변경량
| 파일 | 추가 | 삭제 |
|------|------|------|
| SessionData.java | +23 | 0 |
| SessionManager.java | +35 | 0 |
| MainFrame.java | +30 | -40 |
| **합계** | +88 | -40 |

### 배운 점 (러너스하이 어필 포인트)
1. **"나중에" vs "지금"**: 어렵지 않으면 지금 하자 → 컨텍스트 손실 방지
2. **발견 → 분석 → 해결 사이클**: 30분 내 완료
3. **문서화의 가치**: 비교표 작성 → JSON이 정답임을 확인

---

## Session 21: URL 필터 통계 버그 수정 및 설치 삭제 시 세션 정리 개선

**문제**: URL 필터 적용 시 통계가 전체 기준 + 설치 삭제 시 세션 폴더 안 지워짐
**해결**: Flow 기반 통계 메서드 추가 + WiX CustomAction

### 오늘 한 일
1. Git 최신 변경사항 Pull (Session 19, 20)
2. 버그 발견: URL 필터 적용 시 분석 요약 통계 미반영 (Issue #023)
3. Flow 기반 통계 메서드 추가 (FlowResult.java)
4. 분석 시 요약 패널 깜빡임 수정
5. 설치 삭제 시 세션 데이터 유지 문제 해결 (Issue #024)

### Issue #023: URL 필터 통계 버그
- **증상**: URL 필터를 걸어도 Controller/Service/DAO 개수가 전체 프로젝트 기준
- **원인**: `result.getControllerCount()`가 전체 파싱된 클래스 기준
- **해결**: flows 기반 통계 메서드 추가
  - `getFlowBasedControllerCount()`: flows 내 Controller 수
  - `getFlowBasedServiceCount()`: flows 내 Service 수
  - `getFlowBasedDaoCount()`: flows 내 DAO 수

### Issue #024: 설치 삭제 시 세션 폴더 안 지워짐
- **시도한 방법** (모두 실패):
  - `util:RemoveFolderEx` → WiX 컴파일 오류
  - `RemoveFile Name="*"` → 레지스트리에만 등록, 실제 삭제 안 됨
- **진짜 원인**: WiX Directory 구조 문제 (ProfileFolder가 TARGETDIR 내부에 중첩)
- **최종 해결**: CustomAction으로 `cmd.exe /c rmdir` 직접 실행
  ```xml
  <CustomAction Id="RemoveSessionFolder"
                ExeCommand="cmd.exe /c if exist %USERPROFILE%\.code-flow-tracer rmdir /s /q ..." />
  ```

### 배운 점
- Flow 기반 통계 vs 전체 파싱 통계 구분 필요
- WiX Directory 중첩 문제: 특수 디렉토리는 TARGETDIR 외부에서 독립적으로 참조
- CustomAction이 복잡한 WiX Directory 설정보다 더 확실

---

## Week 4 완료!

- 세션 영속성 구현 및 기술 부채 청산
- URL 필터 통계 버그 수정
- 설치 삭제 시 세션 정리 개선
- 다음: Week 5 CRUD 분석 기능
