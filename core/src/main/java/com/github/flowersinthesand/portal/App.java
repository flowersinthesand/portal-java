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

public class App implements Serializable {

	private final static long serialVersionUID = 1728426808062835850L;
	private final static String FIRST = App.class.getName() + ".first";
	private final static ConcurrentMap<String, App> apps = new ConcurrentHashMap<String, App>();

	public static App find() {
		return apps.get(FIRST);
	}

	public static App find(String name) {
		return name == null ? null : apps.get(name);
	}

	static App add(App app) {
		apps.putIfAbsent(FIRST, app);
		apps.put(app.name(), app);
		return app;
	}

	private String name;
	private Map<String, Object> attrs = new ConcurrentHashMap<String, Object>();
	private Map<String, Room> rooms = new ConcurrentHashMap<String, Room>();
	private Map<Class<?>, Object> beans = new ConcurrentHashMap<Class<?>, Object>();

	public App(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public Object get(String key) {
		return key == null ? null : attrs.get(key);
	}

	public App set(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

	public Map<String, Room> rooms() {
		return rooms;
	}

	public Room room(String name) {
		if (rooms.containsKey(name)) {
			return rooms.get(name);
		}

		rooms.put(name, new Room(name, this));
		return rooms.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T bean(Class<? extends T> clazz) {
		if (beans.containsKey(clazz)) {
			return (T) beans.get(clazz);
		}

		for (Object instance : beans.values()) {
			if (clazz.isAssignableFrom(instance.getClass())) {
				return (T) instance;
			}
		}

		return null;
	}

	App bean(Class<?> clazz, Object t) {
		beans.put(clazz, t);
		return this;
	}

}
