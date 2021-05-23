package org.apache.cordova.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import androidx.annotation.Nullable;

public class MqttService extends Service {

    private static final String TAG = "MqttService";

    private static MqttAndroidClient sClient;
    private int mFailCount = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startConnect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disConnect();
    }

    private void startConnect() {
        if (sClient != null && sClient.isConnected()) return;

        final Mqtt.MqttInfo info = Mqtt.sMqttInfo;
        if (info == null) return;

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);
        options.setUserName(info.username);
        options.setPassword(info.password.toCharArray());
        if (sClient == null) {
            String url = "tcp://" + info.mqtt_host + ":" + info.mqtt_host;
            sClient = new MqttAndroidClient(this, url, info.client_id);
            sClient.setCallback(new MqttCallBackBus());
        }
        try {
            sClient.connect(options, null, new MqttConnectListener());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final class MqttConnectListener implements IMqttActionListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(TAG, "MqttConnectListener onSuccess: ");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.e(TAG, "run: ---> mq fail connect");
            exception.printStackTrace();
            mFailCount += 1;
            if (mFailCount == 1) {
                Log.e(TAG, "run: ---> mq fail 1");
                startConnect();
            } else if (mFailCount == 2) {
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "run: ---> mq fail 2 ");
                        startConnect();
                    }
                }, 5000);
            } else if (mFailCount == 3) {
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "run: ---> mq fail 3 ");
                        startConnect();
                    }
                }, 20000);
            }
        }
    }

    public static boolean isConnect() {
        return sClient != null && sClient.isConnected();
    }

    public static void disConnect() {
        if (sClient == null) return;
        try {
            sClient.disconnect();
            sClient = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
