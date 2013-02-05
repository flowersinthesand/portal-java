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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Socket;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.Dispatcher;
import com.github.flowersinthesand.portal.spi.SocketFactory;

public abstract class AbstractSocketFactory implements SocketFactory {

	private final Logger logger = LoggerFactory.getLogger(AbstractSocketFactory.class);
	protected Map<String, Socket> sockets = new ConcurrentHashMap<String, Socket>();
	protected ObjectMapper mapper = new ObjectMapper();
	@Wire
	protected Dispatcher dispatcher;
	@Wire
	protected ReplyHandler replyHandler;

	@Override
	public Socket find(String id) {
		return sockets.get(id);
	}

	public void fire(String raw) {
		Map<String, Object> m;
		try {
			m = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.info("Receiving an event {}", m);
		dispatcher.fire((String) m.get("type"), sockets.get(m.get("socket")), m.get("data"), (Boolean) m.get("reply") ? (Integer) m.get("id") : 0);
	}

}
