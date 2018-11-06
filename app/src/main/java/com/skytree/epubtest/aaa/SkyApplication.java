package com.skytree.epubtest.aaa;

import android.app.Application;

import com.skytree.epub.BookInformation;
import com.skytree.epub.SkyKeyManager;


import java.util.ArrayList;

public class SkyApplication extends Application {
    public String message = "We are the world.";
    public ArrayList<BookInformation> bis;
    public SkySetting setting;
    public SkyDatabase sd = null;
    public int sortType = 0;
    public SkyKeyManager keyManager;
    private static SkyApplication application;

    public String getApplicationName() {
        int stringId = this.getApplicationInfo().labelRes;
        return this.getString(stringId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String appName = this.getApplicationName();
        if (SkySetting.getStorageDirectory() == null) {
            SkySetting.setStorageDirectory(getFilesDir().getAbsolutePath(), appName);
        }
        application = this;
        sd = new SkyDatabase(this);
        reloadBookInformations();
        loadSetting();
        createSkyDRM();
    }

    public static SkyApplication getApplication() {
        return application;
    }

    public void reloadBookInformations() {
        this.bis = sd.fetchBookInformations(sortType, "");
    }

    public void reloadBookInformations(String key) {
        this.bis = sd.fetchBookInformations(sortType, key);
    }

    public void loadSetting() {
        this.setting = sd.fetchSetting();
    }

    public void saveSetting() {
        sd.updateSetting(this.setting);
    }

    public void createSkyDRM() {
        this.keyManager = new SkyKeyManager("A3UBZzJNCoXmXQlBWD4xNo", "zfZl40AQXu8xHTGKMRwG69");
    }
}
