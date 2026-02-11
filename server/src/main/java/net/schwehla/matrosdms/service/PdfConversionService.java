/*
 * Copyright (c) 2026 Matrosdms
 * AGPL v3 / Commercial dual license
 */
package net.schwehla.matrosdms.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;

import jakarta.annotation.PostConstruct;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Converts text files to searchable PDFs and normalises incoming documents
 * for the Matros DMS pipeline.
 *
 * <h3>Scanner / OCR awareness</h3>
 * Rico-scanned PDFs that were processed by ABBYY FineReader already carry a
 * high-quality invisible text layer.  This service detects that layer through
 * three independent heuristics (producer metadata, font resources, extracted
 * character count) and <b>preserves it unchanged</b> instead of re-rendering.
 *
 * <h3>Thread safety</h3>
 * A {@link Semaphore} limits concurrent conversions (PDFBox is memory-heavy).
 * Font bytes are cached once; a fresh {@link PDType0Font} is loaded into every
 * new {@code PDDocument} because PDFBox 3.x ties font objects to their owner
 * document.
 */
@Service
@Lazy
public class PdfConversionService {

    private static final Logger log = LoggerFactory.getLogger(PdfConversionService.class);

    /* ================= CONFIG ================= */

    private static final String FONT_RESOURCE = "fonts/Roboto-Regular.ttf";

    private static final float FONT_SIZE     = 10f;
    private static final float LEADING       = 14f;   // slightly more than font size for readability

    private static final float MARGIN_LEFT   = 50f;
    private static final float MARGIN_RIGHT  = 50f;
    private static final float MARGIN_TOP    = 50f;
    private static final float MARGIN_BOTTOM = 50f;

    /** Minimum extracted characters to consider a PDF "text-based". */
    private static final int MIN_TEXT_CHARS = 20;

    /** Minimum chars-per-page ratio for a high-quality layer (FineReader typically > 100). */
    private static final double MIN_CHARS_PER_PAGE = 30.0;

    /** Known OCR / scanner producer strings (lower-cased for comparison). */
    private static final Set<String> OCR_PRODUCERS = Set.of(
            "abbyy", "finereader", "fine reader",
            "omnipage", "readiris", "tesseract",
            "ocrmypdf", "adobe acrobat"
    );

    /* ================= STATE ================= */

    /** Raw TTF bytes — cached once, loaded per-document (PDFBox 3.x requirement). */
    private volatile byte[] fontBytes;

    /** Hard concurrency limit — PDFBox is memory-heavy. */
    private final Semaphore conversionSemaphore = new Semaphore(2);

    /* ================= PUBLIC RECORD ================= */

    /**
     * Result of {@link #normalize}: the path to the (possibly converted) file
     * together with its resolved MIME type and extension.
     */
    public record ConversionResult(Path path, String mimeType, String extension) {}

    /* ================= INIT ================= */

    @PostConstruct
    public void loadFontOnce() {
        try (InputStream is = new ClassPathResource(FONT_RESOURCE).getInputStream()) {
            fontBytes = is.readAllBytes();
            log.info("Unicode PDF font cached ({} bytes): {}", fontBytes.length, FONT_RESOURCE);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unicode font is mandatory for text-layer PDFs: " + FONT_RESOURCE, e);
        }
    }

    /* ================= PUBLIC API ================= */

    /**
     * Normalises an incoming file for the DMS pipeline.
     *
     * <ul>
     *   <li>PDF with text layer (FineReader / scanner OCR) → returned as-is</li>
     *   <li>PDF without text layer (image-only scan) → returned as-is (Tika/OCR handles it downstream)</li>
     *   <li>Plain text → converted to a searchable A4 PDF</li>
     *   <li>Everything else → returned as-is with detected MIME</li>
     * </ul>
     *
     * @param source     the original file
     * @param workingDir pipeline scratch directory
     * @param mimeType   MIME type as detected by Tika
     * @return conversion result with resolved path, MIME type and extension
     */
    public ConversionResult normalize(Path source, Path workingDir, String mimeType)
            throws IOException {

        if ("application/pdf".equals(mimeType)) {
            return handlePdf(source, workingDir);
        }

        if (isPlainText(mimeType)) {
            Path dest = workingDir.resolve(
                    stripExtension(source.getFileName().toString()) + ".pdf");
            convertTextToPdf(source, dest);
            return new ConversionResult(dest, "application/pdf", ".pdf");
        }

        // Image, Office, etc. — leave for downstream Tika/OCR
        String ext = extensionOf(source.getFileName().toString());
        return new ConversionResult(source, mimeType, ext);
    }

    /**
     * Converts a UTF-8 text file into a paginated, searchable A4 PDF.
     */
    public Path convertTextToPdf(Path source, Path destination) throws IOException {
        try {
            conversionSemaphore.acquire();
            Files.deleteIfExists(destination);

            try (PDDocument doc = new PDDocument();
                 BufferedReader reader =
                         Files.newBufferedReader(source, StandardCharsets.UTF_8)) {

                PDFont font = loadFont(doc);
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                writeTextLayer(doc, page, font, reader);
                doc.save(destination.toFile());
            }

            return destination;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF conversion interrupted", e);
        } finally {
            conversionSemaphore.release();
        }
    }

    /* ================= PDF HANDLING ================= */

    private ConversionResult handlePdf(Path source, Path workingDir) throws IOException {
        try (PDDocument doc = Loader.loadPDF(source.toFile())) {

            TextLayerInfo info = analyseTextLayer(doc);

            if (info.hasText) {
                log.info("PDF already has text layer → preserving (producer={}, chars={}, pages={}, "
                                + "charsPerPage={}, ocrProducer={})",
                        info.producer, info.charCount, info.pageCount,
                        String.format("%.1f", info.charsPerPage), info.isOcrProducer);
                return new ConversionResult(source, "application/pdf", ".pdf");
            }

            log.debug("PDF has no usable text layer (chars={}, pages={})",
                    info.charCount, info.pageCount);
        }

        // Image-only PDF — return as-is, let Tika/OCR handle downstream
        return new ConversionResult(source, "application/pdf", ".pdf");
    }

    /* ================= TEXT LAYER DETECTION ================= */

    /**
     * Comprehensive text-layer analysis tuned for Rico scanners with FineReader.
     *
     * Three independent heuristics run in a single pass:
     * <ol>
     *   <li><b>Producer / Creator metadata</b> — FineReader, OmniPage, Tesseract, etc.</li>
     *   <li><b>Font resources</b> — OCR engines embed specific fonts (e.g. FineReader embeds
     *       fonts whose names often start with "ABBYY" or contain "TimesNewRoman").</li>
     *   <li><b>Extracted text density</b> — character count per page; a real layer
     *       typically yields &gt; 30 chars/page even for sparse documents.</li>
     * </ol>
     */
    private TextLayerInfo analyseTextLayer(PDDocument doc) throws IOException {
        TextLayerInfo info = new TextLayerInfo();
        info.pageCount = doc.getNumberOfPages();

        // ── 1. Metadata ──
        PDDocumentInformation meta = doc.getDocumentInformation();
        if (meta != null) {
            info.producer = safe(meta.getProducer()) + " / " + safe(meta.getCreator());
            String lc = info.producer.toLowerCase(Locale.ROOT);
            info.isOcrProducer = OCR_PRODUCERS.stream().anyMatch(lc::contains);
        }

        // ── 2. Font heuristic (first 3 pages) ──
        int pagesToCheck = Math.min(info.pageCount, 3);
        for (int i = 0; i < pagesToCheck; i++) {
            PDPage page = doc.getPage(i);
            PDResources res = page.getResources();
            if (res != null) {
                for (COSName fontName : res.getFontNames()) {
                    PDFont font = res.getFont(fontName);
                    if (font != null) {
                        String name = font.getName();
                        if (name != null) {
                            String lcName = name.toLowerCase(Locale.ROOT);
                            if (lcName.contains("abbyy") || lcName.contains("finereader")) {
                                info.hasFineReaderFonts = true;
                            }
                            info.hasAnyFonts = true;
                        }
                    }
                }
            }
        }

        // ── 3. Text extraction ──
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String rawText = stripper.getText(doc);
        info.charCount = rawText == null ? 0 : rawText.trim().length();
        info.charsPerPage = info.pageCount > 0
                ? (double) info.charCount / info.pageCount
                : 0.0;

        // ── Decision ──
        // A FineReader producer with *any* text is highly trustworthy
        if (info.isOcrProducer && info.charCount > 0) {
            info.hasText = true;
        }
        // FineReader fonts detected + some text → trust it
        else if (info.hasFineReaderFonts && info.charCount > 0) {
            info.hasText = true;
        }
        // Generic: enough absolute chars AND reasonable density
        else if (info.charCount >= MIN_TEXT_CHARS && info.charsPerPage >= MIN_CHARS_PER_PAGE) {
            info.hasText = true;
        }

        return info;
    }

    /** Mutable carrier for text-layer analysis results. */
    private static class TextLayerInfo {
        boolean hasText;
        boolean isOcrProducer;
        boolean hasFineReaderFonts;
        boolean hasAnyFonts;
        String  producer = "";
        int     charCount;
        int     pageCount;
        double  charsPerPage;
    }

    /* ================= CORE RENDERING ================= */

    /**
     * Renders text line-by-line into the PDF with automatic page breaks and
     * word wrapping.  Handles RTL/Bidi text (FineReader mixed-script output).
     */
    private void writeTextLayer(PDDocument doc, PDPage page, PDFont font,
                                BufferedReader reader) throws IOException {

        PDRectangle mediaBox = page.getMediaBox();
        float usableWidth = mediaBox.getWidth() - MARGIN_LEFT - MARGIN_RIGHT;
        float startY = mediaBox.getHeight() - MARGIN_TOP;
        float y = startY;

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.setFont(font, FONT_SIZE);
        cs.setLeading(LEADING);
        cs.beginText();
        cs.newLineAtOffset(MARGIN_LEFT, y);

        String line;
        while ((line = reader.readLine()) != null) {

            String sanitized = sanitize(line);

            if (sanitized.isEmpty()) {
                cs.newLine();
                y -= LEADING;

                if (y <= MARGIN_BOTTOM) {
                    cs.endText();
                    cs.close();

                    page = newPage(doc, mediaBox);
                    cs = new PDPageContentStream(doc, page);
                    cs.setFont(font, FONT_SIZE);
                    cs.setLeading(LEADING);
                    cs.beginText();

                    y = startY;
                    cs.newLineAtOffset(MARGIN_LEFT, y);
                }
                continue;
            }

            // Handle RTL / mixed-direction text (FineReader sometimes emits Arabic/Hebrew)
            // java.text.Bidi only analyses direction — we manually reverse pure-RTL runs
            // so PDFBox paints them in the correct visual order.
            sanitized = reorderBidi(sanitized);

            for (String wrapped : wrapLine(sanitized, usableWidth, font)) {

                if (y <= MARGIN_BOTTOM) {
                    cs.endText();
                    cs.close();

                    page = newPage(doc, mediaBox);
                    cs = new PDPageContentStream(doc, page);
                    cs.setFont(font, FONT_SIZE);
                    cs.setLeading(LEADING);
                    cs.beginText();

                    y = startY;
                    cs.newLineAtOffset(MARGIN_LEFT, y);
                }

                cs.showText(encodeForFont(wrapped, font));
                cs.newLine();
                y -= LEADING;
            }
        }

        cs.endText();
        cs.close();
    }

    private PDPage newPage(PDDocument doc, PDRectangle size) {
        PDPage page = new PDPage(size);
        doc.addPage(page);
        return page;
    }

    /* ================= BIDI REORDERING ================= */

    /**
     * Reorders mixed LTR/RTL text into visual order for PDFBox rendering.
     * <p>
     * Uses {@link java.text.Bidi} to analyse directional runs, then reverses
     * the characters within each RTL run so that {@code showText()} paints
     * them in the correct visual order.  Pure-LTR strings are returned as-is
     * (fast path).
     */
    private static String reorderBidi(String text) {
        if (!Bidi.requiresBidi(text.toCharArray(), 0, text.length())) {
            return text;                        // fast path — no RTL chars at all
        }

        Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

        if (bidi.isLeftToRight()) {
            return text;                        // analysis confirms pure LTR
        }

        // Walk each directional run and reverse RTL ones
        int runCount = bidi.getRunCount();
        StringBuilder result = new StringBuilder(text.length());

        for (int i = 0; i < runCount; i++) {
            int start = bidi.getRunStart(i);
            int end   = bidi.getRunLimit(i);
            int level = bidi.getRunLevel(i);

            String run = text.substring(start, end);
            if ((level & 1) != 0) {
                // Odd level → RTL: reverse for visual order
                result.append(new StringBuilder(run).reverse());
            } else {
                result.append(run);
            }
        }

        return result.toString();
    }

    /* ================= FONT LOADING ================= */

    /**
     * Loads the cached TTF into the given document.
     * PDFBox 3.x ties font objects to their owner document, so we must create
     * a fresh instance for every conversion.
     */
    private PDFont loadFont(PDDocument doc) throws IOException {
        try (InputStream is = new ByteArrayInputStream(fontBytes)) {
            return PDType0Font.load(doc, is, true);
        }
    }

    /* ================= SANITIZATION ================= */

    /**
     * Fast, allocation-light, PDFBox-safe sanitisation.
     *
     * <ul>
     *   <li>TAB → 4 spaces</li>
     *   <li>NBSP → regular space</li>
     *   <li>Strips soft hyphens, zero-width spaces, LTR/RTL marks,
     *       BOM, line/paragraph separators, and all C0/C1 control chars</li>
     *   <li>Preserves valid Unicode including supplementary-plane codepoints
     *       (surrogate pairs are kept together)</li>
     * </ul>
     *
     * Handles OCR junk from FineReader, Word exports, and ScanSnap output.
     */
    private String sanitize(String input) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(input.length());
        int len = input.length();

        for (int i = 0; i < len; ) {
            int cp = input.codePointAt(i);
            i += Character.charCount(cp);

            switch (cp) {
                case '\t':
                    sb.append("    ");
                    break;

                case '\u00A0':          // NBSP
                    sb.append(' ');
                    break;

                case '\u00AD':          // soft hyphen
                case '\u200B':          // zero-width space
                case '\u200C':          // zero-width non-joiner
                case '\u200D':          // zero-width joiner
                case '\u200E':          // LTR mark
                case '\u200F':          // RTL mark
                case '\u2028':          // line separator
                case '\u2029':          // paragraph separator
                case '\uFEFF':          // BOM / zero-width no-break space
                case '\uFFFD':          // replacement character (OCR garbage)
                    break;

                default:
                    // Keep printable characters, drop C0/C1 control range
                    if (cp >= 0x20 && cp != 0x7F
                            && !(cp >= 0x80 && cp <= 0x9F)) {
                        sb.appendCodePoint(cp);
                    }
            }
        }

        return sb.toString();
    }

    /* ================= FONT-SAFE ENCODING ================= */

    /**
     * Strips codepoints that the font cannot encode, replacing them with a
     * space (to preserve word boundaries).  This prevents PDFBox from throwing
     * {@code IllegalStateException: could not find the glyphId} at render time.
     *
     * <p>Common culprits: arrows (←→↑↓), box-drawing, math symbols, emoji —
     * anything outside Roboto's glyph table.
     */
    private String encodeForFont(String text, PDFont font) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder sb = new StringBuilder(text.length());
        int len = text.length();

        for (int i = 0; i < len; ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            String ch = text.substring(i, i + charCount);

            try {
                font.encode(ch);   // throws if glyph is missing
                sb.append(ch);
            } catch (IllegalArgumentException | IOException e) {
                sb.append(' ');    // replace with space to keep word boundaries
            }

            i += charCount;
        }

        return sb.toString();
    }

    /* ================= LINE WRAPPING ================= */

    /**
     * Word-wraps a single logical line to fit within {@code maxWidth} points.
     * Falls back to character-level breaking for words that exceed the full
     * line width (e.g. long URLs or FineReader run-on tokens).
     */
    private List<String> wrapLine(String text, float maxWidth, PDFont font)
            throws IOException {

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        float currentWidth = 0f;

        for (String word : text.split(" ")) {

            // ── Handle single words wider than the whole line ──
            float wordWidth = stringWidth(font, word);
            if (wordWidth > maxWidth) {
                // Flush anything accumulated so far
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                    currentWidth = 0f;
                }
                // Break the long word character-by-character
                lines.addAll(breakLongWord(word, maxWidth, font));
                continue;
            }

            float spaceWidth = current.length() == 0 ? 0f : stringWidth(font, " ");
            float candidateWidth = currentWidth + spaceWidth + wordWidth;

            if (candidateWidth > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
                currentWidth = 0f;
            }

            if (current.length() > 0) {
                current.append(' ');
                currentWidth += spaceWidth;
            }
            current.append(word);
            currentWidth += wordWidth;
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    /** Breaks a single oversized word into lines that fit {@code maxWidth}. */
    private List<String> breakLongWord(String word, float maxWidth, PDFont font)
            throws IOException {

        List<String> parts = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        float bufWidth = 0f;

        int len = word.length();
        for (int i = 0; i < len; ) {
            int cp = word.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            float chWidth = stringWidth(font, ch);

            if (bufWidth + chWidth > maxWidth && buf.length() > 0) {
                parts.add(buf.toString());
                buf.setLength(0);
                bufWidth = 0f;
            }

            buf.appendCodePoint(cp);
            bufWidth += chWidth;
            i += Character.charCount(cp);
        }

        if (buf.length() > 0) {
            parts.add(buf.toString());
        }
        return parts;
    }

    /** Measures string width in PDF points — wraps the PDFBox API. */
    private float stringWidth(PDFont font, String s) throws IOException {
        try {
            return font.getStringWidth(s) / 1000f * FONT_SIZE;
        } catch (IllegalArgumentException | IOException e) {
            // Glyph not in font — return estimate to avoid crash
            log.trace("Font cannot encode '{}', estimating width", s);
            return s.length() * FONT_SIZE * 0.5f;
        }
    }

    /* ================= HELPERS ================= */

    private static boolean isPlainText(String mime) {
        return mime != null && (
                mime.startsWith("text/")
             || "application/json".equals(mime)
             || "application/xml".equals(mime)
             || "application/xhtml+xml".equals(mime));
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
