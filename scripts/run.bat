@echo off
REM ============================================
REM Code Flow Tracer - GUI 실행
REM ============================================
REM 사용법: run.bat 더블클릭
REM
REM [향후 개선 예정]
REM - jlink로 생성한 경량 JRE 번들 포함
REM - 현재는 시스템 Java 17+ 필요
REM ============================================

REM UTF-8 콘솔 출력 설정 (한글 깨짐 방지)
chcp 65001 > nul 2>&1

cd /d "%~dp0\.."

REM 번들 JDK가 있으면 사용, 없으면 시스템 Java 사용
REM javaw 사용: 콘솔 창 없이 GUI만 실행
if exist "jdk\bin\javaw.exe" (
    set JAVA_CMD=jdk\bin\javaw.exe
) else (
    set JAVA_CMD=javaw
)

REM start 명령으로 배치 파일 창도 즉시 닫힘
start "" %JAVA_CMD% -jar build\libs\code-flow-tracer.jar --gui
