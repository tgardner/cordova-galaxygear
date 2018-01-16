package net.trentgardner.cordova.galaxygear;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GalaxyGearPlugin extends CordovaPlugin {
    private final String TAG = GalaxyGearPlugin.class.getSimpleName();

    private GearMessageApi api = null;
    private Intent serviceIntent = null;

    private SparseArray<GearConnection> connections = new SparseArray<GearConnection>();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            api = GearMessageApi.Stub.asInterface(service);
            try {
                api.addListener(messageListener);
                Log.i(TAG, "Listener registered with service");
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to add listener", e);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            api = null;
            Log.i(TAG, "Service connection closed!");
        }
    };

    private GearMessageListener.Stub messageListener = new GearMessageListener.Stub() {

        @Override
        public void onConnect(int connectionId) throws RemoteException {
            Log.d(TAG, "messageListener.onConnect");

            createNewGearConnection(connectionId);
        }

        @Override
        public void onDataReceived(int connectionId, String data)
                throws RemoteException {
            Log.d(TAG, "messageListener.onDataReceived");

            GearConnection connection = connections.get(connectionId);
            if (connection == null)
                createNewGearConnection(connectionId);
            else
                connection.onDataReceived(data);
        }

        @Override
        public void onError(int connectionId, String data)
                throws RemoteException {
            Log.d(TAG, "messageListener.onError");

            GearConnection connection = connections.get(connectionId);
            if (connection != null) {
                connection.onError(data);
                connections.delete(connectionId);
            }
        }

    };

    private void createNewGearConnection(int connectionId) {
        GearConnection connection = new GearConnection(connectionId);
        connections.put(connectionId, connection);

        notifyCallbacksOfConnection(connectionId);

    }

    private class GearConnection {
        private int mHandle;

        private List<CallbackContext> dataCallbacks = new ArrayList<CallbackContext>();
        private List<CallbackContext> errorCallbacks = new ArrayList<CallbackContext>();

        GearConnection(int handle) {
            mHandle = handle;
        }

        void addDataListener(CallbackContext callback) {
            dataCallbacks.add(callback);
        }

        void addErrorListener(CallbackContext callback) {
            errorCallbacks.add(callback);
        }

        void onDataReceived(String data) {
            notifyCallbacks(dataCallbacks, createJSONObject(mHandle, data));
        }

        void onError(String data) {
            notifyCallbacks(errorCallbacks, createJSONObject(mHandle, data));
        }

        private void notifyCallbacks(final List<CallbackContext> callbacks,
                                     final JSONObject data) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    for (CallbackContext context : callbacks) {
                        keepCallback(context, data);
                    }
                }
            });
        }
    }

    private List<CallbackContext> connectCallbacks = new ArrayList<CallbackContext>();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Activity context = cordova.getActivity();
        final String PACKAGE_PREFERENCE = "GearProviderPackage";
        final String packageName = preferences.getString(PACKAGE_PREFERENCE, context.getPackageName());

        Log.d(TAG, "initialize - Watch Package: " + packageName);

        final String PROVIDER_CLASS = "net.trentgardner.cordova.galaxygear.GearProviderService";
        serviceIntent = new Intent();
        serviceIntent.setClassName(packageName, PROVIDER_CLASS);

        Log.d(TAG, "Attempting to start service");
        context.startService(serviceIntent);

        Log.d(TAG, "Attempting to bind to service");
        context.bindService(serviceIntent, serviceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean execute(String action, CordovaArgs args,
                           CallbackContext callbackContext) throws JSONException {

        final String ACTION_ONCONNECT = "onConnect";
        final String ACTION_ONDATARECEIVED = "onDataReceived";
        final String ACTION_ONERROR = "onError";
        final String ACTION_SENDDATA = "sendData";
        if (ACTION_ONCONNECT.equals(action))
            onConnect(callbackContext);
        else if (ACTION_ONDATARECEIVED.equals(action))
            onDataReceived(args, callbackContext);
        else if (ACTION_ONERROR.equals(action))
            onError(args, callbackContext);
        else if (ACTION_SENDDATA.equals(action))
            sendData(args, callbackContext);
        else
            return false;

        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        try {
            Activity context = cordova.getActivity();

            if (api != null)
                api.removeListener(messageListener);

            context.unbindService(serviceConnection);
            context.stopService(serviceIntent);
        } catch (Throwable t) {
            // catch any issues, typical for destroy routines
            // even if we failed to destroy something, we need to continue
            // destroying
            Log.w(TAG, "Failed to unbind from the service", t);
        }

        super.onDestroy();
    }

    private void sendData(final CordovaArgs args,
                          final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "sendData");

        int connectionId = args.getInt(0);
        String data = args.getString(1);
        try {
            if (api != null) {
                api.sendData(connectionId, data);
                callbackContext.success();
            } else {
                callbackContext.error("Service not present");
            }
        } catch (RemoteException e) {
            callbackContext.error(e.getMessage());
        }
    }

    private void onConnect(final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "onConnect");

        connectCallbacks.add(callbackContext);

        // Alert the client of any existing connections
        int key;
        for (int i = 0; i < connections.size(); i++) {
            key = connections.keyAt(i);
            connect(callbackContext, key);
        }
    }

    private void onDataReceived(final CordovaArgs args,
                                final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "onDataReceived");

        int connectionId = args.getInt(0);
        GearConnection connection = connections.get(connectionId);
        if (connection != null) {
            connection.addDataListener(callbackContext);
        } else {
            callbackContext.error("Invalid connection handle");
        }
    }

    private void onError(final CordovaArgs args,
                         final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "onError");

        int connectionId = args.getInt(0);
        GearConnection connection = connections.get(connectionId);
        if (connection != null) {
            connection.addErrorListener(callbackContext);
        } else {
            callbackContext.error("Invalid connection handle");
        }
    }

    private void connect(CallbackContext callbackContext, int connectionId) {
        JSONObject o = createJSONObject(connectionId, null);
        keepCallback(callbackContext, o);
    }

    private void notifyCallbacksOfConnection(final int connectionId) {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                for (CallbackContext context : connectCallbacks) {
                    connect(context, connectionId);
                }
            }
        });
    }

    private void keepCallback(final CallbackContext callbackContext,
                              JSONObject message) {
        PluginResult r = new PluginResult(PluginResult.Status.OK, message);
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);
    }

    private JSONObject createJSONObject(int handle, String data) {
        JSONObject o = new JSONObject();

        try {
            o.put("handle", handle);

            if (data != null)
                o.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return o;
    }
}
