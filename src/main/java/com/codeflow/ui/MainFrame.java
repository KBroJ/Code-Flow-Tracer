package com.codeflow.ui;

import com.codeflow.analyzer.FlowAnalyzer;
import com.codeflow.analyzer.FlowNode;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.output.ExcelOutput;
import com.codeflow.parser.IBatisParser;
import com.codeflow.parser.JavaSourceParser;
import com.codeflow.parser.ParsedClass;
import com.codeflow.parser.SqlInfo;
import com.codeflow.session.SessionData;
import com.codeflow.session.SessionManager;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Code Flow Tracer GUI 메인 프레임
 *
 * 사이드 패널 레이아웃:
 * - 왼쪽: 분석 결과 (넓은 영역)
 * - 오른쪽: 설정 패널 (고정 너비, 접기/펼치기 가능)
 */
public class MainFrame extends JFrame {

    private static final String TITLE = "Code Flow Tracer";
    private static final int DEFAULT_WIDTH = 1400;
    private static final int DEFAULT_HEIGHT = 900;
    private static final int SIDE_PANEL_WIDTH = 280;

    // 설정 관련 상수
    private static final int MAX_RECENT_PATHS = 10;

    // 레이아웃 컴포넌트
    private JPanel sidePanel;
    private JPanel endpointListPanel;
    private JPanel tableListPanel;      // 테이블 영향도용 왼쪽 패널
    private JPanel leftCardPanel;       // CardLayout으로 전환되는 왼쪽 패널
    private CardLayout leftCardLayout;
    private JSplitPane mainSplitPane;
    private static final int ENDPOINT_PANEL_WIDTH = 200;
    private static final String CARD_ENDPOINT = "endpoint";
    private static final String CARD_TABLE = "table";

    // 엔드포인트 목록 컴포넌트
    private JTextField endpointSearchField;
    private JList<String> endpointList;
    private DefaultListModel<String> endpointListModel;
    private JLabel endpointCountLabel;
    private List<String> allEndpoints = new ArrayList<>();

    // 테이블 목록 컴포넌트 (테이블 영향도용)
    private JTextField tableSearchField;
    private JList<String> tableList;
    private DefaultListModel<String> tableListModel;
    private JLabel tableCountLabel;
    private List<String> allTableNames = new ArrayList<>();

    // 분석 요약 패널
    private JPanel summaryPanel;
    private JPanel summaryCardPanel;      // CardLayout으로 전환되는 요약 패널
    private CardLayout summaryCardLayout;
    private static final String SUMMARY_CLASS = "classStats";
    private static final String SUMMARY_CRUD = "crudStats";

    // 클래스 통계 라벨 (호출 흐름 탭)
    private JLabel lblTotalClasses;
    private JLabel lblControllerCount;
    private JLabel lblServiceCount;
    private JLabel lblDaoCount;
    private JLabel lblEndpointCount;

    // CRUD 통계 라벨 (테이블 영향도 탭)
    private JLabel lblTotalTables;
    private JLabel lblSelectCount;
    private JLabel lblInsertCount;
    private JLabel lblUpdateCount;
    private JLabel lblDeleteCount;

    // 프로젝트 경로
    private JComboBox<String> projectPathComboBox;
    private JButton browseButton;

    // 분석 옵션
    private JTextField urlFilterField;
    private JPanel urlFilterPanel;        // URL 필터 영역 (탭별 표시/숨김용)
    private JRadioButton rbCompact;
    private JRadioButton rbNormal;
    private JRadioButton rbDetailed;
    private ButtonGroup styleGroup;

    // CRUD 타입 필터 체크박스
    private JCheckBox cbSelect;
    private JCheckBox cbInsert;
    private JCheckBox cbUpdate;
    private JCheckBox cbDelete;

    // 액션 버튼
    private JButton analyzeButton;
    private JButton exportExcelButton;
    private JButton settingsButton;

    // 결과 표시
    private JTabbedPane resultTabbedPane;
    private ResultPanel resultPanel;
    private TableImpactPanel tableImpactPanel;

    // 진행 상태
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // 분석 결과 캐시
    private FlowResult originalResult;  // 필터 없는 원본 결과
    private FlowResult currentResult;   // 현재 표시용 (필터 적용된)
    private Path currentProjectPath;

    // 세션 관리
    private final SessionManager sessionManager = new SessionManager();

    // 색상 상수
    private static final Color COLOR_SECTION_LABEL = new Color(78, 201, 176);  // 청록
    private static final Color COLOR_SEPARATOR = new Color(80, 80, 80);        // 구분선 (밝은 회색)
    private static final Color COLOR_CONTROLLER = new Color(78, 201, 176);
    private static final Color COLOR_SERVICE = new Color(86, 156, 214);
    private static final Color COLOR_DAO = new Color(197, 134, 192);

    public MainFrame() {
        initializeFrame();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        loadSettings();
        restoreSession();
    }

    /**
     * 프레임 기본 설정
     */
    private void initializeFrame() {
        setTitle(TITLE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 윈도우 아이콘 설정
        try {
            java.net.URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                setIconImage(new ImageIcon(iconUrl).getImage());
            }
        } catch (Exception e) {
            // 아이콘 로드 실패 시 무시
        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // 종료 전 세션 저장 (탭/테이블 선택 상태 포함)
                saveSession();
                System.exit(0);
            }
        });
    }

    /**
     * UI 컴포넌트 초기화
     */
    private void initializeComponents() {
        // 클래스 통계 라벨 (호출 흐름 탭용)
        lblTotalClasses = new JLabel("-");
        lblControllerCount = new JLabel("-");
        lblServiceCount = new JLabel("-");
        lblDaoCount = new JLabel("-");
        lblEndpointCount = new JLabel("-");

        // CRUD 통계 라벨 (테이블 영향도 탭용)
        lblTotalTables = new JLabel("-");
        lblSelectCount = new JLabel("-");
        lblInsertCount = new JLabel("-");
        lblUpdateCount = new JLabel("-");
        lblDeleteCount = new JLabel("-");

        // 프로젝트 경로
        projectPathComboBox = new JComboBox<>();
        projectPathComboBox.setEditable(true);
        browseButton = new JButton("📁");
        browseButton.setToolTipText("폴더 선택");
        browseButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        // URL 필터
        urlFilterField = new JTextField();
        urlFilterField.setToolTipText("예: /api/user/*, /user/** (빈칸이면 전체 분석)");

        // 출력 스타일 라디오 버튼
        rbCompact = new JRadioButton("compact");
        rbNormal = new JRadioButton("normal");
        rbDetailed = new JRadioButton("detailed");
        rbNormal.setSelected(true);

        styleGroup = new ButtonGroup();
        styleGroup.add(rbCompact);
        styleGroup.add(rbNormal);
        styleGroup.add(rbDetailed);

        // CRUD 타입 필터 체크박스 (기본: 모두 선택)
        cbSelect = new JCheckBox("SELECT", true);
        cbInsert = new JCheckBox("INSERT", true);
        cbUpdate = new JCheckBox("UPDATE", true);
        cbDelete = new JCheckBox("DELETE", true);
        cbSelect.setToolTipText("조회 SQL만 표시");
        cbInsert.setToolTipText("등록 SQL만 표시");
        cbUpdate.setToolTipText("수정 SQL만 표시");
        cbDelete.setToolTipText("삭제 SQL만 표시");

        // 액션 버튼
        analyzeButton = new JButton("▶  분석 시작");
        analyzeButton.setFont(analyzeButton.getFont().deriveFont(Font.BOLD, 13f));

        exportExcelButton = new JButton("💾  엑셀 저장");
        exportExcelButton.setEnabled(false);

        settingsButton = new JButton("⚙");
        settingsButton.setToolTipText("설정");
        settingsButton.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        settingsButton.setPreferredSize(new Dimension(28, 28));
        settingsButton.setMinimumSize(new Dimension(28, 28));
        settingsButton.setMaximumSize(new Dimension(28, 28));
        settingsButton.setMargin(new Insets(0, 0, 0, 0));
        settingsButton.setFocusPainted(false);

        // 결과 패널
        resultPanel = new ResultPanel();
        tableImpactPanel = new TableImpactPanel();

        // 탭 패널 (호출 흐름 + 테이블 영향도)
        resultTabbedPane = new JTabbedPane();
        resultTabbedPane.addTab("호출 흐름", resultPanel);
        resultTabbedPane.addTab("테이블 영향도", tableImpactPanel);
        resultTabbedPane.setFont(resultTabbedPane.getFont().deriveFont(13f));

        // 진행 상태
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("대기 중");

        statusLabel = new JLabel("프로젝트를 선택하고 '분석 시작' 버튼을 클릭하세요.");

        // 엔드포인트 목록 컴포넌트
        endpointSearchField = new JTextField();
        endpointSearchField.setToolTipText("URL 검색");
        endpointListModel = new DefaultListModel<>();
        endpointList = new JList<>(endpointListModel);
        endpointList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        endpointCountLabel = new JLabel("0개 항목");

        // 테이블 목록 컴포넌트
        tableSearchField = new JTextField();
        tableSearchField.setToolTipText("테이블명 검색");
        tableListModel = new DefaultListModel<>();
        tableList = new JList<>(tableListModel);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableCountLabel = new JLabel("0개 테이블");
    }

    /**
     * 레이아웃 구성
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());

        // 좌측 패널들 생성
        endpointListPanel = createEndpointListPanel();
        tableListPanel = createTableListPanel();

        // CardLayout으로 왼쪽 패널 전환
        leftCardLayout = new CardLayout();
        leftCardPanel = new JPanel(leftCardLayout);
        leftCardPanel.add(endpointListPanel, CARD_ENDPOINT);
        leftCardPanel.add(tableListPanel, CARD_TABLE);

        // 메인 영역 (탭 패널: 호출 흐름 + 테이블 영향도)
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(resultTabbedPane, BorderLayout.CENTER);

        // JSplitPane: 좌측 목록 패널 + 결과 패널 (드래그 조절 가능)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCardPanel, mainPanel);
        mainSplitPane.setDividerLocation(ENDPOINT_PANEL_WIDTH);  // 처음부터 왼쪽 패널 표시
        mainSplitPane.setDividerSize(6);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setBorder(null);

        // 사이드 패널 (우측 설정) - 왼쪽에 여백 추가
        sidePanel = createSidePanel();
        sidePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, COLOR_SEPARATOR),
            BorderFactory.createEmptyBorder(0, 8, 0, 0)  // 왼쪽 여백
        ));

        // 메인 레이아웃
        add(mainSplitPane, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);

        // 하단 상태바
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // 여백
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(5, 5, 5, 5));
    }

    /**
     * 엔드포인트 목록 패널 생성
     */
    private JPanel createEndpointListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(new EmptyBorder(8, 10, 10, 6));
        panel.setPreferredSize(new Dimension(ENDPOINT_PANEL_WIDTH, 0));
        panel.setMinimumSize(new Dimension(120, 0));  // JSplitPane에서 최소 너비

        // 상단: 검색 필드 + 항목 수
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));

        JPanel searchPanel = new JPanel(new BorderLayout());
        JLabel searchIcon = new JLabel("🔍 ");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        endpointSearchField.setPreferredSize(new Dimension(0, 28));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(endpointSearchField, BorderLayout.CENTER);
        headerPanel.add(searchPanel, BorderLayout.NORTH);

        // 항목 수 표시 (검색 바로 아래)
        endpointCountLabel.setForeground(new Color(150, 150, 150));
        endpointCountLabel.setFont(endpointCountLabel.getFont().deriveFont(11f));
        headerPanel.add(endpointCountLabel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);

        // 중앙: URL 리스트
        endpointList.setFont(new Font("D2Coding", Font.PLAIN, 14));
        endpointList.setFixedCellHeight(28);
        JScrollPane listScrollPane = new JScrollPane(endpointList);
        listScrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(listScrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 테이블 목록 패널 생성 (테이블 영향도 탭용)
     */
    private JPanel createTableListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(new EmptyBorder(8, 10, 10, 6));
        panel.setPreferredSize(new Dimension(ENDPOINT_PANEL_WIDTH, 0));
        panel.setMinimumSize(new Dimension(120, 0));

        // 상단: 검색 필드 + 테이블 수
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));

        JPanel searchPanel = new JPanel(new BorderLayout());
        JLabel searchIcon = new JLabel("🔍 ");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        tableSearchField.setPreferredSize(new Dimension(0, 28));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(tableSearchField, BorderLayout.CENTER);
        headerPanel.add(searchPanel, BorderLayout.NORTH);

        // 테이블 수 표시
        tableCountLabel.setForeground(new Color(150, 150, 150));
        tableCountLabel.setFont(tableCountLabel.getFont().deriveFont(11f));
        headerPanel.add(tableCountLabel, BorderLayout.SOUTH);

        panel.add(headerPanel, BorderLayout.NORTH);

        // 중앙: 테이블 리스트
        tableList.setFont(new Font("D2Coding", Font.PLAIN, 14));
        tableList.setFixedCellHeight(28);
        JScrollPane listScrollPane = new JScrollPane(tableList);
        listScrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(listScrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 사이드 패널 생성
     */
    private JPanel createSidePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(8, 15, 10, 15));
        panel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, 0));
        panel.setMinimumSize(new Dimension(SIDE_PANEL_WIDTH, 0));

        // 상단 설정 버튼 (오른쪽 정렬)
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        topBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        topBar.add(settingsButton);
        panel.add(topBar);
        panel.add(Box.createVerticalStrut(8));

        // 1. 분석 요약 섹션
        summaryPanel = createSummarySection();
        summaryPanel.setVisible(true);  // 처음부터 표시 (초기값 0개)
        panel.add(summaryPanel);

        // 2. 프로젝트 경로 섹션
        panel.add(createProjectPathSection());
        panel.add(Box.createVerticalStrut(16));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(16));

        // 3. 분석 옵션 섹션
        panel.add(createOptionsSection());
        panel.add(Box.createVerticalStrut(16));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(16));

        // 4. 액션 버튼
        panel.add(createActionButtonsSection());

        // 빈 공간 채우기
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * 분석 요약 섹션 생성
     */
    private JPanel createSummarySection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // 섹션 라벨
        JLabel sectionLabel = new JLabel("📊 분석 요약");
        sectionLabel.setForeground(COLOR_SECTION_LABEL);
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, 13f));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionLabel);
        section.add(Box.createVerticalStrut(10));

        // CardLayout으로 클래스 통계 / CRUD 통계 전환
        summaryCardLayout = new CardLayout();
        summaryCardPanel = new JPanel(summaryCardLayout);
        summaryCardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Card 1: 클래스 통계 (호출 흐름 탭용)
        JPanel classStatsPanel = createClassStatsPanel();
        summaryCardPanel.add(classStatsPanel, SUMMARY_CLASS);

        // Card 2: CRUD 통계 (테이블 영향도 탭용)
        JPanel crudStatsPanel = createCrudStatsPanel();
        summaryCardPanel.add(crudStatsPanel, SUMMARY_CRUD);

        section.add(summaryCardPanel);
        section.add(Box.createVerticalStrut(12));
        section.add(createSeparator());
        section.add(Box.createVerticalStrut(12));

        return section;
    }

    /**
     * 클래스 통계 패널 생성 (호출 흐름 탭용)
     */
    private JPanel createClassStatsPanel() {
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 클래스 합계
        tablePanel.add(createSummaryRow("클래스", lblTotalClasses, null));

        // 트리 형태 하위 항목
        lblControllerCount.setForeground(COLOR_CONTROLLER);
        tablePanel.add(createSummaryRow("  ├ Controller", lblControllerCount, COLOR_CONTROLLER));

        lblServiceCount.setForeground(COLOR_SERVICE);
        tablePanel.add(createSummaryRow("  ├ Service", lblServiceCount, COLOR_SERVICE));

        lblDaoCount.setForeground(COLOR_DAO);
        tablePanel.add(createSummaryRow("  └ DAO", lblDaoCount, COLOR_DAO));

        // 빈 줄
        tablePanel.add(Box.createVerticalStrut(8));

        // URL 수
        JLabel endpointLabel = new JLabel("URL");
        endpointLabel.setFont(endpointLabel.getFont().deriveFont(Font.BOLD));
        lblEndpointCount.setFont(lblEndpointCount.getFont().deriveFont(Font.BOLD));
        lblEndpointCount.setForeground(COLOR_CONTROLLER);
        tablePanel.add(createSummaryRow(endpointLabel, lblEndpointCount));

        return tablePanel;
    }

    /**
     * CRUD 통계 패널 생성 (테이블 영향도 탭용)
     */
    private JPanel createCrudStatsPanel() {
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 테이블 수
        JLabel tableLabel = new JLabel("테이블");
        tableLabel.setFont(tableLabel.getFont().deriveFont(Font.BOLD));
        lblTotalTables.setFont(lblTotalTables.getFont().deriveFont(Font.BOLD));
        lblTotalTables.setForeground(COLOR_CONTROLLER);
        tablePanel.add(createSummaryRow(tableLabel, lblTotalTables));

        // 빈 줄
        tablePanel.add(Box.createVerticalStrut(8));

        // CRUD 하위 항목
        tablePanel.add(createSummaryRow("SQL 쿼리", new JLabel(""), null));

        lblSelectCount.setForeground(COLOR_CONTROLLER);
        tablePanel.add(createSummaryRow("  ├ SELECT", lblSelectCount, COLOR_CONTROLLER));

        lblInsertCount.setForeground(COLOR_SERVICE);
        tablePanel.add(createSummaryRow("  ├ INSERT", lblInsertCount, COLOR_SERVICE));

        lblUpdateCount.setForeground(new Color(220, 180, 100));  // 노란색 계열
        tablePanel.add(createSummaryRow("  ├ UPDATE", lblUpdateCount, new Color(220, 180, 100)));

        lblDeleteCount.setForeground(new Color(214, 86, 86));  // 빨간색 계열
        tablePanel.add(createSummaryRow("  └ DELETE", lblDeleteCount, new Color(214, 86, 86)));

        return tablePanel;
    }

    /**
     * 프로젝트 경로 섹션 생성
     */
    private JPanel createProjectPathSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // 섹션 라벨
        JLabel sectionLabel = new JLabel("📁 프로젝트 경로");
        sectionLabel.setForeground(COLOR_SECTION_LABEL);
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, 13f));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionLabel);
        section.add(Box.createVerticalStrut(8));

        // 콤보박스 + 폴더 선택 버튼 (가로 배치)
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pathPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        projectPathComboBox.setPreferredSize(new Dimension(0, 28));
        browseButton.setPreferredSize(new Dimension(36, 28));
        pathPanel.add(projectPathComboBox, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        section.add(pathPanel);
        section.add(Box.createVerticalStrut(10));

        return section;
    }

    /**
     * 분석 옵션 섹션 생성
     */
    private JPanel createOptionsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // 섹션 라벨
        JLabel sectionLabel = new JLabel("🔍 분석 옵션");
        sectionLabel.setForeground(COLOR_SECTION_LABEL);
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, 13f));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(sectionLabel);
        section.add(Box.createVerticalStrut(10));

        // URL 필터 (탭별 표시/숨김용 패널로 감싸기)
        urlFilterPanel = new JPanel();
        urlFilterPanel.setLayout(new BoxLayout(urlFilterPanel, BoxLayout.Y_AXIS));
        urlFilterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel urlLabel = new JLabel("URL 필터");
        urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        urlFilterPanel.add(urlLabel);
        urlFilterPanel.add(Box.createVerticalStrut(3));

        urlFilterField.setAlignmentX(Component.LEFT_ALIGNMENT);
        urlFilterField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        urlFilterPanel.add(urlFilterField);
        urlFilterPanel.add(Box.createVerticalStrut(12));

        section.add(urlFilterPanel);

        // 출력 스타일 (가로 배치)
        JLabel styleLabel = new JLabel("출력 스타일");
        styleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(styleLabel);
        section.add(Box.createVerticalStrut(5));

        // 라디오 버튼 가로 배치
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rbCompact.setToolTipText("간결한 출력 (타입 태그 없음)");
        rbNormal.setToolTipText("기본 출력");
        rbDetailed.setToolTipText("상세 출력 (SQL 정보 포함)");

        radioPanel.add(rbCompact);
        radioPanel.add(rbNormal);
        radioPanel.add(rbDetailed);

        section.add(radioPanel);
        section.add(Box.createVerticalStrut(12));

        // CRUD 타입 필터
        JLabel crudLabel = new JLabel("SQL 타입 필터");
        crudLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(crudLabel);
        section.add(Box.createVerticalStrut(5));

        // CRUD 체크박스 가로 배치 (2x2 그리드)
        JPanel crudPanel = new JPanel(new GridLayout(1, 4, 4, 0));
        crudPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        crudPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        crudPanel.add(cbSelect);
        crudPanel.add(cbInsert);
        crudPanel.add(cbUpdate);
        crudPanel.add(cbDelete);
        section.add(crudPanel);

        return section;
    }

    /**
     * 액션 버튼 섹션 생성
     */
    private JPanel createActionButtonsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        // 분석 시작 버튼
        analyzeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        analyzeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        section.add(analyzeButton);
        section.add(Box.createVerticalStrut(8));

        // 엑셀 저장 버튼
        exportExcelButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportExcelButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        section.add(exportExcelButton);
        section.add(Box.createVerticalStrut(10));

        return section;
    }

    /**
     * 상태바 생성
     */
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(progressBar, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 구분선 생성 (1px 라인)
     */
    private JPanel createSeparator() {
        JPanel separator = new JPanel();
        separator.setBackground(new Color(100, 100, 100));  // 밝은 회색 라인
        separator.setPreferredSize(new Dimension(0, 1));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        return separator;
    }

    /**
     * 분석 요약 행 생성 (텍스트 버전)
     */
    private JPanel createSummaryRow(String labelText, JLabel valueLabel, Color labelColor) {
        JLabel label = new JLabel(labelText);
        if (labelColor != null) {
            label.setForeground(labelColor);
        }
        return createSummaryRow(label, valueLabel);
    }

    /**
     * 분석 요약 행 생성 (JLabel 버전) - 점선 리더 포함
     */
    private JPanel createSummaryRow(JLabel label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        row.add(label, BorderLayout.WEST);

        // 점선 리더 (가운데 채우기)
        JPanel dotsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(150, 150, 150));  // 더 밝은 색상
                int y = getHeight() / 2;
                for (int x = 4; x < getWidth() - 4; x += 6) {
                    g.fillOval(x, y, 2, 2);
                }
            }
        };
        dotsPanel.setOpaque(false);
        row.add(dotsPanel, BorderLayout.CENTER);

        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    /**
     * 이벤트 핸들러 설정
     */
    private void setupEventHandlers() {
        // 찾아보기 버튼
        browseButton.addActionListener(this::handleBrowse);

        // 분석 시작 버튼
        analyzeButton.addActionListener(this::handleAnalyze);

        // 엑셀 저장 버튼
        exportExcelButton.addActionListener(this::handleExportExcel);

        // 설정 버튼
        JPopupMenu settingsPopup = createSettingsPopupMenu();
        settingsButton.addActionListener(e ->
            settingsPopup.show(settingsButton, 0, settingsButton.getHeight()));

        // Enter 키로 분석 시작
        JTextField comboEditor = (JTextField) projectPathComboBox.getEditor().getEditorComponent();
        comboEditor.addActionListener(this::handleAnalyze);
        urlFilterField.addActionListener(this::handleAnalyze);

        // 엔드포인트 목록 클릭 이벤트
        endpointList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = endpointList.getSelectedValue();
                if (selected != null) {
                    resultPanel.scrollToEndpoint(selected);
                }
            }
        });

        // 엔드포인트 검색 필터링
        endpointSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterEndpointList(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterEndpointList(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterEndpointList(); }
        });

        // CRUD 타입 체크박스 실시간 필터링
        cbSelect.addActionListener(e -> applyFiltersAndRefresh());
        cbInsert.addActionListener(e -> applyFiltersAndRefresh());
        cbUpdate.addActionListener(e -> applyFiltersAndRefresh());
        cbDelete.addActionListener(e -> applyFiltersAndRefresh());

        // 탭 전환 시 왼쪽 패널, 분석 요약, URL 필터 변경
        resultTabbedPane.addChangeListener(e -> {
            int selectedIndex = resultTabbedPane.getSelectedIndex();
            if (selectedIndex == 0) {
                // 호출 흐름 탭 → 엔드포인트 목록, 클래스 통계, URL 필터 표시
                leftCardLayout.show(leftCardPanel, CARD_ENDPOINT);
                summaryCardLayout.show(summaryCardPanel, SUMMARY_CLASS);
                urlFilterPanel.setVisible(true);
            } else {
                // 테이블 영향도 탭 → 테이블 목록, CRUD 통계, URL 필터 숨김
                leftCardLayout.show(leftCardPanel, CARD_TABLE);
                summaryCardLayout.show(summaryCardPanel, SUMMARY_CRUD);
                urlFilterPanel.setVisible(false);
                // CRUD 통계 업데이트
                if (currentResult != null) {
                    updateCrudSummaryPanel(currentResult);
                }
            }
        });

        // 테이블 목록 클릭 이벤트 (단일 클릭 → 접근 정보 표시)
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = tableList.getSelectedValue();
                if (selected != null) {
                    tableImpactPanel.displayTableAccesses(selected);
                }
            }
        });

        // 테이블 목록 더블클릭 이벤트 (쿼리 상세 화면)
        tableList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = tableList.getSelectedValue();
                    if (selected != null) {
                        tableImpactPanel.showQueryDetailView(selected);
                    }
                }
            }
        });

        // 테이블 검색 필터링
        tableSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterTableList(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterTableList(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTableList(); }
        });
    }

    /**
     * 설정 팝업 메뉴 생성
     */
    private JPopupMenu createSettingsPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem clearAllItem = new JMenuItem("설정/세션 초기화");
        clearAllItem.setToolTipText("저장된 모든 설정 및 분석 결과를 삭제합니다");
        clearAllItem.addActionListener(e -> handleClearAll());
        popup.add(clearAllItem);

        return popup;
    }

    /**
     * 설정/세션 모두 삭제 핸들러
     */
    private void handleClearAll() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "저장된 모든 설정 및 분석 결과를 삭제합니다.\n(최근 경로, 옵션 설정, 분석 결과 포함)\n계속하시겠습니까?",
                "설정/세션 초기화",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (sessionManager.clearSession()) {
                projectPathComboBox.removeAllItems();
                urlFilterField.setText("");
                endpointSearchField.setText("");
                tableSearchField.setText("");
                rbNormal.setSelected(true);
                endpointListModel.clear();  // 왼쪽 엔드포인트 목록 초기화
                tableListModel.clear();     // 왼쪽 테이블 목록 초기화
                allTableNames.clear();
                resultPanel.clear();  // 분석 결과 화면도 초기화
                tableImpactPanel.clear();  // 테이블 영향도 초기화
                originalResult = null;  // 원본 결과 초기화
                currentResult = null;  // 분석 결과 객체도 초기화
                tableCountLabel.setText("0개 테이블");
                // 분석 요약도 초기화
                lblTotalClasses.setText("0개");
                lblControllerCount.setText("0개");
                lblServiceCount.setText("0개");
                lblDaoCount.setText("0개");
                lblEndpointCount.setText("0개");
                statusLabel.setText("설정 및 세션이 초기화되었습니다.");
            } else {
                showError("초기화 실패");
            }
        }
    }

    /**
     * 찾아보기 버튼 핸들러
     */
    private void handleBrowse(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("분석할 프로젝트 폴더 선택");

        String currentPath = getSelectedProjectPath();
        if (!currentPath.isEmpty()) {
            Path path = Paths.get(currentPath);
            if (Files.exists(path)) {
                chooser.setCurrentDirectory(path.toFile());
            }
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String selectedPath = chooser.getSelectedFile().getAbsolutePath();
            projectPathComboBox.setSelectedItem(selectedPath);
        }
    }

    /**
     * 선택된 프로젝트 경로 가져오기
     */
    private String getSelectedProjectPath() {
        Object item = projectPathComboBox.getEditor().getItem();
        return item != null ? item.toString().trim() : "";
    }

    /**
     * 선택된 출력 스타일 가져오기
     */
    private String getSelectedStyle() {
        if (rbCompact.isSelected()) return "compact";
        if (rbDetailed.isSelected()) return "detailed";
        return "normal";
    }

    /**
     * 선택된 SQL 타입(CRUD) 필터 가져오기
     */
    private List<String> getSelectedSqlTypes() {
        List<String> types = new ArrayList<>();
        if (cbSelect.isSelected()) types.add("SELECT");
        if (cbInsert.isSelected()) types.add("INSERT");
        if (cbUpdate.isSelected()) types.add("UPDATE");
        if (cbDelete.isSelected()) types.add("DELETE");
        return types;
    }

    /**
     * 모든 CRUD 타입이 선택되었는지 확인 (필터링 불필요)
     */
    private boolean isAllSqlTypesSelected() {
        return cbSelect.isSelected() && cbInsert.isSelected()
            && cbUpdate.isSelected() && cbDelete.isSelected();
    }

    /**
     * 분석 시작 핸들러
     */
    private void handleAnalyze(ActionEvent e) {
        String pathStr = getSelectedProjectPath();
        if (pathStr.isEmpty()) {
            showError("프로젝트 경로를 입력하세요.");
            projectPathComboBox.requestFocus();
            return;
        }

        Path projectPath = Paths.get(pathStr);
        if (!Files.exists(projectPath)) {
            showError("경로가 존재하지 않습니다: " + pathStr);
            return;
        }

        if (!Files.isDirectory(projectPath)) {
            showError("디렉토리가 아닙니다: " + pathStr);
            return;
        }

        startAnalysis(projectPath);
    }

    /**
     * 백그라운드에서 분석 실행
     */
    private void startAnalysis(Path projectPath) {
        String urlPattern = urlFilterField.getText().trim();

        setUIEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("분석 중...");
        statusLabel.setText("프로젝트를 분석하고 있습니다...");
        resultPanel.clear();
        tableImpactPanel.clear();

        // 분석 중 상태 표시 (패널은 유지, 값만 초기화)
        lblTotalClasses.setText("-");
        lblControllerCount.setText("-");
        lblServiceCount.setText("-");
        lblDaoCount.setText("-");
        lblEndpointCount.setText("-");

        SwingWorker<FlowResult, String> worker = new SwingWorker<>() {
            @Override
            protected FlowResult doInBackground() throws Exception {
                publish("Java 소스 파싱 중...");
                JavaSourceParser parser = new JavaSourceParser();
                List<ParsedClass> parsedClasses = parser.parseProject(projectPath);

                publish("iBatis/MyBatis XML 파싱 중...");
                IBatisParser ibatisParser = new IBatisParser();
                Map<String, SqlInfo> sqlInfoMap = ibatisParser.parseProject(projectPath);

                publish("호출 흐름 분석 중...");
                FlowAnalyzer analyzer = new FlowAnalyzer();
                analyzer.setSqlInfoMap(sqlInfoMap);

                FlowResult result;
                if (urlPattern != null && !urlPattern.isEmpty()) {
                    result = analyzer.analyzeByUrl(projectPath, parsedClasses, urlPattern);
                } else {
                    result = analyzer.analyze(projectPath, parsedClasses);
                }

                // 원본 결과 반환 (CRUD 필터링은 UI에서 실시간 적용)
                return result;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    FlowResult result = get();
                    originalResult = result;  // 원본 저장
                    currentProjectPath = projectPath;

                    // CRUD 필터 적용하여 currentResult 생성
                    if (!isAllSqlTypesSelected()) {
                        List<String> sqlTypes = getSelectedSqlTypes();
                        if (!sqlTypes.isEmpty()) {
                            FlowAnalyzer analyzer = new FlowAnalyzer();
                            currentResult = analyzer.filterBySqlType(originalResult, sqlTypes);
                        } else {
                            currentResult = originalResult;
                        }
                    } else {
                        currentResult = originalResult;
                    }

                    // 요약 정보 업데이트
                    updateSummaryPanel(currentResult);

                    // 엔드포인트 목록 업데이트
                    updateEndpointList(currentResult);

                    // 결과 표시
                    String selectedStyle = getSelectedStyle();
                    resultPanel.displayResult(currentResult, selectedStyle);

                    // 테이블 영향도 업데이트 (먼저 데이터 설정)
                    tableImpactPanel.updateData(currentResult);

                    // 테이블 목록 업데이트 (데이터 설정 후 호출해야 displayTableAccesses 동작)
                    updateTableList(currentResult);

                    // 상태 업데이트
                    int totalCount = originalResult.getFlows().size();
                    int shownCount = currentResult.getFlows().size();
                    if (totalCount == shownCount) {
                        statusLabel.setText(String.format("분석 완료: %d개 URL 발견", totalCount));
                    } else {
                        statusLabel.setText(String.format("분석 완료: %d / %d개 URL (필터 적용)", shownCount, totalCount));
                    }
                    progressBar.setString("완료");

                    exportExcelButton.setEnabled(true);

                    // 설정 저장
                    saveRecentPath(projectPath.toString());
                    saveSettings();

                    // 세션 저장 (분석 결과 영속성 - 원본 저장)
                    saveSession();

                } catch (Exception ex) {
                    String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    showError("분석 중 오류 발생: " + errorMsg);
                    statusLabel.setText("분석 실패");
                    progressBar.setString("오류");
                } finally {
                    setUIEnabled(true);
                    progressBar.setIndeterminate(false);
                }
            }
        };

        worker.execute();
    }

    /**
     * 분석 요약 패널 업데이트 (flows 기반 통계 - URL 필터 적용 시 필터링된 결과만 표시)
     */
    private void updateSummaryPanel(FlowResult result) {
        lblTotalClasses.setText(result.getFlowBasedTotalClasses() + "개");
        lblControllerCount.setText(result.getFlowBasedControllerCount() + "개");
        lblServiceCount.setText(result.getFlowBasedServiceCount() + "개");
        lblDaoCount.setText(result.getFlowBasedDaoCount() + "개");
        lblEndpointCount.setText(result.getFlowBasedEndpointCount() + "개");

        // CRUD 통계도 업데이트 (테이블 영향도 탭에서 사용)
        updateCrudSummaryPanel(result);
    }

    /**
     * CRUD 통계 패널 업데이트 (테이블 영향도 탭용)
     */
    private void updateCrudSummaryPanel(FlowResult result) {
        if (result == null) {
            lblTotalTables.setText("-");
            lblSelectCount.setText("-");
            lblInsertCount.setText("-");
            lblUpdateCount.setText("-");
            lblDeleteCount.setText("-");
            return;
        }

        // FlowAnalyzer에서 테이블 인덱스 빌드
        FlowAnalyzer analyzer = new FlowAnalyzer();
        Map<String, FlowAnalyzer.TableImpact> tableIndex = analyzer.buildTableIndex(result);

        // 테이블 수
        lblTotalTables.setText(tableIndex.size() + "개");

        // CRUD 통계 계산
        long selectCount = 0, insertCount = 0, updateCount = 0, deleteCount = 0;
        for (FlowAnalyzer.TableImpact impact : tableIndex.values()) {
            Map<SqlInfo.SqlType, Long> counts = impact.getCrudCounts();
            selectCount += counts.getOrDefault(SqlInfo.SqlType.SELECT, 0L);
            insertCount += counts.getOrDefault(SqlInfo.SqlType.INSERT, 0L);
            updateCount += counts.getOrDefault(SqlInfo.SqlType.UPDATE, 0L);
            deleteCount += counts.getOrDefault(SqlInfo.SqlType.DELETE, 0L);
        }

        lblSelectCount.setText(selectCount + "개");
        lblInsertCount.setText(insertCount + "개");
        lblUpdateCount.setText(updateCount + "개");
        lblDeleteCount.setText(deleteCount + "개");
    }

    /**
     * 엑셀 저장 핸들러
     */
    private void handleExportExcel(ActionEvent e) {
        if (currentResult == null) {
            showError("먼저 분석을 실행하세요.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("엑셀 파일 저장");
        chooser.setSelectedFile(new java.io.File("code-flow-result.xlsx"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Excel 파일 (*.xlsx)", "xlsx"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path outputPath = chooser.getSelectedFile().toPath();

            if (!outputPath.toString().toLowerCase().endsWith(".xlsx")) {
                outputPath = Paths.get(outputPath.toString() + ".xlsx");
            }

            try {
                ExcelOutput excelOutput = new ExcelOutput();
                excelOutput.export(currentResult, outputPath);

                statusLabel.setText("엑셀 파일 저장됨: " + outputPath.getFileName());
                JOptionPane.showMessageDialog(this,
                        "엑셀 파일이 저장되었습니다:\n" + outputPath,
                        "저장 완료",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                showError("엑셀 저장 실패: " + ex.getMessage());
            }
        }
    }

    /**
     * UI 활성화/비활성화
     */
    private void setUIEnabled(boolean enabled) {
        projectPathComboBox.setEnabled(enabled);
        browseButton.setEnabled(enabled);
        urlFilterField.setEnabled(enabled);
        rbCompact.setEnabled(enabled);
        rbNormal.setEnabled(enabled);
        rbDetailed.setEnabled(enabled);
        analyzeButton.setEnabled(enabled);
        exportExcelButton.setEnabled(enabled && currentResult != null);
    }

    // ===== 설정 저장/로드 =====

    /**
     * 설정 로드 (JSON 파일에서)
     */
    private void loadSettings() {
        SessionData settings = sessionManager.loadSettings();
        if (settings == null) {
            return;
        }

        // 최근 프로젝트 경로
        List<String> recentPaths = settings.getRecentPaths();
        if (recentPaths != null) {
            for (String path : recentPaths) {
                if (path != null && !path.trim().isEmpty() && Files.exists(Paths.get(path.trim()))) {
                    projectPathComboBox.addItem(path.trim());
                }
            }
            if (projectPathComboBox.getItemCount() > 0) {
                projectPathComboBox.setSelectedIndex(0);
            }
        }

        // URL 필터 (오른쪽)
        String urlFilter = settings.getUrlFilter();
        urlFilterField.setText(urlFilter != null ? urlFilter : "");

        // 왼쪽 엔드포인트 검색 필터는 저장하지 않음 (일시적 UI 상태)

        // 출력 스타일
        String style = settings.getOutputStyle();
        if (style == null) style = "normal";
        switch (style) {
            case "compact":
                rbCompact.setSelected(true);
                break;
            case "detailed":
                rbDetailed.setSelected(true);
                break;
            default:
                rbNormal.setSelected(true);
        }

        // SQL 타입 필터
        List<String> sqlTypes = settings.getSqlTypeFilter();
        if (sqlTypes != null && !sqlTypes.isEmpty()) {
            cbSelect.setSelected(sqlTypes.contains("SELECT"));
            cbInsert.setSelected(sqlTypes.contains("INSERT"));
            cbUpdate.setSelected(sqlTypes.contains("UPDATE"));
            cbDelete.setSelected(sqlTypes.contains("DELETE"));
        } else {
            // 저장된 설정이 없으면 모두 선택
            cbSelect.setSelected(true);
            cbInsert.setSelected(true);
            cbUpdate.setSelected(true);
            cbDelete.setSelected(true);
        }
    }

    /**
     * 최근 프로젝트 경로 저장
     */
    private void saveRecentPath(String newPath) {
        List<String> paths = new ArrayList<>();
        paths.add(newPath);

        for (int i = 0; i < projectPathComboBox.getItemCount(); i++) {
            String existingPath = projectPathComboBox.getItemAt(i);
            if (!existingPath.equals(newPath) && paths.size() < MAX_RECENT_PATHS) {
                paths.add(existingPath);
            }
        }

        projectPathComboBox.removeAllItems();
        for (String path : paths) {
            projectPathComboBox.addItem(path);
        }
        projectPathComboBox.setSelectedItem(newPath);

        // JSON에 저장 (왼쪽 필터는 저장하지 않음)
        sessionManager.saveSettings(paths, urlFilterField.getText().trim(), getSelectedStyle(), null, getSelectedSqlTypes());
    }

    /**
     * 설정 저장 (JSON 파일에)
     */
    private void saveSettings() {
        // 현재 ComboBox에서 경로 목록 수집
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < projectPathComboBox.getItemCount(); i++) {
            paths.add(projectPathComboBox.getItemAt(i));
        }

        // 왼쪽 엔드포인트 검색 필터는 저장하지 않음 (일시적 UI 상태)
        sessionManager.saveSettings(paths, urlFilterField.getText().trim(), getSelectedStyle(), null, getSelectedSqlTypes());
    }

    // ===== 세션 저장/복원 =====

    /**
     * 세션 저장 (분석 결과 포함)
     */
    private void saveSession() {
        if (originalResult == null || currentProjectPath == null) {
            return;
        }

        String urlFilter = urlFilterField.getText().trim();
        String outputStyle = getSelectedStyle();
        int selectedTabIndex = resultTabbedPane.getSelectedIndex();
        String selectedEndpoint = endpointList.getSelectedValue();
        String selectedTable = tableList.getSelectedValue();
        boolean tableDetailViewActive = tableImpactPanel.isQueryDetailViewActive();
        int selectedQueryRowIndex = tableImpactPanel.getSelectedQueryRowIndex();

        // 원본 결과 저장 (필터 없는 상태)
        boolean saved = sessionManager.saveSession(
                currentProjectPath.toString(),
                originalResult,
                urlFilter,
                outputStyle,
                selectedTabIndex,
                selectedEndpoint,
                selectedTable,
                tableDetailViewActive,
                selectedQueryRowIndex
        );

        if (saved) {
            System.out.println("세션 저장 완료: " + sessionManager.getSessionFilePath());
        }
    }

    /**
     * 세션 복원 (앱 시작 시 마지막 분석 결과 표시)
     */
    private void restoreSession() {
        SessionData session = sessionManager.loadSession();
        if (session == null || !session.isValid()) {
            return;
        }

        // 프로젝트 경로가 존재하는지 확인
        Path projectPath = Paths.get(session.getProjectPath());
        if (!Files.exists(projectPath)) {
            System.out.println("세션의 프로젝트 경로가 존재하지 않음: " + projectPath);
            return;
        }

        // 분석 결과 복원 (원본으로)
        originalResult = session.getFlowResult();
        currentProjectPath = projectPath;

        // CRUD 필터 적용
        if (!isAllSqlTypesSelected()) {
            List<String> sqlTypes = getSelectedSqlTypes();
            if (!sqlTypes.isEmpty()) {
                FlowAnalyzer analyzer = new FlowAnalyzer();
                currentResult = analyzer.filterBySqlType(originalResult, sqlTypes);
            } else {
                currentResult = originalResult;
            }
        } else {
            currentResult = originalResult;
        }

        // 저장된 상태 값들
        final int savedTabIndex = session.getSelectedTabIndex();
        final String savedSelectedEndpoint = session.getSelectedEndpoint();
        final String savedSelectedTable = session.getSelectedTable();
        final boolean savedTableDetailViewActive = session.isTableDetailViewActive();
        final int savedQueryRowIndex = session.getSelectedQueryRowIndex();

        // UI 업데이트
        SwingUtilities.invokeLater(() -> {
            // 요약 정보 업데이트
            updateSummaryPanel(currentResult);

            // 엔드포인트 목록 업데이트
            updateEndpointList(currentResult);

            // 결과 표시 (저장된 스타일 또는 현재 선택된 스타일)
            String style = session.getOutputStyle();
            if (style == null || style.isEmpty()) {
                style = getSelectedStyle();
            }
            resultPanel.displayResult(currentResult, style);

            // 테이블 영향도 업데이트 (먼저 데이터 설정)
            tableImpactPanel.updateData(currentResult);

            // 테이블 목록 업데이트 (데이터 설정 후 호출)
            updateTableList(currentResult);

            // 엔드포인트 선택 복원 (호출 흐름 탭)
            if (savedSelectedEndpoint != null && !savedSelectedEndpoint.isEmpty()) {
                for (int i = 0; i < endpointListModel.size(); i++) {
                    if (savedSelectedEndpoint.equals(endpointListModel.get(i))) {
                        endpointList.setSelectedIndex(i);
                        endpointList.ensureIndexIsVisible(i);
                        break;
                    }
                }
            }

            // 테이블 선택 복원 (테이블 영향도 탭)
            if (savedSelectedTable != null && !savedSelectedTable.isEmpty()) {
                for (int i = 0; i < tableListModel.size(); i++) {
                    if (savedSelectedTable.equals(tableListModel.get(i))) {
                        tableList.setSelectedIndex(i);
                        tableList.ensureIndexIsVisible(i);
                        // 쿼리 상세 화면이 활성화 상태였다면 복원
                        if (savedTableDetailViewActive && !savedSelectedTable.equals(ALL_TABLES)) {
                            tableImpactPanel.restoreQueryView(savedQueryRowIndex);
                        }
                        break;
                    }
                }
            }

            // 탭 선택 복원
            if (savedTabIndex >= 0 && savedTabIndex < resultTabbedPane.getTabCount()) {
                resultTabbedPane.setSelectedIndex(savedTabIndex);
            }

            // 상태 업데이트
            int totalCount = originalResult.getFlows().size();
            int shownCount = currentResult.getFlows().size();
            if (totalCount == shownCount) {
                statusLabel.setText(String.format("이전 세션 복원됨: %d개 URL (%s)",
                        totalCount, session.getAnalyzedAt().toLocalDate()));
            } else {
                statusLabel.setText(String.format("이전 세션 복원됨: %d / %d개 URL (필터 적용)",
                        shownCount, totalCount));
            }

            exportExcelButton.setEnabled(true);

            // 스크롤 복원은 UI 렌더링 후에 실행 (타이밍 문제 해결)
            if (savedSelectedEndpoint != null && !savedSelectedEndpoint.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    resultPanel.scrollToEndpoint(savedSelectedEndpoint);
                });
            }

            System.out.println("세션 복원 완료: " + currentProjectPath);
        });
    }

    /**
     * 에러 메시지 표시
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 엔드포인트 목록 업데이트
     */
    private void updateEndpointList(FlowResult result) {
        allEndpoints.clear();
        endpointListModel.clear();

        for (FlowNode flow : result.getFlows()) {
            String url = flow.getUrlMapping();
            if (url != null && !url.isEmpty()) {
                allEndpoints.add(url);
                endpointListModel.addElement(url);
            }
        }

        endpointCountLabel.setText(allEndpoints.size() + "개 항목");

        // JSplitPane divider 위치로 패널 표시/숨김 제어
        if (!allEndpoints.isEmpty()) {
            mainSplitPane.setDividerLocation(ENDPOINT_PANEL_WIDTH);
        }
    }

    /**
     * 엔드포인트 목록 필터링
     */
    private void filterEndpointList() {
        String filter = endpointSearchField.getText().toLowerCase().trim();
        endpointListModel.clear();

        int count = 0;
        for (String url : allEndpoints) {
            if (filter.isEmpty() || url.toLowerCase().contains(filter)) {
                endpointListModel.addElement(url);
                count++;
            }
        }

        endpointCountLabel.setText(count + "개 항목");
    }

    private static final String ALL_TABLES = "== 전체 ==";

    /**
     * 테이블 목록 업데이트 (테이블 영향도용)
     */
    private void updateTableList(FlowResult result) {
        allTableNames.clear();
        tableListModel.clear();

        if (result == null) {
            tableCountLabel.setText("0개 테이블");
            return;
        }

        // FlowAnalyzer에서 테이블 인덱스 빌드
        FlowAnalyzer analyzer = new FlowAnalyzer();
        Map<String, FlowAnalyzer.TableImpact> tableIndex = analyzer.buildTableIndex(result);

        allTableNames.addAll(tableIndex.keySet());
        allTableNames.sort(String::compareTo);

        // "전체" 옵션 추가
        tableListModel.addElement(ALL_TABLES);

        for (String tableName : allTableNames) {
            tableListModel.addElement(tableName);
        }

        tableCountLabel.setText(allTableNames.size() + "개 테이블");

        // "전체" 기본 선택 및 상세 화면 표시
        if (tableListModel.size() > 0) {
            tableList.setSelectedIndex(0);
            tableImpactPanel.displayTableAccesses(ALL_TABLES);
        }
    }

    /**
     * 테이블 목록 필터링
     */
    private void filterTableList() {
        String filter = tableSearchField.getText().toUpperCase().trim();
        tableListModel.clear();

        int count = 0;
        for (String tableName : allTableNames) {
            if (filter.isEmpty() || tableName.contains(filter)) {
                tableListModel.addElement(tableName);
                count++;
            }
        }

        tableCountLabel.setText(count + "개 테이블");
    }

    /**
     * CRUD 필터 적용 및 화면 갱신 (실시간 필터링)
     * 테이블 영향도 탭에서는 현재 테이블/쿼리 선택 상태 유지
     */
    private void applyFiltersAndRefresh() {
        if (originalResult == null) {
            return;  // 분석 결과 없으면 무시
        }

        // 테이블 영향도 탭인 경우 현재 상태 저장
        final int currentTab = resultTabbedPane.getSelectedIndex();
        final String savedTableSelection = tableList.getSelectedValue();
        final boolean savedQueryDetailActive = tableImpactPanel.isQueryDetailViewActive();
        final int savedQueryRowIndex = tableImpactPanel.getSelectedQueryRowIndex();

        // 필터링 적용
        FlowResult filtered;
        List<String> sqlTypes = getSelectedSqlTypes();
        if (sqlTypes.isEmpty()) {
            // 모든 체크박스 해제 시 빈 결과
            filtered = new FlowResult(originalResult.getProjectPath());
        } else if (!isAllSqlTypesSelected()) {
            // 일부만 선택 시 필터링
            FlowAnalyzer analyzer = new FlowAnalyzer();
            filtered = analyzer.filterBySqlType(originalResult, sqlTypes);
        } else {
            // 모두 선택 시 원본
            filtered = originalResult;
        }
        currentResult = filtered;

        // UI 갱신
        updateSummaryPanel(filtered);
        updateEndpointList(filtered);
        resultPanel.displayResult(filtered, getSelectedStyle());
        tableImpactPanel.updateData(filtered);

        // 테이블 영향도 탭인 경우 상태 복원
        if (currentTab == 1 && savedTableSelection != null) {
            // 테이블 목록 업데이트 (기본 선택 하지 않음)
            updateTableListWithoutSelection(filtered);

            // 저장된 테이블 선택 복원
            for (int i = 0; i < tableListModel.size(); i++) {
                if (savedTableSelection.equals(tableListModel.get(i))) {
                    tableList.setSelectedIndex(i);
                    tableList.ensureIndexIsVisible(i);
                    // 쿼리 상세 화면 복원
                    if (savedQueryDetailActive && !savedTableSelection.equals(ALL_TABLES)) {
                        tableImpactPanel.restoreQueryView(savedQueryRowIndex);
                    }
                    break;
                }
            }
        } else {
            // 호출 흐름 탭이거나 테이블 선택 없으면 기본 동작
            updateTableList(filtered);
        }

        // 상태 표시
        int total = originalResult.getFlows().size();
        int shown = filtered.getFlows().size();
        if (total == shown) {
            statusLabel.setText(String.format("전체 %d개 URL", total));
        } else {
            statusLabel.setText(String.format("필터 적용: %d / %d개 URL", shown, total));
        }

        // 설정 저장 (필터 상태)
        saveSettings();
    }

    /**
     * 테이블 목록 업데이트 (기본 선택 없이)
     * SQL 필터 변경 시 현재 선택 유지를 위해 사용
     */
    private void updateTableListWithoutSelection(FlowResult result) {
        allTableNames.clear();
        tableListModel.clear();

        if (result == null) {
            tableCountLabel.setText("0개 테이블");
            return;
        }

        // FlowAnalyzer에서 테이블 인덱스 빌드
        FlowAnalyzer analyzer = new FlowAnalyzer();
        Map<String, FlowAnalyzer.TableImpact> tableIndex = analyzer.buildTableIndex(result);

        allTableNames.addAll(tableIndex.keySet());
        allTableNames.sort(String::compareTo);

        // "전체" 옵션 추가
        tableListModel.addElement(ALL_TABLES);

        for (String tableName : allTableNames) {
            tableListModel.addElement(tableName);
        }

        tableCountLabel.setText(allTableNames.size() + "개 테이블");
    }

    /**
     * GUI 실행
     */
    public static void launch() {
        try {
            FlatDarculaLaf.setup();
        } catch (Exception e) {
            System.err.println("FlatLaf 테마 적용 실패: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
