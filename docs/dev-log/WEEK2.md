# Week 2: iBatis/MyBatis 파싱 및 Excel 출력

> 2025-12-18 (수) ~ 2025-12-22 (일)
> Session 5-8

---

## Session 5: IBatisParser 구현 및 DAO-SQL 연결

**문제**: SQL은 Java 코드에 없다. XML을 파싱해야 한다
**해결**: JDOM2 기반 XML 파싱 + DTD 검증 비활성화

### 오늘 한 일
1. SqlInfo 데이터 클래스 생성 (namespace, SQL ID, 타입, 테이블 목록, 쿼리)
2. IBatisParser 구현 (JDOM2 기반, iBatis/MyBatis 형식 지원)
3. DTD 검증 비활성화 (폐쇄망/오프라인 환경 대응)
4. SQL 쿼리에서 테이블명 자동 추출 (정규식)
5. DAO 메서드에서 SQL ID 추출
6. FlowAnalyzer + IBatisParser 연동

### 핵심 결정
- **DTD 비활성화**: 폐쇄망에서 외부 DTD 로드 시 타임아웃 발생
  ```java
  builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
  ```
- **SQL 정보 표시 모드 분리**: 실제 쿼리는 수백 줄 → 콘솔에서는 파일명+SQL ID만

### 배운 점
- JDOM2에서 외부 엔티티 로드 비활성화하는 방법
- iBatis(`sqlMap`)와 MyBatis(`mapper`) XML 구조 차이
- 정적 분석의 한계: 동적 SQL, 변수 기반 SQL ID는 추출 불가

---

## Session 6: ExcelOutput 구현

**문제**: 분석 결과를 산출물 형태로 출력해야 한다
**해결**: Apache POI로 3개 시트 구성 Excel 출력

### 오늘 한 일
1. ExcelOutput 클래스 구현 (Apache POI XSSF)
2. 3개 시트: 요약, API 목록, 호출 흐름
3. 레이어별 색상 구분 (Controller=녹색, Service=파랑, DAO=보라)
4. FlowNode에 filePath 필드 추가
5. Main.java 연동 (-o output.xlsx)

### 핵심 결정
- **3개 시트 분리**:
  - 요약: 경영진/PM 보고용
  - API 목록: API 문서 작성용
  - 호출 흐름: 개발자 분석용
- **레이어별 색상**: 많은 행에서 시각적 구분 용이

### 배운 점
- Apache POI로 엑셀 생성 (XSSFWorkbook, Sheet, Row, Cell)
- CellStyle로 셀 스타일링 (배경색, 테두리, 폰트)
- 자동 필터 설정 (setAutoFilter)

---

## Session 7: ExcelOutput 개선 및 파라미터 표시 고도화

**문제**: 계층 구조가 엑셀 필터링에 불편 + 파라미터가 Controller 것만 표시됨
**해결**: 평면 테이블 형식 + Controller 파라미터 + SQL 파라미터 합집합

### 오늘 한 일
1. 계층 구조 → 평면 테이블 형식으로 변경
2. 시트2 (API 목록) 제거 (자동 추출 불가능한 필드가 많음)
3. SQL 파라미터 추출 기능 추가 (#param#, #{param})
4. 파라미터 표시: Controller 파라미터 + SQL 파라미터 합집합
5. 기본 저장 경로/파일명 및 중복 처리 구현

### 핵심 결정 - 파라미터 표시 범위
```
Controller.getUser(userId, gubun)
├── if(gubun==1) → DAO1.select1() → SQL: #userId#
└── if(gubun==2) → DAO1.select2() → SQL: #centerCd#, #deptId#
```
- SQL 파라미터만? → 분기 조건 파라미터(gubun) 누락
- 모든 파라미터? → 과다 정보
- **결론**: Controller + SQL 파라미터 합집합 (실용적 범위)

### 배운 점
- 정적 분석의 한계: 런타임 분기, 죽은 코드 판별 불가
- 실용적 범위 설정의 중요성 - 모든 것보다 핵심 정보에 집중

---

## Session 8: ExcelOutput SQL 쿼리 표시 개선

**문제**: 동적 SQL 태그 정보가 누락됨 + 쿼리에 불필요한 들여쓰기
**해결**: XML 원본 형태 출력 + 공통 들여쓰기 제거

### 오늘 한 일
1. SQL 목록 시트 컬럼 순서 변경 (테이블 ↔ SQL 파라미터)
2. 동적 SQL 태그 원본 형태 출력 (텍스트만 → XML 태그 포함)
3. 쿼리 길이 제한 제거 (기존 1000자 → 전체 표시)
4. 공통 들여쓰기 제거 로직 개선 (trimPreservingStructure)

### 핵심 결정
- **XML 원본 형태**: 사용자 요청 "xml에 써져있는 그대로"
  ```xml
  <dynamic prepend="WHERE">
      <isNotEmpty property="categoryId">
          AND CATEGORY_ID = #categoryId#
      </isNotEmpty>
  </dynamic>
  ```
- **자동 줄바꿈 비활성화**: 많은 행을 한눈에 보기 위해

### 배운 점
- Excel 클립보드 동작: 줄바꿈 + 따옴표 있으면 `""` 이스케이프
- JDOM2 `getContent()`: 혼합 콘텐츠(텍스트 + 자식요소) 순서대로 순회
- 정적 분석 출력 형식은 사용자 피드백으로 개선 반복 필요

---

## Week 2 완료!

- IBatisParser + ExcelOutput 구현 완료
- ExcelOutput 고도화 (평면 테이블, 파라미터 개선, 동적 SQL)
- 다음: Week 3 Swing GUI
