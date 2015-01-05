var cordova = require('cordova');
var exec = require('cordova/exec');

var MqttPlugin = function() {

	// opts must have {url, userName, password, clientID, topics}
	this.subscribe = function(opts, success_cb, error_cb){
		var conConf = [opts.url, opts.userName, opts.password, opts.clientID, opts.topics];
		exec(success_cb, error_cb, "MqttPlugin", "subscribe", conConf);
	};

};

module.exports = MqttPlugin;