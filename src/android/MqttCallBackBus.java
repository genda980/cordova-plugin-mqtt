package org.apache.cordova.mqtt;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

class MqttCallBackBus implements MqttCallbackExtended {

    private static final String TAG = "MqttCallBackBus";

    private boolean isLost = false;

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        int type = reconnect ? 101 : 102;
        MqttBean mqttBean = new MqttBean(type, new JSONObject());
        //EventBus.getDefault().post(bean);
    }

    @Override
    public void connectionLost(Throwable cause) {
        try {
            if (MqttService.isConnect()) return;
            if (isLost) return;
            isLost = true;
            new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    reloadConnect(cause);
                    isLost = false;
                }
            }, 3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String msg = new String(message.getPayload());
        Log.d(TAG, "messageArrived: mqtt message = " + msg);

        MqttBean bean = JSON.parseObject(msg, MqttBean.class);
        //EventBus.getDefault().post(bean);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    private void reloadConnect(Throwable throwable) {
        if (throwable == null) return;
        if (throwable instanceof MqttException) {
            MqttException mqttException = (MqttException) throwable;
            Log.e(TAG, "connectionLost: ---->  mqtt 连接异常 code = " + mqttException.getReasonCode());
            if (mqttException.getReasonCode() != MqttException.REASON_CODE_CONNECTION_LOST) return;
            MqttBean mqttBean = new MqttBean(100, new JSONObject());
            //EventBus.getDefault().post(mqttBean);
        }
    }
}
