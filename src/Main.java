import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Main {

    private JFrame mainFrame;
    private DefaultTableModel tableModel;
    private JTable StockTable;
    private JEditorPane InfoArea, SummaryArea;
    private JLabel logoLabel; // Label to show the company logo
    private JLabel titleLabel; // Label for company name

    private static final String FMP_API_KEY = "mi3WseUUB9U5p0SzlU35X7HsxYJeTmwz";
    private static final String OPENAI_API_KEY = "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";

    private static final String[] TICKERS = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "BRK-B", "JPM", "V",
            "JNJ", "WMT", "MA", "PG", "UNH", "HD", "BAC", "DIS", "ADBE", "CMCSA", "VZ",
            "NFLX", "INTC", "KO", "PFE", "T", "ABT", "PEP", "XOM", "ORCL", "CSCO", "CVX",
            "CRM", "WFC", "COST", "MRK", "MCD", "ACN", "NEE", "MS", "TMO", "MDT", "TXN",
            "HON", "ABBV", "LIN", "UNP", "PYPL", "NKE", "PM", "QCOM", "LOW", "AMAT",
            "SBUX", "IBM", "AMT", "GE", "AMD", "GS", "NOW", "INTU", "DE"
    };

    public Main() { initGUI(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    private void initGUI() {
        mainFrame = new JFrame("S&P 50 AI Screener with Logos");
        mainFrame.setSize(1250, 850);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        //Table Section
        tableModel = new DefaultTableModel(new String[]{"Ticker","Company","Price","Change%","Day High"}, 0);
        StockTable = new JTable(tableModel);
        StockTable.setRowHeight(30);
        StockTable.getSelectionModel().addListSelectionListener(new RowSelectListener());

        //Details Section (Top Right)
        JPanel detailsPanel = new JPanel(new BorderLayout());

        // Header with Logo and Title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(Color.WHITE);
        logoLabel = new JLabel();
        titleLabel = new JLabel("Select a Stock");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        headerPanel.add(logoLabel);
        headerPanel.add(titleLabel);

        InfoArea = new JEditorPane("text/html", "");
        InfoArea.setEditable(false);

        detailsPanel.add(headerPanel, BorderLayout.NORTH);
        detailsPanel.add(new JScrollPane(InfoArea), BorderLayout.CENTER);

        //AI Summary Section (Bottom Right)
        SummaryArea = new JEditorPane("text/html", "");
        SummaryArea.setEditable(false);
        SummaryArea.setBackground(new Color(245, 248, 255));

        // Layout Splits
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detailsPanel, new JScrollPane(SummaryArea));
        rightSplit.setDividerLocation(400);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(StockTable), rightSplit);
        mainSplit.setDividerLocation(500);

        mainFrame.add(mainSplit, BorderLayout.CENTER);
        mainFrame.setVisible(true);

        loadQuotes();
    }

    private void loadQuotes() {
        new Thread(() -> {
            for (String ticker : TICKERS) {
                String urlStr = "https://financialmodelingprep.com/stable/quote?symbol=" + ticker + "&apikey=" + FMP_API_KEY;
                String json = makeRequest(urlStr);

                if (json != null && !json.contains("Error Message")) {
                    String name = extract(json, "name");
                    String price = extract(json, "price");
                    String change = extract(json, "changesPercentage");
                    String high = extract(json, "dayHigh");
                    SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{ticker, name, price, change, high}));
                }
                try { Thread.sleep(50); } catch (Exception e) {}
            }
        }).start();
    }

    private class RowSelectListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;
            int row = StockTable.getSelectedRow();
            if (row == -1) return;

            String symbol = tableModel.getValueAt(row, 0).toString();
            String name = tableModel.getValueAt(row, 1).toString();

            titleLabel.setText(name + " (" + symbol + ")");
            InfoArea.setText("<html><body style='font-family:sans-serif;'><h3>Loading profile...</h3></body></html>");
            SummaryArea.setText("<html><body style='font-family:sans-serif;'><i>Analyzing Market Data...</i></body></html>");
            logoLabel.setIcon(null);

            new Thread(() -> {
                // 1. Load Company Logo
                loadLogo(symbol);

                // 2. Load Profile Data
                String profileUrl = "https://financialmodelingprep.com/stable/profile?symbol=" + symbol + "&apikey=" + FMP_API_KEY;
                String json = makeRequest(profileUrl);

                if (json != null) {
                    String industry = extract(json, "industry");
                    String desc = extract(json, "description");

                    SwingUtilities.invokeLater(() -> InfoArea.setText("<html><body style='font-family:sans-serif;'><b>Industry:</b> " + industry + "<br><br>" + desc + "</body></html>"));

                    // 3. Get AI Analysis
                    String summary = getAISummary(desc);
                    SwingUtilities.invokeLater(() -> SummaryArea.setText("<html><body style='font-family:sans-serif; padding:10px;'><h2 style='color:#1a2a6c;'>AI Investment Analysis</h2><p style='font-size:11pt;'>" + summary + "</p></body></html>"));
                }
            }).start();
        }
    }

    private void loadLogo(String symbol) {
        try {
            // FMP image
            URL url = new URL("https://financialmodelingprep.com/image-stock/" + symbol + ".png");
            BufferedImage img = ImageIO.read(url);
            if (img != null) {
                // Scale image to fit the header nicely
                Image scaled = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                SwingUtilities.invokeLater(() -> logoLabel.setIcon(new ImageIcon(scaled)));
            }
        } catch (Exception e) {
            System.out.println("No logo found for " + symbol);
        }
    }

    private String makeRequest(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            if (conn.getResponseCode() != 200) return null;

            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return r.lines().collect(Collectors.joining());
            }
        } catch (Exception e) { return null; }
    }

    private String extract(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int start = json.indexOf(search);
            if (start == -1) return "N/A";

            start += search.length();
            while (json.charAt(start) == ' ' || json.charAt(start) == '\"' || json.charAt(start) == ':') start++;

            int end;
            if (json.charAt(start - 1) == '\"') {
                end = start;
                while (end < json.length()) {
                    if (json.charAt(end) == '\"' && json.charAt(end - 1) != '\\') break;
                    end++;
                }
            } else {
                int comma = json.indexOf(",", start);
                int brace = json.indexOf("}", start);
                int bracket = json.indexOf("]", start);
                end = Math.min(comma != -1 ? comma : Integer.MAX_VALUE,
                        Math.min(brace != -1 ? brace : Integer.MAX_VALUE,
                                bracket != -1 ? bracket : Integer.MAX_VALUE));
            }
            String result = json.substring(start, end).trim();
            return result.replace("\\n", " ").replace("\\\"", "\"");
        } catch (Exception e) { return "N/A"; }
    }

    private String getAISummary(String desc) {
        if (desc == null || desc.equals("N/A") || desc.length() < 10) return "No description available.";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String cleanDesc = desc.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ").replace("\r", " ");

            String prompt = "Give a 4-5 sentence unique investment analysis for this company based on its profile. "
                    + "At the end, provide a final consensus: Buy, Sell, or Hold (with strength). Description: " + cleanDesc;

            String body = "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"system\",\"content\":\"You are a financial analyst.\"},"
                    + "{\"role\":\"user\",\"content\":\"" + prompt + "\"}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            if (conn.getResponseCode() != 200) return "AI unavailable (Check key/balance).";

            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = r.lines().collect(Collectors.joining());
                return extract(response, "content");
            }
        } catch (Exception e) {
            return "Connection error with AI service.";
        }
    }
}