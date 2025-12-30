package com.codeflow.parser;

/**
 * 클래스 타입 열거형
 *
 * 전자정부프레임워크 및 Spring MVC 구조에서의 클래스 역할을 나타냅니다.
 */
public enum ClassType {
    CONTROLLER("Controller"),
    SERVICE("Service"),
    DAO("DAO/Repository"),
    COMPONENT("Component"),
    OTHER("Other");

    private final String displayName;

    ClassType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
