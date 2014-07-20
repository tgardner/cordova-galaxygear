
# Cordova Galaxy Gear Plugin

The plugin has functions that allows your app to have bidirectional communication with Samsung Gear 2 Series Tizen  applications and Cordova applications.

It supports the various Android Samsung models listed [here](http://www.samsung.com/global/microsite/gear/gear2_features.html).

# Installation
`cordova plugin add net.trentgardner.cordova.galaxygear`

## Usage

1. Add the plugin to your project.

2. Configure your tizen application to use the following serviceProfile

        <serviceProfile
            id="/system/cordovagearprovider"
            name="cordovagearprovider"
            role="consumer"
            version="2.0" >
            
            <supportedTransports>
                <transport type="TRANSPORT_BT" />
            </supportedTransports>

            <serviceChannel
                id="104"
                dataRate="low"
                priority="low"
                reliability="enable" >
            </serviceChannel>
        </serviceProfile>
A great example of how to do this is the Hello Accessory example from the [SDK](http://developer.samsung.com/samsung-gear) 

## Example
``` javascript
GalaxyGear.onConnect(function(e) {
	alert("Connection Successfully Established - handle: " + e.handle);
	
	GalaxyGear.onDataReceived(e.handle, function(e) {
		alert("Data received - handle: " + e.handle + " data: "+ e.data);
	});
	
	GalaxyGear.sendData(e.handle, "Hello From Cordova!");
});
```
