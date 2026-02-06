import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Main implements ActionListener {

    private JFrame mainFrame;

    private JPanel TopPanel;
    private JPanel BottomPanel;
    private JPanel LeftPanel;
    private JPanel RightPanel;
    private JPanel FilterPanel;
    private JPanel TablePanel;
    private JPanel DetailsPanel;
    private JPanel SummaryPanel;
    private JPanel ButtonPanel;

    // Controls
    private JTextField SearchField;
    private JComboBox<String> SortDropdown;
    private JButton ApplyButton;
    private JButton ClearButton;

    private JTable StockTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // Output areas
    private JEditorPane InfoArea;     // company info
    private JEditorPane SummaryArea;  // text-gen summary

    private int WIDTH = 1100;
    private int HEIGHT = 750;

    public Main() {
        try {
            initGUI();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        new Main();

    }

    private void initGUI() {
        mainFrame = new JFrame("S&P 500 Stock Screener");
        mainFrame.setSize(WIDTH, HEIGHT);
        mainFrame.setLayout(new BorderLayout());

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        TopPanel = new JPanel();
        TopPanel.setLayout(new BorderLayout());
        TopPanel.setVisible(true);

        BottomPanel = new JPanel();
        BottomPanel.setLayout(new GridLayout(1, 2));
        BottomPanel.setVisible(true);

        LeftPanel = new JPanel();
        LeftPanel.setLayout(new BorderLayout());
        LeftPanel.setVisible(true);

        RightPanel = new JPanel();
        RightPanel.setLayout(new GridLayout(2, 1));
        RightPanel.setVisible(true);

        FilterPanel = new JPanel();
        FilterPanel.setLayout(new GridLayout(1, 5));
        FilterPanel.setVisible(true);

        TablePanel = new JPanel();
        TablePanel.setLayout(new BorderLayout());
        TablePanel.setVisible(true);

        DetailsPanel = new JPanel();
        DetailsPanel.setLayout(new BorderLayout());
        DetailsPanel.setVisible(true);

        SummaryPanel = new JPanel();
        SummaryPanel.setLayout(new BorderLayout());
        SummaryPanel.setVisible(true);

        ButtonPanel = new JPanel();
        ButtonPanel.setLayout(new GridLayout(1, 2));
        ButtonPanel.setVisible(true);

        JLabel SearchLabel = new JLabel(" Search:", JLabel.RIGHT);
        SearchLabel.setSize(200, 20);

        SearchField = new JTextField();
        SearchField.setText("");
        SearchField.setSize(300, 20);

        JLabel SortLabel = new JLabel(" Sort:", JLabel.RIGHT);
        SortLabel.setSize(200, 20);

        SortDropdown = new JComboBox<>(new String[]{
                "Ticker (A-Z)",
                "Company (A-Z)",
                "Sector (A-Z)",
                "Market Cap",
                "P/E",
                "Dividend"
        });

        ApplyButton = new JButton("Apply");
        ApplyButton.setActionCommand("apply");
        ApplyButton.addActionListener(new ButtonClickListener());

        ClearButton = new JButton("Clear");
        ClearButton.setActionCommand("clear");
        ClearButton.addActionListener(new ButtonClickListener());

        ButtonPanel.add(ApplyButton);
        ButtonPanel.add(ClearButton);

        FilterPanel.add(SearchLabel);
        FilterPanel.add(SearchField);
        FilterPanel.add(SortLabel);
        FilterPanel.add(SortDropdown);
        FilterPanel.add(ButtonPanel);

        TopPanel.add(FilterPanel, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(new String[]{
                "Ticker", "Company", "Sector", "Market Cap", "P/E", "Dividend"
        }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        StockTable = new JTable(tableModel);
        StockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        StockTable.setRowHeight(24);

        sorter = new TableRowSorter<>(tableModel);
        StockTable.setRowSorter(sorter);

        StockTable.getSelectionModel().addListSelectionListener(new RowSelectListener());

        JScrollPane tableScroll = new JScrollPane(StockTable);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        TablePanel.add(tableScroll, BorderLayout.CENTER);

        InfoArea = new JEditorPane();
        InfoArea.setEditable(false);
        InfoArea.setContentType("text/html");
        InfoArea.setText("<html><body></body></html>"); // blank by default

        SummaryArea = new JEditorPane();
        SummaryArea.setEditable(false);
        SummaryArea.setContentType("text/html");
        SummaryArea.setText("<html><body></body></html>"); // blank by default

        JScrollPane infoScroll = new JScrollPane(InfoArea);
        infoScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JScrollPane summaryScroll = new JScrollPane(SummaryArea);
        summaryScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        DetailsPanel.add(infoScroll, BorderLayout.CENTER);
        SummaryPanel.add(summaryScroll, BorderLayout.CENTER);

        RightPanel.add(DetailsPanel);
        RightPanel.add(SummaryPanel);

        LeftPanel.add(TablePanel, BorderLayout.CENTER);
        BottomPanel.add(LeftPanel);
        BottomPanel.add(RightPanel);

        mainFrame.add(TopPanel, BorderLayout.NORTH);
        mainFrame.add(BottomPanel, BorderLayout.CENTER);

        mainFrame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    private class ButtonClickListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            if (command.equals("apply")) {
            }

            if (command.equals("clear")) {
                SearchField.setText("");
            }
        }
    }

    private class RowSelectListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;

        }
    }


    private static final String MASSIVE_API_KEY = "6AfWpeazzhtzecpGKDKuL0Oq0ggpePYp";

    private String fetchMassiveApi(String endpoint) {
        StringBuilder result = new StringBuilder();

        try {
            // Construct full URL (endpoint must be something like "/v1/stocks/somePath")
            String urlStr = "https://api.massive.com" + endpoint
                    + "?apiKey=6AfWpeazzhtzecpGKDKuL0Oq0ggpePYp" + MASSIVE_API_KEY;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Failed: HTTP error code " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return result.toString(); // raw JSON string
    }
}

