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
 */

package com.xenonota.tasks;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.xenonota.configs.OTAConfig;
import com.xenonota.configs.OTAVersion;
import com.xenonota.dialogs.WaitDialogHandler;
import com.xenonota.fragments.Fragment_OTA;
import com.xenonota.utils.DownloadUtils;
import com.xenonota.utils.OTAUtils;
import com.xenonota.xml.OTADevice;
import com.xenonota.xml.OTAParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.OkHttpClient;

public class MagiskDownloadTask extends AsyncTask<Context, Void, String> implements FetchListener {

    @android.support.annotation.Nullable
    private Request request;
    private Fetch fetch;
    public static final String APP_FETCH_NAMESPACE = "MagiskFetch";

    View progressDialog_layout;
    AlertDialog.Builder progressDialog_builder;
    AlertDialog progressDialog;
    private TextView progressTextView;
    private TextView etaTextView;
    private TextView downloadSpeedTextView;
    private ProgressBar progressBar;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private String filename;
    private String filePath;
    private com.tonyodev.fetch2.Status currentStatus;
    private int downloadID;
    private Button btn_Cancel,btn_Pause;

    private static MagiskDownloadTask mInstance = null;
    private final Handler mHandler = new WaitDialogHandler();
    private Context mContext;
    private boolean mIsBackgroundThread;

    private String JSON_URL = "https://raw.githubusercontent.com/topjohnwu/MagiskManager/update/stable.json";

    private Fragment_OTA frag;

    private MagiskDownloadTask(boolean isBackgroundThread) {
        this.mIsBackgroundThread = isBackgroundThread;
    }

    public static MagiskDownloadTask getInstance(boolean isBackgroundThread, Fragment_OTA frag) {
        if (mInstance == null) {
            mInstance = new MagiskDownloadTask(isBackgroundThread);
        }
        mInstance.frag = frag;
        return mInstance;
    }

    private static boolean isConnectivityAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    @Override
    protected String doInBackground(Context... params) {
        mContext = params[0];

        if (!isConnectivityAvailable(mContext)) {
            return null;
        }

        String url = "";

        showWaitDialog();

        try {
            JSONObject json = new JSONObject(readURL(JSON_URL));
            JSONObject magisk = json.getJSONObject("magisk");
            url = magisk.getString("link");
            filename = "Magisk-v" + magisk.getString("version") + ".zip";
        } catch (Exception ex) {ex.printStackTrace();}

        return url;
    }

    private String readURL(String url){
        String re="";
        try{
            InputStream inputStream = OTAUtils.downloadURL(url);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            re=total.toString();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return re;
    }

    @Override
    protected void onPostExecute(String url) {
        super.onPostExecute(url);

        if (url.equals("")) {
            showToast(R.string.magisk_error);
        } else {
            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + filename;
            downloadMagisk(url);
        }

        hideWaitDialog();

        mInstance = null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mInstance = null;
    }

    private void showWaitDialog() {
        if (!mIsBackgroundThread) {
            Message msg = mHandler.obtainMessage(WaitDialogHandler.MSG_SHOW_DIALOG);
            msg.obj = mContext;
            msg.arg1 = R.string.magisk_check;
            mHandler.sendMessage(msg);
        }
    }

    private void hideWaitDialog() {
        if (!mIsBackgroundThread) {
            Message msg = mHandler.obtainMessage(WaitDialogHandler.MSG_CLOSE_DIALOG);
            mHandler.sendMessage(msg);
        }
    }

    private void showToast(int messageId) {
        if (!mIsBackgroundThread) {
            OTAUtils.toast(messageId, mContext);
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void downloadMagisk(String url){
            File file = new File(filePath);
            fetch = getAppFetchInstance();

            if(file.exists()){
                fetch.removeAllWithStatus(com.tonyodev.fetch2.Status.CANCELLED);
                fetch.removeAllWithStatus(com.tonyodev.fetch2.Status.FAILED);
                fetch.removeAllWithStatus(com.tonyodev.fetch2.Status.COMPLETED);
            }
            else{
                fetch.removeAll();
            }

            LayoutInflater inflater = frag.getLayoutInflater();
            progressDialog_layout = inflater.inflate(R.layout.download_item, null);
            progressTextView = progressDialog_layout.findViewById(R.id.progress_TextView);
            etaTextView = progressDialog_layout.findViewById(R.id.remaining_TextView);
            downloadSpeedTextView = progressDialog_layout.findViewById(R.id.downloadSpeedTextView);
            progressBar = progressDialog_layout.findViewById(R.id.progressBar);
            btn_Cancel = progressDialog_layout.findViewById(R.id.btn_cancel);
            btn_Pause = progressDialog_layout.findViewById(R.id.btn_pause);

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

            progressDialog_builder = new AlertDialog.Builder(frag.getContext());
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
                                Log.d("XenonOTA", "Error:" + error.toString());
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
            if(currentStatus == com.tonyodev.fetch2.Status.PAUSED){
                btn_Pause.setText(R.string.resume);
            }else{
                btn_Pause.setText(R.string.pause);
            }

            mNotifyManager = (NotificationManager) frag.getContext().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(frag.getContext().getApplicationContext(),"xenonota");
            mBuilder.setContentTitle(filename)
                    .setContentText("")
                    .setSmallIcon(R.drawable.ic_ota_available);
            Intent notificationIntent = new Intent(frag.getContext(), MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(frag.getContext(), 0,
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
        if(currentStatus == com.tonyodev.fetch2.Status.DOWNLOADING){
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
        Intent intent = new Intent(frag.getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("NOTIFICATION_ID", 1);
        PendingIntent dismissIntent = PendingIntent.getActivity(frag.getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(dismissIntent);
        mNotifyManager.notify(1, mBuilder.build());
    }

    private void setProgressView(@NonNull final com.tonyodev.fetch2.Status status, final int progress) {
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
                    final String progressString = frag.getResources()
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
        return new Fetch.Builder(frag.getContext().getApplicationContext(), namespace)
                .setDownloader(okHttpDownloader)
                .setDownloadConcurrentLimit(concurrentLimit)
                .enableLogging(enableLogging)
                .enableRetryOnNetworkGain(true)
                .build();
    }

    @Override
    public void onQueued(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils.getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
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
                    .getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
            if(progressDialog.isShowing()){progressDialog.dismiss();}
            setNotificationAlert(filename,frag.getString(R.string.notification_completed));

            AppConfig.persistMagiskZipPath(filePath,frag.getContext().getApplicationContext());

            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(frag.getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setTitle(R.string.download_complete_title)
                    .setMessage(R.string.magisk_download_completed)
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
            etaTextView.setText(DownloadUtils
                    .getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
            if(progressDialog.isShowing()){progressDialog.dismiss();}
            setNotificationAlert(filename,frag.getString(R.string.notification_error));

            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(frag.getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
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
            String eta = DownloadUtils.getETAString(frag.getContext(), etaInMilliseconds);
            String speed = DownloadUtils.getDownloadSpeedString(frag.getContext(), downloadedBytesPerSecond);
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
                    .getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
            setProgressView(download.getStatus(), download.getProgress());
            mBuilder.setContentTitle(filename)
                    .setSubText("")
                    .setContentText(frag.getString(R.string.notification_paused));
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
                    .getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
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
                    .getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
            if(progressDialog.isShowing()){progressDialog.dismiss();}
            setNotificationAlert(filename,frag.getString(R.string.notification_cancelled));

            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(frag.getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
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
                    .getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
        }
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            downloadID = download.getId();
            currentStatus = download.getStatus();
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(DownloadUtils
                    .getETAString(frag.getContext(), 0));
            downloadSpeedTextView.setText(DownloadUtils.getDownloadSpeedString(frag.getContext(), 0));
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
