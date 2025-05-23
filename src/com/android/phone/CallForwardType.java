/* Copyright (c) 2015, 2017-2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
* Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
* Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
* SPDX-License-Identifier: BSD-3-Clause-Clear
*/

package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.MenuItem;

import com.android.ims.FeatureConnector;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;

public class CallForwardType extends PreferenceActivity {
    private static final String LOG_TAG = "CallForwardType";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String BUTTON_CF_KEY_VOICE = "button_cf_key_voice";
    private static final String BUTTON_CF_KEY_VIDEO = "button_cf_key_video";

    private Preference mVoicePreference;
    private Preference mVideoPreference;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private boolean mIsUtCapable = false;
    private boolean mIsVtCapable = false;
    boolean mHideVtCfOption = false;
    private FeatureConnector<ImsManager> mFeatureConnector;
    private int mPhoneId;
    private IntentFilter mIntentFilter;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "onReceive intent : " + intent);
            String action = intent.getAction();
            int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                    SubscriptionManager.INVALID_PHONE_INDEX);
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                if (phoneId == mPhoneId &&
                        IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(
                            intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE))) {
                    Log.d(LOG_TAG, "onSimAbsent, exit");
                    finish();
                }
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                if (mPhone != null) {
                    setPreferencesState(PhoneUtils.isSuppServiceAllowedInAirplaneMode(mPhone));
                }
            }
        }
     };

    private void setPreferencesState(boolean state) {
        if (mVoicePreference != null && findPreference(BUTTON_CF_KEY_VOICE) != null) {
            mVoicePreference.setEnabled(state);
        }
        if (mVideoPreference != null && findPreference(BUTTON_CF_KEY_VIDEO) != null) {
            mVideoPreference.setEnabled(state);
        }
    }

    private ImsMmTelManager.CapabilityCallback mCapabilityCallback =
        new ImsMmTelManager.CapabilityCallback() {
            @Override
            public void onCapabilitiesStatusChanged(MmTelFeature.MmTelCapabilities capabilities) {
                    boolean isUtCapable = capabilities.isCapable(
                            MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT);
                    boolean isVtCapable = capabilities.isCapable(
                            MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO);
                    if (isUtCapable ==  mIsUtCapable && isVtCapable == mIsVtCapable) {
                        return;
                    }
                    mIsUtCapable = isUtCapable;
                    mIsVtCapable = isVtCapable;
                    showVideoOption(mIsUtCapable && mIsVtCapable && !mHideVtCfOption);
            }
    };

    private void setListeners() throws ImsException {
        ImsManager imsMgr = ImsManager.getInstance(mPhone.getContext(), mPhone.getPhoneId());
        imsMgr.addCapabilitiesCallback(mCapabilityCallback, mPhone.getContext().getMainExecutor());
    }

    private void removeListeners() {
        ImsManager imsMgr = ImsManager.getInstance(mPhone.getContext(), mPhone.getPhoneId());
        imsMgr.removeCapabilitiesCallback(mCapabilityCallback);
    }

    private void showVideoOption(boolean show) {
        if (!show && mVideoPreference != null && findPreference(BUTTON_CF_KEY_VIDEO) != null) {
            Log.d(LOG_TAG, "remove video option");
            getPreferenceScreen().removePreference(mVideoPreference);
        } else if (show && mVideoPreference != null) {
            Log.d(LOG_TAG, "enable video option");
            getPreferenceScreen().addPreference(mVideoPreference);
            if (mPhone != null) {
                mVideoPreference.setEnabled(PhoneUtils.isSuppServiceAllowedInAirplaneMode(mPhone));
            }
        }
    }
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(LOG_TAG, "onCreate..");
        /*Loading CallForward Setting page*/
        addPreferencesFromResource(R.xml.call_forward_type);
        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mPhone = mSubscriptionInfoHelper.getPhone();
        mPhoneId = mPhone.getPhoneId();
        mIsUtCapable = mPhone.isUtEnabled();
        mIsVtCapable = mPhone.isVideoEnabled();
        mFeatureConnector = ImsManager.getConnector(
            mPhone.getContext(), mPhone.getPhoneId(), LOG_TAG,
            new FeatureConnector.Listener<ImsManager>() {
                @Override
                public void connectionReady(ImsManager manager, int subId) throws ImsException {
                    Log.d(LOG_TAG, "ImsManager: connection ready.");
                    setListeners();
                }

                @Override
                public void connectionUnavailable(int reason) {
                    Log.d(LOG_TAG, "ImsManager: connection unavailable.");
                    removeListeners();
                }
            }, mPhone.getContext().getMainExecutor());

        /*Voice Button*/
        mVoicePreference = (Preference) findPreference(BUTTON_CF_KEY_VOICE);
        mVoicePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            /*onClicking Voice Button*/
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
                Log.d(LOG_TAG, "Voice button clicked!");
                intent.putExtra(PhoneUtils.SERVICE_CLASS,
                        CommandsInterface.SERVICE_CLASS_VOICE);
                startActivity(intent);
                return true;
            }
        });

        /*Video Button*/
        mVideoPreference = (Preference) findPreference(BUTTON_CF_KEY_VIDEO);
        mVideoPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            /*onClicking Video Button*/
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
                Log.d(LOG_TAG, "Video button clicked!");
                intent.putExtra(PhoneUtils.SERVICE_CLASS,
                        (CommandsInterface.SERVICE_CLASS_DATA_SYNC +
                        CommandsInterface.SERVICE_CLASS_PACKET));
                startActivity(intent);
                return true;
            }
        });
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        CarrierConfigManager cfgManager = (CarrierConfigManager)
                mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (cfgManager != null) {
            mHideVtCfOption = cfgManager.getConfigForSubId(mPhone.getSubId())
                .getBoolean("config_hide_vt_callforward_option");
        }
        /*set preference state base on whether supplementary service is allowed*/
        setPreferencesState(PhoneUtils.isSuppServiceAllowedInAirplaneMode(mPhone));
        registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFeatureConnector.connect();

        if (mHideVtCfOption || !(mPhone.isUtEnabled() && mPhone.isVideoEnabled())) {
            Log.d(LOG_TAG, "VT or/and Ut Service is not enabled");
            showVideoOption(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFeatureConnector.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
