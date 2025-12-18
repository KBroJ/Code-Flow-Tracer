package com.codeflow.output;

import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.ParameterInfo;
import com.codeflow.parser.SqlInfo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 엑셀 출력 (Apache POI)
 *
 * FlowResult를 엑셀 파일로 출력합니다.
 *
 * 시트 구성:
 * 1. 요약 (Summary): 프로젝트 정보, 분석 통계
 * 2. 호출 흐름 (Call Flow): 평면 테이블 형식 (레이어별 컬럼 분리)
 * 3. SQL 목록 (SQL List): SQL 정보 목록
 */
public class ExcelOutput {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 스타일
    private CellStyle headerStyle;
    private CellStyle titleStyle;
    private CellStyle normalStyle;
    private CellStyle alternateStyle;  // 줄무늬용 연회색 스타일

    /**
     * FlowResult를 엑셀 파일로 저장
     *
     * @param result 분석 결과
     * @param outputPath 출력 파일 경로
     */
    public void export(FlowResult result, Path outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 스타일 초기화
            initStyles(workbook);

            // 시트 생성
            createSummarySheet(workbook, result);
            createCallFlowSheet(workbook, result);
            createSqlListSheet(workbook, result);

            // 파일 저장
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fos);
            }
        }
    }

    /**
     * 셀 스타일 초기화
     */
    private void initStyles(Workbook workbook) {
        // 헤더 스타일 (굵은 글씨, 배경색)
        headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 타이틀 스타일
        titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // 일반 스타일 (흰색 배경)
        normalStyle = workbook.createCellStyle();
        normalStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        normalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        normalStyle.setBorderBottom(BorderStyle.THIN);
        normalStyle.setBorderTop(BorderStyle.THIN);
        normalStyle.setBorderLeft(BorderStyle.THIN);
        normalStyle.setBorderRight(BorderStyle.THIN);
        normalStyle.setWrapText(true);
        normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 줄무늬용 스타일 (연회색 배경)
        alternateStyle = workbook.createCellStyle();
        alternateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        alternateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        alternateStyle.setBorderBottom(BorderStyle.THIN);
        alternateStyle.setBorderTop(BorderStyle.THIN);
        alternateStyle.setBorderLeft(BorderStyle.THIN);
        alternateStyle.setBorderRight(BorderStyle.THIN);
        alternateStyle.setWrapText(true);
        alternateStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    /**
     * 요약 시트 생성
     */
    private void createSummarySheet(Workbook workbook, FlowResult result) {
        Sheet sheet = workbook.createSheet("요약");

        int rowNum = 0;

        // 타이틀
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Code Flow Tracer - 분석 결과");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        rowNum++; // 빈 줄

        // 프로젝트 정보
        createSummaryRow(sheet, rowNum++, "프로젝트 경로", result.getProjectPath());
        createSummaryRow(sheet, rowNum++, "분석 시간", result.getAnalyzedAt().format(DATE_FORMAT));

        rowNum++; // 빈 줄

        // 통계
        createSummaryRow(sheet, rowNum++, "전체 클래스 수", String.valueOf(result.getTotalClasses()));
        createSummaryRow(sheet, rowNum++, "Controller 수", String.valueOf(result.getControllerCount()));
        createSummaryRow(sheet, rowNum++, "Service 수", String.valueOf(result.getServiceCount()));
        createSummaryRow(sheet, rowNum++, "DAO 수", String.valueOf(result.getDaoCount()));
        createSummaryRow(sheet, rowNum++, "엔드포인트 수", String.valueOf(result.getEndpointCount()));

        if (result.getUnmappedCallCount() > 0) {
            createSummaryRow(sheet, rowNum++, "매핑 안 된 호출", String.valueOf(result.getUnmappedCallCount()));
        }

        // 열 너비 조정
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 50 * 256);
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(normalStyle);
    }

    /**
     * 호출 흐름 시트 생성 (평면 테이블 형식 - 레이어별 컬럼 분리)
     */
    private void createCallFlowSheet(Workbook workbook, FlowResult result) {
        Sheet sheet = workbook.createSheet("호출 흐름");

        // 헤더
        String[] headers = {"No", "HTTP", "URL", "파라미터", "Controller", "Service", "DAO", "SQL 파일", "SQL ID", "테이블", "쿼리"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 (각 플로우를 평면 테이블로 출력)
        int rowNum = 1;
        int flowNo = 1;
        for (FlowNode flow : result.getFlows()) {
            // Controller 파라미터 (기본 파라미터)
            Set<String> controllerParams = extractControllerParams(flow.getParameters());

            // 플로우를 평면 행들로 변환
            List<FlatFlowRow> flatRows = flattenFlow(flow);

            // 호출 단위로 색상 번갈아가며 적용 (홀수: 흰색, 짝수: 연회색)
            CellStyle rowStyle = (flowNo % 2 == 1) ? normalStyle : alternateStyle;

            for (FlatFlowRow flatRow : flatRows) {
                Row row = sheet.createRow(rowNum);

                // Controller 파라미터 + SQL 파라미터 합집합
                String paramStr = mergeParameters(controllerParams, flatRow.sqlParams);

                createCell(row, 0, String.valueOf(flowNo), rowStyle);
                createCell(row, 1, flow.getHttpMethod() != null ? flow.getHttpMethod() : "", rowStyle);
                createCell(row, 2, flow.getUrlMapping() != null ? flow.getUrlMapping() : "", rowStyle);
                createCell(row, 3, paramStr, rowStyle);
                createCell(row, 4, flatRow.controller, rowStyle);
                createCell(row, 5, flatRow.service, rowStyle);
                createCell(row, 6, flatRow.dao, rowStyle);
                createCell(row, 7, flatRow.sqlFile, rowStyle);
                createCell(row, 8, flatRow.sqlId, rowStyle);
                createCell(row, 9, flatRow.tables, rowStyle);
                // 쿼리 (너무 길면 잘라서 표시)
                String query = flatRow.query;
                if (query != null && query.length() > 500) {
                    query = query.substring(0, 500) + "...";
                }
                createCell(row, 10, query != null ? query : "", rowStyle);

                rowNum++;
            }
            flowNo++;
        }

        // 열 너비 조정
        sheet.setColumnWidth(0, 5 * 256);   // No
        sheet.setColumnWidth(1, 7 * 256);   // HTTP
        sheet.setColumnWidth(2, 25 * 256);  // URL
        sheet.setColumnWidth(3, 30 * 256);  // 파라미터
        sheet.setColumnWidth(4, 30 * 256);  // Controller
        sheet.setColumnWidth(5, 30 * 256);  // Service
        sheet.setColumnWidth(6, 30 * 256);  // DAO
        sheet.setColumnWidth(7, 18 * 256);  // SQL 파일
        sheet.setColumnWidth(8, 22 * 256);  // SQL ID
        sheet.setColumnWidth(9, 25 * 256);  // 테이블
        sheet.setColumnWidth(10, 60 * 256); // 쿼리

        // 필터 추가
        if (rowNum > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowNum - 1, 0, headers.length - 1));
        }
    }

    /**
     * Controller 파라미터에서 실제 사용 필드 추출
     */
    private Set<String> extractControllerParams(List<ParameterInfo> parameters) {
        Set<String> result = new LinkedHashSet<>();

        if (parameters == null || parameters.isEmpty()) {
            return result;
        }

        for (ParameterInfo param : parameters) {
            // Spring 자동 주입 파라미터는 제외
            if (param.isSpringInjected()) {
                continue;
            }

            // @RequestParam, @PathVariable 파라미터는 파라미터명 그대로 사용
            if (param.isRequestParameter()) {
                result.add(param.getName());
            }
            // VO/Map 타입이고 사용 필드가 있으면 사용 필드 추가
            else if (param.hasUsedFields()) {
                result.addAll(param.getUsedFields());
            }
            // 기본 타입(String, int 등)은 파라미터명 사용
            else if (param.isPrimitiveOrWrapper()) {
                result.add(param.getName());
            }
        }

        return result;
    }

    /**
     * Controller 파라미터와 SQL 파라미터를 합집합으로 병합
     */
    private String mergeParameters(Set<String> controllerParams, List<String> sqlParams) {
        Set<String> merged = new LinkedHashSet<>();

        // Controller 파라미터 먼저 추가
        if (controllerParams != null) {
            merged.addAll(controllerParams);
        }

        // SQL 파라미터 추가 (중복 제거됨)
        if (sqlParams != null) {
            merged.addAll(sqlParams);
        }

        return merged.isEmpty() ? "-" : String.join(", ", merged);
    }

    /**
     * 트리 구조의 FlowNode를 평면 행 목록으로 변환
     */
    private List<FlatFlowRow> flattenFlow(FlowNode rootNode) {
        List<FlatFlowRow> rows = new ArrayList<>();
        flattenFlowRecursive(rootNode, new FlowPath(), rows);
        return rows;
    }

    /**
     * 재귀적으로 FlowNode 트리를 평면화
     */
    private void flattenFlowRecursive(FlowNode node, FlowPath currentPath, List<FlatFlowRow> rows) {
        // 현재 노드의 레이어에 따라 경로 업데이트
        FlowPath newPath = currentPath.copy();

        if (node.getClassType() != null) {
            String methodCall = node.getClassName() + "." + node.getMethodName() + "()";

            switch (node.getClassType()) {
                case CONTROLLER:
                    newPath.controller = methodCall;
                    break;
                case SERVICE:
                    newPath.service = methodCall;
                    break;
                case DAO:
                    newPath.dao = methodCall;
                    // SQL 정보가 있으면 추가
                    if (node.hasSqlInfo()) {
                        SqlInfo sqlInfo = node.getSqlInfo();
                        newPath.sqlFile = sqlInfo.getFileName();
                        newPath.sqlId = sqlInfo.getSqlId();
                        newPath.tables = sqlInfo.getTablesAsString();
                        newPath.query = sqlInfo.getQuery();
                        // SQL 파라미터 추가
                        if (sqlInfo.hasSqlParameters()) {
                            newPath.sqlParams = new ArrayList<>(sqlInfo.getSqlParameters());
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        // 자식이 없으면 현재 경로를 행으로 추가 (리프 노드)
        if (node.getChildren().isEmpty()) {
            rows.add(newPath.toFlatRow());
        } else {
            // 자식 노드 처리
            for (FlowNode child : node.getChildren()) {
                flattenFlowRecursive(child, newPath, rows);
            }
        }
    }

    /**
     * SQL 목록 시트 생성
     */
    private void createSqlListSheet(Workbook workbook, FlowResult result) {
        Sheet sheet = workbook.createSheet("SQL 목록");

        // 헤더
        String[] headers = {"No", "SQL 파일", "SQL ID", "타입", "테이블", "쿼리"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 모든 FlowNode에서 SQL 정보 추출
        List<SqlInfo> sqlInfoList = new ArrayList<>();
        for (FlowNode flow : result.getFlows()) {
            collectSqlInfo(flow, sqlInfoList);
        }

        // 데이터 출력
        int rowNum = 1;
        for (SqlInfo sqlInfo : sqlInfoList) {
            Row row = sheet.createRow(rowNum);
            CellStyle style = (rowNum % 2 == 1) ? normalStyle : alternateStyle;

            createCell(row, 0, String.valueOf(rowNum), style);
            createCell(row, 1, sqlInfo.getFileName(), style);
            createCell(row, 2, sqlInfo.getSqlId(), style);
            createCell(row, 3, sqlInfo.getType().name(), style);
            createCell(row, 4, sqlInfo.getTablesAsString(), style);
            // 쿼리 (너무 길면 잘라서 표시)
            String query = sqlInfo.getQuery();
            if (query != null && query.length() > 1000) {
                query = query.substring(0, 1000) + "...";
            }
            createCell(row, 5, query != null ? query : "", style);

            rowNum++;
        }

        // 열 너비 조정
        sheet.setColumnWidth(0, 5 * 256);   // No
        sheet.setColumnWidth(1, 20 * 256);  // SQL 파일
        sheet.setColumnWidth(2, 25 * 256);  // SQL ID
        sheet.setColumnWidth(3, 10 * 256);  // 타입
        sheet.setColumnWidth(4, 30 * 256);  // 테이블
        sheet.setColumnWidth(5, 80 * 256);  // 쿼리

        // 필터 추가
        if (rowNum > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowNum - 1, 0, headers.length - 1));
        }
    }

    /**
     * FlowNode에서 SQL 정보 재귀적으로 수집 (중복 제거)
     */
    private void collectSqlInfo(FlowNode node, List<SqlInfo> sqlInfoList) {
        if (node.hasSqlInfo()) {
            SqlInfo sqlInfo = node.getSqlInfo();
            // 중복 체크 (SQL ID 기준)
            boolean isDuplicate = sqlInfoList.stream()
                .anyMatch(s -> s.getFullSqlId().equals(sqlInfo.getFullSqlId()));
            if (!isDuplicate) {
                sqlInfoList.add(sqlInfo);
            }
        }

        // 자식 노드 처리
        for (FlowNode child : node.getChildren()) {
            collectSqlInfo(child, sqlInfoList);
        }
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * 호출 경로 정보 (평면화 과정에서 사용)
     */
    private static class FlowPath {
        String controller = "";
        String service = "";
        String dao = "";
        String sqlFile = "";
        String sqlId = "";
        String tables = "";
        String query = "";
        List<String> sqlParams = new ArrayList<>();

        FlowPath copy() {
            FlowPath copy = new FlowPath();
            copy.controller = this.controller;
            copy.service = this.service;
            copy.dao = this.dao;
            copy.sqlFile = this.sqlFile;
            copy.sqlId = this.sqlId;
            copy.tables = this.tables;
            copy.query = this.query;
            copy.sqlParams = new ArrayList<>(this.sqlParams);
            return copy;
        }

        FlatFlowRow toFlatRow() {
            return new FlatFlowRow(controller, service, dao, sqlFile, sqlId, tables, query, sqlParams);
        }
    }

    /**
     * 평면화된 호출 흐름 행
     */
    private static class FlatFlowRow {
        final String controller;
        final String service;
        final String dao;
        final String sqlFile;
        final String sqlId;
        final String tables;
        final String query;
        final List<String> sqlParams;

        FlatFlowRow(String controller, String service, String dao,
                   String sqlFile, String sqlId, String tables, String query, List<String> sqlParams) {
            this.controller = controller;
            this.service = service;
            this.dao = dao;
            this.sqlFile = sqlFile;
            this.sqlId = sqlId;
            this.tables = tables;
            this.query = query;
            this.sqlParams = sqlParams;
        }
    }
}
