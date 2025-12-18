## 요약
<!-- 이 PR의 변경 내용을 간단히 요약해주세요 -->


## 변경 내용
<!--
변경된 모듈별로 접이식(details) 형식으로 작성
- 설명이 먼저, 파일명은 괄호 안에
- 신규 파일은 (신규) 표시
- 세부 항목에 커밋 SHA 기준 라인 링크 포함
-->

### 모듈명
<!--
가능한 모듈: parser, analyzer, output, ui, docs, test
변경 없는 모듈은 삭제
-->

<!--
<details>
<summary>변경 내용 설명 (<code>파일명.java</code>)</summary>

- 세부 변경 내용 ([#L시작-L끝](https://github.com/KBroJ/Code-Flow-Tracer/blob/{커밋SHA}/경로/파일.java#L시작-L끝))

</details>

<details>
<summary>(신규) 새 기능 설명 (<code>NewFile.java</code>)</summary>

- 세부 변경 내용 ([#L라인](링크))

</details>
-->


## 테스트 계획
- [ ] `./gradlew build` 성공
- [ ] `./gradlew test` 모든 테스트 통과
- [ ] `samples/` 샘플 코드로 동작 확인 (필요 시)

## 셀프 리뷰 체크리스트
- [ ] 패키지 구조 준수 (`com.codeflow.{parser,analyzer,output,ui}`)
- [ ] 코딩 컨벤션 준수 (네이밍, 들여쓰기, Javadoc)
- [ ] 새 기능 추가 시 테스트 코드 작성
- [ ] 문서 업데이트 (DEV_LOG.md, TODO.md 등)

## 라벨
<!-- PR 생성 시 적절한 라벨을 붙여주세요 -->
- 타입: `feature` / `bug` / `docs` / `refactor` / `test` / `chore`
- 모듈: `parser` / `analyzer` / `output` / `ui`

## 관련 이슈
<!-- 관련 이슈가 있다면 연결해주세요 (선택사항) -->
<!-- Closes #(이슈 번호) -->
