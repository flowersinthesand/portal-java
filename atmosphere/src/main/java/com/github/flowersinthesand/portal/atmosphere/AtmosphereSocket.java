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
package com.github.flowersinthesand.portal.atmosphere;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;

public class AtmosphereSocket extends Socket {

	private final Logger logger = LoggerFactory.getLogger(AtmosphereSocket.class);
	private Timer heartbeatTimer;
	private AtomicInteger eventId = new AtomicInteger(0);
	private Set<Map<String, Object>> cache = new CopyOnWriteArraySet<Map<String, Object>>();
	private Map<Integer, Fn.Callback1<Object>> callbacks = new ConcurrentHashMap<Integer, Fn.Callback1<Object>>();

	public AtmosphereSocket(String query, App app) {
		super(query, app);
	}

	public String id() {
		return param("id");
	}

	public synchronized Timer heartbeatTimer() {
		return heartbeatTimer;
	}

	public synchronized void setHeartbeatTimer() {
		final String id = id();
		
		if (heartbeatTimer != null) {
			logger.debug("Canceling heartbeat timer for Socket#{}", id);
			heartbeatTimer.cancel();
		}
		try {
			long delay = Long.valueOf(param("heartbeat"));
			logger.debug("Setting heartbeat timer for Socket#{}", id);
			heartbeatTimer = new Timer("Heartbeat timer for Socket#" + id);
			heartbeatTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					logger.debug("Closing Socket#{} due to heartbeat fail", id);
					close();
				}
			}, delay);
		} catch (NumberFormatException e) {}
	}

	public AtomicInteger eventId() {
		return eventId;
	}

	public Map<Integer, Fn.Callback1<Object>> callbacks() {
		return callbacks;
	}

	public Set<Map<String, Object>> cache() {
		return cache;
	}

	public Map<String, Object> cache(Map<String, Object> message) {
		if (param("transport").startsWith("longpoll")) {
			cache.add(message);
		}
		return message;
	}

}
