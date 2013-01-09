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
package com.github.flowersinthesand.portal;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.SocketManager;

public class App implements Serializable {

	public final static String NAME = App.class.getName() + ".name";
	public final static String DISPATCHER = App.class.getName() + ".dispatcher";
	public final static String SOCKET_MANAGER = App.class.getName() + ".socketManager";
	public final static String ROOMS = App.class.getName() + ".rooms";
	public final static String FIRST = App.class.getName() + ".first";

	private final static long serialVersionUID = 1728426808062835850L;
	private final static ConcurrentMap<String, App> apps = new ConcurrentHashMap<String, App>();

	public static App find() {
		return apps.get(FIRST);
	}

	public static App find(String name) {
		return name == null ? null : apps.get(name);
	}

	static void add(String name, App app) {
		apps.putIfAbsent(FIRST, app);
		apps.put(name, app);
	}

	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();

	public App() {
		attrs.put(ROOMS, new ConcurrentHashMap<String, Room>());
	}

	public Object get(String key) {
		return key == null ? null : attrs.get(key);
	}

	public App set(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

	public String name() {
		return (String) attrs.get(NAME);
	}

	public Dispatcher dispatcher() {
		return (Dispatcher) attrs.get(DISPATCHER);
	}

	public SocketManager socketManager() {
		return (SocketManager) attrs.get(SOCKET_MANAGER);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Room> rooms() {
		return (Map<String, Room>) attrs.get(ROOMS);
	}

	public Room room(String name) {
		if (rooms().containsKey(name)) {
			return rooms().get(name);
		}

		rooms().put(name, new Room(name, this));
		return rooms().get(name);
	}

}
