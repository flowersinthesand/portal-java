package org.flowersinthesand.portal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.flowersinthesand.portal.Fn.Feedback1;

public class DefaultRoom implements Room {

	private String name;
	private Set<Socket> sockets = new CopyOnWriteArraySet<Socket>();
	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();

	public DefaultRoom(String name) {
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Room add(Socket socket) {
		sockets.add(socket);
		return this;
	}

	@Override
	public Room remove(Socket socket) {
		sockets.remove(socket);
		return this;
	}

	@Override
	public Set<Socket> sockets() {
		return Collections.unmodifiableSet(sockets);
	}

	@Override
	public Set<Socket> sockets(Feedback1<Boolean, Socket> filter) {
		Set<Socket> filtered = new LinkedHashSet<Socket>();
		for (Socket socket : sockets()) {
			if (filter.apply(socket)) {
				filtered.add(socket);
			}
		}
		return filtered;
	}

	@Override
	public int size() {
		return sockets.size();
	}

	@Override
	public Room clear() {
		sockets.clear();
		return this;
	}

	@Override
	public Object get(String key) {
		return attrs.get(key);
	}

	@Override
	public Room set(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

}
