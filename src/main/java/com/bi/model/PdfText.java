package com.bi.model;

/**
 * 代表附加到pdf檔案的文字內容
 */
public class PdfText {

    /**
     * 附加到pdf檔案的頁次
     */
    private int pageNum;
    private String text;
    private String fontName;
    private float size;
    private float axisX;
    private float axisY;

    public static PdfText getInstance(int pageNum, String text,
                                      String fontName, float size, float axisX, float axisY) {
        return new PdfText(pageNum, text, fontName, size, axisX, axisY);
    }

    public static PdfText getInstance(int pageNum, String text,
                                      float size, float axisX, float axisY) {
        return new PdfText(pageNum, text, null, size, axisX, axisY);
    }

    private PdfText(int pageNum, String text, String fontName, float size,
                    float axisX, float axisY) {
        this.pageNum = pageNum;
        this.text = text;
        this.fontName = fontName;
        this.size = size;
        this.axisX = axisX;
        this.axisY = axisY;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getAxisX() {
        return axisX;
    }

    public void setAxisX(float axisX) {
        this.axisX = axisX;
    }

    public float getAxisY() {
        return axisY;
    }

    public void setAxisY(float axisY) {
        this.axisY = axisY;
    }

}
