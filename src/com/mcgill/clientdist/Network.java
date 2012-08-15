package com.mcgill.clientdist;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {
	public static int TCPPort = 55555;
	public static String defaultIP = "192.168.0.140";

	public static final int DISCONNECTED = 0, CONNECTING = 1, CONNECTED = 2;
	
	public static void register(EndPoint endPoint) {
		Kryo kryo = endPoint.getKryo();
		kryo.register(Signal.class);
		kryo.register(Handshake.class);
		kryo.register(String[].class);
	}

	public static class Signal {
		public int volume;
		public long timems;
		public String id;
		public boolean heard;
	}
	
	public static class Handshake {
	}
}