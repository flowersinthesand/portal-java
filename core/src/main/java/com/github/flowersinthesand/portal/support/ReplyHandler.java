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
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Socket;

@Bean
public class ReplyHandler {

	private final Logger logger = LoggerFactory.getLogger(ReplyHandler.class);
	private Map<String, Map<Integer, Fn.Callback1<?>>> callbacks = new ConcurrentHashMap<String, Map<Integer, Fn.Callback1<?>>>();

	@On.close
	public void close(Socket socket) {
		String id = socket.param("id");
		if (callbacks.containsKey(id)) {
			callbacks.remove(id).clear();
		}
	}

	@SuppressWarnings("unchecked")
	@On("reply")
	public void reply(Socket socket, @Data Map<String, Object> data) {
		Integer eventId = (Integer) data.get("id");
		Object response = data.get("data");

		String id = socket.param("id");
		if (callbacks.containsKey(id)) {
			Map<Integer, Fn.Callback1<?>> fns = callbacks.get(id);
			if (fns.containsKey(eventId)) {
				logger.debug("Executing the reply function corresponding to the event#{} with the data {}", eventId, response);
				((Fn.Callback1<Object>) fns.remove(eventId)).call(response);
			}
		}
	}

	public void set(String id, int eventId, final Fn.Callback callback) {
		set(id, eventId, new Fn.Callback1<Object>() {
			@Override
			public void call(Object arg1) {
				callback.call();
			}
		});
	}

	public void set(String id, int eventId, final Fn.Callback1<?> callback) {
		if (!callbacks.containsKey(id)) {
			callbacks.put(id, new ConcurrentHashMap<Integer, Fn.Callback1<?>>());
		}
		callbacks.get(id).put(eventId, callback);
	}

}
