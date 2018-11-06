package com.skytree.epubtest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;

public class SkySetting {
	public int bookCode;
	public String fontName;
	public int fontSize;
	public int lineSpacing;
	public int foreground;
    public int background;
    public int theme;
    public double brightness;
    public int transitionType;	
    public boolean lockRotation;
    public boolean doublePaged;
    public boolean allow3G;
    public boolean globalPagination;
    
    public boolean mediaOverlay;
    public boolean tts;
    public boolean autoStartPlaying;
    public boolean autoLoadNewChapter;
    public boolean highlightTextToVoice;
    
    public static String storageDirectory=null;
    
    public static String getStorageDirectory() { 
    	return storageDirectory;    	
    }
    
    public static void setStorageDirectory(String directory,String appName) {
    	storageDirectory = directory+"/"+appName;
    }
}

class SkyPermission {
	public static final int PERMISSION_GRANTED = 0;
	public static final int REQUEST_CODE = 2099;
	
	public static final int GRANTED = 0;
	public static final int DENIED = 1;
	public static final int DENIED_FOREVER = 2;
	
	String permission = "";
	int state = SkyPermission.DENIED;
	
	public SkyPermission(String permission) {
		this.permission = permission;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public boolean isGranted() {
		boolean ret = (this.state==SkyPermission.GRANTED);
		return ret;
	}
	
	public boolean isWriteSettingsPermission() {
		if (this.permission.equalsIgnoreCase(Manifest.permission.WRITE_SETTINGS)) {
			return true;
		} else {
			return false;
		}
	}
}

class SkyPermissions {
	ArrayList<SkyPermission> skyPermissions = new ArrayList<SkyPermission>();
	
	public void add(SkyPermission permission) {
		skyPermissions.add(permission);
	}
	
	public boolean isGranted(String permission) {
		for (SkyPermission skyPermission : skyPermissions) {
			if (skyPermission.permission.equalsIgnoreCase(permission)) {
				return skyPermission.isGranted();
			}
		}
		return false;
	}
	
	public boolean isAllGranted() {
		boolean res = true;
		for (SkyPermission skyPermission : skyPermissions) {
			if (!skyPermission.isGranted()) {
				res = false;
				break;
			}
		}
		return res;
	}
	
	public SkyPermission get(int i) {
		return skyPermissions.get(i);
	}
	
	public SkyPermission get(String permission) {
		for (SkyPermission skyPermission : skyPermissions) {
			if (skyPermission.permission.equalsIgnoreCase(permission)) return skyPermission;
		}
		return null;
	}
	
	public void checkPermissions(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
		for (SkyPermission skyPermission : skyPermissions) {
			if (ActivityCompat.checkSelfPermission(context, skyPermission.permission) != SkyPermission.PERMISSION_GRANTED) {
				// 현재 시점에서는 영원히 금지되었는지의 여부를 알 수가 없다.
				skyPermission.setState(SkyPermission.DENIED);
			} else {
				skyPermission.setState(SkyPermission.GRANTED);
			}
		}
	}
	
	private String[] toRequestArray() {
		ArrayList<SkyPermission> ngps = new ArrayList<SkyPermission>();
		for (int i = 0; i < skyPermissions.size(); i++) {
			SkyPermission skyPermission = this.get(i);
			if (skyPermission.state!=SkyPermission.GRANTED) {
				ngps.add(skyPermission);
			}
		}
		String[] array = new String[ngps.size()];
		for (int i = 0; i < ngps.size(); i++) {
			SkyPermission skyPermission = ngps.get(i);
			array[i] = skyPermission.permission;
		}
		return array;
	}
	
	public void requestPermissions(Activity activity) {
		String[] ps = this.toRequestArray();
		if (ps.length!=0) {
			ActivityCompat.requestPermissions(activity, ps, SkyPermission.REQUEST_CODE);
		}
	}
}


