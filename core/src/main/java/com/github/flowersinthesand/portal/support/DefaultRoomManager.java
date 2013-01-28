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

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.spi.RoomManager;

@Bean("com.github.flowersinthesand.portal.spi.RoomManager")
public class DefaultRoomManager implements RoomManager {

	private Map<String, Room> rooms = new ConcurrentHashMap<String, Room>();

	@Override
	public Set<Room> all() {
		return Collections.unmodifiableSet(new LinkedHashSet<Room>(rooms.values()));
	}

	@Override
	public Room open(String name) {
		Room room = new Room(name);
		rooms.put(name, room);
		return room;
	}

	@Override
	public Room find(String name) {
		return name == null ? null : rooms.get(name);
	}

	@Override
	public void close(String name) {
		find(name).close();
	}

}
