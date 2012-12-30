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

import org.junit.Assert;
import org.testng.annotations.Test;

public class AppTest {

	@Test
	public void finding() {
		Assert.assertNull(App.find());
		Assert.assertNull(App.find("/notfound"));

		App app1 = new App();
		App.add("/ok", app1);
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok"), app1);

		App app2 = new App();
		App.add("/ok2", app2);
		Assert.assertSame(App.find(), app1);
		Assert.assertSame(App.find("/ok2"), app2);
	}

	@Test
	public void room() {
		App app = new App();
		Assert.assertNotNull(app.room("/ok"));
	}

	@Test
	public void attr() {
		App app = new App();
		Assert.assertNull(app.get("b"));
		
		String data = "data";
		Assert.assertSame(app.set("data", data).get("data"), data);
	}
}
