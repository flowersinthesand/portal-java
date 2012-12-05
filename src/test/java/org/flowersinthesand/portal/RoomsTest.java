package org.flowersinthesand.portal;

import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class RoomsTest {

	@Test
	public void roomss() {
		Assert.assertNull(Rooms.get("/notfound"));
		Rooms rooms = new Rooms();
		Rooms.add("/ok", rooms);
		Assert.assertSame(Rooms.get("/ok"), rooms);
	}

	@Test
	public void finding() {
		Rooms rooms = new Rooms();
		Assert.assertNull(rooms.find("/notfound"));
		rooms.open("/ok");
		Assert.assertNotNull(rooms.find("/ok"));
		rooms.close("/ok");
		Assert.assertNull(rooms.find("/ok"));
		Assert.assertNotNull(rooms.findOrOpen("/ok"));
	}

	@Test
	public void room() {
		Room chat = new Rooms().open("chat");
		Assert.assertEquals(chat.name(), "chat");

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
		
		Assert.assertArrayEquals(new Socket[] { socket1 }, chat.sockets(new Fn.Feedback1<Boolean, Socket>() {
			@Override
			public Boolean apply(Socket s) {
				return s != socket2;
			}
		})
		.toArray(new Socket[] {}));

		chat.remove(socket1);
		Assert.assertArrayEquals(new Socket[] { socket2 }, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 1);

		chat.clear();
		Assert.assertArrayEquals(new Socket[] {}, chat.sockets().toArray(new Socket[] {}));
		Assert.assertEquals(chat.size(), 0);

		Assert.assertNull(chat.get("notfound"));
		Assert.assertSame(chat.set("socket1", socket1).get("socket1"), socket1);
	}

}
