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
package com.github.flowersinthesand.portal.support;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Socket;

@Bean
public class HeartbeatHandler {

	private final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
	private Map<String, Timer> timers = new ConcurrentHashMap<String, Timer>();

	@On.open
	public void open(final Socket socket) {
		long delay = 0;
		try {
			delay = Long.valueOf(socket.param("heartbeat"));
		} catch (NumberFormatException e) {
			return;
		}

		final String id = socket.param("id");
		Timer timer = new Timer();

		logger.debug("Setting heartbeat timer for socket#{}", id);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.debug("Heartbeat of socket#{} fails", id);
				socket.close();
			}
		}, delay);

		timers.put(id, timer);
	}

	@On.close
	public void close(Socket socket) {
		String id = socket.param("id");
		if (timers.containsKey(id)) {
			timers.remove(id).cancel();
		}
	}

	@On("heartbeat")
	public void heartbeat(Socket socket) {
		String id = socket.param("id");
		if (timers.containsKey(id)) {
			close(socket);
			open(socket);
			socket.send("heartbeat");
		}
	}

}
