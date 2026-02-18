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

public class Main {

    private JFrame mainFrame;
    private DefaultTableModel tableModel;
    private JTable StockTable;
    private JEditorPane InfoArea, SummaryArea;

    private static final String FMP_API_KEY = "mi3WseUUB9U5p0SzlU35X7HsxYJeTmwz";
    private static final String OPENAI_API_KEY = "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";

    // Expanded S&P 100 List (~60 Tickers)
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
        mainFrame = new JFrame("S&P 100 AI Screener");
        mainFrame.setSize(1200, 800);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        // Table UI
        tableModel = new DefaultTableModel(new String[]{"Ticker","Company","Price","Change%","Day High"}, 0);
        StockTable = new JTable(tableModel);
        StockTable.setRowHeight(25);
        StockTable.getSelectionModel().addListSelectionListener(new RowSelectListener());

        // Display Panels
        InfoArea = new JEditorPane("text/html", "");
        SummaryArea = new JEditorPane("text/html", "");
        InfoArea.setEditable(false);
        SummaryArea.setEditable(false);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(InfoArea), new JScrollPane(SummaryArea));
        rightSplit.setDividerLocation(400);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(StockTable), rightSplit);
        mainSplit.setDividerLocation(550);

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
                // Small sleep to avoid hitting rate limits on Starter Tier
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
            InfoArea.setText("<html><body style='font-family:sans-serif;'><h3>Loading " + symbol + " profile...</h3></body></html>");
            SummaryArea.setText("<html><body style='font-family:sans-serif;'><i>Generating AI Summary...</i></body></html>");

            new Thread(() -> {
                String profileUrl = "https://financialmodelingprep.com/stable/profile?symbol=" + symbol + "&apikey=" + FMP_API_KEY;
                String json = makeRequest(profileUrl);

                if (json != null) {
                    String industry = extract(json, "industry");
                    String desc = extract(json, "description");

                    SwingUtilities.invokeLater(() -> InfoArea.setText("<html><body style='font-family:sans-serif;'><h2>" + symbol + "</h2><b>Industry:</b> " + industry + "<br><br>" + desc + "</body></html>"));

                    String summary = getAISummary(desc);
                    SwingUtilities.invokeLater(() -> SummaryArea.setText("<html><body style='font-family:sans-serif;'><h2 style='color:#2c3e50;'>AI Analysis</h2><p style='font-size:12pt;'>" + summary + "</p></body></html>"));
                }
            }).start();
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

    // Improved extractor to handle nested JSON and escaped quotes
    private String extract(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int start = json.indexOf(search);
            if (start == -1) return "N/A";

            start += search.length();
            while (json.charAt(start) == ' ' || json.charAt(start) == '\"' || json.charAt(start) == ':') start++;

            int end;
            if (json.charAt(start - 1) == '\"') {
                // Find end quote but ignore escaped ones \"
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
            // Clean up JSON artifacts
            return result.replace("\\n", " ").replace("\\\"", "\"");
        } catch (Exception e) { return "N/A"; }
    }

    private String getAISummary(String desc) {
        if (desc == null || desc.equals("N/A") || desc.length() < 10) return "No description available for AI to summarize.";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            //Clean the description so it doesn't break the JSON structure
            String cleanDesc = desc.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ").replace("\r", " ");

            String body = "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"system\",\"content\":\"You are a helpful financial analyst.\"},{\"role\":\"user\",\"content\":\"Give advice on some of your own investment analysis as if you were a simple investment ai, that analyses some key unique factors when it comes to the said company. Make your response 4-5 sentences, and in a normal paragraph text.A summary will already be provided, so don't repeat anything unless very substantial: " + cleanDesc + "\"}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            if (conn.getResponseCode() != 200) {
                return "AI Error (HTTP " + conn.getResponseCode() + "). Check your API key and balance.";
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = r.lines().collect(Collectors.joining());
                // The content is specifically inside the 'content' field of the 'message' object
                return extract(response, "content");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Summary unavailable due to a connection error.";
        }
    }
}