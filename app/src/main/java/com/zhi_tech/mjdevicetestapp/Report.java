package com.zhi_tech.mjdevicetestapp;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taipp on 5/20/2016.
 */
public class Report extends Activity {

    private SharedPreferences mSp;
    private TextView mSuccess;
    private TextView mFailed;
    private TextView mDefault;
    private List<String> mOkList;
    private List<String> mFailedList;
    private List<String> mDefaultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
        mSp = getSharedPreferences("MJDeviceTestApp", Context.MODE_PRIVATE);
        mSuccess = (TextView) findViewById(R.id.report_success);
        mSuccess.setTextColor(Color.GREEN);
        mFailed = (TextView) findViewById(R.id.report_failed);
        mFailed.setTextColor(Color.RED);
        mDefault = (TextView) findViewById(R.id.report_default);
        mDefault.setTextColor(Color.GRAY);
        mOkList = new ArrayList<String>();
        mFailedList = new ArrayList<String>();
        mDefaultList = new ArrayList<String>();

        for (int itemId : MJDeviceTestApp.itemIds) {
            if((mSp.getString(getString(itemId), null) == null)){
                continue;
            }
            if (AppDefine.DT_SUCCESS.equals(mSp.getString(getString(itemId), null))) {
                mOkList.add(getString(itemId));
            } else if (AppDefine.DT_FAILED.equals(mSp.getString(getString(itemId), null))) {
                mFailedList.add(getString(itemId));
            } else {
                mDefaultList.add(getString(itemId));
            }
        }
        ShowInfo();
    }

    protected void ShowInfo() {
        String okItem = "\n" + getString(R.string.report_ok) + "\n";
        for (int i = 0; i < mOkList.size(); i++) {
            okItem += mOkList.get(i) + " | ";
        }

        mSuccess.setText(okItem);

        String failedItem = "\n" + getString(R.string.report_failed) + "\n";
        for (int j = 0; j < mFailedList.size(); j++) {
            failedItem += mFailedList.get(j) + " | ";
        }
        mFailed.setText(failedItem);

        String defaultItem = "\n" + getString(R.string.report_notest) + "\n";
        for (int k = 0; k < mDefaultList.size(); k++) {
            defaultItem += mDefaultList.get(k) + " | ";
        }
        mDefault.setText(defaultItem);
    }
}