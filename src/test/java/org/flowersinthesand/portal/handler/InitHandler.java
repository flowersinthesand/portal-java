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
package org.flowersinthesand.portal.handler;

import org.flowersinthesand.portal.App;
import org.flowersinthesand.portal.Handler;
import org.flowersinthesand.portal.Name;
import org.flowersinthesand.portal.On;
import org.flowersinthesand.portal.Prepare;
import org.flowersinthesand.portal.Room;

@Handler("/init")
public class InitHandler {

	public static boolean prepared;
	@Name("/init")
	static App app;
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

	public static App getApp() {
		return app;
	}

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
