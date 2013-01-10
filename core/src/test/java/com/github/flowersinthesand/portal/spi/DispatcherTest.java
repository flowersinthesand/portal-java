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
package com.github.flowersinthesand.portal.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.handler.DataBean;
import com.github.flowersinthesand.portal.handler.EventsHandler;
import com.github.flowersinthesand.portal.spi.DefaultDispatcher.EventHandler;

public class DispatcherTest {

	@Test
	public void staticBinding() throws SecurityException, NoSuchMethodException,
			InstantiationException, IllegalAccessException {
		EventsHandler h = new EventsHandler();

		DefaultDispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("load", h, h.getClass().getMethod("onLoad"));

		Map<String, Set<EventHandler>> eventHandlers = dispatcher.eventHandlers();
		Assert.assertNotNull(eventHandlers.get("load"));
	}

	@Test
	public void dynamicBinding() {
		DefaultDispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("e1", null, new Fn.Callback() {
			@Override
			public void call() {}
		});
		dispatcher.on("e2", null, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {}
		});
		dispatcher.on("e3", null, new Fn.Callback2<Object, Fn.Callback>() {
			@Override
			public void call(Object arg1, Fn.Callback reply) {}
		});
		dispatcher.on("e4", null, new Fn.Callback2<Object, Fn.Callback1<Object>>() {
			@Override
			public void call(Object arg1, Fn.Callback1<Object> reply) {}
		});

		Map<String, Set<EventHandler>> eventHandlers = dispatcher.eventHandlers();
		Assert.assertNotNull(eventHandlers.get("e1"));
		Assert.assertNotNull(eventHandlers.get("e2"));
		Assert.assertNotNull(eventHandlers.get("e3"));
		Assert.assertNotNull(eventHandlers.get("e4"));
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

		Dispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("socket", h, clazz.getMethod("onSocket", Socket.class));
		dispatcher.fire("socket", socket);
		Assert.assertArrayEquals(new Object[] { socket }, h.args);

		dispatcher.on("data", h, clazz.getMethod("onData", DataBean.class));
		dispatcher.fire("data", socket, before);
		Assert.assertArrayEquals(new Object[] { after }, h.args);

		dispatcher.on("repli", h, clazz.getMethod("onRepli", Fn.Callback.class));
		dispatcher.fire("repli", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertNull(arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback);

		dispatcher.on("repli-data", h, clazz.getMethod("onRepliData", Fn.Callback1.class, DataBean.class));
		dispatcher.fire("repli-data", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertEquals(after, arg1);
			}
		});
		Assert.assertTrue(h.args[0] instanceof Fn.Callback1);

		dispatcher.on("socket-data-repli", h, clazz.getMethod("onSocketDataRepli", Socket.class, DataBean.class, Fn.Callback1.class));
		dispatcher.fire("socket-data-repli", socket, before, new Fn.Callback1<Object>() {
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

		Dispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("signal", socket, new Fn.Callback() {
			@Override
			public void call() {
				theNumberOfAssertions.add(null);
				Assert.assertTrue(true);
			}
		});
		dispatcher.on("signal", intruder, new Fn.Callback() {
			@Override
			public void call() {
				theNumberOfAssertions.add(null);
				Assert.assertTrue(false);
			}
		});
		dispatcher.fire("signal", socket);

		dispatcher.on("data", socket, new Fn.Callback1<DataBean>() {
			@Override
			public void call(DataBean arg1) {
				theNumberOfAssertions.add(null);
				Assert.assertEquals(arg1, after);
			}
		});
		dispatcher.fire("data", socket, before);
		
		dispatcher.on("repli1", socket, new Fn.Callback2<DataBean, Fn.Callback>() {
			@Override
			public void call(DataBean arg1, Fn.Callback reply) {
				theNumberOfAssertions.add(null);
				Assert.assertEquals(arg1, after);
				reply.call();
			}
		});
		dispatcher.fire("repli1", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertNull(arg1);
			}
		});

		dispatcher.on("repli2", socket, new Fn.Callback2<DataBean, Fn.Callback1<DataBean>>() {
			@Override
			public void call(DataBean arg1, Fn.Callback1<DataBean> reply) {
				theNumberOfAssertions.add(null);
				Assert.assertEquals(arg1, after);
				reply.call(arg1);
			}
		});
		dispatcher.fire("repli2", socket, before, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				Assert.assertEquals(arg1, after);
			}
		});

		Assert.assertEquals(theNumberOfAssertions.size(), 4);
	}
	
}
