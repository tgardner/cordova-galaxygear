
# Cordova Galaxy Gear Plugin

The plugin has functions that allows your app to have bidirectional communication with Samsung Gear 2 Series Tizen  applications and Cordova applications.

It supports the various Android Samsung models listed [here](http://www.samsung.com/global/microsite/gear/gear2_features.html).

## Installation
With Cordova CLI, from npm:
```
$ cordova plugin add cordova-galaxygear
$ cordova prepare
```

## Usage

1. Install the plugin in your Cordova project.

2. Add a `res/xml/assessoryservices.xml` file to your Tizen project with the following
  ````xml
  <resources>
      <application name="CordovaGearConsumer" >
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
      </application>
  </resources>
  ````

3. Modify the `config.xml` file in your Tizen project and add the following under the `<widget>` node
  ````xml
  <tizen:metadata key="AccessoryServicesLocation" value="res/xml/accessoryservices.xml"/>
  <tizen:privilege name="http://developer.samsung.com/privilege/accessoryprotocol"/>
  ````
A great example of this is the Hello Accessory example from the [SDK](http://developer.samsung.com/samsung-gear) 

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
