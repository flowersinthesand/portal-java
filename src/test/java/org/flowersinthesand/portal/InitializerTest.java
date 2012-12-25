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
package org.flowersinthesand.portal;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.flowersinthesand.portal.DefaultEventDispatcher.Invoker;
import org.flowersinthesand.portal.handler.PrepareHandler;
import org.junit.Assert;
import org.testng.annotations.Test;

public class InitializerTest {

	String pkg = "org.flowersinthesand.portal.handler";

	@Test
	public void scanning() throws IOException {
		Assert.assertTrue(new Initializer().init(null).apps().size() > 0);
		Assert.assertFalse(new Initializer().init("nowhere").apps().size() > 0);
		Assert.assertTrue(new Initializer().init(pkg).apps().size() > 0);
	}

	@Test
	public void initialization() throws IOException, SecurityException, NoSuchMethodException {
		App app = new Initializer().init(pkg).apps().get("/prepare");
		Assert.assertNotNull(app);

		Map<String, Set<Invoker>> invokers = ((DefaultEventDispatcher) app.getEventDispatcher()).invokers();
		Assert.assertNotNull(invokers.get("load"));
		Assert.assertNull(invokers.get("ready"));
		Assert.assertTrue(PrepareHandler.prepared);
	}

}
