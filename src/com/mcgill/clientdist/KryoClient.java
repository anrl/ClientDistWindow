package com.mcgill.clientdist;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.os.Handler;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.mcgill.clientdist.Network.Handshake;
import com.mcgill.clientdist.Network.Signal;

public class KryoClient {
	private Handler handler; //handler to send data to the UI
	private ClientDistActivity activity; //object to connect to the main activity
	
	private Client client;
	public String ipAddress;
	private Signal lastSignal;
	
	private boolean shakedHands = false;

	public KryoClient(String ipAddr, ClientDistActivity parent) {
    	activity = parent;
        handler = new Handler();
        
		ipAddress = ipAddr;
		client = new Client();
		//Log.TRACE();
		Network.register(client);


		client.addListener(new Listener() {
			public void connected (Connection connection) {
				postConnectionStatus(Network.CONNECTED);
//				Log.e("Client", "Connected!");
			}

			public void received (Connection connection, Object object) {
				if (object instanceof Signal) {
					System.out.println("Received Signal.");
					lastSignal = (Signal)object;
				}
				if (object instanceof Handshake) {
					System.out.println("Received handshake.");
					postWaitMessage();
					shakedHands = false;
					shakeHands();
				}
			}

			public void disconnected (Connection connection) {
				postConnectionStatus(Network.DISCONNECTED);
//				Log.e("Client", "Disconnected");
			}
		});
		
		client.start();
	}

	public void sendResponse(String androidID, boolean heard) {
		if (client == null)
			return;
		lastSignal.id = androidID;
		lastSignal.heard = heard;
		client.sendTCP(lastSignal);
	}
	
	public void shakeHands() {
		if (client == null || shakedHands) 
			return;
		System.out.println("Shaking hands.");
		shakedHands = true;
		client.sendTCP(new Handshake());
	}

	public void close() {
		client.close();
		client.stop();
	}
	
	public void connect() throws UnknownHostException {
		InetAddress addr;
//		if (ipAddress == "")
//			addr = client.discoverHost(Network.UDPPort, 5000);
//		else
			addr = InetAddress.getByName(ipAddress);

		try {
			client.connect(2000, addr, Network.TCPPort);
		} catch (IOException e) {
//			Log.e("Error", "Server down.");
			postConnectionStatus(Network.DISCONNECTED);
		}
	}

	public void reconnect() {
		try {
			client.reconnect();
		} catch (IOException e) {
//			Log.e("Error", "Server down.");
			postConnectionStatus(Network.DISCONNECTED);
		}
	}
	
	public boolean isConnected() {
		return client.isConnected();
	}

	private void postConnectionStatus(final int data) {
		handler.post(new Runnable() {
			public void run() {
				activity.receiveConnectionStatus(data);
			}
		});
    }

	private void postWaitMessage() {
		handler.post(new Runnable() {
			public void run() {
				activity.waitForData();
			}
		});
    }
}