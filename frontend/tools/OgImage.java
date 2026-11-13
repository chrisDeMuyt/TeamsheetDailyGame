import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

/**
 * Link-preview card for shared results (og:image), 1200x630.
 * Regenerate:  java frontend/tools/OgImage.java frontend/public/og-image.png   (JDK 21+, uses the Inter font)
 */
public class OgImage {
    static final Color BG = new Color(0x0E1A14), PANEL = new Color(0x15261D), PITCH = new Color(0x1F5136),
            ACCENT = new Color(0xF5C542), TEXT = new Color(0xF2F4F1), MUTED = new Color(0xA9B8AE),
            LINE = new Color(242, 244, 241, 90), DASH = new Color(242, 244, 241, 160);

    public static void main(String[] args) throws Exception {
        int w = 1200, h = 630;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(BG);
        g.fillRect(0, 0, w, h);
        // soft glow behind the pitch
        g.setPaint(new RadialGradientPaint(new Point(905, 315), 420, new float[]{0f, 1f},
                new Color[]{new Color(31, 81, 54, 120), new Color(14, 26, 20, 0)}));
        g.fillRect(0, 0, w, h);

        drawPitch(g, 850, 50, 290, 530);

        // ---- left: wordmark, tagline, rules ----
        int x = 80;
        g.setColor(TEXT);
        Font word = font(BOLD, Font.PLAIN, 116, -0.035f);
        g.setFont(word);
        g.drawString("TEAMSHEET", x, 210);

        g.setColor(MUTED);
        g.setFont(font("Inter", Font.PLAIN, 36, 0f));
        g.drawString("The daily football lineup quiz", x + 4, 268);

        String[][] rules = {
                {"3", "players from one famous match lineup"},
                {"30s", "per guess, so think fast"},
                {"100", "points for the rarest picks"},
        };
        int y = 340;
        for (String[] r : rules) {
            g.setColor(PANEL);
            g.fill(new RoundRectangle2D.Double(x, y, 640, 68, 22, 22));
            g.setColor(ACCENT);
            g.setFont(font(BOLD, Font.PLAIN, 30, 0f));
            g.drawString(r[0], x + 26, y + 45);
            g.setColor(TEXT);
            g.setFont(font("Inter", Font.PLAIN, 28, 0f));
            g.drawString(r[1], x + 116, y + 45);
            y += 84;
        }

        g.dispose();
        File out = new File(args.length > 0 ? args[0] : "og-image.png");
        ImageIO.write(img, "png", out);
        System.out.println("wrote " + out.getAbsolutePath());
    }

    /** Vertical pitch, attack at the top, a generic 4-3-3 with three lit (picked) players. */
    static void drawPitch(Graphics2D g, int px, int py, int pw, int ph) {
        g.setColor(PITCH);
        g.fill(new RoundRectangle2D.Double(px, py, pw, ph, 36, 36));
        int m = 18;
        double ix = px + m, iy = py + m, iw = pw - 2 * m, ih = ph - 2 * m;
        g.setColor(LINE);
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new Rectangle.Double(ix, iy, iw, ih));
        g.drawLine((int) ix, (int) (iy + ih / 2), (int) (ix + iw), (int) (iy + ih / 2));
        double cr = iw * 0.17;
        g.draw(new Ellipse2D.Double(ix + iw / 2 - cr, iy + ih / 2 - cr, cr * 2, cr * 2));
        g.draw(new Rectangle.Double(ix + iw * 0.25, iy, iw * 0.5, ih * 0.13));
        g.draw(new Rectangle.Double(ix + iw * 0.25, iy + ih * 0.87, iw * 0.5, ih * 0.13));

        // x 0..100 left->right, y 0..100 own goal->attack
        double[][] empty = {{50, 6}, {84, 26}, {62, 22}, {16, 26}, {72, 50}, {28, 50}, {84, 80}, {16, 80}};
        double[][] lit = {{38, 22}, {50, 44}, {50, 86}};
        double r = 17;
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 6f}, 0f));
        for (double[] p : empty) {
            double cx = ix + iw * p[0] / 100, cy = iy + ih * (1 - p[1] / 100);
            g.setColor(new Color(14, 26, 20, 50));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            g.setColor(DASH);
            g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        }
        for (double[] p : lit) {
            double cx = ix + iw * p[0] / 100, cy = iy + ih * (1 - p[1] / 100);
            g.setColor(new Color(245, 197, 66, 70));
            g.fill(new Ellipse2D.Double(cx - r - 7, cy - r - 7, 2 * r + 14, 2 * r + 14));
            g.setColor(ACCENT);
            g.fill(new Ellipse2D.Double(cx - r - 1, cy - r - 1, 2 * r + 2, 2 * r + 2));
        }
    }

    /** Java can't select weights inside Inter's variable font, but Windows installs the semibold as its own family. */
    static final String BOLD = "Inter Semi Bold";

    static Font font(String family, int style, float size, float tracking) {
        Font f = new Font(family, style, 1).deriveFont(size);
        return tracking == 0f ? f : f.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }
}
