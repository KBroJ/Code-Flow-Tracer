package com.codeflow.parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 파싱된 Java 클래스 정보
 */
public class ParsedClass {

    private Path filePath;
    private String packageName;
    private String className;
    private ClassType classType;
    private List<ParsedMethod> methods = new ArrayList<>();

    // Getters and Setters
    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public List<ParsedMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<ParsedMethod> methods) {
        this.methods = methods;
    }

    public void addMethod(ParsedMethod method) {
        this.methods.add(method);
    }

    /**
     * 전체 클래스명 (패키지 포함)
     */
    public String getFullClassName() {
        if (packageName == null || packageName.isEmpty()) {
            return className;
        }
        return packageName + "." + className;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%d methods)",
            classType.getDisplayName(),
            getFullClassName(),
            methods.size());
    }
}
