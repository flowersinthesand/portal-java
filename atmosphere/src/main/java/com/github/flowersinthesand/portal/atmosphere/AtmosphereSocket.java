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
package com.github.flowersinthesand.portal.atmosphere;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.spi.SocketManager;

public class AtmosphereSocket extends Socket {

	private AtomicInteger eventId = new AtomicInteger(0);
	private Set<Map<String, Object>> cache = new CopyOnWriteArraySet<Map<String, Object>>();

	public AtmosphereSocket(String query, SocketManager manager) {
		super(query, manager);
	}

	public String id() {
		return param("id");
	}

	public AtomicInteger eventId() {
		return eventId;
	}

	public Set<Map<String, Object>> cache() {
		return cache;
	}

	public Map<String, Object> cache(Map<String, Object> message) {
		if (param("transport").startsWith("longpoll")) {
			cache.add(message);
		}
		return message;
	}

}
