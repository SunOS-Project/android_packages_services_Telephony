/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.phone.testapps.satellitetestapp;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.wrapper.CarrierRoamingNtnModeListenerWrapper2;
import android.telephony.satellite.wrapper.NtnSignalStrengthCallbackWrapper;
import android.telephony.satellite.wrapper.NtnSignalStrengthWrapper;
import android.telephony.satellite.wrapper.SatelliteCapabilitiesCallbackWrapper;
import android.telephony.satellite.wrapper.SatelliteCommunicationAllowedStateCallbackWrapper;
import android.telephony.satellite.wrapper.SatelliteManagerWrapper;
import android.telephony.satellite.wrapper.SatelliteModemStateCallbackWrapper2;
import android.telephony.satellite.wrapper.SatelliteSubscriberInfoWrapper;
import android.telephony.satellite.wrapper.SatelliteSubscriberProvisionStatusWrapper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Activity related to SatelliteControl APIs for satellite.
 */
public class TestSatelliteWrapper extends Activity {
    private static final String TAG = "TestSatelliteWrapper";
    ArrayList<String> mLogMessages = new ArrayList<>();
    ArrayAdapter<String> mAdapter;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private SatelliteManagerWrapper mSatelliteManagerWrapper;
    private NtnSignalStrengthCallback mNtnSignalStrengthCallback = null;
    private SatelliteModemStateCallback mModemStateCallback = null;
    private CarrierRoamingNtnModeListener mCarrierRoamingNtnModeListener = null;
    private SatelliteCommunicationAllowedStateCallback mSatelliteCommunicationAllowedStateCallback;
    private SatelliteCapabilitiesCallbackWrapper mSatelliteCapabilitiesCallback;
    private SubscriptionManager mSubscriptionManager;
    private int mSubId;
    private List<SatelliteSubscriberProvisionStatusWrapper> mSatelliteSubscriberProvisionStatuses =
            new ArrayList<>();

    private ListView mLogListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSatelliteManagerWrapper = SatelliteManagerWrapper.getInstance(this);
        mSubscriptionManager = getSystemService(SubscriptionManager.class);
        mSubId = getActiveSubId();

        setContentView(R.layout.activity_TestSatelliteWrapper);
        findViewById(R.id.requestNtnSignalStrength)
                .setOnClickListener(this::requestNtnSignalStrength);
        findViewById(R.id.registerForNtnSignalStrengthChanged)
                .setOnClickListener(this::registerForNtnSignalStrengthChanged);
        findViewById(R.id.unregisterForNtnSignalStrengthChanged)
                .setOnClickListener(this::unregisterForNtnSignalStrengthChanged);
        findViewById(R.id.isOnlyNonTerrestrialNetworkSubscription)
                .setOnClickListener(this::isOnlyNonTerrestrialNetworkSubscription);
        findViewById(R.id.registerForSatelliteCapabilitiesChanged)
                .setOnClickListener(this::registerForCapabilitiesChanged);
        findViewById(R.id.unregisterForSatelliteCapabilitiesChanged)
                .setOnClickListener(this::unregisterForCapabilitiesChanged);
        findViewById(R.id.isNonTerrestrialNetwork)
                .setOnClickListener(this::isNonTerrestrialNetwork);
        findViewById(R.id.getAvailableServices)
                .setOnClickListener(this::getAvailableServices);
        findViewById(R.id.isUsingNonTerrestrialNetwork)
                .setOnClickListener(this::isUsingNonTerrestrialNetwork);
        findViewById(R.id.requestAttachEnabledForCarrier_enable)
                .setOnClickListener(this::requestAttachEnabledForCarrier_enable);
        findViewById(R.id.requestAttachEnabledForCarrier_disable)
                .setOnClickListener(this::requestAttachEnabledForCarrier_disable);
        findViewById(R.id.requestIsAttachEnabledForCarrier)
                .setOnClickListener(this::requestIsAttachEnabledForCarrier);
        findViewById(R.id.addAttachRestrictionForCarrier)
                .setOnClickListener(this::addAttachRestrictionForCarrier);
        findViewById(R.id.removeAttachRestrictionForCarrier)
                .setOnClickListener(this::removeAttachRestrictionForCarrier);
        findViewById(R.id.getAttachRestrictionReasonsForCarrier)
                .setOnClickListener(this::getAttachRestrictionReasonsForCarrier);
        findViewById(R.id.getSatellitePlmnsForCarrier)
                .setOnClickListener(this::getSatellitePlmnsForCarrier);
        findViewById(R.id.registerForCarrierRoamingNtnModeChanged)
                .setOnClickListener(this::registerForCarrierRoamingNtnModeChanged);
        findViewById(R.id.unregisterForCarrierRoamingNtnModeChanged)
                .setOnClickListener(this::unregisterForCarrierRoamingNtnModeChanged);
        findViewById(R.id.registerForCommunicationAllowedStateChanged)
                .setOnClickListener(this::registerForCommunicationAllowedStateChanged);
        findViewById(R.id.unregisterForCommunicationAllowedStateChanged)
                .setOnClickListener(this::unregisterForCommunicationAllowedStateChanged);
        findViewById(R.id.registerForModemStateChanged)
                .setOnClickListener(this::registerForModemStateChanged);
        findViewById(R.id.unregisterForModemStateChanged)
                .setOnClickListener(this::unregisterForModemStateChanged);
        findViewById(R.id.requestSatelliteSubscriberProvisionStatusWrapper)
                .setOnClickListener(this::requestSatelliteSubscriberProvisionStatus);
        findViewById(R.id.provisionSatelliteWrapper)
                .setOnClickListener(this::provisionSatellite);
        findViewById(R.id.deprovisionSatelliteWrapper)
                .setOnClickListener(this::deprovisionSatellite);

        findViewById(R.id.Back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TestSatelliteWrapper.this, SatelliteTestApp.class));
            }
        });
        findViewById(R.id.ClearLog).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearListView();
            }
        });

        mLogListView = findViewById(R.id.logListView);
        mAdapter = new ArrayAdapter<>(this, R.layout.log_textview, mLogMessages);
        mLogListView.setAdapter(mAdapter);

        addLogMessage("TestSatelliteWrapper.onCreate()");
    }


    private void clearListView() {
        mLogMessages.clear();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSatelliteManagerWrapper != null) {
            if (mNtnSignalStrengthCallback != null) {
                logd("unregisterForNtnSignalStrengthChanged()");
                mSatelliteManagerWrapper.unregisterForNtnSignalStrengthChanged(
                        mNtnSignalStrengthCallback);
            }
            if (mSatelliteCapabilitiesCallback != null) {
                logd("unregisterForCapabilitiesChanged()");
                mSatelliteManagerWrapper.unregisterForCapabilitiesChanged(
                        mSatelliteCapabilitiesCallback);
            }
        }
        mSubscriptionManager = null;
        mSatelliteManagerWrapper = null;
        mExecutor.shutdown();
    }

    private void requestNtnSignalStrength(View view) {
        addLogMessage("requestNtnSignalStrength");
        logd("requestNtnSignalStrength");
        OutcomeReceiver<NtnSignalStrengthWrapper,
                SatelliteManagerWrapper.SatelliteExceptionWrapper> receiver =
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(NtnSignalStrengthWrapper level) {
                        if (level != null) {
                            addLogMessage("requestNtnSignalStrength level is " + level.getLevel());
                        }
                    }

                    @Override
                    public void onError(
                            SatelliteManagerWrapper.SatelliteExceptionWrapper exception) {
                        if (exception != null) {
                            String onError = "requestNtnSignalStrength exception: "
                                    + translateResultCodeToString(exception.getErrorCode());
                            logd(onError);
                            addLogMessage(onError);
                        }
                    }
                };

        try {
            mSatelliteManagerWrapper.requestNtnSignalStrength(mExecutor, receiver);
        } catch (SecurityException ex) {
            String errorMessage = "requestNtnSignalStrength: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void registerForCarrierRoamingNtnModeChanged(View view) {
        addLogMessage("registerForCarrierRoamingNtnModeChanged");
        logd("registerForCarrierRoamingNtnModeChanged()");
        if (mCarrierRoamingNtnModeListener == null) {
            logd("Creating new CarrierRoamingNtnModeListener instance.");
            mCarrierRoamingNtnModeListener = new CarrierRoamingNtnModeListener();
        }

        try {
            mSatelliteManagerWrapper.registerForCarrierRoamingNtnModeChanged(mSubId, mExecutor,
                    mCarrierRoamingNtnModeListener);
        } catch (Exception ex) {
            String errorMessage = "registerForCarrierRoamingNtnModeChanged: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
            mCarrierRoamingNtnModeListener = null;
        }
    }

    private void unregisterForCarrierRoamingNtnModeChanged(View view) {
        addLogMessage("unregisterForCarrierRoamingNtnModeChanged");
        logd("unregisterForCarrierRoamingNtnModeChanged()");
        if (mCarrierRoamingNtnModeListener != null) {
            mSatelliteManagerWrapper.unregisterForCarrierRoamingNtnModeChanged(mSubId,
                    mCarrierRoamingNtnModeListener);
            mCarrierRoamingNtnModeListener = null;
            addLogMessage("mCarrierRoamingNtnModeListener was unregistered");
        } else {
            addLogMessage("mCarrierRoamingNtnModeListener is null, ignored.");
        }
    }

    private void registerForCommunicationAllowedStateChanged(View view) {
        addLogMessage("registerForCommunicationAllowedStateChanged");
        logd("registerForCommunicationAllowedStateChanged()");
        if (mSatelliteCommunicationAllowedStateCallback == null) {
            logd("Creating new CarrierRoamingNtnModeListener instance.");
            mSatelliteCommunicationAllowedStateCallback =
                    new SatelliteCommunicationAllowedStateCallback();
        }

        try {
            mSatelliteManagerWrapper.registerForCommunicationAllowedStateChanged(mExecutor,
                    mSatelliteCommunicationAllowedStateCallback);
        } catch (Exception ex) {
            String errorMessage = "registerForCommunicationAllowedStateChanged: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
            mSatelliteCommunicationAllowedStateCallback = null;
        }
    }

    private void unregisterForCommunicationAllowedStateChanged(View view) {
        addLogMessage("unregisterForCommunicationAllowedStateChanged");
        logd("unregisterForCommunicationAllowedStateChanged()");
        if (mSatelliteCommunicationAllowedStateCallback != null) {
            mSatelliteManagerWrapper.unregisterForCommunicationAllowedStateChanged(
                    mSatelliteCommunicationAllowedStateCallback);
            mSatelliteCommunicationAllowedStateCallback = null;
            addLogMessage("mSatelliteCommunicationAllowedStateCallback was unregistered");
        } else {
            addLogMessage("mSatelliteCommunicationAllowedStateCallback is null, ignored.");
        }
    }

    private void registerForNtnSignalStrengthChanged(View view) {
        addLogMessage("registerForNtnSignalStrengthChanged");
        logd("registerForNtnSignalStrengthChanged()");
        if (mNtnSignalStrengthCallback == null) {
            logd("create new NtnSignalStrengthCallback instance.");
            mNtnSignalStrengthCallback = new NtnSignalStrengthCallback();
        }

        try {
            mSatelliteManagerWrapper.registerForNtnSignalStrengthChanged(mExecutor,
                    mNtnSignalStrengthCallback);
        } catch (Exception ex) {
            String errorMessage = "registerForNtnSignalStrengthChanged: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
            mNtnSignalStrengthCallback = null;
        }
    }

    private void unregisterForNtnSignalStrengthChanged(View view) {
        addLogMessage("unregisterForNtnSignalStrengthChanged");
        logd("unregisterForNtnSignalStrengthChanged()");
        if (mNtnSignalStrengthCallback != null) {
            mSatelliteManagerWrapper.unregisterForNtnSignalStrengthChanged(
                    mNtnSignalStrengthCallback);
            mNtnSignalStrengthCallback = null;
            addLogMessage("mNtnSignalStrengthCallback was unregistered");
        } else {
            addLogMessage("mNtnSignalStrengthCallback is null, ignored.");
        }
    }

    private void isOnlyNonTerrestrialNetworkSubscription(View view) {
        addLogMessage("isOnlyNonTerrestrialNetworkSubscription");
        logd("isOnlyNonTerrestrialNetworkSubscription()");
        List<SubscriptionInfo> infoList = mSubscriptionManager.getAvailableSubscriptionInfoList();
        List<Integer> subIdList = infoList.stream()
                .map(SubscriptionInfo::getSubscriptionId)
                .toList();

        Map<Integer, Boolean> resultMap = subIdList.stream().collect(
                Collectors.toMap(
                        id -> id,
                        id -> {
                            boolean result = mSatelliteManagerWrapper
                                    .isOnlyNonTerrestrialNetworkSubscription(id);
                            addLogMessage("SatelliteManagerWrapper"
                                    + ".isOnlyNonTerrestrialNetworkSubscription(" + id + ")");
                            return result;
                        }
                ));

        for (Map.Entry<Integer, Boolean> entry : resultMap.entrySet()) {
            int subId = entry.getKey();
            boolean result = entry.getValue();
            addLogMessage("Subscription ID: " + subId + ", Result: " + result);
        }
    }

    private void registerForCapabilitiesChanged(View view) {
        addLogMessage("registerForCapabilitiesChanged");
        logd("registerForCapabilitiesChanged()");
        if (mSatelliteCapabilitiesCallback == null) {
            mSatelliteCapabilitiesCallback =
                    SatelliteCapabilities -> {
                        String message = "Received SatelliteCapabillities : "
                                + SatelliteCapabilities;
                        logd(message);
                        addLogMessage(message);
                    };
        }

        int result = mSatelliteManagerWrapper.registerForCapabilitiesChanged(mExecutor,
                mSatelliteCapabilitiesCallback);
        if (result != SatelliteManagerWrapper.SATELLITE_RESULT_SUCCESS) {
            String onError = translateResultCodeToString(result);
            logd(onError);
            addLogMessage(onError);
            mSatelliteCapabilitiesCallback = null;
        }
    }

    private void unregisterForCapabilitiesChanged(View view) {
        addLogMessage("unregisterForCapabilitiesChanged");
        logd("unregisterForCapabilitiesChanged()");
        if (mSatelliteCapabilitiesCallback != null) {
            mSatelliteManagerWrapper.unregisterForCapabilitiesChanged(
                    mSatelliteCapabilitiesCallback);
            mSatelliteCapabilitiesCallback = null;
            addLogMessage("mSatelliteCapabilitiesCallback was unregistered");
        } else {
            addLogMessage("mSatelliteCapabilitiesCallback is null, ignored.");
        }
    }

    private void registerForModemStateChanged(View view) {
        addLogMessage("registerForModemStateChanged");
        logd("registerForSatelliteModemStateChanged()");
        if (mModemStateCallback == null) {
            logd("create new ModemStateCallback instance.");
            mModemStateCallback = new SatelliteModemStateCallback();
        }

        try {
            mSatelliteManagerWrapper.registerForModemStateChanged(mExecutor, mModemStateCallback);
        } catch (Exception ex) {
            String errorMessage = "registerForModemStateChanged: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
            mModemStateCallback = null;
        }
    }

    private void unregisterForModemStateChanged(View view) {
        addLogMessage("unregisterForModemStateChanged");
        logd("unregisterForModemStateChanged()");
        if (mModemStateCallback != null) {
            mSatelliteManagerWrapper.unregisterForModemStateChanged(mModemStateCallback);
            mModemStateCallback = null;
            addLogMessage("mModemStateCallback was unregistered");
        } else {
            addLogMessage("mModemStateCallback is null, ignored.");
        }
    }

    private void requestSatelliteSubscriberProvisionStatus(View view) {
        addLogMessage("requestSatelliteSubscriberProvisionStatus");
        logd("requestSatelliteSubscriberProvisionStatus");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("requestSatelliteSubscriberProvisionStatus: Subscription ID is invalid");
            logd("requestSatelliteSubscriberProvisionStatus: Subscription ID is invalid");
            return;
        }

        OutcomeReceiver<List<SatelliteSubscriberProvisionStatusWrapper>,
                SatelliteManagerWrapper.SatelliteExceptionWrapper> receiver =
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(List<SatelliteSubscriberProvisionStatusWrapper> result) {
                        mSatelliteSubscriberProvisionStatuses = result;
                        logd("requestSatelliteSubscriberProvisionStatus: onResult=" + result);
                        addLogMessage(
                                "requestSatelliteSubscriberProvisionStatus: onResult=" + result);
                    }

                    @Override
                    public void onError(
                            SatelliteManagerWrapper.SatelliteExceptionWrapper exception) {
                        if (exception != null) {
                            String onError = "requestSatelliteSubscriberProvisionStatus exception: "
                                    + translateResultCodeToString(exception.getErrorCode());
                            logd(onError);
                            addLogMessage(onError);
                        }
                    }
                };

        try {
            mSatelliteManagerWrapper.requestSatelliteSubscriberProvisionStatus(mExecutor, receiver);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "requestSatelliteSubscriberProvisionStatus: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void provisionSatellite(View view) {
        addLogMessage("provisionSatellite");
        logd("provisionSatellite");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("provisionSatellite: Subscription ID is invalid");
            logd("provisionSatellite: Subscription ID is invalid");
            return;
        }

        OutcomeReceiver<Boolean,
                SatelliteManagerWrapper.SatelliteExceptionWrapper> receiver =
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Boolean result) {
                        logd("provisionSatellite: onResult=" + result);
                        addLogMessage("provisionSatellite: onResult=" + result);
                    }

                    @Override
                    public void onError(
                            SatelliteManagerWrapper.SatelliteExceptionWrapper exception) {
                        if (exception != null) {
                            String onError = "provisionSatellite exception: "
                                    + translateResultCodeToString(exception.getErrorCode());
                            logd(onError);
                            addLogMessage(onError);
                        }
                    }
                };

        List<SatelliteSubscriberInfoWrapper> list = new ArrayList<>();
        for (SatelliteSubscriberProvisionStatusWrapper status :
                mSatelliteSubscriberProvisionStatuses) {
            list.add(status.getSatelliteSubscriberInfo());
        }
        try {
            mSatelliteManagerWrapper.provisionSatellite(list, mExecutor, receiver);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "provisionSatellite: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void deprovisionSatellite(View view) {
        addLogMessage("deprovisionSatellite");
        logd("deprovisionSatellite");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("deprovisionSatellite: Subscription ID is invalid");
            logd("deprovisionSatellite: Subscription ID is invalid");
            return;
        }

        OutcomeReceiver<Boolean,
                SatelliteManagerWrapper.SatelliteExceptionWrapper> receiver =
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Boolean result) {
                        logd("deprovisionSatellite: onResult=" + result);
                        addLogMessage("deprovisionSatellite: onResult=" + result);
                    }

                    @Override
                    public void onError(
                            SatelliteManagerWrapper.SatelliteExceptionWrapper exception) {
                        if (exception != null) {
                            String onError = "deprovisionSatellite exception: "
                                    + translateResultCodeToString(exception.getErrorCode());
                            logd(onError);
                            addLogMessage(onError);
                        }
                    }
                };

        List<SatelliteSubscriberInfoWrapper> list = new ArrayList<>();
        for (SatelliteSubscriberProvisionStatusWrapper status :
                mSatelliteSubscriberProvisionStatuses) {
            list.add(status.getSatelliteSubscriberInfo());
        }
        try {
            mSatelliteManagerWrapper.deprovisionSatellite(list, mExecutor, receiver);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "deprovisionSatellite: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    public class NtnSignalStrengthCallback implements NtnSignalStrengthCallbackWrapper {
        @Override
        public void onNtnSignalStrengthChanged(
                @NonNull NtnSignalStrengthWrapper ntnSignalStrength) {
            String message = "Received NTN SignalStrength : " + ntnSignalStrength.getLevel();
            logd(message);
            addLogMessage(message);
        }
    }

    private class CarrierRoamingNtnModeListener implements CarrierRoamingNtnModeListenerWrapper2 {

        @Override
        public void onCarrierRoamingNtnModeChanged(boolean active) {
            String message = "Received onCarrierRoamingNtnModeChanged active: " + active;
            logd(message);
            addLogMessage(message);
        }

        @Override
        public void onCarrierRoamingNtnEligibleStateChanged(boolean eligible) {
            String message = "Received onCarrierRoamingNtnEligibleStateChanged "
                    + "eligible: " + eligible;
            logd(message);
            addLogMessage(message);
        }
    }

    private class SatelliteCommunicationAllowedStateCallback implements
            SatelliteCommunicationAllowedStateCallbackWrapper {

        @Override
        public void onSatelliteCommunicationAllowedStateChanged(boolean isAllowed) {
            String message =
                    "Received onSatelliteCommunicationAllowedStateChanged isAllowed: " + isAllowed;
            logd(message);
            addLogMessage(message);
        }
    }

    private class SatelliteModemStateCallback implements SatelliteModemStateCallbackWrapper2 {
        @Override
        public void onSatelliteModemStateChanged(int state) {
            String message = "Received onSatelliteModemStateChanged state: " + state;
            logd(message);
            addLogMessage(message);
        }

        @Override
        public void onEmergencyModeChanged(boolean isEmergency) {
            String message = "Received onEmergencyModeChanged isEmergency: " + isEmergency;
            logd(message);
            addLogMessage(message);
        }
    }

    private class SatelliteCommunicationAllowedStateCallback implements
            SatelliteCommunicationAllowedStateCallbackWrapper {

        @Override
        public void onSatelliteCommunicationAllowedStateChanged(boolean isAllowed) {
            String message =
                    "Received onSatelliteCommunicationAllowedStateChanged isAllowed: " + isAllowed;
            logd(message);
            addLogMessage(message);
        }
    }

    private void isNonTerrestrialNetwork(View view) {
        boolean isNonTerrestrialNetwork = mSatelliteManagerWrapper.isNonTerrestrialNetwork(mSubId);
        addLogMessage("isNonTerrestrialNetwork=" + isNonTerrestrialNetwork);
        logd("isNonTerrestrialNetwork=" + isNonTerrestrialNetwork);
    }

    private void getAvailableServices(View view) {
        List<Integer> as = mSatelliteManagerWrapper.getAvailableServices(mSubId);
        String availableServices = as.stream().map(Object::toString).collect(
                Collectors.joining(", "));
        addLogMessage("getAvailableServices=" + availableServices);
        logd("getAvailableServices=" + availableServices);
    }

    private void isUsingNonTerrestrialNetwork(View view) {
        boolean isUsingNonTerrestrialNetwork =
                mSatelliteManagerWrapper.isUsingNonTerrestrialNetwork(mSubId);
        addLogMessage("isUsingNonTerrestrialNetwork=" + isUsingNonTerrestrialNetwork);
        logd("isUsingNonTerrestrialNetwork=" + isUsingNonTerrestrialNetwork);
    }

    private void requestAttachEnabledForCarrier_enable(View view) {
        addLogMessage("requestAttachEnabledForCarrier");
        logd("requestAttachEnabledForCarrier");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("requestAttachEnabledForCarrier: Subscription ID is invalid");
            logd("requestAttachEnabledForCarrier: Subscription ID is invalid");
            return;
        }

        Consumer<Integer> callback = result -> {
            addLogMessage("requestAttachEnabledForCarrier result: " + result);
            logd("requestAttachEnabledForCarrier result: " + result);
        };

        try {
            mSatelliteManagerWrapper.requestAttachEnabledForCarrier(mSubId, true, mExecutor,
                    callback);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "requestAttachEnabledForCarrier: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void requestAttachEnabledForCarrier_disable(View view) {
        addLogMessage("requestAttachEnabledForCarrier");
        logd("requestAttachEnabledForCarrier");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("requestAttachEnabledForCarrier: Subscription ID is invalid");
            logd("requestAttachEnabledForCarrier: Subscription ID is invalid");
            return;
        }

        Consumer<Integer> callback = result -> {
            addLogMessage("requestAttachEnabledForCarrier result: " + result);
            logd("requestAttachEnabledForCarrier result: " + result);
        };

        try {
            mSatelliteManagerWrapper.requestAttachEnabledForCarrier(mSubId, false, mExecutor,
                    callback);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "requestAttachEnabledForCarrier: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void requestIsAttachEnabledForCarrier(View view) {
        logd("requestIsAttachEnabledForCarrier");
        addLogMessage("requestIsAttachEnabledForCarrier");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("requestIsAttachEnabledForCarrier: Subscription ID is invalid");
            logd("requestIsAttachEnabledForCarrier: Subscription ID is invalid");
            return;
        }

        OutcomeReceiver<Boolean,
                SatelliteManagerWrapper.SatelliteExceptionWrapper> receiver =
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Boolean result) {
                        logd("requestIsAttachEnabledForCarrier: onResult=" + result);
                        addLogMessage("requestIsAttachEnabledForCarrier: onResult=" + result);
                    }

                    @Override
                    public void onError(
                            SatelliteManagerWrapper.SatelliteExceptionWrapper exception) {
                        if (exception != null) {
                            String onError = "requestIsAttachEnabledForCarrier exception: "
                                    + translateResultCodeToString(exception.getErrorCode());
                            logd(onError);
                            addLogMessage(onError);
                        }
                    }
                };

        try {
            mSatelliteManagerWrapper.requestIsAttachEnabledForCarrier(mSubId, mExecutor, receiver);
        } catch (SecurityException | IllegalStateException | IllegalArgumentException ex) {
            String errorMessage = "requestIsAttachEnabledForCarrier: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void addAttachRestrictionForCarrier(View view) {
        addLogMessage("addAttachRestrictionForCarrier");
        logd("addAttachRestrictionForCarrier");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("addAttachRestrictionForCarrier: Subscription ID is invalid");
            logd("addAttachRestrictionForCarrier: Subscription ID is invalid");
            return;
        }

        int reason = SatelliteManagerWrapper.SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER;

        Consumer<Integer> callback = result -> {
            addLogMessage("addAttachRestrictionForCarrier result: " + result);
            logd("addAttachRestrictionForCarrier result: " + result);
        };

        try {
            mSatelliteManagerWrapper.addAttachRestrictionForCarrier(mSubId, reason, mExecutor,
                    callback);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "addAttachRestrictionForCarrier: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void removeAttachRestrictionForCarrier(View view) {
        addLogMessage("removeAttachRestrictionForCarrier");
        logd("removeAttachRestrictionForCarrier");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("removeAttachRestrictionForCarrier: Subscription ID is invalid");
            logd("removeAttachRestrictionForCarrier: Subscription ID is invalid");
            return;
        }

        int reason = SatelliteManagerWrapper.SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER;

        Consumer<Integer> callback = result -> {
            addLogMessage("removeAttachRestrictionForCarrier result: " + result);
            logd("removeAttachRestrictionForCarrier result: " + result);
        };

        try {
            mSatelliteManagerWrapper.removeAttachRestrictionForCarrier(mSubId, reason, mExecutor,
                    callback);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "removeAttachRestrictionForCarrier: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void getAttachRestrictionReasonsForCarrier(View view) {
        addLogMessage("getAttachRestrictionReasonsForCarrier");
        logd("getAttachRestrictionReasonsForCarrier");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("getAttachRestrictionReasonsForCarrier: Subscription ID is invalid");
            logd("getAttachRestrictionReasonsForCarrier: Subscription ID is invalid");
            return;
        }

        try {
            Set<Integer> reasons = mSatelliteManagerWrapper.getAttachRestrictionReasonsForCarrier(
                    mSubId);
            String stringReasons = reasons.stream().map(Object::toString).collect(
                    Collectors.joining(", "));
            logd("getAttachRestrictionReasonsForCarrier=" + stringReasons);
            addLogMessage("getAttachRestrictionReasonsForCarrier=" + stringReasons);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "getAttachRestrictionReasonsForCarrier: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private void getSatellitePlmnsForCarrier(View view) {
        addLogMessage("getSatellitePlmnsForCarrier");
        logd("getSatellitePlmnsForCarrier");

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addLogMessage("getSatellitePlmnsForCarrier: Subscription ID is invalid");
            logd("getSatellitePlmnsForCarrier: Subscription ID is invalid");
            return;
        }

        try {
            List<String> reasons = mSatelliteManagerWrapper.getSatellitePlmnsForCarrier(
                    mSubId);
            String stringReasons = reasons.stream().collect(Collectors.joining(", "));
            logd("getSatellitePlmnsForCarrier=" + stringReasons);
            addLogMessage("getSatellitePlmnsForCarrier=" + stringReasons);
        } catch (SecurityException | IllegalArgumentException ex) {
            String errorMessage = "getSatellitePlmnsForCarrier: " + ex.getMessage();
            logd(errorMessage);
            addLogMessage(errorMessage);
        }
    }

    private int getActiveSubId() {
        int subId;
        List<SubscriptionInfo> subscriptionInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();

        if (subscriptionInfoList != null && subscriptionInfoList.size() > 0) {
            subId = subscriptionInfoList.get(0).getSubscriptionId();
        } else {
            subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        logd("getActiveSubId() returns " + subId);
        return subId;
    }

    private String translateResultCodeToString(
            @SatelliteManagerWrapper.SatelliteResult int result) {
        switch (result) {
            case SatelliteManagerWrapper.SATELLITE_RESULT_SUCCESS:
                return "SATELLITE_RESULT_SUCCESS";
            case SatelliteManagerWrapper.SATELLITE_RESULT_ERROR:
                return "SATELLITE_RESULT_ERROR";
            case SatelliteManagerWrapper.SATELLITE_RESULT_SERVER_ERROR:
                return "SATELLITE_RESULT_SERVER_ERROR";
            case SatelliteManagerWrapper.SATELLITE_RESULT_SERVICE_ERROR:
                return "SATELLITE_RESULT_SERVICE_ERROR";
            case SatelliteManagerWrapper.SATELLITE_RESULT_MODEM_ERROR:
                return "SATELLITE_RESULT_MODEM_ERROR";
            case SatelliteManagerWrapper.SATELLITE_RESULT_NETWORK_ERROR:
                return "SATELLITE_RESULT_NETWORK_ERROR";
            case SatelliteManagerWrapper.SATELLITE_RESULT_INVALID_TELEPHONY_STATE:
                return "SATELLITE_RESULT_INVALID_TELEPHONY_STATE";
            case SatelliteManagerWrapper.SATELLITE_RESULT_INVALID_MODEM_STATE:
                return "SATELLITE_RESULT_INVALID_MODEM_STATE";
            case SatelliteManagerWrapper.SATELLITE_RESULT_INVALID_ARGUMENTS:
                return "SATELLITE_RESULT_INVALID_ARGUMENTS";
            case SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_FAILED:
                return "SATELLITE_RESULT_REQUEST_FAILED";
            case SatelliteManagerWrapper.SATELLITE_RESULT_RADIO_NOT_AVAILABLE:
                return "SATELLITE_RESULT_RADIO_NOT_AVAILABLE";
            case SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_NOT_SUPPORTED:
                return "SATELLITE_RESULT_REQUEST_NOT_SUPPORTED";
            case SatelliteManagerWrapper.SATELLITE_RESULT_NO_RESOURCES:
                return "SATELLITE_RESULT_NO_RESOURCES";
            case SatelliteManagerWrapper.SATELLITE_RESULT_SERVICE_NOT_PROVISIONED:
                return "SATELLITE_RESULT_SERVICE_NOT_PROVISIONED";
            case SatelliteManagerWrapper.SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS:
                return "SATELLITE_RESULT_SERVICE_PROVISION_IN_PROGRESS";
            case SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_ABORTED:
                return "SATELLITE_RESULT_REQUEST_ABORTED";
            case SatelliteManagerWrapper.SATELLITE_RESULT_ACCESS_BARRED:
                return "SATELLITE_RESULT_ACCESS_BARRED";
            case SatelliteManagerWrapper.SATELLITE_RESULT_NETWORK_TIMEOUT:
                return "SATELLITE_RESULT_NETWORK_TIMEOUT";
            case SatelliteManagerWrapper.SATELLITE_RESULT_NOT_REACHABLE:
                return "SATELLITE_RESULT_NOT_REACHABLE";
            case SatelliteManagerWrapper.SATELLITE_RESULT_NOT_AUTHORIZED:
                return "SATELLITE_RESULT_NOT_AUTHORIZED";
            case SatelliteManagerWrapper.SATELLITE_RESULT_NOT_SUPPORTED:
                return "SATELLITE_RESULT_NOT_SUPPORTED";
            case SatelliteManagerWrapper.SATELLITE_RESULT_REQUEST_IN_PROGRESS:
                return "SATELLITE_RESULT_REQUEST_IN_PROGRESS";
            case SatelliteManagerWrapper.SATELLITE_RESULT_MODEM_BUSY:
                return "SATELLITE_RESULT_MODEM_BUSY";
            case SatelliteManagerWrapper.SATELLITE_RESULT_ILLEGAL_STATE:
                return "SATELLITE_RESULT_ILLEGAL_STATE";
            default:
                return "INVALID CODE: " + result;
        }
    }

    private void addLogMessage(String message) {
        runOnUiThread(() -> {
            mLogMessages.add(message);
            mAdapter.notifyDataSetChanged();
            mLogListView.setSelection(mAdapter.getCount() - 1);
        });
    }

    private static void logd(String message) {
        if (message != null) {
            Log.d(TAG, message);
        }
    }
}
