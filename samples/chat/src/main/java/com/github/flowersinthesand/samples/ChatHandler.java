package com.github.flowersinthesand.samples;

import java.util.Map;

import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.Handler;
import com.github.flowersinthesand.portal.Name;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.Socket;

@Handler("/chat")
public class ChatHandler {

	@Name("chat")
	Room room;

	@On.open
	public void open(Socket socket) {
		room.add(socket);
	}

	@On.message
	public void message(@Data Map<String, Object> message) {
		room.send("message", message);
	}

}
