package org.pluginporo.mqtt;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.io.StringWriter;
import java.io.PrintWriter;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.StrictMode;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

public class MqttPlugin extends CordovaPlugin implements MqttCallback {

  private static final String LOG_TAG = "MqttPlugin";

  CallbackContext pluginCallbackContext = null;
  MqttClient client = null;
  MqttConnectOptions connOpt;

  String clientID = null;
  String brokerUrl = null;
  String userName = null;
  String password = null;
  ArrayList<String> subTopics = null;

  // args = [url, username, password, clientID, topics[]]
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (android.os.Build.VERSION.SDK_INT >= 11) {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
    }

    if (action.equals("subscribe")) {
      this.setOpts(args);
      this.pluginCallbackContext = callbackContext;
      try{
        this.runClient();
      } catch(Exception e){

      }
      return true;
    }
    else if (action.equals("stop")) {
      callbackContext.success("stopped");
      return true;
    }
    return false;
  }

  @Override
  public void connectionLost(Throwable t) {
    System.out.println("Connection lost!");
    // sendUpdate(getInfo("log", "connection lost"), true);
    try{
      this.runClient();
    } catch(Exception e){

    }
    // code to reconnect to the broker would go here if desired
  }
 
  @Override
  public void deliveryComplete(MqttDeliveryToken token) {
    try {
      System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }
 
  @Override
  public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
    String msg = new String(message.getPayload());
    String tpk = topic.getName();
    System.out.println("-------------------------------------------------");
    System.out.println("| Topic:" + tpk);
    System.out.println("| Message: " + msg);
    System.out.println("-------------------------------------------------");

    sendUpdate(getInfo(tpk, msg), true);
  }

  private void runClient () throws InterruptedException {
    // setup MQTT Client
    connOpt = new MqttConnectOptions();
    connOpt.setCleanSession(true);
    connOpt.setKeepAliveInterval(30);
    connOpt.setUserName(userName);
    connOpt.setPassword(password.toCharArray());
    
    // Connect to Broker
    try {
      // sendUpdate(getInfo("log", "connecting " + brokerUrl), true);
      client = new MqttClient(brokerUrl, clientID, new MemoryPersistence());
      client.setCallback(this);
      client.connect(connOpt);
      // sendUpdate(getInfo("log", "connected to " + brokerUrl), true);
    } catch (Exception e) {
      // sendUpdate(getInfo("log", "err " + getStackTrace(e)), true);
      e.printStackTrace();
      Thread.sleep(5000);
      runClient();
    }
    
    // setup topic
    // MqttTopic topic = client.getTopic(subTopic);
    // sendUpdate(getInfo("create topic  " + subTopic), true);

    // subscribe to topic if subscriber
    try {
      int subQoS = 0;
      for(int i = 0; i < subTopics.size(); i++){
        String tp = (String) subTopics.get(i);
        client.subscribe(tp, subQoS);
      }

      // sendUpdate(getInfo("subscribed topic  " + subTopic), true);
    } catch (Exception e) {
      // sendUpdate(getInfo("err " + getStackTrace(e)), true);
      e.printStackTrace();
    }
  }

  private void disconnect(){
    try {
      // wait to ensure subscribed messages are delivered
      Thread.sleep(5000);
      client.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private JSONObject getInfo(String topic, String content) {
    JSONObject obj = new JSONObject();
    try {
      obj.put("topic", topic);
      obj.put("content", content);
    } catch (JSONException e) {
      Log.e(LOG_TAG, e.getMessage(), e);
    }
    return obj;
  }

  private void sendUpdate(String info, boolean keepCallback) {
    if (this.pluginCallbackContext != null) {
      PluginResult result = new PluginResult(PluginResult.Status.OK, info);
      result.setKeepCallback(keepCallback);
      this.pluginCallbackContext.sendPluginResult(result);
    }
  }

  private void sendUpdate(JSONObject info, boolean keepCallback) {
    if (this.pluginCallbackContext != null) {
      PluginResult result = new PluginResult(PluginResult.Status.OK, info);
      result.setKeepCallback(keepCallback);
      this.pluginCallbackContext.sendPluginResult(result);
    }
  }

  public static String getStackTrace(final Throwable throwable) {
     final StringWriter sw = new StringWriter();
     final PrintWriter pw = new PrintWriter(sw, true);
     throwable.printStackTrace(pw);
     return sw.getBuffer().toString();
  }

  // url, username, password, clientID, topic
  private void setOpts(JSONArray args) throws JSONException{
    this.brokerUrl = (String) args.get(0);
    this.userName = (String) args.get(1);
    this.password = (String) args.get(2);
    this.clientID = (String) args.get(3);

   this.subTopics = new ArrayList<String>();
    JSONArray jsonArray = (JSONArray)args.get(4); 
    if (jsonArray != null) { 
      int len = jsonArray.length();
        for (int i=0;i<len;i++){ 
          this.subTopics.add(jsonArray.get(i).toString());
        } 
    }
   
  }

}
