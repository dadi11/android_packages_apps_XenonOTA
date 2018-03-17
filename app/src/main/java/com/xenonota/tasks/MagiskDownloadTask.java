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

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;

import com.xenonota.MainActivity;
import com.xenonota.R;
import com.xenonota.configs.AppConfig;
import com.xenonota.dialogs.WaitDialogHandler;
import com.xenonota.fragments.Fragment_OTA;
import com.xenonota.utils.OTAUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MagiskDownloadTask extends AsyncTask<Context, Void, String> {

    private String DOWNLOADER_ID = "GAPPS_DOWNLOAD";

    private static MagiskDownloadTask mInstance = null;
    private final Handler mHandler = new WaitDialogHandler();
    private Context mContext;
    private boolean mIsBackgroundThread;

    private String JSON_URL = "https://raw.githubusercontent.com/topjohnwu/MagiskManager/update/stable.json";

    private String Filename;

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
            Filename = "Magisk-v" + magisk.getString("version") + ".zip";
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

    private void downloadMagisk(String URL){
        Downloader downloader = new Downloader(MainActivity.getActivity(), URL, Filename);
        downloader.setDownloadTaskListener(new Downloader.DownloadTask() {
            @Override
            public void onDownloadCompleted(String ID, String FilePath) {
                AppConfig.persistMagiskZipPath(FilePath,frag.getContext().getApplicationContext());

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

            @Override
            public void onDownloadCanceled(String ID) {
                android.support.v7.app.AlertDialog.Builder builder;
                builder = new android.support.v7.app.AlertDialog.Builder(frag.getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
                builder.setTitle(R.string.download_cancelled_title)
                        .setMessage(R.string.download_cancelled_msg)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }

            @Override
            public void onDownloadError(String ID, String Error) {
                android.support.v7.app.AlertDialog.Builder builder;
                builder = new android.support.v7.app.AlertDialog.Builder(frag.getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
                String errMsg = frag.getString(R.string.download_interrupted_msg) + "\n\n" + Error;
                builder.setTitle(R.string.download_interrupted_title)
                        .setMessage(errMsg)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });
        downloader.execute(DOWNLOADER_ID);
    }
}
