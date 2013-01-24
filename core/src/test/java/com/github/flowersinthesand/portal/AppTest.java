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

import com.github.flowersinthesand.portal.handler.EventsHandler;
import com.github.flowersinthesand.portal.handler.InitHandler;
import com.github.flowersinthesand.portal.support.DefaultDispatcher;
import com.github.flowersinthesand.portal.support.DefaultDispatcher.EventHandler;

public class AppTest {

	@Test
	public void scan() {
		App app1 = new App(new Options().url("/t").packageOf(new EventsHandler()));
		Assert.assertNotNull(app1.bean(EventsHandler.class));
		Assert.assertNotNull(app1.bean(InitHandler.class));
		
		App app2 = new App(new Options().url("/t").packageOf("org.flowersinthesand.portal.handler"));
		Assert.assertNull(app2.bean(EventsHandler.class));
		Assert.assertNull(app2.bean(InitHandler.class));
	}

	@Test
	public void init() throws IOException {
		App app = new App(new Options().url("/init").packageOf("com.github.flowersinthesand.portal.handler"));

		DefaultDispatcher dispatcher = app.bean(DefaultDispatcher.class);
		Map<String, Set<EventHandler>> eventHandlers = dispatcher.eventHandlers();

		Assert.assertNotNull(eventHandlers.get("load"));
		Assert.assertNull(eventHandlers.get("ready"));
	}

	@Test
	public void inject() throws IOException {
		new App(new Options().url("/init").packageOf("com.github.flowersinthesand.portal.handler"));
		
		Assert.assertEquals(InitHandler.getPrivateRoom().name(), "privateRoom");
		Assert.assertEquals(InitHandler.getPackagePrivateRoom().name(), "packagePrivateRoom");
		Assert.assertEquals(InitHandler.getPublicRoom().name(), "publicRoom");
		Assert.assertNotNull(InitHandler.getDispatcher());
	}

	@Test
	public void preparation() throws IOException {
		new App(new Options().url("/init").packageOf("com.github.flowersinthesand.portal.handler"));
		Assert.assertTrue(InitHandler.prepared);
	}

	@Test
	public void finding() {
		Assert.assertNull(App.find());
		Assert.assertNull(App.find("/notfound"));

		App app1 = new App(new Options().url("/ok"));
		Assert.assertNull(App.find());
		Assert.assertNull(App.find("/ok"));
		
		app1.register();
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok"), app1);

		App app2 = new App(new Options().url("/ok2")).register();
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok2"), app2);
		
		App app3 = new App(new Options().name("ok3").url("/ok3")).register();
		Assert.assertSame(App.find("ok3"), app3);
	}

	@Test
	public void room() {
		Assert.assertEquals(new App(new Options().url("nothing")).room("/ok").name(), "/ok");
	}

	@Test
	public void attr() {
		App app = new App(new Options().url("nothing"));
		Assert.assertNull(app.get("b"));

		String data = "data";
		Assert.assertSame(app.set("data", data).get("data"), data);
	}

}
