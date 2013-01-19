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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.handler.InitHandler;
import com.github.flowersinthesand.portal.spi.DefaultDispatcher;
import com.github.flowersinthesand.portal.spi.DefaultDispatcher.EventHandler;
import com.github.flowersinthesand.portal.spi.InitializerAdapter;

public class AppTest {

	@Test
	public void scan() {
		new App().init("/t", new Options().packages("com.github.flowersinthesand.portal"), new InitializerAdapter() {
			int size = 0;

			@Override
			public void postInstantiation(String name, Object bean) {
				if (name.startsWith("com.github.flowersinthesand.portal")) {
					size++;
				}
			}

			@Override
			public void postInitialization() {
				Assert.assertEquals(size, 3);
			}
		});
		new App().init("/t", new Options().packages("org.flowersinthesand.portal"), new InitializerAdapter() {
			int size = 0;

			@Override
			public void postInstantiation(String name, Object bean) {
				if (name.startsWith("org.flowersinthesand.portal")) {
					size++;
				}
			}

			@Override
			public void postInitialization() {
				Assert.assertEquals(size, 0);
			}
		});
	}

	@Test
	public void init() throws IOException {
		new App().init("/init", new Options().packages("com.github.flowersinthesand.portal"), new InitializerAdapter() {
			@Override
			public void postInstantiation(String name, Object bean) {
				if (bean.getClass().isAssignableFrom(DefaultDispatcher.class)) {
					DefaultDispatcher dispatcher = (DefaultDispatcher) bean;
					Map<String, Set<EventHandler>> eventHandlers = dispatcher.eventHandlers();
					
					Assert.assertNotNull(eventHandlers.get("load"));
					Assert.assertNull(eventHandlers.get("ready"));
				}
			}
		});
	}
	

	@Test
	public void inject() throws IOException {
		new App().init("/init", new Options().packages("com.github.flowersinthesand.portal"));
		
		Assert.assertEquals(InitHandler.getPrivateRoom().name(), "privateRoom");
		Assert.assertEquals(InitHandler.getPackagePrivateRoom().name(), "packagePrivateRoom");
		Assert.assertEquals(InitHandler.getPublicRoom().name(), "publicRoom");
	}

	@Test
	public void preparation() throws IOException {
		new App().init("/init", new Options().packages("com.github.flowersinthesand.portal"));
		Assert.assertTrue(InitHandler.prepared);
	}

	@Test
	public void finding() {
		try {
			
		Assert.assertNull(App.find());
		Assert.assertNull(App.find("/notfound"));

		App app1 = new App().init("/ok").register();
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok"), app1);

		App app2 = new App().init("/ok2").register();
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok2"), app2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void room() {
		Assert.assertEquals(new App().init("nothing").room("/ok").name(), "/ok");
	}

	@Test
	public void attr() {
		App app = new App().init("nothing");
		Assert.assertNull(app.get("b"));

		String data = "data";
		Assert.assertSame(app.set("data", data).get("data"), data);
	}

}
