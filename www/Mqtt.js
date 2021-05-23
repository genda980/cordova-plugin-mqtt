var exec = require('cordova/exec');

exports.connect = function (arg0, success, error) {
    exec(success, error, 'Mqtt', 'connect', [arg0]);
};

exports.disConnect = function(success, error) {
    exec(success, error, 'Mqtt', 'disConnect', [{}]);
};

exports.checkStatus = function (success, error) {
    exec(success, error, 'Mqtt', 'checkStatus', [{}]);
};

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

