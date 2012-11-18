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
package org.flowersinthesand.portal.config;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.flowersinthesand.portal.dispatcher.Dispatcher;
import org.flowersinthesand.portal.dispatcher.Dispatcher.Invoker;
import org.flowersinthesand.portal.handler.InitHandler;
import org.junit.Assert;
import org.testng.annotations.Test;

public class InitializerTest {

	@Test
	public void scanning() throws IOException {
		Dispatcher dispatcher1 = new Dispatcher();
		new Initializer(dispatcher1).init(null);
		Assert.assertTrue(dispatcher1.events().size() > 0);

		Dispatcher dispatcher2 = new Dispatcher();
		new Initializer(dispatcher2).init("nowhere");
		Assert.assertEquals(dispatcher2.events().size(), 0);

		Dispatcher dispatcher3 = new Dispatcher();
		new Initializer(dispatcher3).init("org.flowersinthesand.portal.handler");
		Assert.assertTrue(dispatcher3.events().size() > 0);
	}

	@Test
	public void initialization() throws IOException, SecurityException, NoSuchMethodException {
		Dispatcher dispatcher = new Dispatcher();
		new Initializer(dispatcher).init("org.flowersinthesand.portal.handler");

		Map<String, Map<String, Set<Invoker>>> events = dispatcher.events();
		Assert.assertNotNull(events.get("/init"));

		Map<String, Set<Invoker>> event = events.get("/init");
		Assert.assertNotNull(event.get("load"));
		Assert.assertNull(event.get("ready"));

		Assert.assertTrue(InitHandler.prepared);
	}

}
