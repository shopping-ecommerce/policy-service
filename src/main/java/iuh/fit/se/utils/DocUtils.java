// iuh/fit/se/utils/DocUtils.java
package iuh.fit.se.utils;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;

public class DocUtils {
    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();

    /** Markdown -> HTML (để hiển thị web) */
    public static String mdToHtml(String markdown, String title, String version, String startDate) {
        Node doc = MD_PARSER.parse(markdown == null ? "" : markdown);
        String body = HTML_RENDERER.render(doc);

        String header = """
      <div style="margin-bottom:16px;border-bottom:1px solid #e5e7eb;padding-bottom:8px;">
        <h1 style="margin:0;font-size:20px;">%s</h1>
        <div style="font-size:12px;color:#6b7280;">Phiên bản %s • Hiệu lực từ %s</div>
      </div>
      """.formatted(escape(title), escape(version), escape(startDate == null ? "-" : startDate));

        return """
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="UTF-8" />
          <style type="text/css">
            body { font-family: 'Noto Sans', sans-serif; line-height:1.6; font-size: 12pt; }
            h1, h2, h3 { font-weight: bold; }
            table, th, td { border: 1px solid #ddd; border-collapse: collapse; }
            th, td { padding: 6px; }
            code, pre { font-family: monospace; }
          </style>
        </head>
        <body>
          %s
          <div>%s</div>
        </body>
      </html>
      """.formatted(header, body);
    }

    /** Markdown -> XHTML (well-formed cho PDF) */
    public static String mdToXhtml(String markdown, String title, String version, String startDate) {
        Node doc = MD_PARSER.parse(markdown == null ? "" : markdown);
        String body = HTML_RENDERER.render(doc);

        String header = """
      <div style="margin-bottom:16px;border-bottom:1px solid #e5e7eb;padding-bottom:8px;">
        <h1 style="margin:0;font-size:20px;">%s</h1>
        <div style="font-size:12px;color:#6b7280;">Phiên bản %s • Hiệu lực từ %s</div>
      </div>
      """.formatted(escape(title), escape(version), escape(startDate == null ? "-" : startDate));

        return """
      <?xml version="1.0" encoding="UTF-8"?>
      <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
        "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
      <html xmlns="http://www.w3.org/1999/xhtml">
        <head>
          <meta http-equiv="Content-Type" content="application/xhtml+xml; charset=UTF-8" />
          <meta charset="UTF-8" />
          <style type="text/css">
            @page { size: A4; margin: 20mm; }
            body { font-family: 'Noto Sans', sans-serif; line-height:1.6; font-size: 12pt; }
            h1, h2, h3 { font-weight: bold; }
            table, th, td { border: 1px solid #ddd; border-collapse: collapse; }
            th, td { padding: 6px; }
            code, pre { font-family: monospace; }
          </style>
        </head>
        <body>
          %s
          <div>%s</div>
        </body>
      </html>
      """.formatted(header, body);
    }

    /** XHTML -> PDF; nhúng NotoSans từ classpath */
    public static byte[] htmlToPdfBytes(String xhtml) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // NHÚNG font từ classpath
            addFont(builder, "fonts/NotoSans-Regular.ttf", "Noto Sans", 400, BaseRendererBuilder.FontStyle.NORMAL);
            addFont(builder, "fonts/NotoSans-Bold.ttf",    "Noto Sans", 700, BaseRendererBuilder.FontStyle.NORMAL);
            // Nếu có italic:
            // addFont(builder, "fonts/NotoSans-Italic.ttf",  "Noto Sans", 400, BaseRendererBuilder.FontStyle.ITALIC);

            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    private static void addFont(PdfRendererBuilder builder, String cpPath, String family, int weight,
                                BaseRendererBuilder.FontStyle style) throws Exception {
        // subset=true để nhúng tối thiểu
        builder.useFont(() -> {
            try {
                return new ClassPathResource(cpPath).getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, family, weight, style, true);
    }

    public static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("<","&lt;").replace(">","&gt;");
    }
}