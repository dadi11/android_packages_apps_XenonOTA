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
 *
 * Created by Devil7DK for XenonHD
 *
 * Parts of this code was took from the app "RnOpenGapps" - https://github.com/hjthjthjt/RnOpenGApps
 */

package com.xenonota.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Downloader;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Func;
import com.tonyodev.fetch2.Func2;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2downloaders.OkHttpDownloader;
import com.xenonota.MainActivity;
import com.xenonota.R;
import com.xenonota.configs.AppConfig;
import com.xenonota.utils.DownloadUtils;
import com.xenonota.utils.OTAUtils;
import com.xenonota.dialogs.OpenFileDialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.OkHttpClient;

public class Fragment_Gapps extends Fragment implements FetchListener {

    @android.support.annotation.Nullable
    private Request request;
    private Fetch fetch;
    public static final String APP_FETCH_NAMESPACE = "GAppsFetch";

    View progressDialog_layout;
    android.support.v7.app.AlertDialog.Builder progressDialog_builder;
    android.support.v7.app.AlertDialog progressDialog;
    private TextView progressTextView;
    private TextView etaTextView;
    private TextView downloadSpeedTextView;
    private ProgressBar progressBar;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    String url;
    private String filename;
    private String filePath;
    private Status currentStatus;
    private int downloadID;
    private Button btn_Cancel,btn_Pause;

    public static Fragment_Gapps newInstance() {
        return new Fragment_Gapps();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gapps, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_gapps, menu);
        super.onCreateOptionsMenu(menu,menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.choose_gapps_zip:
                ChooseGappsZIP();
                return true;
            case R.id.clear_downloads_gapps:{
                ClearDownloads();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void ChooseGappsZIP(){
        OpenFileDialog dialog = new OpenFileDialog(getContext());
        dialog.setFilter("(.*).zip");
        dialog.setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
            @Override
            public void OnSelectedFile(String fileName) {
                AppConfig.persistGappsZipPath(fileName,getContext().getApplicationContext());
                Toast.makeText(getContext(), getString(R.string.gapps_set,fileName),Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void ClearDownloads(){
        LayoutInflater inflater = getLayoutInflater();
        View dLayout = inflater.inflate(R.layout.custom_alertdialog, null);
        android.support.v7.app.AlertDialog.Builder dBuilder = new android.support.v7.app.AlertDialog.Builder(getContext());
        TextView message = dLayout.findViewById(R.id.custom_message);
        Button cancel = dLayout.findViewById(R.id.custom_negative);
        Button clear = dLayout.findViewById(R.id.custom_positive);
        message.setText(R.string.clear_downloads_warning_gapps);
        cancel.setText(R.string.cancel);
        clear.setText(R.string.clear);
        dBuilder.setTitle(R.string.confirm);
        dBuilder.setView(dLayout);
        dBuilder.setCancelable(true);
        final android.support.v7.app.AlertDialog dDialog = dBuilder.create();
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    fetch = getAppFetchInstance();
                    fetch.deleteAll();
                    fetch.close();
                }catch(Exception ex){
                    ex.printStackTrace();
                }
                dDialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dDialog.dismiss();
            }
        });
        dDialog.show();
    }

    CardView do_card;
    SharedPreferences data_of_download;
    String a_v;
    String c_u;
    String v_r;
    SharedPreferences.Editor save_data;
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity act = getActivity();
        a_v = osRelease.substring(0,3);
        if(osCPU.contains("armeabi")){
            c_u="arm";
        }else if(osCPU.contains("arm64")){
            c_u="arm64";
        }else if(osCPU.contains("x86_64")){
            c_u="x86_64";
        }else if(osCPU.equals("x86")){
            c_u="x86";
        }
        v_r = AppConfig.getGappsVariant(getContext().getApplicationContext());
        if(act!=null){
            act.setTitle(R.string.app_name);
            data_of_download = act.getSharedPreferences("data", Context.MODE_PRIVATE);
            save_data = act.getSharedPreferences("data",Context.MODE_PRIVATE).edit();
            do_card = act.findViewById(R.id.state_latest);
        }
        myDeviceInfo();
        stateGApps();
        GetLatestVersion();
        do_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickDownload();
            }
        });
        setHasOptionsMenu(true);

    }

    public static boolean isInstall(Context context, String packageName){
        try {
            PackageInfo pkginfo = context.getPackageManager().getPackageInfo(packageName.trim(), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            if(pkginfo!=null){
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private void GetLatestVersion(){
        String inCPU = c_u;
        if(inCPU.equals("arm")){
            getLatest_ARM();
        }else if(inCPU.equals("arm64")){
            getLatest_ARM64();
        }else if(inCPU.equals("x86_64")){
            getLatest_x86_64();
        }else if(inCPU.equals("x86")){
            getLatest_x86();
        }
    }

    private void clickDownload(){
        final String date = data_of_download.getString("date","");
        v_r = AppConfig.getGappsVariant(getContext().getApplicationContext());

        LayoutInflater inflater = getLayoutInflater();
        View dLayout = inflater.inflate(R.layout.custom_alertdialog, null);
        android.support.v7.app.AlertDialog.Builder dBuilder = new android.support.v7.app.AlertDialog.Builder(getContext());
        TextView message = dLayout.findViewById(R.id.custom_message);
        Button cancel = dLayout.findViewById(R.id.custom_negative);
        Button download = dLayout.findViewById(R.id.custom_positive);
        message.setText(getString(R.string.download_confirm_1)+a_v+getString(R.string.download_confirm_2)+c_u+getString(R.string.download_confirm_3)+v_r);
        cancel.setText(R.string.cancel);
        download.setText(R.string.download);
        dBuilder.setTitle(R.string.download_confirm);
        dBuilder.setView(dLayout);
        dBuilder.setCancelable(true);
        final android.support.v7.app.AlertDialog dDialog = dBuilder.create();
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                url = "https://github.com/opengapps/"+c_u+"/releases/download/"+date+"/open_gapps-"+c_u+"-"+a_v+"-"+v_r+"-"+date+".zip";
                filename = url.substring(url.lastIndexOf('/') + 1);
                filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + filename;
                downloadGapps();
                dDialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dDialog.dismiss();
            }
        });
        dDialog.show();

    }

    public void stateGApps(){
        boolean stateofGApps = isInstall(getActivity().getApplicationContext(),"com.google.android.gms");
        ImageView status_icon = getActivity().findViewById(R.id.status_icon);
        FrameLayout status_container = getActivity().findViewById(R.id.status_container);
        TextView status_text = getActivity().findViewById(R.id.status_text);
        if(stateofGApps){
            status_icon.setImageResource(R.drawable.ic_check_circle);
            status_container.setBackgroundResource(R.color.card_green);
            status_text.setText(R.string.gapps_ok);
            status_text.setTextColor(this.getResources().getColor(R.color.card_green,null));
        }else{
            status_icon.setImageResource(R.drawable.ic_warning);
            status_container.setBackgroundResource(R.color.warning);
            status_text.setText(R.string.gapps_warning);
            status_text.setTextColor(this.getResources().getColor(R.color.warning));
        }
    }

    String osRelease = android.os.Build.VERSION.RELEASE;
    String osModel = Build.MODEL;
    String osCPU = Build.SUPPORTED_ABIS[0];
    int osSDK = android.os.Build.VERSION.SDK_INT;
    public void myDeviceInfo(){
        TextView myDeviceModelText = getActivity().findViewById(R.id.my_device_model);
        TextView myDeviceOS = getActivity().findViewById(R.id.my_device_sdk);
        TextView myDeviceCPU = getActivity().findViewById(R.id.my_device_cpu);
        TextView status_latest = getActivity().findViewById(R.id.status_latest);
        myDeviceModelText.setText(osModel);
        myDeviceCPU.setText(c_u);
        myDeviceOS.setText("Android "+osRelease+" (API "+osSDK+")");
        status_latest.setText(R.string.loading);
    }


    public int progress = 0;

    public String arm64;
    public void getLatest_ARM64(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("https://api.github.com/repos/opengapps/arm64/releases/latest");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) !=null){
                        response.append(line);
                    }
                    arm64 = response.toString();
                    JSONObject json = new JSONObject(arm64);
                    String version = json.getString("tag_name");
                    save_data.putString("date",version).apply();
                    progress = 100;
                    TextView status_latest = getActivity().findViewById(R.id.status_latest);
                    status_latest.setText(getString(R.string.latest_release)+" ARM64: "+version);
                    ProgressBar progressBar = getActivity().findViewById(R.id.progress_bar);
                    if(progress ==100){
                        progressBar.setVisibility(View.INVISIBLE);
                        ImageView status_github = getActivity().findViewById(R.id.status_github);
                        status_github.setImageResource(R.drawable.ic_github);
                        status_github.setVisibility(View.VISIBLE);
                        do_card.setClickable(true);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(reader!=null){
                        try{
                            reader.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    if(connection!=null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    public String arm;
    public void getLatest_ARM(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("https://api.github.com/repos/opengapps/arm/releases/latest");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) !=null){
                        response.append(line);
                    }
                    arm = response.toString();
                    JSONObject json = new JSONObject(arm);
                    String version = json.getString("tag_name");
                    save_data.putString("date",version).apply();
                    progress = 100;
                    TextView status_latest = getActivity().findViewById(R.id.status_latest);
                    status_latest.setText(getString(R.string.latest_release)+" ARM: "+version);
                    ProgressBar progressBar = getActivity().findViewById(R.id.progress_bar);
                    if(progress ==100){
                        progressBar.setVisibility(View.INVISIBLE);
                        ImageView status_github = getActivity().findViewById(R.id.status_github);
                        status_github.setImageResource(R.drawable.ic_github);
                        status_github.setVisibility(View.VISIBLE);
                        do_card.setClickable(true);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(reader!=null){
                        try{
                            reader.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    if(connection!=null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    public String x86;
    public void getLatest_x86(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("https://api.github.com/repos/opengapps/x86/releases/latest");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) !=null){
                        response.append(line);
                    }
                    x86 = response.toString();
                    JSONObject json = new JSONObject(x86);
                    String version = json.getString("tag_name");
                    save_data.putString("date",version).apply();
                    progress = 100;
                    TextView status_latest = getActivity().findViewById(R.id.status_latest);
                    status_latest.setText(getString(R.string.latest_release)+" x86: "+version);
                    ProgressBar progressBar = getActivity().findViewById(R.id.progress_bar);
                    if(progress ==100){
                        progressBar.setVisibility(View.INVISIBLE);
                        ImageView status_github = getActivity().findViewById(R.id.status_github);
                        status_github.setImageResource(R.drawable.ic_github);
                        status_github.setVisibility(View.VISIBLE);
                        do_card.setClickable(true);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(reader!=null){
                        try{
                            reader.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    if(connection!=null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    public String x86_64;
    public void getLatest_x86_64(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try{
                    URL url = new URL("https://api.github.com/repos/opengapps/arm/releases/latest");
                    connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) !=null){
                        response.append(line);
                    }
                    x86_64 = response.toString();
                    JSONObject json = new JSONObject(x86_64);
                    String version = json.getString("tag_name");
                    save_data.putString("date",version).apply();
                    progress = 100;
                    TextView status_latest = getActivity().findViewById(R.id.status_latest);
                    status_latest.setText(getString(R.string.latest_release)+" x86_64: "+version);
                    ProgressBar progressBar = getActivity().findViewById(R.id.progress_bar);
                    if(progress ==100){
                        progressBar.setVisibility(View.INVISIBLE);
                        ImageView status_github = getActivity().findViewById(R.id.status_github);
                        status_github.setImageResource(R.drawable.ic_github);
                        status_github.setVisibility(View.VISIBLE);
                        do_card.setClickable(true);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(reader!=null){
                        try{
                            reader.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    if(connection!=null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void downloadGapps(){
            File file = new File(filePath);
            fetch = getAppFetchInstance();

            if(file.exists()){
                fetch.removeAllWithStatus(Status.CANCELLED);
                fetch.removeAllWithStatus(Status.FAILED);
                fetch.removeAllWithStatus(Status.COMPLETED);
            }
            else{
                fetch.removeAll();
            }

            LayoutInflater inflater = getLayoutInflater();
            progressDialog_layout = inflater.inflate(R.layout.download_item, null);
            progressTextView = progressDialog_layout.findViewById(R.id.progress_TextView);
            etaTextView = progressDialog_layout.findViewById(R.id.remaining_TextView);
            downloadSpeedTextView = progressDialog_layout.findViewById(R.id.downloadSpeedTextView);
            progressBar = progressDialog_layout.findViewById(R.id.progressBar);
            btn_Cancel = progressDialog_layout.findViewById(R.id.btn_cancel);
            btn_Pause = progressDialog_layout.findViewById(R.id.btn_pause);

        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorPrimaryDark,null),android.graphics.PorterDuff.Mode.MULTIPLY);

            btn_Cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelDownload();
                }
            });
            btn_Pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pauseDownload();
                }
            });

            progressDialog_builder = new android.support.v7.app.AlertDialog.Builder(getContext());
            progressDialog_builder.setTitle(R.string.downloader_title);
            progressDialog_builder.setView(progressDialog_layout);
            progressDialog_builder.setCancelable(false);
            progressDialog = progressDialog_builder.create();
            progressDialog.show();

            request = new Request(url, filePath);
            fetch.getDownload(request.getId(), new Func2<Download>() {
                @Override
                public void call(@Nullable Download download) {
                    if (download == null) {
                        fetch.enqueue(request, new Func<Download>() {
                            @Override
                            public void call(@NotNull Download download) {
                                setProgressView(download.getStatus(), download.getProgress());
                            }
                        }, new Func<Error>() {
                            @Override
                            public void call(@NotNull Error error) {
                                OTAUtils.logInfo("Error:" + error.toString());
                            }
                        });
                    } else {
                        request = download.getRequest();
                        currentStatus = download.getStatus();
                        downloadID = download.getId();
                        setProgressView(download.getStatus(), download.getProgress());
                    }
                }
            });
            fetch.addListener(this);
            if (request != null) {
                fetch.getDownload(request.getId(), new Func2<Download>() {
                    @Override
                    public void call(@Nullable Download download) {
                        if (download != null) {
                            currentStatus = download.getStatus();
                            downloadID = download.getId();
                            setProgressView(download.getStatus(), download.getProgress());
                        }
                    }
                });
            }

            btn_Cancel.setText(R.string.cancel);
            if(currentStatus == Status.PAUSED){
                btn_Pause.setText(R.string.resume);
            }else{
                btn_Pause.setText(R.string.pause);
            }

            mNotifyManager = (NotificationManager) getContext().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(getContext().getApplicationContext(),"xenonota");
            mBuilder.setContentTitle(filename)
                    .setContentText("")
                    .setSmallIcon(R.drawable.ic_ota_available);
            Intent notificationIntent = new Intent(getContext(), MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(getContext(), 0,
                    notificationIntent, 0);
            mBuilder.setContentIntent(intent);
            mBuilder.setOngoing(true);
            mBuilder.setProgress(100, 0, true);
            mNotifyManager.notify(1, mBuilder.build());

    }

    private void cancelDownload(){
        fetch.cancel(downloadID);
    }

    private void pauseDownload(){
        if(currentStatus == Status.DOWNLOADING){
            btn_Pause.setText(R.string.resume);
            fetch.pause(downloadID);
        }else{
            btn_Pause.setText(R.string.pause);
            try{
                fetch.resume(downloadID);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private void setNotificationAlert(String title,String message){
        mBuilder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSubText(null)
                .setProgress(0,0,false)
                .setSmallIcon(R.drawable.ic_ota_notavailable);
        mBuilder.setOngoing(false);
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("NOTIFICATION_ID", 1);
        PendingIntent dismissIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(dismissIntent);
        mNotifyManager.notify(1, mBuilder.build());
    }

    private void setProgressView(@NonNull final Status status, final int progress) {
        switch (status) {
            case QUEUED: {
                break;
            }
            case PAUSED:
            case DOWNLOADING:
            case COMPLETED: {
                if (progress == -1) {
                    progressTextView.setText(R.string.downloading);
                } else {
                    final String progressString = getResources()
                            .getString(R.string.percent_progress, progress);
                    progressBar.setIndeterminate(false);
                    progressTextView.setText(progressString);
                    progressBar.setProgress(progress);
                    mBuilder.setContentText(progressString);
                    mBuilder.setProgress(100, progress, false);
                    mNotifyManager.notify(1, mBuilder.build());
                }
                break;
            }
            default: {
                progressTextView.setText("");
                progressBar.setProgress(0);
                progressBar.setIndeterminate(true);
                break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @NonNull
    public Fetch getAppFetchInstance() {
        if (fetch == null || fetch.isClosed()) {
            fetch = getNewFetchInstance(APP_FETCH_NAMESPACE);
        }
        return fetch;
    }

    @NonNull
    public Fetch getNewFetchInstance(@NonNull final String namespace) {
        final OkHttpClient client = new OkHttpClient.Builder().build();
        final Downloader okHttpDownloader = new OkHttpDownloader(client);
        final int concurrentLimit = 2;
        final boolean enableLogging = true;
        return new Fetch.Builder(getContext().getApplicationContext(), namespace)
                .setDownloader(okHttpDownloader)
                .setDownloadConcurrentLimit(concurrentLimit)
                .enableLogging(enableLogging)
                .enableRetryOnNetworkGain(true)
                .build();
    }

    private void showDownloadErrorSnackBar(Error error) {
        final Snackbar snackbar = Snackbar.make(getView(), "Download Failed: ErrorCode: "
                + error, Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (request != null) {
                    fetch.retry(request.getId());
                    snackbar.dismiss();
                }
            }
        });

        snackbar.show();
    }

    @Override
    public void onQueued(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils.getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
            mBuilder.setProgress(100, 0, true);
            mBuilder.setSubText("");
        }
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils
                    .getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
            if(progressDialog.isShowing()){progressDialog.dismiss();}
            setNotificationAlert(filename,getString(R.string.notification_completed));

            AppConfig.persistGappsZipPath(filePath,getContext().getApplicationContext());

            android.support.v7.app.AlertDialog.Builder builder;
            builder = new android.support.v7.app.AlertDialog.Builder(getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setTitle(R.string.download_complete_title)
                    .setMessage(R.string.download_complete_msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onError(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            showDownloadErrorSnackBar(download.getError());
            etaTextView.setText(DownloadUtils
                    .getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
            if(progressDialog.isShowing()){progressDialog.dismiss();}
            setNotificationAlert(filename,getString(R.string.notification_error));

            android.support.v7.app.AlertDialog.Builder builder;
            builder = new android.support.v7.app.AlertDialog.Builder(getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setTitle(R.string.download_interrupted_title)
                    .setMessage(R.string.download_interrupted_msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            String eta = DownloadUtils.getETAString(getContext(), etaInMilliseconds);
            String speed = DownloadUtils.getDownloadSpeedString(getContext(), downloadedBytesPerSecond);
            etaTextView.setText(eta);
            downloadSpeedTextView.setText(speed);
            mBuilder.setSubText(eta);
            setProgressView(download.getStatus(), download.getProgress());
        }
    }

    @Override
    public void onPaused(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils
                    .getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
            setProgressView(download.getStatus(), download.getProgress());
            mBuilder.setContentTitle(filename)
                    .setSubText("")
                    .setContentText(getString(R.string.notification_paused));
            mNotifyManager.notify(1, mBuilder.build());
        }
    }

    @Override
    public void onResumed(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils
                    .getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
            mBuilder.setContentTitle(filename);
        }
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils
                    .getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
            if(progressDialog.isShowing()){progressDialog.dismiss();}
            setNotificationAlert(filename,getString(R.string.notification_cancelled));

            android.support.v7.app.AlertDialog.Builder builder;
            builder = new android.support.v7.app.AlertDialog.Builder(getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setTitle(R.string.download_cancelled_title)
                    .setMessage(R.string.download_cancelled_msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils
                    .getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
        }
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils
                    .getETAString(getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(getContext(), 0));
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
