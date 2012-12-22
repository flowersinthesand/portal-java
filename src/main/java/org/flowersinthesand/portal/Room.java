/*
 * Copyright 2012 Donghwan Kim
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
package org.flowersinthesand.portal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.flowersinthesand.portal.Fn.Feedback1;

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

	public Room add(Socket socket) {
		sockets.add(socket);
		return this;
	}

	public Room remove(Socket socket) {
		sockets.remove(socket);
		return this;
	}

	public Set<Socket> sockets() {
		return Collections.unmodifiableSet(sockets);
	}

	public Set<Socket> sockets(Feedback1<Boolean, Socket> filter) {
		Set<Socket> filtered = new LinkedHashSet<Socket>();
		for (Socket socket : sockets()) {
			if (filter.apply(socket)) {
				filtered.add(socket);
			}
		}
		return filtered;
	}

	public int size() {
		return sockets.size();
	}

	public Room clear() {
		sockets.clear();
		return this;
	}

	public Object get(String key) {
		return attrs.get(key);
	}

	public Room set(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

}
