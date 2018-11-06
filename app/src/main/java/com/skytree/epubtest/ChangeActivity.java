package com.skytree.epubtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.skytree.epubtest.simpledemo.MainSimpleActivity;

/**
 * Created by zyt on 2018/9/4.
 */

public class ChangeActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);
        findViewById(R.id.change_Button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ChangeActivity.this, HomeActivity.class);
                intent.putExtra("skipType", 1);
                startActivity(intent);
            }
        });
        findViewById(R.id.change_Button01).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ChangeActivity.this, HomeActivity.class);
                intent.putExtra("skipType", 2);
                startActivity(intent);
            }
        });
        findViewById(R.id.change_Button02).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(ChangeActivity.this, MainSimpleActivity.class);
                startActivity(intent);
            }
        });
    }
}
