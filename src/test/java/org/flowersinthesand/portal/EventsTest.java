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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowersinthesand.portal.Events;
import org.flowersinthesand.portal.Fn;
import org.flowersinthesand.portal.Events.Invoker;
import org.flowersinthesand.portal.Fn.Callback;
import org.flowersinthesand.portal.Fn.Callback1;
import org.flowersinthesand.portal.Socket;
import org.flowersinthesand.portal.handler.DataBean;
import org.flowersinthesand.portal.handler.EventsHandler;
import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class EventsTest {

	String app = "/events";

	@Test
	public void staticBinding() throws SecurityException, NoSuchMethodException,
			InstantiationException, IllegalAccessException {
		EventsHandler h = new EventsHandler();

		Events events = new Events();
		events.on(app, "load", h, h.getClass().getMethod("onLoad"));

		Map<String, Map<String, Set<Invoker>>> invokers = events.invokers();
		Assert.assertNotNull(invokers.get(app));

		Map<String, Set<Invoker>> event = invokers.get(app);
		Assert.assertNotNull(event.get("load"));
	}

	@Test
	public void dynamicBinding() {
		Events events = new Events();
		events.on(app, "e1", null, new Fn.Callback() {
			@Override
			public void call() {}
		});
		events.on(app, "e2", null, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {}
		});
		events.on(app, "e3", null, new Fn.Callback2<Object, Fn.Callback>() {
			@Override
			public void call(Object arg1, Callback reply) {}
		});
		events.on(app, "e4", null, new Fn.Callback2<Object, Fn.Callback1<Object>>() {
			@Override
			public void call(Object arg1, Callback1<Object> reply) {}
		});

		Map<String, Map<String, Set<Invoker>>> invokers = events.invokers();
		Assert.assertNotNull(invokers.get(app));

		Map<String, Set<Invoker>> event = invokers.get(app);
		Assert.assertNotNull(event.get("e1"));
		Assert.assertNotNull(event.get("e2"));
		Assert.assertNotNull(event.get("e3"));
		Assert.assertNotNull(event.get("e4"));
	}

	@Test
	public void staticFiring() throws SecurityException, NoSuchMethodException {
		EventsHandler h = new EventsHandler();
		Class<?> clazz = h.getClass(); 
		Socket socket = Mockito.mock(Socket.class);
		
		Map<String, Object> before = new LinkedHashMap<String, Object>();
		before.put("number", 100);
		before.put("string", "String");
		final DataBean after = new DataBean();
		after.setNumber(100);
		after.setString("String");

		Events events = new Events();
		events.on(app, "socket", h, clazz.getMethod("onSocket", Socket.class));
		events.fire(app, "socket", socket);
		Assert.assertArrayEquals(new Object[] { socket }, h.args);

		events.on(app, "data", h, clazz.getMethod("onData", DataBean.class));
		events.fire(app, "data", socket, before);
		Assert.assertArrayEquals(new Object[] { after }, h.args);

		events.on(app, "repli", h, clazz.getMethod("onRepli", Fn.Callback.class));
		events.fire(app, "repli", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertNull(arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback);

		events.on(app, "repli-data", h, clazz.getMethod("onRepliData", Fn.Callback1.class, DataBean.class));
		events.fire(app, "repli-data", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertEquals(after, arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback1);

		events.on(app, "socket-data-repli", h, clazz.getMethod("onSocketDataRepli", Socket.class, DataBean.class, Fn.Callback1.class));
		events.fire(app, "socket-data-repli", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertEquals(after, arg1);
			}
		});
		Assert.assertSame(socket, h.args[0]);
		Assert.assertEquals(after, h.args[1]);
		Assert.assertTrue(h.args[2] instanceof Fn.Callback1);
	}

	@Test
	public void dynamicFiring() {
		final List<Object> theNumberOfAssertions = new ArrayList<Object>();
		final Socket socket = Mockito.mock(Socket.class);
		final Socket intruder = Mockito.mock(Socket.class);
		
		Map<String, Object> before = new LinkedHashMap<String, Object>();
		before.put("number", 100);
		before.put("string", "String");
		final DataBean after = new DataBean();
		after.setNumber(100);
		after.setString("String");

		Events events = new Events();
		events.on(app, "signal", socket, new Fn.Callback() {
			@Override
			public void call() {
				theNumberOfAssertions.add(null);
				Assert.assertTrue(true);
			}
		});
		events.on(app, "signal", intruder, new Fn.Callback() {
			@Override
			public void call() {
				theNumberOfAssertions.add(null);
				Assert.assertTrue(false);
			}
		});
		events.fire(app, "signal", socket);

		events.on(app, "data", socket, new Fn.Callback1<DataBean>() {
			@Override
			public void call(DataBean arg1) {
				theNumberOfAssertions.add(null);
				Assert.assertEquals(arg1, after);
			}
		});
		events.fire(app, "data", socket, before);
		
		events.on(app, "repli1", socket, new Fn.Callback2<DataBean, Fn.Callback>() {
			@Override
			public void call(DataBean arg1, Fn.Callback reply) {
				theNumberOfAssertions.add(null);
				Assert.assertEquals(arg1, after);
				reply.call();
			}
		});
		events.fire(app, "repli1", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertNull(arg1);
			}
		});

		events.on(app, "repli2", socket, new Fn.Callback2<DataBean, Fn.Callback1<DataBean>>() {
			@Override
			public void call(DataBean arg1, Fn.Callback1<DataBean> reply) {
				theNumberOfAssertions.add(null);
				Assert.assertEquals(arg1, after);
				reply.call(arg1);
			}
		});
		events.fire(app, "repli2", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertEquals(arg1, after);
			}
		});

		Assert.assertEquals(theNumberOfAssertions.size(), 4);
	}
	
}
