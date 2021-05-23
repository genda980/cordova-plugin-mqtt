var exec = require('cordova/exec');

// client_id mqtt_host mqtt_port_tcp password username
exports.connect = function (arg0, success, error) {
    exec(success, error, 'Mqtt', 'connect', [arg0]);
};

exports.disConnect = function(success, error) {
    exec(success, error, 'Mqtt', 'disConnect', [{}]);
};

// 返回js success 1 -> 已连接 0 -> 连接丢失
exports.checkStatus = function (success, error) {
    exec(success, error, 'Mqtt', 'checkStatus', [{}]);
};

// 100 连接断开 101 重新连接 102 连接成功
exports.onMessageEvent = function (eventId, params) {
    cordova.fireDocumentEvent('Mqtt.onMessageEvent', {
        eventId: eventId,
        params: params
    });
};

exports.syncInit = function (arg0, success, error) {
    exec(success, error, 'Mqtt', 'syncInit', [arg0]);
};

exports.syncMsg = function (arg0, success, error) {
    exec(success, error, 'Mqtt', 'syncMsg', [arg0]);
};

exports.onSyncMsgEvent = function (eventId, params) {
    cordova.fireDocumentEvent('Mqtt.onSyncMsgEvent', {
        eventId: eventId,
        params: params,
    });
};

