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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Data;
import com.github.flowersinthesand.portal.On;
import com.github.flowersinthesand.portal.Reply;
import com.github.flowersinthesand.portal.Socket;

@Bean
public class ReplyHandler {

	private final Logger logger = LoggerFactory.getLogger(ReplyHandler.class);
	private Map<String, Map<Integer, Reply.Fn>> replies = new ConcurrentHashMap<String, Map<Integer, Reply.Fn>>();

	@On
	public void close(Socket socket) {
		if (replies.containsKey(socket.id())) {
			replies.remove(socket.id()).clear();
		}
	}

	@On
	public void reply(Socket socket, @Data Map<String, Object> data) {
		Integer eventId = (Integer) data.get("id");
		Object response = data.get("data");

		if (replies.containsKey(socket.id())) {
			Map<Integer, Reply.Fn> fns = replies.get(socket.id());
			if (fns.containsKey(eventId)) {
				logger.debug("Executing the reply function corresponding to the event#{} with the data {}", eventId, response);
				Reply.Fn reply = fns.remove(eventId);
				reply.done();
				reply.done(response);
			}
		}
	}

	public void set(String id, int eventId, final Reply.Fn reply) {
		if (!replies.containsKey(id)) {
			replies.put(id, new ConcurrentHashMap<Integer, Reply.Fn>());
		}
		replies.get(id).put(eventId, reply);
	}

}
