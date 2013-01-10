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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.handler.EventsHandler;
import com.github.flowersinthesand.portal.handler.InitHandler;
import com.github.flowersinthesand.portal.spi.DefaultDispatcher;
import com.github.flowersinthesand.portal.spi.DefaultDispatcher.EventHandler;
import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.NoOpSocketManager;
import com.github.flowersinthesand.portal.spi.SocketManager;

@SuppressWarnings("serial")
public class InitializerTest {

	@Test
	public void controller() {
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>(), new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 0);
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("controllers", new LinkedHashSet<String>(){{
					add(EventsHandler.class.getName());
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 1);
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("controllers", new LinkedHashSet<String>(){{
					add(EventsHandler.class.getName());
					add(InitHandler.class.getName());
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 2);
	}

	@Test
	public void scan() {
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", ".");
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 0);
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", ".");
				put("locations", new LinkedHashSet<String>(){{
					add("/target");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 2);
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", "../");
				put("locations", new LinkedHashSet<String>(){{
					add("/target");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 0);
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", ".");
				put("locations", new LinkedHashSet<String>(){{
					add("/src");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 0);
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("packages", new LinkedHashSet<String>(){{
					add("com.github.flowersinthesand.portal");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 2);
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("packages", new LinkedHashSet<String>(){{
					add("org.flowersinthesand.portal");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.size() == 0);
	}
	
	@Test
	public void classes() {
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", ".");
				put("locations", new LinkedHashSet<String>(){{
					add("");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>() {{
				put(SocketManager.class, NoOpSocketManager.class);
			}})
			.apps()
			.get("/init")
			.bean(SocketManager.class) instanceof NoOpSocketManager);
		
		Assert.assertTrue(new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", ".");
				put("locations", new LinkedHashSet<String>(){{
					add("");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>() {{
				put(Dispatcher.class, DefaultDispatcher.class);
			}})
			.apps()
			.get("/init")
			.bean(Dispatcher.class) instanceof DefaultDispatcher);
	}

	@Test
	public void init() throws IOException {
		App app = new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", ".");
				put("locations", new LinkedHashSet<String>(){{
					add("");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.get("/init");
		Assert.assertNotNull(app);
		
		Map<String, Set<EventHandler>> eventHandlers = ((DefaultDispatcher) app.bean(Dispatcher.class)).eventHandlers();
		Assert.assertNotNull(eventHandlers.get("load"));
		Assert.assertNull(eventHandlers.get("ready"));
	}

	@Test
	public void inject() throws IOException {
		App app = new Initializer()
			.init(new LinkedHashMap<String, Object>() {{
				put("base", ".");
				put("locations", new LinkedHashSet<String>(){{
					add("");
				}});
			}}, new LinkedHashMap<Class<?>, Class<?>>())
			.apps()
			.get("/init");
		
		Assert.assertEquals(InitHandler.getPrivateRoom().name(), "privateRoom");
		Assert.assertEquals(InitHandler.getPackagePrivateRoom().name(), "packagePrivateRoom");
		Assert.assertEquals(InitHandler.getPublicRoom().name(), "publicRoom");
		Assert.assertSame(InitHandler.getApp(), app);
	}

	@Test
	public void preparation() throws IOException {
		new Initializer()
		.init(new LinkedHashMap<String, Object>() {{
			put("base", ".");
			put("locations", new LinkedHashSet<String>(){{
				add("");
			}});
		}}, new LinkedHashMap<Class<?>, Class<?>>());
		Assert.assertTrue(InitHandler.prepared);
	}

}
