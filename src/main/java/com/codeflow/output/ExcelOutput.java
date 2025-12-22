package com.codeflow.output;

import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.parser.SqlInfo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        normalStyle.setWrapText(false);
        normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 줄무늬용 스타일 (연회색 배경)
        alternateStyle = workbook.createCellStyle();
        alternateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        alternateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        alternateStyle.setBorderBottom(BorderStyle.THIN);
        alternateStyle.setBorderTop(BorderStyle.THIN);
        alternateStyle.setBorderLeft(BorderStyle.THIN);
        alternateStyle.setBorderRight(BorderStyle.THIN);
        alternateStyle.setWrapText(false);
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

        // 헤더 (파일명/메소드명 분리, Service 인터페이스/구현체 분리)
        String[] headers = {"No", "HTTP", "URL",
                "Controller 파일", "Controller 메소드",
                "Service 파일", "ServiceImpl 파일", "ServiceImpl 메소드",
                "DAO 파일", "DAO 메소드",
                "SQL 파일"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 (각 플로우를 평면 테이블로 출력)
        int rowNum = 1;
        int flowNo = 1;  // URL 그룹 번호 (색상 구분용)
        for (FlowNode flow : result.getFlows()) {
            // 플로우를 평면 행들로 변환
            List<FlatFlowRow> flatRows = flattenFlow(flow);

            // 호출 단위로 색상 번갈아가며 적용 (홀수: 흰색, 짝수: 연회색)
            CellStyle rowStyle = (flowNo % 2 == 1) ? normalStyle : alternateStyle;

            for (FlatFlowRow flatRow : flatRows) {
                Row row = sheet.createRow(rowNum);

                // No는 순차 번호 (rowNum)
                createCell(row, 0, String.valueOf(rowNum), rowStyle);
                createCell(row, 1, flow.getHttpMethod() != null ? flow.getHttpMethod() : "", rowStyle);
                createCell(row, 2, flow.getUrlMapping() != null ? flow.getUrlMapping() : "", rowStyle);
                createCell(row, 3, flatRow.controllerFile, rowStyle);
                createCell(row, 4, flatRow.controllerMethod, rowStyle);
                createCell(row, 5, flatRow.serviceInterfaceFile, rowStyle);
                createCell(row, 6, flatRow.serviceImplFile, rowStyle);
                createCell(row, 7, flatRow.serviceImplMethod, rowStyle);
                createCell(row, 8, flatRow.daoFile, rowStyle);
                createCell(row, 9, flatRow.daoMethod, rowStyle);
                createCell(row, 10, flatRow.sqlFile, rowStyle);

                rowNum++;
            }
            flowNo++;
        }

        // 열 너비 조정 (파일명/메소드명 분리, Service 인터페이스/구현체 분리)
        sheet.setColumnWidth(0, 5 * 256);   // No
        sheet.setColumnWidth(1, 7 * 256);   // HTTP
        sheet.setColumnWidth(2, 25 * 256);  // URL
        sheet.setColumnWidth(3, 22 * 256);  // Controller 파일
        sheet.setColumnWidth(4, 22 * 256);  // Controller 메소드
        sheet.setColumnWidth(5, 18 * 256);  // Service 파일 (인터페이스)
        sheet.setColumnWidth(6, 22 * 256);  // ServiceImpl 파일
        sheet.setColumnWidth(7, 22 * 256);  // ServiceImpl 메소드
        sheet.setColumnWidth(8, 18 * 256);  // DAO 파일
        sheet.setColumnWidth(9, 18 * 256);  // DAO 메소드
        sheet.setColumnWidth(10, 18 * 256); // SQL 파일

        // 필터 추가
        if (rowNum > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowNum - 1, 0, headers.length - 1));
        }
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
     * 재귀적으로 FlowNode 트리를 평면화 (파일명/메소드명 분리)
     */
    private void flattenFlowRecursive(FlowNode node, FlowPath currentPath, List<FlatFlowRow> rows) {
        // 현재 노드의 레이어에 따라 경로 업데이트
        FlowPath newPath = currentPath.copy();

        if (node.getClassType() != null) {
            String fileName = node.getClassName() + ".java";
            String methodName = node.getMethodName() + "()";

            switch (node.getClassType()) {
                case CONTROLLER:
                    newPath.controllerFile = fileName;
                    newPath.controllerMethod = methodName;
                    break;
                case SERVICE:
                    // 구현체 파일과 메소드
                    newPath.serviceImplFile = fileName;
                    newPath.serviceImplMethod = methodName;
                    // 인터페이스 파일 (implementedInterfaces에서 첫 번째 항목)
                    List<String> interfaces = node.getImplementedInterfaces();
                    if (interfaces != null && !interfaces.isEmpty()) {
                        newPath.serviceInterfaceFile = interfaces.get(0) + ".java";
                    }
                    break;
                case DAO:
                    newPath.daoFile = fileName;
                    newPath.daoMethod = methodName;
                    // SQL 정보가 있으면 추가
                    if (node.hasSqlInfo()) {
                        SqlInfo sqlInfo = node.getSqlInfo();
                        newPath.sqlFile = sqlInfo.getFileName();
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
     * SQL 목록 시트 생성 (호출 URL, SQL 파라미터 포함)
     */
    private void createSqlListSheet(Workbook workbook, FlowResult result) {
        Sheet sheet = workbook.createSheet("SQL 목록");

        // 헤더 (호출 URL, SQL 파라미터 추가)
        String[] headers = {"No", "호출 URL", "SQL 파일", "SQL ID", "타입", "테이블", "SQL 파라미터", "쿼리"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 모든 FlowNode에서 SQL 정보 + URL 추출
        List<SqlWithUrl> sqlWithUrlList = new ArrayList<>();
        for (FlowNode flow : result.getFlows()) {
            String url = flow.getUrlMapping() != null ? flow.getUrlMapping() : "";
            collectSqlInfoWithUrl(flow, url, sqlWithUrlList);
        }

        // 데이터 출력 (URL 기준으로 색상 구분)
        int rowNum = 1;
        int urlColorIndex = 1;
        String previousUrl = null;
        for (SqlWithUrl sqlWithUrl : sqlWithUrlList) {
            // URL이 바뀌면 색상 인덱스 변경
            if (previousUrl != null && !previousUrl.equals(sqlWithUrl.url)) {
                urlColorIndex++;
            }
            previousUrl = sqlWithUrl.url;

            Row row = sheet.createRow(rowNum);
            CellStyle style = (urlColorIndex % 2 == 1) ? normalStyle : alternateStyle;

            SqlInfo sqlInfo = sqlWithUrl.sqlInfo;
            createCell(row, 0, String.valueOf(rowNum), style);
            createCell(row, 1, sqlWithUrl.url, style);
            createCell(row, 2, sqlInfo.getFileName(), style);
            createCell(row, 3, sqlInfo.getSqlId(), style);
            createCell(row, 4, sqlInfo.getType().name(), style);
            createCell(row, 5, sqlInfo.getTablesAsString(), style);
            // SQL 파라미터
            String sqlParams = sqlInfo.getSqlParametersAsString();
            createCell(row, 6, sqlParams.isEmpty() ? "-" : sqlParams, style);
            // 쿼리 (전체 표시)
            String query = sqlInfo.getQuery();
            createCell(row, 7, query != null ? query : "", style);

            rowNum++;
        }

        // 열 너비 조정
        sheet.setColumnWidth(0, 5 * 256);   // No
        sheet.setColumnWidth(1, 25 * 256);  // 호출 URL
        sheet.setColumnWidth(2, 16 * 256);  // SQL 파일
        sheet.setColumnWidth(3, 20 * 256);  // SQL ID
        sheet.setColumnWidth(4, 10 * 256);  // 타입
        sheet.setColumnWidth(5, 22 * 256);  // 테이블
        sheet.setColumnWidth(6, 35 * 256);  // SQL 파라미터
        sheet.setColumnWidth(7, 60 * 256);  // 쿼리

        // 필터 추가
        if (rowNum > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowNum - 1, 0, headers.length - 1));
        }
    }

    /**
     * FlowNode에서 SQL 정보 + URL 재귀적으로 수집 (URL별로 수집, 같은 URL 내 중복만 제거)
     */
    private void collectSqlInfoWithUrl(FlowNode node, String url, List<SqlWithUrl> sqlWithUrlList) {
        if (node.hasSqlInfo()) {
            SqlInfo sqlInfo = node.getSqlInfo();
            // 같은 URL + 같은 SQL ID 조합만 중복 제거
            boolean isDuplicate = sqlWithUrlList.stream()
                .anyMatch(s -> s.url.equals(url) && s.sqlInfo.getFullSqlId().equals(sqlInfo.getFullSqlId()));
            if (!isDuplicate) {
                sqlWithUrlList.add(new SqlWithUrl(url, sqlInfo));
            }
        }

        // 자식 노드 처리
        for (FlowNode child : node.getChildren()) {
            collectSqlInfoWithUrl(child, url, sqlWithUrlList);
        }
    }

    /**
     * SQL 정보 + 호출 URL 쌍
     */
    private static class SqlWithUrl {
        final String url;
        final SqlInfo sqlInfo;

        SqlWithUrl(String url, SqlInfo sqlInfo) {
            this.url = url;
            this.sqlInfo = sqlInfo;
        }
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * 호출 경로 정보 (평면화 과정에서 사용, 파일명/메소드명 분리, Service 인터페이스/구현체 분리)
     */
    private static class FlowPath {
        String controllerFile = "";
        String controllerMethod = "";
        String serviceInterfaceFile = "";  // Service 인터페이스 파일
        String serviceImplFile = "";       // ServiceImpl 구현체 파일
        String serviceImplMethod = "";     // ServiceImpl 메소드
        String daoFile = "";
        String daoMethod = "";
        String sqlFile = "";

        FlowPath copy() {
            FlowPath copy = new FlowPath();
            copy.controllerFile = this.controllerFile;
            copy.controllerMethod = this.controllerMethod;
            copy.serviceInterfaceFile = this.serviceInterfaceFile;
            copy.serviceImplFile = this.serviceImplFile;
            copy.serviceImplMethod = this.serviceImplMethod;
            copy.daoFile = this.daoFile;
            copy.daoMethod = this.daoMethod;
            copy.sqlFile = this.sqlFile;
            return copy;
        }

        FlatFlowRow toFlatRow() {
            return new FlatFlowRow(controllerFile, controllerMethod,
                    serviceInterfaceFile, serviceImplFile, serviceImplMethod,
                    daoFile, daoMethod, sqlFile);
        }
    }

    /**
     * 평면화된 호출 흐름 행 (파일명/메소드명 분리, Service 인터페이스/구현체 분리)
     */
    private static class FlatFlowRow {
        final String controllerFile;
        final String controllerMethod;
        final String serviceInterfaceFile;  // Service 인터페이스 파일
        final String serviceImplFile;       // ServiceImpl 구현체 파일
        final String serviceImplMethod;     // ServiceImpl 메소드
        final String daoFile;
        final String daoMethod;
        final String sqlFile;

        FlatFlowRow(String controllerFile, String controllerMethod,
                   String serviceInterfaceFile, String serviceImplFile, String serviceImplMethod,
                   String daoFile, String daoMethod, String sqlFile) {
            this.controllerFile = controllerFile;
            this.controllerMethod = controllerMethod;
            this.serviceInterfaceFile = serviceInterfaceFile;
            this.serviceImplFile = serviceImplFile;
            this.serviceImplMethod = serviceImplMethod;
            this.daoFile = daoFile;
            this.daoMethod = daoMethod;
            this.sqlFile = sqlFile;
        }
    }
}
