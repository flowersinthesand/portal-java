package com.github.flowersinthesand.portal.atmosphere;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Fn.Callback;
import com.github.flowersinthesand.portal.Fn.Callback1;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.SocketManager;

public class AtmosphereSocketManager implements SocketManager {

	@Override
	public void setApp(App app) {}

	@Override
	public boolean opened(Socket socket) {
		return false;
	}

	@Override
	public void send(Socket socket, String event, Object data) {}

	@Override
	public void send(Socket socket, String event, Object data, Callback callback) {}

	@Override
	public <A> void send(Socket socket, String event, Object data, Callback1<A> callback) {}

	@Override
	public void close(Socket socket) {}

}
