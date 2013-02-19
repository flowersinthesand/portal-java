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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Socket;

@Bean
public class HeartbeatHandler {

	private final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
	private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<String, ScheduledFuture<?>>();

	@On
	public void open(final Socket socket) {
		long delay = 0;
		try {
			delay = Long.valueOf(socket.param("heartbeat"));
		} catch (NumberFormatException e) {
			return;
		}

		logger.debug("Setting heartbeat timer for socket#{}", socket.id());
		futures.put(socket.id(), service.schedule(new Runnable() {
			@Override
			public void run() {
				logger.debug("Heartbeat of socket#{} fails", socket.id());
				socket.close();
			}
		}, delay, TimeUnit.MILLISECONDS));
	}

	@On
	public void close(Socket socket) {
		if (futures.containsKey(socket.id())) {
			futures.remove(socket.id()).cancel(true);
		}
	}

	@On
	public void heartbeat(Socket socket) {
		if (futures.containsKey(socket.id())) {
			close(socket);
			open(socket);
			socket.send("heartbeat");
		}
	}

	public ScheduledExecutorService service() {
		return service;
	}

}
