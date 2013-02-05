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

import java.nio.CharBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Fn;
import com.github.flowersinthesand.portal.Socket;

public abstract class AbstractSocket implements Socket {

	protected static final String padding2K = CharBuffer.allocate(2048).toString().replace('\0', ' ');

	private final Logger logger = LoggerFactory.getLogger(AbstractSocket.class);
	protected boolean isAndroid;
	protected Map<String, String> params;
	protected ObjectMapper mapper = new ObjectMapper();
	protected AtomicInteger eventId = new AtomicInteger();

	@Override
	public String id() {
		return params.get("id");
	}

	@Override
	public String param(String key) {
		return params.get(key);
	}

	@Override
	public Socket send(String event) {
		doSend(event, null, false);
		return this;
	}

	@Override
	public Socket send(String event, Object data) {
		doSend(event, data, false);
		return this;
	}

	@Override
	public Socket send(String event, Object data, Fn.Callback callback) {
		doSend(event, data, true);
		bindReply(callback);
		return this;
	}

	@Override
	public Socket send(String event, Object data, Fn.Callback1<?> callback) {
		doSend(event, data, true);
		bindReply(callback);
		return this;
	}

	protected void doSend(String type, Object data, boolean reply) {
		Map<String, Object> message = new LinkedHashMap<String, Object>();

		message.put("id", eventId.incrementAndGet());
		message.put("type", type);
		message.put("data", data);
		message.put("reply", reply);

		logger.info("Socket#{} is sending an event {}", id(), message);
		cache(message);
		transmit(format(message));
	}

	protected String format(Object message) {
		StringBuilder builder = new StringBuilder();
		String transport = param("transport");
		String data;
		try {
			data = mapper.writeValueAsString(message);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		logger.debug("Formatting data {} for {} transport", data, transport);

		if (transport.equals("ws")) {
			builder.append(data);
		} else if (transport.equals("sse") || transport.startsWith("stream")) {
			if (isAndroid) {
				builder.append(padding2K).append(padding2K);
			}
			for (String datum : data.split("\r\n|\r|\n")) {
				builder.append("data: ").append(datum).append("\n");
			}
			builder.append("\n");
		} else if (transport.startsWith("longpoll")) {
			if (transport.equals("longpolljsonp")) {
				try {
					builder.append(param("callback")).append("(").append(mapper.writeValueAsString(data)).append(");");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				builder.append(data);
			}
		}

		return builder.toString();
	}

	@Override
	public Socket close() {
		logger.info("Closing socket#{}", id());
		disconnect();
		return this;
	}

	protected Map<String, String> params(Map<String, String[]> params) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Entry<String, String[]> entry : params.entrySet()) {
			map.put(entry.getKey(), entry.getValue()[0]);
		}

		return map;
	}

	protected boolean isAndroid(String userAgent) {
		return userAgent.matches(".*Android\\s[23]\\..*");
	}

	protected void cache(Map<String, Object> message) {}

	abstract protected void bindReply(Fn.Callback callback);

	abstract protected void bindReply(Fn.Callback1<?> callback);

	abstract protected void transmit(String it);

	abstract protected void disconnect();

}