package org.apache.cordova.mqtt;

import android.content.Intent;
import android.text.TextUtils;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class Mqtt extends CordovaPlugin {

    static MqttInfo sMqttInfo;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject jsonObject = new JSONObject(args.optString(0));
        if (action.equals("connect")) {
            connect(jsonObject, callbackContext);
            return true;
        }
        if (action.equals("disConnect")) {
            disConnect();
            return true;
        }
        if (action.equals("checkStatus")) {
            checkStatus(callbackContext);
            return true;
        }
        return false;
    }

    private void connect(JSONObject obj, CallbackContext callbackContext) {
        String clientId = obj.optString("client_id", "");
        String host = obj.optString("mqtt_host", "");
        int port = obj.optInt("mqtt_port_tcp", 0);
        String pwd = obj.optString("password", "");
        String userName = obj.optString("username", "");
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(host) || port == 0 || TextUtils.isEmpty(pwd) || TextUtils.isEmpty(userName)) {
            callbackContext.error("Params error");
            return;
        }
        sMqttInfo = new MqttInfo(clientId, host, port, pwd, userName);
        cordova.getActivity().startService(new Intent(cordova.getContext(), MqttService.class));
    }

    private void disConnect() {
        MqttService.disConnect();
    }

    private void checkStatus(CallbackContext callbackContext) {
        if (MqttService.isConnect()) {
            callbackContext.success(1);
        } else {
            callbackContext.success(0);
        }
    }

    public static class MqttInfo {
        public String client_id;
        public String mqtt_host;
        public int mqtt_port;
        public String password;
        public String username;

        public MqttInfo(String client_id, String mqtt_host, int mqtt_port, String password, String username) {
            this.client_id = client_id;
            this.mqtt_host = mqtt_host;
            this.mqtt_port = mqtt_port;
            this.password = password;
            this.username = username;
        }
    }
}
