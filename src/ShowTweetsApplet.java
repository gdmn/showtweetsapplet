
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JApplet;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

/**
 *
 * @author dmn
 */
public class ShowTweetsApplet extends JApplet {

    private StringBuffer buffer = new StringBuffer();

    /**
     * Initialization method that will be called after the applet is loaded
     * into the browser.
     */
    public void init() {
        BufferedReader in = null;
        try {
            //URL feed = new URL("http://twitter.com/statuses/user_timeline/48274520.rss");
            URL feed = new URL("http://api.twitter.com/1/statuses/user_timeline.xml?screen_name=g_damian");
            in = new BufferedReader(new InputStreamReader(feed.openStream()));
            String inputLine;
            String data = null;
            while ((inputLine = in.readLine()) != null) {
                inputLine = inputLine.trim();
                if (inputLine.startsWith("<created_at>") && inputLine.endsWith("</created_at>")) {
                    inputLine = inputLine.substring(12, inputLine.length() - 13);
                    data = inputLine.replaceAll("\\+\\d* ", "");
                }
                if (inputLine.startsWith("<text>") && inputLine.endsWith("</text>")) {
                    inputLine = inputLine.substring(6, inputLine.length() - 7);
                    buffer.append(escapeMessage(inputLine));
                    buffer.append("<br/>[" + data + "]");
                    buffer.append("\n");
                }
            }
            in.close();
        } catch (MalformedURLException ex) {
            Logger.getLogger(ShowTweetsApplet.class.getName()).log(Level.SEVERE, null, ex);
            appendException(buffer, ex);
        } catch (IOException ex) {
            Logger.getLogger(ShowTweetsApplet.class.getName()).log(Level.SEVERE, null, ex);
            appendException(buffer, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ShowTweetsApplet.class.getName()).log(Level.SEVERE, null, ex);
                appendException(buffer, ex);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void start() {
        super.start();
        if (!Boolean.parseBoolean(getParameter("hidden"))) {
            JEditorPane editorPane = createEditorPane();
            editorPane.setText(getTweets());
            JScrollPane editorScrollPane = new JScrollPane(editorPane);
            editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            add(editorScrollPane);
        } else {
            add(new JLabel("hidden"));
        }
    }

    private JEditorPane createEditorPane() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(true);
        editorPane.setOpaque(true);
        editorPane.addHyperlinkListener(hyperlinkListener);
        ((HTMLDocument) editorPane.getDocument()).getStyleSheet().addRule(
                String.format("p { font-family: %s; font-size: %dpx; margin: 0px 0px 5px 0px; }",
                "Droid Sans", 10));
        return editorPane;
    }

    public String getTweets() {
        return buffer == null ? "null!" : parseMessage(buffer.toString());
    }

    public String getTest() {
        return "return test";
    }
    private HyperlinkListener hyperlinkListener = new HyperlinkListener() {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JEditorPane pane = (JEditorPane) e.getSource();

                if (e instanceof HTMLFrameHyperlinkEvent) {
                    HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
                    HTMLDocument doc = (HTMLDocument) pane.getDocument();
                    doc.processHTMLFrameHyperlinkEvent(evt);
                } else {
                    try {
                        if (Desktop.isDesktopSupported() && (Desktop.getDesktop() != null)) {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    };
    private static final Pattern urlPattern = Pattern.compile("(ftp|http|https|smb):\\/\\/" + "[^\\s^<^>]" + "+", Pattern.CASE_INSENSITIVE);

    private static String preParsingMessage(String message) {
        return message.replace("\r\n", "\n").
                replace("\n", "</p><p>");
    }

    private static String escapeMessage(String message) {
        return message.replace("<", "&lt;").
                replace(">", "&gt;");
    }

    public static String parseMessage(String message) {
        Matcher m = urlPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        int cp = 0;
        String append;
        while (m.find()) {
            String link = message.substring(m.start(), m.end());
            String title;
            title = preParsingMessage(link);
            append = preParsingMessage(message.substring(cp, m.start()));
            sb.append(append);
            cp = m.end();
            sb.append(String.format("<a href=\"%s\">%s</a>", link, title));
            //m.appendReplacement(sb, String.format("<a href=\"%s\">%s</a>", link, title));
        }
        //m.appendTail(sb);
        append = preParsingMessage(message.substring(cp, message.length()));
        sb.append(append);
        return "<p>" + sb.toString() + "</p>";
    }

    @Override
    public void stop() {
        super.stop();
    }

    private static void appendException(StringBuffer buffer, Exception ex) {
        buffer.append(ex.toString());
        System.err.println(ex.toString());
        buffer.append("\n");
        for (StackTraceElement i : ex.getStackTrace()) {
            buffer.append(i.toString());
            System.err.println(i.toString());
            buffer.append("\n");
        }
    }
}
