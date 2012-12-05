package org.flowersinthesand.portal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rooms {

	private final static Map<String, Rooms> roomss = new ConcurrentHashMap<String, Rooms>();

	public static Rooms get(String app) {
		return roomss.get(app);
	}

	static void add(String app, Rooms rooms) {
		roomss.put(app, rooms);
	}

	private final Logger logger = LoggerFactory.getLogger(Rooms.class);
	private final Map<String, Room> rooms = new ConcurrentHashMap<String, Room>();

	public Room find(String name) {
		return rooms.get(name);
	}

	public Room open(String name) {
		rooms.put(name, new DefaultRoom(name));
		return find(name);
	}

	public void close(String name) {
		rooms.remove(name);
	}

	public Room findOrOpen(String name) {
		Room room = find(name);
		return room == null ? open(name) : room;
	}

}
