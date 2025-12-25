# VERSION - 버전 관리

> CFT (Code Flow Tracer) 버전 히스토리

## 현재 버전: v1.0.0

---

## 버전 규칙 (Semantic Versioning)

```
MAJOR.MINOR.PATCH (예: 1.2.3)

MAJOR: 하위 호환 안 되는 큰 변경
MINOR: 하위 호환되는 기능 추가
PATCH: 버그 수정
```

---

## 버전 히스토리

### v1.0.0 (2025-12-25) - 첫 정식 릴리즈

#### 핵심 기능
- Java 소스 코드 파싱 (Java 1.4 ~ 21 지원)
- 호출 흐름 분석 (Controller → Service → DAO)
- 다중 구현체 감지 및 경고
- iBatis/MyBatis XML 파싱

#### 출력
- Console 출력 (트리 형태)
- Excel 출력 (호출 흐름, URL 매핑, 요약)

#### 인터페이스
- CLI (Picocli)
- Swing GUI (FlatLaf 다크 테마)
- 폰트 크기 조절 (Ctrl + 휠)
- 패널 리사이즈 (JSplitPane)

---

### v0.1.0 (2025-12-20) - 내부 개발

- 프로젝트 초기 설정
- 기본 파싱 기능

---

## 다음 버전 계획

| 버전 | 예정 기능 |
|------|----------|
| v1.1.0 | jlink 경량 JRE |
| v1.2.0 | Mermaid 다이어그램 |
| v2.0.0 | 쿼리 튜닝 어드바이저 |

---

## 변경 유형 범례

| 태그 | 의미 |
|------|------|
| Added | 새 기능 |
| Changed | 기존 기능 변경 |
| Fixed | 버그 수정 |
| Removed | 기능 제거 |
