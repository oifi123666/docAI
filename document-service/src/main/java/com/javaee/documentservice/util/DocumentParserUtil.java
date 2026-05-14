package com.javaee.documentservice.util;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文档解析工具类
 * 支持多种文档格式：Word、PDF、HTML、Markdown、图片OCR等
 */
public class DocumentParserUtil {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserUtil.class);

    // 支持的图片格式
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "bmp", "gif", "tiff", "tif")
    );

    // 支持的所有文档格式
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(
            Arrays.asList("docx", "doc", "txt", "pdf", "md", "markdown", "html", "htm", "xml",
                    "jpg", "jpeg", "png", "bmp", "gif", "tiff", "tif")
    );

    /**
     * 解析文档内容
     * @param fileContent 文件内容字节数组
     * @param fileName 文件名（用于判断格式）
     * @return 解析后的文本内容
     */
    public static String parseDocument(byte[] fileContent, String fileName) {
        if (fileContent == null || fileContent.length == 0) {
            return "";
        }

        String extension = getFileExtension(fileName);
        log.info("开始解析文档: fileName={}, extension={}, size={}", fileName, extension, fileContent.length);

        try {
            return switch (extension.toLowerCase()) {
                case "docx" -> parseDocx(fileContent);
                case "doc" -> parseDoc(fileContent);
                case "txt" -> parseTxt(fileContent);
                case "pdf" -> parsePdf(fileContent);
                case "md", "markdown" -> parseMarkdown(fileContent);
                case "html", "htm" -> parseHtml(fileContent);
                case "xml" -> parseXml(fileContent);
                case "jpg", "jpeg", "png", "bmp", "gif", "tiff", "tif" -> parseImageOcr(fileContent);
                default -> {
                    log.warn("不支持的文件格式: {}, 尝试作为纯文本处理", extension);
                    yield parseTxt(fileContent);
                }
            };
        } catch (Exception e) {
            log.error("解析文档失败: {}", fileName, e);
            // 如果解析失败，尝试作为纯文本处理
            try {
                return new String(fileContent, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 判断文件是否为支持的图片格式
     */
    public static boolean isImageFile(String fileName) {
        String extension = getFileExtension(fileName);
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 判断文件是否为支持的文档格式
     */
    public static boolean isSupportedDocument(String fileName) {
        String extension = getFileExtension(fileName);
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 解析DOCX文件
     */
    private static String parseDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder text = new StringBuilder();
            
            // 解析段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            
            // 解析表格
            for (var table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        text.append(cell.getText()).append("\t");
                    }
                    text.append("\n");
                }
            }
            
            return text.toString().trim();
        }
    }

    /**
     * 解析DOC文件（旧版Word）
     */
    private static String parseDoc(byte[] content) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(content))) {
            WordExtractor extractor = new WordExtractor(document);
            return extractor.getText().trim();
        }
    }

    /**
     * 解析TXT文件
     */
    private static String parseTxt(byte[] content) {
        return new String(content, StandardCharsets.UTF_8).trim();
    }

    /**
     * 解析PDF文件
     */
    private static String parsePdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }

    /**
     * 解析HTML文件
     */
    private static String parseHtml(byte[] content) throws IOException {
        String html = new String(content, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(html);
        
        StringBuilder text = new StringBuilder();
        
        // 提取标题
        String title = doc.title();
        if (title != null && !title.isEmpty()) {
            text.append(title).append("\n\n");
        }
        
        // 提取主要内容（p标签、h标签等）
        Elements elements = doc.select("p, h1, h2, h3, h4, h5, h6, li, td, th");
        for (Element element : elements) {
            String elementText = element.text();
            if (elementText != null && !elementText.trim().isEmpty()) {
                text.append(elementText).append("\n");
            }
        }
        
        return text.toString().trim();
    }

    /**
     * 解析Markdown文件
     */
    private static String parseMarkdown(byte[] content) {
        // Markdown本身就是文本格式，直接读取即可
        // 如果需要更复杂的解析，可以使用 flexmark 转换为HTML后再提取文本
        String markdown = new String(content, StandardCharsets.UTF_8);
        return markdown.trim();
    }

    /**
     * 解析XML文件
     */
    private static String parseXml(byte[] content) throws IOException {
        String xml = new String(content, StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(xml);
        
        // 提取所有文本内容
        return doc.text().trim();
    }

    /**
     * 解析图片文件，使用OCR识别文字
     * 注意：需要安装 Tesseract OCR 引擎
     */
    private static String parseImageOcr(byte[] content) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                log.warn("无法解析图片文件");
                return "";
            }
            
            ITesseract tesseract = new Tesseract();
            // 设置Tesseract数据路径（可选，如果Tesseract已安装到系统路径可以不设置）
            // tesseract.setDatapath("path/to/tessdata");
            
            // 设置识别语言（英文+简体中文）
            tesseract.setLanguage("eng+chi_sim");
            
            try {
                String result = tesseract.doOCR(image);
                return result.trim();
            } catch (TesseractException e) {
                log.error("OCR识别失败", e);
                return "";
            }
        }
    }

    /**
     * 清理解析后的文本（去除多余空白）
     */
    public static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        // 去除多余的换行和空格
        return text.replaceAll("\\n+", "\n")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    /**
     * 获取文档摘要
     */
    public static String getSummary(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        text = cleanText(text);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}