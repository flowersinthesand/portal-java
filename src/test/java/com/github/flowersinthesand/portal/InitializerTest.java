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

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.DefaultEventDispatcher;
import com.github.flowersinthesand.portal.Initializer;
import com.github.flowersinthesand.portal.DefaultEventDispatcher.Invoker;
import com.github.flowersinthesand.portal.handler.InitHandler;

public class InitializerTest {

	String pkg = "com.github.flowersinthesand.portal.handler";

	@Test
	public void scanning() throws IOException {
		Assert.assertTrue(new Initializer().init(null).apps().size() > 0);
		Assert.assertFalse(new Initializer().init("nowhere").apps().size() > 0);
		Assert.assertTrue(new Initializer().init(pkg).apps().size() > 0);
	}

	@Test
	public void initialization() throws IOException {
		App app = new Initializer().init(pkg).apps().get("/init");
		Assert.assertNotNull(app);

		Map<String, Set<Invoker>> invokers = ((DefaultEventDispatcher) app.eventDispatcher()).invokers();
		Assert.assertNotNull(invokers.get("load"));
		Assert.assertNull(invokers.get("ready"));
	}

	@Test
	public void injection() throws IOException {
		App app = new Initializer().init(pkg).apps().get("/init");
		
		Assert.assertEquals(InitHandler.getPrivateRoom().name(), "privateRoom");
		Assert.assertEquals(InitHandler.getPackagePrivateRoom().name(), "packagePrivateRoom");
		Assert.assertEquals(InitHandler.getPublicRoom().name(), "publicRoom");
		Assert.assertSame(InitHandler.getApp(), app);
	}

	@Test
	public void preparation() throws IOException {
		new Initializer().init(pkg).apps().get("/init");
		
		Assert.assertTrue(InitHandler.prepared);
	}


}
