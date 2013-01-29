/*
 * Copyright 2012-2013 Donghwan Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.flowersinthesand.portal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class Room {

	private String name;
	private Set<Socket> sockets = new CopyOnWriteArraySet<Socket>();
	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();

	public Room(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public Object get(String key) {
		return key == null ? null : attrs.get(key);
	}

	public Room set(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

	public Room add(Socket... sockets) {
		for (Socket socket : sockets) {
			if (socket.opened()) {
				this.sockets.add(socket);
			}
		}
		return this;
	}

	public Room add(Room room) {
		return add(room.sockets.toArray(new Socket[] {}));
	}

	public Room in(Socket... sockets) {
		return new Room(name + ".in").add(this).add(sockets);
	}

	public Room remove(Socket... sockets) {
		for (Socket socket : sockets) {
			this.sockets.remove(socket);
		}
		return this;
	}

	public Room remove(Room room) {
		return remove(room.sockets.toArray(new Socket[] {}));
	}

	public Room out(Socket... sockets) {
		return new Room(name + ".out").add(this).remove(sockets);
	}

	public Room send(String event) {
		return send(event, null);
	}

	public Room send(String event, Object data) {
		for (Socket s : sockets) {
			s.send(event, data);
		}
		return this;
	}

	public Set<Socket> sockets() {
		return Collections.unmodifiableSet(sockets);
	}

	public int size() {
		return sockets.size();
	}

	public Room close() {
		for (Socket s : sockets) {
			s.close();
		}
		sockets.clear();
		attrs.clear();
		return this;
	}

}
