@echo off
REM ============================================
REM Code Flow Tracer - CLI 실행
REM ============================================
REM 사용법: analyze.bat -p <프로젝트경로> [옵션]
REM
REM 예시:
REM   analyze.bat -p C:\projects\my-app
REM   analyze.bat -p C:\projects\my-app -f excel -o result.xlsx
REM
REM [향후 개선 예정]
REM - jlink로 생성한 경량 JRE 번들 포함
REM - 현재는 시스템 Java 17+ 필요
REM ============================================

REM UTF-8 콘솔 출력 설정 (한글 깨짐 방지)
chcp 65001 > nul 2>&1

cd /d "%~dp0\.."

REM 번들 JDK가 있으면 사용, 없으면 시스템 Java 사용
if exist "jdk\bin\java.exe" (
    set JAVA_CMD=jdk\bin\java.exe
) else (
    set JAVA_CMD=java
)

%JAVA_CMD% -jar build\libs\code-flow-tracer.jar %*
