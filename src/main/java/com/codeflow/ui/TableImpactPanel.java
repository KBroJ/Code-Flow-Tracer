package com.codeflow.ui;

import com.codeflow.analyzer.FlowAnalyzer;
import com.codeflow.analyzer.FlowResult;

import javax.swing.*;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 테이블 영향도 분석 패널 (가운데 영역)
 *
 * 구조:
 * - 상단: 브레드크럼 (테이블명 > 쿼리)
 * - 가운데: CardLayout (접근 정보 테이블 / 쿼리 상세 뷰)
 */
public class TableImpactPanel extends JPanel {

    // CardLayout 상수
    private static final String CARD_ACCESS_TABLE = "accessTable";
    private static final String CARD_QUERY_DETAIL = "queryDetail";

    // 색상 상수
    private static final Color COLOR_HEADER = new Color(78, 201, 176);
    private static final Color COLOR_BREADCRUMB_LINK = new Color(86, 156, 214);

    // 브레드크럼 컴포넌트
    private JPanel breadcrumbPanel;
    private JLabel breadcrumbTableLabel;
    private JLabel breadcrumbSeparator;
    private JLabel breadcrumbQueryLabel;

    // CardLayout 컴포넌트
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // 접근 정보 테이블 뷰
    private JTable accessTable;
    private DefaultTableModel accessTableModel;
    private TableRowSorter<DefaultTableModel> accessTableSorter;
    private JTextField accessSearchField;   // 검색 필드
    private JLabel accessTableHeader;

    // 쿼리 상세 뷰
    private JTextArea queryTextArea;
    private JLabel queryInfoLabel;

    // 데이터
    private Map<String, FlowAnalyzer.TableImpact> tableIndex;
    private List<FlowAnalyzer.TableAccess> currentAccessList = new ArrayList<>();
    private String currentTableName = null;
    private int currentQueryRowIndex = -1;  // 현재 선택된 쿼리 행 인덱스 (-1은 전체 쿼리)

    public TableImpactPanel() {
        setLayout(new BorderLayout());
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // 브레드크럼 컴포넌트
        breadcrumbTableLabel = new JLabel("테이블을 선택하세요");
        breadcrumbTableLabel.setFont(breadcrumbTableLabel.getFont().deriveFont(Font.BOLD, 14f));
        breadcrumbTableLabel.setForeground(COLOR_HEADER);
        breadcrumbTableLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        breadcrumbSeparator = new JLabel(" > ");
        breadcrumbSeparator.setFont(breadcrumbSeparator.getFont().deriveFont(14f));
        breadcrumbSeparator.setForeground(new Color(150, 150, 150));
        breadcrumbSeparator.setVisible(false);

        breadcrumbQueryLabel = new JLabel("쿼리");
        breadcrumbQueryLabel.setFont(breadcrumbQueryLabel.getFont().deriveFont(Font.BOLD, 14f));
        breadcrumbQueryLabel.setForeground(COLOR_HEADER);
        breadcrumbQueryLabel.setVisible(false);

        // 접근 정보 검색 필드
        accessSearchField = new JTextField();
        accessSearchField.setFont(new Font("D2Coding", Font.PLAIN, 13));
        accessSearchField.setToolTipText("URL, XML 파일명, SQL ID로 검색 (실시간)");

        // 접근 정보 테이블 (컬럼: CRUD / URL / XML 파일 / SQL ID)
        String[] columns = {"CRUD", "URL", "XML 파일", "SQL ID"};
        accessTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        accessTable = new JTable(accessTableModel);
        accessTable.setFont(new Font("D2Coding", Font.PLAIN, 13));
        accessTable.setRowHeight(26);
        accessTable.getTableHeader().setReorderingAllowed(false);

        // 컬럼 너비 설정
        accessTable.getColumnModel().getColumn(0).setPreferredWidth(70);   // CRUD
        accessTable.getColumnModel().getColumn(1).setPreferredWidth(300);  // URL
        accessTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // XML 파일
        accessTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // SQL ID

        // 정렬 및 필터링 기능
        accessTableSorter = new TableRowSorter<>(accessTableModel);
        accessTable.setRowSorter(accessTableSorter);

        accessTableHeader = new JLabel("접근 정보");
        accessTableHeader.setFont(accessTableHeader.getFont().deriveFont(12f));
        accessTableHeader.setForeground(new Color(150, 150, 150));

        // 쿼리 상세 뷰
        queryTextArea = new JTextArea();
        queryTextArea.setFont(new Font("D2Coding", Font.PLAIN, 13));
        queryTextArea.setEditable(false);
        queryTextArea.setLineWrap(true);
        queryTextArea.setWrapStyleWord(true);

        queryInfoLabel = new JLabel("");
        queryInfoLabel.setFont(queryInfoLabel.getFont().deriveFont(12f));
        queryInfoLabel.setForeground(new Color(150, 150, 150));
    }

    private void layoutComponents() {
        // 상단: 브레드크럼 패널
        breadcrumbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        breadcrumbPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        breadcrumbPanel.add(breadcrumbTableLabel);
        breadcrumbPanel.add(breadcrumbSeparator);
        breadcrumbPanel.add(breadcrumbQueryLabel);

        add(breadcrumbPanel, BorderLayout.NORTH);

        // 가운데: CardLayout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Card 1: 접근 정보 테이블
        JPanel accessTablePanel = new JPanel(new BorderLayout(0, 5));
        accessTablePanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        // 상단: 검색 필드
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        JLabel searchIcon = new JLabel("🔍 ");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        accessSearchField.setPreferredSize(new Dimension(0, 28));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(accessSearchField, BorderLayout.CENTER);
        accessTablePanel.add(searchPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(accessTable);
        accessTablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // 더블클릭 안내 레이블
        JLabel hintLabel = new JLabel("💡 행을 더블클릭하면 해당 SQL 쿼리를 볼 수 있습니다");
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));
        hintLabel.setForeground(new Color(150, 150, 150));
        accessTablePanel.add(hintLabel, BorderLayout.SOUTH);

        cardPanel.add(accessTablePanel, CARD_ACCESS_TABLE);

        // Card 2: 쿼리 상세 뷰
        JPanel queryDetailPanel = new JPanel(new BorderLayout(0, 5));
        queryDetailPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        JScrollPane queryScrollPane = new JScrollPane(queryTextArea);
        queryDetailPanel.add(queryScrollPane, BorderLayout.CENTER);
        queryDetailPanel.add(queryInfoLabel, BorderLayout.SOUTH);

        cardPanel.add(queryDetailPanel, CARD_QUERY_DETAIL);

        add(cardPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        // 브레드크럼 테이블명 클릭 → 접근 정보 테이블로 돌아가기
        breadcrumbTableLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentTableName != null && breadcrumbSeparator.isVisible()) {
                    showAccessTableView();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (breadcrumbSeparator.isVisible()) {
                    breadcrumbTableLabel.setForeground(COLOR_BREADCRUMB_LINK);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                breadcrumbTableLabel.setForeground(COLOR_HEADER);
            }
        });

        // 접근 정보 테이블 더블클릭 → 해당 행의 쿼리 상세 뷰
        accessTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = accessTable.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = accessTable.convertRowIndexToModel(viewRow);
                        showQueryForRow(modelRow);
                    }
                }
            }
        });

        // 마우스 뒤로가기 버튼 지원 - 쿼리 상세 화면에서 뒤로가기
        // 마우스 확장 버튼: 4 = 뒤로가기(XBUTTON1), 5 = 앞으로가기(XBUTTON2)
        MouseAdapter mouseBackButtonAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 버튼 4 = 마우스 뒤로가기 버튼 (XBUTTON1)
                if (e.getButton() == 4) {
                    if (breadcrumbSeparator.isVisible()) {
                        showAccessTableView();
                    }
                }
            }
        };

        // 패널 전체에 마우스 뒤로가기 버튼 리스너 추가
        addMouseListener(mouseBackButtonAdapter);
        cardPanel.addMouseListener(mouseBackButtonAdapter);
        accessTable.addMouseListener(mouseBackButtonAdapter);
        queryTextArea.addMouseListener(mouseBackButtonAdapter);
        breadcrumbPanel.addMouseListener(mouseBackButtonAdapter);

        // 접근 정보 검색 필드 실시간 필터링
        accessSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterAccessTable(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterAccessTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterAccessTable(); }
        });
    }

    /**
     * 접근 정보 테이블 실시간 필터링
     */
    private void filterAccessTable() {
        String searchText = accessSearchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            accessTableSorter.setRowFilter(null);
        } else {
            // 모든 컬럼에서 검색 (대소문자 무시)
            accessTableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(searchText)));
        }
    }

    /**
     * 분석 결과로 데이터 업데이트
     */
    public void updateData(FlowResult result) {
        if (result == null) {
            clear();
            return;
        }

        FlowAnalyzer analyzer = new FlowAnalyzer();
        this.tableIndex = analyzer.buildTableIndex(result);

        // 초기 상태로 설정
        currentTableName = null;
        breadcrumbTableLabel.setText("테이블을 선택하세요");
        breadcrumbSeparator.setVisible(false);
        breadcrumbQueryLabel.setVisible(false);
        accessTableModel.setRowCount(0);
        currentAccessList.clear();
        cardLayout.show(cardPanel, CARD_ACCESS_TABLE);
    }

    private static final String ALL_TABLES = "== 전체 ==";

    /**
     * 특정 테이블의 접근 정보 표시 (왼쪽 목록에서 클릭 시)
     */
    public void displayTableAccesses(String tableName) {
        if (tableIndex == null || tableName == null) {
            return;
        }

        currentTableName = tableName;
        currentAccessList.clear();
        accessTableModel.setRowCount(0);

        // 검색 필드 초기화
        accessSearchField.setText("");

        // 브레드크럼 업데이트
        breadcrumbSeparator.setVisible(false);
        breadcrumbQueryLabel.setVisible(false);

        // "전체" 선택 시 모든 테이블 접근 정보 표시
        if (tableName.equals(ALL_TABLES)) {
            breadcrumbTableLabel.setText("전체 테이블");

            for (FlowAnalyzer.TableImpact impact : tableIndex.values()) {
                // 접근 정보 추가
                for (FlowAnalyzer.TableAccess access : impact.getAccesses()) {
                    currentAccessList.add(access);
                    accessTableModel.addRow(new Object[]{
                        access.getSqlType() != null ? access.getSqlType().name() : "-",
                        access.getUrl() != null ? access.getUrl() : "-",
                        access.getXmlFileName() != null ? access.getXmlFileName() : "-",
                        access.getSqlId() != null ? access.getSqlId() : "-"
                    });
                }
            }

            cardLayout.show(cardPanel, CARD_ACCESS_TABLE);
            return;
        }

        // 개별 테이블 선택
        breadcrumbTableLabel.setText(tableName);

        // 테이블 데이터 조회
        FlowAnalyzer.TableImpact impact = tableIndex.get(tableName);
        if (impact == null) {
            return;
        }

        // 접근 정보 테이블 데이터 추가
        for (FlowAnalyzer.TableAccess access : impact.getAccesses()) {
            currentAccessList.add(access);
            accessTableModel.addRow(new Object[]{
                access.getSqlType() != null ? access.getSqlType().name() : "-",
                access.getUrl() != null ? access.getUrl() : "-",
                access.getXmlFileName() != null ? access.getXmlFileName() : "-",
                access.getSqlId() != null ? access.getSqlId() : "-"
            });
        }

        // 접근 정보 테이블 뷰 표시
        cardLayout.show(cardPanel, CARD_ACCESS_TABLE);
    }

    /**
     * 쿼리 상세 뷰로 전환 (왼쪽 목록 더블클릭 시 - 전체 쿼리 표시)
     */
    public void showQueryDetailView(String tableName) {
        if (tableIndex == null || tableName == null) {
            return;
        }

        // "전체" 더블클릭 시 무시 (쿼리가 너무 많음)
        if (tableName.equals(ALL_TABLES)) {
            return;
        }

        currentTableName = tableName;
        currentQueryRowIndex = -1;  // 전체 쿼리 표시
        FlowAnalyzer.TableImpact impact = tableIndex.get(tableName);
        if (impact == null) {
            return;
        }

        // 브레드크럼 업데이트
        breadcrumbTableLabel.setText(tableName);
        breadcrumbSeparator.setVisible(true);
        breadcrumbQueryLabel.setVisible(true);
        breadcrumbQueryLabel.setText("쿼리 전체");

        // 모든 쿼리 수집
        StringBuilder allQueries = new StringBuilder();
        int queryCount = 0;
        for (FlowAnalyzer.TableAccess access : impact.getAccesses()) {
            String query = access.getQuery();
            if (query != null && !query.trim().isEmpty()) {
                if (allQueries.length() > 0) {
                    allQueries.append("\n\n");
                    allQueries.append("─".repeat(60));
                    allQueries.append("\n\n");
                }
                // SQL ID 헤더
                String sqlId = access.getSqlId();
                String sqlType = access.getSqlType() != null ? access.getSqlType().name() : "?";
                allQueries.append("/* [").append(sqlType).append("] ");
                if (sqlId != null) {
                    allQueries.append(sqlId);
                }
                allQueries.append(" */\n");
                allQueries.append(query.trim());
                queryCount++;
            }
        }

        if (allQueries.length() == 0) {
            queryTextArea.setText("쿼리 정보가 없습니다.");
        } else {
            queryTextArea.setText(allQueries.toString());
        }
        queryTextArea.setCaretPosition(0);

        queryInfoLabel.setText(String.format("총 %d개 쿼리", queryCount));

        // 쿼리 상세 뷰 표시
        cardLayout.show(cardPanel, CARD_QUERY_DETAIL);
    }

    /**
     * 특정 행의 쿼리 표시 (테이블 더블클릭 시)
     */
    private void showQueryForRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= currentAccessList.size()) {
            return;
        }

        currentQueryRowIndex = rowIndex;  // 선택된 행 인덱스 저장
        FlowAnalyzer.TableAccess access = currentAccessList.get(rowIndex);
        String query = access.getQuery();
        String sqlId = access.getSqlId();
        String sqlType = access.getSqlType() != null ? access.getSqlType().name() : "?";

        // 브레드크럼 업데이트
        breadcrumbSeparator.setVisible(true);
        breadcrumbQueryLabel.setVisible(true);
        breadcrumbQueryLabel.setText(sqlId != null ? sqlId : "쿼리");

        if (query == null || query.trim().isEmpty()) {
            queryTextArea.setText("쿼리 정보가 없습니다.");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("/* [").append(sqlType).append("] ");
            if (sqlId != null) {
                sb.append(sqlId);
            }
            sb.append(" */\n");
            sb.append(query.trim());
            queryTextArea.setText(sb.toString());
        }
        queryTextArea.setCaretPosition(0);

        String xmlFile = access.getXmlFileName();
        queryInfoLabel.setText(xmlFile != null ? "파일: " + xmlFile : "");

        // 쿼리 상세 뷰 표시
        cardLayout.show(cardPanel, CARD_QUERY_DETAIL);
    }

    /**
     * 접근 정보 테이블 뷰로 돌아가기
     */
    private void showAccessTableView() {
        breadcrumbSeparator.setVisible(false);
        breadcrumbQueryLabel.setVisible(false);
        cardLayout.show(cardPanel, CARD_ACCESS_TABLE);
    }

    /**
     * 초기화
     */
    public void clear() {
        tableIndex = null;
        currentTableName = null;
        currentAccessList.clear();
        accessTableModel.setRowCount(0);
        queryTextArea.setText("");
        breadcrumbTableLabel.setText("테이블을 선택하세요");
        breadcrumbSeparator.setVisible(false);
        breadcrumbQueryLabel.setVisible(false);
        queryInfoLabel.setText("");
        accessSearchField.setText("");
        cardLayout.show(cardPanel, CARD_ACCESS_TABLE);
    }

    /**
     * 쿼리 상세 화면이 활성화 상태인지 확인
     */
    public boolean isQueryDetailViewActive() {
        return breadcrumbSeparator.isVisible();
    }

    /**
     * 현재 선택된 쿼리 행 인덱스 반환 (-1은 전체 쿼리)
     */
    public int getSelectedQueryRowIndex() {
        return currentQueryRowIndex;
    }

    /**
     * 특정 쿼리 행의 상세 화면 복원 (세션 복원용)
     * @param rowIndex 복원할 행 인덱스 (-1이면 전체 쿼리)
     */
    public void restoreQueryView(int rowIndex) {
        if (rowIndex < 0) {
            // 전체 쿼리 표시
            if (currentTableName != null) {
                showQueryDetailView(currentTableName);
            }
        } else {
            // 특정 행의 쿼리 표시
            showQueryForRow(rowIndex);
        }
    }
}
