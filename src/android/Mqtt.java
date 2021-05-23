package org.apache.cordova.mqtt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.util.Iterator;

import io.sqlc.SQLiteAndroidDatabase;

/**
 * This class echoes a string called from JavaScript.
 */
public class Mqtt extends CordovaPlugin {

    private static final String TAG = "Mqtt";

    static MqttInfo sMqttInfo;

    private SQLiteAndroidDatabase mDB;

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
        if (action.equals("syncInit")) {
            syncInit(jsonObject, callbackContext);
            return true;
        }
        if (action.equals("syncMsg")) {
            new AsyncMsgTask().execute(jsonObject);
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

    private void syncInit(JSONObject obj, CallbackContext callbackContext) {
        try {
            String dbName = obj.getString("db_name");
            if (TextUtils.isEmpty(dbName)) {
                callbackContext.error(1);
                return;
            }
            mDB = new SQLiteAndroidDatabase();
            File dbFile = this.cordova.getActivity().getDatabasePath(dbName);
            if (!dbFile.exists()) {
                dbFile.getParentFile().mkdirs();
            }
            mDB.open(dbFile);
            callbackContext.success();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncMsgTask extends AsyncTask<JSONObject, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            JSONObject dataJson = jsonObjects[0];
            int chatId = dataJson.optInt("chat_id");
            int chatType = dataJson.optInt("chat_type", 1);
            int hasMore = dataJson.optInt("has_more");
            long lastSyncMTime = dataJson.optLong("last_sync_mtime"); // 客户端请求的 sync_time
            long lastMTime = dataJson.optLong("last_mtime", 1);

            boolean tableRes = judgePrvTable(chatId);
            if (!tableRes) {
                return parseClubResult(0, chatType, chatId, lastSyncMTime, lastMTime, hasMore);
            }

            try {
                JSONObject userInfoObj = dataJson.getJSONObject("user_infos");
                JSONArray listArray = dataJson.getJSONArray("list");

                Iterator userIterator = userInfoObj.keys();
                while (userIterator.hasNext()) {
                    String key = (String) userIterator.next();
                    JSONObject user = userInfoObj.getJSONObject(key);
                    Log.e(TAG, "PrvAsyncTask: ----->  key = " + key + "   value = " + user.toString());
                    upUser(user, key);
                }

                int length = listArray.length();
                for (int i = 0; i < length; i++) {
                    JSONObject msgObj = listArray.getJSONObject(i);
                    Log.e(TAG, "PrvAsyncTask: sdsad  " + msgObj.toString());
                    boolean upRes = upPrvMsg(chatId, msgObj, chatType);
                    if (!upRes) {
                        return parseClubResult(0, chatType, chatId, lastSyncMTime, lastMTime, hasMore);
                    } else {
                        Log.e(TAG, "PrvAsyncTask: ----->  up msg success");
                    }
                }

                return parseClubResult(1, chatType, chatId, lastSyncMTime, lastMTime, hasMore);

            } catch (Exception e) {
                e.printStackTrace();
                return parseClubResult(0, chatType, chatId, lastSyncMTime, lastMTime, hasMore);
            }
        }

        @Override
        protected void onPostExecute(JSONObject backObj) {
            Log.e(TAG, "PrvAsyncTask: -----> finish ...");
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    @SuppressLint("DefaultLocale") final String jsStr =
                            String.format("Mqtt.onSyncMsgEvent(%d, %s)", 1, backObj);
                    webView.loadUrl("javascript:" + jsStr);
                }
            });
        }
    }

    private boolean judgePrvTable(int chatId) {
        String createSql = "CREATE TABLE IF NOT EXISTS tb_msg_" + chatId + " (id INTEGER PRIMARY KEY AUTOINCREMENT, chat_id INTEGER, send_uid INTEGER, mtime INTEGER, ctime INTEGER, msg_type INTEGER, msg_content varchar(500), status INTEGER, m_type INTEGER, remark varchar(500))";
        JSONArray createArray = mDB.myExecute(createSql, new JSONArray());
        JSONObject createRes = parseResult(createArray);
        if (createRes == null) return false;

        String ctimeSql = "CREATE index IF NOT EXISTS idx_ctime on tb_msg_" + chatId + "(ctime, m_type)";
        JSONArray ctimeArray = mDB.myExecute(ctimeSql, new JSONArray());
        JSONObject ctimeRes = parseResult(ctimeArray);
        if (ctimeRes == null) return false;

        String mtimeSql = "CREATE index IF NOT EXISTS idx_mtime on tb_msg_" + chatId + "(mtime)";
        JSONArray mtimeArray = mDB.myExecute(mtimeSql, new JSONArray());
        JSONObject mtimeRes = parseResult(mtimeArray);
        if (mtimeRes == null) return false;

        return true;
    }

    private void upUser(JSONObject userObj, String uid) {
        String querySql = "SELECT uid FROM tb_user WHERE uid = ? LIMIT 1";
        JSONArray queryParams = new JSONArray();
        queryParams.put(uid);
        JSONArray queryArray = mDB.myExecute(querySql, queryParams);
        JSONObject queryRes = parseResult(queryArray);
        if (queryRes != null && !judgeExist(queryRes)) {
            Log.e(TAG, "ClubAsyncTask: -----> upUser not exist ..." + uid);
            String insertSql = "INSERT INTO tb_user (uid, nickname, avatar, sex, is_auth, level, birthday, is_vip) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            JSONArray insertParams = new JSONArray();
            insertParams.put(userObj.optInt("uid"));
            insertParams.put(userObj.optString("nickname", "U_" + uid));
            insertParams.put(userObj.optString("avatar", ""));
            insertParams.put(userObj.optInt("sex", 1));
            insertParams.put(userObj.optInt("is_auth", 0));
            insertParams.put(userObj.optInt("level", 0));
            insertParams.put(userObj.optString("birthday", ""));
            insertParams.put(userObj.optInt("is_vip", 0));
            mDB.myExecute(insertSql, insertParams);
        }
    }

    private boolean upPrvMsg(int chatId, JSONObject msgObj, int chatType) {
        long mtime = msgObj.optLong("mtime", 0);
        int sendUid = msgObj.optInt("send_uid", 0);
        if (mtime == 0) return true;
        if (sendUid == 0) return true;
        Log.e(TAG, "PrvAsyncTask: -----> upPrvMsg mtime = " + mtime);

        String querySql = "SELECT id FROM tb_msg_" + chatId + " WHERE mtime = ? LIMIT 1";
        JSONArray queryParams = new JSONArray();
        queryParams.put(mtime);
        JSONArray queryArray = mDB.myExecute(querySql, queryParams);
        JSONObject queryRes = parseResult(queryArray);

        if (queryRes == null) return false;

        if (judgeExist(queryRes)) return true;

        String insertSql = "INSERT INTO tb_msg_" + chatId + " (chat_id,send_uid,mtime,ctime,msg_type,msg_content,status,m_type,remark) VALUES (?,?,?,?,?,?,?,?,?)";
        JSONArray insertParams = new JSONArray();
        insertParams.put(chatId);
        insertParams.put(sendUid);
        insertParams.put(mtime);
        insertParams.put(getCtimeFromMtime(mtime));
        insertParams.put(msgObj.optInt("msg_type", 0));
        insertParams.put(msgObj.optString("msg_content", ""));
        insertParams.put(0);
        insertParams.put(chatType == 3 ? 1 : 0);
        insertParams.put("");
        JSONArray insertArray = mDB.myExecute(insertSql, insertParams);
        JSONObject insertRes = parseResult(insertArray);
        if (insertRes == null) return false;

        return true;
    }

    private boolean judgeExist(JSONObject result) {
        return !result.isNull("rows");
    }

    private long getCtimeFromMtime(long mTime) {
        BigDecimal leftTime = new BigDecimal(mTime);
        BigDecimal rightTime = new BigDecimal(1000);
        BigDecimal divide = leftTime.divide(rightTime, 0, BigDecimal.ROUND_HALF_UP);
        return divide.longValue();
    }

    private JSONObject parseResult(JSONArray result) {
        try {
            JSONObject jsonObject = result.getJSONObject(0);
            String type = jsonObject.getString("type");
            if (type.equals("success")) {
                return jsonObject.getJSONObject("result");
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject parseClubResult(int success, int chatType, int chatId, long localSyncTime, long nextSyncTime, int hasMore) {
        try {
            JSONObject resObj = new JSONObject();
            resObj.put("success", success);
            resObj.put("local_sync_time", localSyncTime + "");
            resObj.put("next_sync_time", nextSyncTime + "");
            resObj.put("has_more", hasMore);
            resObj.put("chat_id", chatId);
            resObj.put("chat_type", chatType);
            return resObj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
}
