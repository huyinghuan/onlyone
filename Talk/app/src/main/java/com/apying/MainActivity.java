package com.apying;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends Activity {
    TelephonyManager telephonyManager = null;
    String url = "http://192.168.1.100:3000/chat";
    private final String TAG = "test";
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        setContentView(R.layout.activity_main);
        connectServer();
        checkBaseStatus();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mSocket.disconnect();
    }
    /**
     * 链接服务器**/
    private void connectServer(){
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "connect");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        autoLogin();
                    }
                });
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {}

        }).on("login:fail", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "登录失败");
            }

        }).on("login:success", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "登录成功");
            }

        }).on("message", new Emitter.Listener(){

            @Override
            public void call(final Object... args) {
                Log.d(TAG, "message");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray data= (JSONArray)args[0];
                        int length = data.length();
                        Log.d(TAG, length + "");
                        for(int i=0; i < length; i++){
                            try {
                                Log.d(TAG, data.getInt(i) + "");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
        mSocket.connect();
    }

    /**
     * 获取手机号码和设备id并且进行注册或者登录**/
    private void autoLogin(){
        String IMSI = telephonyManager.getSubscriberId();
        String deviceId = telephonyManager.getDeviceId();
        setTextForInput(IMSI, deviceId);
        mSocket.emit("login", IMSI, deviceId);
    }

    private void setTextForInput(String phoneNumber, String deviceId){
        EditText fPhoneNumber = (EditText)findViewById(R.id.phone_number);
        EditText fDeviceId = (EditText) findViewById(R.id.device_id);
        fPhoneNumber.setText(phoneNumber);
        fDeviceId.setText(deviceId);
    }

    /**
     * 监听按钮事件
     * **/
    public void sendHeartbeat(View view){
        Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //vib.vibrate(400);
    }

    /**
     * 检查基础状态**/
    private void checkBaseStatus(){
        checkConnectionStatus();
        monitorConnectionStatus();
    }

    /**
     * 检查网络状态**/
    private void checkConnectionStatus(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if(!isConnected){
            Log.w(TAG, "无法连接网络");
        }else{
            Log.d(TAG, "连接网络正常");
        }
    }

    /**
     * 监视网络状态变化*/
    private void monitorConnectionStatus(){
        BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkConnectionStatus();
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);
    }
 }
