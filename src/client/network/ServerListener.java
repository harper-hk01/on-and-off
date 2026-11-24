package client.network;

import common.Message;

public interface ServerListener {
	public void onMessage(Message msg);
	public void onDisconnected();
}
