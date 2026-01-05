# Week 3: GUI 구현 및 배포

> 2025-12-22 (일) ~ 2025-12-26 (목)
> Session 9-17

---

## Session 9: 블로그 글 작성 및 테스트 샘플 확장

**문제**: 프로젝트를 외부에 알리고 복잡한 시나리오를 테스트해야 한다
**해결**: 러너스하이 블로그 1편 작성 + 샘플 코드 확장

### 오늘 한 일
1. 러너스하이 블로그 1편 작성 (티스토리 #54)
2. 주문(Order) 도메인 샘플 추가 (Controller, Service, DAO, SQL)
3. 연관 도메인 추가 (Product, Stock, Payment, Delivery)
4. .gitignore에 docs/blog/ 제외

### 배운 점
- 결과물 스크린샷이 블로그 신뢰도를 높여줌
- 구체적인 프로젝트명이 추상적 표현보다 설득력 있음

---

## Session 10: README 데모 섹션 추가

**문제**: GitHub 방문자가 도구의 실제 결과물을 확인하기 어렵다
**해결**: README에 스크린샷 추가 + IDE 경고 수정

### 오늘 한 일
1. README.md "실행 결과" 섹션 추가 (콘솔/엑셀 스크린샷)
2. assets/ 폴더 생성 및 이미지 캡처 가이드
3. FlowAnalyzer.java switch문 경고 수정 (COMPONENT, OTHER case 추가)

### 배운 점
- README 첫인상이 프로젝트 평가에 중요
- switch문에서 default 대신 모든 case 명시가 더 안전

---

## Session 11: 다중 구현체 경고 기능 구현

**문제**: 인터페이스에 여러 구현체가 있을 때 첫 번째만 사용됨 (정적 분석 한계)
**해결**: 해결 대신 경고하여 사용자가 확인하도록 유도

### 오늘 한 일
1. FlowAnalyzer에 multipleImplWarnings 필드 추가
2. 콘솔 출력: Service 노드 옆에 인라인 경고 `(외 UserServiceV2, V3)`
3. 엑셀 출력: 연한 살구색 강조 + 비고 칼럼 + 요약 시트 경고 섹션
4. 테스트 샘플 확장 (UserServiceV2, V3 추가)

### 핵심 결정
- **경고 위치**: 상단 요약 → Service 노드 인라인 (어느 서비스가 문제인지 명확)
- **정적 분석 한계 인정**: 런타임에 어떤 구현체가 주입되는지 알 수 없음

### 배운 점
- 정적 분석 도구는 한계를 명확히 알리는 것이 중요
- 경고 위치가 UX에 큰 영향을 미침

---

## Session 12: Swing GUI 구현

**문제**: CLI만으로는 비개발자 사용이 어렵다
**해결**: Swing GUI 구현 (JTree + SwingWorker)

### 오늘 한 일
1. MainFrame.java 구현 (1200x800, 시스템 룩앤필)
2. 프로젝트 경로 선택 UI (JFileChooser)
3. ResultPanel.java (JTree로 호출 흐름 시각화)
4. SwingWorker로 백그라운드 분석 + ProgressBar
5. 엑셀 저장 기능 (JFileChooser)
6. --gui 옵션으로 GUI 모드 실행

### 트러블슈팅
| Issue | 문제 | 해결 |
|-------|------|------|
| #009 | JTree 한글 □ 표시 | Consolas → 맑은 고딕 |
| #010 | 텍스트 드래그 선택 불가 | JEditorPane + HTML 방식 |
| #011 | 창 닫아도 프로세스 남음 | WindowListener + System.exit(0) |

### 배운 점
- SwingWorker로 UI 블로킹 방지
- 다국어 UI에서는 한글 지원 폰트 사용 필수

---

## Session 13: GUI 개선 (다크 테마 + 콘솔 스타일)

**문제**: 기본 Swing UI가 촌스럽다 + 설정이 저장되지 않는다
**해결**: FlatLaf 다크 테마 + Java Preferences API

### 오늘 한 일
1. FlatLaf Darcula 테마 적용 (한 줄 코드로 모던 UI)
2. 다크 테마용 색상 수정 (VS Code 터미널 색상 팔레트 참고)
3. 콘솔 스타일 결과 패널 (CLI와 동일한 형식)
4. Preferences API로 설정 저장 (최근 경로 10개, URL 필터, 출력 스타일)

### 트러블슈팅
| Issue | 문제 | 해결 |
|-------|------|------|
| #012 | 박스 문자 정렬 불일치 | HTML `<table>` + CSS border |

### 핵심 결정
- **FlatLaf**: 한 줄로 모던 UI, IntelliJ Darcula 스타일
- **Preferences API**: Java 표준, OS별 적절한 위치에 자동 저장

### 배운 점
- 터미널 고정폭과 HTML monospace는 동작 방식이 다름
- CLI 출력을 GUI로 변환 시 환경 특성 고려 필요

---

## Session 14: GUI UX 개선 (리사이즈, 레이아웃, 폰트 조절)

**문제**: 좌측 패널 크기 조절 불가 + 분석 요약 레이아웃 어색함
**해결**: JSplitPane + Leader Dots + Ctrl+휠 폰트 조절

### 오늘 한 일
1. JSplitPane으로 드래그 리사이즈 구현
2. Leader Dots (점선 리더)로 빈 공간 시각적 연결
3. Ctrl+휠 폰트 크기 조절 (9px ~ 24px)

### 트러블슈팅
| Issue | 문제 | 해결 |
|-------|------|------|
| #013 | JSplitPane 패널 안 보임 | setVisible 대신 dividerLocation으로 제어 |
| #014 | 분석 요약 정렬 어색 | Leader dots (Word 목차 스타일) |

### 배운 점
- JSplitPane은 visibility 대신 divider 위치로 제어
- Ctrl+휠 이벤트 처리 시 e.consume()으로 스크롤 이벤트 차단 필요

---

## Session 15: jpackage 설치 파일 생성

**문제**: 폐쇄망에서 Java 없이 실행해야 한다
**해결**: jpackage로 JRE 번들 포함 설치파일 생성

### 오늘 한 일
1. 버전 관리 체계 수립 (VERSION.md, v1.0.0)
2. jpackage 설정 구현 (build.gradle)
3. WiX 3.14 설치 (WiX 6.0과 JDK 21 호환성 문제)
4. 한글 인코딩 문제 해결 (영문 description)
5. CFT-1.0.0.exe (77.3 MB) 생성 성공
6. exe 실행 문제 해결 (--arguments '--gui' 추가)
7. 설치 UI 커스터마이징 (바로가기 체크박스 간격)
8. 설치 삭제 시 레지스트리 자동 정리

### 트러블슈팅
| Issue | 문제 | 해결 |
|-------|------|------|
| #015 | WiX tools not found | WiX 6.0은 candle/light 없음 → WiX 3.14 |
| #016 | exit code 311 | description 한글 → 영문 변경 |
| #017 | 바로가기 체크박스 간격 | Y좌표 140→165로 수정 |
| #018 | exe 실행 무반응 | --arguments '--gui' 추가 |

### 배운 점
- jpackage는 OS별로 추가 도구 필요 (Windows: WiX)
- WiX 버전과 JDK 버전 간 호환성 매트릭스 존재
- CLI/GUI 겸용 앱 배포 시 기본 실행 인자 설정 필수

---

## Session 16: 문서 일관성 수정 및 아이콘 추가

**문제**: 문서가 최신 상태가 아님 + 아이콘이 없어서 완성도 낮아 보임
**해결**: 전체 문서 검토 + DALL-E 아이콘 생성

### 오늘 한 일
1. 모든 문서 파일 검토 및 일관성 수정
2. Markdown 출력 기능 제거 (폐쇄망에서 불필요)
3. DALL-E로 애플리케이션 아이콘 생성 (노드 흐름 디자인)
4. GUI 윈도우 + jpackage 설치파일 아이콘 적용

### 핵심 결정
- **노드 흐름 아이콘**: Controller→Service→DAO 흐름을 시각적으로 표현
- **PNG + ICO**: Swing은 PNG, jpackage는 ICO

### 배운 점
- 문서 일관성 관리는 기능 추가만큼 중요
- 아이콘 하나로 애플리케이션 완성도가 크게 향상

---

## Session 17: GitHub Release v1.0.0 발행

**문제**: 배포 파일을 사용자에게 전달해야 한다
**해결**: GitHub Release로 공식 배포

### 오늘 한 일
1. PR #13 머지 완료 (아이콘 + 문서 일관성)
2. shadowJar + jpackage 빌드
3. GitHub Release v1.0.0 생성 (CFT-1.0.0.exe, code-flow-tracer.jar)
4. 릴리즈 노트 작성 및 @ 기호 문제 수정

### 트러블슈팅
| Issue | 문제 | 해결 |
|-------|------|------|
| #019 | Gradle clean 시 파일 잠금 | jpackage 출력 경로를 build/release로 변경 |

### 배운 점
- Windows에서 exe 파일은 탐색기, 백신 등에 의해 잠길 수 있음
- GitHub 릴리즈 노트에서 @ 기호는 멘션으로 해석됨

---

## Week 3 완료!

- Swing GUI 구현 및 jpackage 배포
- v1.0.0 릴리즈 완료
- 다음: Week 4 세션 영속성 및 버그 수정
