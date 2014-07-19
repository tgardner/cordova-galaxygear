package net.trentgardner.cordova.galaxygear;

import net.trentgardner.cordova.galaxygear.GearMessageListener;

interface GearMessageApi {
	void sendData(int connectionId, String data);
   
	void addListener(GearMessageListener listener);
	
	void removeListener(GearMessageListener listener);
}
