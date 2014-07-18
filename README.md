
# Cordova Galaxy Gear Plugin

The plugin has functions that allows your app to have bidirectional communication with Samsung Gear 2 Series Tizen  applications and Cordova applications.

It supports the various Android Samsung models listed [here](http://www.samsung.com/global/microsite/gear/gear2_features.html).

## Usage

1. Configure your tizen application to use the following serviceProfile

        <serviceProfile
            id="/system/cordovagearprovider"
            name="cordovagearprovider"
            role="provider"
            serviceImpl="net.trentgardner.cordova.galaxygear.service.GearProviderService"
            version="1.0"
            serviceLimit="ANY"
	    	serviceTimeout="10">
            <supportedTransports>
                <transport type="TRANSPORT_BT" />
            </supportedTransports>

            <serviceChannel
                id="104"
                dataRate="low"
                priority="low"
                reliability= "enable"/>   
    	</serviceProfile>
A great example of how to do this is the Hello Accessory example from the [SDK](http://developer.samsung.com/samsung-gear) 

2. Add your compiled Tizen widget (.wgt file) to `CordovaGearProvider/assets` and deploy CordovaGearProvider.

3. Add CordovaGearPlugin to your cordova project and follow the example provided.
