package com.skytree.epubtest.aaa;

import com.skytree.epub.Book;
import com.skytree.epub.KeyListener;

/**
 * Created by zyt on 2018/9/3.
 */

public class KeyDelegate implements KeyListener {
    @Override
    public String getKeyForEncryptedData(String uuidForContent, String contentName, String uuidForEpub) {
        String key = SkyApplication.getApplication().keyManager.getKey(uuidForContent, uuidForEpub);
        return key;
    }

    @Override
    public Book getBook() {
        return null;
    }
}