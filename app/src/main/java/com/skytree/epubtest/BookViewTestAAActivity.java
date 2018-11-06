package com.skytree.epubtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skytree.epub.Book;
import com.skytree.epub.Highlights;
import com.skytree.epub.KeyListener;
import com.skytree.epub.PageInformation;
import com.skytree.epub.PageMovedListener;
import com.skytree.epub.PageTransition;
import com.skytree.epub.PagingInformation;
import com.skytree.epub.ReflowableControl;
import com.skytree.epub.ScriptListener;
import com.skytree.epub.Setting;
import com.skytree.epub.SkyProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 翻书页面
 */

public class BookViewTestAAActivity extends Activity {
    ReflowableControl rv;
    RelativeLayout ePubView;

    ImageButton rotationButton;
    ImageButton listButton;
    ImageButton fontButton;
    ImageButton searchButton;
    Rect bookmarkRect;
    Rect bookmarkedRect;

    boolean isRotationLocked;

    TextView titleLabel;
    TextView authorLabel;
    TextView pageIndexLabel;
    TextView secondaryIndexLabel;

    boolean isBoxesShown;
    SkySetting setting;
    SkyDatabase sd;

    Button outsideButton;

    String fileName;
    String author;
    String title;

    View pagingView;


    double pagePositionInBook = -1;
    int bookCode;

    boolean autoStartPlayingWhenNewChapterLoaded = false;  // TTS 임시
    boolean autoMoveChapterWhenParallesFinished = true;
    boolean isAutoPlaying = true;
    boolean isChapterJustLoaded = false;

    boolean isDoublePagedForLandscape;
    boolean isGlobalPagination;

    boolean isRTL = false;
    boolean isVerticalWriting = false;

    Highlights highlights;

    boolean isFullScreenForNexus = true;

    ArrayList<Theme> themes = new ArrayList<Theme>();
    int themeIndex = -1;

    View videoView = null;

    ArrayList<CustomFont> fonts = new ArrayList<CustomFont>();

    PageInformation currentPageInformation;

    TextToSpeech ttsPlayer;

    public int getDensityDPI() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int density = metrics.densityDpi;
        return density;
    }

    public boolean isHighDensityPhone() {    // if HIGH density (not XHIGH) phone like Galaxy S2, retuns true;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int p0 = metrics.heightPixels;
        int p1 = metrics.widthPixels;
        int max = Math.max(p0, p1);
        if (metrics.densityDpi == 240 && max == 800) {
            return true;
        } else {
            return false;
        }
    }

    // We use 240 base to meet the webview coodinate system instead of 160.
    public int getPS(float dip) {
        float density = this.getDensityDPI();
        float factor = (float) density / 240.f;
        int px = (int) (dip * factor);
        return px;
    }

    public int getPSFromDP(float dps) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps, metrics);
        return (int) pixels;
    }

    public int getPXFromLeft(float dip) {
        int ps = this.getPS(dip);
        return ps;
    }

    public int getPXFromRight(float dip) {
        int ps = this.getPS(dip);
        int ms = this.getWidth() - ps;
        return ms;
    }

    public int getPYFromTop(float dip) {
        int ps = this.getPS(dip);
        return ps;
    }

    public int getPYFromBottom(float dip) {
        int ps = this.getPS(dip);
        int ms = this.getHeight() - ps;
        return ms;
    }

    public int pxl(float dp) {
        return this.getPXFromLeft(dp);
    }

    public int pxr(float dp) {
        return this.getPXFromRight(dp);
    }

    public int pyt(float dp) {
        return this.getPYFromTop(dp);
    }

    public int pyb(float dp) {
        return this.getPYFromBottom(dp);
    }

    public int ps(float dp) {
        return this.getPS(dp);
    }

    public int pw(float sdp) {
        int ps = this.getPS(sdp * 2);
        int ms = this.getWidth() - ps;
        return ms;
    }

    public int cx(float dp) {
        int ps = this.getPS(dp);
        int ms = this.getWidth() / 2 - ps / 2;
        return ms;
    }

    // in double paged and landscape mode,get the center of view(its width is dpWidth) on left page
    public int lcx(float dpWidth) {
        int ps = this.getPS(dpWidth);
        int ms = this.getWidth() / 4 - ps / 2;
        return ms;
    }

    // in double paged and landscape mode,get the center of view(its width is dpWidth) on right page
    public int rcx(float dpWidth) {
        int ps = this.getPS(dpWidth);
        int ms = this.getWidth() / 2 + this.getWidth() / 4 - ps / 2;
        return ms;
    }


    public float getDIP(float px) {
        float densityDPI = this.getDensityDPI();
        float dip = px / (densityDPI / 240);
        return dip;
    }


    public int getDarkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        int darker = Color.HSVToColor(hsv);
        return darker;
    }

    /**
     * 修改阅读书的背景
     *
     * @return
     */
    public Bitmap getBackgroundForLandscape() {
        Bitmap backgroundForLandscape = null;
        Theme theme = getCurrentTheme();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        if (this.isDoublePagedForLandscape) {
            backgroundForLandscape = BitmapFactory.decodeFile(SkySetting.getStorageDirectory() + "/images/" + theme.doublePagedName, options);
        } else {
            backgroundForLandscape = BitmapFactory.decodeFile(SkySetting.getStorageDirectory() + "/images/" + theme.landscapeName, options);
        }
        return backgroundForLandscape;
    }

    /**
     * 修改阅读书的背景
     *
     * @return
     */
    public Bitmap getBackgroundForPortrait() {
        Bitmap backgroundForPortrait;
        Theme theme = getCurrentTheme();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = true;
        backgroundForPortrait = BitmapFactory.decodeFile(SkySetting.getStorageDirectory() + "/images/" + theme.portraitName, options);
        return backgroundForPortrait;
    }

    /**
     * 图片链接转换成 Bitmap
     * @param filename
     * @return
     */
    public Bitmap getBitmap(String filename) {
        Bitmap bitmap;
        bitmap = BitmapFactory.decodeFile(SkySetting.getStorageDirectory() + "/images/" + filename);
        return bitmap;
    }

    public int getMaxSize() {
        int width = this.getRawWidth();
        int height = this.getRawHeight();
        return Math.max(width, height);
    }

    public Theme getCurrentTheme() {
        Theme theme = themes.get(themeIndex);
        return theme;
    }

    public void setThemeIndex(int index) {
        themeIndex = index;
    }

    //  	String fontNames[] = {"Book Fonts","Sans Serif","Serif","Monospace"};

    public void makeFonts() {
        fonts.clear();
        fonts.add(0, new CustomFont("Monospace", ""));
        fonts.add(0, new CustomFont("Serif", ""));
        fonts.add(0, new CustomFont("Sans Serif", ""));
        fonts.add(0, new CustomFont("Book Fonts", ""));
        for (int i = 0; i < fonts.size(); i++) {
            this.fonts.add(fonts.get(i));
        }
    }

    public int getOSVersion() {
        return Build.VERSION.SDK_INT;
    }


    public void onDestroy() {
        // Stop loading the ad.
        this.unregisterSkyReceiver(); // New in SkyEpub sdk 7.x
        super.onDestroy();
    }

    boolean isInitialized = false;


    /**
     * 绘制view以及初始化书本
     */
    public void makeLayout() {
        debug("makeLayout");
        // make fonts
        //TODO 1111
//        this.makeFonts();
        // clear the existing themes.
        themes.clear();
        // add themes
        // String name,int foregroundColor,int backgroundColor,int controlColor,int controlHighlightColor,int seekBarColor,int seekThumbColor,int selectorColor,int selectionColor,String portraitName,String landscapeName,String doublePagedName,int bookmarkId
        themes.add(new Theme("white", Color.BLACK, 0xffffffff, Color.argb(240, 94, 61, 35), Color.LTGRAY, Color.argb(240, 94, 61, 35), Color.argb(120, 160, 124, 95), Color.DKGRAY, 0x22222222, "Phone-Portrait-White.png", "Phone-Landscape-White.png", "Phone-Landscape-Double-White.png", R.drawable.bookmark2x));
        themes.add(new Theme("brown", Color.BLACK, 0xffece3c7, Color.argb(240, 94, 61, 35), Color.argb(255, 255, 255, 255), Color.argb(240, 94, 61, 35), Color.argb(120, 160, 124, 95), Color.DKGRAY, 0x22222222, "Phone-Portrait-Brown.png", "Phone-Landscape-Brown.png", "Phone-Landscape-Double-Brown.png", R.drawable.bookmark2x));
        themes.add(new Theme("black", Color.LTGRAY, 0xff323230, Color.LTGRAY, Color.LTGRAY, Color.LTGRAY, Color.LTGRAY, Color.LTGRAY, 0x77777777, null, null, "Phone-Landscape-Double-Black.png", R.drawable.bookmarkgray2x));
        themes.add(new Theme("Leaf", 0xFF1F7F0E, 0xffF8F7EA, 0xFF186D08, Color.LTGRAY, 0xFF186D08, 0xFF186D08, Color.DKGRAY, 0x22222222, null, null, null, R.drawable.bookmarkgray2x));
        themes.add(new Theme("夕陽", 0xFFA13A0A, 0xFFF6DFD9, 0xFFA13A0A, 0xFFDC4F0E, 0xFFA13A0A, 0xFFA13A0A, Color.DKGRAY, 0x22222222, null, null, null, R.drawable.bookmarkgray2x));
        this.setBrightness((float) setting.brightness);
        // create highlights object to contains highlights of this book.
        highlights = new Highlights();
        Bundle bundle = getIntent().getExtras();
        fileName = bundle.getString("BOOKNAME");
        author = bundle.getString("AUTHOR");
        title = bundle.getString("TITLE");
        bookCode = bundle.getInt("BOOKCODE");
        if (pagePositionInBook == -1) pagePositionInBook = bundle.getDouble("POSITION");
        themeIndex = setting.theme;
        themeIndex = 0;
        this.isGlobalPagination = bundle.getBoolean("GLOBALPAGINATION");
        this.isRTL = bundle.getBoolean("RTL");
        this.isVerticalWriting = bundle.getBoolean("VERTICALWRITING");
        this.isDoublePagedForLandscape = bundle.getBoolean("DOUBLEPAGED");
//		if (this.isRTL) this.isDoublePagedForLandscape = false; // In RTL mode, SDK does not support double paged.

        autoStartPlayingWhenNewChapterLoaded = this.setting.autoStartPlaying;
        autoMoveChapterWhenParallesFinished = this.setting.autoLoadNewChapter;

        ePubView = new RelativeLayout(this);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        ePubView.setLayoutParams(rlp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height
        if (this.getOSVersion() >= 11) {
            rv = new ReflowableControl(this);                        // in case that device supports transparent webkit, the background image under the content can be shown. in some devices, content may be overlapped.
        } else {
            rv = new ReflowableControl(this, getCurrentTheme().backgroundColor);            // in case that device can not support transparent webkit, the background color will be set in one color.
        }

        // if true, sdk will adjust the size of image,video tag to fit screen.
        rv.setAutoAdjustContent(true);
        // if true, sdk will prevent swiping for pageTransition.
        rv.setSwipeEnabled(true);

        // if false highlight will be drawed on the back of text - this is default.
        // for the very old devices of which GPU does not support transparent webView background, set the value to true.
        rv.setDrawingHighlightOnFront(false);
        // set the bookCode to identify the book file.
        rv.bookCode = this.bookCode;
        // set bitmaps for engine.
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bookmarked2x);
        rv.setPagesStackImage(bitmap);
        rv.setPagesCenterImage(bitmap);
        // for epub3 which has page-progression-direction="rtl", rv.isRTL() will return true.
        // for old RTL epub which does not have <spine toc="ncx" page-progression-direction="rtl"> in opf file.
        // you can enforce RTL mode.

/*
        // delay times for proper operations.
		// !! DO NOT SET these values if there's no issue on your epub reader. !!
		// !! if delayTime is decresed, performance will be increase
		// !! if delayTime is set to too low value, a lot of problem can be occurred.
		// bringDelayTime(default 500 ms) is for curlView and mainView transition - if the value is too short, blink may happen.
		rv.setBringDelayTime(500);
		// reloadDelayTime(default 100) is used for delay before reload (eg. changeFont, loadChapter or etc)
		rv.setReloadDelayTime(100);
		// reloadDelayTimeForRotation(default 1000) is used for delay before rotation
		rv.setReloadDelayTimeForRotation(1000);
		// retotaionDelayTime(default 1500) is used for delay after rotation.
		rv.setRotationDelayTime(1500);
		// finalDelayTime(default 500) is used for the delay after loading chapter.
		rv.setFinalDelayTime(500);
		// rotationFactor affects the delayTime before Rotation. default value 1.0f
		rv.setRotationFactor(1.0f);
		// If recalcDelayTime is too short, setContentBackground function failed to work properly.
		rv.setRecalcDelayTime(2500);
*/


        // set the max width or height for background.
        rv.setMaxSizeForBackground(1024);
//		rv.setBaseDirectory(SkySetting.getStorageDirectory() + "/books");
//		rv.setBookName(fileName);
        // set the file path of epub to open
        // Be sure that the file exists before setting.
        rv.setBookPath(SkySetting.getStorageDirectory() + "/books/" + fileName);
        // if true, double pages will be displayed on landscape mode.
        rv.setDoublePagedForLandscape(this.isDoublePagedForLandscape);
        // set the initial font style for book.
        // set the initial line space for book.
        rv.setLineSpacing(this.getRealLineSpace(setting.lineSpacing)); // the value is supposed to be percent(%).
        // set the horizontal gap(margin) on both left and right side of each page.
        rv.setHorizontalGapRatio(0.30);
        // set the vertical gap(margin) on both top and bottom side of each page.
        rv.setVerticalGapRatio(0.22);
        // set the PageMovedListener which is called whenever page is moved.
        rv.setPageMovedListener(new PageMovedDelegate());
        // set the SelectionListener to handle text selection.
        // set the scriptListener to set custom javascript.
        rv.setScriptListener(new ScriptDelegate());

        // enable/disable scroll mode
        rv.setScrollMode(false);

        // for some anroid device, when rendering issues are occurred, use "useSoftwareLayer"
        rv.useSoftwareLayer();
        // In search keyword, if true, sdk will return search result with the full information such as position, pageIndex.
        rv.setFullSearch(false);
        // if true, sdk will return raw text for search result, highlight text or body text without character escaping.
        rv.setRawTextRequired(false);

        // if true, sdk will read the content of book directry from file system, not via Internal server.
//		rv.setDirectRead(true);

        // If you want to make your own provider, please look into EpubProvider.java in Advanced demo.
//		EpubProvider epubProvider = new EpubProvider();
//		rv.setContentProvider(epubProvider);

        // SkyProvider is the default ContentProvider which is presented with SDK.
        // SkyProvider can read the content of epub file without unzipping.
        // SkyProvider is also fully integrated with SkyDRM solution.
        SkyProvider skyProvider = new SkyProvider();
        skyProvider.setKeyListener(new KeyDelegate());
        rv.setContentProvider(skyProvider);

        // set the start positon to open the book.
        rv.setStartPositionInBook(pagePositionInBook);
        // DO NOT USE BELOW, if true , sdk will use DOM to highlight text.
//		rv.useDOMForHighlight(false);
        // if true, globalPagination will be activated.
        // this enables the calculation of page number based on entire book ,not on each chapter.
        // this globalPagination consumes huge computing power.
        // AVOID GLOBAL PAGINATION FOR LOW SPEC DEVICES.
        rv.setGlobalPagination(this.isGlobalPagination);
        // set the navigation area on both left and right side to go to the previous or next page when the area is clicked.
        rv.setNavigationAreaWidthRatio(0.2f); // both left and right side.
        // set the navigation area enabled
        rv.setNavigationAreaEnabled(true);
        //设置字体
        rv.changeFont(setting.fontName, this.getRealFontSize(setting.fontSize));
        // set the device locked to prevent Rotation.
        rv.setRotationLocked(setting.lockRotation);
        isRotationLocked = setting.lockRotation;

        // set the audio playing based on Sequence.
        rv.setSequenceBasedForMediaOverlay(false);

        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        params.height = RelativeLayout.LayoutParams.MATCH_PARENT;

        rv.setLayoutParams(params);
        this.applyThemeToRV(themeIndex);
        if (this.isFullScreenForNexus && SkyUtility.isNexus() && Build.VERSION.SDK_INT >= 19) {
            rv.setImmersiveMode(true);
        }
        // If you want to get the license key for commercial use, please email us (skytree21@gmail.com).
        // Without the license key, watermark message will be shown in background.
        rv.setLicenseKey("0000-0000-0000-0000");

        rv.setSigilStyleEnabled(false);
        rv.setBookStyleEnabled(true);
        rv.setBookFontEnabled(false);
//		rv.setDelayFactor(0.25f);

        // set PageTransition Effect
        int transitionType = bundle.getInt("transitionType");
        if (transitionType == 0) {
            rv.setPageTransition(PageTransition.None);
        } else if (transitionType == 1) {
            rv.setPageTransition(PageTransition.Slide);
        } else if (transitionType == 2) {
            rv.setPageTransition(PageTransition.Curl);
        }

        // setCurlQuality effects the image quality when tuning page in Curl Transition Mode.
        // If "Out of Memory" occurs in high resolution devices with big screen,
        // this value should be decreased like 0.25f or below.
        if (this.getMaxSize() <= 1280) {
            rv.setCurlQuality(1.0f);
        } else if (this.getMaxSize() <= 1920) {
            rv.setCurlQuality(0.9f);
        } else {
            rv.setCurlQuality(0.8f);
        }


        // set the color of text selector.
        rv.setSelectorColor(getCurrentTheme().selectorColor);
        // set the color of text selection area.
        rv.setSelectionColor(getCurrentTheme().selectionColor);

        // setCustomDrawHighlight & setCustomDrawCaret work only if SDK >= 11
        // if true, sdk will ask you how to draw the highlighted text
        rv.setCustomDrawHighlight(true);
        // if true, sdk will require you to draw the custom selector.
        rv.setCustomDrawCaret(true);

        rv.setFontUnit("px");

        rv.setFingerTractionForSlide(true);

        // make engine not to send any event to iframe
        // if iframe clicked, onIFrameClicked will be fired with source of iframe
        // By Using that source of iframe, you can load the content of iframe in your own webView or another browser.
        rv.setSendingEventsToIFrameEnabled(false);

        // make engine send any event to video(tag) or not
        // if video tag is clicked, onVideoClicked will be fired with source of iframe
        // By Using that source of video, you can load the content of video in your own media controller or another browser.
        rv.setSendingEventsToVideoEnabled(true);

        // make engine send any event to video(tag) or not
        // if video tag is clicked, onVideoClicked will be fired with source of iframe
        // By Using that source of video, you can load the content of video in your own media controller or another browser.
        rv.setSendingEventsToAudioEnabled(true);

        // if true, sdk will return the character offset from the chapter beginning , not from element index.
        // then startIndex, endIndex of highlight will be 0 (zero)
        rv.setGlobalOffset(true);
        // if true, sdk will return the text of each page in the PageInformation object which is passed in onPageMoved event.
        rv.setExtractText(true);

        // if true, TextToSpeech will be enabled
        rv.setTTSEnabled(this.setting.tts);            // if true, TextToSpeech will be enabled.
//		rv.setTTSLanguage(Locale.US); 	// change Locale according to the language of book. if not set, skyepub sdk tries to dectect the locale for this book.
        rv.setTTSPitch(1.0f);            // if value is 2.0f, the pitch of voice is double times higher than normal, 1.0f is normal pitch.
        rv.setTTSSpeedRate(1.0f);        // if value is 2.0f , the speed is double times faster than normal. 1.0f is normal speed;

        rv.setExternalSynthesizerEnabled(false); // if true, skyepub will request to generate tts voice file.

        ttsPlayer = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                try {
                    if (status != TextToSpeech.ERROR) {
                        ttsPlayer.setPitch(rv.getTtsPitch());
                        ttsPlayer.setSpeechRate(rv.getTTSSpeedRate());
                        ttsPlayer.setLanguage(rv.getTTSLocale());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // Add ReflowableView into Main View.
        ePubView.addView(rv);
        this.makeControls();
        recalcLabelsLayout();
        setContentView(ePubView);
        this.isInitialized = true;
    }

    // if the current theme should be changed while book is opened,
    // use this function.
    private void changeTheme(int newIndex) {
        if (newIndex > themes.size() - 1 || newIndex < 0) return;
        this.setThemeIndex(newIndex);
        this.applyThemeToRV(newIndex);
        this.processPageMoved(rv.getPageInformation());
    }

    // if the current theme should be changed while book is opened,
    // use this function. (it takes some time because this reconstructs every user interface.)
    private void changeTheme2(int newIndex) {
        if (newIndex > themes.size() - 1 || newIndex < 0) return;
        this.setThemeIndex(newIndex);
        this.ePubView.removeAllViews();
        this.makeLayout();
    }

    /**
     * 重要不能删除
     *
     * @param themeIndex
     */
    public void applyThemeToRV(int themeIndex) {
        this.themeIndex = themeIndex;
        // set BackgroundImage
        // the first  Rect should be the rect of background image itself
        // the second Rect is used to define the inner client area which the real contentView will reside.
        if (this.isDoublePagedForLandscape) {
            rv.setBackgroundForLandscape(this.getBackgroundForLandscape(), new Rect(0, 0, 2004, 1506), new Rect(32, 0, 2004 - 32, 1506));            // Android Rect - left,top,right,bottom
        } else {
            rv.setBackgroundForLandscape(this.getBackgroundForLandscape(), new Rect(0, 0, 2004, 1506), new Rect(0, 0, 2004 - 32, 1506));            // Android Rect - left,top,right,bottom
        }
        rv.setBackgroundForPortrait(this.getBackgroundForPortrait(), new Rect(0, 0, 1002, 1506), new Rect(0, 0, 1002 - 32, 1506));            // Android Rect - left,top,right,bottom

        // setBackgroundColor is used to set the background color in initial time.
        // changeBackgroundColor is used to set the background color in run time.
        // both are effective only when background image is not set or null.
        if (!this.isInitialized) {
            rv.setBackgroundColor(getCurrentTheme().backgroundColor);
            rv.setForegroundColor(getCurrentTheme().foregroundColor);
        } else {
            rv.changeBackgroundColor(getCurrentTheme().backgroundColor);
            rv.changeForegroundColor(getCurrentTheme().foregroundColor);
            rv.recalcLayout();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    rv.repaint();
                }
            }, 1000);
        }
    }

    public int getColorWithAlpha(int color, int alpha) {
        int red, green, blue;
        red = Color.red(color);
        green = Color.green(color);
        blue = Color.blue(color);
        int newColor = Color.argb(alpha, red, green, blue);
        return newColor;
    }

    public int getBrighterColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 1.2f; // value component
        int darker = Color.HSVToColor(hsv);
        return darker;
    }

    public void removeBoxes() {
        this.ePubView.removeView(pagingView);
    }

    public void makeBoxes() {
        this.removeBoxes();
        this.makeOutsideButton();
        this.makePagingView();
    }

    public void makeOutsideButton() {
        outsideButton = new Button(this);
        outsideButton.setId(9999);
        outsideButton.setBackgroundColor(Color.TRANSPARENT);
//		rv.customView.addView(outsideButton);
        ePubView.addView(outsideButton);
        hideOutsideButton();
    }

    public void showOutsideButton() {
        this.setFrame(outsideButton, 0, 0, this.getWidth(), this.getHeight());
        outsideButton.setVisibility(View.VISIBLE);
    }

    public void hideOutsideButton() {
        if (outsideButton == null) {
            return;
        }
        outsideButton.setVisibility(View.INVISIBLE);
        outsideButton.setVisibility(View.GONE);
    }

    public Typeface getTypeface(String fontName, int fontStyle) {
        Typeface tf = null;
        if (fontName.toLowerCase().contains("book")) {
            tf = Typeface.create(Typeface.DEFAULT, fontStyle);
        } else if (fontName.toLowerCase().contains("default")) {
            tf = Typeface.create(Typeface.DEFAULT, fontStyle);
        } else if (fontName.toLowerCase().contains("mono")) {
            tf = Typeface.create(Typeface.MONOSPACE, fontStyle);
        } else if ((fontName.toLowerCase().contains("sans"))) {
            tf = Typeface.create(Typeface.SANS_SERIF, fontStyle);
        } else if ((fontName.toLowerCase().contains("serif"))) {
            tf = Typeface.create(Typeface.SERIF, fontStyle);
        }
        return tf;
    }

    // CustomFont
    public CustomFont getCustomFont(int fontIndex) {
        if (fontIndex < 0) fontIndex = 0;
        if (fontIndex > (fonts.size() - 1)) fontIndex = fonts.size() - 1;
        return fonts.get(fontIndex);
    }

    // CustomFont
    public int getFontIndex(String fontName) {
        for (int i = 0; i < fonts.size(); i++) {
            CustomFont customFont = fonts.get(i);
            String name = customFont.getFullName();
            if (name.equalsIgnoreCase(fontName)) return i;
        }
        return 0;
    }

    /**
     * 创建textView
     *
     * @param id
     * @param text
     * @param gravity
     * @param textSize
     * @param textColor
     * @return
     */
    public TextView makeLabel(int id, String text, int gravity, float textSize, int textColor) {
        TextView label = new TextView(this);
        label.setId(id);
        label.setGravity(gravity);
        label.setBackgroundColor(Color.TRANSPARENT);
        label.setText(text);
        label.setTextColor(textColor);
        label.setTextSize(textSize);
        return label;
    }

    public void setFrame(View view, int dx, int dy, int width, int height) {
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height
        param.leftMargin = dx;
        param.topMargin = dy;
        param.width = width;
        param.height = height;
        view.setLayoutParams(param);
    }

    /**
     * 设置view的位置
     *
     * @param view
     * @param px
     * @param py
     */
    public void setLocation(View view, int px, int py) {
        if (view == null) {
            return;
        }
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height
        param.leftMargin = px;
        param.topMargin = py;
        view.setLayoutParams(param);
    }

    public void setLocation2(View view, int px, int py) {
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT); // width,height
        param.leftMargin = px;
        param.topMargin = py;
        view.setLayoutParams(param);
    }

    int getLabelWidth(TextView tv) {
        tv.measure(0, 0);       //must call measure!
        return tv.getMeasuredWidth();  //get height
    }

    int getLabelHeight(TextView tv) {
        tv.measure(0, 0);       //must call measure!
        return tv.getMeasuredHeight(); //get width
    }


    public boolean isAboveIcecream() {
        if (Build.VERSION.SDK_INT >= 14) { //  api >= icecream
            return true;
        } else {
            return false;
        }
    }

    public boolean isHoneycomb() {
        return false;
        //		int API = android.os.Build.VERSION.SDK_INT;
//		if (API == 11 || API == 12 || API == 13 ) { 	// Honeycomb
//			return true;
//		}else {
//			return false;
//		}
    }

    public void removeControls() {
        rv.customView.removeView(rotationButton);
        rv.customView.removeView(titleLabel);
        rv.customView.removeView(authorLabel);

        ePubView.removeView(rotationButton);
        ePubView.removeView(listButton);
        ePubView.removeView(fontButton);
        ePubView.removeView(searchButton);

        ePubView.removeView(pageIndexLabel);
        ePubView.removeView(secondaryIndexLabel);
    }

    /**
     * 初始化界面的其他UI
     */
    public void makeControls() {
        this.removeControls();
        Theme theme = getCurrentTheme();

        titleLabel = this.makeLabel(3000, title, Gravity.CENTER_HORIZONTAL, 17, Color.argb(240, 94, 61, 35));    // setTextSize in android uses sp (Scaled Pixel) as default, they say that sp guarantees the device dependent size, but as usual in android it can't be 100% sure.
        authorLabel = this.makeLabel(3000, author, Gravity.CENTER_HORIZONTAL, 17, Color.argb(240, 94, 61, 35));
        pageIndexLabel = this.makeLabel(3000, "......", Gravity.CENTER_HORIZONTAL, 13, Color.argb(240, 94, 61, 35));
        secondaryIndexLabel = this.makeLabel(3000, "......", Gravity.CENTER_HORIZONTAL, 13, Color.argb(240, 94, 61, 35));
        //取消title和作者的添加
//        rv.customView.addView(titleLabel);
//        rv.customView.addView(authorLabel);


        ePubView.addView(pageIndexLabel);
        ePubView.addView(secondaryIndexLabel);


        int filterColor = theme.controlColor;

        authorLabel.setTextColor(filterColor);
        titleLabel.setTextColor(filterColor);
        pageIndexLabel.setTextColor(filterColor);
        secondaryIndexLabel.setTextColor(filterColor);

    }

    public void makePagingView() {
        Theme theme = getCurrentTheme();
        pagingView = new View(this);
        pagingView.setBackgroundDrawable(new DottedDrawable(Color.BLACK, theme.seekBarColor, 100));

        ePubView.addView(pagingView);
        this.hidePagingView();
    }

    public void showPagingView() {
        pagingView.setVisibility(View.VISIBLE);
    }

    public void hidePagingView() {
        pagingView.setVisibility(View.INVISIBLE);
        pagingView.setVisibility(View.GONE);
    }

    public void changePagingView(int value) {
        Theme theme = this.getCurrentTheme();
        pagingView.setBackgroundDrawable(new DottedDrawable(Color.RED, theme.seekBarColor, value));
    }

    public void setLabelLength(TextView label, int maxLength) {
        String text = (String) label.getText();
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
            text = text + "..";
        }
        label.setText(text);
    }

    public void recalcLabelsLayout() {
        this.authorLabel.setVisibility(View.VISIBLE);
        this.secondaryIndexLabel.setVisibility(View.VISIBLE);
        int sd = this.getWidth() / 40;
        this.titleLabel.setText(this.title);
        String authorText = this.author;
        if (authorText.length() > 12) authorText = authorText.substring(0, 12);
        this.authorLabel.setText(authorText);

        if (!this.isTablet()) {                                                            // phone
            if (this.isPortrait()) {
                this.setLabelLength(titleLabel, 10);
                this.setLocation(titleLabel, (this.getWidth() / 2 - this.getLabelWidth(titleLabel) / 2) - sd, pyt(28));
                this.authorLabel.setVisibility(View.INVISIBLE);
                this.authorLabel.setVisibility(View.GONE);
                this.secondaryIndexLabel.setVisibility(View.INVISIBLE);
                this.secondaryIndexLabel.setVisibility(View.GONE);
                this.setLocation(pageIndexLabel, (this.getWidth() / 2 - this.getLabelWidth(pageIndexLabel) / 2) - sd, pyb(90));
            } else {
                if (this.isDoublePagedForLandscape) {
                    this.setLabelLength(titleLabel, 10);
                    if (this.isHighDensityPhone()) {
                        this.authorLabel.setVisibility(View.INVISIBLE);
                        this.authorLabel.setVisibility(View.GONE);
                    } else {
                        this.authorLabel.setVisibility(View.VISIBLE);
                    }
                    this.secondaryIndexLabel.setVisibility(View.VISIBLE);
                    this.setLocation(titleLabel, this.getWidth() / 4 - this.getLabelWidth(titleLabel) / 2, pyt(17));
                    this.setLocation(authorLabel, this.getWidth() / 2 + this.getWidth() / 4 - this.getLabelWidth(authorLabel) / 2 - sd * 4, pyt(17));
                    if (!this.isRTL) {
                        this.setLocation(pageIndexLabel, this.getWidth() / 4 - this.getLabelWidth(pageIndexLabel) / 2, pyb(85));
                        this.setLocation(secondaryIndexLabel, this.getWidth() / 2 + this.getWidth() / 4 - this.getLabelWidth(secondaryIndexLabel) / 2, pyb(85));
                    } else {
                        this.setLocation(secondaryIndexLabel, this.getWidth() / 4 - this.getLabelWidth(pageIndexLabel) / 2, pyb(85));
                        this.setLocation(pageIndexLabel, this.getWidth() / 2 + this.getWidth() / 4 - this.getLabelWidth(secondaryIndexLabel) / 2, pyb(85));
                    }
                } else {
                    this.setLabelLength(titleLabel, 40);
                    this.setLocation(titleLabel, (this.getWidth() / 2 - this.getLabelWidth(titleLabel) / 2) - sd, pyt(17));
                    this.authorLabel.setVisibility(View.INVISIBLE);
                    this.authorLabel.setVisibility(View.GONE);
                    this.secondaryIndexLabel.setVisibility(View.INVISIBLE);
                    this.secondaryIndexLabel.setVisibility(View.GONE);
                    this.setLocation(pageIndexLabel, (this.getWidth() / 2 - this.getLabelWidth(pageIndexLabel) / 2) - sd, pyb(85));
                }
            }
        } else {
            if (this.isPortrait()) {                                                    // tablet
                this.setLabelLength(titleLabel, 20);
                this.setLocation(titleLabel, (this.getWidth() / 2 - this.getLabelWidth(titleLabel) / 2) - sd, pyt(28 + 20));
                this.authorLabel.setVisibility(View.INVISIBLE);
                this.authorLabel.setVisibility(View.GONE);
                this.secondaryIndexLabel.setVisibility(View.INVISIBLE);
                this.secondaryIndexLabel.setVisibility(View.GONE);
                if (this.isHoneycomb()) {
                    this.setLocation(pageIndexLabel, (this.getWidth() / 2 - this.getLabelWidth(pageIndexLabel) / 2) - sd, pyb(100 + 80));
                } else {
                    this.setLocation(pageIndexLabel, (this.getWidth() / 2 - this.getLabelWidth(pageIndexLabel) / 2) - sd, pyb(100));
                }
            } else {
                if (this.isDoublePagedForLandscape) {
                    this.setLabelLength(titleLabel, 20);
                    this.setLocation(titleLabel, this.getWidth() / 4 - this.getLabelWidth(titleLabel) / 2, pyt(30));
                    this.setLocation(authorLabel, this.getWidth() / 2 + this.getWidth() / 4 - this.getLabelWidth(authorLabel) / 2 - sd * 4, pyt(30));

                    this.setLocation(pageIndexLabel, this.getWidth() / 4 - this.getLabelWidth(pageIndexLabel) / 2, pyb(88));
                    this.setLocation(secondaryIndexLabel, this.getWidth() / 2 + this.getWidth() / 4 - this.getLabelWidth(secondaryIndexLabel) / 2, pyb(88));
                } else {
                    this.setLabelLength(titleLabel, 50);
                    this.setLocation(titleLabel, (this.getWidth() / 2 - this.getLabelWidth(titleLabel) / 2) - sd, pyt(27));
                    this.authorLabel.setVisibility(View.INVISIBLE);
                    this.authorLabel.setVisibility(View.GONE);
                    this.secondaryIndexLabel.setVisibility(View.INVISIBLE);
                    this.secondaryIndexLabel.setVisibility(View.GONE);

                    this.setLocation(pageIndexLabel, (this.getWidth() / 2 - this.getLabelWidth(pageIndexLabel) / 2) - sd, pyb(73));
                }
            }
        }
    }

    public void setLabelsText(String title, String author) {
        titleLabel.setText(title);
        authorLabel.setText(author);
    }

    /**
     * 设置页码数
     *
     * @param pageIndex
     * @param pageCount
     */
    public void setIndexLabelsText(int pageIndex, int pageCount) {
        if (pageIndex == -1 || pageCount == -1 || pageCount == 0) {
            pageIndexLabel.setText("");
            secondaryIndexLabel.setText("");
            return;
        }

        int pi = 0;
        int si = 0;
        int pc;
        if (rv.isDoublePaged()) {
            pc = pageCount * 2;
            pi = pageIndex * 2 + 1;
            si = pageIndex * 2 + 2;
        } else {
            pc = pageCount;
            pi = pageIndex + 1;
            si = pageIndex + 2;
        }
        String pt = String.format("%3d/%3d", pi, pc);
        String st = String.format("%3d/%3d", si, pc);
        pageIndexLabel.setText(pt);
        secondaryIndexLabel.setText(st);
    }

    public void setBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }

    public boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) return true;
        else return false;
    }

    // this is not 100% accurate function.
    public boolean isTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @SuppressLint("NewApi")
    public int getRawWidth() {
        int width = 0, height = 0;
        final DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        Method mGetRawH = null, mGetRawW = null;

        try {
            // For JellyBeans and onward
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                display.getRealMetrics(metrics);

                width = metrics.widthPixels;
                height = metrics.heightPixels;
            } else {
                mGetRawH = Display.class.getMethod("getRawHeight");
                mGetRawW = Display.class.getMethod("getRawWidth");

                try {
                    width = (Integer) mGetRawW.invoke(display);
                    height = (Integer) mGetRawH.invoke(display);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return 0;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return 0;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
            return width;
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
            return 0;
        }
    }

    @SuppressLint("NewApi")
    public int getRawHeight() {
        int width = 0, height = 0;
        final DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        Method mGetRawH = null, mGetRawW = null;

        try {
            // For JellyBeans and onward
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                display.getRealMetrics(metrics);
                width = metrics.widthPixels;
                height = metrics.heightPixels;
            } else {
                mGetRawH = Display.class.getMethod("getRawHeight");
                mGetRawW = Display.class.getMethod("getRawWidth");
                try {
                    width = (Integer) mGetRawW.invoke(display);
                    height = (Integer) mGetRawH.invoke(display);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return 0;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return 0;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
            return height;
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
            return 0;
        }
    }


    public int getWidth() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        if (SkyUtility.isNexus() && isFullScreenForNexus) {
            if (!this.isPortrait() && Build.VERSION.SDK_INT >= 19) {
                width = this.getRawWidth();
            }
        }
        return width;
    }

    // modify for fullscreen
    public int getHeight() {
        if (Build.VERSION.SDK_INT >= 19) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int height = this.getRawHeight();
            height += ps(50);
            if (Build.DEVICE.contains("maguro") && this.isPortrait()) {
                height -= ps(65);
            }

            return height;
        } else {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int height = metrics.heightPixels;
            height += ps(50);
            return height;
        }
    }

    public void log(String msg) {
        Log.w("EPub", msg);
    }

    // this event is called after device is rotated.
    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (this.isPortrait()) {
            log("portrait");
        } else {
            log("landscape");
        }
        this.hideBoxes();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sd = new SkyDatabase(this);
        setting = sd.fetchSetting();
        registerSkyReceiver(); // New in SkyEpub SDK 7.x
        this.makeLayout();
    }

    private BroadcastReceiver skyReceiver = null;

    private void registerSkyReceiver() {
        if (skyReceiver != null) return;
        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction(Book.SKYERROR);
        this.skyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int code = intent.getIntExtra("code", 0);
                int level = intent.getIntExtra("level", 0);
                String message = intent.getStringExtra("message");
                if (intent.getAction().equals(Book.SKYERROR)) {
                    if (level == 1) {
                        showToast("SkyError " + message);
                    }
                }
            }
        };
        this.registerReceiver(this.skyReceiver, theFilter);
    }

    private void unregisterSkyReceiver() {
        try {
            if (skyReceiver != null)
                this.unregisterReceiver(skyReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 锁定方向
     */
    private void lockRotation() {
        this.rv.setRotationLocked(true);
    }

    /**
     * 取消锁定方向
     */
    private void unlockRotation() {
        if (this.isRotationLocked) {
            this.rv.setRotationLocked(true);
        } else {
            this.rv.setRotationLocked(false);
        }
    }

    private void rotationPressed() {
        isRotationLocked = !isRotationLocked;
        if (isRotationLocked) {
            rv.setRotationLocked(true);
        } else {
            rv.setRotationLocked(false);
        }
    }

    boolean isPagesHidden = false;

    private void hidePages() {
        this.pageIndexLabel.setVisibility(View.INVISIBLE);
        this.pageIndexLabel.setVisibility(View.GONE);
        if (!this.isPortrait() && this.isDoublePagedForLandscape) {
            this.secondaryIndexLabel.setVisibility(View.INVISIBLE);
            this.secondaryIndexLabel.setVisibility(View.GONE);
        }
        rv.hidePages();
        rv.setVisibility(View.INVISIBLE);
    }

    private void showPages() {
        this.pageIndexLabel.setVisibility(View.VISIBLE);
        if (!this.isPortrait() && this.isDoublePagedForLandscape) {
            this.secondaryIndexLabel.setVisibility(View.VISIBLE);
        }
        rv.showPages();
        rv.setVisibility(View.VISIBLE);
    }


    private String getPageText() {
        String text = "";
        int si = rv.getStartIndexInPage();
        int ei = rv.getEndIndexInPage();

        int max = Math.max(si, ei);
        int min = Math.min(si, ei);

        for (int i = min; i <= max; i++) {
            String name = rv.getNodeNameByUniqueIndex(i);
            if (name.equalsIgnoreCase("sky")) {
                String nt = rv.getNodeTextByUniqueIndex(i);
                text = nt + "\r\n";
            }
        }
        return text;
    }

    private int getNumberOfPagesForChapter(int chapterIndex) {
        PagingInformation pga = rv.makePagingInformation(chapterIndex);
        PagingInformation pgi = sd.fetchPagingInformation(pga);
        if (pgi != null) return pgi.numberOfPagesInChapter;
        else return -1;
    }


    int getRealFontSize(int fontSizeIndex) {
        int rs = 0;
        switch (fontSizeIndex) {
            case 0:
                rs = 24;
                break;
            case 1:
                rs = 27;
                break;
            case 2:
                rs = 30;
                break;
            case 3:
                rs = 34;
                break;
            case 4:
                rs = 37;
                break;
            default:
                rs = 27;
        }
        if (this.getOSVersion() >= 19) {
            rs = (int) ((double) rs * 0.75f);
        }

        if (Build.DEVICE.contains("maguro")) {
            rs = (int) ((double) rs * 0.75f);
        }

        return rs;
    }

    public int getRealLineSpace(int lineSpaceIndex) {
        int rs = -1;
        if (lineSpaceIndex == 0) {
            rs = 125;
        } else if (lineSpaceIndex == 1) {
            rs = 150;
        } else if (lineSpaceIndex == 2) {
            rs = 165;
        } else if (lineSpaceIndex == 3) {
            rs = 180;
        } else if (lineSpaceIndex == 4) {
            rs = 200;
        } else {
            this.setting.lineSpacing = 1;
            rs = 150;
        }
        return rs;
    }

    public void decreaseLineSpace() {
        if (this.setting.lineSpacing != 0) {
            this.setting.lineSpacing--;
            rv.changeLineSpacing(this.getRealLineSpace(setting.lineSpacing));
        }
    }

    public void increaseLineSpace() {
        if (this.setting.lineSpacing != 4) {
            this.setting.lineSpacing++;
            rv.changeLineSpacing(this.getRealLineSpace(setting.lineSpacing));
        }
    }


    public void fontSelected(int index) {
        CustomFont customFont = this.getCustomFont(index);
        String name = customFont.getFullName();
        if (!setting.fontName.equalsIgnoreCase(name)) {
            setting.fontName = name;
            rv.changeFont(setting.fontName, this.getRealFontSize(setting.fontSize));
        }
    }

    private void showToast(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    void hideBoxes() {
        if (isPagesHidden) this.showPages();
    }

    class KeyDelegate implements KeyListener {
        @Override
        public String getKeyForEncryptedData(String uuidForContent, String contentName, String uuidForEpub) {
            // TODO 111111
//            String key = app.keyManager.getKey(uuidForContent, uuidForEpub);
            return "";
        }

        @Override
        public Book getBook() {
            return rv.getBook();
        }
    }


    class ScriptDelegate implements ScriptListener {
        @Override
        public String getScriptForChapter(int chapterIndex) {
            String customScript = null;
            return customScript;
        }

        @Override
        public String getStyleForChapter(int chapterIndex) {
            String customCSS = null;
            return customCSS;
        }
    }

    private void processPageMoved(PageInformation pi) {
        currentPageInformation = pi;

        double ppb = pi.pagePositionInBook;
        double pageDelta = ((1.0f / pi.numberOfChaptersInBook) / pi.numberOfPagesInChapter);
        int progress = (int) ((double) 999.0f * (ppb));
        int pib = pi.pageIndexInBook;

        if (rv.isGlobalPagination()) {
            if (!rv.isPaging()) {
                int cgpi = rv.getPageIndexInBookByPagePositionInBook(pi.pagePositionInBook);
                setIndexLabelsText(pi.pageIndexInBook, pi.numberOfPagesInBook);
                debug("gpi " + pi.pageIndexInBook + " cgpi " + cgpi);
            } else {
                setIndexLabelsText(-1, -1); // do not display
            }
        } else {
            setIndexLabelsText(pi.pageIndex, pi.numberOfPagesInChapter);
        }
        pagePositionInBook = (float) pi.pagePositionInBook;


        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (autoStartPlayingWhenNewChapterLoaded && isChapterJustLoaded) {
                    if (isAutoPlaying) {
                        rv.playFirstParallelInPage();
                    }
                }
                isChapterJustLoaded = false;
            }
        }, 100);

        debug(pi.pageDescription);
    }

    class PageMovedDelegate implements PageMovedListener {
        public void onPageMoved(PageInformation pi) {
            processPageMoved(pi);
        }

        public void onChapterLoaded(int chapterIndex) {
//            if (rv.isMediaOverlayAvailable() && (setting.mediaOverlay || setting.tts)) {
//                isChapterJustLoaded = true;
//            } else {
//            }
        }

        @Override
        public void onFailedToMove(boolean isFirstPage) {
            // TODO Auto-generated method stub
            if (isFirstPage) {
                showToast("This is the first page.");
            } else {
                showToast("This is the last page.");
            }
        }
    }

    public void debug(String msg) {
        if (Setting.isDebug() && msg != null) {
            Log.d(Setting.getTag(), msg);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sd.updatePosition(bookCode, pagePositionInBook);
        sd.updateSetting(setting);
        if (!this.isRotationLocked && !rv.isPlayingStarted()) {
            rotationPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if (this.isBoxesShown) {
            hideBoxes();
        } else {
            if (videoView != null) {
                ePubView.removeView(videoView);
            }
            finish();
            return;
        }
    }
}


