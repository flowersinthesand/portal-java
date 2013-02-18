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
package com.github.flowersinthesand.portal.support;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.spi.RoomFactory;

@Bean("roomFactory")
public class DefaultRoomFactory implements RoomFactory {

	private Map<String, Room> rooms = new ConcurrentHashMap<String, Room>();

	@Override
	public Set<Room> all() {
		return Collections.unmodifiableSet(new LinkedHashSet<Room>(rooms.values()));
	}

	@Override
	public Room open(String name) {
		if (rooms.containsKey(name)) {
			throw new IllegalStateException("Room '" + name + "' already exists");
		}

		Room room = new DefaultRoom(name);
		rooms.put(name, room);

		return room;
	}

	@Override
	public Room find(String name) {
		return rooms.get(name);
	}

	@Override
	public void remove(String name) {
		rooms.remove(name);
	}

	static class DefaultRoom implements Room {

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
		public Object get(String key) {
			return key == null ? null : attrs.get(key);
		}

		@Override
		public Room set(String key, Object value) {
			attrs.put(key, value);
			return this;
		}

		@Override
		public Room add(Socket... sockets) {
			for (Socket socket : sockets) {
				if (socket.opened()) {
					this.sockets.add(socket);
				}
			}
			return this;
		}

		@Override
		public Room add(Room room) {
			return add(room.sockets().toArray(new Socket[] {}));
		}

		@Override
		public Room in(Socket... sockets) {
			return new DefaultRoom(name + ".in").add(this).add(sockets);
		}

		@Override
		public Room in(Room room) {
			return in(room.sockets().toArray(new Socket[] {}));
		}

		@Override
		public Room remove(Socket... sockets) {
			for (Socket socket : sockets) {
				this.sockets.remove(socket);
			}
			return this;
		}

		@Override
		public Room remove(Room room) {
			return remove(room.sockets().toArray(new Socket[] {}));
		}

		@Override
		public Room out(Socket... sockets) {
			return new DefaultRoom(name + ".out").add(this).remove(sockets);
		}

		@Override
		public Room out(Room room) {
			return out(room.sockets().toArray(new Socket[] {}));
		}

		@Override
		public Room send(String event) {
			return send(event, null);
		}

		@Override
		public Room send(String event, Object data) {
			for (Socket s : sockets) {
				s.send(event, data);
			}
			return this;
		}

		@Override
		public Set<Socket> sockets() {
			return Collections.unmodifiableSet(sockets);
		}

		@Override
		public int size() {
			return sockets.size();
		}

		@Override
		public Room close() {
			for (Socket s : sockets) {
				s.close();
			}
			sockets.clear();
			attrs.clear();
			return this;
		}

		boolean closed() {
			return sockets.isEmpty() && attrs.isEmpty();
		}

	}

}
