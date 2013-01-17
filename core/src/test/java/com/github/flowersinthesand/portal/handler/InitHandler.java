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
package com.github.flowersinthesand.portal.handler;

import com.github.flowersinthesand.portal.Handler;
import com.github.flowersinthesand.portal.Name;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Prepare;
import com.github.flowersinthesand.portal.Room;

@Handler("/init")
public class InitHandler {

	public static boolean prepared;
	@Name("privateRoom")
	private static Room privateRoom;
	@Name("packagePrivateRoom")
	static Room packagePrivateRoom;
	@Name("publicRoom")
	public static Room publicRoom;

	@Prepare
	public void prepare1() {
		prepared = true;
	}

	@On("load")
	public void onLoad() {}

	public static Room getPrivateRoom() {
		return privateRoom;
	}

	public static Room getPackagePrivateRoom() {
		return packagePrivateRoom;
	}

	public static Room getPublicRoom() {
		return publicRoom;
	}

}
