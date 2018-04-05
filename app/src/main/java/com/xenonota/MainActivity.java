/**
 * Copyright (C) 2018 XenonHD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xenonota;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.xenonota.adapters.ViewPagerAdapter;
import com.xenonota.fragments.Fragment_Gapps;
import com.xenonota.fragments.Fragment_OTA;
import com.xenonota.fragments.Fragment_Settings;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView navigation;
    ViewPager viewPager;
    MenuItem prevMenuItem;

    private static final int STORAGE_PERMISSION_CODE = 200;

    Fragment_OTA fragment_ota;
    Fragment_Gapps fragment_gapps;
    Fragment_Settings fragment_settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        checkStoragePermissions();
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        viewPager = findViewById(R.id.viewpager);
        viewPager.addOnPageChangeListener(mOnPageChangeListener);
        setupViewPager(viewPager);

        con = MainActivity.this;
        act = MainActivity.this;

        initChannels(getApplicationContext());
    }

    public void initChannels(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("xenonota",
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setSound(null, null);
        channel.setDescription("Downloads");
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static Context con ;
    public static Context getContext(){
        return con;
    }

    private static Activity act;
    public static Activity getActivity(){return act;}

    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(MainActivity.this,  R.string.permission_not_enabled, Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (prevMenuItem != null) {
                prevMenuItem.setChecked(false);
            }
            else
            {
                navigation.getMenu().getItem(0).setChecked(false);
            }

            switch(position){
                case 0:{
                    fragment_ota.setHasOptionsMenu(true);
                    break;
                }
                case 1:{
                    fragment_gapps.setHasOptionsMenu(true);
                    break;
                }
                case 2:{
                    fragment_settings.setHasOptionsMenu(false);
                    break;
                }
            }
            invalidateOptionsMenu();

            navigation.getMenu().getItem(position).setChecked(true);
            prevMenuItem = navigation.getMenu().getItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_rom:
                    viewPager.setCurrentItem(0);
                    break;
                case R.id.navigation_gapps:
                    viewPager.setCurrentItem(1);
                    break;
                case R.id.navigation_settings:
                    viewPager.setCurrentItem(2);
                    break;
            }
            return true;
        }
    };

    private void setupViewPager(ViewPager viewPager)
    {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        fragment_ota = Fragment_OTA.newInstance();
        fragment_gapps = Fragment_Gapps.newInstance();
        fragment_settings = Fragment_Settings.newInstance();
        adapter.addFragment(fragment_ota);
        adapter.addFragment(fragment_gapps);
        adapter.addFragment(fragment_settings);
        viewPager.setAdapter(adapter);
        fragment_ota.setHasOptionsMenu(true);
        invalidateOptionsMenu();
    }

}
