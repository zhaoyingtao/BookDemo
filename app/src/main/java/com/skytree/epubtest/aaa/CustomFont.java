package com.skytree.epubtest.aaa;

/**
 * Created by zyt on 2018/9/3.
 */

public class CustomFont {
    public String fontFaceName;
    public String fontFileName;

    public CustomFont(String faceName, String fileName) {
        this.fontFaceName = faceName;
        this.fontFileName = fileName;
    }

    public String getFullName() {
        String fullName = "";
        if (fontFileName == null || fontFileName.isEmpty()) {
            fullName = this.fontFaceName;
        } else {
            fullName = this.fontFaceName + "!!!/fonts/" + this.fontFileName;
        }
        return fullName;
    }
}