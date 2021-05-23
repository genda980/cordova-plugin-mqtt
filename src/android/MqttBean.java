package org.apache.cordova.mqtt;

public class MqttBean {

    public int type;
    public Object data;

    public MqttBean() {
    }

    public MqttBean(int type, Object data) {
        this.type = type;
        this.data = data;
    }
}
