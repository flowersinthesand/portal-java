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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

public class RoomTest {
	
	@Test
	public void name() {
		Room room = new Room("room", new LinkedHashMap<String, Room>());
		Assert.assertEquals(room.name(), "room");
	}

	@Test
	public void sockets() {
		Room chat = new Room("chat", new LinkedHashMap<String, Room>());
		
		Socket socket1 = Mockito.mock(Socket.class);
		Mockito.when(socket1.opened()).thenReturn(true);
		chat.add(socket1);
		Assert.assertArrayEquals(new Socket[] { socket1 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 1);
		
		chat.add(socket1);
		Assert.assertArrayEquals(new Socket[] { socket1 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 1);

		Socket socket2 = Mockito.mock(Socket.class);
		Mockito.when(socket2.opened()).thenReturn(true);
		chat.add(socket2);
		Assert.assertArrayEquals(new Socket[] { socket1, socket2 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 2);

		chat.remove(socket1);
		Assert.assertArrayEquals(new Socket[] { socket2 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 1);
	}
	
	@Test
	public void rooms() {
		Room chat = new Room("chat", new LinkedHashMap<String, Room>());
		Room hollowjan = new Room("hollowjan", new LinkedHashMap<String, Room>());
		Room envy = new Room("envy", new LinkedHashMap<String, Room>());
		
		Socket socket1 = Mockito.mock(Socket.class);
		Socket socket2 = Mockito.mock(Socket.class);
		Socket socket3 = Mockito.mock(Socket.class);
		Mockito.when(socket1.opened()).thenReturn(true);
		Mockito.when(socket2.opened()).thenReturn(true);
		Mockito.when(socket3.opened()).thenReturn(true);
				
		chat.add(socket1).add(socket2);
		hollowjan.add(socket2).add(socket3);
		envy.add(socket1).add(socket3);
		
		chat.add(hollowjan);
		Assert.assertEquals(chat.size(), 3);
		
		chat.remove(envy);
		Assert.assertEquals(chat.size(), 1);
	}
	
	@Test
	public void sending() {
		Room chat = new Room("chat", new LinkedHashMap<String, Room>());
		final List<Object> executed = new ArrayList<Object>(); 
		Answer<Object> increment = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				executed.add(1);
				return null;
			}
		};
		
		Socket socket1 = Mockito.mock(Socket.class);
		Mockito.when(socket1.opened()).thenReturn(true);

		Socket socket2 = Mockito.mock(Socket.class);
		Mockito.when(socket2.opened()).thenReturn(true);
		
		chat.add(socket1).add(socket2);
		
		Mockito.when(socket1.send("e", null)).then(increment);
		Mockito.when(socket2.send("e", null)).then(increment);
		
		String data = "data";
		Mockito.when(socket1.send("ed", data)).then(increment);
		Mockito.when(socket2.send("ed", data)).then(increment);
		
		chat.send("e");
		Assert.assertEquals(executed.size(), 2);
		
		executed.clear();
		chat.send("ed", data);
		Assert.assertEquals(executed.size(), 2);
	}
	
	@Test
	public void closing() {
		Map<String, Room> rooms = new LinkedHashMap<String, Room>();
		Room chat = new Room("chat", rooms);
		final List<Object> executed = new ArrayList<Object>(); 
		Answer<Object> increment = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				executed.add(1);
				return null;
			}
		};

		Socket socket1 = Mockito.mock(Socket.class);
		Mockito.when(socket1.opened()).thenReturn(true);

		Socket socket2 = Mockito.mock(Socket.class);
		Mockito.when(socket2.opened()).thenReturn(true);

		chat.add(socket1).add(socket2).set("data", new Object());

		Mockito.when(socket1.close()).then(increment);
		Mockito.when(socket2.close()).then(increment);

		chat.close();
		Assert.assertEquals(executed.size(), 2);
		Assert.assertEquals(chat.size(), 0);
		Assert.assertNull(chat.get("data"));
		Assert.assertEquals(rooms.size(), 0);
	}

	@Test
	public void attr() {
		Room room = new Room("room", new LinkedHashMap<String, Room>());
		Assert.assertNull(room.get("notfound"));
		
		String data = "data";
		Assert.assertSame(room.set("data", data).get("data"), data);
	}

}
