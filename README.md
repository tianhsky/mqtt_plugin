Cordova MQTT Plugin
==============

MQTT plugin for Cordova / PhoneGap.

## Supported Platforms

- Android

## How to use

### Install

	cordova plugin add org.pluginporo.mqtt_plugin

### Subscribe

	window.mqtt = new navigator.mqtt_plugin();
	window.mqtt.subscribe({
			url: "tcp://host:5672/",
			userName: "username",
			password: "password",
			clientID: "clientid",
			topics: ["topic1", "topic2"]
		},
		function(data){
			// success callback
			console.log(data);
		},
		function(err){
			// error callback
		}
	);