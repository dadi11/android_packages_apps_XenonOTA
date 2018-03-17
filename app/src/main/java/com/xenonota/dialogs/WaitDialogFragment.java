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

package com.xenonota.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.xenonota.R;

public class WaitDialogFragment extends DialogFragment {
    private String message;
    public static WaitDialogFragment newInstance(String message) {
        WaitDialogFragment frag = new WaitDialogFragment();
        frag.message = message;
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(true);
        AlertDialog dialog = ShowProgressDialog(getContext(),"", message);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (getOTADialogListener() != null) {
            getOTADialogListener().onProgressCancelled();
        }
    }

    private OTADialogListener getOTADialogListener() {
        if (getActivity() instanceof OTADialogListener) {
            return (OTADialogListener) getActivity();
        }
        return null;
    }

    public interface OTADialogListener {
        void onProgressCancelled();
    }


    private AlertDialog ShowProgressDialog(Context context, String title, String message)
    {

        final ProgressBar progressBar =
                new ProgressBar(
                        context,
                        null,
                        android.R.attr.progressBarStyleHorizontal);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorPrimaryDark,null),android.graphics.PorterDuff.Mode.MULTIPLY);

        progressBar.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        progressBar.setIndeterminate(true);

        final LinearLayout container =
                new LinearLayout(context);

        container.addView(progressBar);

        int padding =
                getDialogPadding(context);

        container.setPadding(
                padding, (message == null ? padding : 0), padding, 0);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context).
                        setTitle(title).
                        setMessage(message).
                        setView(container);

        builder.setCancelable(false);
        return builder.create();
    }

    private int getDialogPadding(Context context)
    {
        int[] sizeAttr = new int[] { android.support.v7.appcompat.R.attr.dialogPreferredPadding };
        TypedArray a = context.obtainStyledAttributes((new TypedValue()).data, sizeAttr);
        int size = a.getDimensionPixelSize(0, -1);
        a.recycle();

        return size;
    }
}
