package net.trentgardner.cordova.galaxygear;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.trentgardner.cordova.galaxygear.GearMessageApi;
import net.trentgardner.cordova.galaxygear.GearMessageListener;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

public class GearProviderService extends SAAgent {
	public static final String TAG = GearProviderService.class.getSimpleName();

	public Context mContext = null;

	public static final int SERVICE_CONNECTION_RESULT_OK = 0;

	public static final int GALAXY_GEAR_CHANNEL_ID = 104;

	private SparseArray<GalaxyGearProviderConnection> mConnectionsMap = new SparseArray<GalaxyGearProviderConnection>();

	private List<GearMessageListener> listeners = new ArrayList<GearMessageListener>();

	private final GearMessageApi.Stub apiEndpoint = new GearMessageApi.Stub() {
		@Override
		public void sendData(int connectionId, String data)
				throws RemoteException {
			Log.d(TAG, "GearMessageApi.sendData");

			final byte[] message = data.getBytes();
			
			synchronized (mConnectionsMap) {
				final GalaxyGearProviderConnection connection = mConnectionsMap.get(connectionId);

				if (connection == null) {
					Log.e(TAG, "Connection handler not found!");
					return;
				}
				
				new Thread(new Runnable() {
					public void run() {
						try {
							connection.send(GALAXY_GEAR_CHANNEL_ID, message);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		}

		@Override
		public void removeListener(GearMessageListener listener)
				throws RemoteException {
			Log.d(TAG, "GearMessageApi.removeListener");

			synchronized (listeners) {
				listeners.remove(listener);
			}
		}

		@Override
		public void addListener(final GearMessageListener listener)
				throws RemoteException {
			Log.d(TAG, "GearMessageApi.addListener");

			synchronized (listeners) {
				listeners.add(listener);
			}

			// Tell the new listener of any existing connections
			synchronized (mConnectionsMap) {
				int key = 0;
				for (int i = 0; i < mConnectionsMap.size(); i++) {
					key = mConnectionsMap.keyAt(i);
					Log.d(TAG, Integer.toString(key));
					listener.onConnect(key);
				}
			}
		}
	};

	public GearProviderService() {
		super(TAG, GalaxyGearProviderConnection.class);
	}

	public class GalaxyGearProviderConnection extends SASocket {
		private int mConnectionId;
		private final String CONNECTION_TAG = TAG + "."
				+ GalaxyGearProviderConnection.class.getSimpleName();

		public GalaxyGearProviderConnection() {
			super(GalaxyGearProviderConnection.class.getName());
		}
		
		@Override
		public void onError(int channelId, String errorString, int error) {
			final String message = "Connection is not alive ERROR: "
					+ errorString + "  " + error;
			onError(message);
		}

		@Override
		public void onReceive(int channelId, byte[] data) {
			Log.d(CONNECTION_TAG, "onReceive");

			String sData = new String(data);

			synchronized (listeners) {
				for (GearMessageListener listener : listeners) {
					try {
						listener.onDataReceived(mConnectionId, sData);
					} catch (RemoteException e) {
						Log.w(TAG, "Failed to notify listener " + listener, e);
					}
				}
			}
		}

		@Override
		protected void onServiceConnectionLost(int errorCode) {
			final String message = "onServiceConectionLost for peer = "
					+ mConnectionId + "error code =" + errorCode;
			onError(message);

			if (mConnectionsMap != null) {
				mConnectionsMap.remove(mConnectionId);
			}
		}
		
		private void onError(String data) {
			Log.e(CONNECTION_TAG, data);
			
			synchronized (listeners) {
				for (GearMessageListener listener : listeners) {
					try {
						listener.onError(mConnectionId, data);
					} catch (RemoteException e) {
						Log.w(TAG, "Failed to notify listener " + listener, e);
					}
				}
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate");

		SA mAccessory = new SA();
		try {
			mAccessory.initialize(this);
		} catch (SsdkUnsupportedException e) {
			// Error Handling
		} catch (Exception e1) {
			Log.e(TAG, "Cannot initialize Accessory package.");
			e1.printStackTrace();
			/*
			 * Your application can not use Accessory package of Samsung Mobile
			 * SDK. You application should work smoothly without using this SDK,
			 * or you may want to notify user and close your app gracefully
			 * (release resources, stop Service threads, close UI thread, etc.)
			 */
			stopSelf();
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");

		return apiEndpoint;
	}

	@Override
	protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
		Log.d(TAG, "acceptServiceConnectionRequest");
		acceptServiceConnectionRequest(peerAgent);
	}

	@Override
	protected void onFindPeerAgentResponse(SAPeerAgent arg0, int arg1) {
		Log.d(TAG, "onFindPeerAgentResponse  arg1 =" + arg1);
	}

	@Override
	protected void onServiceConnectionResponse(SASocket thisConnection,
			int result) {
		if (result == CONNECTION_SUCCESS) {
			if (thisConnection != null) {
				GalaxyGearProviderConnection myConnection = (GalaxyGearProviderConnection) thisConnection;

				if (mConnectionsMap == null) {
					mConnectionsMap = new SparseArray<GalaxyGearProviderConnection>();
				}

				myConnection.mConnectionId = (int) (System.currentTimeMillis() & 255);

				Log.d(TAG, "onServiceConnection connectionID = "
						+ myConnection.mConnectionId);

				mConnectionsMap.put(myConnection.mConnectionId, myConnection);
				Log.i(TAG, "Connection Success");

				synchronized (listeners) {
					for (GearMessageListener listener : listeners) {
						try {
							listener.onConnect(myConnection.mConnectionId);
						} catch (RemoteException e) {
							Log.w(TAG, "Failed to notify listener " + listener, e);
						}
					}
				}
			} else {
				Log.e(TAG, "SASocket object is null");
			}
		} else if (result == CONNECTION_ALREADY_EXIST) {
			Log.e(TAG, "onServiceConnectionResponse, CONNECTION_ALREADY_EXIST");
		} else {
			Log.e(TAG, "onServiceConnectionResponse result error =" + result);
		}
	}
}