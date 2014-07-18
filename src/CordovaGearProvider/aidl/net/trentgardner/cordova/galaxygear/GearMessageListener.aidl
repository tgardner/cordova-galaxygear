package net.trentgardner.cordova.galaxygear;

interface GearMessageListener {
	void onConnect(int connectionId);
	
	void onDataReceived(int connectionId, String data);
	
	void onError(int connectionId, String data);
}
