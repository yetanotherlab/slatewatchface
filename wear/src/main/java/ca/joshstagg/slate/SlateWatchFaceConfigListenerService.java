package ca.joshstagg.slate;

/**
 * Slate ca.joshstagg.slate
 * Copyright 2015  Josh Stagg
 */

/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * A {@link WearableListenerService} listening for {@link ca.joshstagg.slate.SlateWatchFaceService} config messages
 * and updating the config {@link com.google.android.gms.wearable.DataItem} accordingly.
 */
public class SlateWatchFaceConfigListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "SlateListenerService";

    private GoogleApiClient mGoogleApiClient;

    @Override // WearableListenerService
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!messageEvent.getPath().equals(SlateWatchFaceUtil.PATH_WITH_FEATURE)) {
            return;
        }
        byte[] rawData = messageEvent.getData();
        // It's allowed that the message carries only some of the keys used in the config DataItem
        // and skips the ones that we don't want to change.
        DataMap configKeysToOverwrite = DataMap.fromByteArray(rawData);
        Log.d(TAG, "Received watch face config message: " + configKeysToOverwrite);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(Wearable.API).build();
        }
        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult =
                    mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                return;
            }
        }

        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        for (String key : configKeysToOverwrite.keySet()) {
            switch(key) {
                case SlateWatchFaceUtil.KEY_SECONDS_COLOR:
                    prefs.putString(key, configKeysToOverwrite.getString(key));
                    break;
                case SlateWatchFaceUtil.KEY_SHOW_DATE:
                case SlateWatchFaceUtil.KEY_SMOOTH_MODE:
                    prefs.putBoolean(key, configKeysToOverwrite.getBoolean(key));
                    break;
            }
        }
        prefs.commit();
        SlateWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
    }
}
