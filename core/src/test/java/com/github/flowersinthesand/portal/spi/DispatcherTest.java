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
package com.github.flowersinthesand.portal.spi;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.handler.DataBean;
import com.github.flowersinthesand.portal.handler.EventsHandler;
import com.github.flowersinthesand.portal.handler.OrderHandler;
import com.github.flowersinthesand.portal.handler.ThrowingHandler;
import com.github.flowersinthesand.portal.handler.ThrowingHandler.TestException;
import com.github.flowersinthesand.portal.support.DefaultDispatcher;

public class DispatcherTest {

	@Test
	public void binding() throws SecurityException, NoSuchMethodException {
		EventsHandler h = new EventsHandler();

		Dispatcher dispatcher = new DefaultDispatcher();
		dispatcher.on("load", h, h.getClass().getMethod("load"));

		Assert.assertNotNull(dispatcher.handlers("load"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void firing() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException {
		Dispatcher dispatcher = new DefaultDispatcher();
		Field field = DefaultDispatcher.class.getDeclaredField("evaluator");
		field.setAccessible(true);
		field.set(dispatcher, new DefaultDispatcher.DefaultEvaluator());
		
		EventsHandler h = new EventsHandler();
		Class<?> clazz = h.getClass();
		final Map<String, Object> replyInfo = new LinkedHashMap<String, Object>();
		Socket socket = Mockito.mock(Socket.class);
		Mockito.when(socket.send(Mockito.anyString(), Mockito.anyMap())).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				replyInfo.putAll((Map<String, Object>) invocation.getArguments()[1]);
				return null;
			}
		});
		
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

		dispatcher.on("repli", h, clazz.getMethod("repli", Reply.Fn.class));
		dispatcher.fire("repli", socket, before, 1);
		Assert.assertEquals(replyInfo.get("exception"), false);
		Assert.assertNull(replyInfo.get("data"));
		Assert.assertTrue(h.args[0] instanceof Reply.Fn);
		replyInfo.clear();

		dispatcher.on("repli-fail", h, clazz.getMethod("repliFail", Reply.Fn.class));
		dispatcher.fire("repli-fail", socket, before, 1);
		Assert.assertEquals(replyInfo.get("exception"), true);
		Assert.assertEquals(((Map<String, Object>) replyInfo.get("data")).get("type"), RuntimeException.class.getName());
		Assert.assertEquals(((Map<String, Object>) replyInfo.get("data")).get("message"), "X");
		Assert.assertTrue(h.args[0] instanceof Reply.Fn);
		replyInfo.clear();
		
		dispatcher.on("repli2", h, clazz.getMethod("repli2"));
		dispatcher.fire("repli2", socket, before, 1);
		
		dispatcher.on("repli-data", h, clazz.getMethod("repliData", Reply.Fn.class, DataBean.class));
		dispatcher.fire("repli-data", socket, before, 1);
		Assert.assertEquals(replyInfo.get("data"), after);
		Assert.assertTrue(h.args[0] instanceof Reply.Fn);
		replyInfo.clear();

		dispatcher.on("repli-data2", h, clazz.getMethod("repliData2", DataBean.class));
		dispatcher.fire("repli-data2", socket, before, 1);
		Assert.assertEquals(replyInfo.get("data"), after);
		replyInfo.clear();
		
		dispatcher.on("socket-data-repli", h, clazz.getMethod("socketDataRepli", Socket.class, DataBean.class, Reply.Fn.class));
		dispatcher.fire("socket-data-repli", socket, before, 1);
		Assert.assertEquals(replyInfo.get("data"), after);
		Assert.assertSame(socket, h.args[0]);
		Assert.assertTrue(h.args[2] instanceof Reply.Fn);
		replyInfo.clear();
	}
	
	@Test
	public void order() throws SecurityException, NoSuchMethodException {
		OrderHandler h = new OrderHandler();

		Dispatcher dispatcher = new DefaultDispatcher();
		Socket socket = Mockito.mock(Socket.class);

		for (String methodName : new String[] { "x1", "x2", "x3", "y1", "y2", "y3" }) {
			dispatcher.on(methodName.substring(0, 1), h, h.getClass().getMethod(methodName));
		}

		dispatcher.fire("x", socket);
		Assert.assertArrayEquals(h.args.toArray(), new Object[] { -1, 0, 1 });
		h.args.clear();
		dispatcher.fire("y", socket);
		Assert.assertArrayEquals(h.args.toArray(), new Object[] { -1, 0, 1 });
	}
	
	@Test
	public void throwing() throws SecurityException, NoSuchMethodException {
		ThrowingHandler h = new ThrowingHandler();

		Dispatcher dispatcher = new DefaultDispatcher();
		final Map<String, Object> replyInfo = new LinkedHashMap<String, Object>();
		Socket socket = Mockito.mock(Socket.class);
		Mockito.when(socket.send(Mockito.anyString(), Mockito.anyMap())).thenAnswer(new Answer<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				replyInfo.putAll((Map<String, Object>) invocation.getArguments()[1]);
				return null;
			}
		});
		
		Map<String, Object> failInfo = new LinkedHashMap<String, Object>();
		failInfo.put("type", TestException.class.getName());
		failInfo.put("message", "Hello");

		dispatcher.on("success", h, h.getClass().getMethod("success"));
		dispatcher.fire("success", socket, null, 1);
		Assert.assertEquals(replyInfo.get("exception"), false);

		dispatcher.on("fail1", h, h.getClass().getMethod("fail1"));
		dispatcher.fire("fail1", socket, null, 1);
		Assert.assertEquals(replyInfo.get("exception"), true);
		Assert.assertEquals(replyInfo.get("data"), failInfo);

		dispatcher.on("fail2", h, h.getClass().getMethod("fail2"));
		dispatcher.fire("fail2", socket, null, 1);
		Assert.assertEquals(replyInfo.get("exception"), true);
		Assert.assertEquals(replyInfo.get("data"), failInfo);

		dispatcher.on("fail3", h, h.getClass().getMethod("fail3"));
		try {
			dispatcher.fire("fail3", socket, null, 1);
			Assert.assertTrue(false);
		} catch (RuntimeException e) {
			Throwable ex = e;
			while (ex.getCause() != null) {
				ex = ex.getCause();
			}

			Assert.assertTrue(ex.getClass().isAssignableFrom(TestException.class));
		}
	}

}
