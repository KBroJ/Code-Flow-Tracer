package com.codeflow.parser;

/**
 * 클래스 타입 열거형
 *
 * 전자정부프레임워크 및 Spring MVC 구조에서의 클래스 역할을 나타냅니다.
 */
public enum ClassType {
    CONTROLLER("Controller", "요청을 받아 Service를 호출"),
    SERVICE("Service", "비즈니스 로직 처리"),
    DAO("DAO/Repository", "데이터 접근 계층"),
    COMPONENT("Component", "일반 컴포넌트"),
    OTHER("Other", "분류되지 않은 클래스");

    private final String displayName;
    private final String description;

    ClassType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
