package com.nutomic.syncthingandroid.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.syncthing.RestApi;
import com.nutomic.syncthingandroid.syncthing.SyncthingService;
import com.nutomic.syncthingandroid.syncthing.SyncthingServiceBinder;

import java.util.LinkedList;

/**
 * Connects to {@link SyncthingService} and provides access to it.
 */
public class SyncthingActivity extends ActionBarActivity implements ServiceConnection {

    private SyncthingService mSyncthingService;

    private LinkedList<OnServiceConnectedListener> mServiceConnectedListeners = new LinkedList<>();

    private AlertDialog mLoadingDialog;

    private AlertDialog mDisabledDialog;

    /**
     * To be used for Fragments.
     */
    public interface OnServiceConnectedListener {
        public void onServiceConnected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, SyncthingService.class));
        bindService(new Intent(this, SyncthingService.class),
                this, Context.BIND_AUTO_CREATE);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("loading_dialog_active"))
                showLoadingDialog();
            if (savedInstanceState.getBoolean("disabled_dialog_active"))
                showDisabledDialog();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("loading_dialog_active", mLoadingDialog != null);
        outState.putBoolean("disabled_dialog_active", mDisabledDialog != null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        SyncthingServiceBinder binder = (SyncthingServiceBinder) iBinder;
        mSyncthingService = binder.getService();
        for (OnServiceConnectedListener listener : mServiceConnectedListeners) {
            listener.onServiceConnected();
        }
        mServiceConnectedListeners.clear();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mSyncthingService = null;
    }

    /**
     * Used for Fragments to use the Activity's service connection.
     */
    public void registerOnServiceConnectedListener(OnServiceConnectedListener listener) {
        if (mSyncthingService != null) {
            listener.onServiceConnected();
        } else {
            mServiceConnectedListeners.addLast(listener);
        }
    }

    /**
     * Returns service object (or null if not bound).
     */
    public SyncthingService getService() {
        return mSyncthingService;
    }

    /**
     * Returns RestApi instance, or null if SyncthingService is not yet connected.
     */
    public RestApi getApi() {
        return (getService() != null)
                ? getService().getApi()
                : null;
    }

    public void handleLoadingDialog(SyncthingService.State currentState) {
        if (currentState != SyncthingService.State.ACTIVE && !isFinishing()) {
            if (currentState == SyncthingService.State.DISABLED) {
                if (mLoadingDialog != null) {
                    mLoadingDialog.dismiss();
                    mLoadingDialog = null;
                }
                showDisabledDialog();
            } else if (mLoadingDialog == null) {
                showLoadingDialog();
            }
            return;
        }

        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }

    public void showLoadingDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.loading_dialog, null);
        TextView loadingText = (TextView) dialogLayout.findViewById(R.id.loading_text);
        loadingText.setText((getService() != null && getService().isFirstStart())
                ? R.string.web_gui_creating_key
                : R.string.api_loading);
        mLoadingDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(dialogLayout)
                .show();
    }

    public void showDisabledDialog() {
        mDisabledDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.syncthing_disabled_title)
                .setMessage(R.string.syncthing_disabled_message)
                .setPositiveButton(R.string.syncthing_disabled_change_settings,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                                Intent intent =
                                        new Intent(SyncthingActivity.this, SettingsActivity.class);
                                intent.setAction(SettingsActivity.ACTION_APP_SETTINGS_FRAGMENT);
                                startActivity(intent);
                            }
                        }
                )
                .setNegativeButton(R.string.exit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }
                )
                .show();
        mDisabledDialog.setCancelable(false);
    }

    @Override
    public void finish() {
        // FIXME: this has no effect? (activity finished with dialog open)
        if (mLoadingDialog == null && mDisabledDialog == null)
            super.finish();
    }
}
