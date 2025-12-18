package com.codeflow.output;

import com.codeflow.analyzer.FlowAnalyzer;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.JavaSourceParser;
import com.codeflow.parser.ParsedClass;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * ConsoleOutput 데모 (실행해서 출력 확인용)
 */
public class ConsoleOutputDemo {

    // UTF-8 PrintStream 싱글톤 (Java 10+)
    private static final PrintStream UTF8_OUT =
        new PrintStream(System.out, true, StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        Path samplesPath = Paths.get("samples");

        // 1. 파싱
        JavaSourceParser parser = new JavaSourceParser();
        List<ParsedClass> parsedClasses = parser.parseProject(samplesPath);

        // 2. 분석
        FlowAnalyzer analyzer = new FlowAnalyzer();
        FlowResult result = analyzer.analyze(samplesPath, parsedClasses);

        // 3. 출력
        UTF8_OUT.println("\n========== NORMAL 스타일 (색상 O) ==========\n");
        ConsoleOutput.colored().print(result);

        UTF8_OUT.println("\n========== DETAILED 스타일 ==========\n");
        ConsoleOutput.detailed().print(result);

        UTF8_OUT.println("\n========== COMPACT 스타일 ==========\n");
        ConsoleOutput.compact().print(result);
    }
}
