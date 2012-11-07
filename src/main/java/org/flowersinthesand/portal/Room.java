package org.flowersinthesand.portal;

import java.util.Set;

public abstract class Room {

	public static Room find(String name) {
		// TODO
		return null;
	}

	public static Room open(String name) {
		// TODO
		return null;
	}

	public static Room findOrOpen(String name) {
		// TODO
		return null;
	}

	public abstract void close();

	public abstract String name();

	public abstract Room add(Socket socket);

	public abstract Room remove(Socket socket);

	public abstract Set<Socket> sockets();

	public abstract Set<Socket> sockets(Fn.Feedback1<Boolean, Socket> filter);

	public abstract int size();

	public abstract Room clear();

	public abstract Object get(String key);

	public abstract Room set(String key, Object value);

}
