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
package com.github.flowersinthesand.portal;

import java.io.IOException;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.handler.AppHandler;
import com.github.flowersinthesand.portal.handler.EventsHandler;
import com.github.flowersinthesand.portal.spi.Dispatcher;

public class AppTest {

	@Test
	public void scan() {
		App app1 = new App(new Options().url("/t").packageOf(new EventsHandler()));
		Assert.assertNotNull(app1.bean(EventsHandler.class));
		Assert.assertNotNull(app1.bean(AppHandler.class));

		App app2 = new App(new Options().url("/t").packageOf("org.flowersinthesand.portal.handler"));
		try {
			app2.bean(EventsHandler.class);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
		try {
			app2.bean(AppHandler.class);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void init() throws IOException {
		App app = new App(new Options().url("/init").packageOf("com.github.flowersinthesand.portal.handler"));

		Dispatcher dispatcher = app.bean(Dispatcher.class);
		Assert.assertNotNull(dispatcher.handlers("load"));
		Assert.assertNull(dispatcher.handlers("ready"));
	}

	@Test
	public void inject() throws IOException {
		new App(new Options().url("/init").packageOf("com.github.flowersinthesand.portal.handler"));
		
		Assert.assertEquals(AppHandler.getPrivateRoom().name(), "privateRoom");
		Assert.assertEquals(AppHandler.getPackagePrivateRoom().name(), "packagePrivateRoom");
		Assert.assertEquals(AppHandler.getPublicRoom().name(), "publicRoom");
		Assert.assertEquals(AppHandler.getAnonymous().name(), "anonymous");
		Assert.assertNotNull(AppHandler.getDispatcher());
	}

	@Test
	public void lifecycle() throws IOException {
		App app = new App(new Options().url("/init").packageOf("com.github.flowersinthesand.portal.handler"));
		Assert.assertTrue(AppHandler.initialized);
		app.close();
		Assert.assertTrue(AppHandler.destroyed);
	}

	@Test
	public void finding() {
		Assert.assertNull(App.find());
		Assert.assertNull(App.find("/notfound"));

		App app1 = new App(new Options().url("/ok").register(true));
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok"), app1);

		App app2 = new App(new Options().url("/ok2").register(true));
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok2"), app2);
		
		App app3 = new App(new Options().name("ok3").url("/ok3").register(true));
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
	
	@Test
	public void close() {
		App app = new App(new Options().url("/t").packageOf(new EventsHandler())).set("xx", "yy");
		Assert.assertEquals(app.get("xx"), "yy");
		Assert.assertNotNull(app.bean(EventsHandler.class));

		app.close();
		Assert.assertNull(app.get("xx"));
		try {
			app.bean(EventsHandler.class);
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		}
	}

}
