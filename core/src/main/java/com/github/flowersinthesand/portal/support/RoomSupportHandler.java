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
package com.github.flowersinthesand.portal.support;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Destroy;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Order;
import com.github.flowersinthesand.portal.Room;
import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.RoomFactory;

@Bean
public class RoomSupportHandler {

	@Wire
	private Room hall;
	@Wire
	private RoomFactory roomFactory;

	@On
	@Order(Integer.MIN_VALUE)
	public void open(Socket socket) {
		hall.add(socket);
	}

	@On
	@Order(Integer.MIN_VALUE)
	public void close(Socket socket) {
		for (Room room : roomFactory.all()) {
			room.remove(socket);
		}
	}

	@Destroy
	public void destroy() {
		for (Room room : roomFactory.all()) {
			room.close();
			roomFactory.remove(room.name());
		}
	}

}
