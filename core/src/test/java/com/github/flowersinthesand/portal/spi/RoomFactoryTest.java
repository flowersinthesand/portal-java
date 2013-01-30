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

import org.junit.Assert;
import org.testng.annotations.Test;

import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.support.DefaultRoomFactory;

public class RoomFactoryTest {

	@Test
	public void opening() {
		RoomFactory factory = new DefaultRoomFactory();
		Assert.assertEquals(factory.open("1").name(), "1");
		try {
			factory.open("1");
			Assert.assertTrue(false);
		} catch (IllegalStateException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void finding() {
		RoomFactory factory = new DefaultRoomFactory();
		Assert.assertNull(factory.find("1"));
		factory.open("1");
		Assert.assertEquals(factory.find("1").name(), "1");
	}

	@Test
	public void removal() {
		RoomFactory factory = new DefaultRoomFactory();
		factory.open("1");
		Assert.assertEquals(factory.find("1").name(), "1");
		factory.remove("1");
		Assert.assertNull(factory.find("1"));
	}

	@Test
	public void all() {
		RoomFactory factory = new DefaultRoomFactory();
		Room r1 = factory.open("1");
		Assert.assertArrayEquals(factory.all().toArray(), new Object[] { r1 });
		Room r2 = factory.open("2");
		Assert.assertArrayEquals(factory.all().toArray(), new Object[] { r1, r2 });
		factory.remove("1");
		Assert.assertArrayEquals(factory.all().toArray(), new Object[] { r2 });
	}

}
