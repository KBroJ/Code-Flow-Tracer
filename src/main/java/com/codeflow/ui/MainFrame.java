package com.codeflow.ui;

import com.codeflow.analyzer.FlowAnalyzer;
import com.codeflow.analyzer.FlowResult;
import com.codeflow.output.ExcelOutput;
import com.codeflow.parser.IBatisParser;
import com.codeflow.parser.JavaSourceParser;
import com.codeflow.parser.ParsedClass;
import com.codeflow.parser.SqlInfo;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Code Flow Tracer GUI 메인 프레임
 *
 * 프로젝트 경로 선택, 분석 옵션 설정, 결과 표시 기능 제공
 */
public class MainFrame extends JFrame {

    private static final String TITLE = "Code Flow Tracer";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 800;

    // 설정 저장 (Preferences API)
    private static final String PREF_RECENT_PATHS = "recentPaths";
    private static final String PREF_URL_FILTER = "urlFilter";
    private static final String PREF_OUTPUT_STYLE = "outputStyle";
    private static final int MAX_RECENT_PATHS = 10;
    private final Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);

    // 경로 선택 (콤보박스로 최근 경로 드롭다운)
    private JComboBox<String> projectPathComboBox;
    private JButton browseButton;

    // 분석 옵션
    private JTextField urlFilterField;
    private JComboBox<String> styleComboBox;

    // 분석 버튼
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

    public MainFrame() {
        initializeFrame();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    /**
     * 프레임 기본 설정
     */
    private void initializeFrame() {
        setTitle(TITLE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 화면 중앙에 표시

        // 창 닫을 때 확실히 프로세스 종료
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * 설정 팝업 메뉴 생성
     */
    private JPopupMenu createSettingsPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem clearSettingsItem = new JMenuItem("설정 초기화");
        clearSettingsItem.setToolTipText("저장된 최근 경로 및 옵션 설정을 모두 삭제합니다");
        clearSettingsItem.addActionListener(e -> handleClearSettings());
        popup.add(clearSettingsItem);

        return popup;
    }

    /**
     * 설정 초기화 핸들러
     */
    private void handleClearSettings() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "저장된 모든 설정(최근 경로, URL 필터, 출력 스타일)을 삭제합니다.\n계속하시겠습니까?",
                "설정 초기화",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                prefs.clear();
                projectPathComboBox.removeAllItems();
                urlFilterField.setText("");
                styleComboBox.setSelectedItem("normal");
                statusLabel.setText("설정이 초기화되었습니다.");
            } catch (Exception ex) {
                showError("설정 초기화 실패: " + ex.getMessage());
            }
        }
    }

    /**
     * UI 컴포넌트 초기화
     */
    private void initializeComponents() {
        // 경로 선택 (편집 가능한 콤보박스)
        projectPathComboBox = new JComboBox<>();
        projectPathComboBox.setEditable(true);
        projectPathComboBox.setPreferredSize(new Dimension(500, 28));
        loadRecentPaths();
        browseButton = new JButton("찾아보기...");

        // 분석 옵션
        urlFilterField = new JTextField(20);
        urlFilterField.setToolTipText("예: /api/user/*, /user/** (빈칸이면 전체 분석)");
        urlFilterField.setText(prefs.get(PREF_URL_FILTER, ""));

        styleComboBox = new JComboBox<>(new String[]{"normal", "compact", "detailed"});
        styleComboBox.setSelectedItem(prefs.get(PREF_OUTPUT_STYLE, "normal"));

        // 버튼
        analyzeButton = new JButton("분석 시작");
        analyzeButton.setFont(analyzeButton.getFont().deriveFont(Font.BOLD));

        exportExcelButton = new JButton("엑셀 저장");
        exportExcelButton.setEnabled(false); // 분석 전에는 비활성화

        settingsButton = new JButton("\u2699"); // ⚙ 톱니바퀴
        settingsButton.setToolTipText("설정");
        settingsButton.setFont(settingsButton.getFont().deriveFont(16f));

        // 결과 패널
        resultPanel = new ResultPanel();

        // 진행 상태
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString("대기 중");

        statusLabel = new JLabel("프로젝트 경로를 선택하고 '분석 시작' 버튼을 클릭하세요.");
    }

    /**
     * 레이아웃 구성
     */
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));

        // 상단 패널: 경로 선택 + 옵션
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 중앙: 결과 표시 (ResultPanel 내부에 이미 JScrollPane 있음)
        resultPanel.setBorder(new TitledBorder("분석 결과"));
        add(resultPanel, BorderLayout.CENTER);

        // 하단: 진행 상태
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        // 여백 설정
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    /**
     * 상단 패널 생성 (경로 선택 + 옵션)
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // 경로 선택 패널
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setBorder(new TitledBorder("프로젝트 경로"));
        pathPanel.add(projectPathComboBox, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        // 옵션 패널
        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        optionPanel.setBorder(new TitledBorder("분석 옵션"));

        optionPanel.add(new JLabel("URL 필터:"));
        optionPanel.add(urlFilterField);
        optionPanel.add(Box.createHorizontalStrut(20));
        optionPanel.add(new JLabel("출력 스타일:"));
        optionPanel.add(styleComboBox);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.add(exportExcelButton);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(settingsButton);

        // 조합
        JPanel upperPanel = new JPanel(new BorderLayout(10, 5));
        upperPanel.add(pathPanel, BorderLayout.NORTH);
        upperPanel.add(optionPanel, BorderLayout.CENTER);

        panel.add(upperPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 하단 패널 생성 (진행 상태)
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(new EmptyBorder(5, 0, 0, 0));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(progressBar, BorderLayout.CENTER);

        return panel;
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

        // 설정 버튼 (팝업 메뉴 표시)
        JPopupMenu settingsPopup = createSettingsPopupMenu();
        settingsButton.addActionListener(e -> {
            settingsPopup.show(settingsButton, 0, settingsButton.getHeight());
        });

        // Enter 키로 분석 시작 (콤보박스 에디터에 리스너 추가)
        JTextField comboEditor = (JTextField) projectPathComboBox.getEditor().getEditorComponent();
        comboEditor.addActionListener(this::handleAnalyze);
        urlFilterField.addActionListener(this::handleAnalyze);
    }

    /**
     * 찾아보기 버튼 핸들러
     */
    private void handleBrowse(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("분석할 프로젝트 폴더 선택");

        // 현재 경로가 있으면 해당 위치에서 시작
        String currentPath = getSelectedPath();
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
     * 콤보박스에서 현재 선택/입력된 경로 가져오기
     */
    private String getSelectedPath() {
        Object item = projectPathComboBox.getEditor().getItem();
        return item != null ? item.toString().trim() : "";
    }

    /**
     * 분석 시작 버튼 핸들러
     */
    private void handleAnalyze(ActionEvent e) {
        String pathStr = getSelectedPath();
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

        // 분석 실행 (백그라운드 스레드)
        startAnalysis(projectPath);
    }

    /**
     * 백그라운드에서 분석 실행
     */
    private void startAnalysis(Path projectPath) {
        String urlPattern = urlFilterField.getText().trim();

        // UI 상태 변경
        setUIEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("분석 중...");
        statusLabel.setText("프로젝트를 분석하고 있습니다...");
        resultPanel.clear();

        // SwingWorker로 백그라운드 실행
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

                    // 결과 표시
                    String selectedStyle = (String) styleComboBox.getSelectedItem();
                    resultPanel.displayResult(result, selectedStyle);

                    // 통계 표시
                    int endpointCount = result.getFlows().size();
                    statusLabel.setText(String.format("분석 완료: %d개 엔드포인트 발견", endpointCount));
                    progressBar.setString("완료");

                    // 엑셀 저장 버튼 활성화
                    exportExcelButton.setEnabled(true);

                    // 설정 저장 (분석 성공 시)
                    saveRecentPath(projectPath.toString());
                    saveSettings();

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
     * 엑셀 저장 버튼 핸들러
     */
    private void handleExportExcel(ActionEvent e) {
        if (currentResult == null) {
            showError("먼저 분석을 실행하세요.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("엑셀 파일 저장");
        chooser.setSelectedFile(new java.io.File("code-flow-result.xlsx"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel 파일 (*.xlsx)", "xlsx"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path outputPath = chooser.getSelectedFile().toPath();

            // .xlsx 확장자 추가
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
        styleComboBox.setEnabled(enabled);
        analyzeButton.setEnabled(enabled);
        exportExcelButton.setEnabled(enabled && currentResult != null);
    }

    // ===== 설정 저장/로드 =====

    /**
     * 최근 경로 목록 로드
     */
    private void loadRecentPaths() {
        String pathsStr = prefs.get(PREF_RECENT_PATHS, "");
        if (!pathsStr.isEmpty()) {
            String[] paths = pathsStr.split("\\|");
            for (String path : paths) {
                if (!path.trim().isEmpty() && Files.exists(Paths.get(path.trim()))) {
                    projectPathComboBox.addItem(path.trim());
                }
            }
            // 가장 최근 경로 선택
            if (projectPathComboBox.getItemCount() > 0) {
                projectPathComboBox.setSelectedIndex(0);
            }
        }
    }

    /**
     * 최근 경로 저장 (가장 최근 경로를 맨 위로)
     */
    private void saveRecentPath(String newPath) {
        List<String> paths = new ArrayList<>();
        paths.add(newPath);

        // 기존 항목 중 중복 제거하고 추가
        for (int i = 0; i < projectPathComboBox.getItemCount(); i++) {
            String existingPath = projectPathComboBox.getItemAt(i);
            if (!existingPath.equals(newPath) && paths.size() < MAX_RECENT_PATHS) {
                paths.add(existingPath);
            }
        }

        // 콤보박스 갱신
        projectPathComboBox.removeAllItems();
        for (String path : paths) {
            projectPathComboBox.addItem(path);
        }
        projectPathComboBox.setSelectedItem(newPath);

        // Preferences에 저장
        prefs.put(PREF_RECENT_PATHS, String.join("|", paths));
    }

    /**
     * URL 필터, 출력 스타일 설정 저장
     */
    private void saveSettings() {
        prefs.put(PREF_URL_FILTER, urlFilterField.getText().trim());
        prefs.put(PREF_OUTPUT_STYLE, (String) styleComboBox.getSelectedItem());
    }

    /**
     * 에러 메시지 표시
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * GUI 실행
     */
    public static void launch() {
        // FlatLaf 다크 테마 적용 (컴포넌트 생성 전에 설정)
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
