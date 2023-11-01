/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.messaging.ui.mediapicker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Method;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.DraftMessageData.DraftMessageSubscriptionDataProvider;
import com.android.messaging.datamodel.data.MediaPickerMessagePartData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.ui.mediapicker.camerafocus.PieItem.OnClickListener;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.MediaUtil;
import com.android.messaging.util.MediaUtil.OnCompletionListener;
import com.android.messaging.util.SafeAsyncTask;
import com.android.messaging.util.ThreadUtil;
import com.android.messaging.util.UiUtils;
import com.google.common.annotations.VisibleForTesting;

/**
 * Hosts an audio recorder with tap and hold to record functionality.
 */
public class ETHRecordView extends FrameLayout {
    /**
     * An interface that communicates with the hosted ETHRecordView.
     */
    public interface HostInterface {
        void onAddressPressed(final String address);
    }

    private final Object walletManager;
    private Class walletProxy;
    private Method getAddress;
    private Method createSession;
    private Method hasBeenFulfilled;

    /** The initial state, the user may press and hold to start recording */
    private static final int MODE_IDLE = 1;

    /** The user has pressed the record button and we are playing the sound indicating the
     *  start of recording session. Don't record yet since we don't want the beeping sound
     *  to get into the recording. */
    private static final int MODE_STARTING = 2;

    /** When the user is actively recording */
    private static final int MODE_RECORDING = 3;

    /** When the user has finished recording, we need to record for some additional time. */
    private static final int MODE_STOPPING = 4;

    // Bug: 16020175: The framework's MediaRecorder would cut off the ending portion of the
    // recorded audio by about half a second. To mitigate this issue, we continue the recording
    // for some extra time before stopping it.
    private static final int AUDIO_RECORD_ENDING_BUFFER_MILLIS = 500;

    /**
     * The minimum duration of any recording. Below this threshold, it will be treated as if the
     * user clicked the record button and inform the user to tap and hold to record.
     */
    private static final int AUDIO_RECORD_MINIMUM_DURATION_MILLIS = 300;

    // For accessibility, the touchable record button is bigger than the record button visual.
    private ImageView mRecordButtonVisual;
    private View mRecordButton;
    private SoundLevels mSoundLevels;
    private TextView mHintTextView;
    private PausableChronometer mTimerTextView;
    private LevelTrackingMediaRecorder mMediaRecorder;
    private long mAudioRecordStartTimeMillis;

    private int mCurrentMode = MODE_IDLE;
    private HostInterface mHostInterface;
    private int mThemeColor;

    public ETHRecordView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        walletManager = context.getSystemService("wallet");
        try {
            walletProxy = Class.forName("android.os.WalletProxy");
        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
            walletProxy = null;
        }
        getAddress = walletProxy.getDeclaredMethods()[3];
        createSession = walletProxy.getDeclaredMethods()[2];
        hasBeenFulfilled = walletProxy.getDeclaredMethods()[6];
    }

    public void setHostInterface(final HostInterface hostInterface) {
        mHostInterface = hostInterface;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRecordButton = findViewById(R.id.record_button_visual);
        mRecordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                System.out.println("ETHRecordView: onClick");
                
                mHostInterface.onAddressPressed("My address is: " + getEthAddr());
            }
        });
    }

    private String getEthAddr() {
        try {
            String sessionId = (String) createSession.invoke(walletManager);
            String requestId = (String) getAddress.invoke(walletManager, sessionId);

            Thread.sleep(100);

            while (hasBeenFulfilled.invoke(walletManager, requestId).equals("notfulfilled")) {
            }

            String ethAddress = (String) hasBeenFulfilled.invoke(walletManager, requestId);
            return ethAddress;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean shouldHandleTouch() {
        return mCurrentMode != MODE_IDLE;
    }

    public void setThemeColor(final int color) {
        mThemeColor = color;
    }

}
