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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class App implements Serializable {

	public final static String NAME = "org.flowersinthesand.portal.App.name";
	public final static String EVENTS = "org.flowersinthesand.portal.App.events";
	public final static String SOCKETS = "org.flowersinthesand.portal.App.sockets";
	public final static String ROOMS = "org.flowersinthesand.portal.App.rooms";
	public final static String FIRST = "org.flowersinthesand.portal.App.first";

	private final static long serialVersionUID = 1728426808062835850L;
	private final static ConcurrentMap<String, App> apps = new ConcurrentHashMap<String, App>();

	public static App find() {
		return apps.get(FIRST);
	}

	public static App find(String name) {
		return name == null ? null : apps.get(name);
	}

	static synchronized void add(String name, App app) {
		apps.putIfAbsent(FIRST, app);
		apps.put(name, app);
	}

	public static void clear() {
		apps.clear();
	}

	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();

	public App() {
		attrs.put(ROOMS, new ConcurrentHashMap<String, Room>());
	}

	public Object get(String key) {
		return attrs.get(key);
	}

	public App set(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

	public String name() {
		return (String) attrs.get(NAME);
	}

	public Events events() {
		return (Events) attrs.get(EVENTS);
	}

	public Sockets sockets() {
		return (Sockets) attrs.get(SOCKETS);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Room> rooms() {
		return (Map<String, Room>) attrs.get(ROOMS);
	}

	public Room findRoom(String name) {
		return rooms().get(name);
	}

	public Room openRoom(String name) {
		rooms().put(name, new Room(name));
		return room(name);
	}

	public Room room(String name) {
		Room room = findRoom(name);
		return room == null ? openRoom(name) : room;
	}

}
