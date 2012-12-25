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

import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class RoomTest {
	
	@Test
	public void name() {
		Room room = new Room("room");
		Assert.assertEquals(room.name(), "room");
	}

	@Test
	public void sockets() {
		Room chat = new Room("chat");
		
		Socket socket1 = Mockito.mock(Socket.class);
		chat.add(socket1);
		Assert.assertArrayEquals(new Socket[] { socket1 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 1);
		
		chat.add(socket1);
		Assert.assertArrayEquals(new Socket[] { socket1 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 1);

		final Socket socket2 = Mockito.mock(Socket.class);
		chat.add(socket2);
		Assert.assertArrayEquals(new Socket[] { socket1, socket2 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 2);

		chat.remove(socket1);
		Assert.assertArrayEquals(new Socket[] { socket2 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 1);

		chat.clear();
		Assert.assertArrayEquals(new Socket[] {}, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 0);
	}

	@Test
	public void attr() {
		Room room = new Room("room");
		Assert.assertNull(room.get("notfound"));
		
		String data = "data";
		Assert.assertSame(room.set("data", data).get("data"), data);
		Assert.assertEquals(room.replace("data", new Fn.Feedback1<String, String>() {
			@Override
			public String apply(String old) {
				return "new" + old;
			}
		})
		.get("data"), "newdata");
	}

}
