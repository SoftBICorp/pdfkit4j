package com.bi.util;

import com.bi.model.PdfText;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.*;
import java.text.AttributedString;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

@Slf4j
public final class PdfUtil {

    private static final String FONT_KAIU = "./fonts/kaiu.ttf";

    private static final File LOGO_FILE = new File("./images/watermarkLogo.png");

    private static final Font ENGLISH_FONT = new Font("Arial", Font.BOLD, 14);

    private static final Font CHINESE_FONT = new Font("微軟正黑體", Font.BOLD, 14);

    public static void concatPdF(List<InputStream> streamOfPDFFiles, OutputStream outputStream, boolean paginate) {
        Document document = null;
        try {
            List<InputStream> pdfs = streamOfPDFFiles;
            List<PdfReader> readers = new ArrayList<PdfReader>();
            Iterator<InputStream> iteratorPDFs = pdfs.iterator();

            Rectangle rectangle = null;
            // Create Readers for the pdfs.
            while (iteratorPDFs.hasNext()) {
                InputStream pdf = iteratorPDFs.next();
                PdfReader pdfReader = new PdfReader(pdf);
                if (rectangle == null) {
                    rectangle = pdfReader.getPageSize(1);
                }
                readers.add(pdfReader);
            }
            document = new Document(rectangle);
            // Create a writer for the outputstream
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            document.open();
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            PdfContentByte cb = writer.getDirectContent(); // Holds the PDF
            PdfImportedPage page;
            int pageOfCurrentReaderPDF = 0;
            Iterator<PdfReader> iteratorPDFReader = readers.iterator();

            // Loop through the PDF files and add to the output.
            while (iteratorPDFReader.hasNext()) {
                PdfReader pdfReader = iteratorPDFReader.next();

                // Create a new page in the target for each source page.
                while (pageOfCurrentReaderPDF < pdfReader.getNumberOfPages()) {
                    document.newPage();
                    ++pageOfCurrentReaderPDF;
                    // currentPageNumber++;
                    page = writer.getImportedPage(pdfReader, pageOfCurrentReaderPDF);
                    cb.addTemplate(page, 0, 0);

                    // Code for pagination.
                    if (paginate) {
                        cb.beginText();
                        cb.setFontAndSize(bf, 9);
                        // 不編頁碼，若要編頁碼這行要打開，這樣可能會對原來的頁碼產生混亂要注意
                        // cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "" +
                        // currentPageNumber + " of " + totalPages, 520, 5, 0);
                        cb.endText();
                    }
                }
                pageOfCurrentReaderPDF = 0;
            }
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error merging PDF", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    public static void addText(String inputFilePath, String outputFilePath, List<PdfText> texts) throws Exception {

        FileInputStream inputStream = new FileInputStream(inputFilePath);
        FileOutputStream outputStream = new FileOutputStream(outputFilePath);

        // Document document = new Document(PageSize.A4);

        // Read the existing PDF document
        PdfReader pdfReader = new PdfReader(inputStream);

        // Get the PdfStamper object
        PdfStamper pdfStamper = new PdfStamper(pdfReader, outputStream);

        String fontName = FONT_KAIU;
        // Get the PdfContentByte type by pdfStamper.
        for (PdfText pt : texts) {
            PdfContentByte pageContent = pdfStamper.getOverContent(pt.getPageNum());
            pageContent.beginText();
            fontName = pt.getFontName() == null ? fontName : pt.getFontName();
            pageContent.setFontAndSize(BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED), pt.getSize());
            // pageContent.setColorFill(BaseColor.BLACK);
            pageContent.showTextAligned(Element.ALIGN_LEFT, StringUtils.isEmpty(pt.getText()) ? "" : pt.getText(), pt.getAxisX(), pt.getAxisY(), 0);
            pageContent.endText();
        }
        pdfStamper.close();
    }

    public static void concatenate(String mainPdfFilePath, List<Map<String, String>> mergeFiles, String outputFilePath) throws DocumentException, IOException {
        Document document = new Document();
        PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputFilePath));
        document.open();
        PdfReader reader;

        int n;
        // loop over the documents you want to concatenate
        for (int i = 0; i < mergeFiles.size(); i++) {
            Map<String, String> fileData = mergeFiles.get(i);
            // loop over the pages in that document
            if ("PDF".equals(fileData.get("FILETYPE"))) {
                reader = new PdfReader(fileData.get("FILEPATH"));
                n = reader.getNumberOfPages();
                for (int page = 0; page < n; ) {
                    copy.addPage(copy.getImportedPage(reader, ++page));
                }
                copy.freeReader(reader);
                reader.close();
            } else {
                /* File is image. Create a new PDF in memory, write the image to its first page,
                 * and then use PdfCopy to copy that first page back into the main PDF */
                Document imageDocument = new Document(new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth())); //橫向A4
                ByteArrayOutputStream imageDocumentOutputStream = new ByteArrayOutputStream();
                PdfWriter imageDocumentWriter = PdfWriter.getInstance(imageDocument, imageDocumentOutputStream);

                imageDocument.open();

                if (imageDocument.newPage()) {
                    Image image = Image.getInstance(fileData.get("FILEPATH"));
                    image.scaleToFit(PageSize.A4.getHeight(), PageSize.A4.getWidth());
                    image.setAbsolutePosition(0, 0);
                    imageDocument.add(image);
                    imageDocument.close();
                    imageDocumentWriter.close();

                    PdfReader imageDocumentReader = new PdfReader(imageDocumentOutputStream.toByteArray());
                    copy.addPage(copy.getImportedPage(imageDocumentReader, 1));
                    copy.freeReader(imageDocumentReader);
                    imageDocumentReader.close();
                }
            }
        }
        document.close();
        if (StringUtils.isNotEmpty(mainPdfFilePath)) {
            String mainParent = mainPdfFilePath.substring(0, mainPdfFilePath.lastIndexOf("/"));
            String mainName = mainPdfFilePath.substring(mainPdfFilePath.lastIndexOf("/") + 1);
            String outputParent = outputFilePath.substring(0, mainPdfFilePath.lastIndexOf("/"));
            String outputName = outputFilePath.substring(mainPdfFilePath.lastIndexOf("/") + 1);
            FileUtils.copyFile(new File(outputParent + File.separator + outputName), new File(mainParent + File.separator + mainName));
            FileUtils.forceDelete(new File(outputParent + File.separator + outputName));
        }
    }

    /**
     * 在A4大小的pdf檔案中套印浮水印，產出套印浮水印後的pdf檔案。（支援直式與橫式A4）
     * <p>
     * 2017年6月13日
     *
     * @param src      來源pdf檔案路徑
     * @param dest     目的pdf檔案路徑
     * @param userCode 列印pdf的使用者代碼
     * @param userName 列印pdf的使用者名稱
     * @param ip       列印pdf的使用者電腦IP
     * @throws IOException
     * @throws DocumentException
     */
    public static void addA4PdfWatermark(String src, String dest, String userCode, String userName, String ip) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(src);
        FileOutputStream os = new FileOutputStream(dest);
        addA4PdfWatermark(reader, os, userCode, userName, ip);
        if (os != null) {
            os.close();
        }
        if (reader != null) {
            reader.close();
        }
    }

    /**
     * 在A4大小的pdf InputStream中套印浮水印並回傳套印後的InputStream。（支援直式與橫式A4）
     * <p>
     * 2017年6月14日
     *
     * @param src      來源pdf InputStream
     * @param userCode 列印pdf的使用者代碼
     * @param userName 列印pdf的使用者名稱
     * @param ip       列印pdf的使用者電腦IP
     * @return 套印浮水印後的InputStream
     * @throws IOException
     * @throws DocumentException
     */
    public static InputStream addA4PdfWatermark(InputStream src, String userCode, String userName, String ip) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(src);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        addA4PdfWatermark(reader, os, userCode, userName, ip);
        byte[] data = os.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        if (src != null) {
            src.close();
        }
        if (os != null) {
            os.close();
        }
        if (reader != null) {
            reader.close();
        }
        return bis;
    }

    private static void addA4PdfWatermark(PdfReader reader, OutputStream os, String userCode, String userName, String ip) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, os);
        Image img = Image.getInstance(genA4WatermarkBufferedImage(userCode, userName, ip), Color.WHITE);
        img.setAbsolutePosition(0, 0);
        PdfContentByte watermark;
        for (int pageIndex = 1; pageIndex <= reader.getNumberOfPages(); pageIndex++) {
            watermark = stamper.getUnderContent(pageIndex);
            watermark.addImage(img);
        }
        stamper.setFormFlattening(true);
        stamper.close();
    }

    /**
     * 利用單一格浮水印組合成A4大小的浮水印（支援直式與橫式A4）
     * <p>
     * 2017年6月13日
     *
     * @param userCode
     * @param userName
     * @param ip
     * @return
     * @throws IOException
     */
    private static BufferedImage genA4WatermarkBufferedImage(String userCode, String userName, String ip) throws IOException {
        // 為了支援直式與橫式A4，長寬都設成A4的長邊
        int a4Width = 842, a4Height = 842;
        BufferedImage img = genWatermarkBufferedImage(userCode, userName, ip);
        BufferedImage bi = new BufferedImage(a4Width, a4Height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = bi.getGraphics();
        g.setColor(Color.WHITE);
        g.clearRect(0, 0, a4Width, a4Height);
        for (int h = 0; h < bi.getHeight(); h += img.getHeight()) {
            for (int w = 0; w < bi.getWidth(); w += img.getWidth()) {
                g.drawImage(img, w, h, null);
            }
        }
        g.dispose();
        return bi;
    }

    /**
     * 產出單一格浮水印
     * <p>
     * 2017年6月13日
     *
     * @param userCode
     * @param userName
     * @param ip
     * @return
     * @throws IOException
     */
    private static BufferedImage genWatermarkBufferedImage(String userCode, String userName, String ip) throws IOException {
        int width = 280, height = 280;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = bi.getGraphics();

        double p = Math.PI / 6.0;
        Graphics2D g2d = (Graphics2D) g;

        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f));

        AffineTransform at = new AffineTransform();
        at.setToRotation(-p);
        g2d.setTransform(at);

        DateFormat DF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        AttributedString as1 = new AttributedString(userCode + " " + userName);
        AttributedString as2 = new AttributedString(DF.format(new Date()));
        AttributedString as3 = new AttributedString(ip);

        as1.addAttribute(TextAttribute.FONT, CHINESE_FONT);
        as2.addAttribute(TextAttribute.FONT, ENGLISH_FONT);
        as3.addAttribute(TextAttribute.FONT, ENGLISH_FONT);
        g2d.setColor(Color.GRAY);
        g2d.drawString(as1.getIterator(), -16, 142);
        g2d.drawString(as2.getIterator(), -16, 158);
        g2d.drawString(as3.getIterator(), -16, 174);

        BufferedImage logo = ImageIO.read(LOGO_FILE);
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        logo = op.filter(logo, null);
        g2d.drawImage(logo, -22, 70, null);
        g.dispose();
        return bi;
    }

    /**
     * PDF 解密結果(InputStream)
     *
     * @param filePath
     * @param fileName
     * @param ownerPassword
     */
    public static InputStream pdfDecryptionInputStream(String filePath, String fileName, String ownerPassword) {
        PdfReader pdfReader = null;
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        try {
            pdfReader = new PdfReader(filePath + File.separator + fileName, ownerPassword.getBytes());
            PdfStamper pdfStamper = new PdfStamper(pdfReader, pdf);
            pdfStamper.close();
            return new ByteArrayInputStream(pdf.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                pdf.close();
            } catch (IOException e) {
                log.error("Error closing PDF: {}", e.getMessage());
            }

            if (pdfReader != null) {
                pdfReader.close();
            }
        }
    }

    /**
     * PDF 加密
     *
     * @param filePath
     * @param fileName
     * @param ownerPassword
     * @param userPassword
     */
    private static void preparePdfEncryption(String filePath, String fileName, String ownerPassword, String userPassword) {
        String encryptKey = "Encrypt_";
        String from = filePath + File.separator + fileName;
        String to = filePath + File.separator + encryptKey + fileName;

        try {
            PdfReader reader = new PdfReader(from);
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(to));
            stamper.setEncryption(userPassword.getBytes(), ownerPassword.getBytes(),
                    PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128 | PdfWriter.DO_NOT_ENCRYPT_METADATA);
            stamper.close();
            reader.close();

            FileUtils.forceDelete(new File(from));
            FileUtils.moveFile(new File(to), new File(from));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static InputStream genBillImage(InputStream inputStream, String idn, String userCode, String kycPath) {
        PdfReader reader = null;
        PdfStamper stamp = null;
        try {
            String filePath = null;
            Image watermark = null;
            File pdfPath = new File(kycPath);
            if (!pdfPath.exists()) pdfPath.mkdirs();
            filePath = kycPath + "/" + userCode + "_cusBill.pdf";
            watermark = Image.getInstance("./images/watermark/" + userCode + ".jpg");
            File pdfFile = new File(filePath);
            if (pdfFile.exists()) pdfFile.delete();
            if (idn.length() == 11) idn = idn.substring(0, 10);
            else if (idn.length() == 9) idn = idn.substring(0, 8);
            if (StringUtils.isEmpty(idn)) reader = new PdfReader(inputStream);
            else reader = new PdfReader(inputStream, idn.getBytes());

            stamp = new PdfStamper(reader, new FileOutputStream(filePath));
            int total = reader.getNumberOfPages();
            PdfGState gs1 = new PdfGState();
            gs1.setFillOpacity(0.1f);
            float pageHeight = 0;
            float pageWidth = 0;
            for (int i = 1; i <= total; i++) {
                pageHeight = reader.getPageSize(i).getHeight();
                pageWidth = reader.getPageSize(i).getWidth();
                PdfContentByte under = stamp.getOverContent(i);
                under.saveState();
                under.setGState(gs1);

                float waterHeight = 0;
                float waterWidth = 20;
                while (waterWidth < pageWidth) {
                    waterHeight = 0;
                    while (waterHeight < pageHeight) {
                        watermark.setAbsolutePosition(waterWidth, waterHeight);
                        under.addImage(watermark);
                        waterHeight += 130;
                    }
                    waterWidth += 150;
                }

                under.restoreState();
            }
            stamp.close();
            reader.close();
            return new FileInputStream(filePath);
        } catch (Exception e) {
            log.error("Get image error: ", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e1) {
            }
            try {
                if (stamp != null) stamp.close();
            } catch (Exception e) {
            }
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
            }
        }
        return null;
    }
}
