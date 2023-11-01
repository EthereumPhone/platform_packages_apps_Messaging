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

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.ui.conversation.ComposeMessageView;
import com.android.messaging.util.OsUtil;
import com.android.messaging.ui.PlainTextEditText;

/**
 * Chooser which allows the user to record audio
 */
class EthAddressMediaChooser extends MediaChooser implements ETHRecordView.HostInterface {
    private View mEnabledView;
    private View mMissingPermissionView;
    private PlainTextEditText inputField;
    private MediaPicker mMediaPicker;

    EthAddressMediaChooser(final MediaPicker mediaPicker, PlainTextEditText mInputField) {
        super(mediaPicker);
        System.out.println("EthAddressMediaChooser: " + mInputField + " isNull " + (mInputField == null));
        inputField = mInputField;
        mMediaPicker = mediaPicker;
    }

    @Override
    public int getSupportedMediaTypes() {
        return MediaPicker.MEDIA_TYPE_LOCATION;
    }

    @Override
    public int getIconResource() {
        return R.drawable.ic_send_eth;
    }

    @Override
    public int getIconDescriptionResource() {
        return R.string.mediapicker_audioChooserDescription;
    }

    @Override
    public void onAddressPressed(String item) {
        System.out.println("onAddressPressed: " + item + " " + inputField);
        //mComposeMessageView.changeTextMessage(item);
        inputField.setText(item);
        mMediaPicker.dismiss(true);
    }

    @Override
    protected View createView(final ViewGroup container) {
        final LayoutInflater inflater = getLayoutInflater();
        final ETHRecordView view = (ETHRecordView) inflater.inflate(
                R.layout.mediapicker_eth_chooser,
                container /* root */,
                false /* attachToRoot */);
        view.setHostInterface(this);
        view.setThemeColor(mMediaPicker.getConversationThemeColor());
        return view;
    }

    @Override
    int getActionBarTitleResId() {
        return R.string.mediapicker_audio_title;
    }

}
