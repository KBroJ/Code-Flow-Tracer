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
    private JSplitPane mainSplitPane;
    private static final int ENDPOINT_PANEL_WIDTH = 200;

    // 엔드포인트 목록 컴포넌트
    private JTextField endpointSearchField;
    private JList<String> endpointList;
    private DefaultListModel<String> endpointListModel;
    private JLabel endpointCountLabel;
    private List<String> allEndpoints = new ArrayList<>();

    // 분석 요약 패널
    private JPanel summaryPanel;
    private JLabel lblTotalClasses;
    private JLabel lblControllerCount;
    private JLabel lblServiceCount;
    private JLabel lblDaoCount;
    private JLabel lblEndpointCount;

    // 프로젝트 경로
    private JComboBox<String> projectPathComboBox;
    private JButton browseButton;

    // 분석 옵션
    private JTextField urlFilterField;
    private JRadioButton rbCompact;
    private JRadioButton rbNormal;
    private JRadioButton rbDetailed;
    private ButtonGroup styleGroup;

    // 액션 버튼
    private JButton analyzeButton;
    private JButton exportExcelButton;
    private JButton settingsButton;

    // 결과 표시
    private ResultPanel resultPanel;

    // 진행 상태
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // 분석 결과 캐시
    private FlowResult currentResult;
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
                System.exit(0);
            }
        });
    }

    /**
     * UI 컴포넌트 초기화
     */
    private void initializeComponents() {
        // 분석 요약 라벨
        lblTotalClasses = new JLabel("-");
        lblControllerCount = new JLabel("-");
        lblServiceCount = new JLabel("-");
        lblDaoCount = new JLabel("-");
        lblEndpointCount = new JLabel("-");

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
    }

    /**
     * 레이아웃 구성
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());

        // 좌측 엔드포인트 목록 패널
        endpointListPanel = createEndpointListPanel();

        // 메인 영역 (결과 패널)
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(resultPanel, BorderLayout.CENTER);

        // JSplitPane: 좌측 URL 목록 + 결과 패널 (드래그 조절 가능)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, endpointListPanel, mainPanel);
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

        // 요약 테이블 (전체 너비 사용, 점선 리더로 채움)
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

        section.add(tablePanel);
        section.add(Box.createVerticalStrut(12));
        section.add(createSeparator());
        section.add(Box.createVerticalStrut(12));

        return section;
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

        // URL 필터
        JLabel urlLabel = new JLabel("URL 필터");
        urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(urlLabel);
        section.add(Box.createVerticalStrut(3));

        urlFilterField.setAlignmentX(Component.LEFT_ALIGNMENT);
        urlFilterField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        section.add(urlFilterField);
        section.add(Box.createVerticalStrut(12));

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
                rbNormal.setSelected(true);
                endpointListModel.clear();  // 왼쪽 엔드포인트 목록 초기화
                resultPanel.clear();  // 분석 결과 화면도 초기화
                currentResult = null;  // 분석 결과 객체도 초기화
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
        summaryPanel.setVisible(false);

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
                    currentResult = result;
                    currentProjectPath = projectPath;

                    // 요약 정보 업데이트
                    updateSummaryPanel(result);
                    summaryPanel.setVisible(true);

                    // 엔드포인트 목록 업데이트
                    updateEndpointList(result);

                    // 결과 표시
                    String selectedStyle = getSelectedStyle();
                    resultPanel.displayResult(result, selectedStyle);

                    // 상태 업데이트
                    int endpointCount = result.getFlows().size();
                    statusLabel.setText(String.format("분석 완료: %d개 URL 발견", endpointCount));
                    progressBar.setString("완료");

                    exportExcelButton.setEnabled(true);

                    // 설정 저장
                    saveRecentPath(projectPath.toString());
                    saveSettings();

                    // 세션 저장 (분석 결과 영속성)
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
     * 분석 요약 패널 업데이트
     */
    private void updateSummaryPanel(FlowResult result) {
        lblTotalClasses.setText(result.getTotalClasses() + "개");
        lblControllerCount.setText(result.getControllerCount() + "개");
        lblServiceCount.setText(result.getServiceCount() + "개");
        lblDaoCount.setText(result.getDaoCount() + "개");
        lblEndpointCount.setText(result.getEndpointCount() + "개");
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
        sessionManager.saveSettings(paths, urlFilterField.getText().trim(), getSelectedStyle(), null);
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
        sessionManager.saveSettings(paths, urlFilterField.getText().trim(), getSelectedStyle(), null);
    }

    // ===== 세션 저장/복원 =====

    /**
     * 세션 저장 (분석 결과 포함)
     */
    private void saveSession() {
        if (currentResult == null || currentProjectPath == null) {
            return;
        }

        String urlFilter = urlFilterField.getText().trim();
        String outputStyle = getSelectedStyle();

        boolean saved = sessionManager.saveSession(
                currentProjectPath.toString(),
                currentResult,
                urlFilter,
                outputStyle
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

        // 분석 결과 복원
        currentResult = session.getFlowResult();
        currentProjectPath = projectPath;

        // UI 업데이트
        SwingUtilities.invokeLater(() -> {
            // 요약 정보 업데이트
            updateSummaryPanel(currentResult);
            summaryPanel.setVisible(true);

            // 엔드포인트 목록 업데이트
            updateEndpointList(currentResult);

            // 결과 표시 (저장된 스타일 또는 현재 선택된 스타일)
            String style = session.getOutputStyle();
            if (style == null || style.isEmpty()) {
                style = getSelectedStyle();
            }
            resultPanel.displayResult(currentResult, style);

            // 상태 업데이트
            int endpointCount = currentResult.getFlows().size();
            statusLabel.setText(String.format("이전 세션 복원됨: %d개 URL (%s)",
                    endpointCount, session.getAnalyzedAt().toLocalDate()));

            exportExcelButton.setEnabled(true);

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
