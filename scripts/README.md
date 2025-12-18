# 실행 스크립트

## 현재 상태

기본 구조만 작성됨. 시스템에 Java 17+ 설치 필요.

## 파일 설명

| 파일 | 용도 |
|------|------|
| `run.bat` | GUI 모드 실행 (더블클릭) |
| `analyze.bat` | CLI 모드 실행 |

## 사용법

```bash
# GUI 모드
scripts\run.bat

# CLI 모드
scripts\analyze.bat -p C:\projects\my-app
scripts\analyze.bat -p C:\projects\my-app -f excel -o result.xlsx
```

## 향후 계획

배포 시 아래 구조로 패키징 예정:

```
code-flow-tracer/
├── jdk/                    # jlink 경량 JRE (~40MB)
│   └── bin/java.exe
├── code-flow-tracer.jar
├── run.bat
└── analyze.bat
```

번들 JDK가 있으면 자동으로 사용, 없으면 시스템 Java 사용.
