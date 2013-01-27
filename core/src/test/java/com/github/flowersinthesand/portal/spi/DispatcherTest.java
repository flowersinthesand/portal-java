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

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.handler.DataBean;
import com.github.flowersinthesand.portal.handler.EventsHandler;
import com.github.flowersinthesand.portal.support.DefaultDispatcher;

public class DispatcherTest {

	@Test
	public void binding() throws SecurityException, NoSuchMethodException,
			InstantiationException, IllegalAccessException {
		EventsHandler h = new EventsHandler();

		Dispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("load", h, h.getClass().getMethod("load"));

		Map<String, Set<Dispatcher.Handler>> handlers = dispatcher.handlers();
		Assert.assertNotNull(handlers.get("load"));
	}

	@Test
	public void firing() throws SecurityException, NoSuchMethodException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Dispatcher dispatcher = new DefaultDispatcher();
		Field field = DefaultDispatcher.class.getDeclaredField("evaluator");
		field.setAccessible(true);
		field.set(dispatcher, new DefaultDispatcher.DefaultEvaluator());
		
		EventsHandler h = new EventsHandler();
		Class<?> clazz = h.getClass(); 
		Socket socket = Mockito.mock(Socket.class);
		
		Map<String, Object> before = new LinkedHashMap<String, Object>();
		before.put("number", 100);
		before.put("string", "String");
		DataBean after = new DataBean(100, "String");

		dispatcher.on("socket", h, clazz.getMethod("socket", Socket.class));
		dispatcher.fire("socket", socket);
		Assert.assertArrayEquals(new Object[] { socket }, h.args);

		dispatcher.on("data", h, clazz.getMethod("data", DataBean.class));
		dispatcher.fire("data", socket, before);
		Assert.assertArrayEquals(new Object[] { after }, h.args);

		dispatcher.on("nestedData", h, clazz.getMethod("nestedData", Map.class, DataBean.class, DataBean.class));
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("data1", before);
		map.put("data2", before);
		dispatcher.fire("nestedData", socket, map);
		Assert.assertArrayEquals(new Object[] { map, after, after }, h.args);

		MyCallback callback = null;
		dispatcher.on("repli", h, clazz.getMethod("repli", Fn.Callback.class));
		callback = new MyCallback();
		dispatcher.fire("repli", socket, before, callback);
		Assert.assertTrue(callback.called);
		Assert.assertNull(callback.arg1);
		Assert.assertTrue(h.args[0] instanceof Fn.Callback);

		dispatcher.on("repli-data", h, clazz.getMethod("repliData", Fn.Callback1.class, DataBean.class));
		callback = new MyCallback();
		dispatcher.fire("repli-data", socket, before, callback);
		Assert.assertTrue(callback.called);
		Assert.assertEquals(after, callback.arg1);
		Assert.assertTrue(h.args[0] instanceof Fn.Callback1);

		dispatcher.on("repli-data-return", h, clazz.getMethod("repliDataReturn", DataBean.class));
		callback = new MyCallback();
		dispatcher.fire("repli-data-return", socket, before, callback);
		Assert.assertTrue(callback.called);
		Assert.assertEquals(after, callback.arg1);
		
		dispatcher.on("socket-data-repli", h, clazz.getMethod("socketDataRepli", Socket.class, DataBean.class, Fn.Callback1.class));
		callback = new MyCallback();
		dispatcher.fire("socket-data-repli", socket, before, callback);
		Assert.assertTrue(callback.called);
		Assert.assertEquals(after, callback.arg1);
		Assert.assertSame(socket, h.args[0]);
		Assert.assertTrue(h.args[2] instanceof Fn.Callback1);
	}

	static class MyCallback implements Fn.Callback1<Object> {
		boolean called;
		Object arg1;

		@Override
		public void call(Object arg1) {
			this.arg1 = arg1;
			called = true;
		}
	}

}
