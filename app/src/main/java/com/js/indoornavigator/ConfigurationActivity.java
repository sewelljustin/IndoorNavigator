package com.js.indoornavigator;


import android.app.Activity;
import android.content.Context;
import android.view.View.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import android.widget.CheckBox;


public class ConfigurationActivity extends Activity {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor edit;

    // Boolean references for check boxes
    boolean highestRssi;
    boolean density1;
    boolean density2;
    boolean bayes;

    // References to check boxes
    CheckBox highestRssiCheckBox;
    CheckBox density1CheckBox;
    CheckBox density2CheckBox;
    CheckBox bayesCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_layout);

        sharedPreferences = getSharedPreferences(getString(R.string.pref_file_key), Context.MODE_PRIVATE);
        edit = sharedPreferences.edit();

        // Get boolean values from shared preferences
        highestRssi = sharedPreferences.getBoolean(getString(R.string.highestRssiKey), false);
        density1= sharedPreferences.getBoolean(getString(R.string.density1Key), false);
        density2 = sharedPreferences.getBoolean(getString(R.string.density2Key), false);
        bayes = sharedPreferences.getBoolean(getString(R.string.bayesKey), false);

        highestRssiCheckBox = (CheckBox) findViewById(R.id.highestRssiCheckBox);
        density1CheckBox = (CheckBox) findViewById(R.id.density1CheckBox);
        density2CheckBox = (CheckBox) findViewById(R.id.density2CheckBox);
        bayesCheckBox = (CheckBox) findViewById(R.id.bayesCheckBox);

        highestRssiCheckBox.setOnClickListener(highestRssiCheckBoxListener);
        density1CheckBox.setOnClickListener(density1CheckBoxListener);
        density2CheckBox.setOnClickListener(density2CheckBoxListener);
        bayesCheckBox.setOnClickListener(bayesCheckBoxListener);

    }

    public OnClickListener highestRssiCheckBoxListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setFalse();
            highestRssi = true;
            updateCheckBoxes();
        }
    };

    public OnClickListener density1CheckBoxListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setFalse();
            density1 = true;
            updateCheckBoxes();
        }
    };

    public OnClickListener density2CheckBoxListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setFalse();
            density2 = true;
            updateCheckBoxes();
        }
    };

    public OnClickListener bayesCheckBoxListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setFalse();
            bayes = true;
            updateCheckBoxes();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        updateCheckBoxes();

    }

    @Override
    protected void onPause() {
        super.onPause();

        edit.putBoolean(getString(R.string.highestRssiKey), highestRssi);
        edit.putBoolean(getString(R.string.density1Key), density1);
        edit.putBoolean(getString(R.string.density2Key), density2);
        edit.putBoolean(getString(R.string.bayesKey), bayes);
        edit.commit();
    }

    public void updateCheckBoxes() {
        highestRssiCheckBox.setChecked(highestRssi);
        density1CheckBox.setChecked(density1);
        density2CheckBox.setChecked(density2);
        bayesCheckBox.setChecked(bayes);
    }

    private void setFalse() {
        highestRssi = false;
        density1 = false;
        density2 = false;
        bayes = false;
    }
}
