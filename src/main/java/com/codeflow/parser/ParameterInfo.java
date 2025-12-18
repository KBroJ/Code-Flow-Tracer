package com.codeflow.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * 메서드 파라미터 정보
 *
 * 파라미터 타입, 이름, 실제 사용되는 필드/키 정보를 저장합니다.
 */
public class ParameterInfo {

    private String name;           // 파라미터 이름 (예: userVO, params)
    private String type;           // 파라미터 타입 (예: UserVO, Map<String, Object>)
    private String simpleType;     // 단순 타입명 (예: UserVO, Map)
    private List<String> usedFields = new ArrayList<>();  // 사용된 필드/키 목록
    private boolean hasRequestParam;   // @RequestParam 어노테이션 여부
    private boolean hasPathVariable;   // @PathVariable 어노테이션 여부

    public ParameterInfo() {
    }

    public ParameterInfo(String name, String type) {
        this.name = name;
        this.type = type;
        this.simpleType = extractSimpleType(type);
    }

    /**
     * 제네릭 등을 제거한 단순 타입명 추출
     * 예: Map<String, Object> → Map
     */
    private String extractSimpleType(String type) {
        if (type == null) return "";
        int genericStart = type.indexOf('<');
        if (genericStart > 0) {
            return type.substring(0, genericStart);
        }
        return type;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        this.simpleType = extractSimpleType(type);
    }

    public String getSimpleType() {
        return simpleType;
    }

    public List<String> getUsedFields() {
        return usedFields;
    }

    public void setUsedFields(List<String> usedFields) {
        this.usedFields = usedFields;
    }

    public void addUsedField(String field) {
        if (!usedFields.contains(field)) {
            usedFields.add(field);
        }
    }

    /**
     * Map 타입인지 확인
     */
    public boolean isMapType() {
        return "Map".equals(simpleType) || "HashMap".equals(simpleType)
            || "LinkedHashMap".equals(simpleType) || "ConcurrentHashMap".equals(simpleType);
    }

    /**
     * 기본 타입인지 확인 (String, int, Integer 등)
     */
    public boolean isPrimitiveOrWrapper() {
        return simpleType.equals("String") || simpleType.equals("int") || simpleType.equals("Integer")
            || simpleType.equals("long") || simpleType.equals("Long")
            || simpleType.equals("boolean") || simpleType.equals("Boolean")
            || simpleType.equals("double") || simpleType.equals("Double")
            || simpleType.equals("float") || simpleType.equals("Float");
    }

    /**
     * VO/DTO 타입인지 확인 (Map도 기본타입도 아닌 경우)
     */
    public boolean isVoType() {
        return !isMapType() && !isPrimitiveOrWrapper();
    }

    /**
     * 사용 필드가 있는지 확인
     */
    public boolean hasUsedFields() {
        return usedFields != null && !usedFields.isEmpty();
    }

    public boolean isHasRequestParam() {
        return hasRequestParam;
    }

    public void setHasRequestParam(boolean hasRequestParam) {
        this.hasRequestParam = hasRequestParam;
    }

    public boolean isHasPathVariable() {
        return hasPathVariable;
    }

    public void setHasPathVariable(boolean hasPathVariable) {
        this.hasPathVariable = hasPathVariable;
    }

    /**
     * @RequestParam 또는 @PathVariable로 선언된 파라미터인지 확인
     */
    public boolean isRequestParameter() {
        return hasRequestParam || hasPathVariable;
    }

    /**
     * Spring 프레임워크가 자동 주입하는 파라미터인지 확인
     * (API 문서화에 불필요한 파라미터들)
     */
    public boolean isSpringInjected() {
        return simpleType.equals("Model") || simpleType.equals("ModelMap")
            || simpleType.equals("ModelAndView")
            || simpleType.equals("HttpServletRequest") || simpleType.equals("HttpServletResponse")
            || simpleType.equals("HttpSession")
            || simpleType.equals("RedirectAttributes")
            || simpleType.equals("BindingResult") || simpleType.equals("Errors")
            || simpleType.equals("Principal") || simpleType.equals("Authentication")
            || simpleType.equals("Locale") || simpleType.equals("TimeZone")
            || simpleType.equals("OutputStream") || simpleType.equals("Writer");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ").append(name);
        if (hasUsedFields()) {
            sb.append(" [").append(String.join(", ", usedFields)).append("]");
        }
        return sb.toString();
    }
}
