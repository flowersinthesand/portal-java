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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.DefaultEventDispatcher.EventHandler;
import com.github.flowersinthesand.portal.atmosphere.NoOpSocketManager;
import com.github.flowersinthesand.portal.handler.InitHandler;

public class InitializerTest {
	
	List<File> files;
	Map<String, String> options;

	@BeforeMethod
	public void setPath() throws URISyntaxException {
		files = new ArrayList<File>();
		files.add(new File(Thread.currentThread().getContextClassLoader().getResource("").toURI()));
		options = new LinkedHashMap<String, String>();
		options.put(SocketManager.class.getName(), NoOpSocketManager.class.getName());
	}

	@Test
	public void scanning() throws IOException {
		Assert.assertTrue(new Initializer().init(files, options).apps().size() > 0);
		files.set(0, new File(files.get(0), "fake"));
		Assert.assertTrue(new Initializer().init(files, options).apps().size() == 0);
	}

	@Test
	public void initialization() throws IOException {
		App app = new Initializer().init(files, options).apps().get("/init");
		Assert.assertNotNull(app);

		Map<String, Set<EventHandler>> eventHandlers = ((DefaultEventDispatcher) app.eventDispatcher()).eventHandlers();
		Assert.assertNotNull(eventHandlers.get("load"));
		Assert.assertNull(eventHandlers.get("ready"));
	}

	@Test
	public void injection() throws IOException {
		App app = new Initializer().init(files, options).apps().get("/init");
		
		Assert.assertEquals(InitHandler.getPrivateRoom().name(), "privateRoom");
		Assert.assertEquals(InitHandler.getPackagePrivateRoom().name(), "packagePrivateRoom");
		Assert.assertEquals(InitHandler.getPublicRoom().name(), "publicRoom");
		Assert.assertSame(InitHandler.getApp(), app);
	}

	@Test
	public void preparation() throws IOException {
		new Initializer().init(files, options).apps().get("/init");
		
		Assert.assertTrue(InitHandler.prepared);
	}


}
